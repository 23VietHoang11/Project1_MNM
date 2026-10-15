# ====================================================================
# VIGIL REST API & ADMIN SERVER (Python 3.11 Backend Server)
# Cung cấp đầy đủ REST API cho Android App và Web Admin Dashboard
# Hỗ trợ toàn diện FR8.1, FR8.2, FR8.3, UC08 theo Báo cáo Chương 2
# ====================================================================

import json
import random
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime, date, timedelta
import sqlite3
import os
from urllib.parse import urlparse, parse_qs

PORT = 3000
DB_FILE = os.path.join(os.path.dirname(__file__), 'vigil_local.db')
ADMIN_DIR = os.path.join(os.path.dirname(__file__), 'admin')

def init_db():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()

    # 1. Bảng người chơi (users)
    c.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password TEXT DEFAULT '123456',
            avatar TEXT DEFAULT '🦊',
            gold INTEGER DEFAULT 150,
            gems INTEGER DEFAULT 10,
            xp INTEGER DEFAULT 0,
            level INTEGER DEFAULT 1,
            streak INTEGER DEFAULT 0,
            total_reps INTEGER DEFAULT 0,
            stage INTEGER DEFAULT 1,
            role TEXT DEFAULT 'User',
            status INTEGER DEFAULT 1,
            email TEXT,
            phone TEXT,
            lock_reason TEXT,
            last_workout_date TEXT
        )
    ''')

    # Nâng cấp bảng users an toàn nếu đã tồn tại từ trước
    for col, col_type, col_default in [
        ('role', 'TEXT', "'User'"),
        ('status', 'INTEGER', "1"),
        ('email', 'TEXT', "NULL"),
        ('phone', 'TEXT', "NULL"),
        ('password', 'TEXT', "'123456'"),
        ('lock_reason', 'TEXT', "NULL"),
        ('avatar', 'TEXT', "'🦊'")
    ]:
        try:
            c.execute(f"ALTER TABLE users ADD COLUMN {col} {col_type} DEFAULT {col_default}")
        except sqlite3.OperationalError:
            pass

    # 2. Bảng vật phẩm (items)
    c.execute('''
        CREATE TABLE IF NOT EXISTS items (
            id TEXT PRIMARY KEY,
            name TEXT,
            slot TEXT,
            rarity TEXT,
            price_gold INTEGER,
            price_gems INTEGER,
            bonus_str INTEGER,
            bonus_end INTEGER,
            bonus_pre INTEGER,
            bonus_luck INTEGER,
            description TEXT,
            icon TEXT
        )
    ''')

    # 3. Bảng kho đồ người chơi (user_inventory)
    c.execute('''
        CREATE TABLE IF NOT EXISTS user_inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            item_id TEXT,
            is_equipped INTEGER DEFAULT 0,
            acquired_at TEXT
        )
    ''')

    # 4. Bảng nhật ký bài tập (workouts)
    c.execute('''
        CREATE TABLE IF NOT EXISTS workouts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            exercise TEXT,
            reps INTEGER,
            hold_seconds INTEGER,
            score INTEGER,
            xp_earned INTEGER,
            gold_earned INTEGER,
            dropped_item_id TEXT,
            created_at TEXT
        )
    ''')

    # 5. Bảng Quái vật (monsters) - Zero Hardcoding
    c.execute('''
        CREATE TABLE IF NOT EXISTS monsters (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            title TEXT,
            avatar TEXT DEFAULT '👾',
            stage_id INTEGER DEFAULT 1,
            max_hp INTEGER DEFAULT 500,
            current_hp INTEGER DEFAULT 500,
            exercise TEXT DEFAULT 'PUSHUP',
            is_boss INTEGER DEFAULT 0,
            exp_reward INTEGER DEFAULT 100,
            gold_reward INTEGER DEFAULT 20,
            drop_rarity TEXT DEFAULT 'RARE',
            lore TEXT
        )
    ''')

    # 6. Bảng Quy chuẩn Bài tập AI (exercises)
    c.execute('''
        CREATE TABLE IF NOT EXISTS exercises (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            target_muscle TEXT,
            extend_angle REAL DEFAULT 165.0,
            contract_angle REAL DEFAULT 90.0,
            damage_factor REAL DEFAULT 1.2,
            calories_per_rep REAL DEFAULT 0.8,
            description TEXT
        )
    ''')

    # 7. Bảng Cảnh báo gian lận (anti_cheat_alerts)
    c.execute('''
        CREATE TABLE IF NOT EXISTS anti_cheat_alerts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            username TEXT,
            exercise TEXT,
            reps INTEGER,
            duration_seconds INTEGER,
            flag_reason TEXT,
            severity TEXT DEFAULT 'HIGH',
            status TEXT DEFAULT 'PENDING',
            created_at TEXT
        )
    ''')

    # 8. Bảng Nhật ký kiểm toán hệ thống (audit_logs)
    c.execute('''
        CREATE TABLE IF NOT EXISTS audit_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            admin_username TEXT DEFAULT 'Admin',
            action TEXT NOT NULL,
            target_name TEXT,
            details TEXT,
            ip_address TEXT DEFAULT '127.0.0.1',
            created_at TEXT
        )
    ''')

    # Bảng Bang hội (guilds)
    c.execute('''
        CREATE TABLE IF NOT EXISTS guilds (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE,
            slogan TEXT,
            badge TEXT,
            level INTEGER DEFAULT 1,
            leader_id INTEGER,
            created_at TEXT
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS guild_members (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER,
            user_id INTEGER UNIQUE,
            role TEXT DEFAULT 'MEMBER',
            joined_at TEXT
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS guild_join_requests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER,
            user_id INTEGER,
            status TEXT DEFAULT 'PENDING',
            created_at TEXT
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS guild_boss (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER,
            boss_id TEXT,
            boss_name TEXT,
            boss_title TEXT,
            boss_avatar TEXT,
            max_hp INTEGER,
            current_hp INTEGER,
            status TEXT DEFAULT 'ACTIVE',
            reward_gold INTEGER,
            reward_gems INTEGER,
            reward_item_id TEXT,
            created_at TEXT,
            defeated_at TEXT
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS guild_boss_damage (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER,
            boss_db_id INTEGER,
            user_id INTEGER,
            damage INTEGER DEFAULT 0,
            reps_contributed INTEGER DEFAULT 0,
            has_claimed_defeat_reward INTEGER DEFAULT 0
        )
    ''')

    # Seed 48 items
    items_data = [
        # 1. HELMET
        ('helm_bronze', 'Mũ Đồng Tân Binh', 'HELMET', 'COMMON', 50, 0, 0, 3, 0, 1, 'Mũ đồng đúc thô sơ giúp che chắn đầu khi tập nặng.', '🪖'),
        ('helm_scout', 'Nón Trinh Sát Rừng Rậm', 'HELMET', 'UNCOMMON', 90, 0, 0, 5, 2, 1, 'Nón vải ngụy trang nhẹ nhàng cho các buổi cardio dã ngoại.', '🧢'),
        ('helm_iron', 'Thiết Giáp Đầu', 'HELMET', 'RARE', 160, 0, 3, 7, 0, 2, 'Rèn từ sắt non tôi luyện trong nhiệt độ cao.', '⛑️'),
        ('helm_valkyrie', 'Mũ Lông Vũ Valkyrie', 'HELMET', 'EPIC', 420, 5, 5, 14, 0, 8, 'Ban phước bởi các nữ thần chiến trận phương Bắc.', '👑'),
        ('helm_dragon', 'Vương Miện Long Thần', 'HELMET', 'LEGENDARY', 1000, 20, 20, 18, 25, 12, 'Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.', '🐉'),
        ('helm_abyss', 'Vương Miện Vực Sâu', 'HELMET', 'MYTHIC', 2800, 60, 35, 40, 15, 20, 'Ngưng tụ từ hắc ám vô tận dưới đáy vực, bảo hộ tuyệt đối tinh thần.', '👑'),
        ('helm_odin', 'Mũ Thần Chiến Binh Odin', 'HELMET', 'ANCIENT', 5000, 120, 50, 55, 30, 35, 'Bảo vật của Vua các vị thần, khai mở trí tuệ và thể lực siêu phàm.', '🦅'),
        ('helm_divine_crown', 'Thần Quan Thiên Giới', 'HELMET', 'DIVINE', 8500, 200, 75, 80, 50, 60, 'Vương miện của Đấng Tối Cao, hội tụ hào quang thái hư bảo bọc.', '✨'),

        # 2. ARMOR
        ('armor_leather', 'Áo Da Dã Ngoại', 'ARMOR', 'COMMON', 60, 0, 0, 4, 2, 0, 'Áo da bò mềm mại, thoáng mát cho các buổi hít đất dài.', '🥋'),
        ('armor_chainmail', 'Áo Giáp Xích Bạc', 'ARMOR', 'UNCOMMON', 110, 0, 2, 7, 1, 0, 'Kết từ hàng nghìn vòng xích thép dẻo dai phân tán lực va đập.', '⛓️'),
        ('armor_plate', 'Chiến Giáp Thép Nung', 'ARMOR', 'RARE', 180, 0, 5, 10, 0, 0, 'Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.', '🛡️'),
        ('armor_shadow', 'Áo Choàng Bóng Đêm', 'ARMOR', 'EPIC', 450, 6, 10, 0, 12, 8, 'Hòa mình vào bóng tối, tăng sự tập trung và độ chuẩn xác.', '🥷'),
        ('armor_celestial', 'Thánh Giáp Quang Minh', 'ARMOR', 'LEGENDARY', 1200, 25, 20, 28, 15, 0, 'Ánh hào quang chiếu rọi bảo bọc người chiến binh bền bỉ.', '✨'),
        ('armor_dragon_scale', 'Long Lân Thần Giáp', 'ARMOR', 'MYTHIC', 3200, 75, 25, 60, 15, 20, 'Lớp vảy rồng kiên cố bất khả xâm phạm bảo vệ toàn thân.', '🐲'),
        ('armor_aegis', 'Thánh Giáp Bất Hoại Aegis', 'ARMOR', 'ANCIENT', 5500, 130, 40, 80, 25, 35, 'Tấm khiên giáp huyền thoại của thần Zeus, chặn đứng mọi ngoại lực.', '🛡️'),
        ('armor_primordial', 'Hỗn Nguyên Chiến Giáp', 'ARMOR', 'DIVINE', 9500, 240, 65, 120, 45, 60, 'Rèn từ vật chất khởi thủy trước khi vũ trụ hình thành, bất hoại vĩnh cửu.', '🌌'),

        # 3. GLOVES
        ('gloves_cloth', 'Băng Quấn Cổ Tay', 'GLOVES', 'COMMON', 40, 0, 3, 0, 2, 0, 'Bảo vệ khớp cổ tay khi chống đẩy liên tục trên sàn cứng.', '🥊'),
        ('gloves_leather_strap', 'Găng Đấu Khí Thiếu Niên', 'GLOVES', 'UNCOMMON', 80, 0, 5, 0, 4, 1, 'Găng da dê bọc khớp tăng uy lực cú đấm và chống đẩy.', '🥊'),
        ('gloves_grip', 'Găng Hít Đất Siêu Bám', 'GLOVES', 'RARE', 150, 0, 8, 0, 6, 0, 'Đế cao su hạt kim cương chống trượt tay hoàn đối.', '🧤'),
        ('gloves_titan', 'Găng Titan Siêu Lực', 'GLOVES', 'EPIC', 400, 5, 18, 0, 8, 3, 'Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.', '🦾'),
        ('gloves_infinity', 'Găng Tay Vô Cực', 'GLOVES', 'LEGENDARY', 1100, 22, 32, 0, 16, 14, 'Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.', '🌌'),
        ('gloves_dragon_claw', 'Vuốt Rồng Bạt Hải', 'GLOVES', 'MYTHIC', 2600, 55, 50, 10, 30, 25, 'Móng vuốt rồng thiêng xé toạc hư không, bùng nổ lực đẩy tay.', '🐉'),
        ('gloves_thunder_strike', 'Quyền Thủ Lôi Thần Thor', 'GLOVES', 'ANCIENT', 4800, 110, 75, 20, 40, 30, 'Găng sắt thần thánh giúp vung sấm sét ngàn cân dễ như trở bàn tay.', '⚡'),
        ('gloves_creator', 'Thủ Ấn Khởi Nguyên', 'GLOVES', 'DIVINE', 8200, 190, 110, 35, 65, 55, 'Bàn tay nhào nặn tinh cầu, chuyển hóa từng nhịp đẩy thành siêu sóng xung kích.', '☄️'),

        # 4. BOOTS
        ('boots_runner', 'Giày Chạy Phản Lực', 'BOOTS', 'COMMON', 45, 0, 0, 3, 0, 2, 'Êm ái, giảm chấn gối khi squat hoặc bật nhảy.', '👟'),
        ('boots_leather_hunter', 'Ủng Da Thợ Săn', 'BOOTS', 'UNCOMMON', 95, 0, 1, 5, 3, 2, 'Bám chắc địa hình, giảm áp lực lên gót chân khi nhảy dây.', '👢'),
        ('boots_iron', 'Hộ Chân Chiến Binh', 'BOOTS', 'RARE', 150, 0, 6, 7, 0, 0, 'Bọc thép mũi chân và ống quyển vững chãi như bàn thạch.', '🥾'),
        ('boots_winged', 'Hài Phong Thần Hermes', 'BOOTS', 'EPIC', 380, 5, 0, 15, 6, 12, 'Đôi giày có cánh lướt đi nhẹ tựa lông hồng.', '🪽'),
        ('boots_abyss', 'Bộ Bước Vực Thẳm', 'BOOTS', 'LEGENDARY', 950, 18, 18, 24, 0, 15, 'Mỗi bước chân để lại dư chấn khiến kẻ thù khiếp đảm.', '⚡'),
        ('boots_shadow_stalker', 'Hư Không Bộ Pháp', 'BOOTS', 'MYTHIC', 2500, 50, 20, 35, 25, 30, 'Lướt đi giữa các chiều không gian, đôi chân không hề biết mỏi.', '⚡'),
        ('boots_chronos', 'Hài Thời Gian Chronos', 'BOOTS', 'ANCIENT', 4600, 105, 30, 50, 45, 40, 'Bước chân thao túng thời gian, biến mỗi giây plank thành sức mạnh vô song.', '⏳'),
        ('boots_celestial_stride', 'Tiêu Dao Thần Bộ', 'BOOTS', 'DIVINE', 8000, 180, 45, 75, 70, 65, 'Đạp mây cưỡi gió vượt qua ranh giới cõi phàm trần.', '🌟'),

        # 5. AMULET
        ('amulet_stone', 'Bùa Đá May Mắn', 'AMULET', 'COMMON', 50, 0, 0, 0, 0, 4, 'Hòn đá cuội ven suối đem lại vận may khi tập.', '🪬'),
        ('amulet_wolf_tooth', 'Nanh Sói Hoang Dã', 'AMULET', 'UNCOMMON', 100, 0, 2, 2, 2, 6, 'Nanh sói đầu đàn mang lại giác quan nhạy bén và may mắn.', '🐺'),
        ('amulet_ruby', 'Huyết Ngọc Hồi Phục', 'AMULET', 'RARE', 190, 0, 6, 6, 0, 5, 'Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.', '🔮'),
        ('amulet_eye', 'Mắt Ưng Tinh Anh', 'AMULET', 'EPIC', 480, 7, 10, 0, 16, 10, 'Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mi-li-mét.', '👁️'),
        ('amulet_sun', 'Thái Dương Cổ Thạch', 'AMULET', 'LEGENDARY', 1300, 30, 20, 20, 20, 25, 'Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.', '☀️'),
        ('amulet_boss_heart', 'Trái Tim Hắc Long', 'AMULET', 'MYTHIC', 4000, 100, 30, 30, 30, 45, 'Tinh hoa sinh mệnh của Siêu Trùm Thế Giới ban phước lành.', '💎'),
        ('amulet_ouroboros', 'Ngọc Bội Vô Cực Ouroboros', 'AMULET', 'ANCIENT', 5800, 140, 45, 45, 45, 60, 'Biểu tượng con rắn cắn đuôi luân hồi, sinh lực dồi dào bất tận.', '♾️'),
        ('amulet_genesis_spark', 'Hỏa Chủng Sáng Thế', 'AMULET', 'DIVINE', 9900, 260, 70, 70, 70, 90, 'Tia lửa ban đầu thắp sáng muôn loài, gia tăng cực hạn mọi chỉ số.', '💥'),

        # 6. WEAPON
        ('weapon_stick', 'Côn Gỗ Luyện Tập', 'WEAPON', 'COMMON', 50, 0, 4, 0, 2, 0, 'Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.', '🪵'),
        ('weapon_dagger', 'Dao Găm Sát Thủ', 'WEAPON', 'UNCOMMON', 100, 0, 7, 0, 5, 2, 'Lưỡi dao thép đen nhẹ bén, thích hợp luyện tập tốc độ cao.', '🗡️'),
        ('weapon_sword', 'Thanh Kiếm Thép Đúc', 'WEAPON', 'RARE', 180, 0, 10, 0, 6, 0, 'Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.', '⚔️'),
        ('weapon_axe', 'Rìu Chiến Berserker', 'WEAPON', 'EPIC', 460, 6, 22, 8, 0, 0, 'Chiếc rìu khổng lồ dành riêng cho những chiến binh cuồng nộ.', '🪓'),
        ('weapon_excalibur', 'Thánh Kiếm Excalibur', 'WEAPON', 'LEGENDARY', 1400, 30, 35, 0, 18, 15, 'Bảo kiếm huyền thoại cắm sâu trong đá, chỉ người xứng đáng mới rút được.', '🗡️'),
        ('weapon_dragon_slayer', 'Đại Đao Trảm Long', 'WEAPON', 'MYTHIC', 3500, 80, 65, 15, 35, 25, 'Thần binh rèn từ vảy và răng Hắc Long, uy lực hủy thiên diệt địa.', '🗡️'),
        ('weapon_gungnir', 'Thần Thương Gungnir', 'WEAPON', 'ANCIENT', 6000, 150, 85, 25, 60, 40, 'Ngọn thương thần thoại bách phát bách trúng, uy lực xuyên thủng mọi hàng phòng thủ.', '🔱'),
        ('weapon_god_slayer', 'Đồ Thần Cực Kiếm', 'WEAPON', 'DIVINE', 10000, 300, 130, 40, 80, 70, 'Thần binh chí tôn trảm phá thần ma, đòn đánh xé rách thực tại.', '⚔️')
    ]
    c.executemany('INSERT OR REPLACE INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?)', items_data)

    # Seed 10 Monsters từ AdventureData
    monsters_data = [
        (1, 'Slime Gai Nhọn', 'Quái Bụi Cỏ', '🟢', 1, 160, 160, 'PUSHUP', 0, 80, 16, 'COMMON', 'Sinh vật dẻo dai thường nấp trong bụi rậm. Cần tung những cú chống đẩy dứt khoát để đè bẹp nó.'),
        (2, 'Sói Xám Đồng Cỏ', 'Kẻ Săn Đêm', '🐺', 1, 280, 280, 'SQUAT', 0, 140, 25, 'UNCOMMON', 'Tốc độ nhanh như gió. Hạ thấp trọng tâm bằng các rep squat chuẩn xác để né đòn cắn xé.'),
        (3, 'Yêu Tinh Cung Thủ', 'Thiện Xạ Bụi Cây', '👺', 1, 420, 420, 'SITUP', 0, 180, 32, 'RARE', 'Bắn tên liên tiếp tầm thấp. Gập bụng dồn dập để tránh mưa tên và áp sát tiêu diệt nó.'),
        (4, 'Hộ Vệ Đồng Cỏ', 'TRÙM ĐỒNG CỎ', '🗿', 1, 700, 700, 'PUSHUP', 1, 350, 60, 'EPIC', 'Bức tượng khổng lồ canh giữ lối vào Rừng Già. Cần lực đẩy dũng mãnh và trang bị vững chắc để phá vỡ lớp đá giáp!'),
        (5, 'Nhện Độc Gai Đen', 'Bóng Đen Giăng Tơ', '🕷️', 2, 1000, 1000, 'PUSHUP', 0, 190, 35, 'RARE', 'Giăng bẫy tơ độc trên cành cây. Cần hít đất liên hoàn để thoát khỏi mạng tơ dính.'),
        (6, 'Quỷ Lùn Đầm Lầy', 'Kẻ Đào Trộm Mộ', '🧟', 2, 1400, 1400, 'SQUAT', 0, 220, 40, 'RARE', 'Dưới bùn lầy lún sâu, đôi chân bạn phải vững chãi qua từng rep squat để đè bẹp nó.'),
        (7, 'Ma Cây Cổ Thụ', 'TRÙM RỪNG GIÀ', '🌳', 2, 2200, 2200, 'PLANK', 1, 500, 85, 'EPIC', 'Rễ cây bóp nghẹt mặt đất. Giữ vững tư thế Plank kiên định để chống lại sức ép ngàn cân!'),
        (8, 'Hỏa Lang Huyết Ngục', 'Ngọn Lửa Săn Mồi', '🔥', 3, 2800, 2800, 'PUSHUP', 0, 280, 50, 'RARE', 'Lớp lông bốc cháy rừng rực. Đòi hỏi từng cú chống đẩy dứt khoát không ngơi nghỉ để dập tắt ngọn lửa.'),
        (9, 'Golem Dung Nham', 'Núi Lửa Di Động', '🌋', 3, 4000, 4000, 'SQUAT', 0, 360, 65, 'EPIC', 'Mỗi bước đi chấn động dung nham. Chân trụ thép qua các rep squat dũng mãnh mới đánh bại được.'),
        (10, 'Hắc Long Nham Thạch', 'CHÚA TỂ HỎA NGỤC', '🐉', 3, 6500, 6500, 'PUSHUP', 1, 800, 150, 'LEGENDARY', 'Rồng thần ngủ say vạn năm. Đập tan đôi cánh lửa bằng chuỗi đòn chống đẩy thần thánh!')
    ]
    c.executemany('INSERT OR REPLACE INTO monsters VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)', monsters_data)

    # Seed 6 Exercises với góc chuẩn MediaPipe
    exercises_data = [
        ('PUSHUP', 'Hít đất (Push-up)', 'Cơ ngực, Tay sau, Vai trước', 165.0, 90.0, 1.2, 0.8, 'Tư thế plank chuẩn, hạ người cho khuỷu tay gập ≤ 90° rồi duỗi thẳng lại ~165°.'),
        ('SQUAT', 'Squat', 'Cơ đùi trước, Mông, Gân kheo', 170.0, 90.0, 1.0, 1.0, 'Đứng thẳng, hạ mông xuống như ngồi ghế cho đến khi đùi song song với sàn (góc đầu gối ≤ 90°).'),
        ('SITUP', 'Gập bụng (Sit-up)', 'Cơ thẳng bụng, Cơ lõi (Core)', 160.0, 75.0, 1.1, 0.6, 'Nằm ngửa gập gối, siết cơ bụng nâng thân trên lên cho góc hông gập ≤ 75°.'),
        ('PLANK', 'Plank', 'Toàn thân, Cơ lõi, Lưng dưới', 180.0, 170.0, 1.5, 0.1, 'Giữ sống lưng và hông thẳng hàng (170° - 180°), duy trì đồng hồ bấm giờ.'),
        ('PULLUP', 'Hít xà (Pull-up)', 'Cơ xô lưng, Bắp tay trước', 160.0, 70.0, 2.0, 1.5, 'Treo người trên xà, kéo ngực lên đến khi cằm vượt quá thanh xà.'),
        ('DIP', 'Xà kép / Ghế (Dip)', 'Tay sau, Ngực dưới', 160.0, 90.0, 1.4, 0.9, 'Hạ người cho khuỷu tay gập góc 90° rồi đẩy thẳng lên.')
    ]
    c.executemany('INSERT OR REPLACE INTO exercises VALUES (?,?,?,?,?,?,?,?)', exercises_data)

    # Seed duy nhất tài khoản Quản trị viên tối cao: Hachimi (Tất cả tài khoản khác đều do người chơi tạo thật)
    c.execute('''
        INSERT OR REPLACE INTO users (id, username, password, avatar, gold, gems, xp, level, streak, total_reps, stage, role, status, email, phone) 
        VALUES (1, "Hachimi", "123456", "🦊", 3450, 85, 480, 12, 8, 840, 3, "Admin", 1, "hachimi@vigil.app", "0901234567")
    ''')
    # Xóa sạch các tài khoản ảo mẫu, chỉ giữ lại Hachimi và các tài khoản thật do người dùng tạo
    c.execute("DELETE FROM users WHERE username IN ('admin', 'ShadowBlade', 'IronSpartan', 'SpeedyHacker99')")
    c.execute("DELETE FROM anti_cheat_alerts WHERE username IN ('SpeedyHacker99', 'BotFarmMaster')")
    c.execute("UPDATE users SET role = 'Admin' WHERE LOWER(username) = 'hachimi'")
    c.execute("UPDATE users SET role = 'User' WHERE (role IS NULL OR role = '') AND LOWER(username) != 'hachimi'")

    # Seed Audit Logs ban đầu
    c.execute('SELECT COUNT(*) FROM audit_logs')
    if c.fetchone()[0] == 0:
        c.execute('''
            INSERT INTO audit_logs (admin_username, action, target_name, details, ip_address, created_at)
            VALUES ("Admin", "CREATE_MONSTER", "Hắc Long Nham Thạch", "Khởi tạo Boss Thế Giới Chương 3: 6500 HP", "127.0.0.1", datetime("now"))
        ''')
        c.execute('''
            INSERT INTO audit_logs (admin_username, action, target_name, details, ip_address, created_at)
            VALUES ("Admin", "UPDATE_EXERCISE", "Hít đất (Push-up)", "Cập nhật góc gập chuẩn contract = 90.0°", "127.0.0.1", datetime("now"))
        ''')
        c.execute('''
            INSERT INTO audit_logs (admin_username, action, target_name, details, ip_address, created_at)
            VALUES ("Admin", "LOCK_USER", "SpeedyHacker99", "Khóa tài khoản lý do: Gian lận số rep MediaPipe", "127.0.0.1", datetime("now"))
        ''')

    # Seed default guild and member for user 1
    c.execute('INSERT OR IGNORE INTO guilds (id, name, slogan, badge, level, leader_id, created_at) VALUES (1, "Thiên Đình Vigil", "Tập luyện bất tử - Đồ sát cự long", "🛡️", 5, 1, datetime("now"))')
    c.execute('INSERT OR IGNORE INTO guild_members (id, guild_id, user_id, role, joined_at) VALUES (1, 1, 1, "LEADER", datetime("now"))')

    c.execute('SELECT COUNT(*) FROM guild_boss')
    if c.fetchone()[0] == 0:
        c.execute('''
            INSERT INTO guild_boss (id, guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id, created_at)
            VALUES (1, 1, "boss_nether_dragon", "Hắc Long Viễn Cổ - Nidhogg", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐉", 500000, 500000, "ACTIVE", 15000, 350, "weapon_dragon_slayer", datetime("now"))
        ''')

    # Seed workouts mẫu
    c.execute('SELECT COUNT(*) FROM workouts')
    if c.fetchone()[0] == 0:
        c.execute('INSERT INTO workouts (user_id, exercise, reps, hold_seconds, score, xp_earned, gold_earned, created_at) VALUES (1, "Hít đất (Push-up)", 45, 0, 45, 360, 90, datetime("now", "-10 minutes"))')
        c.execute('INSERT INTO workouts (user_id, exercise, reps, hold_seconds, score, xp_earned, gold_earned, created_at) VALUES (3, "Squat", 60, 0, 60, 480, 120, datetime("now", "-25 minutes"))')
        c.execute('INSERT INTO workouts (user_id, exercise, reps, hold_seconds, score, xp_earned, gold_earned, created_at) VALUES (2, "Gập bụng (Sit-up)", 35, 0, 35, 280, 70, datetime("now", "-1 hour"))')

    conn.commit()
    conn.close()


class RequestHandler(BaseHTTPRequestHandler):
    def _send_json(self, data, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))

    def do_OPTIONS(self):
        self._send_json({'status': 'ok'})

    def do_GET(self):
        clean_path = self.path.split('?')[0]

        # 1. PHỤC VỤ STATIC FILES CHO WEB ADMIN DASHBOARD
        if clean_path in ['/admin', '/admin/']:
            admin_index = os.path.join(ADMIN_DIR, 'index.html')
            if os.path.exists(admin_index):
                self.send_response(200)
                self.send_header('Content-Type', 'text/html; charset=utf-8')
                self.end_headers()
                with open(admin_index, 'rb') as f:
                    self.wfile.write(f.read())
                return

        if clean_path.startswith('/admin/'):
            rel_path = clean_path[len('/admin/'):]
            file_path = os.path.join(ADMIN_DIR, rel_path)
            if os.path.exists(file_path) and os.path.isfile(file_path):
                ext = os.path.splitext(file_path)[1].lower()
                ctypes = {
                    '.html': 'text/html; charset=utf-8',
                    '.css': 'text/css; charset=utf-8',
                    '.js': 'application/javascript; charset=utf-8',
                    '.json': 'application/json',
                    '.png': 'image/png',
                    '.jpg': 'image/jpeg',
                    '.svg': 'image/svg+xml'
                }
                ctype = ctypes.get(ext, 'application/octet-stream')
                self.send_response(200)
                self.send_header('Content-Type', ctype)
                self.end_headers()
                with open(file_path, 'rb') as f:
                    self.wfile.write(f.read())
                return

        if clean_path.startswith('/css/') or clean_path.startswith('/js/'):
            file_path = os.path.join(ADMIN_DIR, clean_path.lstrip('/'))
            if os.path.exists(file_path) and os.path.isfile(file_path):
                ext = os.path.splitext(file_path)[1].lower()
                ctype = 'text/css; charset=utf-8' if ext == '.css' else 'application/javascript; charset=utf-8'
                self.send_response(200)
                self.send_header('Content-Type', ctype)
                self.end_headers()
                with open(file_path, 'rb') as f:
                    self.wfile.write(f.read())
                return

        # 2. XỬ LÝ DATABASE CHO CÁC API
        conn = sqlite3.connect(DB_FILE)
        conn.row_factory = sqlite3.Row
        c = conn.cursor()

        if clean_path == '/':
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.end_headers()
            html = f"""<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Vigil RPG Fitness - Backend & Admin Portal</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0D0B1E; color: #F5EBDD; padding: 40px; }}
        .card {{ background: #181530; border: 1px solid #2A2650; border-radius: 16px; padding: 28px; max-width: 680px; margin: 0 auto; box-shadow: 0 8px 32px rgba(0,0,0,0.4); }}
        h1 {{ color: #FF6B35; margin-top: 0; display: flex; align-items: center; gap: 10px; }}
        .badge {{ background: #4CC38A; color: #0D0B1E; padding: 4px 10px; border-radius: 8px; font-weight: bold; font-size: 13px; }}
        a {{ color: #4DA8E8; text-decoration: none; font-weight: bold; }}
        a:hover {{ text-decoration: underline; }}
        .btn-admin {{ display: inline-flex; align-items: center; gap: 8px; background: linear-gradient(135deg, #8B5CF6, #FF5A7A); color: #FFF; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: bold; margin-top: 15px; box-shadow: 0 4px 15px rgba(255, 90, 122, 0.3); }}
        .btn-admin:hover {{ transform: translateY(-2px); text-decoration: none; }}
        ul {{ line-height: 1.8; }}
        .note {{ background: #0F0D22; padding: 14px; border-radius: 10px; border-left: 4px solid #FFC94D; margin-top: 20px; font-size: 13px; color: #8E88B3; }}
    </style>
</head>
<body>
    <div class="card">
        <h1>🛡️ Vigil Backend & Admin Console</h1>
        <p><span class="badge">● ĐANG HOẠT ĐỘNG</span> Máy chủ REST API và Web Admin Dashboard đã sẵn sàng!</p>
        
        <div style="background: rgba(139, 92, 246, 0.15); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: 12px; padding: 18px; margin: 20px 0;">
            <h3 style="margin-top:0; color:#FF5A7A;">👑 Bảng Quản Trị Hệ Thống (Admin Dashboard)</h3>
            <p style="font-size:14px; margin-bottom:12px;">Đầy đủ các chức năng FR8.1, FR8.2, FR8.3, UC08 theo Báo cáo Chương 2:</p>
            <a href="/admin" class="btn-admin">🚀 Mở Giao Diện Web Admin Dashboard (/admin)</a>
        </div>

        <p><strong>Các đường dẫn API có sẵn:</strong></p>
        <ul>
            <li>Kiểm tra trạng thái: <a href="/api/health" target="_blank">/api/health</a></li>
            <li>Tổng quan Admin (FR8.3): <a href="/api/admin/overview" target="_blank">/api/admin/overview</a></li>
            <li>Quản lý Người chơi (FR8.1): <a href="/api/admin/users" target="_blank">/api/admin/users</a></li>
            <li>Quản lý Quái vật (FR8.2): <a href="/api/admin/monsters" target="_blank">/api/admin/monsters</a></li>
            <li>Cửa Hàng & Vật phẩm: <a href="/api/admin/items" target="_blank">/api/admin/items</a></li>
            <li>Cấu hình Bài tập AI: <a href="/api/admin/exercises" target="_blank">/api/admin/exercises</a></li>
            <li>Cảnh báo Gian lận: <a href="/api/admin/anti-cheat" target="_blank">/api/admin/anti-cheat</a></li>
            <li>Nhật ký Kiểm toán: <a href="/api/admin/audit-logs" target="_blank">/api/admin/audit-logs</a></li>
        </ul>
    </div>
</body>
</html>"""
            self.wfile.write(html.encode('utf-8'))
            conn.close()
            return

        elif clean_path == '/api/health':
            self._send_json({'status': 'ok', 'mode': 'python-sql', 'port': PORT, 'admin': 'ready'})

        # -------------------------------------------------------------
        # ADMIN API ENDPOINTS (FR8.1, FR8.2, FR8.3, UC08)
        # -------------------------------------------------------------
        elif clean_path == '/api/admin/overview':
            # 1. Total users
            c.execute('SELECT COUNT(*) FROM users')
            total_users = c.fetchone()[0]

            # 2. DAU
            c.execute('SELECT COUNT(DISTINCT user_id) FROM workouts WHERE date(created_at) = date("now")')
            dau_today = c.fetchone()[0] or 1

            # 3. Total reps & calories
            c.execute('SELECT SUM(total_reps) FROM users')
            total_reps = c.fetchone()[0] or 0
            total_calories = int(total_reps * 0.8)

            # 4. Total workouts
            c.execute('SELECT COUNT(*) FROM workouts')
            total_workouts = c.fetchone()[0]

            # 5. Users list
            c.execute('SELECT id, username, email, phone, level, xp, gold, gems, streak, total_reps, stage, role, status, avatar, lock_reason FROM users ORDER BY id ASC')
            users_list = [dict(r) for r in c.fetchall()]

            # 6. Monsters list
            c.execute('SELECT id, name, title, avatar, stage_id as stageId, max_hp as maxHp, exercise, is_boss as isBoss, exp_reward as expReward, gold_reward as goldReward, drop_rarity as dropRarity, lore FROM monsters ORDER BY stage_id ASC, id ASC')
            monsters_list = [dict(r) for r in c.fetchall()]
            for m in monsters_list:
                m['isBoss'] = bool(m['isBoss'])

            # 7. Items list
            c.execute('SELECT * FROM items ORDER BY price_gold ASC')
            items_list = [dict(r) for r in c.fetchall()]

            # 8. Exercises list
            c.execute('SELECT id, name, target_muscle as targetMuscle, extend_angle as extendAngle, contract_angle as contractAngle, damage_factor as damageFactor, calories_per_rep as caloriesPerRep, description FROM exercises')
            exercises_list = [dict(r) for r in c.fetchall()]

            # 9. Anti-cheat alerts
            c.execute('SELECT id, user_id as userId, username, exercise, reps, duration_seconds as durationSeconds, flag_reason as flagReason, severity, status, created_at as timestamp FROM anti_cheat_alerts ORDER BY id DESC')
            cheat_alerts = [dict(r) for r in c.fetchall()]

            # 10. Audit logs
            c.execute('SELECT id, admin_username as adminUsername, action, target_name as targetName, details, ip_address as ipAddress, created_at as timestamp FROM audit_logs ORDER BY id DESC LIMIT 50')
            audit_logs = [dict(r) for r in c.fetchall()]

            # 11. Recent workouts
            c.execute('''
                SELECT w.id, u.username, w.exercise, w.reps, (w.reps * 0.8) as calories, w.gold_earned as goldEarned, w.xp_earned as xpEarned, w.created_at as timestamp
                FROM workouts w
                JOIN users u ON w.user_id = u.id
                ORDER BY w.id DESC LIMIT 10
            ''')
            recent_workouts = [dict(r) for r in c.fetchall()]

            self._send_json({
                'overview': {
                    'totalUsers': total_users,
                    'dau': max(dau_today, 3),
                    'totalReps': total_reps,
                    'totalCalories': total_calories,
                    'totalWorkouts': total_workouts,
                    'winRate': 89.4
                },
                'users': users_list,
                'monsters': monsters_list,
                'items': items_list,
                'exercises': exercises_list,
                'antiCheatAlerts': cheat_alerts,
                'auditLogs': audit_logs,
                'recentWorkouts': recent_workouts
            })

        elif clean_path == '/api/admin/users':
            c.execute('SELECT id, username, email, phone, level, xp, gold, gems, streak, total_reps, stage, role, status, avatar, lock_reason FROM users ORDER BY id ASC')
            self._send_json([dict(r) for r in c.fetchall()])

        elif clean_path in ['/api/admin/monsters', '/api/monsters']:
            c.execute('SELECT id, name, title, avatar, stage_id as stageId, max_hp as maxHp, exercise, is_boss as isBoss, exp_reward as expReward, gold_reward as goldReward, drop_rarity as dropRarity, lore FROM monsters ORDER BY stage_id ASC, id ASC')
            rows = [dict(r) for r in c.fetchall()]
            for r in rows:
                r['isBoss'] = bool(r['isBoss'])
            self._send_json(rows)

        elif clean_path == '/api/admin/items':
            c.execute('SELECT * FROM items ORDER BY price_gold ASC')
            self._send_json([dict(r) for r in c.fetchall()])

        elif clean_path in ['/api/admin/exercises', '/api/exercises']:
            c.execute('SELECT id, name, target_muscle as targetMuscle, extend_angle as extendAngle, contract_angle as contractAngle, damage_factor as damageFactor, calories_per_rep as caloriesPerRep, description FROM exercises')
            self._send_json([dict(r) for r in c.fetchall()])

        elif clean_path == '/api/admin/anti-cheat':
            c.execute('SELECT id, user_id as userId, username, exercise, reps, duration_seconds as durationSeconds, flag_reason as flagReason, severity, status, created_at as timestamp FROM anti_cheat_alerts ORDER BY id DESC')
            self._send_json([dict(r) for r in c.fetchall()])

        elif clean_path == '/api/admin/audit-logs':
            c.execute('SELECT id, admin_username as adminUsername, action, target_name as targetName, details, ip_address as ipAddress, created_at as timestamp FROM audit_logs ORDER BY id DESC LIMIT 100')
            self._send_json([dict(r) for r in c.fetchall()])

        # -------------------------------------------------------------
        # APP CLIENT API ENDPOINTS
        # -------------------------------------------------------------
        elif clean_path == '/api/shop/items':
            c.execute('SELECT * FROM items ORDER BY price_gold ASC')
            rows = [dict(r) for r in c.fetchall()]
            self._send_json(rows)

        elif clean_path.startswith('/api/user/'):
            username = clean_path.split('/')[-1]
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user:
                role = "Admin" if username.lower() == "hachimi" else "User"
                c.execute('INSERT INTO users (username, role, status) VALUES (?, ?, 1)', (username, role))
                conn.commit()
                c.execute('SELECT * FROM users WHERE username = ?', (username,))
                user = c.fetchone()

            user_dict = dict(user)
            if user_dict.get('status') == 0:
                reason = user_dict.get('lock_reason') or 'Vi phạm quy định hệ thống / gian lận'
                self._send_json({
                    'error': 'BANNED',
                    'isBanned': True,
                    'message': f"⛔ TÀI KHOẢN ĐÃ BỊ KHÓA!\n\nLý do: {reason}\n\nVui lòng liên hệ Quản trị viên để được hỗ trợ mở khóa.",
                    'lockReason': reason,
                    'user': user_dict
                }, 403)
                conn.close()
                return

            c.execute('''
                SELECT ui.id as inv_id, ui.is_equipped, i.* 
                FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.user_id = ?
            ''', (user_dict['id'],))
            inventory = [dict(r) for r in c.fetchall()]
            self._send_json({'user': user_dict, 'inventory': inventory})

        elif clean_path.startswith('/api/guild/boss'):
            parsed = urlparse(self.path)
            qs = parse_qs(parsed.query)
            guild_id = int(qs.get('guildId', ['1'])[0])
            username = qs.get('username', [''])[0]

            c.execute('SELECT * FROM guild_boss WHERE guild_id = ? ORDER BY id DESC LIMIT 1', (guild_id,))
            boss = c.fetchone()
            if not boss:
                c.execute('''
                    INSERT INTO guild_boss (guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id, created_at)
                    VALUES (?, "boss_nether_dragon", "Hắc Long Viễn Cổ - Nidhogg", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐉", 500000, 500000, "ACTIVE", 15000, 350, "weapon_dragon_slayer", datetime("now"))
                ''', (guild_id,))
                conn.commit()
                c.execute('SELECT * FROM guild_boss WHERE id = last_insert_rowid()')
                boss = c.fetchone()

            boss_dict = dict(boss)
            c.execute('''
                SELECT gbd.user_id, u.username, u.avatar, u.level, gbd.damage, gbd.reps_contributed, gbd.has_claimed_defeat_reward
                FROM guild_boss_damage gbd
                JOIN users u ON gbd.user_id = u.id
                WHERE gbd.boss_db_id = ?
                ORDER BY gbd.damage DESC
            ''', (boss_dict['id'],))
            rows = c.fetchall()
            total_damage = sum(r['damage'] for r in rows)
            contributors = []
            my_contrib = {'damage': 0, 'reps': 0, 'percentage': 0, 'rank': 0, 'hasClaimedDefeatReward': False}
            for idx, r in enumerate(rows):
                p = round((r['damage'] / total_damage * 100), 1) if total_damage > 0 else 0
                item = {
                    'userId': r['user_id'],
                    'username': r['username'],
                    'avatar': r['avatar'] or '🧑‍🎤',
                    'level': r['level'] or 1,
                    'damage': r['damage'],
                    'reps': r['reps_contributed'],
                    'percentage': p,
                    'rank': idx + 1,
                    'hasClaimed': bool(r['has_claimed_defeat_reward'])
                }
                contributors.append(item)
                if username and r['username'].lower() == username.lower():
                    my_contrib = {
                        'damage': item['damage'],
                        'reps': item['reps'],
                        'percentage': item['percentage'],
                        'rank': item['rank'],
                        'hasClaimedDefeatReward': item['hasClaimed']
                    }

            c.execute('SELECT name FROM items WHERE id = ?', (boss_dict['reward_item_id'],))
            item_row = c.fetchone()
            reward_item_name = item_row['name'] if item_row else 'Vật Phẩm Huyền Thoại'

            self._send_json({
                'success': True,
                'boss': {
                    'id': boss_dict['id'],
                    'guildId': boss_dict['guild_id'],
                    'bossId': boss_dict['boss_id'],
                    'name': boss_dict['boss_name'],
                    'title': boss_dict['boss_title'],
                    'avatar': boss_dict['boss_avatar'],
                    'maxHp': boss_dict['max_hp'],
                    'currentHp': boss_dict['current_hp'],
                    'status': boss_dict['status'],
                    'rewardGold': boss_dict['reward_gold'],
                    'rewardGems': boss_dict['reward_gems'],
                    'rewardItemId': boss_dict['reward_item_id'],
                    'rewardItemName': reward_item_name
                },
                'contributors': contributors,
                'myContribution': my_contrib
            })

        elif clean_path.startswith('/api/guilds/applications/'):
            try:
                guild_id = int(clean_path.split('/')[-1])
            except Exception:
                guild_id = 1
            c.execute('''
                SELECT r.id, r.guild_id, r.user_id, r.status, r.created_at,
                       u.username, u.avatar, u.level, u.total_reps, g.name as guild_name
                FROM guild_join_requests r
                JOIN users u ON r.user_id = u.id
                JOIN guilds g ON r.guild_id = g.id
                WHERE r.guild_id = ? AND r.status = 'PENDING'
                ORDER BY r.id DESC
            ''', (guild_id,))
            rows = [dict(r) for r in c.fetchall()]
            self._send_json({'success': True, 'applications': rows})

        else:
            self._send_json({'error': 'Not found'}, 404)

        conn.close()

    def do_POST(self):
        length = int(self.headers.get('content-length', 0))
        body = json.loads(self.rfile.read(length)) if length > 0 else {}
        conn = sqlite3.connect(DB_FILE)
        conn.row_factory = sqlite3.Row
        c = conn.cursor()

        # -------------------------------------------------------------
        # XÁC THỰC NGƯỜI DÙNG CLIENT APP (/api/auth/register, /api/auth/login)
        # -------------------------------------------------------------
        if self.path == '/api/auth/register':
            username = body.get('username', '').strip()
            password = body.get('password', '').strip() or '123456'
            avatar = body.get('avatar', '🧑‍🎤')
            if not username:
                self._send_json({'success': False, 'error': 'Tên tài khoản không được để trống!'}, 400)
                conn.close()
                return

            c.execute('SELECT id FROM users WHERE LOWER(username) = LOWER(?)', (username,))
            if c.fetchone():
                self._send_json({'success': False, 'error': f'Tài khoản "{username}" đã tồn tại! Vui lòng chọn tên khác.'}, 400)
                conn.close()
                return

            role = "Admin" if username.lower() == "hachimi" else "User"
            c.execute('''
                INSERT INTO users (username, password, avatar, role, status, gold, gems, xp, level, streak, total_reps, stage, email, phone)
                VALUES (?, ?, ?, ?, 1, 0, 0, 0, 1, 0, 0, 1, ?, ?)
            ''', (username, password, avatar, role, f"{username.lower()}@vigil.app", "0900000000"))
            new_id = c.lastrowid

            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Hệ thống', 'REGISTER_USER', username, f'Người chơi mới tạo tài khoản (ID #{new_id}, Vai trò: {role})'))
            conn.commit()

            c.execute('SELECT * FROM users WHERE id = ?', (new_id,))
            created_user = dict(c.fetchone())
            self._send_json({
                'success': True,
                'message': f'Đăng ký tài khoản {username} thành công!',
                'user': created_user
            })

        elif self.path == '/api/auth/login':
            username = body.get('username', '').strip()
            password = body.get('password', '').strip()
            c.execute('SELECT * FROM users WHERE LOWER(username) = LOWER(?)', (username,))
            user = c.fetchone()
            if not user:
                self._send_json({'success': False, 'error': f'Tài khoản "{username}" không tồn tại!'}, 404)
                conn.close()
                return

            # KIỂM TRA TÀI KHOẢN BỊ BAN / KHÓA
            if user['status'] == 0:
                reason = user['lock_reason'] or 'Vi phạm quy định hệ thống / gian lận'
                self._send_json({
                    'success': False,
                    'isBanned': True,
                    'error': f'⛔ TÀI KHOẢN ĐÃ BỊ KHÓA!\n\nLý do: {reason}\n\nTài khoản của bạn đã bị Quản trị viên cấm đăng nhập.',
                    'lockReason': reason,
                    'user': dict(user)
                }, 403)
                conn.close()
                return

            if user['password'] and password and user['password'] != password:
                self._send_json({'success': False, 'error': 'Mật khẩu không chính xác!'}, 401)
                conn.close()
                return

            self._send_json({
                'success': True,
                'message': f'Đăng nhập thành công! Chào {user["username"]}!',
                'user': dict(user)
            })

        # -------------------------------------------------------------
        # ADMIN POST API ENDPOINTS (FR8.1, FR8.2, UC08)
        # -------------------------------------------------------------
        elif self.path == '/api/admin/login':
            username = body.get('username', '').strip()
            password = body.get('password', '').strip()
            c.execute('SELECT * FROM users WHERE LOWER(username) = LOWER(?)', (username,))
            user = c.fetchone()
            if not user:
                self._send_json({'success': False, 'error': f'Tài khoản "{username}" không tồn tại trong hệ thống!'}, 404)
            elif user['status'] == 0:
                reason = user['lock_reason'] or 'Vi phạm quy định hệ thống'
                self._send_json({'success': False, 'isBanned': True, 'error': f'⛔ TÀI KHOẢN ĐÃ BỊ KHÓA!\n\nLý do: {reason}'}, 403)
            elif user['role'] != 'Admin':
                self._send_json({'success': False, 'error': f'⛔ TỪ CHỐI TRUY CẬP: Tài khoản "{user["username"]}" là Người Chơi (User), không có quyền Quản Trị Viên! Chỉ nick Hachimi hoặc tài khoản được cấp quyền Admin mới được phép truy cập trang Quản Trị.'}, 403)
            elif user['password'] and password and user['password'] != password:
                self._send_json({'success': False, 'error': 'Mật khẩu quản trị viên không chính xác!'}, 401)
            else:
                self._send_json({
                    'success': True,
                    'message': f'Xin chào Quản trị viên {user["username"]}!',
                    'admin': {
                        'id': user['id'],
                        'username': user['username'],
                        'role': user['role'],
                        'avatar': user['avatar'] or '🦊'
                    }
                })

        elif self.path == '/api/admin/users/lock':
            user_id = body.get('userId')
            username = body.get('username')
            status = body.get('status', 0)
            reason = body.get('reason', 'Vi phạm quy định hệ thống')

            if username:
                c.execute('SELECT id, username FROM users WHERE LOWER(username) = LOWER(?)', (username.strip(),))
            elif user_id:
                c.execute('SELECT id, username FROM users WHERE id = ?', (user_id,))
            else:
                self._send_json({'success': False, 'error': 'Thiếu userId hoặc username!'}, 400)
                conn.close()
                return
            u = c.fetchone()
            if not u:
                self._send_json({'success': False, 'error': 'Không tìm thấy người dùng!'}, 404)
                conn.close()
                return
            if u['username'].lower() == 'hachimi' and status == 0:
                self._send_json({'success': False, 'error': 'Không thể khóa tài khoản Quản trị viên tối cao Hachimi!'}, 400)
                conn.close()
                return

            target_name = u['username']
            c.execute('UPDATE users SET status = ?, lock_reason = ? WHERE id = ?', (status, reason if status == 0 else None, u['id']))
            action = 'LOCK_USER' if status == 0 else 'UNLOCK_USER'
            details = f"Lý do: {reason}" if status == 0 else "Mở khóa tài khoản"
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin (Hachimi)', action, target_name, details))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã cập nhật trạng thái tài khoản!'})

        elif self.path == '/api/admin/users/update':
            user_id = body.get('userId')
            username = body.get('username')
            level = int(body.get('level', 1))
            gold = int(body.get('gold', 0))
            gems = int(body.get('gems', 0))
            streak = int(body.get('streak', 0))
            role = body.get('role', 'User')
            password = body.get('password')

            # Đảm bảo Hachimi luôn giữ vai trò Admin tối cao, không thể bị giáng xuống User
            if username and username.lower() == 'hachimi':
                role = 'Admin'

            # Kiểm tra vai trò cũ để ghi log phân quyền
            c.execute('SELECT role FROM users WHERE id = ?', (user_id,))
            old_row = c.fetchone()
            old_role = old_row['role'] if old_row else 'User'

            if password:
                c.execute('UPDATE users SET username=?, level=?, gold=?, gems=?, streak=?, role=?, password=? WHERE id=?',
                          (username, level, gold, gems, streak, role, password, user_id))
            else:
                c.execute('UPDATE users SET username=?, level=?, gold=?, gems=?, streak=?, role=? WHERE id=?',
                          (username, level, gold, gems, streak, role, user_id))

            detail_str = f"Level: {level}, Gold: {gold}, Gems: {gems}"
            if old_role != role:
                detail_str += f" • [PHÂN QUYỀN]: Đổi vai trò từ {old_role} sang {role}"

            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin (Hachimi)', 'UPDATE_ROLE' if old_role != role else 'UPDATE_USER', username, detail_str))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã cập nhật thông tin và phân quyền người chơi thành công!'})

        elif self.path == '/api/admin/monsters/save':
            m = body
            m_id = m.get('id')
            name = m.get('name', 'Quái vật mới')
            title = m.get('title', 'Kẻ địch')
            avatar = m.get('avatar', '👾')
            stage_id = int(m.get('stageId', 1))
            max_hp = int(m.get('maxHp', 500))
            exercise = m.get('exercise', 'PUSHUP')
            is_boss = 1 if m.get('isBoss') else 0
            exp_reward = int(m.get('expReward', 100))
            gold_reward = int(m.get('goldReward', 20))
            drop_rarity = m.get('dropRarity', 'RARE')
            lore = m.get('lore', '')

            if m_id:
                c.execute('''
                    INSERT OR REPLACE INTO monsters (id, name, title, avatar, stage_id, max_hp, current_hp, exercise, is_boss, exp_reward, gold_reward, drop_rarity, lore)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (int(m_id), name, title, avatar, stage_id, max_hp, max_hp, exercise, is_boss, exp_reward, gold_reward, drop_rarity, lore))
            else:
                c.execute('''
                    INSERT INTO monsters (name, title, avatar, stage_id, max_hp, current_hp, exercise, is_boss, exp_reward, gold_reward, drop_rarity, lore)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (name, title, avatar, stage_id, max_hp, max_hp, exercise, is_boss, exp_reward, gold_reward, drop_rarity, lore))
            
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin', 'SAVE_MONSTER', name, f"HP: {max_hp}, Bài tập: {exercise}, Chương: {stage_id}"))
            conn.commit()
            self._send_json({'success': True, 'message': f'Đã lưu cấu hình quái vật "{name}" vào CSDL thành công!'})

        elif self.path == '/api/admin/monsters/delete':
            m_id = body.get('id')
            c.execute('SELECT name FROM monsters WHERE id = ?', (m_id,))
            row = c.fetchone()
            name = row['name'] if row else f'#{m_id}'
            c.execute('DELETE FROM monsters WHERE id = ?', (m_id,))
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin', 'DELETE_MONSTER', name, 'Xóa khỏi hệ thống'))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã xóa quái vật!'})

        elif self.path == '/api/admin/items/save':
            item = body
            c.execute('''
                INSERT OR REPLACE INTO items (id, name, slot, rarity, price_gold, price_gems, bonus_str, bonus_end, bonus_pre, bonus_luck, description, icon)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (item.get('id'), item.get('name'), item.get('slot'), item.get('rarity'), int(item.get('price_gold', 0)),
                  int(item.get('price_gems', 0)), int(item.get('bonus_str', 0)), int(item.get('bonus_end', 0)), int(item.get('bonus_pre', 0)),
                  int(item.get('bonus_luck', 0)), item.get('description', ''), item.get('icon', '⚔️')))
            
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin', 'SAVE_ITEM', item.get('name'), f"Slot: {item.get('slot')}, Giá: {item.get('price_gold')} Vàng"))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã lưu vật phẩm thành công!'})

        elif self.path == '/api/admin/items/delete':
            item_id = body.get('id')
            c.execute('DELETE FROM items WHERE id = ?', (item_id,))
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin', 'DELETE_ITEM', item_id, 'Xóa khỏi Cửa Hàng'))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã xóa vật phẩm!'})

        elif self.path == '/api/admin/exercises/save':
            ex = body
            c.execute('''
                INSERT OR REPLACE INTO exercises (id, name, target_muscle, extend_angle, contract_angle, damage_factor, calories_per_rep, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (ex.get('id'), ex.get('name'), ex.get('targetMuscle', 'Toàn thân'), float(ex.get('extendAngle', 165.0)),
                  float(ex.get('contractAngle', 90.0)), float(ex.get('damageFactor', 1.2)), float(ex.get('caloriesPerRep', 0.8)), ex.get('description', '')))
            
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin', 'UPDATE_EXERCISE', ex.get('name'), f"Góc duỗi: {ex.get('extendAngle')}°, Góc gập: {ex.get('contractAngle')}°, Hệ số: {ex.get('damageFactor')}x"))
            conn.commit()
            self._send_json({'success': True, 'message': f'Đã lưu thông số góc chuẩn AI cho bài tập {ex.get("name")}!'})

        elif self.path == '/api/admin/anti-cheat/ban':
            alert_id = body.get('alertId')
            user_id = body.get('userId')
            username = body.get('username')
            reason = body.get('reason', 'Phát hiện gian lận tốc độ rep MediaPipe (< 200ms)')

            if username and username.lower() == 'hachimi':
                self._send_json({'success': False, 'error': 'Không thể khóa tài khoản Quản trị viên tối cao Hachimi!'}, 400)
                conn.close()
                return

            if user_id:
                c.execute('UPDATE users SET status = 0, lock_reason = ? WHERE id = ?', (reason, user_id))
            elif username:
                c.execute('UPDATE users SET status = 0, lock_reason = ? WHERE LOWER(username) = LOWER(?)', (reason, username.strip()))
            else:
                self._send_json({'success': False, 'error': 'Thiếu user_id hoặc username!'}, 400)
                conn.close()
                return

            if alert_id:
                c.execute('UPDATE anti_cheat_alerts SET status = "BANNED" WHERE id = ?', (alert_id,))
            c.execute('INSERT INTO audit_logs (admin_username, action, target_name, details, created_at) VALUES (?, ?, ?, ?, datetime("now"))',
                      ('Admin (Hachimi)', 'BAN_CHEAT', username or f'ID #{user_id}', f'Khóa tài khoản vĩnh viễn lý do: {reason}'))
            conn.commit()
            self._send_json({'success': True, 'message': f'Đã khóa vĩnh viễn tài khoản gian lận [{username or user_id}]!'})

        elif self.path == '/api/admin/anti-cheat/resolve':
            alert_id = body.get('alertId')
            status = body.get('status', 'RESOLVED')
            c.execute('UPDATE anti_cheat_alerts SET status = ? WHERE id = ?', (status, alert_id))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã xử lý cảnh báo!'})

        # -------------------------------------------------------------
        # APP CLIENT POST API ENDPOINTS
        # -------------------------------------------------------------
        elif self.path == '/api/shop/buy':
            username = body.get('username', 'Hachimi')
            item_id = body.get('itemId')
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            c.execute('SELECT * FROM items WHERE id = ?', (item_id,))
            item = c.fetchone()

            if not user or not item:
                self._send_json({'error': 'Không tìm thấy'}, 404)
            elif user['gold'] < item['price_gold'] or user['gems'] < item['price_gems']:
                self._send_json({'error': 'Không đủ tiền!'}, 400)
            else:
                c.execute('UPDATE users SET gold = gold - ?, gems = gems - ? WHERE id = ?', 
                          (item['price_gold'], item['price_gems'], user['id']))
                c.execute('INSERT INTO user_inventory (user_id, item_id, is_equipped, acquired_at) VALUES (?, ?, 0, ?)',
                          (user['id'], item['id'], datetime.now().isoformat()))
                conn.commit()
                self._send_json({'success': True, 'message': f'Đã mua {item["name"]}!'})

        elif self.path == '/api/inventory/equip':
            inv_id = body.get('invId')
            username = body.get('username', 'Hachimi')
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            c.execute('''
                SELECT ui.*, i.slot FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.id = ? AND ui.user_id = ?
            ''', (inv_id, user['id']))
            inv = c.fetchone()
            if inv:
                c.execute('''
                    UPDATE user_inventory SET is_equipped = 0
                    WHERE user_id = ? AND item_id IN (SELECT id FROM items WHERE slot = ?)
                ''', (user['id'], inv['slot']))
                c.execute('UPDATE user_inventory SET is_equipped = 1 WHERE id = ?', (inv_id,))
                conn.commit()
                self._send_json({'success': True, 'message': 'Đã trang bị!'})
            else:
                self._send_json({'error': 'Không tìm thấy'}, 404)

        elif self.path == '/api/inventory/unequip':
            inv_id = body.get('invId')
            c.execute('UPDATE user_inventory SET is_equipped = 0 WHERE id = ?', (inv_id,))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã tháo trang bị!'})

        elif self.path == '/api/workout/finish':
            username = body.get('username', 'Hachimi')
            score = body.get('score', 0)
            reps = body.get('reps', score)
            exercise = body.get('exercise', 'PUSHUP')
            is_boss = body.get('isBoss', False)
            duration_seconds = body.get('durationSeconds', 0)

            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user or score <= 0:
                self._send_json({'error': 'Lỗi dữ liệu'}, 400)
                conn.close()
                return

            # Kiểm tra Anti-Cheat tự động (NFR1: Rep hợp lệ tối thiểu > 200ms)
            if duration_seconds > 0 and reps > 0:
                speed_per_rep = duration_seconds / reps
                if speed_per_rep < 0.20 or reps > 150:
                    c.execute('''
                        INSERT INTO anti_cheat_alerts (user_id, username, exercise, reps, duration_seconds, flag_reason, severity, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, "HIGH", "PENDING", datetime("now"))
                    ''', (user['id'], username, exercise, reps, duration_seconds,
                          f"Tốc độ {int(speed_per_rep * 1000)}ms/rep (< 200ms theo tiêu chuẩn NFR1)"))
                    conn.commit()

            c.execute('''
                SELECT SUM(i.bonus_luck) FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.user_id = ? AND ui.is_equipped = 1
            ''', (user['id'],))
            luck_bonus = c.fetchone()[0] or 0
            total_luck = 5 + (user['total_reps'] // 5) + luck_bonus

            base_chance = 0.75 if (is_boss or score >= 9) else 0.35
            drop_chance = min(0.95, base_chance + (total_luck * 0.005))

            dropped_item = None
            if random.random() <= drop_chance:
                roll = random.random()
                if is_boss:
                    luck_factor = total_luck * 0.001
                    if roll < 0.01 + luck_factor * 0.1: rarity = 'DIVINE'
                    elif roll < 0.04 + luck_factor * 0.2: rarity = 'ANCIENT'
                    elif roll < 0.10 + luck_factor * 0.3: rarity = 'MYTHIC'
                    elif roll < 0.22 + luck_factor * 0.4: rarity = 'LEGENDARY'
                    elif roll < 0.45: rarity = 'EPIC'
                    elif roll < 0.75: rarity = 'RARE'
                    else: rarity = 'UNCOMMON'
                else:
                    luck_factor = total_luck * 0.0005
                    if roll < 0.002 + luck_factor * 0.05: rarity = 'MYTHIC'
                    elif roll < 0.015 + luck_factor * 0.1: rarity = 'LEGENDARY'
                    elif roll < 0.08 + luck_factor * 0.2: rarity = 'EPIC'
                    elif roll < 0.25: rarity = 'RARE'
                    elif roll < 0.60: rarity = 'UNCOMMON'
                    else: rarity = 'COMMON'

                c.execute('SELECT * FROM items WHERE rarity = ? ORDER BY RANDOM() LIMIT 1', (rarity,))
                item_row = c.fetchone()
                if not item_row:
                    c.execute('SELECT * FROM items ORDER BY RANDOM() LIMIT 1')
                    item_row = c.fetchone()

                if item_row:
                    dropped_item = dict(item_row)
                    c.execute('INSERT INTO user_inventory (user_id, item_id, is_equipped, acquired_at) VALUES (?, ?, 0, ?)',
                              (user['id'], dropped_item['id'], datetime.now().isoformat()))

            xp_earned = score * 15 if is_boss else score * 8
            gold_earned = score * 4 if is_boss else score * 2

            new_gold = user['gold'] + gold_earned
            new_xp = user['xp'] + xp_earned
            new_level = user['level']
            new_reps = user['total_reps'] + score
            new_stage = user['stage']

            if is_boss and score >= 10:
                new_stage = min(4, new_stage + 1)

            xp_needed = 80 + new_level * 160
            while new_xp >= xp_needed:
                new_xp -= xp_needed
                new_level += 1
                xp_needed = 80 + new_level * 160

            today_str = date.today().isoformat()
            new_streak = user['streak']
            if user['last_workout_date'] != today_str:
                new_streak += 1

            c.execute('''
                UPDATE users 
                SET gold = ?, xp = ?, level = ?, streak = ?, total_reps = ?, stage = ?, last_workout_date = ?
                WHERE id = ?
            ''', (new_gold, new_xp, new_level, new_streak, new_reps, new_stage, today_str, user['id']))

            c.execute('''
                INSERT INTO workouts (user_id, exercise, reps, hold_seconds, score, xp_earned, gold_earned, dropped_item_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (user['id'], exercise, reps, 0, score, xp_earned, gold_earned,
                  dropped_item['id'] if dropped_item else None, datetime.now().isoformat()))

            conn.commit()
            self._send_json({
                'success': True,
                'score': score,
                'xpEarned': xp_earned,
                'goldEarned': gold_earned,
                'newLevel': new_level,
                'newGold': new_gold,
                'newXp': new_xp,
                'newStreak': new_streak,
                'droppedItem': dropped_item
            })

        elif self.path == '/api/guild/boss/attack':
            username = body.get('username', 'Hachimi')
            damage = int(body.get('damage', 100))
            reps = int(body.get('reps', 10))
            guild_id = int(body.get('guildId', 1))

            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            c.execute('SELECT * FROM guild_boss WHERE guild_id = ? AND status = "ACTIVE" ORDER BY id DESC LIMIT 1', (guild_id,))
            boss = c.fetchone()

            if not user or not boss:
                self._send_json({'error': 'Boss đã bị hạ gục hoặc không tồn tại!'}, 400)
                conn.close()
                return

            new_hp = max(0, boss['current_hp'] - damage)
            new_status = 'DEFEATED' if new_hp == 0 else 'ACTIVE'
            defeated_at = datetime.now().isoformat() if new_hp == 0 else None

            c.execute('UPDATE guild_boss SET current_hp = ?, status = ?, defeated_at = ? WHERE id = ?',
                      (new_hp, new_status, defeated_at, boss['id']))

            c.execute('SELECT * FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?', (boss['id'], user['id']))
            existing = c.fetchone()
            if existing:
                c.execute('UPDATE guild_boss_damage SET damage = damage + ?, reps_contributed = reps_contributed + ? WHERE id = ?',
                          (damage, reps, existing['id']))
            else:
                c.execute('INSERT INTO guild_boss_damage (guild_id, boss_db_id, user_id, damage, reps_contributed) VALUES (?, ?, ?, ?, ?)',
                          (guild_id, boss['id'], user['id'], damage, reps))

            gold_bonus = reps * 3
            xp_bonus = reps * 5
            c.execute('UPDATE users SET gold = gold + ?, xp = xp + ?, total_reps = total_reps + ? WHERE id = ?',
                      (gold_bonus, xp_bonus, reps, user['id']))

            conn.commit()
            self._send_json({
                'success': True,
                'bossCurrentHp': new_hp,
                'bossMaxHp': boss['max_hp'],
                'isDefeated': new_hp == 0,
                'damageDealt': damage,
                'goldBonus': gold_bonus,
                'xpBonus': xp_bonus
            })

        elif self.path == '/api/guild/boss/claim':
            username = body.get('username', 'Hachimi')
            guild_id = int(body.get('guildId', 1))
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            c.execute('SELECT * FROM guild_boss WHERE guild_id = ? AND status = "DEFEATED" ORDER BY id DESC LIMIT 1', (guild_id,))
            boss = c.fetchone()

            if not user or not boss:
                self._send_json({'error': 'Boss chưa bị hạ gục hoặc không tìm thấy!'}, 400)
                conn.close()
                return

            c.execute('SELECT * FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?', (boss['id'], user['id']))
            dmg_row = c.fetchone()
            if not dmg_row:
                self._send_json({'error': 'Bạn chưa tham gia chiến dịch diệt trùm này!'}, 400)
                conn.close()
                return

            if dmg_row['has_claimed_defeat_reward']:
                self._send_json({'error': 'Bạn đã nhận thưởng diệt trùm rồi!'}, 400)
                conn.close()
                return

            c.execute('UPDATE guild_boss_damage SET has_claimed_defeat_reward = 1 WHERE id = ?', (dmg_row['id'],))
            c.execute('UPDATE users SET gold = gold + ?, gems = gems + ? WHERE id = ?',
                      (boss['reward_gold'], boss['reward_gems'], user['id']))

            if boss['reward_item_id']:
                c.execute('INSERT INTO user_inventory (user_id, item_id, is_equipped, acquired_at) VALUES (?, ?, 0, ?)',
                          (user['id'], boss['reward_item_id'], datetime.now().isoformat()))

            c.execute('SELECT name, icon FROM items WHERE id = ?', (boss['reward_item_id'],))
            item_row = c.fetchone()
            item_name = item_row['name'] if item_row else 'Trang Bị Thần Thoại'
            item_icon = item_row['icon'] if item_row else '🎁'

            conn.commit()
            self._send_json({
                'success': True,
                'message': f'🎉 Nhận thưởng thành công: {boss["reward_gold"]} Vàng, {boss["reward_gems"]} Kim Cương và {item_icon} {item_name}!',
                'rewardGold': boss['reward_gold'],
                'rewardGems': boss['reward_gems'],
                'rewardItemId': boss['reward_item_id'],
                'rewardItemName': item_name
            })

        elif self.path == '/api/guild/boss/summon':
            guild_id = int(body.get('guildId', 1))
            templates = [
                ('boss_infernal_colossus', 'Hỏa Ma Khổng Lồ - Surtr', 'CHÚA TỂ TẬN DIỆT', '🌋', 450000, 12000, 300, 'armor_primordial'),
                ('boss_nether_dragon', 'Hắc Long Viễn Cổ - Nidhogg', 'SIÊU TRÙM THẾ GIỚI BANG HỘI', '🐉', 500000, 15000, 350, 'weapon_dragon_slayer'),
                ('boss_void_leviathan', 'Cự Thú Hư Không - Leviathan', 'THẦN THÚ BIỂN SÂU', '🦑', 600000, 20000, 400, 'weapon_god_slayer')
            ]
            tpl = random.choice(templates)
            c.execute('''
                INSERT INTO guild_boss (guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, datetime("now"))
            ''', (guild_id, tpl[0], tpl[1], tpl[2], tpl[3], tpl[4], tpl[4], tpl[5], tpl[6], tpl[7]))
            conn.commit()
            self._send_json({
                'success': True,
                'message': f"🔥 Tiếng gầm thét rung chuyển! {tpl[1]} đã giáng lâm khiêu chiến bang hội!"
            })

        elif self.path == '/api/guilds/apply':
            guild_id = int(body.get('guildId', 1))
            username = body.get('username', 'Hachimi')
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user:
                self._send_json({'error': 'Không tìm thấy người chơi!'}, 404)
                conn.close()
                return

            c.execute('SELECT * FROM guild_members WHERE user_id = ?', (user['id'],))
            if c.fetchone():
                self._send_json({'error': 'Bạn đã tham gia một bang hội rồi!'}, 400)
                conn.close()
                return

            c.execute('SELECT * FROM guild_join_requests WHERE guild_id = ? AND user_id = ? AND status = "PENDING"', (guild_id, user['id']))
            if c.fetchone():
                self._send_json({'error': 'Bạn đã gửi đơn xin vào bang này rồi, vui lòng chờ duyệt!'}, 400)
                conn.close()
                return

            c.execute('INSERT INTO guild_join_requests (guild_id, user_id, status, created_at) VALUES (?, ?, "PENDING", ?)',
                      (guild_id, user['id'], datetime.now().isoformat()))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã gửi đơn xin gia nhập! Đang chờ Chủ bang phê duyệt.'})

        elif self.path == '/api/guilds/cancel-application':
            username = body.get('username', 'Hachimi')
            guild_id = body.get('guildId')
            app_id = body.get('applicationId')
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user:
                self._send_json({'error': 'Không tìm thấy người chơi!'}, 404)
                conn.close()
                return

            if app_id:
                c.execute('DELETE FROM guild_join_requests WHERE id = ? AND user_id = ?', (app_id, user['id']))
            elif guild_id:
                c.execute('DELETE FROM guild_join_requests WHERE guild_id = ? AND user_id = ?', (guild_id, user['id']))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã hủy đơn xin gia nhập bang hội.'})

        elif self.path == '/api/guilds/approve-application':
            app_id = body.get('applicationId')
            c.execute('SELECT * FROM guild_join_requests WHERE id = ?', (app_id,))
            app_row = c.fetchone()
            if not app_row:
                self._send_json({'error': 'Không tìm thấy đơn xin gia nhập!'}, 404)
                conn.close()
                return

            c.execute('INSERT OR IGNORE INTO guild_members (guild_id, user_id, role) VALUES (?, ?, "MEMBER")',
                      (app_row['guild_id'], app_row['user_id']))
            c.execute('UPDATE guild_join_requests SET status = "ACCEPTED" WHERE id = ?', (app_id,))
            c.execute('DELETE FROM guild_join_requests WHERE user_id = ? AND id != ?', (app_row['user_id'], app_id))

            c.execute('SELECT username FROM users WHERE id = ?', (app_row['user_id'],))
            applicant = c.fetchone()
            name = applicant['username'] if applicant else 'Thành viên mới'
            conn.commit()
            self._send_json({'success': True, 'message': f'Đã phê duyệt {name} gia nhập bang hội!'})

        elif self.path == '/api/guilds/reject-application':
            app_id = body.get('applicationId')
            c.execute('UPDATE guild_join_requests SET status = "REJECTED" WHERE id = ?', (app_id,))
            conn.commit()
            self._send_json({'success': True, 'message': 'Đã từ chối đơn xin gia nhập bang hội.'})

        else:
            self._send_json({'error': 'Not found'}, 404)

        conn.close()

if __name__ == '__main__':
    import sys
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass
    init_db()
    server = HTTPServer(('0.0.0.0', PORT), RequestHandler)
    print("====================================================")
    print(f"[Vigil Backend] SQL Backend Server dang chay tai cong {PORT}")
    print(f"[Vigil Backend] Tren trinh duyet PC: http://localhost:{PORT}")
    print(f"[Vigil Backend] Web Admin Dashboard: http://localhost:{PORT}/admin")
    print(f"[Vigil Backend] Tren Android Emulator: http://10.0.2.2:{PORT}")
    print("====================================================")
    server.serve_forever()
