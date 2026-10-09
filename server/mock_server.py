# ====================================================================
# VIGIL REST API SERVER (Python 3.11 Alternative Server)
# Cung cấp các API hệt như server.js để chạy ngay lập tức với Python
# ====================================================================

import json
import random
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime, date, timedelta
import sqlite3
import os

PORT = 3000
DB_FILE = os.path.join(os.path.dirname(__file__), 'vigil_local.db')

def init_db():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            gold INTEGER DEFAULT 150,
            gems INTEGER DEFAULT 10,
            xp INTEGER DEFAULT 0,
            level INTEGER DEFAULT 1,
            streak INTEGER DEFAULT 0,
            total_reps INTEGER DEFAULT 0,
            stage INTEGER DEFAULT 1,
            last_workout_date TEXT
        )
    ''')
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
    c.execute('''
        CREATE TABLE IF NOT EXISTS user_inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            item_id TEXT,
            is_equipped INTEGER DEFAULT 0,
            acquired_at TEXT
        )
    ''')
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

    # Seed default guild and member for user 1
    c.execute('INSERT OR IGNORE INTO guilds (id, name, slogan, badge, level, leader_id, created_at) VALUES (1, "Thiên Đình Vigil", "Tập luyện bất tử - Đồ sát cự long", "🛡️", 5, 1, datetime("now"))')
    c.execute('INSERT OR IGNORE INTO guild_members (id, guild_id, user_id, role, joined_at) VALUES (1, 1, 1, "LEADER", datetime("now"))')

    # Seed default guild boss with full HP, no fake damage
    c.execute('SELECT COUNT(*) FROM guild_boss')
    if c.fetchone()[0] == 0:
        c.execute('''
            INSERT INTO guild_boss (id, guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id, created_at)
            VALUES (1, 1, "boss_nether_dragon", "Hắc Long Viễn Cổ - Nidhogg", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐉", 500000, 500000, "ACTIVE", 15000, 350, "weapon_dragon_slayer", datetime("now"))
        ''')

    c.execute('INSERT OR IGNORE INTO users (id, username, gold, gems, xp, level, streak, total_reps, stage) VALUES (1, "Hachimi", 200, 10, 0, 1, 0, 0, 1)')
    c.execute('INSERT OR IGNORE INTO user_inventory (id, user_id, item_id, is_equipped) VALUES (1, 1, "helm_bronze", 1)')

    conn.commit()
    conn.close()


class RequestHandler(BaseHTTPRequestHandler):
    def _send_json(self, data, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))

    def do_OPTIONS(self):
        self._send_json({'status': 'ok'})

    def do_GET(self):
        conn = sqlite3.connect(DB_FILE)
        conn.row_factory = sqlite3.Row
        c = conn.cursor()

        if self.path == '/':
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.end_headers()
            html = """<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Vigil RPG Fitness - Backend API</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0D0B1E; color: #F5EBDD; padding: 40px; }
        .card { background: #181530; border: 1px solid #2A2650; border-radius: 16px; padding: 24px; max-width: 650px; margin: 0 auto; box-shadow: 0 8px 32px rgba(0,0,0,0.4); }
        h1 { color: #FF6B35; margin-top: 0; }
        .badge { background: #4CC38A; color: #0D0B1E; padding: 4px 10px; border-radius: 8px; font-weight: bold; font-size: 13px; }
        a { color: #4DA8E8; text-decoration: none; font-weight: bold; }
        a:hover { text-decoration: underline; }
        ul { line-height: 1.8; }
        .note { background: #0F0D22; padding: 12px; border-radius: 10px; border-left: 4px solid #FFC94D; margin-top: 20px; font-size: 13px; color: #8E88B3; }
    </style>
</head>
<body>
    <div class="card">
        <h1>🛡️ Vigil Backend API</h1>
        <p><span class="badge">● ĐANG HOẠT ĐỘNG</span> Máy chủ SQL của Vigil đã sẵn sàng kết nối!</p>
        <p><strong>Các đường dẫn API có sẵn:</strong></p>
        <ul>
            <li>Kiểm tra trạng thái: <a href="/api/health" target="_blank">/api/health</a></li>
            <li>Danh sách Cửa Hàng (24 vật phẩm): <a href="/api/shop/items" target="_blank">/api/shop/items</a></li>
            <li>Thông tin người chơi & kho đồ: <a href="/api/user/Hachimi" target="_blank">/api/user/Hachimi</a></li>
        </ul>
        <div class="note">
            💡 <strong>Lưu ý kết nối:</strong><br>
            • Trên trình duyệt máy tính: truy cập <code>http://localhost:3000</code><br>
            • Trên máy ảo Android: app gọi <code>http://10.0.2.2:3000</code> (đã tích hợp tự động)<br>
            • Trên điện thoại thật cùng Wifi: kết nối <code>http://192.168.0.103:3000</code>
        </div>
    </div>
</body>
</html>"""
            self.wfile.write(html.encode('utf-8'))
            conn.close()
            return
        elif self.path == '/api/health':
            self._send_json({'status': 'ok', 'mode': 'python-sql', 'port': PORT})
        elif self.path == '/api/shop/items':
            c.execute('SELECT * FROM items ORDER BY price_gold ASC')
            rows = [dict(r) for r in c.fetchall()]
            self._send_json(rows)
        elif self.path.startswith('/api/user/'):
            username = self.path.split('/')[-1]
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user:
                c.execute('INSERT INTO users (username) VALUES (?)', (username,))
                conn.commit()
                c.execute('SELECT * FROM users WHERE username = ?', (username,))
                user = c.fetchone()

            user_dict = dict(user)
            c.execute('''
                SELECT ui.id as inv_id, ui.is_equipped, i.* 
                FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.user_id = ?
            ''', (user_dict['id'],))
            inventory = [dict(r) for r in c.fetchall()]
            self._send_json({'user': user_dict, 'inventory': inventory})
        elif self.path.startswith('/api/guild/boss'):
            from urllib.parse import urlparse, parse_qs
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
        elif self.path.startswith('/api/guilds/applications/'):
            # /api/guilds/applications/<guild_id>
            try:
                guild_id = int(self.path.split('/')[-1])
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

        if self.path == '/api/shop/buy':
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
                # Tháo món cùng slot
                c.execute('''
                    UPDATE user_inventory SET is_equipped = 0
                    WHERE user_id = ? AND item_id IN (SELECT id FROM items WHERE slot = ?)
                ''', (user['id'], inv['slot']))
                # Mặc món mới
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
            exercise = body.get('exercise', 'PUSHUP')
            is_boss = body.get('isBoss', False)

            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user or score <= 0:
                self._send_json({'error': 'Lỗi dữ liệu'}, 400)
                conn.close()
                return

            # Tính luck tổng
            c.execute('''
                SELECT SUM(i.bonus_luck) FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.user_id = ? AND ui.is_equipped = 1
            ''', (user['id'],))
            luck_bonus = c.fetchone()[0] or 0
            total_luck = 5 + (user['total_reps'] // 5) + luck_bonus

            # TỶ LỆ RỚT ĐỒ (Loot Drop Rate System):
            base_chance = 0.75 if (is_boss or score >= 9) else 0.35
            drop_chance = min(0.95, base_chance + (total_luck * 0.005))

            dropped_item = None
            if random.random() <= drop_chance:
                # Rarity roll (8 phẩm cấp)
                roll = random.random()
                if is_boss:
                    luck_factor = total_luck * 0.001
                    if roll < 0.01 + luck_factor * 0.1:
                        rarity = 'DIVINE'
                    elif roll < 0.04 + luck_factor * 0.2:
                        rarity = 'ANCIENT'
                    elif roll < 0.10 + luck_factor * 0.3:
                        rarity = 'MYTHIC'
                    elif roll < 0.22 + luck_factor * 0.4:
                        rarity = 'LEGENDARY'
                    elif roll < 0.45:
                        rarity = 'EPIC'
                    elif roll < 0.75:
                        rarity = 'RARE'
                    else:
                        rarity = 'UNCOMMON'
                else:
                    luck_factor = total_luck * 0.0005
                    if roll < 0.002 + luck_factor * 0.05:
                        rarity = 'MYTHIC'
                    elif roll < 0.015 + luck_factor * 0.1:
                        rarity = 'LEGENDARY'
                    elif roll < 0.08 + luck_factor * 0.2:
                        rarity = 'EPIC'
                    elif roll < 0.25:
                        rarity = 'RARE'
                    elif roll < 0.60:
                        rarity = 'UNCOMMON'
                    else:
                        rarity = 'COMMON'

                c.execute('SELECT * FROM items WHERE rarity = ? ORDER BY RANDOM() LIMIT 1', (rarity,))
                item = c.fetchone()
                if not item:
                    c.execute('SELECT * FROM items ORDER BY RANDOM() LIMIT 1')
                    item = c.fetchone()
                dropped_item = dict(item)

                # Thêm vào kho
                c.execute('INSERT INTO user_inventory (user_id, item_id, is_equipped, acquired_at) VALUES (?, ?, 0, ?)',
                          (user['id'], dropped_item['id'], datetime.now().isoformat()))

            # Cập nhật streak, xp, vàng
            today = str(date.today())
            yesterday = str(date.today() - timedelta(days=1))
            new_streak = user['streak']
            if user['last_workout_date'] != today:
                new_streak = (user['streak'] + 1) if user['last_workout_date'] == yesterday else 1

            xp_earned = score * 10
            gold_earned = score * 2
            new_xp = user['xp'] + xpEarned
            new_level = user['level']
            needed = 80 + new_level * 160
            while new_xp >= needed:
                new_xp -= needed
                new_level += 1
                needed = 80 + new_level * 160

            new_gold = user['gold'] + gold_earned
            new_total = user['total_reps'] + score
            new_stage = (user['stage'] + 1) if (score >= 9) else user['stage']

            c.execute('''
                UPDATE users SET gold=?, xp=?, level=?, streak=?, total_reps=?, stage=?, last_workout_date=?
                WHERE id=?
            ''', (new_gold, new_xp, new_level, new_streak, new_total, new_stage, today, user['id']))

            c.execute('''
                INSERT INTO workouts (user_id, exercise, reps, score, xp_earned, gold_earned, dropped_item_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (user['id'], exercise, score, score, xp_earned, gold_earned, dropped_item['id'] if dropped_item else None, datetime.now().isoformat()))

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
            guild_id = int(body.get('guildId', 1))
            username = body.get('username', 'Hachimi')
            damage = max(1, int(body.get('damage', 100)))
            reps = max(1, int(body.get('reps', 1)))
            exercise = body.get('exercise', 'SQUAT')

            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            if not user:
                self._send_json({'error': 'Không tìm thấy người chơi!'}, 404)
                conn.close()
                return

            c.execute('SELECT * FROM guild_boss WHERE guild_id = ? AND status = "ACTIVE" ORDER BY id DESC LIMIT 1', (guild_id,))
            boss = c.fetchone()
            if not boss:
                self._send_json({'error': 'Boss đã bị tiêu diệt hoặc chưa được triệu hồi!'}, 400)
                conn.close()
                return

            new_hp = max(0, boss['current_hp'] - damage)
            is_def = (new_hp == 0)
            c.execute('UPDATE guild_boss SET current_hp = ?, status = ?, defeated_at = ? WHERE id = ?',
                      (new_hp, 'DEFEATED' if is_def else 'ACTIVE', datetime.now().isoformat() if is_def else None, boss['id']))

            c.execute('SELECT * FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?', (boss['id'], user['id']))
            dmg_record = c.fetchone()
            if dmg_record:
                c.execute('UPDATE guild_boss_damage SET damage = damage + ?, reps_contributed = reps_contributed + ? WHERE id = ?',
                          (damage, reps, dmg_record['id']))
            else:
                c.execute('INSERT INTO guild_boss_damage (guild_id, boss_db_id, user_id, damage, reps_contributed) VALUES (?, ?, ?, ?, ?)',
                          (guild_id, boss['id'], user['id'], damage, reps))

            effort_gold = reps * 2
            effort_xp = reps * 5
            c.execute('UPDATE users SET gold = gold + ?, xp = xp + ?, total_reps = total_reps + ? WHERE id = ?',
                      (effort_gold, effort_xp, reps, user['id']))
            conn.commit()

            self._send_json({
                'success': True,
                'message': f"🎉 Tuyệt đỉnh! Bạn và bang hội đã kết liễu {boss['boss_name']}!" if is_def else f"⚔️ Đã gây {damage} sát thương lên {boss['boss_name']}!",
                'damageDealt': damage,
                'reps': reps,
                'bossRemainingHp': new_hp,
                'isDefeated': is_def,
                'effortGold': effort_gold,
                'effortXp': effort_xp
            })
        elif self.path == '/api/guild/boss/claim':
            guild_id = int(body.get('guildId', 1))
            username = body.get('username', 'Hachimi')
            c.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = c.fetchone()
            c.execute('SELECT * FROM guild_boss WHERE guild_id = ? AND status = "DEFEATED" ORDER BY id DESC LIMIT 1', (guild_id,))
            boss = c.fetchone()
            if not boss or not user:
                self._send_json({'error': 'Boss chưa bị tiêu diệt hoặc không tìm thấy người chơi!'}, 400)
                conn.close()
                return

            c.execute('SELECT * FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?', (boss['id'], user['id']))
            dmg_row = c.fetchone()
            if not dmg_row:
                self._send_json({'error': 'Bạn chưa tham gia đánh boss!'}, 400)
                conn.close()
                return
            if dmg_row['has_claimed_defeat_reward']:
                self._send_json({'error': 'Đã nhận thưởng rồi!'}, 400)
                conn.close()
                return

            c.execute('UPDATE users SET gold = gold + ?, gems = gems + ? WHERE id = ?',
                      (boss['reward_gold'], boss['reward_gems'], user['id']))
            c.execute('UPDATE guild_boss_damage SET has_claimed_defeat_reward = 1 WHERE id = ?', (dmg_row['id'],))
            if boss['reward_item_id']:
                c.execute('INSERT INTO user_inventory (user_id, item_id, is_equipped, acquired_at) VALUES (?, ?, 0, ?)',
                          (user['id'], boss['reward_item_id'], datetime.now().isoformat()))
            conn.commit()

            c.execute('SELECT name FROM items WHERE id = ?', (boss['reward_item_id'],))
            item_row = c.fetchone()
            item_name = item_row['name'] if item_row else 'Vật Phẩm Huyền Thoại'

            self._send_json({
                'success': True,
                'message': f"🎉 Nhận thành công {boss['reward_gold']} Vàng, {boss['reward_gems']} Kim Cương và {item_name}!",
                'rewardGold': boss['reward_gold'],
                'rewardGems': boss['reward_gems'],
                'rewardItemId': boss['reward_item_id'],
                'rewardItemName': item_name
            })
        elif self.path == '/api/guild/boss/summon':
            guild_id = int(body.get('guildId', 1))
            templates = [
                ('boss_nether_dragon', 'Hắc Long Viễn Cổ - Nidhogg', 'SIÊU TRÙM THẾ GIỚI BANG HỘI', '🐉', 500000, 15000, 350, 'weapon_dragon_slayer'),
                ('boss_inferno_titan', 'Cự Nhân Hỏa Ngục - Surtr', 'SIÊU TRÙM THẾ GIỚI BANG HỘI', '🌋', 650000, 18000, 400, 'armor_dragon_scale'),
                ('boss_void_behemoth', 'Thần Thú Hư Không - Leviathan', 'SIÊU TRÙM THẾ GIỚI BANG HỘI', '🐲', 800000, 22000, 500, 'amulet_boss_heart')
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
    print(f"[Vigil Backend] Tren Android Emulator: http://10.0.2.2:{PORT}")
    print("====================================================")
    server.serve_forever()
