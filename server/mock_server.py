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

    # Seed items
    items_data = [
        ('helm_bronze', 'Mũ Đồng Tân Binh', 'HELMET', 'COMMON', 50, 0, 0, 3, 0, 1, 'Mũ đồng đúc thô sơ.', '🪖'),
        ('helm_iron', 'Thiết Giáp Đầu', 'HELMET', 'RARE', 160, 0, 3, 7, 0, 2, 'Rèn từ sắt non tôi luyện.', '⛑️'),
        ('helm_valkyrie', 'Mũ Lông Vũ Valkyrie', 'HELMET', 'EPIC', 420, 5, 5, 14, 0, 8, 'Ban phước bởi nữ thần chiến trận.', '👑'),
        ('helm_dragon', 'Vương Miện Long Thần', 'HELMET', 'LEGENDARY', 1000, 20, 18, 25, 0, 12, 'Tỏa ra uy áp của loài rồng cổ đại.', '🐉'),
        ('armor_leather', 'Áo Da Dã Ngoại', 'ARMOR', 'COMMON', 60, 0, 0, 4, 2, 0, 'Áo da bò mềm mại, thoáng mát.', '🥋'),
        ('armor_plate', 'Chiến Giáp Thép Nung', 'ARMOR', 'RARE', 180, 0, 5, 10, 0, 0, 'Tấm giáp kiên cố bảo vệ cơ hoành.', '🛡️'),
        ('armor_shadow', 'Áo Choàng Bóng Đêm', 'ARMOR', 'EPIC', 450, 6, 10, 0, 12, 8, 'Hòa mình vào bóng tối.', '🥷'),
        ('armor_celestial', 'Thánh Giáp Quang Minh', 'ARMOR', 'LEGENDARY', 1200, 25, 20, 28, 15, 0, 'Ánh hào quang chiếu rọi.', '✨'),
        ('gloves_cloth', 'Băng Quấn Cổ Tay', 'GLOVES', 'COMMON', 40, 0, 3, 0, 2, 0, 'Bảo vệ khớp cổ tay.', '🥊'),
        ('gloves_grip', 'Găng Hít Đất Siêu Bám', 'GLOVES', 'RARE', 150, 0, 8, 0, 6, 0, 'Đế cao su hạt kim cương.', '🧤'),
        ('gloves_titan', 'Găng Titan Siêu Lực', 'GLOVES', 'EPIC', 400, 5, 18, 0, 8, 3, 'Khung titan trợ lực bùng nổ.', '🦾'),
        ('gloves_infinity', 'Găng Tay Vô Cực', 'GLOVES', 'LEGENDARY', 1100, 22, 32, 0, 16, 14, 'Nắm giữ sức mạnh vũ trụ.', '🌌'),
        ('boots_runner', 'Giày Chạy Phản Lực', 'BOOTS', 'COMMON', 45, 0, 0, 3, 0, 2, 'Êm ái, giảm chấn gối khi squat.', '👟'),
        ('boots_iron', 'Hộ Chân Chiến Binh', 'BOOTS', 'RARE', 150, 0, 6, 7, 0, 0, 'Bọc thép mũi chân vững chãi.', '🥾'),
        ('boots_winged', 'Hài Phong Thần Hermes', 'BOOTS', 'EPIC', 380, 5, 0, 15, 6, 12, 'Lướt đi nhẹ tựa lông hồng.', '🪽'),
        ('boots_abyss', 'Bộ Bước Vực Thẳm', 'BOOTS', 'LEGENDARY', 950, 18, 18, 24, 0, 15, 'Mỗi bước chân để lại uy chấn.', '⚡'),
        ('amulet_stone', 'Bùa Đá May Mắn', 'AMULET', 'COMMON', 50, 0, 0, 0, 0, 4, 'Hòn đá cuội ven suối đem lại vận may.', '🪬'),
        ('amulet_ruby', 'Huyết Ngọc Hồi Phục', 'AMULET', 'RARE', 190, 0, 6, 6, 0, 5, 'Viên hồng ngọc đẩy nhanh phục hồi.', '🔮'),
        ('amulet_eye', 'Mắt Ưng Tinh Anh', 'AMULET', 'EPIC', 480, 7, 10, 0, 16, 10, 'Giúp nhìn rõ từng biên độ góc khớp.', '👁️'),
        ('amulet_sun', 'Thái Dương Cổ Thạch', 'AMULET', 'LEGENDARY', 1300, 30, 20, 20, 20, 25, 'Cội nguồn sinh lực vĩnh cửu.', '☀️'),
        ('weapon_stick', 'Côn Gỗ Luyện Tập', 'WEAPON', 'COMMON', 50, 0, 4, 0, 2, 0, 'Khúc gỗ sồi chắc nịch.', '🪵'),
        ('weapon_sword', 'Thanh Kiếm Thép Đúc', 'WEAPON', 'RARE', 180, 0, 10, 0, 6, 0, 'Lưỡi kiếm sắc bén rèn từ lò luyện kim.', '⚔️'),
        ('weapon_axe', 'Rìu Chiến Berserker', 'WEAPON', 'EPIC', 460, 6, 22, 8, 0, 0, 'Chiếc rìu khổng lồ của chiến binh cuồng nộ.', '🪓'),
        ('weapon_excalibur', 'Thánh Kiếm Excalibur', 'WEAPON', 'LEGENDARY', 1400, 30, 35, 0, 18, 15, 'Bảo kiếm huyền thoại rút từ trong đá.', '🗡️')
    ]
    c.executemany('INSERT OR IGNORE INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?)', items_data)

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
                # Rarity roll
                roll = random.random()
                if roll < 0.05 + total_luck * 0.001:
                    rarity = 'LEGENDARY'
                elif roll < 0.25 + total_luck * 0.002:
                    rarity = 'EPIC'
                elif roll < 0.60:
                    rarity = 'RARE'
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
