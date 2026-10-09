-- ====================================================================
-- CƠ SỞ DỮ LIỆU VIGIL RPG FITNESS (CHÍNH THỨC)
-- Hệ thống RPG kết hợp luyện tập thể hình, AI Camera Pose Tracking
-- ====================================================================

CREATE DATABASE IF NOT EXISTS vigil_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vigil_db;

-- 1. Bảng người dùng & chỉ số RPG
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE DEFAULT 'Hachimi',
    password VARCHAR(255) NOT NULL DEFAULT '123456',
    avatar VARCHAR(20) NOT NULL DEFAULT '🦊',
    gold INT NOT NULL DEFAULT 0,
    gems INT NOT NULL DEFAULT 0,
    xp INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    streak INT NOT NULL DEFAULT 0,
    total_reps INT NOT NULL DEFAULT 0,
    stage INT NOT NULL DEFAULT 1,
    last_workout_date VARCHAR(20) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Bảng vật phẩm (Items trong Shop & Drop Table với 8 Phẩm Cấp đa dạng)
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slot ENUM('HELMET', 'ARMOR', 'GLOVES', 'BOOTS', 'AMULET', 'WEAPON') NOT NULL,
    rarity ENUM('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'ANCIENT', 'DIVINE') NOT NULL,
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

-- 8. Bảng lời mời kết bạn (Friend Requests)
CREATE TABLE IF NOT EXISTS friend_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sender_receiver (sender_id, receiver_id)
) ENGINE=InnoDB;

-- 9. Bảng bang hội (Guilds)
CREATE TABLE IF NOT EXISTS guilds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    badge VARCHAR(20) DEFAULT '🛡️',
    slogan VARCHAR(255) NOT NULL,
    leader_id INT NOT NULL,
    level INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. Bảng thành viên bang hội (Guild Members)
CREATE TABLE IF NOT EXISTS guild_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guild_id INT NOT NULL,
    user_id INT UNIQUE NOT NULL,
    role ENUM('LEADER', 'MEMBER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. Bảng lời mời gia nhập bang hội từ thành viên/chủ bang (Guild Invitations)
CREATE TABLE IF NOT EXISTS guild_invitations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guild_id INT NOT NULL,
    inviter_id INT NOT NULL,
    invitee_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_guild_invitee (guild_id, invitee_id, status)
) ENGINE=InnoDB;

-- 12. Bảng đơn xin gia nhập bang hội từ người chơi (Guild Join Applications - Cần chủ bang phê duyệt)
CREATE TABLE IF NOT EXISTS guild_join_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guild_id INT NOT NULL,
    user_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_guild_user_req (guild_id, user_id)
) ENGINE=InnoDB;

-- 13. BOSS THẾ GIỚI BANG HỘI (GUILD WORLD BOSS RAID)
CREATE TABLE IF NOT EXISTS guild_boss (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guild_id INT NOT NULL,
    boss_id VARCHAR(50) NOT NULL DEFAULT 'boss_nether_dragon',
    boss_name VARCHAR(100) NOT NULL DEFAULT 'Hắc Long Viễn Cổ - Nidhogg',
    boss_title VARCHAR(100) NOT NULL DEFAULT 'SIÊU TRÙM THẾ GIỚI BANG HỘI',
    boss_avatar VARCHAR(20) NOT NULL DEFAULT '🐉',
    max_hp INT NOT NULL DEFAULT 500000,
    current_hp INT NOT NULL DEFAULT 500000,
    status ENUM('ACTIVE', 'DEFEATED') DEFAULT 'ACTIVE',
    reward_gold INT NOT NULL DEFAULT 15000,
    reward_gems INT NOT NULL DEFAULT 350,
    reward_item_id VARCHAR(50) NOT NULL DEFAULT 'weapon_dragon_slayer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    INDEX idx_guild_status (guild_id, status)
) ENGINE=InnoDB;

-- Bảng đóng góp sát thương của từng thành viên trong Bang
CREATE TABLE IF NOT EXISTS guild_boss_damage (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guild_id INT NOT NULL,
    boss_db_id INT NOT NULL,
    user_id INT NOT NULL,
    damage INT NOT NULL DEFAULT 0,
    reps_contributed INT NOT NULL DEFAULT 0,
    last_attack_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    has_claimed_defeat_reward BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (boss_db_id) REFERENCES guild_boss(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_boss_user (boss_db_id, user_id)
) ENGINE=InnoDB;

-- ====================================================================
-- SEED DATA: HỆ THỐNG VẬT PHẨM ĐA DẠNG (8 PHẨM CẤP X 6 SLOTS)
-- ====================================================================

INSERT INTO items (id, name, slot, rarity, price_gold, price_gems, bonus_str, bonus_end, bonus_pre, bonus_luck, description, icon) VALUES
-- 1. MŨ GIÁP (HELMET)
('helm_bronze', 'Mũ Đồng Tân Binh', 'HELMET', 'COMMON', 50, 0, 0, 3, 0, 1, 'Mũ đồng đúc thô sơ giúp che chắn đầu khi tập nặng.', '🪖'),
('helm_scout', 'Nón Trinh Sát Rừng Rậm', 'HELMET', 'UNCOMMON', 90, 0, 0, 5, 2, 1, 'Nón vải ngụy trang nhẹ nhàng cho các buổi cardio dã ngoại.', '🧢'),
('helm_iron', 'Thiết Giáp Đầu', 'HELMET', 'RARE', 160, 0, 3, 7, 0, 2, 'Rèn từ sắt non tôi luyện trong nhiệt độ cao.', '⛑️'),
('helm_valkyrie', 'Mũ Lông Vũ Valkyrie', 'HELMET', 'EPIC', 420, 5, 5, 14, 0, 8, 'Ban phước bởi các nữ thần chiến trận phương Bắc.', '👑'),
('helm_dragon', 'Vương Miện Long Thần', 'HELMET', 'LEGENDARY', 1000, 20, 20, 18, 25, 12, 'Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.', '🐉'),
('helm_abyss', 'Vương Miện Vực Sâu', 'HELMET', 'MYTHIC', 2800, 60, 35, 40, 15, 20, 'Ngưng tụ từ hắc ám vô tận dưới đáy vực, bảo hộ tuyệt đối tinh thần.', '👑'),
('helm_odin', 'Mũ Thần Chiến Binh Odin', 'HELMET', 'ANCIENT', 5000, 120, 50, 55, 30, 35, 'Bảo vật của Vua các vị thần, khai mở trí tuệ và thể lực siêu phàm.', '🦅'),
('helm_divine_crown', 'Thần Quan Thiên Giới', 'HELMET', 'DIVINE', 8500, 200, 75, 80, 50, 60, 'Vương miện của Đấng Tối Cao, hội tụ hào quang thái hư bảo bọc.', '✨'),

-- 2. GIÁP NGỰC (ARMOR)
('armor_leather', 'Áo Da Dã Ngoại', 'ARMOR', 'COMMON', 60, 0, 0, 4, 2, 0, 'Áo da bò mềm mại, thoáng mát cho các buổi hít đất dài.', '🥋'),
('armor_chainmail', 'Áo Giáp Xích Bạc', 'ARMOR', 'UNCOMMON', 110, 0, 2, 7, 1, 0, 'Kết từ hàng nghìn vòng xích thép dẻo dai phân tán lực va đập.', '⛓️'),
('armor_plate', 'Chiến Giáp Thép Nung', 'ARMOR', 'RARE', 180, 0, 5, 10, 0, 0, 'Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.', '🛡️'),
('armor_shadow', 'Áo Choàng Bóng Đêm', 'ARMOR', 'EPIC', 450, 6, 10, 0, 12, 8, 'Hòa mình vào bóng tối, tăng sự tập trung và độ chuẩn xác.', '🥷'),
('armor_celestial', 'Thánh Giáp Quang Minh', 'ARMOR', 'LEGENDARY', 1200, 25, 20, 28, 15, 0, 'Ánh hào quang chiếu rọi bảo bọc người chiến binh bền bỉ.', '✨'),
('armor_dragon_scale', 'Long Lân Thần Giáp', 'ARMOR', 'MYTHIC', 3200, 75, 25, 60, 15, 20, 'Lớp vảy rồng kiên cố bất khả xâm phạm bảo vệ toàn thân.', '🐲'),
('armor_aegis', 'Thánh Giáp Bất Hoại Aegis', 'ARMOR', 'ANCIENT', 5500, 130, 40, 80, 25, 35, 'Tấm khiên giáp huyền thoại của thần Zeus, chặn đứng mọi ngoại lực.', '🛡️'),
('armor_primordial', 'Hỗn Nguyên Chiến Giáp', 'ARMOR', 'DIVINE', 9500, 240, 65, 120, 45, 60, 'Rèn từ vật chất khởi thủy trước khi vũ trụ hình thành, bất hoại vĩnh cửu.', '🌌'),

-- 3. GĂNG TAY (GLOVES)
('gloves_cloth', 'Băng Quấn Cổ Tay', 'GLOVES', 'COMMON', 40, 0, 3, 0, 2, 0, 'Bảo vệ khớp cổ tay khi chống đẩy liên tục trên sàn cứng.', '🥊'),
('gloves_leather_strap', 'Găng Đấu Khí Thiếu Niên', 'GLOVES', 'UNCOMMON', 80, 0, 5, 0, 4, 1, 'Găng da dê bọc khớp tăng uy lực cú đấm và chống đẩy.', '🥊'),
('gloves_grip', 'Găng Hít Đất Siêu Bám', 'GLOVES', 'RARE', 150, 0, 8, 0, 6, 0, 'Đế cao su hạt kim cương chống trượt tay hoàn đối.', '🧤'),
('gloves_titan', 'Găng Titan Siêu Lực', 'GLOVES', 'EPIC', 400, 5, 18, 0, 8, 3, 'Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.', '🦾'),
('gloves_infinity', 'Găng Tay Vô Cực', 'GLOVES', 'LEGENDARY', 1100, 22, 32, 0, 16, 14, 'Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.', '🌌'),
('gloves_dragon_claw', 'Vuốt Rồng Bạt Hải', 'GLOVES', 'MYTHIC', 2600, 55, 50, 10, 30, 25, 'Móng vuốt rồng thiêng xé toạc hư không, bùng nổ lực đẩy tay.', '🐉'),
('gloves_thunder_strike', 'Quyền Thủ Lôi Thần Thor', 'GLOVES', 'ANCIENT', 4800, 110, 75, 20, 40, 30, 'Găng sắt thần thánh giúp vung sấm sét ngàn cân dễ như trở bàn tay.', '⚡'),
('gloves_creator', 'Thủ Ấn Khởi Nguyên', 'GLOVES', 'DIVINE', 8200, 190, 110, 35, 65, 55, 'Bàn tay nhào nặn tinh cầu, chuyển hóa từng nhịp đẩy thành siêu sóng xung kích.', '☄️'),

-- 4. GIÀY (BOOTS)
('boots_runner', 'Giày Chạy Phản Lực', 'BOOTS', 'COMMON', 45, 0, 0, 3, 0, 2, 'Êm ái, giảm chấn gối khi squat hoặc bật nhảy.', '👟'),
('boots_leather_hunter', 'Ủng Da Thợ Săn', 'BOOTS', 'UNCOMMON', 95, 0, 1, 5, 3, 2, 'Bám chắc địa hình, giảm áp lực lên gót chân khi nhảy dây.', '👢'),
('boots_iron', 'Hộ Chân Chiến Binh', 'BOOTS', 'RARE', 150, 0, 6, 7, 0, 0, 'Bọc thép mũi chân và ống quyển vững chãi như bàn thạch.', '🥾'),
('boots_winged', 'Hài Phong Thần Hermes', 'BOOTS', 'EPIC', 380, 5, 0, 15, 6, 12, 'Đôi giày có cánh lướt đi nhẹ tựa lông hồng.', '🪽'),
('boots_abyss', 'Bộ Bước Vực Thẳm', 'BOOTS', 'LEGENDARY', 950, 18, 18, 24, 0, 15, 'Mỗi bước chân để lại dư chấn khiến kẻ thù khiếp đảm.', '⚡'),
('boots_shadow_stalker', 'Hư Không Bộ Pháp', 'BOOTS', 'MYTHIC', 2500, 50, 20, 35, 25, 30, 'Lướt đi giữa các chiều không gian, đôi chân không hề biết mỏi.', '⚡'),
('boots_chronos', 'Hài Thời Gian Chronos', 'BOOTS', 'ANCIENT', 4600, 105, 30, 50, 45, 40, 'Bước chân thao túng thời gian, biến mỗi giây plank thành sức mạnh vô song.', '⏳'),
('boots_celestial_stride', 'Tiêu Dao Thần Bộ', 'BOOTS', 'DIVINE', 8000, 180, 45, 75, 70, 65, 'Đạp mây cưỡi gió vượt qua ranh giới cõi phàm trần.', '🌟'),

-- 5. BÙA CHÚ (AMULET)
('amulet_stone', 'Bùa Đá May Mắn', 'AMULET', 'COMMON', 50, 0, 0, 0, 0, 4, 'Hòn đá cuội ven suối đem lại vận may khi tập.', '🪬'),
('amulet_wolf_tooth', 'Nanh Sói Hoang Dã', 'AMULET', 'UNCOMMON', 100, 0, 2, 2, 2, 6, 'Nanh sói đầu đàn mang lại giác quan nhạy bén và may mắn.', '🐺'),
('amulet_ruby', 'Huyết Ngọc Hồi Phục', 'AMULET', 'RARE', 190, 0, 6, 6, 0, 5, 'Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.', '🔮'),
('amulet_eye', 'Mắt Ưng Tinh Anh', 'AMULET', 'EPIC', 480, 7, 10, 0, 16, 10, 'Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mi-li-mét.', '👁️'),
('amulet_sun', 'Thái Dương Cổ Thạch', 'AMULET', 'LEGENDARY', 1300, 30, 20, 20, 20, 25, 'Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.', '☀️'),
('amulet_boss_heart', 'Trái Tim Hắc Long', 'AMULET', 'MYTHIC', 4000, 100, 30, 30, 30, 45, 'Tinh hoa sinh mệnh của Siêu Trùm Thế Giới ban phước lành.', '💎'),
('amulet_ouroboros', 'Ngọc Bội Vô Cực Ouroboros', 'AMULET', 'ANCIENT', 5800, 140, 45, 45, 45, 60, 'Biểu tượng con rắn cắn đuôi luân hồi, sinh lực dồi dào bất tận.', '♾️'),
('amulet_genesis_spark', 'Hỏa Chủng Sáng Thế', 'AMULET', 'DIVINE', 9900, 260, 70, 70, 70, 90, 'Tia lửa ban đầu thắp sáng muôn loài, gia tăng cực hạn mọi chỉ số.', '💥'),

-- 6. VŨ KHÍ (WEAPON)
('weapon_stick', 'Côn Gỗ Luyện Tập', 'WEAPON', 'COMMON', 50, 0, 4, 0, 2, 0, 'Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.', '🪵'),
('weapon_dagger', 'Dao Găm Sát Thủ', 'WEAPON', 'UNCOMMON', 100, 0, 7, 0, 5, 2, 'Lưỡi dao thép đen nhẹ bén, thích hợp luyện tập tốc độ cao.', '🗡️'),
('weapon_sword', 'Thanh Kiếm Thép Đúc', 'WEAPON', 'RARE', 180, 0, 10, 0, 6, 0, 'Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.', '⚔️'),
('weapon_axe', 'Rìu Chiến Berserker', 'WEAPON', 'EPIC', 460, 6, 22, 8, 0, 0, 'Chiếc rìu khổng lồ dành riêng cho những chiến binh cuồng nộ.', '🪓'),
('weapon_excalibur', 'Thánh Kiếm Excalibur', 'WEAPON', 'LEGENDARY', 1400, 30, 35, 0, 18, 15, 'Bảo kiếm huyền thoại cắm sâu trong đá, chỉ người xứng đáng mới rút được.', '🗡️'),
('weapon_dragon_slayer', 'Đại Đao Trảm Long', 'WEAPON', 'MYTHIC', 3500, 80, 65, 15, 35, 25, 'Thần binh rèn từ vảy và răng Hắc Long, uy lực hủy thiên diệt địa.', '🗡️'),
('weapon_gungnir', 'Thần Thương Gungnir', 'WEAPON', 'ANCIENT', 6000, 150, 85, 25, 60, 40, 'Ngọn thương thần thoại bách phát bách trúng, uy lực xuyên thủng mọi hàng phòng thủ.', '🔱'),
('weapon_god_slayer', 'Đồ Thần Cực Kiếm', 'WEAPON', 'DIVINE', 10000, 300, 130, 40, 80, 70, 'Thần binh chí tôn trảm phá thần ma, đòn đánh xé rách thực tại.', '⚔️')
ON DUPLICATE KEY UPDATE name=VALUES(name), rarity=VALUES(rarity), price_gold=VALUES(price_gold), bonus_str=VALUES(bonus_str);

-- SEED MẪU NHIỆM VỤ
INSERT INTO challenges (id, name, category, quote, goal, reward_xp, reward_gold, reward_gems) VALUES
('chal_daily_1', 'Tay Vững', 'DAILY', '“Bốn mươi rep, chiến binh — cứ từ từ.”', 40, 100, 13, 0),
('chal_daily_2', 'Máu Đầu Tiên', 'DAILY', '“Hạ một kẻ thù trong ngày.”', 1, 50, 10, 0),
('chal_daily_3', 'Bất Hoại', 'DAILY', '“Trăm rep trong một ngày — huyền thoại.”', 100, 520, 55, 1),
('chal_weekly_1', 'Vigil Trường Kỳ', 'WEEKLY', '“200 rep trong cả tuần.”', 200, 300, 60, 0),
('chal_weekly_2', 'Không Ngơi Nghỉ', 'WEEKLY', '“Hạ mười hai kẻ thù trong tuần này.”', 12, 480, 90, 1),
('chal_weekly_3', 'Ý Chí Bất Khuất', 'WEEKLY', '“500 rep trong bảy ngày. Một huyền thoại.”', 500, 900, 150, 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- KHỞI TẠO USER GỐC (CHỈ GIỮ LẠI USER THỰC SỰ CỦA NGƯỜI DÙNG)
INSERT INTO users (id, username, password, avatar, gold, gems, xp, level, streak, total_reps, stage) VALUES
(1, 'Hachimi', '123456', '🦊', 0, 0, 80, 2, 3, 45, 2)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- TẶNG TÂN BINH 1 MŨ ĐỒNG
INSERT INTO user_inventory (user_id, item_id, is_equipped)
SELECT 1, 'helm_bronze', TRUE
WHERE NOT EXISTS (SELECT 1 FROM user_inventory WHERE user_id = 1 AND item_id = 'helm_bronze');

-- BANG HỘI KHỞI ĐẦU CHO USER
INSERT INTO guilds (id, name, badge, slogan, leader_id, level) VALUES
(1, 'Vigil Chiến Binh 🛡️', '🛡️', 'Tập luyện bứt phá giới hạn mỗi ngày!', 1, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO guild_members (guild_id, user_id, role) VALUES
(1, 1, 'LEADER')
ON DUPLICATE KEY UPDATE role=VALUES(role);

-- BOSS THẾ GIỚI CHO BANG HỘI
INSERT INTO guild_boss (id, guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id) VALUES
(1, 1, 'boss_nether_dragon', 'Hắc Long Viễn Cổ - Nidhogg', 'SIÊU TRÙM THẾ GIỚI BANG HỘI', '🐉', 500000, 500000, 'ACTIVE', 15000, 350, 'weapon_dragon_slayer')
ON DUPLICATE KEY UPDATE boss_name=VALUES(boss_name);
