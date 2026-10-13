package com.dvm.rune_server_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dvm.rune_server_backend.common.RuneConstants;
import com.dvm.rune_server_backend.mapper.DragonMapper;
import com.dvm.rune_server_backend.model.dto.RuneOptimizeRequest;
import com.dvm.rune_server_backend.model.dto.RuneSceneRequest;
import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.model.vo.*;
import com.dvm.rune_server_backend.service.AiService;
import com.dvm.rune_server_backend.service.DragonService;
import com.dvm.rune_server_backend.service.RuneOptimizeService;
import com.dvm.rune_server_backend.service.RuneService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.dvm.rune_server_backend.common.RuneConstants.PERCENT_STATS;

/**
 * @author 25826
 * 符文搭配优化：推荐行（当前分）+ 研磨搭配行（潜在分）
 */
@Service
@RequiredArgsConstructor
public class RuneOptimizeServiceImpl implements RuneOptimizeService {

    private final ObjectMapper objectMapper;
    private final DragonMapper dragonMapper;

    private final RuneService runeService;
    private final AiService aiService;
    private final DragonService dragonService;

    /**
     * base-8 编码的数值套组件数向量下标，通过取余可以获取对应的套组件数
     * 顺序对齐 NUMERICAL_SETS：体力/机会/坚固/强烈/伤害/快速
     * 例：key = 20
     * counts[0] = (20 / 1) % 8 = 20 % 8 = 4   → 体力 4 件
     * counts[1] = (20 / 8) % 8 = 2 % 8 = 2    → 机会 2 件
     */
    private static final int[] SET_BASE = {1, 8, 64, 512, 4096, 32768};
    // 6个纯数值套组各自装了几件
    private static final int MAX_SET_KEY = 1 << 18;

    // 全部词条
    private static final List<String> ALL_STAT_TYPES;
    static {
        List<String> all = new ArrayList<>(PERCENT_STATS);
        all.addAll(List.of("攻击力+", "防御力+", "生命力+"));
        ALL_STAT_TYPES = List.copyOf(all);
    }

    // 研磨规划：潜在分 + 被替换词条 X + 目标词条 Y
    private record GrindPlan(double potentialScore, RuneVO.SubStatsVO targetSub, String targetStat) {}

    // 6 槽位最优搭配结果
    private record BestResult(List<RuneOptimizeSlotVO> slots, double total) {}

    @Override
    public RuneOptimizeInitVO getInit(Long userId, Long dragonId){
        if (dragonId == null) {
            return buildInit(RuneConstants.emptyWeights(), "请选择一条龙");
        }
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId);
        Dragons dragon = dragonMapper.selectOne(wrapper);
        if (dragon == null) {
            return buildInit(RuneConstants.emptyWeights(), "该账号不存在这条龙，请重试");
        }
        String aiAnalysis = dragon.getAiAnalysis();
        if (aiAnalysis != null && !aiAnalysis.isBlank()) {
            RuneOptimizeInitVO init = parseAiAnalysis(aiAnalysis);
            if (init != null) {
                return init;
            }
        }
        dragonService.refreshBaseAnalysis(userId, dragonId);
        Map<String, Integer> weights = RuneConstants.baseWeights(dragon.getCategory());
        return buildInit(weights, "暂无分析结果，使用类别基础权重（" + dragon.getCategory() + "）");
    }

    @Override
    public RuneOptimizeVO optimize(Long userId, RuneOptimizeRequest dto) {
        Long dragonId = dto.getDragonId();
        // 权重词条清洗 + 阈值设置
        Map<String, Integer> weights = RuneConstants.sanitizeWeights(dto.getWeights());

        List<RuneVO> allRunes = runeService.selectRuneByUserIdAndCondition(userId, null, null, null);
        // 缺键时 false 处理
        boolean includeOthersEquipped = Boolean.TRUE.equals(dto.getIncludeOthersEquipped());
        // 候选池：AI 分析搭配需要剔除锁定符文
        List<RuneVO> candidates = allRunes.stream().filter
                        // 未锁定
                        (r -> !Boolean.TRUE.equals(r.getLocked()) ||
                        // 已锁定，但为自身装备的符文
                        (r.getEquippedDragonId() != null && r.getEquippedDragonId().equals(dragonId)))
                        // 闲置符文
                        .filter(r -> !Boolean.TRUE.equals(r.getEquipped()) ||
                        // 自身装备的符文
                        (r.getEquippedDragonId() != null && r.getEquippedDragonId().equals(dragonId)) ||
                        // 其他龙装备的符文
                        includeOthersEquipped)
                        .toList();

        // 预计算每件符文的当前分与研磨潜在分数
        Map<Long, Double> curScores = new HashMap<>();
        Map<Long, GrindPlan> grindPlans = new HashMap<>();
        for (RuneVO r : candidates) {
            double cur = scoreRune(r, weights);
            curScores.put(r.getRuneId(), cur);

            GrindPlan plan = potentialPlan(r, weights, cur);
            if (plan != null) {
                grindPlans.put(r.getRuneId(), plan);
            }
        }

        // 各套组需求数量 → 符文类型对应数量下限
        Map<String, Integer> floors = dragonFloors(userId, dto.getDragonId());
        // 凑不齐下限时的放宽提示
        String warning = null;
        BestResult rec;
        BestResult grind;
        if (floors.isEmpty()) {
            // 无需求：走 solve()
            rec = solve(candidates, weights, curScores, null);
            grind = solve(candidates, weights, curScores, grindPlans);
        } else {
            rec = solveWithFloors(candidates, weights, curScores, null, floors);
            if (rec == null) {
                // todo：更改为有多少组符合要求就选取多少组，剩余无法满足要求的使用无要求最优补齐
                // 用户搭配无法凑齐 → 放宽为无要求最优，warning 提示前端
                warning = "当前选择符文数量不足，已提供备选搭配";
                rec = solve(candidates, weights, curScores, null);
                grind = solve(candidates, weights, curScores, grindPlans);
            } else {
                grind = solveWithFloors(candidates, weights, curScores, grindPlans, floors);
                // 无要求兜底，防 NPE
                if (grind == null) {
                    grind = solve(candidates, weights, curScores, grindPlans);
                }
            }
        }
        RuneOptimizeVO vo = new RuneOptimizeVO();
        vo.setSlotResults(rec.slots());
        vo.setTotalScore(rec.total());

        // 研磨搭配行：6 槽符文 + 每槽研磨建议 + 下限值
        List<GrindSlotVO> grindSlots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            RuneOptimizeSlotVO slot = grind.slots().get(i);
            GrindSlotVO g = new GrindSlotVO();
            g.setSlotNumber(slot.getSlotNumber());
            g.setRune(slot.getRune());
            if (slot.getRune() != null) {
                double cur = curScores.getOrDefault(slot.getRune().getRuneId(), 0.0);
                g.setScore(cur);
                g.setPotentialScore(slot.getScore() == null ? cur : slot.getScore());
                // 下限目标 B_k = 推荐行该槽位符文的当前分
                RuneVO recRune = rec.slots().get(i).getRune();
                Double bK = recRune != null ? curScores.get(recRune.getRuneId()) : null;
                g.setGrind(buildGrindAdvice(slot.getRune(), weights, bK, cur,
                        grindPlans.get(slot.getRune().getRuneId())));
            }
            grindSlots.add(g);
        }
        vo.setGrindSlots(grindSlots);
        vo.setGrindTotalScore(grind.total());
        vo.setWarning(warning);
        return vo;
    }

    @Override
    public RuneOptimizeInitVO analyzeScene(Long userId, RuneSceneRequest dto){
        if (dto == null || dto.getDragonId() == null) {
            return buildInit(RuneConstants.emptyWeights(), "请选择一条龙");
        }
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dto.getDragonId());
        Dragons dragon = dragonMapper.selectOne(wrapper);
        if (dragon == null) {
            return buildInit(RuneConstants.emptyWeights(), "当前账号不存在这条龙，请重试");
        }
        String sceneText = dto.getScene();
        RuneOptimizeInitVO vo = aiService.analyzeScene(dragon, dto.getWeights(), sceneText);
        if (vo == null) {
            Map<String, Integer> weights = RuneConstants.baseWeights(dragon.getCategory());
            return buildInit(weights, "AI分析失败，使用类别基础权重（" + dragon.getCategory() + "）");
        }
        return vo;
    }

    /**
     * 套组感知的全局最优搭配
     * 先计算单个符文的分数，并按照在不考虑组合加成的条件下进行分数累加
     * 随后在末尾统一加分，求出分数最大组合
     * 最后从后往前找到符合要求的最优符文
     * @param candidates 已有符文候选池（includeOthersEquipped决定是否选择被其他角色装备的符文）
     * @param weights 当前角色的各属性需求权重
     * @param curScores 每件符文当前分数
     * @param grindPlans null → 当前推荐行；非 null → 推荐研磨
     * @return 最优符文搭配
     */
    private BestResult solve(List<RuneVO> candidates, Map<String, Integer> weights,
                              Map<Long, Double> curScores, Map<Long, GrindPlan> grindPlans) {

        // 1. 每槽 × 每套组 → 6×13 → 筛选出该组合下单独分最高的符文
        List<String> setTypes = new ArrayList<>(RuneConstants.SET_STACK.keySet());
        Map<String, Integer> setIdxMap = new HashMap<>();
        int[] numIdxOfSet = new int[setTypes.size()];
        for (int i = 0; i < setTypes.size(); i++) {
            setIdxMap.put(setTypes.get(i), i);
            // 该套组是否属于 6 个数值套组。非数值套组不进状态向量
            numIdxOfSet[i] = RuneConstants.NUMERICAL_SETS.indexOf(setTypes.get(i));
        }

        // 按照槽位 slot 分组
        List<RuneVO>[] bySlot = new List[6];
        for (int i = 0; i < 6; i++) {
            bySlot[i] = new ArrayList<>();
        }
        for (RuneVO r : candidates) {
            Integer slot = r.getSlotNumber();
            if (slot == null || slot < 1 || slot > 6) { continue; }
            // 主属性非法：防止手动输入异常符文
            if (r.getMainStatType() == null || !RuneConstants.isMainStatAllowed(slot, r.getMainStatType())) { continue; }
            bySlot[slot - 1].add(r);
        }

        RuneVO[][] best = new RuneVO[6][setTypes.size()];
        for (int slot = 0; slot < 6; slot++) {
            for (RuneVO r : bySlot[slot]) {
                Integer setIdx = setIdxMap.get(r.getSetType());
                if (setIdx == null) { continue; }
                double s = scoreFor(r.getRuneId(), curScores, grindPlans);
                RuneVO cur = best[slot][setIdx];
                if (cur == null || s > scoreFor(cur.getRuneId(), curScores, grindPlans)) {
                    best[slot][setIdx] = r;
                }
            }
        }

        // 2.DP 逐槽推进
        /*
          ·dp数组长度：262144 = 2^18，通过18位数字表示每种数值符文各存储了几个
          ·dp[i] 则用于存储这些符文的总分之和，仅考虑单件分数，不考虑套组激活额外分数和对应槽位
           例1：000 000 000 000 000 110 = 6，按照 SET_BASE 的内容可以看出，当前一共有 6 个体力符文
                dp[6] = 6个体力符文的分数总和
           例2：000 000 000 000 001 100 = 12，按照 SET_BASE 的内容可以看出，当前一共有 4 个体力符文 + 1 个机会符文
                由于当前一共就 5 个符文，但一个角色可塞入 6 个符文，因此，如果其他位置存在可塞入非数值符文，则塞入
                实际上共 2 种可能：4 个体力符文 + 1 个机会符文 + 1 空槽 /  4 个体力符文 + 1 个机会符文 + 1 个非数值符文
                dp[12] = 4个体力符文 + 1个机会符文的分数总和（ + 1 个非数值符文分数总和）
                此时，哪种组合的分数更高，哪种组合留下
          ·每 3 个一组的数必须满足总和加起来小于等于 6，满足这种要求的下标实际共 924 个
           例：000 000 000 000 110 110 = 54，一共有 6 个体力符文 + 6 个机会符文 = 12 个符文
                ∵ 12 > 6，超过可放置数量 → 该位置实际上不可放置符文，dp[54] = -∞
         */
        double[] dp = new double[MAX_SET_KEY];
        /*
          ·prev：通过类似 0-1 背包的方式，用于记录在某个时刻、某个组合情况下，某个槽位的符文类型
          ·prev[a][b] 存储的内容为 a 槽位、b 组合情况下的套组下标。此外：-1 = 未设置，-2 = 空槽
          ·prev数组大小为 6 * 262144，其中：
          （1）6 表示 6 个槽位，从低到高一一对应，如 0 对应的是 (0+1) 号槽位，即 1 号槽位，以此类推
          （2）262144 表示编码的取值范围，与 dp 数组长度对应。与 dp 的存储情况类似：在 prev[5][i]，至多有 924 个内容存储着有效的套组信息
          ·prev[5][i]：这行存储着符文套组的最终状态。对 i 进行二进制展开，可以得到每个数值套组分别有多少数量
           从下往上从 6 号槽位开始逐步遍历，即可获得所有槽位存储的符文类型，进而通过 best 数组来找到对应的符文
           例：prev[5][12] → 000 000 000 000 001 100 = 12 → 4 个体力符文 + 1 个机会符文（+ 1 个非数值符文），前者对应的数字为 1，后者对应的数字为 8
               若 prev[5][12] = k，k =（int类型）SET_TYPE("体力")，则表明
               （1）在 6 号位存储的是体力符文，对应的自定义数字编码为 1
               （2）此时，可在 best[5][k]找到目标符文，这是该套组的一个需求符文，占据了 6 号槽位
               （3）12 -（体力编码）1 = 11 → 还有 3 个体力符文 + 1 个机会符文（+ 1 个非数值符文）→ 需要从 prev[4][11] 开始寻找前面的槽位内容
               （4）根据上面的信息可知：实际上，prev的右上三角部分并不存储有效信息
         */
        int[][] prev = new int[6][MAX_SET_KEY];
        Arrays.fill(dp, Double.NEGATIVE_INFINITY);
        // 空槽保底：不装任何符文，分数为 0
        dp[0] = 0;

        // 从第 1 个槽位开始遍历到第 6 个槽位 → 从 prev 获取符文的时候需要倒序
        for (int slot = 0; slot < 6; slot++) {
            double[] ndp = new double[MAX_SET_KEY];
            // 记录当前槽位符文的类型
            int[] np = new int[MAX_SET_KEY];
            Arrays.fill(ndp, Double.NEGATIVE_INFINITY);
            Arrays.fill(np, -1);
            // 记录当前槽、当前组合下符文的类型
            for (int key = 0; key < MAX_SET_KEY; key++) {
                if (dp[key] == Double.NEGATIVE_INFINITY) { continue; }
                // 空槽保底：不装任何符文，分数不变，保证任何状态都能继续推进
                if (dp[key] > ndp[key]) {
                    ndp[key] = dp[key];
                    np[key] = -2;
                }
                for (int t = 0; t < setTypes.size(); t++) {
                    RuneVO r = best[slot][t];
                    if (r == null) { continue; }
                    int newKey;
                    if (numIdxOfSet[t] >= 0) {
                        // 数值套组：件数 +1（最多 6 件）
                        int count = (key / SET_BASE[numIdxOfSet[t]]) % 8;
                        if (count >= 6) { continue; }
                        newKey = key + SET_BASE[numIdxOfSet[t]];
                    } else {
                        // 非数值套组（吸血/冥想等）：不进状态向量
                        newKey = key;
                    }
                    double ns = dp[key] + scoreFor(r.getRuneId(), curScores, grindPlans);
                    if (ns > ndp[newKey]) {
                        ndp[newKey] = ns;
                        np[newKey] = t;
                    }
                }
            }
            dp = ndp;
            prev[slot] = np;
        }

        // 3.添加套组加成，取全局最优
        int bestKey = -1;
        double bestTotal = Double.NEGATIVE_INFINITY;
        for (int key = 0; key < MAX_SET_KEY; key++) {
            if (dp[key] == Double.NEGATIVE_INFINITY) { continue; }
            int[] counts = new int[6];
            for (int i = 0; i < 6; i++) {
                counts[i] = (key / SET_BASE[i]) % 8;
            }
            double total = dp[key] + RuneConstants.setBonusScore(weights, counts);
            if (total > bestTotal) {
                bestTotal = total;
                bestKey = key;
            }
        }

        // 4.回溯，从末态往前还原每槽选择
        RuneVO[] chosen = new RuneVO[6];
        if (bestKey >= 0) {
            int key = bestKey;
            for (int slot = 5; slot >= 0; slot--) {
                int t = prev[slot][key];
                // 空槽或未定义
                if (t < 0) { continue; }
                chosen[slot] = best[slot][t];
                if (numIdxOfSet[t] >= 0) {
                    key -= SET_BASE[numIdxOfSet[t]];
                }
            }
        }

        // 5.组装 6 槽
        List<RuneOptimizeSlotVO> slots = new ArrayList<>();
        for (int slot = 1; slot <= 6; slot++) {
            RuneOptimizeSlotVO vo = new RuneOptimizeSlotVO();
            vo.setSlotNumber(slot);
            RuneVO r = chosen[slot - 1];
            if (r != null) {
                vo.setRune(r);
                vo.setScore(scoreFor(r.getRuneId(), curScores, grindPlans));
            }
            slots.add(vo);
        }
        return new BestResult(slots, bestKey < 0 ? 0.0 : bestTotal);
    }

    // 读该 rune_set_reqs，将json解析成 “套组 → 件数下限”
    private Map<String, Integer> dragonFloors(Long userId, Long dragonId) {
        if (dragonId == null) { return Map.of(); }
        Dragons dragon = dragonMapper.selectOne(new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId));
        if (dragon == null) { return Map.of(); }
        return parseRuneSetFloors(dragon.getRuneSetReqs());
    }

    // 提取用户选择的套组，并以 Map 存储套组+件数
    private Map<String, Integer> parseRuneSetFloors(String raw) {
        if (raw == null || raw.isBlank()) { return Map.of(); }
        try {
            // 转换为节点对象，便于进行遍历等操作
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || !root.isObject()) { return Map.of(); }
            Map<String, Integer> floors = new LinkedHashMap<>();
            // 迭代器对用户选择套组进行遍历
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> en = it.next();
                String set = en.getKey();
                // 白名单符文套组对应数量检查
                List<Integer> tiers = RuneConstants.SET_REQ_TIERS.get(set);
                JsonNode v = en.getValue();
                if (tiers == null || v == null || !v.isIntegralNumber()) { continue; }
                // 提供的套组数量确认为整数后，转为 int
                int c = v.asInt();
                if (tiers.contains(c)) { floors.put(set, c); }
            }
            if (floors.values().stream().mapToInt(Integer::intValue).sum() > 6) {
                return Map.of();
            }
            return floors;
        } catch (Exception e) {
            return Map.of();
        }
    }


    // =================================================================
    // [后端 1] 搭配选择（件数下限）接入求解
    // solve() 的 DP 状态只对 6 个数值套组计件；非数值套组（生存/吸血/冥想…）放一件
    // 后 DP 看不见。若用户在 DragonForm 选的下限含非数值套组，就必须把它也计件强制。
    //   状态向量 = 6 数值维（key 低 6 位，base-8，顺序对齐 NUMERICAL_SETS）
    //            ∪ 有下限的非数值套组（自第 6 位起各占一维）—— 总维数 ≤ 6+3=9
    //   key 统一按 8^i 编码成 long；稀疏 Map 只存可达态（≤ C(15,9)=5005，毫秒级），
    //   不需要 solve() 那 2^18 的稠密数组。
    //   终态先过「每根有下限的维 count≥floor」的门，再叠 setBonusScore 取最大；
    //   无任何达标态 → 返回 null，由 optimize() 放宽回无要求最优并附 warning。
    // =================================================================


    // todo 1. 对函数进行理解
    // todo 2. 重复代码整理与提取
    // todo 3. 放宽兜底机制：若用户选择的套组仅能满足部分，则将能满足的部分加入，其余部分采用兜底机制
    // 件数下限强制的全局最优（只在 floors 非空时被调用；下限都是 ≥ 的硬约束）
    private BestResult solveWithFloors(List<RuneVO> candidates, Map<String, Integer> weights,
                                        Map<Long, Double> curScores, Map<Long, GrindPlan> grindPlans,
                                        Map<String, Integer> floors) {
        List<String> setTypes = new ArrayList<>(RuneConstants.SET_STACK.keySet());
        Map<String, Integer> setIdxMap = new HashMap<>();
        for (int i = 0; i < setTypes.size(); i++) {
            setIdxMap.put(setTypes.get(i), i);
        }
        List<RuneVO>[] bySlot = new List[6];
        for (int i = 0; i < 6; i++) {
            bySlot[i] = new ArrayList<>();
        }
        for (RuneVO r : candidates) {
            Integer slot = r.getSlotNumber();
            if (slot == null || slot < 1 || slot > 6) { continue; }
            if (r.getMainStatType() == null || !RuneConstants.isMainStatAllowed(slot, r.getMainStatType())) { continue; }
            bySlot[slot - 1].add(r);
        }
        RuneVO[][] best = new RuneVO[6][setTypes.size()];
        for (int slot = 0; slot < 6; slot++) {
            for (RuneVO r : bySlot[slot]) {
                Integer setIdx = setIdxMap.get(r.getSetType());
                if (setIdx == null) { continue; }
                double s = scoreFor(r.getRuneId(), curScores, grindPlans);
                RuneVO cur = best[slot][setIdx];
                if (cur == null || s > scoreFor(cur.getRuneId(), curScores, grindPlans)) {
                    best[slot][setIdx] = r;
                }
            }
        }

        // 相较于 solve 增加了新的维度：如果用户选择了 1/2/3 套非数值符文，则维度分别变为 7/8/9
        Map<String, Integer> dimOfSet = new HashMap<>();
        for (int i = 0; i < RuneConstants.NUMERICAL_SETS.size(); i++) {
            dimOfSet.put(RuneConstants.NUMERICAL_SETS.get(i), i);
        }
        List<String> extraSets = floors.keySet().stream()
                .filter(s -> !RuneConstants.NUMERICAL_SETS.contains(s))
                .sorted().toList();
        for (int j = 0; j < extraSets.size(); j++) {
            dimOfSet.put(extraSets.get(j), 6 + j);
        }
        int dims = 6 + extraSets.size();
        // 参照原有的 SET_BASE 进行位数扩展，用于表示某个符文套组要求的符文下限
        // weight[i] = (111)b ^ i
        long[] weight = new long[dims];
        for (int i = 0; i < dims; i++) {
            weight[i] = 1L << (3 * i);
        }
        int[] floorOfDim = new int[dims];
        floors.forEach((set, c) -> floorOfDim[dimOfSet.get(set)] = c);

        Map<Long, Double> dp = new LinkedHashMap<>();
        dp.put(0L, 0.0);
        List<Map<Long, Integer>> prev = new ArrayList<>(6);
        for (int slot = 0; slot < 6; slot++) {
            Map<Long, Double> ndp = new LinkedHashMap<>();
            Map<Long, Integer> np = new LinkedHashMap<>();
            for (Map.Entry<Long, Double> e : dp.entrySet()) {
                long key = e.getKey();
                double base = e.getValue();
                // 空槽保底
                if (base > ndp.getOrDefault(key, Double.NEGATIVE_INFINITY)) {
                    ndp.put(key, base);
                    np.put(key, -2);
                }
                for (int t = 0; t < setTypes.size(); t++) {
                    RuneVO r = best[slot][t];
                    if (r == null) { continue; }
                    int dim = dimOfSet.getOrDefault(setTypes.get(t), -1);
                    long newKey;
                    if (dim >= 0) {
                        // 数值套组 / 有下限的非数值套组：该维 +1
                        int digit = (int) ((key / weight[dim]) % 8);
                        if (digit >= 6) { continue; }
                        newKey = key + weight[dim];
                    } else {
                        // 无数值加成、且非选中套组不计件
                        newKey = key;
                    }
                    double ns = base + scoreFor(r.getRuneId(), curScores, grindPlans);
                    if (ns > ndp.getOrDefault(newKey, Double.NEGATIVE_INFINITY)) {
                        ndp.put(newKey, ns);
                        np.put(newKey, t);
                    }
                }
            }
            dp = ndp;
            prev.add(np);
        }

        long bestKey = -1;
        double bestTotal = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Long, Double> e : dp.entrySet()) {
            long key = e.getKey();
            boolean meet = true;
            for (int d = 0; d < dims; d++) {
                if (floorOfDim[d] > 0 && (int) ((key / weight[d]) % 8) < floorOfDim[d]) {
                    meet = false;
                    break;
                }
            }
            if (!meet) { continue; }
            int[] counts = new int[6];
            for (int i = 0; i < 6; i++) {
                counts[i] = (int) ((key / weight[i]) % 8);
            }
            double total = e.getValue() + RuneConstants.setBonusScore(weights, counts);
            if (total > bestTotal) {
                bestTotal = total;
                bestKey = key;
            }
        }
        // 候选凑不齐下限 → 调用方放宽回退
        if (bestKey < 0) {
            return null;
        }

        RuneVO[] chosen = new RuneVO[6];
        long key = bestKey;
        for (int slot = 5; slot >= 0; slot--) {
            Integer t = prev.get(slot).get(key);
            if (t == null || t == -2) { continue; }
            chosen[slot] = best[slot][t];
            int dim = dimOfSet.getOrDefault(setTypes.get(t), -1);
            if (dim >= 0) {
                // 减回该件占的那一维
                key -= weight[dim];
            }
        }

        // 组装 6 槽
        List<RuneOptimizeSlotVO> slots = new ArrayList<>();
        for (int slot = 1; slot <= 6; slot++) {
            RuneOptimizeSlotVO vo = new RuneOptimizeSlotVO();
            vo.setSlotNumber(slot);
            RuneVO r = chosen[slot - 1];
            if (r != null) {
                vo.setRune(r);
                vo.setScore(scoreFor(r.getRuneId(), curScores, grindPlans));
            }
            slots.add(vo);
        }
        return new BestResult(slots, bestTotal);
    }

    /**
     * 研磨潜在分数计算
     * 1.有词条已研磨：只能继续研磨已研磨词条 X，定位该词条
     * 2.未被研磨：在所有子词条里选「替换收益最高」的 X
     * 3.目标词条 Y 遵循唯一性：主/额/副词条已存在的不再可选，但当前研磨词条 X 除外。即 {Y} = {X} ∪ {全部词条 \ 已存在词条}
     * 4.潜在分 = 当前分 + max(0, 权重[Y]×10 − 权重[X]×数值(X))，按最大研磨值 10 计算
     * 5.未研磨且无正收益 → 不值得研磨，按当前分参与打分 → 返回 null
     */
    private GrindPlan potentialPlan(RuneVO rune, Map<String, Integer> weights, double curScore) {
        if (!isGrindAble(rune)) { return null; }
        List<RuneVO.SubStatsVO> subs = rune.getSubStats();
        if (subs == null || subs.isEmpty()) { return null; }

        String mainType = rune.getMainStatType();
        RuneVO.SubStatsVO grindSub = findGrindSub(rune);
        // 1+2
        RuneVO.SubStatsVO x = grindSub != null ? grindSub : pickBestX(subs, mainType, weights);
        if (x == null) { return null; }

        String y = pickBestY(subs, mainType, x.getStatType(), weights);
        if (y == null) { return null; }

        double xContrib = singleScore(x.getStatType(), x.getStatValue(), weights);
        double yContrib = singleScore(y, BigDecimal.TEN, weights);
        double gain = yContrib - xContrib;

        // 未研磨且无收益 → 不值得研磨
        if (grindSub == null && gain <= 0) { return null; }

        return new GrindPlan(curScore + Math.max(0, gain), x, y);
    }

    // 选择替换收益最高的子词条作为研磨目标 X
    private RuneVO.SubStatsVO pickBestX(List<RuneVO.SubStatsVO> subs, String mainType, Map<String, Integer> weights) {
        RuneVO.SubStatsVO best = null;
        double bestGain = Double.NEGATIVE_INFINITY;
        for (RuneVO.SubStatsVO s : subs) {
            String y = pickBestY(subs, mainType, s.getStatType(), weights);
            if (y == null) { continue; }
            double gain = singleScore(y, BigDecimal.TEN, weights) - singleScore(s.getStatType(), s.getStatValue(), weights);
            if (gain > bestGain) {
                bestGain = gain;
                best = s;
            }
        }
        return best;
    }

    // 按唯一性规则选目标词条 Y：{当前研磨词条 X} ∪ {全部词条 \ 已存在词条} 中权重最高者）
    private String pickBestY(List<RuneVO.SubStatsVO> subs, String mainType, String xStat, Map<String, Integer> weights) {
        Set<String> present = new HashSet<>();
        if (mainType != null) { present.add(mainType); }
        for (RuneVO.SubStatsVO s : subs) {
            if (s.getStatType() != null) { present.add(s.getStatType()); }
        }
        String best = null;
        int bestW = -1;
        for (String st : ALL_STAT_TYPES) {
            // 已存在且不是当前研磨词条 → 不可选
            if (present.contains(st) && !st.equals(xStat)) { continue; }
            Integer w = weights.get(st);
            int wv = w == null ? 0 : w;
            if (wv > bestW) {
                bestW = wv;
                best = st;
            }
        }
        return best;
    }

    // 定位已研磨的词条：grindSortOrder → sortOrder
    private RuneVO.SubStatsVO findGrindSub(RuneVO rune) {
        Long gid = rune.getGrindSortOrder();
        if (gid == null || rune.getSubStats() == null) { return null; }
        for (RuneVO.SubStatsVO s : rune.getSubStats()) {
            if (s.getSortOrder() != null && s.getSortOrder().equals(gid.intValue())) {
                return s;
            }
        }
        return null;
    }

    // 是否可研磨
    private boolean isGrindAble(RuneVO rune) {
        if (!Boolean.TRUE.equals(rune.getIsGrindable())) { return false; }
        if ("活动".equals(rune.getSetType())) { return false; }
        if (rune.getStarRating() != null && rune.getStarRating() >= 7) { return false; }
        return true;
    }

    // 研磨模式下取潜在分，推荐模式下取当前分
    private double scoreFor(Long runeId, Map<Long, Double> curScores, Map<Long, GrindPlan> grindPlans) {
        if (grindPlans == null) { return curScores.getOrDefault(runeId, 0.0); }
        GrindPlan plan = grindPlans.get(runeId);
        return plan != null ? plan.potentialScore() : curScores.getOrDefault(runeId, 0.0);
    }

    // 生成单个槽位的研磨建议（已研磨/未研磨可研磨/不可研磨）+ 下限值 v_min
    private GrindAdviceVO buildGrindAdvice(RuneVO rune, Map<String, Integer> weights, Double bK,
                                           double curScore, GrindPlan plan) {
        GrindAdviceVO adv = new GrindAdviceVO();
        adv.setGrindable(isGrindAble(rune));

        RuneVO.SubStatsVO groundSub = findGrindSub(rune);
        adv.setAlreadyGround(groundSub != null);
        if (groundSub != null) {
            adv.setGroundStatType(groundSub.getStatType());
            adv.setGroundStatValue(groundSub.getStatValue());
        }

        // 不可研磨：只给说明
        if (plan == null) {
            if ("活动".equals(rune.getSetType())) {
                adv.setMessage("活动符文不可研磨");
            } else if (rune.getStarRating() != null && rune.getStarRating() >= 7) {
                adv.setMessage("7星符文研磨成本过高，暂不考虑研磨");
            } else if (!Boolean.TRUE.equals(rune.getIsGrindable())) {
                adv.setMessage("强化等级不足");
            } else {
                adv.setMessage("暂无权重高于当前词条的研磨目标，不建议研磨");
            }
            return adv;
        }

        adv.setTargetStat(plan.targetStat());
        Integer wY = weights.get(plan.targetStat());
        double xContrib = singleScore(plan.targetSub().getStatType(), plan.targetSub().getStatValue(), weights);

        // 下限值：v_min = ceil((B_k − S_基础) / 权重[Y])，S_基础 = 当前分 − 权重[X]×数值(X)
        if (bK != null && wY != null && wY > 0) {
            double sBase = curScore - xContrib;
            double need = (bK - sBase) / wY;
            int vMin = (int) Math.ceil(need);
            // 研磨值范围 7-10
            vMin = Math.max(7, Math.min(10, vMin));
            adv.setMinValue(new BigDecimal(vMin));

            boolean overMax = need > 10; // 即使磨满10%也不及推荐行
            if (Boolean.TRUE.equals(adv.getAlreadyGround())) {
                if (plan.targetStat().equals(adv.getGroundStatType())) {
                    adv.setMessage(String.format("已研磨【%s】%s%%；词条数值大于 %d%% 即可达标",
                            plan.targetStat(), formatValue(adv.getGroundStatValue()), vMin));
                } else {
                    adv.setMessage(String.format("已研磨【%s】，可再研磨；目标词条【%s】，词条数值大于 %d%% 即可",
                            adv.getGroundStatType(), plan.targetStat(), vMin));
                }
            } else {
                adv.setMessage(String.format("建议研磨【%s(%s%%)】→【%s】，词条数值大于 %d%% 即可",
                        plan.targetSub().getStatType(), formatValue(plan.targetSub().getStatValue()),
                        plan.targetStat(), vMin));
            }
            if (overMax) {
                adv.setMessage(adv.getMessage() + "；注：即使磨满10%%也不及推荐行（套组入选除外）");
            }
        } else if (Boolean.TRUE.equals(adv.getAlreadyGround())) {
            // 无推荐分参照：只报已研磨词条与当前值
            adv.setMessage(String.format("已研磨【%s】%s%%，可继续研磨提升数值",
                    adv.getGroundStatType(), formatValue(adv.getGroundStatValue())));
        } else {
            // 无推荐分参照：只报研磨目标
            adv.setMessage(String.format("建议研磨【%s(%s%%)】→【%s】",
                    plan.targetSub().getStatType(), formatValue(plan.targetSub().getStatValue()),
                    plan.targetStat()));
        }
        return adv;
    }

    // 7.00 → "7"
    private String formatValue(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    // 单个符文分数计算：Σ 权重[词条]×数值
    private double scoreRune(RuneVO rune, Map<String, Integer> weights){
        double mainScore = singleScore(rune.getMainStatType(), rune.getMainStatValue(), weights);
        double subScore = 0;
        if(rune.getSubStats() != null) {
            for (RuneVO.SubStatsVO subStat : rune.getSubStats()) {
                subScore += singleScore(subStat.getStatType(), subStat.getStatValue(), weights);
            }
        }
        return mainScore + subScore;
    }

    // 获取单一词条分数
    private double singleScore(String statType, BigDecimal statValue, Map<String, Integer> weights) {
        if(statType == null || statValue == null) {
            return 0;
        }
        Integer w = weights.get(statType);
        return w == null ? 0 : w * statValue.doubleValue();
    }


    // 构建统一返回结构
    private RuneOptimizeInitVO buildInit(Map<String, Integer> weights, String rationale) {
        RuneOptimizeInitVO vo = new RuneOptimizeInitVO();
        vo.setWeights(weights);
        vo.setRationale(rationale);
        return vo;
    }

    // AiAnalysis拆分解析
    private RuneOptimizeInitVO parseAiAnalysis(String aiAnalysis) {
        try{
            RuneOptimizeInitVO vo = objectMapper.readValue(aiAnalysis, RuneOptimizeInitVO.class);
            if (vo.getWeights() == null || vo.getWeights().isEmpty()) {
                return null;
            }
            return vo;
        } catch (Exception e) {
            return null;
        }
    }
}
