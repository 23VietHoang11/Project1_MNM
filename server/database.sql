-- ====================================================================
-- DATABASE SCHEMA: VIGIL RPG FITNESS
-- MySQL / MariaDB compatible
-- ====================================================================

CREATE DATABASE IF NOT EXISTS vigil_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vigil_db;

-- 1. Bảng người dùng (Users / Heroes)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE DEFAULT 'Hachimi',
    password VARCHAR(255) NOT NULL DEFAULT '123456',
    avatar VARCHAR(20) NOT NULL DEFAULT '🧑‍🎤',
    gold INT NOT NULL DEFAULT 100,
    gems INT NOT NULL DEFAULT 10,
    xp INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    streak INT NOT NULL DEFAULT 0,
    total_reps INT NOT NULL DEFAULT 0,
    stage INT NOT NULL DEFAULT 1,
    last_workout_date VARCHAR(20) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Bảng vật phẩm (Items trong Shop & Drop Table)
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slot ENUM('HELMET', 'ARMOR', 'GLOVES', 'BOOTS', 'AMULET', 'WEAPON') NOT NULL,
    rarity ENUM('COMMON', 'RARE', 'EPIC', 'LEGENDARY') NOT NULL,
    price_gold INT NOT NULL DEFAULT 0,
    price_gems INT NOT NULL DEFAULT 0,
    bonus_str INT NOT NULL DEFAULT 0,
    bonus_end INT NOT NULL DEFAULT 0,
    bonus_pre INT NOT NULL DEFAULT 0,
    bonus_luck INT NOT NULL DEFAULT 0,
    description TEXT,
    icon VARCHAR(20) NOT NULL DEFAULT '⚔️'
) ENGINE=InnoDB;

-- 3. Bảng kho đồ người chơi (User Inventory & Equipped status)
CREATE TABLE IF NOT EXISTS user_inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    INDEX idx_user_equipped (user_id, is_equipped)
) ENGINE=InnoDB;

-- 4. Bảng nhật ký bài tập (Workout Sessions & Loot Drops)
CREATE TABLE IF NOT EXISTS workouts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    exercise VARCHAR(50) NOT NULL,
    reps INT NOT NULL DEFAULT 0,
    hold_seconds INT NOT NULL DEFAULT 0,
    score INT NOT NULL,
    xp_earned INT NOT NULL,
    gold_earned INT NOT NULL,
    dropped_item_id VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (dropped_item_id) REFERENCES items(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. Bảng nhiệm vụ (Challenges)
CREATE TABLE IF NOT EXISTS challenges (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category ENUM('DAILY', 'WEEKLY', 'BOSS') NOT NULL,
    quote VARCHAR(255),
    goal INT NOT NULL,
    reward_xp INT NOT NULL,
    reward_gold INT NOT NULL,
    reward_gems INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- 6. Bảng tiến độ nhiệm vụ người chơi
CREATE TABLE IF NOT EXISTS user_challenges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    challenge_id VARCHAR(50) NOT NULL,
    current_progress INT NOT NULL DEFAULT 0,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    date_key VARCHAR(20) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_chal_date (user_id, challenge_id, date_key)
) ENGINE=InnoDB;

-- 7. Bảng bạn bè giữa các người chơi (Friends)
CREATE TABLE IF NOT EXISTS friends (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_friend (user_id, friend_id)
) ENGINE=InnoDB;

-- ====================================================================
-- SEED DATA: DỮ LIỆU VẬT PHẨM MẪU (24 ITEMS ĐẦY ĐỦ 6 SLOTS & 4 ĐỘ HIẾM)
-- ====================================================================

INSERT INTO items (id, name, slot, rarity, price_gold, price_gems, bonus_str, bonus_end, bonus_pre, bonus_luck, description, icon) VALUES
-- MŨ GIÁP (HELMET)
('helm_bronze', 'Mũ Đồng Tân Binh', 'HELMET', 'COMMON', 50, 0, 0, 3, 0, 1, 'Mũ đồng đúc thô sơ giúp che chắn đầu khi tập nặng.', '🪖'),
('helm_iron', 'Thiết Giáp Đầu', 'HELMET', 'RARE', 160, 0, 3, 7, 0, 2, 'Rèn từ sắt non tôi luyện trong nhiệt độ cao.', '⛑️'),
('helm_valkyrie', 'Mũ Lông Vũ Valkyrie', 'HELMET', 'EPIC', 420, 5, 5, 14, 0, 8, 'Ban phước bởi các nữ thần chiến trận phương Bắc.', '👑'),
('helm_dragon', 'Vương Miện Long Thần', 'HELMET', 'LEGENDARY', 1000, 20, 18, 25, 0, 12, 'Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.', '🐉'),

-- GIÁP NGỰC (ARMOR)
('armor_leather', 'Áo Da Dã Ngoại', 'ARMOR', 'COMMON', 60, 0, 0, 4, 2, 0, 'Áo da bò mềm mại, thoáng mát cho các buổi hít đất dài.', '🥋'),
('armor_plate', 'Chiến Giáp Thép Nung', 'ARMOR', 'RARE', 180, 0, 5, 10, 0, 0, 'Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.', '🛡️'),
('armor_shadow', 'Áo Choàng Bóng Đêm', 'ARMOR', 'EPIC', 450, 6, 10, 0, 12, 8, 'Hòa mình vào bóng tối, tăng sự tập trung và độ chuẩn xác.', '🥷'),
('armor_celestial', 'Thánh Giáp Quang Minh', 'ARMOR', 'LEGENDARY', 1200, 25, 20, 28, 15, 0, 'Ánh hào quang chiếu rọi bảo bọc người chiến binh bền bỉ.', '✨'),

-- GĂNG TAY (GLOVES)
('gloves_cloth', 'Băng Quấn Cổ Tay', 'GLOVES', 'COMMON', 40, 0, 3, 0, 2, 0, 'Bảo vệ khớp cổ tay khi chống đẩy liên tục trên sàn cứng.', '🥊'),
('gloves_grip', 'Găng Hít Đất Siêu Bám', 'GLOVES', 'RARE', 150, 0, 8, 0, 6, 0, 'Đế cao su hạt kim cương chống trượt tay hoàn đối.', '🧤'),
('gloves_titan', 'Găng Titan Siêu Lực', 'GLOVES', 'EPIC', 400, 5, 18, 0, 8, 3, 'Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.', '🦾'),
('gloves_infinity', 'Găng Tay Vô Cực', 'GLOVES', 'LEGENDARY', 1100, 22, 32, 0, 16, 14, 'Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.', '🌌'),

-- GIÀY (BOOTS)
('boots_runner', 'Giày Chạy Phản Lực', 'BOOTS', 'COMMON', 45, 0, 0, 3, 0, 2, 'Êm ái, giảm chấn gối khi squat hoặc bật nhảy.', '👟'),
('boots_iron', 'Hộ Chân Chiến Binh', 'BOOTS', 'RARE', 150, 0, 6, 7, 0, 0, 'Bọc thép mũi chân và ống quyển vững chãi như bàn thạch.', '🥾'),
('boots_winged', 'Hài Phong Thần Hermes', 'BOOTS', 'EPIC', 380, 5, 0, 15, 6, 12, 'Đôi giày có cánh lướt đi nhẹ tựa lông hồng.', '🪽'),
('boots_abyss', 'Bộ Bước Vực Thẳm', 'BOOTS', 'LEGENDARY', 950, 18, 18, 24, 0, 15, 'Mỗi bước chân để lại dư chấn khiến kẻ thù khiếp đảm.', '⚡'),

-- BÙA CHÚ (AMULET)
('amulet_stone', 'Bùa Đá May Mắn', 'AMULET', 'COMMON', 50, 0, 0, 0, 0, 4, 'Hòn đá cuội ven suối đem lại vận may khi tập.', '🪬'),
('amulet_ruby', 'Huyết Ngọc Hồi Phục', 'AMULET', 'RARE', 190, 0, 6, 6, 0, 5, 'Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.', '🔮'),
('amulet_eye', 'Mắt Ưng Tinh Anh', 'AMULET', 'EPIC', 480, 7, 10, 0, 16, 10, 'Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mi-li-mét.', '👁️'),
('amulet_sun', 'Thái Dương Cổ Thạch', 'AMULET', 'LEGENDARY', 1300, 30, 20, 20, 20, 25, 'Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.', '☀️'),

-- VŨ KHÍ (WEAPON)
('weapon_stick', 'Côn Gỗ Luyện Tập', 'WEAPON', 'COMMON', 50, 0, 4, 0, 2, 0, 'Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.', '🪵'),
('weapon_sword', 'Thanh Kiếm Thép Đúc', 'WEAPON', 'RARE', 180, 0, 10, 0, 6, 0, 'Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.', '⚔️'),
('weapon_axe', 'Rìu Chiến Berserker', 'WEAPON', 'EPIC', 460, 6, 22, 8, 0, 0, 'Chiếc rìu khổng lồ dành riêng cho những chiến binh cuồng nộ.', '🪓'),
('weapon_excalibur', 'Thánh Kiếm Excalibur', 'WEAPON', 'LEGENDARY', 1400, 30, 35, 0, 18, 15, 'Bảo kiếm huyền thoại cắm sâu trong đá, chỉ người xứng đáng mới rút được.', '🗡️')
ON DUPLICATE KEY UPDATE name=VALUES(name), price_gold=VALUES(price_gold), bonus_str=VALUES(bonus_str);

-- SEED MẪU NHIỆM VỤ
INSERT INTO challenges (id, name, category, quote, goal, reward_xp, reward_gold, reward_gems) VALUES
('chal_daily_1', 'Tay Vững', 'DAILY', '“Bốn mươi rep, chiến binh — cứ từ từ.”', 40, 100, 13, 0),
('chal_daily_2', 'Máu Đầu Tiên', 'DAILY', '“Hạ một kẻ thù trong ngày.”', 1, 50, 10, 0),
('chal_daily_3', 'Bất Hoại', 'DAILY', '“Trăm rep trong một ngày — huyền thoại.”', 100, 520, 55, 1),
('chal_weekly_1', 'Vigil Trường Kỳ', 'WEEKLY', '“200 rep trong cả tuần.”', 200, 300, 60, 0),
('chal_weekly_2', 'Không Ngơi Nghỉ', 'WEEKLY', '“Hạ mười hai kẻ thù trong tuần này.”', 12, 480, 90, 1),
('chal_weekly_3', 'Ý Chí Bất Khuất', 'WEEKLY', '“500 rep trong bảy ngày. Một huyền thoại.”', 500, 900, 150, 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- KHỞI TẠO USER MẶC ĐỊNH & DỮ LIỆU BẢNG XẾP HẠNG THẾ GIỚI
INSERT INTO users (id, username, password, avatar, gold, gems, xp, level, streak, total_reps, stage) VALUES
(1, 'Hachimi', '123456', '🦊', 250, 10, 80, 2, 3, 45, 2),
(2, 'ShadowBlade', '123456', '🥷', 500, 25, 450, 5, 7, 210, 4),
(3, 'ValkyrieGym', '123456', '👑', 900, 40, 950, 8, 14, 480, 7),
(4, 'IronTitan', '123456', '🗿', 1400, 60, 1600, 12, 21, 850, 10),
(5, 'DragonFit', '123456', '🐉', 300, 15, 250, 3, 5, 120, 3),
(6, 'Phoenix', '123456', '🔥', 750, 30, 720, 6, 9, 340, 5)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- TẶNG TÂN BINH 1 MŨ ĐỒNG VÀ TRANG BỊ LUÔN
INSERT INTO user_inventory (user_id, item_id, is_equipped)
SELECT 1, 'helm_bronze', TRUE
WHERE NOT EXISTS (SELECT 1 FROM user_inventory WHERE user_id = 1 AND item_id = 'helm_bronze');

-- KẾT BẠN MẪU GIỮA HACHIMI VÀ SHADOWBLADE
INSERT IGNORE INTO friends (user_id, friend_id) VALUES (1, 2), (2, 1);
