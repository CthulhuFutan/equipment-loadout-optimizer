package com.dvm.rune_server_backend.service.impl;

import com.dvm.rune_server_backend.common.RuneConstants;
import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.model.vo.RuneOptimizeInitVO;
import com.dvm.rune_server_backend.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 25826
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek")
public class AiServiceImpl implements AiService {

    private final RestTemplate aiRestTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${ai.skill-location}")
    private String skillLocation;
    @Value("${deepseek.api-key:}")
    private String apiKey;
    @Value("${deepseek.base-url}")
    private String baseUrl;
    @Value("${deepseek.model}")
    private String model;

    private static final String ANTHROPIC_PATH = "/anthropic";

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if(!isAvailable()){
            log.warn("当前AI分析不可使用，请检查配置是否正确");
            return null;
        }
        try{
            // anthropic相关 AI 兼容
            if (baseUrl.endsWith(ANTHROPIC_PATH)) {
                return callAnthropic(systemPrompt, userPrompt);
            }
            return callOpenAi(systemPrompt, userPrompt);
        } catch (Exception e){
            log.error("AI调用失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public RuneOptimizeInitVO analyzeScene(Dragons dragon, Map<String, Integer> currentWeights, String sceneText) {
        if (dragon == null) {
            return null;
        }
        String skillRules = loadSkillRules();
        String raw = chat(buildSystemPrompt(skillRules), buildUserPrompt(dragon, currentWeights, sceneText));
        if (raw == null) {
            return null;
        }
        return parseAndClean(raw);
    }

    // Anthropic 兼容
    private String callAnthropic(String systemPrompt, String userPrompt) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 2048);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", 0.4);
        body.put("thinking", Map.of("type", "disabled"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        String response = aiRestTemplate.postForObject(baseUrl + "/v1/messages", new HttpEntity<>(body, headers), String.class);
        JsonNode text = objectMapper.readTree(response).path("content").path(0).path("text");
        return (text.isMissingNode() || text.isNull()) ? null : text.asText();
    }

    // OpenAI 兼容
    private String callOpenAi(String systemPrompt, String userPrompt) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", 0.4);
        body.put("max_tokens", 2048);
        body.put("response_format", Map.of("type", "json_object"));
        // SKILL.md回答模板 → 使用非思考模式
        body.put("thinking", Map.of("type", "disabled"));

        String response = aiRestTemplate.postForObject(baseUrl + "/chat/completions", new HttpEntity<>(body, authHeaders()), String.class);
        JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
        return (content.isMissingNode() || content.isNull()) ? null : content.asText();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    // 获取skill.md有效信息，便于让AI在后继通过该规则格式仿照进行分析
    private String loadSkillRules() {
        try {
            Resource resource = resourceLoader.getResource(skillLocation);
            // 若skillLocation的指向位置存在异常则可重定位兜底
            if (!resource.exists()) {
                log.warn("skill 文件不存在: {}，回退 classpath:ai/skill.md", skillLocation);
                resource = resourceLoader.getResource("classpath:ai/skill.md");
            }
            return stripFrontMatter(resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("读取 skill 文件失败: {}", e.getMessage());
            return "";
        }
    }

    private String stripFrontMatter(String text) {
        if (text == null) {
            return "";
        }
        String body = text.replaceFirst("^\uFEFF", "").strip();
        if (!body.startsWith("---")) {
            log.warn("skill 文件不符合标准格式，按全文返回");
            return body;
        }
        int end = body.indexOf("\n---", 3);
        if (end < 0) {
            log.warn("skill 文件缺少收尾 ---，按全文返回");
            return body;
        }
        return body.substring(end + 4).stripLeading();
    }

    // system prompt
    private String buildSystemPrompt(String skillRules) {
        return "你是游戏 DVM 的符文搭配分析专家，请严格遵循以下规则分析。\n\n"
                + "【分析规则（必读）】\n" + skillRules + "\n\n"
                + "【可用词条白名单】weights 的 key 只能取以下 10 个之一，禁止自创：\n"
                + String.join("、", RuneConstants.PERCENT_STATS) + "\n\n"
                + "【输出格式（严格）】只输出一个 JSON 对象，禁止解释文字、禁止 markdown 围栏：\n"
                + "以下【仅为格式示例，数值不代表分析结论】：\n"
                + "{\"weights\":{\"攻击力%\":8,\"致命率%\":8,\"命中%\":7},\"rationale\":\"一句话说明\"}\n"
                + "要求：\n"
                + "- weights：严格按【分析规则】根据当前龙的技能/类别/场景独立得出。\n"
                + "  value 为 0-10 的整数，0=无关；与场景冲突时，场景优先。\n"
                + "  攻击龙侧重攻击词条；防御龙侧重防御力%、生命力%（输出词条置 0）；\n"
                + "  辅助/治疗龙同理按其定位侧重。\n"
                + "- rationale：中文，不超过 100 字。";
    }

    // user prompt
    private String buildUserPrompt(Dragons dragon, Map<String, Integer> currentWeights, String sceneText) {
        StringBuilder sb = new StringBuilder();
        sb.append("【龙的信息】\n");
        sb.append("名称：").append(nvl(dragon.getDragonName())).append("\n");
        sb.append("属性：").append(nvl(dragon.getElement())).append("\n");
        sb.append("类别：").append(nvl(dragon.getCategory())).append("\n");
        appendSkill(sb, "队长技能", dragon.getLeaderSkillName(), dragon.getLeaderSkillDesc());
        appendSkill(sb, "主动技能", dragon.getActiveSkillName(), dragon.getActiveSkillDesc());
        appendSkill(sb, "被动技能1", dragon.getPassiveSkill1Name(), dragon.getPassiveSkill1Desc());
        appendSkill(sb, "被动技能2", dragon.getPassiveSkill2Name(), dragon.getPassiveSkill2Desc());
        appendSkill(sb, "被动技能3", dragon.getPassiveSkill3Name(), dragon.getPassiveSkill3Desc());
        appendSkill(sb, "宝物", dragon.getTreasureName(), dragon.getTreasureDesc());
        if (currentWeights != null && !currentWeights.isEmpty()) {
            sb.append("当前滑块参考权重：").append(currentWeights).append("\n");
        }
        if (hasText(sceneText)) {
            sb.append("【当前场景需求】\n").append(sceneText).append("\n");
        }
        return sb.toString();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void appendSkill(StringBuilder sb, String prefix, String name, String desc) {
        if (!hasText(name)) return;
        sb.append(prefix).append("：").append(name);
        if (hasText(desc)) {
            sb.append("——").append(desc);
        }
        sb.append("\n");
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // JSON字段格式解析，同时进行补0、拒收等操作
    private RuneOptimizeInitVO parseAndClean(String raw) {
        try {
            String json = stripJsonFences(raw);
            RuneOptimizeInitVO vo = objectMapper.readValue(json, RuneOptimizeInitVO.class);
            // 置0
            Map<String, Integer> cleaned = RuneConstants.emptyWeights();
            if (vo.getWeights() != null) {
                for (String stat : RuneConstants.PERCENT_STATS) {
                    Integer w = vo.getWeights().get(stat);
                    if (w != null) {
                        cleaned.put(stat, Math.max(0, Math.min(10, w)));
                    }
                }
            }
            if (cleaned.values().stream().allMatch(v -> v == 0)) {
                log.warn("AI 返回全 0 权重，视为无效");
                return null;
            }
            vo.setWeights(cleaned);
            if (vo.getRationale() == null) {
                vo.setRationale("");
            }
            return vo;
        } catch (Exception e) {
            log.warn("AI 返回内容解析失败，返回 null 走兜底: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对于AI可能返回的如下格式进行处理
     * ```json
     * {"weights":{...}}
     * ```
     */
    private String stripJsonFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) {
                s = s.substring(nl + 1);
                int last = s.lastIndexOf("```");
                if (last >= 0) {
                    s = s.substring(0, last);
                }
                s = s.trim();
            }
        }
        return s;
    }
}
