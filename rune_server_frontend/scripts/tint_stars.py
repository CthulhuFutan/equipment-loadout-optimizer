# -*- coding: utf-8 -*-
"""
把 runes/star/rune_star_01~07.png 按品质色重新着色，
保留原图亮度(明暗渐变/高光)，仅替换色相+饱和度。
生成到 runes/star/colored/rune_star_0{n}_{color}.png
"""
from PIL import Image
import colorsys, os

# 品质色（与 constants.ts 的 RUNE_QUALITY_COLORS 一致）
COLORS = {
    'gray':   (0x35, 0x35, 0x35),  # 灰色
    'blue':   (0x11, 0x22, 0x4E),  # 蓝色
    'purple': (0x53, 0x18, 0x6C),  # 紫色
    'orange': (0xF3, 0xB5, 0x3E),  # 橙色（含「特殊」）
}

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(BASE, '..', 'public', 'runes', 'star')
DST = os.path.join(SRC, 'colored')
os.makedirs(DST, exist_ok=True)

for i in range(1, 8):
    src_path = os.path.join(SRC, f'rune_star_0{i}.png')
    src = Image.open(src_path).convert('RGBA')
    for key, (r, g, b) in COLORS.items():
        h_t, l_t, s_t = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
        out = Image.new('RGBA', src.size, (0, 0, 0, 0))
        ps = src.load()
        po = out.load()
        for y in range(src.height):
            for x in range(src.width):
                pr, pg, pb, pa = ps[x, y]
                if pa == 0:
                    continue
                h_p, l_p, s_p = colorsys.rgb_to_hls(pr / 255.0, pg / 255.0, pb / 255.0)
                nr, ng, nb = colorsys.hls_to_rgb(h_t, l_p, s_t)  # 保留原亮度 l_p
                po[x, y] = (int(round(nr * 255)), int(round(ng * 255)), int(round(nb * 255)), pa)
        out.save(os.path.join(DST, f'rune_star_0{i}_{key}.png'))
    print(f'  star0{i} done')

print('OK ->', DST)
