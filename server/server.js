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

let memoryDB = {
    users: [
        { id: 1, username: 'Hachimi', password: '123456', avatar: '🦊', gold: 0, gems: 0, xp: 80, level: 2, streak: 3, total_reps: 45, stage: 2 }
    ],
    items: [],
    inventory: [{ id: 1, user_id: 1, item_id: 'helm_bronze', is_equipped: 1 }],
    friends: [],
    friend_requests: [],
    workouts: [],
    guilds: [
        { id: 1, name: 'Vigil Chiến Binh 🛡️', badge: '🛡️', slogan: 'Tập luyện bứt phá giới hạn mỗi ngày!', leader_id: 1, level: 1 }
    ],
    guild_members: [
        { id: 1, guild_id: 1, user_id: 1, role: 'LEADER' }
    ],
    guild_invitations: [],
    guild_join_requests: [],
    guild_bosses: [
        { id: 1, guild_id: 1, boss_id: 'boss_nether_dragon', boss_name: 'Hắc Long Viễn Cổ - Nidhogg', boss_title: 'SIÊU TRÙM THẾ GIỚI BANG HỘI', boss_avatar: '🐉', max_hp: 500000, current_hp: 500000, status: 'ACTIVE', reward_gold: 15000, reward_gems: 350, reward_item_id: 'weapon_dragon_slayer' }
    ],
    guild_boss_damage: []
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
        // 1. MŨ GIÁP (HELMET)
        { id: 'helm_bronze', name: 'Mũ Đồng Tân Binh', slot: 'HELMET', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 0, bonus_end: 3, bonus_pre: 0, bonus_luck: 1, description: 'Mũ đồng đúc thô sơ giúp che chắn đầu khi tập nặng.', icon: '🪖' },
        { id: 'helm_scout', name: 'Nón Trinh Sát Rừng Rậm', slot: 'HELMET', rarity: 'UNCOMMON', price_gold: 90, price_gems: 0, bonus_str: 0, bonus_end: 5, bonus_pre: 2, bonus_luck: 1, description: 'Nón vải ngụy trang nhẹ nhàng cho các buổi cardio dã ngoại.', icon: '🧢' },
        { id: 'helm_iron', name: 'Thiết Giáp Đầu', slot: 'HELMET', rarity: 'RARE', price_gold: 160, price_gems: 0, bonus_str: 3, bonus_end: 7, bonus_pre: 0, bonus_luck: 2, description: 'Rèn từ sắt non tôi luyện trong nhiệt độ cao.', icon: '⛑️' },
        { id: 'helm_valkyrie', name: 'Mũ Lông Vũ Valkyrie', slot: 'HELMET', rarity: 'EPIC', price_gold: 420, price_gems: 5, bonus_str: 5, bonus_end: 14, bonus_pre: 0, bonus_luck: 8, description: 'Ban phước bởi các nữ thần chiến trận phương Bắc.', icon: '👑' },
        { id: 'helm_dragon', name: 'Vương Miện Long Thần', slot: 'HELMET', rarity: 'LEGENDARY', price_gold: 1000, price_gems: 20, bonus_str: 20, bonus_end: 18, bonus_pre: 25, bonus_luck: 12, description: 'Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.', icon: '🐉' },
        { id: 'helm_abyss', name: 'Vương Miện Vực Sâu', slot: 'HELMET', rarity: 'MYTHIC', price_gold: 2800, price_gems: 60, bonus_str: 35, bonus_end: 40, bonus_pre: 15, bonus_luck: 20, description: 'Ngưng tụ từ hắc ám vô tận dưới đáy vực, bảo hộ tuyệt đối tinh thần.', icon: '👑' },
        { id: 'helm_odin', name: 'Mũ Thần Chiến Binh Odin', slot: 'HELMET', rarity: 'ANCIENT', price_gold: 5000, price_gems: 120, bonus_str: 50, bonus_end: 55, bonus_pre: 30, bonus_luck: 35, description: 'Bảo vật của Vua các vị thần, khai mở trí tuệ và thể lực siêu phàm.', icon: '🦅' },
        { id: 'helm_divine_crown', name: 'Thần Quan Thiên Giới', slot: 'HELMET', rarity: 'DIVINE', price_gold: 8500, price_gems: 200, bonus_str: 75, bonus_end: 80, bonus_pre: 50, bonus_luck: 60, description: 'Vương miện của Đấng Tối Cao, hội tụ hào quang thái hư bảo bọc.', icon: '✨' },

        // 2. GIÁP NGỰC (ARMOR)
        { id: 'armor_leather', name: 'Áo Da Dã Ngoại', slot: 'ARMOR', rarity: 'COMMON', price_gold: 60, price_gems: 0, bonus_str: 0, bonus_end: 4, bonus_pre: 2, bonus_luck: 0, description: 'Áo da bò mềm mại, thoáng mát cho các buổi hít đất dài.', icon: '🥋' },
        { id: 'armor_chainmail', name: 'Áo Giáp Xích Bạc', slot: 'ARMOR', rarity: 'UNCOMMON', price_gold: 110, price_gems: 0, bonus_str: 2, bonus_end: 7, bonus_pre: 1, bonus_luck: 0, description: 'Kết từ hàng nghìn vòng xích thép dẻo dai phân tán lực va đập.', icon: '⛓️' },
        { id: 'armor_plate', name: 'Chiến Giáp Thép Nung', slot: 'ARMOR', rarity: 'RARE', price_gold: 180, price_gems: 0, bonus_str: 5, bonus_end: 10, bonus_pre: 0, bonus_luck: 0, description: 'Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.', icon: '🛡️' },
        { id: 'armor_shadow', name: 'Áo Choàng Bóng Đêm', slot: 'ARMOR', rarity: 'EPIC', price_gold: 450, price_gems: 6, bonus_str: 10, bonus_end: 0, bonus_pre: 12, bonus_luck: 8, description: 'Hòa mình vào bóng tối, tăng sự tập trung và độ chuẩn xác.', icon: '🥷' },
        { id: 'armor_celestial', name: 'Thánh Giáp Quang Minh', slot: 'ARMOR', rarity: 'LEGENDARY', price_gold: 1200, price_gems: 25, bonus_str: 20, bonus_end: 28, bonus_pre: 15, bonus_luck: 0, description: 'Ánh hào quang chiếu rọi bảo bọc người chiến binh bền bỉ.', icon: '✨' },
        { id: 'armor_dragon_scale', name: 'Long Lân Thần Giáp', slot: 'ARMOR', rarity: 'MYTHIC', price_gold: 3200, price_gems: 75, bonus_str: 25, bonus_end: 60, bonus_pre: 15, bonus_luck: 20, description: 'Lớp vảy rồng kiên cố bất khả xâm phạm bảo vệ toàn thân.', icon: '🐲' },
        { id: 'armor_aegis', name: 'Thánh Giáp Bất Hoại Aegis', slot: 'ARMOR', rarity: 'ANCIENT', price_gold: 5500, price_gems: 130, bonus_str: 40, bonus_end: 80, bonus_pre: 25, bonus_luck: 35, description: 'Tấm khiên giáp huyền thoại của thần Zeus, chặn đứng mọi ngoại lực.', icon: '🛡️' },
        { id: 'armor_primordial', name: 'Hỗn Nguyên Chiến Giáp', slot: 'ARMOR', rarity: 'DIVINE', price_gold: 9500, price_gems: 240, bonus_str: 65, bonus_end: 120, bonus_pre: 45, bonus_luck: 60, description: 'Rèn từ vật chất khởi thủy trước khi vũ trụ hình thành, bất hoại vĩnh cửu.', icon: '🌌' },

        // 3. GĂNG TAY (GLOVES)
        { id: 'gloves_cloth', name: 'Băng Quấn Cổ Tay', slot: 'GLOVES', rarity: 'COMMON', price_gold: 40, price_gems: 0, bonus_str: 3, bonus_end: 0, bonus_pre: 2, bonus_luck: 0, description: 'Bảo vệ khớp cổ tay khi chống đẩy liên tục trên sàn cứng.', icon: '🥊' },
        { id: 'gloves_leather_strap', name: 'Găng Đấu Khí Thiếu Niên', slot: 'GLOVES', rarity: 'UNCOMMON', price_gold: 80, price_gems: 0, bonus_str: 5, bonus_end: 0, bonus_pre: 4, bonus_luck: 1, description: 'Găng da dê bọc khớp tăng uy lực cú đấm và chống đẩy.', icon: '🥊' },
        { id: 'gloves_grip', name: 'Găng Hít Đất Siêu Bám', slot: 'GLOVES', rarity: 'RARE', price_gold: 150, price_gems: 0, bonus_str: 8, bonus_end: 0, bonus_pre: 6, bonus_luck: 0, description: 'Đế cao su hạt kim cương chống trượt tay hoàn đối.', icon: '🧤' },
        { id: 'gloves_titan', name: 'Găng Titan Siêu Lực', slot: 'GLOVES', rarity: 'EPIC', price_gold: 400, price_gems: 5, bonus_str: 18, bonus_end: 0, bonus_pre: 8, bonus_luck: 3, description: 'Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.', icon: '🦾' },
        { id: 'gloves_infinity', name: 'Găng Tay Vô Cực', slot: 'GLOVES', rarity: 'LEGENDARY', price_gold: 1100, price_gems: 22, bonus_str: 32, bonus_end: 0, bonus_pre: 16, bonus_luck: 14, description: 'Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.', icon: '🌌' },
        { id: 'gloves_dragon_claw', name: 'Vuốt Rồng Bạt Hải', slot: 'GLOVES', rarity: 'MYTHIC', price_gold: 2600, price_gems: 55, bonus_str: 50, bonus_end: 10, bonus_pre: 30, bonus_luck: 25, description: 'Móng vuốt rồng thiêng xé toạc hư không, bùng nổ lực đẩy tay.', icon: '🐉' },
        { id: 'gloves_thunder_strike', name: 'Quyền Thủ Lôi Thần Thor', slot: 'GLOVES', rarity: 'ANCIENT', price_gold: 4800, price_gems: 110, bonus_str: 75, bonus_end: 20, bonus_pre: 40, bonus_luck: 30, description: 'Găng sắt thần thánh giúp vung sấm sét ngàn cân dễ như trở bàn tay.', icon: '⚡' },
        { id: 'gloves_creator', name: 'Thủ Ấn Khởi Nguyên', slot: 'GLOVES', rarity: 'DIVINE', price_gold: 8200, price_gems: 190, bonus_str: 110, bonus_end: 35, bonus_pre: 65, bonus_luck: 55, description: 'Bàn tay nhào nặn tinh cầu, chuyển hóa từng nhịp đẩy thành siêu sóng xung kích.', icon: '☄️' },

        // 4. GIÀY (BOOTS)
        { id: 'boots_runner', name: 'Giày Chạy Phản Lực', slot: 'BOOTS', rarity: 'COMMON', price_gold: 45, price_gems: 0, bonus_str: 0, bonus_end: 3, bonus_pre: 0, bonus_luck: 2, description: 'Êm ái, giảm chấn gối khi squat hoặc bật nhảy.', icon: '👟' },
        { id: 'boots_leather_hunter', name: 'Ủng Da Thợ Săn', slot: 'BOOTS', rarity: 'UNCOMMON', price_gold: 95, price_gems: 0, bonus_str: 1, bonus_end: 5, bonus_pre: 3, bonus_luck: 2, description: 'Bám chắc địa hình, giảm áp lực lên gót chân khi nhảy dây.', icon: '👢' },
        { id: 'boots_iron', name: 'Hộ Chân Chiến Binh', slot: 'BOOTS', rarity: 'RARE', price_gold: 150, price_gems: 0, bonus_str: 6, bonus_end: 7, bonus_pre: 0, bonus_luck: 0, description: 'Bọc thép mũi chân và ống quyển vững chãi như bàn thạch.', icon: '🥾' },
        { id: 'boots_winged', name: 'Hài Phong Thần Hermes', slot: 'BOOTS', rarity: 'EPIC', price_gold: 380, price_gems: 5, bonus_str: 0, bonus_end: 15, bonus_pre: 6, bonus_luck: 12, description: 'Đôi giày có cánh lướt đi nhẹ tựa lông hồng.', icon: '🪽' },
        { id: 'boots_abyss', name: 'Bộ Bước Vực Thẳm', slot: 'BOOTS', rarity: 'LEGENDARY', price_gold: 950, price_gems: 18, bonus_str: 18, bonus_end: 24, bonus_pre: 0, bonus_luck: 15, description: 'Mỗi bước chân để lại dư chấn khiến kẻ thù khiếp đảm.', icon: '⚡' },
        { id: 'boots_shadow_stalker', name: 'Hư Không Bộ Pháp', slot: 'BOOTS', rarity: 'MYTHIC', price_gold: 2500, price_gems: 50, bonus_str: 20, bonus_end: 35, bonus_pre: 25, bonus_luck: 30, description: 'Lướt đi giữa các chiều không gian, đôi chân không hề biết mỏi.', icon: '⚡' },
        { id: 'boots_chronos', name: 'Hài Thời Gian Chronos', slot: 'BOOTS', rarity: 'ANCIENT', price_gold: 4600, price_gems: 105, bonus_str: 30, bonus_end: 50, bonus_pre: 45, bonus_luck: 40, description: 'Bước chân thao túng thời gian, biến mỗi giây plank thành sức mạnh vô song.', icon: '⏳' },
        { id: 'boots_celestial_stride', name: 'Tiêu Dao Thần Bộ', slot: 'BOOTS', rarity: 'DIVINE', price_gold: 8000, price_gems: 180, bonus_str: 45, bonus_end: 75, bonus_pre: 70, bonus_luck: 65, description: 'Đạp mây cưỡi gió vượt qua ranh giới cõi phàm trần.', icon: '🌟' },

        // 5. BÙA CHÚ (AMULET)
        { id: 'amulet_stone', name: 'Bùa Đá May Mắn', slot: 'AMULET', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 0, bonus_end: 0, bonus_pre: 0, bonus_luck: 4, description: 'Hòn đá cuội ven suối đem lại vận may khi tập.', icon: '🪬' },
        { id: 'amulet_wolf_tooth', name: 'Nanh Sói Hoang Dã', slot: 'AMULET', rarity: 'UNCOMMON', price_gold: 100, price_gems: 0, bonus_str: 2, bonus_end: 2, bonus_pre: 2, bonus_luck: 6, description: 'Nanh sói đầu đàn mang lại giác quan nhạy bén và may mắn.', icon: '🐺' },
        { id: 'amulet_ruby', name: 'Huyết Ngọc Hồi Phục', slot: 'AMULET', rarity: 'RARE', price_gold: 190, price_gems: 0, bonus_str: 6, bonus_end: 6, bonus_pre: 0, bonus_luck: 5, description: 'Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.', icon: '🔮' },
        { id: 'amulet_eye', name: 'Mắt Ưng Tinh Anh', slot: 'AMULET', rarity: 'EPIC', price_gold: 480, price_gems: 7, bonus_str: 10, bonus_end: 0, bonus_pre: 16, bonus_luck: 10, description: 'Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mi-li-mét.', icon: '👁️' },
        { id: 'amulet_sun', name: 'Thái Dương Cổ Thạch', slot: 'AMULET', rarity: 'LEGENDARY', price_gold: 1300, price_gems: 30, bonus_str: 20, bonus_end: 20, bonus_pre: 20, bonus_luck: 25, description: 'Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.', icon: '☀️' },
        { id: 'amulet_boss_heart', name: 'Trái Tim Hắc Long', slot: 'AMULET', rarity: 'MYTHIC', price_gold: 4000, price_gems: 100, bonus_str: 30, bonus_end: 30, bonus_pre: 30, bonus_luck: 45, description: 'Tinh hoa sinh mệnh của Siêu Trùm Thế Giới ban phước lành.', icon: '💎' },
        { id: 'amulet_ouroboros', name: 'Ngọc Bội Vô Cực Ouroboros', slot: 'AMULET', rarity: 'ANCIENT', price_gold: 5800, price_gems: 140, bonus_str: 45, bonus_end: 45, bonus_pre: 45, bonus_luck: 60, description: 'Biểu tượng con rắn cắn đuôi luân hồi, sinh lực dồi dào bất tận.', icon: '♾️' },
        { id: 'amulet_genesis_spark', name: 'Hỏa Chủng Sáng Thế', slot: 'AMULET', rarity: 'DIVINE', price_gold: 9900, price_gems: 260, bonus_str: 70, bonus_end: 70, bonus_pre: 70, bonus_luck: 90, description: 'Tia lửa ban đầu thắp sáng muôn loài, gia tăng cực hạn mọi chỉ số.', icon: '💥' },

        // 6. VŨ KHÍ (WEAPON)
        { id: 'weapon_stick', name: 'Côn Gỗ Luyện Tập', slot: 'WEAPON', rarity: 'COMMON', price_gold: 50, price_gems: 0, bonus_str: 4, bonus_end: 0, bonus_pre: 2, bonus_luck: 0, description: 'Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.', icon: '🪵' },
        { id: 'weapon_dagger', name: 'Dao Găm Sát Thủ', slot: 'WEAPON', rarity: 'UNCOMMON', price_gold: 100, price_gems: 0, bonus_str: 7, bonus_end: 0, bonus_pre: 5, bonus_luck: 2, description: 'Lưỡi dao thép đen nhẹ bén, thích hợp luyện tập tốc độ cao.', icon: '🗡️' },
        { id: 'weapon_sword', name: 'Thanh Kiếm Thép Đúc', slot: 'WEAPON', rarity: 'RARE', price_gold: 180, price_gems: 0, bonus_str: 10, bonus_end: 0, bonus_pre: 6, bonus_luck: 0, description: 'Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.', icon: '⚔️' },
        { id: 'weapon_axe', name: 'Rìu Chiến Berserker', slot: 'WEAPON', rarity: 'EPIC', price_gold: 460, price_gems: 6, bonus_str: 22, bonus_end: 8, bonus_pre: 0, bonus_luck: 0, description: 'Chiếc rìu khổng lồ dành riêng cho những chiến binh cuồng nộ.', icon: '🪓' },
        { id: 'weapon_excalibur', name: 'Thánh Kiếm Excalibur', slot: 'WEAPON', rarity: 'LEGENDARY', price_gold: 1400, price_gems: 30, bonus_str: 35, bonus_end: 0, bonus_pre: 18, bonus_luck: 15, description: 'Bảo kiếm huyền thoại cắm sâu trong đá.', icon: '🗡️' },
        { id: 'weapon_dragon_slayer', name: 'Đại Đao Trảm Long', slot: 'WEAPON', rarity: 'MYTHIC', price_gold: 3500, price_gems: 80, bonus_str: 65, bonus_end: 15, bonus_pre: 35, bonus_luck: 25, description: 'Thần binh rèn từ vảy và răng Hắc Long, uy lực hủy thiên diệt địa.', icon: '🗡️' },
        { id: 'weapon_gungnir', name: 'Thần Thương Gungnir', slot: 'WEAPON', rarity: 'ANCIENT', price_gold: 6000, price_gems: 150, bonus_str: 85, bonus_end: 25, bonus_pre: 60, bonus_luck: 40, description: 'Ngọn thương thần thoại bách phát bách trúng, uy lực xuyên thủng mọi hàng phòng thủ.', icon: '🔱' },
        { id: 'weapon_god_slayer', name: 'Đồ Thần Cực Kiếm', slot: 'WEAPON', rarity: 'DIVINE', price_gold: 10000, price_gems: 300, bonus_str: 130, bonus_end: 40, bonus_pre: 80, bonus_luck: 70, description: 'Thần binh chí tôn trảm phá thần ma, đòn đánh xé rách thực tại.', icon: '⚔️' }
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

// =========================================================================
// 1. XÁC THỰC: ĐĂNG KÝ & ĐĂNG NHẬP
// =========================================================================

// A. ĐĂNG KÝ TÀI KHOẢN MỚI
app.post('/api/auth/register', async (req, res) => {
    const { username, password, avatar } = req.body;
    if (!username || !password) {
        return res.status(400).json({ success: false, error: 'Vui lòng nhập tên tài khoản và mật khẩu!' });
    }
    const chosenAvatar = avatar || '🧑‍🎤';

    try {
        if (!useFallback) {
            const [existing] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            if (existing.length > 0) {
                return res.status(400).json({ success: false, error: 'Tên tài khoản đã tồn tại, vui lòng chọn tên khác!' });
            }

            const [result] = await pool.query(`
                INSERT INTO users (username, password, avatar, gold, gems, xp, level, streak, total_reps, stage)
                VALUES (?, ?, ?, 0, 0, 0, 1, 1, 0, 1)
            `, [username, password, chosenAvatar]);

            const newUserId = result.insertId;
            // Tặng mũ đồng cơ bản
            await pool.query('INSERT INTO user_inventory (user_id, item_id, is_equipped) VALUES (?, "helm_bronze", TRUE)', [newUserId]);

            const [users] = await pool.query('SELECT id, username, avatar, gold, gems, xp, level, streak, total_reps, stage FROM users WHERE id = ?', [newUserId]);
            return res.json({ success: true, message: 'Đăng ký thành công!', user: users[0] });
        } else {
            const exists = memoryDB.users.some(u => u.username.toLowerCase() === username.toLowerCase());
            if (exists) {
                return res.status(400).json({ success: false, error: 'Tên tài khoản đã tồn tại, vui lòng chọn tên khác!' });
            }
            const newUser = {
                id: memoryDB.users.length + 1,
                username,
                password,
                avatar: chosenAvatar,
                gold: 0,
                gems: 0,
                xp: 0,
                level: 1,
                streak: 1,
                total_reps: 0,
                stage: 1
            };
            memoryDB.users.push(newUser);
            memoryDB.inventory.push({ id: memoryDB.inventory.length + 1, user_id: newUser.id, item_id: 'helm_bronze', is_equipped: 1 });
            return res.json({ success: true, message: 'Đăng ký thành công!', user: newUser });
        }
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// B. ĐĂNG NHẬP
app.post('/api/auth/login', async (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).json({ success: false, error: 'Vui lòng nhập tên tài khoản và mật khẩu!' });
    }

    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
            if (users.length === 0) {
                return res.status(404).json({ success: false, error: 'Tài khoản không tồn tại!' });
            }
            const user = users[0];
            if (user.password && user.password !== password) {
                return res.status(401).json({ success: false, error: 'Mật khẩu không chính xác!' });
            }
            return res.json({
                success: true,
                message: 'Đăng nhập thành công!',
                user: {
                    id: user.id,
                    username: user.username,
                    avatar: user.avatar || '🧑‍🎤',
                    gold: user.gold,
                    gems: user.gems,
                    xp: user.xp,
                    level: user.level,
                    streak: user.streak,
                    total_reps: user.total_reps,
                    stage: user.stage
                }
            });
        } else {
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) {
                return res.status(404).json({ success: false, error: 'Tài khoản không tồn tại!' });
            }
            if (user.password && user.password !== password) {
                return res.status(401).json({ success: false, error: 'Mật khẩu không chính xác!' });
            }
            return res.json({ success: true, message: 'Đăng nhập thành công!', user });
        }
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
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
                user = { id: memoryDB.users.length + 1, username, gold: 0, gems: 0, xp: 0, level: 1, streak: 0, total_reps: 0, stage: 1 };
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
                    const luckFactor = totalLuck * 0.001;
                    if (rollRarity < 0.01 + luckFactor * 0.1) chosenRarity = 'DIVINE';
                    else if (rollRarity < 0.04 + luckFactor * 0.2) chosenRarity = 'ANCIENT';
                    else if (rollRarity < 0.10 + luckFactor * 0.3) chosenRarity = 'MYTHIC';
                    else if (rollRarity < 0.22 + luckFactor * 0.4) chosenRarity = 'LEGENDARY';
                    else if (rollRarity < 0.45) chosenRarity = 'EPIC';
                    else if (rollRarity < 0.75) chosenRarity = 'RARE';
                    else chosenRarity = 'UNCOMMON';
                } else {
                    const luckFactor = totalLuck * 0.0005;
                    if (rollRarity < 0.002 + luckFactor * 0.05) chosenRarity = 'MYTHIC';
                    else if (rollRarity < 0.015 + luckFactor * 0.1) chosenRarity = 'LEGENDARY';
                    else if (rollRarity < 0.08 + luckFactor * 0.2) chosenRarity = 'EPIC';
                    else if (rollRarity < 0.25) chosenRarity = 'RARE';
                    else if (rollRarity < 0.60) chosenRarity = 'UNCOMMON';
                    else chosenRarity = 'COMMON';
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

// =========================================================================
// 8. BẠN BÈ & BẢNG XẾP HẠNG THẾ GIỚI
// =========================================================================

// A. THÊM BẠN BÈ BẰNG TÊN TÀI KHOẢN
app.post('/api/friends/add', async (req, res) => {
    const { username, friendUsername } = req.body;
    if (!username || !friendUsername) return res.status(400).json({ success: false, error: 'Thiếu thông tin người chơi!' });
    if (username.toLowerCase() === friendUsername.toLowerCase()) {
        return res.status(400).json({ success: false, error: 'Không thể kết bạn với chính mình!' });
    }

    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            const [friends] = await pool.query('SELECT id FROM users WHERE username = ?', [friendUsername]);
            if (users.length === 0 || friends.length === 0) {
                return res.status(404).json({ success: false, error: 'Không tìm thấy người chơi "' + friendUsername + '"!' });
            }
            const userId = users[0].id;
            const friendId = friends[0].id;

            // Kiểm tra đã là bạn chưa
            const [existing] = await pool.query('SELECT id FROM friends WHERE user_id = ? AND friend_id = ?', [userId, friendId]);
            if (existing.length > 0) {
                return res.status(400).json({ success: false, error: 'Hai bạn đã là bạn bè từ trước!' });
            }

            // Kết bạn hai chiều
            await pool.query('INSERT IGNORE INTO friends (user_id, friend_id) VALUES (?, ?), (?, ?)', [userId, friendId, friendId, userId]);
            return res.json({ success: true, message: 'Đã kết bạn thành công với ' + friendUsername + '!' });
        } else {
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            const friend = memoryDB.users.find(u => u.username.toLowerCase() === friendUsername.toLowerCase());
            if (!user || !friend) return res.status(404).json({ success: false, error: 'Không tìm thấy người chơi "' + friendUsername + '"!' });

            if (!memoryDB.friends) memoryDB.friends = [];
            const alreadyFriend = memoryDB.friends.some(f => f.user_id === user.id && f.friend_id === friend.id);
            if (alreadyFriend) return res.status(400).json({ success: false, error: 'Hai bạn đã là bạn bè từ trước!' });

            memoryDB.friends.push({ id: memoryDB.friends.length + 1, user_id: user.id, friend_id: friend.id });
            memoryDB.friends.push({ id: memoryDB.friends.length + 1, user_id: friend.id, friend_id: user.id });
            return res.json({ success: true, message: 'Đã kết bạn thành công với ' + friendUsername + '!' });
        }
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// B. LẤY DANH SÁCH BẠN BÈ
app.get('/api/friends/:username', async (req, res) => {
    const { username } = req.params;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi' });
            const userId = users[0].id;

            const [rows] = await pool.query(`
                SELECT u.id, u.username, u.avatar, u.level, u.streak, u.total_reps,
                    (50 + u.total_reps * 25 + u.level * 30) AS combat_power
                FROM friends f
                JOIN users u ON f.friend_id = u.id
                WHERE f.user_id = ?
                ORDER BY combat_power DESC
            `, [userId]);
            return res.json({ success: true, friends: rows });
        } else {
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) return res.status(404).json({ error: 'Không tìm thấy người chơi' });
            if (!memoryDB.friends) memoryDB.friends = [];

            const friendIds = memoryDB.friends.filter(f => f.user_id === user.id).map(f => f.friend_id);
            const friends = memoryDB.users
                .filter(u => friendIds.includes(u.id))
                .map(u => ({
                    id: u.id,
                    username: u.username,
                    avatar: u.avatar || '🧑‍🎤',
                    level: u.level,
                    streak: u.streak,
                    total_reps: u.total_reps,
                    combat_power: 50 + u.total_reps * 25 + u.level * 30
                }));
            return res.json({ success: true, friends });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// C. HỦY KẾT BẠN
app.post('/api/friends/remove', async (req, res) => {
    const { username, friendUsername } = req.body;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            const [friends] = await pool.query('SELECT id FROM users WHERE username = ?', [friendUsername]);
            if (users.length === 0 || friends.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi' });

            await pool.query('DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)', 
                [users[0].id, friends[0].id, friends[0].id, users[0].id]);
            return res.json({ success: true, message: 'Đã hủy kết bạn!' });
        } else {
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            const friend = memoryDB.users.find(u => u.username.toLowerCase() === friendUsername.toLowerCase());
            if (!user || !friend) return res.status(404).json({ error: 'Không tìm thấy người chơi' });

            if (memoryDB.friends) {
                memoryDB.friends = memoryDB.friends.filter(f => !( (f.user_id === user.id && f.friend_id === friend.id) || (f.user_id === friend.id && f.friend_id === user.id) ));
            }
            return res.json({ success: true, message: 'Đã hủy kết bạn!' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// D. GỬI LỜI MỜI KẾT BẠN
app.post('/api/friends/request', async (req, res) => {
    const { username, targetUsername } = req.body;
    if (!username || !targetUsername) return res.status(400).json({ error: 'Thiếu thông tin người chơi!' });
    if (username.toLowerCase() === targetUsername.toLowerCase()) return res.status(400).json({ error: 'Không thể kết bạn với chính mình!' });

    try {
        if (!useFallback) {
            const [senders] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            const [receivers] = await pool.query('SELECT id FROM users WHERE username = ?', [targetUsername]);
            if (senders.length === 0 || receivers.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });
            const sId = senders[0].id;
            const rId = receivers[0].id;

            const [friends] = await pool.query('SELECT id FROM friends WHERE user_id = ? AND friend_id = ?', [sId, rId]);
            if (friends.length > 0) return res.status(400).json({ error: 'Hai bạn đã là bạn bè!' });

            await pool.query('INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, "PENDING") ON DUPLICATE KEY UPDATE status="PENDING"', [sId, rId]);
            return res.json({ success: true, message: `Đã gửi lời mời kết bạn tới ${targetUsername}!` });
        } else {
            if (!memoryDB.friend_requests) memoryDB.friend_requests = [];
            const sender = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            const receiver = memoryDB.users.find(u => u.username.toLowerCase() === targetUsername.toLowerCase());
            if (!sender || !receiver) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            memoryDB.friend_requests.push({ id: memoryDB.friend_requests.length + 1, sender_id: sender.id, receiver_id: receiver.id, status: 'PENDING' });
            return res.json({ success: true, message: `Đã gửi lời mời kết bạn tới ${targetUsername}!` });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// E. PHẢN HỒI LỜI MỜI KẾT BẠN (CHẤP NHẬN HOẶC TỪ CHỐI)
app.post('/api/friends/respond', async (req, res) => {
    const { username, requestId, action } = req.body; // action: 'ACCEPT' or 'DECLINE'
    try {
        if (!useFallback) {
            const [reqs] = await pool.query('SELECT fr.*, u.username as sender_name FROM friend_requests fr JOIN users u ON fr.sender_id = u.id WHERE fr.id = ?', [requestId]);
            if (reqs.length === 0) return res.status(404).json({ error: 'Không tìm thấy lời mời!' });
            const fr = reqs[0];

            if (action === 'ACCEPT') {
                await pool.query('INSERT IGNORE INTO friends (user_id, friend_id) VALUES (?, ?), (?, ?)', [fr.sender_id, fr.receiver_id, fr.receiver_id, fr.sender_id]);
                await pool.query('DELETE FROM friend_requests WHERE id = ?', [requestId]);
                return res.json({ success: true, message: `Đã chấp nhận lời mời kết bạn từ ${fr.sender_name}!` });
            } else {
                await pool.query('DELETE FROM friend_requests WHERE id = ?', [requestId]);
                return res.json({ success: true, message: 'Đã từ chối lời mời kết bạn.' });
            }
        } else {
            if (!memoryDB.friend_requests) memoryDB.friend_requests = [];
            const idx = memoryDB.friend_requests.findIndex(r => r.id === requestId);
            if (idx === -1) return res.status(404).json({ error: 'Không tìm thấy lời mời!' });
            const fr = memoryDB.friend_requests[idx];

            if (action === 'ACCEPT') {
                if (!memoryDB.friends) memoryDB.friends = [];
                memoryDB.friends.push({ id: memoryDB.friends.length + 1, user_id: fr.sender_id, friend_id: fr.receiver_id });
                memoryDB.friends.push({ id: memoryDB.friends.length + 1, user_id: fr.receiver_id, friend_id: fr.sender_id });
                memoryDB.friend_requests.splice(idx, 1);
                return res.json({ success: true, message: 'Đã chấp nhận lời mời kết bạn!' });
            } else {
                memoryDB.friend_requests.splice(idx, 1);
                return res.json({ success: true, message: 'Đã từ chối lời mời kết bạn.' });
            }
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// F. GỬI LỜI MỜI GIA NHẬP BANG HỘI
app.post('/api/guilds/invite', async (req, res) => {
    const { inviterUsername, targetUsername, guildId } = req.body;
    try {
        if (!useFallback) {
            const [inviters] = await pool.query('SELECT id FROM users WHERE username = ?', [inviterUsername]);
            const [targets] = await pool.query('SELECT id FROM users WHERE username = ?', [targetUsername]);
            if (inviters.length === 0 || targets.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            await pool.query('INSERT INTO guild_invitations (guild_id, inviter_id, invitee_id, status) VALUES (?, ?, ?, "PENDING")', 
                [guildId, inviters[0].id, targets[0].id]);
            return res.json({ success: true, message: `Đã gửi lời mời gia nhập bang tới ${targetUsername}!` });
        } else {
            if (!memoryDB.guild_invitations) memoryDB.guild_invitations = [];
            const inviter = memoryDB.users.find(u => u.username.toLowerCase() === inviterUsername.toLowerCase());
            const target = memoryDB.users.find(u => u.username.toLowerCase() === targetUsername.toLowerCase());
            if (!inviter || !target) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            memoryDB.guild_invitations.push({ id: memoryDB.guild_invitations.length + 1, guild_id: guildId, inviter_id: inviter.id, invitee_id: target.id, status: 'PENDING' });
            return res.json({ success: true, message: `Đã gửi lời mời gia nhập bang tới ${targetUsername}!` });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// G1. NỘP ĐƠN XIN GIA NHẬP BANG HỘI (Cần Chủ Bang Phê Duyệt)
app.post('/api/guilds/apply', async (req, res) => {
    const { username, guildId } = req.body;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });
            const userId = users[0].id;

            const [members] = await pool.query('SELECT id FROM guild_members WHERE user_id = ?', [userId]);
            if (members.length > 0) return res.status(400).json({ error: 'Bạn đang là thành viên của bang hội khác!' });

            const [existing] = await pool.query('SELECT id, status FROM guild_join_requests WHERE guild_id = ? AND user_id = ?', [guildId, userId]);
            if (existing.length > 0 && existing[0].status === 'PENDING') {
                return res.status(400).json({ error: 'Đơn xin gia nhập của bạn đang chờ chủ bang phê duyệt!' });
            }

            await pool.query(
                'INSERT INTO guild_join_requests (guild_id, user_id, status) VALUES (?, ?, "PENDING") ON DUPLICATE KEY UPDATE status="PENDING", created_at=CURRENT_TIMESTAMP',
                [guildId, userId]
            );
            return res.json({ success: true, message: 'Đã gửi đơn xin gia nhập! Đang chờ Chủ bang phê duyệt.' });
        } else {
            if (!memoryDB.guild_join_requests) memoryDB.guild_join_requests = [];
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            const isMember = memoryDB.guild_members.some(m => m.user_id === user.id);
            if (isMember) return res.status(400).json({ error: 'Bạn đang là thành viên của bang hội khác!' });

            const pending = memoryDB.guild_join_requests.find(r => r.guild_id == guildId && r.user_id === user.id && r.status === 'PENDING');
            if (pending) return res.status(400).json({ error: 'Đơn xin gia nhập của bạn đang chờ chủ bang phê duyệt!' });

            memoryDB.guild_join_requests.push({
                id: memoryDB.guild_join_requests.length + 1,
                guild_id: parseInt(guildId),
                user_id: user.id,
                status: 'PENDING',
                created_at: new Date().toISOString()
            });
            return res.json({ success: true, message: 'Đã gửi đơn xin gia nhập! Đang chờ Chủ bang phê duyệt.' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// G2. HỦY ĐƠN XIN GIA NHẬP BANG HỘI
app.post('/api/guilds/cancel-application', async (req, res) => {
    const { username, guildId, applicationId } = req.body;
    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });
            const userId = users[0].id;

            if (applicationId) {
                await pool.query('DELETE FROM guild_join_requests WHERE id = ? AND user_id = ?', [applicationId, userId]);
            } else if (guildId) {
                await pool.query('DELETE FROM guild_join_requests WHERE guild_id = ? AND user_id = ?', [guildId, userId]);
            }
            return res.json({ success: true, message: 'Đã hủy đơn xin gia nhập bang hội.' });
        } else {
            if (!memoryDB.guild_join_requests) memoryDB.guild_join_requests = [];
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            memoryDB.guild_join_requests = memoryDB.guild_join_requests.filter(r => {
                if (applicationId && r.id === parseInt(applicationId) && r.user_id === user.id) return false;
                if (guildId && r.guild_id === parseInt(guildId) && r.user_id === user.id) return false;
                return true;
            });
            return res.json({ success: true, message: 'Đã hủy đơn xin gia nhập bang hội.' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// G3. LẤY DANH SÁCH ĐƠN XIN GIA NHẬP CHỜ PHÊ DUYỆT (Dành cho Chủ Bang)
app.get('/api/guilds/applications/:guildId', async (req, res) => {
    const guildId = parseInt(req.params.guildId);
    try {
        if (!useFallback) {
            const [rows] = await pool.query(`
                SELECT r.id, r.guild_id, r.user_id, r.status, r.created_at,
                       u.username, u.avatar, u.level, u.total_reps, g.name as guild_name
                FROM guild_join_requests r
                JOIN users u ON r.user_id = u.id
                JOIN guilds g ON r.guild_id = g.id
                WHERE r.guild_id = ? AND r.status = 'PENDING'
                ORDER BY r.id DESC
            `, [guildId]);
            return res.json({ success: true, applications: rows });
        } else {
            if (!memoryDB.guild_join_requests) memoryDB.guild_join_requests = [];
            const pendingReqs = memoryDB.guild_join_requests.filter(r => r.guild_id === guildId && r.status === 'PENDING');
            const applications = pendingReqs.map(r => {
                const u = memoryDB.users.find(user => user.id === r.user_id) || {};
                const g = memoryDB.guilds.find(guild => guild.id === r.guild_id) || {};
                return {
                    id: r.id,
                    guild_id: r.guild_id,
                    user_id: r.user_id,
                    status: r.status,
                    created_at: r.created_at,
                    username: u.username || 'Chiến Binh',
                    avatar: u.avatar || '🧑‍🎤',
                    level: u.level || 1,
                    total_reps: u.total_reps || 0,
                    guild_name: g.name || ''
                };
            });
            return res.json({ success: true, applications });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// G4. PHÊ DUYỆT ĐƠN XIN GIA NHẬP (Chủ Bang chấp thuận)
app.post('/api/guilds/approve-application', async (req, res) => {
    const { leaderUsername, applicationId } = req.body;
    try {
        if (!useFallback) {
            const [appRows] = await pool.query('SELECT * FROM guild_join_requests WHERE id = ?', [applicationId]);
            if (appRows.length === 0) return res.status(404).json({ error: 'Không tìm thấy đơn xin gia nhập!' });
            const application = appRows[0];

            // Thêm vào thành viên bang
            await pool.query('INSERT IGNORE INTO guild_members (guild_id, user_id, role) VALUES (?, ?, "MEMBER")',
                [application.guild_id, application.user_id]);

            // Cập nhật trạng thái đơn
            await pool.query('UPDATE guild_join_requests SET status = "ACCEPTED" WHERE id = ?', [applicationId]);
            // Xóa các đơn xin gia nhập bang khác của người chơi này
            await pool.query('DELETE FROM guild_join_requests WHERE user_id = ? AND id != ?', [application.user_id, applicationId]);

            const [applicant] = await pool.query('SELECT username FROM users WHERE id = ?', [application.user_id]);
            const applicantName = applicant.length > 0 ? applicant[0].username : 'Thành viên mới';
            return res.json({ success: true, message: `Đã phê duyệt ${applicantName} gia nhập bang hội!` });
        } else {
            if (!memoryDB.guild_join_requests) memoryDB.guild_join_requests = [];
            const appIdx = memoryDB.guild_join_requests.findIndex(r => r.id === parseInt(applicationId));
            if (appIdx === -1) return res.status(404).json({ error: 'Không tìm thấy đơn xin gia nhập!' });
            const application = memoryDB.guild_join_requests[appIdx];

            // Thêm vào guild_members
            if (!memoryDB.guild_members) memoryDB.guild_members = [];
            if (!memoryDB.guild_members.some(m => m.user_id === application.user_id)) {
                memoryDB.guild_members.push({
                    id: memoryDB.guild_members.length + 1,
                    guild_id: application.guild_id,
                    user_id: application.user_id,
                    role: 'MEMBER'
                });
            }

            memoryDB.guild_join_requests.splice(appIdx, 1);
            // Xóa đơn xin khác
            memoryDB.guild_join_requests = memoryDB.guild_join_requests.filter(r => r.user_id !== application.user_id);

            const user = memoryDB.users.find(u => u.id === application.user_id);
            const applicantName = user ? user.username : 'Thành viên mới';
            return res.json({ success: true, message: `Đã phê duyệt ${applicantName} gia nhập bang hội!` });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// G5. TỪ CHỐI ĐƠN XIN GIA NHẬP (Chủ Bang từ chối)
app.post('/api/guilds/reject-application', async (req, res) => {
    const { leaderUsername, applicationId } = req.body;
    try {
        if (!useFallback) {
            await pool.query('UPDATE guild_join_requests SET status = "REJECTED" WHERE id = ?', [applicationId]);
            return res.json({ success: true, message: 'Đã từ chối đơn xin gia nhập bang hội.' });
        } else {
            if (!memoryDB.guild_join_requests) memoryDB.guild_join_requests = [];
            const appIdx = memoryDB.guild_join_requests.findIndex(r => r.id === parseInt(applicationId));
            if (appIdx !== -1) {
                memoryDB.guild_join_requests.splice(appIdx, 1);
            }
            return res.json({ success: true, message: 'Đã từ chối đơn xin gia nhập bang hội.' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// D. BẢNG XẾP HẠNG THẾ GIỚI (World Leaderboard theo Lực Chiến, Cấp Độ, Reps, Streak)
app.get('/api/leaderboard', async (req, res) => {
    const { username, sortBy = 'CP' } = req.query;
    try {
        if (!useFallback) {
            let orderClause = 'combat_power DESC';
            if (sortBy === 'LEVEL') orderClause = 'u.level DESC, combat_power DESC';
            else if (sortBy === 'REPS') orderClause = 'u.total_reps DESC, combat_power DESC';
            else if (sortBy === 'STREAK') orderClause = 'u.streak DESC, combat_power DESC';

            let currentUserId = null;
            if (username) {
                const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
                if (users.length > 0) currentUserId = users[0].id;
            }

            const [rows] = await pool.query(`
                SELECT u.id, u.username, u.avatar, u.level, u.streak, u.total_reps,
                    (50 + u.total_reps * 25 + u.level * 30) AS combat_power,
                    CASE 
                        WHEN u.level < 3 THEN 'Tân Binh'
                        WHEN u.level < 6 THEN 'Võ Tăng'
                        WHEN u.level < 10 THEN 'Chiến Binh'
                        ELSE 'Huyền Thoại'
                    END AS title,
                    ${currentUserId ? `(EXISTS (SELECT 1 FROM friends WHERE user_id = ${currentUserId} AND friend_id = u.id))` : 'FALSE'} AS is_friend,
                    ${currentUserId ? `(u.id = ${currentUserId})` : 'FALSE'} AS is_current_user
                FROM users u
                ORDER BY ${orderClause}
                LIMIT 50
            `);

            const leaderboard = rows.map((r, idx) => ({
                rank: idx + 1,
                id: r.id,
                username: r.username,
                avatar: r.avatar || '🧑‍🎤',
                level: r.level,
                combatPower: r.combat_power,
                totalReps: r.total_reps,
                streak: r.streak,
                title: r.title,
                isFriend: Boolean(r.is_friend),
                isCurrentUser: Boolean(r.is_current_user)
            }));

            return res.json({ success: true, sortBy, leaderboard });
        } else {
            let currentUserId = null;
            if (username) {
                const found = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
                if (found) currentUserId = found.id;
            }

            let sorted = [...memoryDB.users].map(u => ({
                ...u,
                combat_power: 50 + u.total_reps * 25 + u.level * 30,
                title: u.level < 3 ? 'Tân Binh' : u.level < 6 ? 'Võ Tăng' : u.level < 10 ? 'Chiến Binh' : 'Huyền Thoại',
                is_friend: currentUserId && memoryDB.friends ? memoryDB.friends.some(f => f.user_id === currentUserId && f.friend_id === u.id) : false,
                is_current_user: currentUserId ? u.id === currentUserId : false
            }));

            if (sortBy === 'LEVEL') sorted.sort((a, b) => b.level - a.level || b.combat_power - a.combat_power);
            else if (sortBy === 'REPS') sorted.sort((a, b) => b.total_reps - a.total_reps || b.combat_power - a.combat_power);
            else if (sortBy === 'STREAK') sorted.sort((a, b) => b.streak - a.streak || b.combat_power - a.combat_power);
            else sorted.sort((a, b) => b.combat_power - a.combat_power);

            const leaderboard = sorted.map((u, idx) => ({
                rank: idx + 1,
                id: u.id,
                username: u.username,
                avatar: u.avatar || '🧑‍🎤',
                level: u.level,
                combatPower: u.combat_power,
                totalReps: u.total_reps,
                streak: u.streak,
                title: u.title,
                isFriend: Boolean(u.is_friend),
                isCurrentUser: Boolean(u.is_current_user)
            }));

            return res.json({ success: true, sortBy, leaderboard });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ====================================================================
// G. BOSS THẾ GIỚI BANG HỘI (GUILD WORLD BOSS RAID APIs)
// ====================================================================

// Danh mục các Siêu Trùm Thế Giới xoay tua
const WORLD_BOSS_TEMPLATES = [
    {
        bossId: 'boss_nether_dragon',
        name: 'Hắc Long Viễn Cổ - Nidhogg',
        title: 'SIÊU TRÙM THẾ GIỚI BANG HỘI',
        avatar: '🐉',
        maxHp: 500000,
        rewardGold: 15000,
        rewardGems: 350,
        rewardItemId: 'weapon_dragon_slayer',
        rewardItemName: 'Đại Đao Trảm Long'
    },
    {
        bossId: 'boss_inferno_titan',
        name: 'Cự Nhân Hỏa Ngục - Surtr',
        title: 'SIÊU TRÙM THẾ GIỚI BANG HỘI',
        avatar: '🌋',
        maxHp: 650000,
        rewardGold: 18000,
        rewardGems: 400,
        rewardItemId: 'armor_dragon_scale',
        rewardItemName: 'Long Lân Thần Giáp'
    },
    {
        bossId: 'boss_void_behemoth',
        name: 'Thần Thú Hư Không - Leviathan',
        title: 'SIÊU TRÙM THẾ GIỚI BANG HỘI',
        avatar: '🐲',
        maxHp: 800000,
        rewardGold: 22000,
        rewardGems: 500,
        rewardItemId: 'amulet_boss_heart',
        rewardItemName: 'Trái Tim Hắc Long'
    }
];

// 1. Lấy thông tin Boss Thế Giới hiện tại của Bang và danh sách đóng góp sát thương
app.get('/api/guild/boss', async (req, res) => {
    const guildId = parseInt(req.query.guildId) || 1;
    const username = req.query.username || '';

    try {
        if (!useFallback) {
            // Kiểm tra Boss hiện tại của bang
            let [bosses] = await pool.query(
                'SELECT * FROM guild_boss WHERE guild_id = ? ORDER BY id DESC LIMIT 1',
                [guildId]
            );

            let boss = bosses.length > 0 ? bosses[0] : null;
            if (!boss) {
                // Tạo mới boss mặc định nếu chưa có
                const tpl = WORLD_BOSS_TEMPLATES[0];
                const [ins] = await pool.query(
                    `INSERT INTO guild_boss (guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id)
                     VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)`,
                    [guildId, tpl.bossId, tpl.name, tpl.title, tpl.avatar, tpl.maxHp, tpl.maxHp, tpl.rewardGold, tpl.rewardGems, tpl.rewardItemId]
                );
                const [created] = await pool.query('SELECT * FROM guild_boss WHERE id = ?', [ins.insertId]);
                boss = created[0];
            }

            // Lấy danh sách thành viên đóng góp sát thương
            const [rows] = await pool.query(`
                SELECT gbd.user_id, u.username, u.avatar, u.level, gbd.damage, gbd.reps_contributed, gbd.has_claimed_defeat_reward
                FROM guild_boss_damage gbd
                JOIN users u ON gbd.user_id = u.id
                WHERE gbd.boss_db_id = ?
                ORDER BY gbd.damage DESC
            `, [boss.id]);

            const totalDamageDealt = rows.reduce((acc, r) => acc + (r.damage || 0), 0);
            const contributors = rows.map((r, idx) => ({
                userId: r.user_id,
                username: r.username,
                avatar: r.avatar || '🧑‍🎤',
                level: r.level,
                damage: r.damage,
                reps: r.reps_contributed,
                percentage: totalDamageDealt > 0 ? parseFloat(((r.damage / totalDamageDealt) * 100).toFixed(1)) : 0,
                rank: idx + 1,
                hasClaimed: Boolean(r.has_claimed_defeat_reward)
            }));

            // Tìm thông tin đóng góp của người chơi hiện tại
            let myContribution = { damage: 0, reps: 0, percentage: 0, rank: 0, hasClaimedDefeatReward: false };
            if (username) {
                const found = contributors.find(c => c.username.toLowerCase() === username.toLowerCase());
                if (found) {
                    myContribution = {
                        damage: found.damage,
                        reps: found.reps,
                        percentage: found.percentage,
                        rank: found.rank,
                        hasClaimedDefeatReward: found.hasClaimed
                    };
                }
            }

            // Lấy thông tin chi tiết vật phẩm phần thưởng
            const [itemRows] = await pool.query('SELECT name FROM items WHERE id = ?', [boss.reward_item_id]);
            const rewardItemName = itemRows.length > 0 ? itemRows[0].name : 'Vật Phẩm Huyền Thoại';

            return res.json({
                success: true,
                boss: {
                    id: boss.id,
                    guildId: boss.guild_id,
                    bossId: boss.boss_id,
                    name: boss.boss_name,
                    title: boss.boss_title,
                    avatar: boss.boss_avatar,
                    maxHp: boss.max_hp,
                    currentHp: boss.current_hp,
                    status: boss.status,
                    rewardGold: boss.reward_gold,
                    rewardGems: boss.reward_gems,
                    rewardItemId: boss.reward_item_id,
                    rewardItemName: rewardItemName
                },
                contributors,
                myContribution
            });
        } else {
            // Chế độ In-Memory Fallback
            if (!memoryDB.guild_bosses) memoryDB.guild_bosses = [];
            let boss = memoryDB.guild_bosses.slice().reverse().find(b => b.guild_id == guildId);
            if (!boss) {
                const tpl = WORLD_BOSS_TEMPLATES[0];
                boss = {
                    id: memoryDB.guild_bosses.length + 1,
                    guild_id: guildId,
                    boss_id: tpl.bossId,
                    boss_name: tpl.name,
                    boss_title: tpl.title,
                    boss_avatar: tpl.avatar,
                    max_hp: tpl.maxHp,
                    current_hp: tpl.maxHp,
                    status: 'ACTIVE',
                    reward_gold: tpl.rewardGold,
                    reward_gems: tpl.rewardGems,
                    reward_item_id: tpl.rewardItemId
                };
                memoryDB.guild_bosses.push(boss);
            }

            if (!memoryDB.guild_boss_damage) memoryDB.guild_boss_damage = [];
            const damages = memoryDB.guild_boss_damage.filter(d => d.boss_db_id === boss.id);
            damages.sort((a, b) => b.damage - a.damage);

            const totalDamageDealt = damages.reduce((acc, d) => acc + d.damage, 0);
            const contributors = damages.map((d, idx) => {
                const u = memoryDB.users.find(usr => usr.id === d.user_id) || { username: 'Anh Hùng', avatar: '🧑‍🎤', level: 1 };
                return {
                    userId: d.user_id,
                    username: u.username,
                    avatar: u.avatar || '🧑‍🎤',
                    level: u.level || 1,
                    damage: d.damage,
                    reps: d.reps_contributed || 0,
                    percentage: totalDamageDealt > 0 ? parseFloat(((d.damage / totalDamageDealt) * 100).toFixed(1)) : 0,
                    rank: idx + 1,
                    hasClaimed: Boolean(d.has_claimed_defeat_reward)
                };
            });

            let myContribution = { damage: 0, reps: 0, percentage: 0, rank: 0, hasClaimedDefeatReward: false };
            if (username) {
                const found = contributors.find(c => c.username.toLowerCase() === username.toLowerCase());
                if (found) {
                    myContribution = {
                        damage: found.damage,
                        reps: found.reps,
                        percentage: found.percentage,
                        rank: found.rank,
                        hasClaimedDefeatReward: found.hasClaimed
                    };
                }
            }

            const item = (memoryDB.items || []).find(it => it.id === boss.reward_item_id);
            const rewardItemName = item ? item.name : 'Vật Phẩm Huyền Thoại';

            return res.json({
                success: true,
                boss: {
                    id: boss.id,
                    guildId: boss.guild_id,
                    bossId: boss.boss_id,
                    name: boss.boss_name,
                    title: boss.boss_title,
                    avatar: boss.boss_avatar,
                    maxHp: boss.max_hp,
                    currentHp: boss.current_hp,
                    status: boss.status,
                    rewardGold: boss.reward_gold,
                    rewardGems: boss.reward_gems,
                    rewardItemId: boss.reward_item_id,
                    rewardItemName: rewardItemName
                },
                contributors,
                myContribution
            });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 2. Tấn công Boss Thế Giới (Góp sát thương từ buổi tập)
app.post('/api/guild/boss/attack', async (req, res) => {
    const { guildId, username, damage, reps, exercise } = req.body;
    const dmg = Math.max(1, parseInt(damage) || 100);
    const repCount = Math.max(1, parseInt(reps) || 1);

    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id, gold, xp, total_reps FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });
            const user = users[0];

            let [bosses] = await pool.query(
                'SELECT * FROM guild_boss WHERE guild_id = ? AND status = "ACTIVE" ORDER BY id DESC LIMIT 1',
                [guildId]
            );

            if (bosses.length === 0) {
                return res.status(400).json({ error: 'Boss hiện tại đã bị tiêu diệt hoặc chưa được triệu hồi!' });
            }
            const boss = bosses[0];

            // Trừ máu Boss
            const newHp = Math.max(0, boss.current_hp - dmg);
            const isDefeated = newHp === 0;

            await pool.query(
                'UPDATE guild_boss SET current_hp = ?, status = ?, defeated_at = ? WHERE id = ?',
                [newHp, isDefeated ? 'DEFEATED' : 'ACTIVE', isDefeated ? new Date() : null, boss.id]
            );

            // Ghi nhận sát thương đóng góp của thành viên
            await pool.query(`
                INSERT INTO guild_boss_damage (guild_id, boss_db_id, user_id, damage, reps_contributed)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    damage = damage + VALUES(damage),
                    reps_contributed = reps_contributed + VALUES(reps_contributed)
            `, [guildId, boss.id, user.id, dmg, repCount]);

            // Thưởng nhanh cho thành viên mỗi lượt tập: vàng = reps * 2, xp = reps * 5
            const effortGold = repCount * 2;
            const effortXp = repCount * 5;
            await pool.query(
                'UPDATE users SET gold = gold + ?, xp = xp + ?, total_reps = total_reps + ? WHERE id = ?',
                [effortGold, effortXp, repCount, user.id]
            );

            // Ghi log vào workouts
            await pool.query(
                'INSERT INTO workouts (user_id, exercise, reps, score, xp_earned, gold_earned) VALUES (?, ?, ?, ?, ?, ?)',
                [user.id, exercise || 'SQUAT', repCount, dmg, effortXp, effortGold]
            );

            return res.json({
                success: true,
                message: isDefeated ? `🎉 Tuyệt đỉnh! Bạn và bang hội đã kết liễu ${boss.boss_name}!` : `⚔️ Đã gây ${dmg} sát thương lên ${boss.boss_name}!`,
                damageDealt: dmg,
                reps: repCount,
                bossRemainingHp: newHp,
                isDefeated: isDefeated,
                effortGold,
                effortXp
            });
        } else {
            // Fallback in-memory
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            if (!memoryDB.guild_bosses) memoryDB.guild_bosses = [];
            let boss = memoryDB.guild_bosses.slice().reverse().find(b => b.guild_id == guildId && b.status === 'ACTIVE');
            if (!boss) return res.status(400).json({ error: 'Boss hiện tại đã bị tiêu diệt hoặc chưa được triệu hồi!' });

            const newHp = Math.max(0, boss.current_hp - dmg);
            const isDefeated = newHp === 0;
            boss.current_hp = newHp;
            if (isDefeated) {
                boss.status = 'DEFEATED';
                boss.defeated_at = new Date().toISOString();
            }

            if (!memoryDB.guild_boss_damage) memoryDB.guild_boss_damage = [];
            let record = memoryDB.guild_boss_damage.find(d => d.boss_db_id === boss.id && d.user_id === user.id);
            if (record) {
                record.damage += dmg;
                record.reps_contributed = (record.reps_contributed || 0) + repCount;
            } else {
                memoryDB.guild_boss_damage.push({
                    id: memoryDB.guild_boss_damage.length + 1,
                    guild_id: guildId,
                    boss_db_id: boss.id,
                    user_id: user.id,
                    damage: dmg,
                    reps_contributed: repCount,
                    has_claimed_defeat_reward: false
                });
            }

            const effortGold = repCount * 2;
            const effortXp = repCount * 5;
            user.gold += effortGold;
            user.xp += effortXp;
            user.total_reps += repCount;

            return res.json({
                success: true,
                message: isDefeated ? `🎉 Tuyệt đỉnh! Bạn và bang hội đã kết liễu ${boss.boss_name}!` : `⚔️ Đã gây ${dmg} sát thương lên ${boss.boss_name}!`,
                damageDealt: dmg,
                reps: repCount,
                bossRemainingHp: newHp,
                isDefeated: isDefeated,
                effortGold,
                effortXp
            });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 3. Nhận phần thưởng siêu hậu hĩnh khi Boss Thế Giới bị tiêu diệt
app.post('/api/guild/boss/claim', async (req, res) => {
    const { guildId, username } = req.body;

    try {
        if (!useFallback) {
            const [users] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
            if (users.length === 0) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });
            const user = users[0];

            const [bosses] = await pool.query(
                'SELECT * FROM guild_boss WHERE guild_id = ? AND status = "DEFEATED" ORDER BY id DESC LIMIT 1',
                [guildId]
            );
            if (bosses.length === 0) return res.status(400).json({ error: 'Boss chưa bị tiêu diệt, hãy cùng bang hội tiếp tục chiến đấu!' });
            const boss = bosses[0];

            const [damageRows] = await pool.query(
                'SELECT * FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?',
                [boss.id, user.id]
            );
            if (damageRows.length === 0) return res.status(400).json({ error: 'Bạn chưa góp sát thương cho chiến dịch diệt Boss này!' });
            if (damageRows[0].has_claimed_defeat_reward) return res.status(400).json({ error: 'Bạn đã nhận phần thưởng cho lần diệt Boss này rồi!' });

            // Trao thưởng Vàng & Kim Cương
            await pool.query('UPDATE users SET gold = gold + ?, gems = gems + ? WHERE id = ?', [boss.reward_gold, boss.reward_gems, user.id]);

            // Trao Trang Bị Huyền Thoại vào kho đồ
            if (boss.reward_item_id) {
                await pool.query('INSERT INTO user_inventory (user_id, item_id, is_equipped) VALUES (?, ?, FALSE)', [user.id, boss.reward_item_id]);
            }

            // Đánh dấu đã nhận thưởng
            await pool.query('UPDATE guild_boss_damage SET has_claimed_defeat_reward = TRUE WHERE boss_db_id = ? AND user_id = ?', [boss.id, user.id]);

            const [itemRows] = await pool.query('SELECT name, icon FROM items WHERE id = ?', [boss.reward_item_id]);
            const itemName = itemRows.length > 0 ? itemRows[0].name : 'Vật Phẩm Huyền Thoại';
            const itemIcon = itemRows.length > 0 ? itemRows[0].icon : '🎁';

            return res.json({
                success: true,
                message: `🎉 Chúc mừng! Bạn nhận được ${boss.reward_gold} Vàng, ${boss.reward_gems} Kim Cương và ${itemIcon} ${itemName}!`,
                rewardGold: boss.reward_gold,
                rewardGems: boss.reward_gems,
                rewardItemId: boss.reward_item_id,
                rewardItemName: itemName
            });
        } else {
            const user = memoryDB.users.find(u => u.username.toLowerCase() === username.toLowerCase());
            if (!user) return res.status(404).json({ error: 'Không tìm thấy người chơi!' });

            let boss = (memoryDB.guild_bosses || []).slice().reverse().find(b => b.guild_id == guildId && b.status === 'DEFEATED');
            if (!boss) return res.status(400).json({ error: 'Boss chưa bị tiêu diệt, hãy cùng bang hội tiếp tục chiến đấu!' });

            let record = (memoryDB.guild_boss_damage || []).find(d => d.boss_db_id === boss.id && d.user_id === user.id);
            if (!record) return res.status(400).json({ error: 'Bạn chưa góp sát thương cho chiến dịch diệt Boss này!' });
            if (record.has_claimed_defeat_reward) return res.status(400).json({ error: 'Bạn đã nhận phần thưởng rồi!' });

            user.gold += boss.reward_gold;
            user.gems += boss.reward_gems;
            record.has_claimed_defeat_reward = true;

            if (boss.reward_item_id) {
                if (!memoryDB.inventory) memoryDB.inventory = [];
                memoryDB.inventory.push({
                    id: memoryDB.inventory.length + 1,
                    user_id: user.id,
                    item_id: boss.reward_item_id,
                    is_equipped: 0
                });
            }

            const item = (memoryDB.items || []).find(i => i.id === boss.reward_item_id);
            const itemName = item ? item.name : 'Vật Phẩm Huyền Thoại';

            return res.json({
                success: true,
                message: `🎉 Chúc mừng! Bạn nhận được ${boss.reward_gold} Vàng, ${boss.reward_gems} Kim Cương và ${itemName}!`,
                rewardGold: boss.reward_gold,
                rewardGems: boss.reward_gems,
                rewardItemId: boss.reward_item_id,
                rewardItemName: itemName
            });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 4. Triệu hồi Boss Thế Giới mới sau khi boss cũ bị hạ gục
app.post('/api/guild/boss/summon', async (req, res) => {
    const { guildId, username } = req.body;
    try {
        // Chọn ngẫu nhiên hoặc xoay tua 1 Siêu Boss trong danh sách
        const nextTemplate = WORLD_BOSS_TEMPLATES[Math.floor(Math.random() * WORLD_BOSS_TEMPLATES.length)];

        if (!useFallback) {
            const [ins] = await pool.query(
                `INSERT INTO guild_boss (guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id)
                 VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)`,
                [guildId, nextTemplate.bossId, nextTemplate.name, nextTemplate.title, nextTemplate.avatar, nextTemplate.maxHp, nextTemplate.maxHp, nextTemplate.rewardGold, nextTemplate.rewardGems, nextTemplate.rewardItemId]
            );

            return res.json({
                success: true,
                message: `🔥 Tiếng gầm thét rung chuyển! ${nextTemplate.name} đã giáng lâm khiêu chiến bang hội!`,
                bossId: ins.insertId
            });
        } else {
            const newBoss = {
                id: (memoryDB.guild_bosses || []).length + 1,
                guild_id: guildId,
                boss_id: nextTemplate.bossId,
                boss_name: nextTemplate.name,
                boss_title: nextTemplate.title,
                boss_avatar: nextTemplate.avatar,
                max_hp: nextTemplate.maxHp,
                current_hp: nextTemplate.maxHp,
                status: 'ACTIVE',
                reward_gold: nextTemplate.rewardGold,
                reward_gems: nextTemplate.rewardGems,
                reward_item_id: nextTemplate.rewardItemId
            };
            if (!memoryDB.guild_bosses) memoryDB.guild_bosses = [];
            memoryDB.guild_bosses.push(newBoss);

            return res.json({
                success: true,
                message: `🔥 Tiếng gầm thét rung chuyển! ${nextTemplate.name} đã giáng lâm khiêu chiến bang hội!`,
                bossId: newBoss.id
            });
        }
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
