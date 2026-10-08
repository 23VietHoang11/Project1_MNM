const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Cấu hình kết nối MySQL
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'vigil_db',
    port: process.env.DB_PORT || 3306,
    multipleStatements: true
};

let pool = null;
let useFallback = false;

// Dữ liệu bộ nhớ tạm phòng khi người dùng chưa bật MySQL (Tránh crash server)
let memoryDB = {
    users: [{ id: 1, username: 'Hachimi', gold: 200, gems: 10, xp: 0, level: 1, streak: 1, total_reps: 0, stage: 1 }],
    items: [],
    inventory: [{ id: 1, user_id: 1, item_id: 'helm_bronze', is_equipped: 1 }],
    workouts: []
};

// Khởi tạo Database
async function initDatabase() {
    try {
        console.log(`[MySQL] Đang thử kết nối tới MySQL ${dbConfig.host}:${dbConfig.port}...`);
        // Kết nối không chỉ định DB trước để tạo DB nếu chưa có
        const rootConn = await mysql.createConnection({
            host: dbConfig.host,
            user: dbConfig.user,
            password: dbConfig.password,
            port: dbConfig.port
        });
        await rootConn.query(`CREATE DATABASE IF NOT EXISTS \`${dbConfig.database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`);
        await rootConn.end();

        // Tạo pool kết nối chính
        pool = mysql.createPool(dbConfig);
        console.log(`[MySQL] Đã kết nối thành công tới database '${dbConfig.database}'!`);

        // Đọc và chạy database.sql nếu bảng items chưa có
        const [tables] = await pool.query("SHOW TABLES LIKE 'items'");
        if (tables.length === 0) {
            console.log("[MySQL] Đang khởi tạo bảng và dữ liệu mẫu từ database.sql...");
            const sqlFile = path.join(__dirname, 'database.sql');
            if (fs.existsSync(sqlFile)) {
                const sqlContent = fs.readFileSync(sqlFile, 'utf8');
                await pool.query(sqlContent);
                console.log("[MySQL] Khởi tạo dữ liệu mẫu thành công!");
            }
        }
    } catch (err) {
        console.warn(`[MySQL Cảnh báo] Không thể kết nối tới MySQL (${err.message}).`);
        console.warn("[MySQL] Chuyển sang chế độ Local In-Memory Fallback để bạn có thể test ngay mà không cần bật MySQL!");
        useFallback = true;
        loadSampleMemoryItems();
    }
}

function loadSampleMemoryItems() {
    memoryDB.items = [
        { id: 'helm_bronze', name: 'Mũ Đồng Tân Binh', slot: 'HELMET', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 0, bonus_end: 3, bonus_pre: 0, bonus_luck: 1, description: 'Mũ đồng đúc thô sơ giúp che chắn đầu khi tập nặng.', icon: '🪖' },
        { id: 'helm_iron', name: 'Thiết Giáp Đầu', slot: 'HELMET', rarity: 'RARE', price_gold: 160, price_gems: 0, bonus_str: 3, bonus_end: 7, bonus_pre: 0, bonus_luck: 2, description: 'Rèn từ sắt non tôi luyện trong nhiệt độ cao.', icon: '⛑️' },
        { id: 'helm_valkyrie', name: 'Mũ Lông Vũ Valkyrie', slot: 'HELMET', rarity: 'EPIC', price_gold: 420, price_gems: 5, bonus_str: 5, bonus_end: 14, bonus_pre: 0, bonus_luck: 8, description: 'Ban phước bởi các nữ thần chiến trận phương Bắc.', icon: '👑' },
        { id: 'helm_dragon', name: 'Vương Miện Long Thần', slot: 'HELMET', rarity: 'LEGENDARY', price_gold: 1000, price_gems: 20, bonus_str: 18, bonus_end: 25, bonus_pre: 0, bonus_luck: 12, description: 'Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.', icon: '🐉' },
        { id: 'armor_leather', name: 'Áo Da Dã Ngoại', slot: 'ARMOR', rarity: 'COMMON', price_gold: 60, price_gems: 0, bonus_str: 0, bonus_end: 4, bonus_pre: 2, bonus_luck: 0, description: 'Áo da bò mềm mại, thoáng mát cho các buổi hít đất dài.', icon: '🥋' },
        { id: 'armor_plate', name: 'Chiến Giáp Thép Nung', slot: 'ARMOR', rarity: 'RARE', price_gold: 180, price_gems: 0, bonus_str: 5, bonus_end: 10, bonus_pre: 0, bonus_luck: 0, description: 'Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.', icon: '🛡️' },
        { id: 'armor_shadow', name: 'Áo Choàng Bóng Đêm', slot: 'ARMOR', rarity: 'EPIC', price_gold: 450, price_gems: 6, bonus_str: 10, bonus_end: 0, bonus_pre: 12, bonus_luck: 8, description: 'Hòa mình vào bóng tối, tăng sự tập trung và độ chuẩn xác.', icon: '🥷' },
        { id: 'armor_celestial', name: 'Thánh Giáp Quang Minh', slot: 'ARMOR', rarity: 'LEGENDARY', price_gold: 1200, price_gems: 25, bonus_str: 20, bonus_end: 28, bonus_pre: 15, bonus_luck: 0, description: 'Ánh hào quang chiếu rọi bảo bọc người chiến binh bền bỉ.', icon: '✨' },
        { id: 'gloves_cloth', name: 'Băng Quấn Cổ Tay', slot: 'GLOVES', rarity: 'COMMON', price_gold: 40, price_gems: 0, bonus_str: 3, bonus_end: 0, bonus_pre: 2, bonus_luck: 0, description: 'Bảo vệ khớp cổ tay khi chống đẩy liên tục trên sàn cứng.', icon: '🥊' },
        { id: 'gloves_grip', name: 'Găng Hít Đất Siêu Bám', slot: 'GLOVES', rarity: 'RARE', price_gold: 150, price_gems: 0, bonus_str: 8, bonus_end: 0, bonus_pre: 6, bonus_luck: 0, description: 'Đế cao su hạt kim cương chống trượt tay hoàn đối.', icon: '🧤' },
        { id: 'gloves_titan', name: 'Găng Titan Siêu Lực', slot: 'GLOVES', rarity: 'EPIC', price_gold: 400, price_gems: 5, bonus_str: 18, bonus_end: 0, bonus_pre: 8, bonus_luck: 3, description: 'Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.', icon: '🦾' },
        { id: 'gloves_infinity', name: 'Găng Tay Vô Cực', slot: 'GLOVES', rarity: 'LEGENDARY', price_gold: 1100, price_gems: 22, bonus_str: 32, bonus_end: 0, bonus_pre: 16, bonus_luck: 14, description: 'Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.', icon: '🌌' },
        { id: 'boots_runner', name: 'Giày Chạy Phản Lực', slot: 'BOOTS', rarity: 'COMMON', price_gold: 45, price_gems: 0, bonus_str: 0, bonus_end: 3, bonus_pre: 0, bonus_luck: 2, description: 'Êm ái, giảm chấn gối khi squat hoặc bật nhảy.', icon: '👟' },
        { id: 'boots_iron', name: 'Hộ Chân Chiến Binh', slot: 'BOOTS', rarity: 'RARE', price_gold: 150, price_gems: 0, bonus_str: 6, bonus_end: 7, bonus_pre: 0, bonus_luck: 0, description: 'Bọc thép mũi chân và ống quyển vững chãi như bàn thạch.', icon: '🥾' },
        { id: 'boots_winged', name: 'Hài Phong Thần Hermes', slot: 'BOOTS', rarity: 'EPIC', price_gold: 380, price_gems: 5, bonus_str: 0, bonus_end: 15, bonus_pre: 6, bonus_luck: 12, description: 'Đôi giày có cánh lướt đi nhẹ tựa lông hồng.', icon: '🪽' },
        { id: 'boots_abyss', name: 'Bộ Bước Vực Thẳm', slot: 'BOOTS', rarity: 'LEGENDARY', price_gold: 950, price_gems: 18, bonus_str: 18, bonus_end: 24, bonus_pre: 0, bonus_luck: 15, description: 'Mỗi bước chân để lại dư chấn khiến kẻ thù khiếp đảm.', icon: '⚡' },
        { id: 'amulet_stone', name: 'Bùa Đá May Mắn', slot: 'AMULET', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 0, bonus_end: 0, bonus_pre: 0, bonus_luck: 4, description: 'Hòn đá cuội ven suối đem lại vận may khi tập.', icon: '🪬' },
        { id: 'amulet_ruby', name: 'Huyết Ngọc Hồi Phục', slot: 'AMULET', rarity: 'RARE', price_gold: 190, price_gems: 0, bonus_str: 6, bonus_end: 6, bonus_pre: 0, bonus_luck: 5, description: 'Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.', icon: '🔮' },
        { id: 'amulet_eye', name: 'Mắt Ưng Tinh Anh', slot: 'AMULET', rarity: 'EPIC', price_gold: 480, price_gems: 7, bonus_str: 10, bonus_end: 0, bonus_pre: 16, bonus_luck: 10, description: 'Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mi-li-mét.', icon: '👁️' },
        { id: 'amulet_sun', name: 'Thái Dương Cổ Thạch', slot: 'AMULET', rarity: 'LEGENDARY', price_gold: 1300, price_gems: 30, bonus_str: 20, bonus_end: 20, bonus_pre: 20, bonus_luck: 25, description: 'Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.', icon: '☀️' },
        { id: 'weapon_stick', name: 'Côn Gỗ Luyện Tập', slot: 'WEAPON', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 4, bonus_end: 0, bonus_pre: 2, bonus_luck: 0, description: 'Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.', icon: '🪵' },
        { id: 'weapon_sword', name: 'Thanh Kiếm Thép Đúc', slot: 'WEAPON', rarity: 'RARE', price_gold: 180, price_gems: 0, bonus_str: 10, bonus_end: 0, bonus_pre: 6, bonus_luck: 0, description: 'Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.', icon: '⚔️' },
        { id: 'weapon_axe', name: 'Rìu Chiến Berserker', slot: 'WEAPON', rarity: 'EPIC', price_gold: 460, price_gems: 6, bonus_str: 22, bonus_end: 8, bonus_pre: 0, bonus_luck: 0, description: 'Chiếc rìu khổng lồ dành riêng cho những chiến binh cuồng nộ.', icon: '🪓' },
        { id: 'weapon_excalibur', name: 'Thánh Kiếm Excalibur', slot: 'WEAPON', rarity: 'LEGENDARY', price_gold: 1400, price_gems: 30, bonus_str: 35, bonus_end: 0, bonus_pre: 18, bonus_luck: 15, description: 'Bảo kiếm huyền thoại cắm sâu trong đá.', icon: '🗡️' }
    ];
}

// ====================== CÁC ENDPOINT REST API ======================

// 1. Kiểm tra trạng thái máy chủ
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        mode: useFallback ? 'in-memory-fallback' : 'mysql',
        database: dbConfig.database,
        time: new Date().toISOString()
    });
});

// 2. Lấy thông tin người chơi, chỉ số, và kho đồ
app.get('/api/user/:username', async (req, res) => {
    const { username } = req.params;
    try {
        if (!useFallback) {
            let [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            if (users.length === 0) {
                await pool.query('INSERT INTO users (username) VALUES (?)', [username]);
                [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            }
            const user = users[0];

            // Lấy kho đồ và trang bị hiện tại
            const [inventory] = await pool.query(`
                SELECT ui.id as inv_id, ui.is_equipped, i.* 
                FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.user_id = ?
            `, [user.id]);

            return res.json({ user, inventory });
        } else {
            let user = memoryDB.users.find(u => u.username === username);
            if (!user) {
                user = { id: memoryDB.users.length + 1, username, gold: 100, gems: 5, xp: 0, level: 1, streak: 0, total_reps: 0, stage: 1 };
                memoryDB.users.push(user);
            }
            const inventory = memoryDB.inventory
                .filter(inv => inv.user_id === user.id)
                .map(inv => {
                    const item = memoryDB.items.find(i => i.id === inv.item_id);
                    return { inv_id: inv.id, is_equipped: inv.is_equipped, ...item };
                });
            return res.json({ user, inventory });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 3. Lấy danh sách vật phẩm trong Cửa Hàng
app.get('/api/shop/items', async (req, res) => {
    try {
        if (!useFallback) {
            const [items] = await pool.query('SELECT * FROM items ORDER BY price_gold ASC');
            return res.json(items);
        } else {
            return res.json(memoryDB.items);
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 4. Mua vật phẩm từ Cửa Hàng
app.post('/api/shop/buy', async (req, res) => {
    const { username, itemId } = req.body;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            const [items] = await pool.query('SELECT * FROM items WHERE id = ?', [itemId]);
            if (users.length === 0 || items.length === 0) return res.status(404).json({ error: 'Không tìm thấy người dùng hoặc vật phẩm' });

            const user = users[0];
            const item = items[0];

            if (user.gold < item.price_gold || user.gems < item.price_gems) {
                return res.status(400).json({ error: 'Không đủ Vàng hoặc Kim Cương để mua vật phẩm này!' });
            }

            // Trừ tiền và thêm vào kho đồ
            await pool.query('UPDATE users SET gold = gold - ?, gems = gems - ? WHERE id = ?', [item.price_gold, item.price_gems, user.id]);
            await pool.query('INSERT INTO user_inventory (user_id, item_id, is_equipped) VALUES (?, ?, FALSE)', [user.id, item.id]);

            return res.json({ success: true, message: `Mua thành công ${item.name}!`, item });
        } else {
            const user = memoryDB.users.find(u => u.username === username);
            const item = memoryDB.items.find(i => i.id === itemId);
            if (!user || !item) return res.status(404).json({ error: 'Không tìm thấy' });

            if (user.gold < item.price_gold || user.gems < item.price_gems) {
                return res.status(400).json({ error: 'Không đủ Vàng hoặc Kim Cương!' });
            }
            user.gold -= item.price_gold;
            user.gems -= item.price_gems;
            memoryDB.inventory.push({ id: memoryDB.inventory.length + 1, user_id: user.id, item_id: item.id, is_equipped: 0 });
            return res.json({ success: true, message: `Mua thành công ${item.name}!`, item });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 5. Trang bị vật phẩm vào slot của Anh Hùng
app.post('/api/inventory/equip', async (req, res) => {
    const { username, invId } = req.body;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'User không tồn tại' });
            const user = users[0];

            const [invEntries] = await pool.query(`
                SELECT ui.*, i.slot 
                FROM user_inventory ui
                JOIN items i ON ui.item_id = i.id
                WHERE ui.id = ? AND ui.user_id = ?
            `, [invId, user.id]);
            if (invEntries.length === 0) return res.status(404).json({ error: 'Không tìm thấy vật phẩm trong kho' });

            const targetSlot = invEntries[0].slot;

            // Tháo tất cả các món đồ cùng loại slot đang đeo
            await pool.query(`
                UPDATE user_inventory ui
                JOIN items i ON ui.item_id = i.id
                SET ui.is_equipped = FALSE
                WHERE ui.user_id = ? AND i.slot = ?
            `, [user.id, targetSlot]);

            // Trang bị món đồ mới
            await pool.query('UPDATE user_inventory SET is_equipped = TRUE WHERE id = ?', [invId]);
            return res.json({ success: true, message: 'Đã trang bị thành công!' });
        } else {
            const user = memoryDB.users.find(u => u.username === username);
            const inv = memoryDB.inventory.find(i => i.id === invId && i.user_id === user.id);
            if (!inv) return res.status(404).json({ error: 'Không tìm thấy' });
            const item = memoryDB.items.find(i => i.id === inv.item_id);

            // Tháo món cùng slot
            memoryDB.inventory.forEach(otherInv => {
                const otherItem = memoryDB.items.find(i => i.id === otherInv.item_id);
                if (otherItem && otherItem.slot === item.slot && otherInv.user_id === user.id) {
                    otherInv.is_equipped = 0;
                }
            });
            inv.is_equipped = 1;
            return res.json({ success: true, message: 'Đã trang bị thành công!' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 6. Tháo trang bị
app.post('/api/inventory/unequip', async (req, res) => {
    const { username, invId } = req.body;
    try {
        if (!useFallback) {
            await pool.query('UPDATE user_inventory SET is_equipped = FALSE WHERE id = ?', [invId]);
            return res.json({ success: true, message: 'Đã tháo trang bị!' });
        } else {
            const inv = memoryDB.inventory.find(i => i.id === invId);
            if (inv) inv.is_equipped = 0;
            return res.json({ success: true, message: 'Đã tháo trang bị!' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 7. Hoàn thành bài tập & Tính toán Tỷ Lệ Rớt Đồ (Loot Drop Rate System)
app.post('/api/workout/finish', async (req, res) => {
    const { username, exercise, reps, holdSeconds, score, isBoss, isFreeTraining } = req.body;
    if (!score || score <= 0) return res.status(400).json({ error: 'Score phải lớn hơn 0' });

    try {
        let user;
        let allItems = [];

        if (!useFallback) {
            const [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            user = users[0];
            const [items] = await pool.query('SELECT * FROM items');
            allItems = items;
        } else {
            user = memoryDB.users.find(u => u.username === username);
            allItems = memoryDB.items;
        }

        const todayStr = new Date().toISOString().split('T')[0];
        let newStreak = user.streak;
        if (user.last_workout_date !== todayStr) {
            const yesterday = new Date();
            yesterday.setDate(yesterday.getDate() - 1);
            const yesterdayStr = yesterday.toISOString().split('T')[0];
            newStreak = (user.last_workout_date === yesterdayStr) ? user.streak + 1 : 1;
        }
        const newTotalReps = user.total_reps + score;

        let droppedItem = null;
        let xpEarned = 0;
        let goldEarned = 0;
        let newXp = user.xp;
        let newLevel = user.level;
        let newGold = user.gold;
        let newStage = user.stage;

        if (isFreeTraining) {
            // Chế độ tập tự do: không có thưởng, không rơi đồ, không tăng stage
            xpEarned = 0;
            goldEarned = 0;
            droppedItem = null;
        } else {
            // 1. Tính toán thuộc tính tổng (Base + bonus từ trang bị đang đeo)
            let totalLuck = 5 + Math.floor(user.total_reps / 5);
            if (!useFallback) {
                const [bonus] = await pool.query(`
                    SELECT SUM(i.bonus_luck) as sum_luck 
                    FROM user_inventory ui
                    JOIN items i ON ui.item_id = i.id
                    WHERE ui.user_id = ? AND ui.is_equipped = TRUE
                `, [user.id]);
                if (bonus[0].sum_luck) totalLuck += Number(bonus[0].sum_luck);
            }

            // 2. TÍNH TỶ LỆ RỚT ĐỒ (Đã giảm theo cân bằng game):
            // Quái thường: Base 12% + (Luck * 0.2%) (Tối đa 25%)
            // Trùm: Base 35% + (Luck * 0.3%) (Tối đa 50%)
            const baseRate = isBoss ? 0.35 : 0.12;
            const luckBonus = isBoss ? totalLuck * 0.003 : totalLuck * 0.002;
            const maxChance = isBoss ? 0.50 : 0.25;
            const dropChance = Math.min(maxChance, baseRate + luckBonus);
            const rollDrop = Math.random();

            if (rollDrop <= dropChance) {
                const rollRarity = Math.random();
                let chosenRarity = 'COMMON';
                if (isBoss) {
                    if (rollRarity < Math.min(0.08, 0.02 + totalLuck * 0.001)) chosenRarity = 'LEGENDARY';
                    else if (rollRarity < Math.min(0.25, 0.12 + totalLuck * 0.002)) chosenRarity = 'EPIC';
                    else if (rollRarity < 0.60) chosenRarity = 'RARE';
                } else {
                    if (rollRarity < Math.min(0.02, 0.005 + totalLuck * 0.0005)) chosenRarity = 'LEGENDARY';
                    else if (rollRarity < Math.min(0.10, 0.04 + totalLuck * 0.001)) chosenRarity = 'EPIC';
                    else if (rollRarity < 0.35) chosenRarity = 'RARE';
                }

                const candidateItems = allItems.filter(i => i.rarity === chosenRarity);
                if (candidateItems.length > 0) {
                    droppedItem = candidateItems[Math.floor(Math.random() * candidateItems.length)];
                } else {
                    droppedItem = allItems[Math.floor(Math.random() * allItems.length)];
                }
            }

            // 3. Tính toán phần thưởng kinh nghiệm, vàng
            xpEarned = isBoss ? score * 15 : score * 8;
            goldEarned = isBoss ? score * 4 : score * 2;
            newGold += goldEarned;
            newXp += xpEarned;

            let xpNeeded = 80 + newLevel * 160;
            while (newXp >= xpNeeded) {
                newXp -= xpNeeded;
                newLevel++;
                xpNeeded = 80 + newLevel * 160;
            }
        }

        // Lưu vào cơ sở dữ liệu
        if (!useFallback) {
            await pool.query(`
                UPDATE users 
                SET gold = ?, xp = ?, level = ?, streak = ?, total_reps = ?, stage = ?, last_workout_date = ?
                WHERE id = ?
            `, [newGold, newXp, newLevel, newStreak, newTotalReps, newStage, todayStr, user.id]);

            let droppedItemId = null;
            if (droppedItem) {
                droppedItemId = droppedItem.id;
                await pool.query('INSERT INTO user_inventory (user_id, item_id, is_equipped) VALUES (?, ?, FALSE)', [user.id, droppedItem.id]);
            }

            await pool.query(`
                INSERT INTO workouts (user_id, exercise, reps, hold_seconds, score, xp_earned, gold_earned, dropped_item_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            `, [user.id, exercise, reps || 0, holdSeconds || 0, score, xpEarned, goldEarned, droppedItemId]);
        } else {
            user.gold = newGold;
            user.xp = newXp;
            user.level = newLevel;
            user.streak = newStreak;
            user.total_reps = newTotalReps;
            user.stage = newStage;
            user.last_workout_date = todayStr;
            if (droppedItem) {
                memoryDB.inventory.push({ id: memoryDB.inventory.length + 1, user_id: user.id, item_id: droppedItem.id, is_equipped: 0 });
            }
        }

        return res.json({
            success: true,
            score,
            xpEarned,
            goldEarned,
            newLevel,
            newGold,
            newXp,
            newStreak,
            droppedItem // Trả về vật phẩm vừa rớt (nếu có)
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Khởi động server
initDatabase().then(() => {
    app.listen(PORT, '0.0.0.0', () => {
        console.log(`====================================================`);
        console.log(`🚀 Vigil Backend API đang chạy tại: http://localhost:${PORT}`);
        console.log(`📱 Từ Android Emulator gọi: http://10.0.2.2:${PORT}`);
        console.log(`🌐 Từ thiết bị thật cùng Wifi: http://<IP_MÁY_BẠN>:${PORT}`);
        console.log(`====================================================`);
    });
});
