CREATE DATABASE IF NOT EXISTS dvm_rune
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE dvm_rune;

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密结果',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS dragons (
    dragon_id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    dragon_name           VARCHAR(100) NOT NULL COMMENT '龙名称',
    dragon_image	  VARCHAR(300) COMMENT '龙图片路径，可为空',
    element               ENUM('火','水','地','光','暗') NOT NULL COMMENT '属性：火/水/地/光/暗',
    category              ENUM('攻击','防御','辅助','治疗') NOT NULL COMMENT '类别：攻击/防御/辅助/治疗',
    leader_skill_name     VARCHAR(100) COMMENT '队长技能',
    leader_skill_icon     VARCHAR(300) COMMENT '队长技能图标路径',
    leader_skill_desc     TEXT         COMMENT '队长技能描述',
    active_skill_name     VARCHAR(100) COMMENT '主动技能名',
    active_skill_icon     VARCHAR(300) COMMENT '主动技能图标路径',
    active_skill_desc     TEXT         COMMENT '主动技能描述',
    mana_cost             TINYINT      DEFAULT 1 COMMENT '耗蓝量 1-3',
    passive_skill_1_name  VARCHAR(100) COMMENT '被动技能1',
    passive_skill_1_icon  VARCHAR(300) COMMENT '被动技能1图标路径',
    passive_skill_1_desc  TEXT         COMMENT '被动技能1描述',
    passive_skill_2_name  VARCHAR(100) COMMENT '被动技能2',
    passive_skill_2_icon  VARCHAR(300) COMMENT '被动技能2图标路径',
    passive_skill_2_desc  TEXT         COMMENT '被动技能2描述',
    passive_skill_3_name  VARCHAR(100) COMMENT '被动技能3',
    passive_skill_3_icon  VARCHAR(300) COMMENT '被动技能3图标路径',
    passive_skill_3_desc  TEXT         COMMENT '被动技能3描述',
    treasure_name         VARCHAR(100) COMMENT '宝物名称',
    treasure_icon         VARCHAR(300) COMMENT '宝物图标路径',
    treasure_desc         TEXT         COMMENT '宝物增益描述',
    ai_analysis           JSON         COMMENT 'AI技能分析结果',
    rune_set_reqs         JSON         COMMENT '搭配选择（各套组件数需求下限）：{"生存":2,"体力":4}；NULL=无要求',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_category (user_id, category),
    INDEX idx_user_element (user_id, element)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='龙表';

CREATE TABLE IF NOT EXISTS runes (
    rune_id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    image_url	     VARCHAR(300)  COMMENT '符文对应图片地址',
    slot_number      TINYINT       NOT NULL COMMENT '槽位 1-6',
    rune_name        VARCHAR(50)   NULL COMMENT '符文名称（活动符文专用）',
    set_type         ENUM('活动','体力','机会','坚固','强烈','伤害','快速','吸血','冥想','反击','生存','决心','仇恨') NOT NULL COMMENT '套装类型',
    quality          ENUM('橙色','紫色','蓝色','灰色') NOT NULL COMMENT '品质',
    star_rating      TINYINT       DEFAULT 6 COMMENT '星级 6或7',
    levels           TINYINT       DEFAULT 0 COMMENT '强化等级 0-15',
    main_stat_type   VARCHAR(20)   NOT NULL COMMENT '主属性类型',
    main_stat_value  DECIMAL(10,2) NOT NULL COMMENT '主属性数值',
    is_grindable     BOOLEAN       GENERATED ALWAYS AS (levels >= 12) STORED COMMENT '是否可研磨（≥12级自动为TRUE）',
    grind_sort_order TINYINT       NULL COMMENT '被研磨强化的sort_order，NULL=未研磨',
    locked           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '锁定：1=不可被装备/卸下',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_slot (user_id, slot_number),
    INDEX idx_user_set_type (user_id, set_type),
    INDEX idx_user_quality (user_id, quality),
    INDEX idx_user_main (user_id, main_stat_type)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='符文表';

CREATE TABLE IF NOT EXISTS rune_sub_stats (
    rune_sub_id BIGINT        AUTO_INCREMENT PRIMARY KEY,
    rune_id     BIGINT        NOT NULL,
    stat_type   VARCHAR(20)   NOT NULL COMMENT '属性类型',
    stat_value  DECIMAL(10,2) NOT NULL COMMENT '属性数值',
    sort_order  TINYINT       DEFAULT 1 COMMENT '排序序号: 0=额外词条, 1-4=子词条',
    FOREIGN KEY (rune_id) REFERENCES runes(rune_id) ON DELETE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='符文副属性表';

CREATE TABLE IF NOT EXISTS dragon_equipment (
    equip_id    BIGINT  AUTO_INCREMENT PRIMARY KEY,
    dragon_id   BIGINT  NOT NULL,
    rune_id     BIGINT  NOT NULL,
    slot_number TINYINT NOT NULL COMMENT '槽位 1-6',

    FOREIGN KEY (dragon_id) REFERENCES dragons(dragon_id) ON DELETE CASCADE,
    FOREIGN KEY (rune_id)   REFERENCES runes(rune_id)   ON DELETE CASCADE,
    UNIQUE KEY uk_rune_id (rune_id),
    UNIQUE KEY uk_dragon_slot (dragon_id, slot_number)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='装备关系表';