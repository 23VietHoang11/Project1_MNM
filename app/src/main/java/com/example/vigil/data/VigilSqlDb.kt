package com.example.vigil.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vigil.model.DailyCheckInReward
import com.example.vigil.model.FriendProfile
import com.example.vigil.model.Guild
import com.example.vigil.model.GuildMember
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.LeaderboardEntry
import com.example.vigil.model.QuestItem
import com.example.vigil.model.Rarity
import com.example.vigil.model.UserProfile
import com.example.vigil.model.WorkoutReward
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.min
import kotlin.random.Random

/**
 * Cơ sở dữ liệu SQL cục bộ (SQLite) tương thích 100% với cấu trúc bảng MySQL của Vigil.
 * Chạy trực tiếp trên thiết bị Android, lưu trữ bền vững kể cả khi offline.
 */
class VigilSqlDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vigil_app.db"
        const val DATABASE_VERSION = 3

        @Volatile
        private var INSTANCE: VigilSqlDb? = null

        fun get(context: Context): VigilSqlDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VigilSqlDb(context.applicationContext).also { INSTANCE = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Bảng users (Hỗ trợ xác thực đăng ký & đăng nhập)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL DEFAULT '123456',
                avatar TEXT NOT NULL DEFAULT '🧑‍🎤',
                gold INTEGER NOT NULL DEFAULT 0,
                gems INTEGER NOT NULL DEFAULT 0,
                xp INTEGER NOT NULL DEFAULT 0,
                level INTEGER NOT NULL DEFAULT 1,
                streak INTEGER NOT NULL DEFAULT 0,
                total_reps INTEGER NOT NULL DEFAULT 0,
                stage INTEGER NOT NULL DEFAULT 1,
                last_workout_date TEXT
            );
        """.trimIndent())

        // 2. Bảng items (Cửa hàng & Danh mục rớt đồ)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS items (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                slot TEXT NOT NULL,
                rarity TEXT NOT NULL,
                price_gold INTEGER NOT NULL DEFAULT 0,
                price_gems INTEGER NOT NULL DEFAULT 0,
                bonus_str INTEGER NOT NULL DEFAULT 0,
                bonus_end INTEGER NOT NULL DEFAULT 0,
                bonus_pre INTEGER NOT NULL DEFAULT 0,
                bonus_luck INTEGER NOT NULL DEFAULT 0,
                description TEXT,
                icon TEXT NOT NULL DEFAULT '⚔️'
            );
        """.trimIndent())

        // 3. Bảng user_inventory
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_inventory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                item_id TEXT NOT NULL,
                is_equipped INTEGER NOT NULL DEFAULT 0,
                acquired_at TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (item_id) REFERENCES items(id)
            );
        """.trimIndent())

        // 4. Bảng workouts
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                exercise TEXT NOT NULL,
                reps INTEGER NOT NULL DEFAULT 0,
                hold_seconds INTEGER NOT NULL DEFAULT 0,
                score INTEGER NOT NULL,
                xp_earned INTEGER NOT NULL,
                gold_earned INTEGER NOT NULL,
                dropped_item_id TEXT,
                created_at TEXT
            );
        """.trimIndent())

        // 5. Bảng friends (Kết bạn giữa các người chơi bằng tên)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS friends (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                friend_id INTEGER NOT NULL,
                created_at TEXT,
                UNIQUE(user_id, friend_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (friend_id) REFERENCES users(id)
            );
        """.trimIndent())

        // 6. Bảng bang hội (Guilds)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                badge TEXT NOT NULL DEFAULT '🛡️',
                slogan TEXT NOT NULL,
                leader_id INTEGER NOT NULL,
                level INTEGER NOT NULL DEFAULT 1,
                created_at TEXT
            );
        """.trimIndent())

        // 7. Bảng thành viên bang hội (Guild Members)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL UNIQUE,
                role TEXT NOT NULL DEFAULT 'MEMBER',
                joined_at TEXT,
                FOREIGN KEY (guild_id) REFERENCES guilds(id),
                FOREIGN KEY (user_id) REFERENCES users(id)
            );
        """.trimIndent())

        // 8. Bảng ghi nhận lần đầu hạ gục Boss để thưởng Gem (Boss First Clear)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS boss_clears (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                stage_id INTEGER NOT NULL,
                cleared_at TEXT,
                UNIQUE(user_id, stage_id)
            );
        """.trimIndent())

        // 9. Bảng điểm danh tuần (Weekly Login Check-in)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS weekly_checkins (
                user_id INTEGER PRIMARY KEY,
                checkin_day INTEGER NOT NULL DEFAULT 0,
                last_date TEXT,
                week_key TEXT
            );
        """.trimIndent())

        // 10. Bảng nhận thưởng nhiệm vụ (Quest Claims)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS quest_claims (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                quest_id TEXT NOT NULL,
                period_key TEXT NOT NULL,
                claimed_at TEXT,
                UNIQUE(user_id, quest_id, period_key)
            );
        """.trimIndent())

        // Nạp dữ liệu mặc định ban đầu
        seedItems(db)
        seedInitialUser(db)
        seedSampleLeaderboardUsers(db)
        seedSampleGuilds(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE users ADD COLUMN password TEXT NOT NULL DEFAULT '123456'") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN avatar TEXT NOT NULL DEFAULT '🧑‍🎤'") } catch (e: Exception) {}
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS friends (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    friend_id INTEGER NOT NULL,
                    created_at TEXT,
                    UNIQUE(user_id, friend_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (friend_id) REFERENCES users(id)
                );
            """.trimIndent())
            seedSampleLeaderboardUsers(db)
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS guilds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    badge TEXT NOT NULL DEFAULT '🛡️',
                    slogan TEXT NOT NULL,
                    leader_id INTEGER NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS guild_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    guild_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL UNIQUE,
                    role TEXT NOT NULL DEFAULT 'MEMBER',
                    joined_at TEXT,
                    FOREIGN KEY (guild_id) REFERENCES guilds(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS boss_clears (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    stage_id INTEGER NOT NULL,
                    cleared_at TEXT,
                    UNIQUE(user_id, stage_id)
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS weekly_checkins (
                    user_id INTEGER PRIMARY KEY,
                    checkin_day INTEGER NOT NULL DEFAULT 0,
                    last_date TEXT,
                    week_key TEXT
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS quest_claims (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    quest_id TEXT NOT NULL,
                    period_key TEXT NOT NULL,
                    claimed_at TEXT,
                    UNIQUE(user_id, quest_id, period_key)
                );
            """.trimIndent())
            seedSampleGuilds(db)
        }
    }

    private fun seedInitialUser(db: SQLiteDatabase) {
        val cv = ContentValues().apply {
            put("id", 1)
            put("username", "Hachimi")
            put("password", "123456")
            put("avatar", "🦊")
            put("gold", 0)
            put("gems", 0)
            put("xp", 120)
            put("level", 5)
            put("streak", 4)
            put("total_reps", 160)
            put("stage", 3)
            put("last_workout_date", LocalDate.now().toString())
        }
        db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_IGNORE)

        // Tặng tân binh 1 mũ đồng và găng tay cơ bản
        val invCv = ContentValues().apply {
            put("user_id", 1)
            put("item_id", "helm_bronze")
            put("is_equipped", 1)
            put("acquired_at", LocalDate.now().toString())
        }
        db.insertWithOnConflict("user_inventory", null, invCv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun seedSampleLeaderboardUsers(db: SQLiteDatabase) {
        val sampleUsers = listOf(
            Triple("ShadowBlade", 9 to 380, Triple("🥷", 12, 850)),
            Triple("ValkyrieGym", 8 to 310, Triple("👑", 8, 700)),
            Triple("IronTitan", 7 to 260, Triple("🗿", 5, 450)),
            Triple("DragonFit", 6 to 190, Triple("🐉", 4, 320)),
            Triple("Phoenix", 4 to 110, Triple("🔥", 2, 220))
        )

        sampleUsers.forEachIndexed { idx, (uname, stats, extra) ->
            val uCv = ContentValues().apply {
                put("id", idx + 2)
                put("username", uname)
                put("password", "123456")
                put("avatar", extra.first)
                put("gold", extra.third)
                put("gems", 10)
                put("xp", 60)
                put("level", stats.first)
                put("streak", extra.second)
                put("total_reps", stats.second)
                put("stage", stats.first - 1)
                put("last_workout_date", LocalDate.now().toString())
            }
            db.insertWithOnConflict("users", null, uCv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        // Tạo sẵn mối quan hệ bạn bè mẫu cho Hachimi với IronTitan (ID 4)
        val f1 = ContentValues().apply {
            put("user_id", 1)
            put("friend_id", 4)
            put("created_at", LocalDate.now().toString())
        }
        val f2 = ContentValues().apply {
            put("user_id", 4)
            put("friend_id", 1)
            put("created_at", LocalDate.now().toString())
        }
        db.insertWithOnConflict("friends", null, f1, SQLiteDatabase.CONFLICT_IGNORE)
        db.insertWithOnConflict("friends", null, f2, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun seedItems(db: SQLiteDatabase) {
        val itemsList = listOf(
            // MŨ GIÁP
            Item("helm_bronze", "Mũ Đồng Tân Binh", ItemSlot.HELMET, Rarity.COMMON, 50, 0, 0, 3, 0, 1, "Mũ đồng đúc thô sơ che chắn đầu khi tập nặng.", "🪖"),
            Item("helm_iron", "Thiết Giáp Đầu", ItemSlot.HELMET, Rarity.RARE, 160, 0, 3, 7, 0, 2, "Rèn từ sắt non tôi luyện nhiệt độ cao.", "⛑️"),
            Item("helm_valkyrie", "Mũ Lông Vũ Valkyrie", ItemSlot.HELMET, Rarity.EPIC, 420, 5, 5, 14, 0, 8, "Ban phước bởi nữ thần chiến trận phương Bắc.", "👑"),
            Item("helm_dragon", "Vương Miện Long Thần", ItemSlot.HELMET, Rarity.LEGENDARY, 1000, 20, 18, 25, 0, 12, "Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.", "🐉"),

            // GIÁP NGỰC
            Item("armor_leather", "Áo Da Dã Ngoại", ItemSlot.ARMOR, Rarity.COMMON, 60, 0, 0, 4, 2, 0, "Áo da bò mềm mại, thoáng mát cho buổi hít đất dài.", "🥋"),
            Item("armor_plate", "Chiến Giáp Thép Nung", ItemSlot.ARMOR, Rarity.RARE, 180, 0, 5, 10, 0, 0, "Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.", "🛡️"),
            Item("armor_shadow", "Áo Choàng Bóng Đêm", ItemSlot.ARMOR, Rarity.EPIC, 450, 6, 10, 0, 12, 8, "Hòa mình vào bóng tối, tăng tập trung và chuẩn xác.", "🥷"),
            Item("armor_celestial", "Thánh Giáp Quang Minh", ItemSlot.ARMOR, Rarity.LEGENDARY, 1200, 25, 20, 28, 15, 0, "Ánh hào quang chiếu rọi bảo bọc chiến binh bền bỉ.", "✨"),

            // GĂNG TAY
            Item("gloves_cloth", "Băng Quấn Cổ Tay", ItemSlot.GLOVES, Rarity.COMMON, 40, 0, 3, 0, 2, 0, "Bảo vệ khớp cổ tay khi chống đẩy trên sàn cứng.", "🥊"),
            Item("gloves_grip", "Găng Hít Đất Siêu Bám", ItemSlot.GLOVES, Rarity.RARE, 150, 0, 8, 0, 6, 0, "Đế cao su hạt kim cương chống trượt tay hoàn đối.", "🧤"),
            Item("gloves_titan", "Găng Titan Siêu Lực", ItemSlot.GLOVES, Rarity.EPIC, 400, 5, 18, 0, 8, 3, "Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.", "🦾"),
            Item("gloves_infinity", "Găng Tay Vô Cực", ItemSlot.GLOVES, Rarity.LEGENDARY, 1100, 22, 32, 0, 16, 14, "Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.", "🌌"),

            // GIÀY
            Item("boots_runner", "Giày Chạy Phản Lực", ItemSlot.BOOTS, Rarity.COMMON, 45, 0, 0, 3, 0, 2, "Êm ái, giảm chấn gối khi squat hoặc bật nhảy.", "👟"),
            Item("boots_iron", "Hộ Chân Chiến Binh", ItemSlot.BOOTS, Rarity.RARE, 150, 0, 6, 7, 0, 0, "Bọc thép mũi chân và ống quyển vững chãi.", "🥾"),
            Item("boots_winged", "Hài Phong Thần Hermes", ItemSlot.BOOTS, Rarity.EPIC, 380, 5, 0, 15, 6, 12, "Đôi giày có cánh lướt đi nhẹ tựa lông hồng.", "🪽"),
            Item("boots_abyss", "Bộ Bước Vực Thẳm", ItemSlot.BOOTS, Rarity.LEGENDARY, 950, 18, 18, 24, 0, 15, "Mỗi bước chân để lại uy chấn khiến kẻ thù khiếp đảm.", "⚡"),

            // BÙA CHÚ
            Item("amulet_stone", "Bùa Đá May Mắn", ItemSlot.AMULET, Rarity.COMMON, 50, 0, 0, 0, 0, 4, "Hòn đá cuội ven suối đem lại vận may khi tập.", "🪬"),
            Item("amulet_ruby", "Huyết Ngọc Hồi Phục", ItemSlot.AMULET, Rarity.RARE, 190, 0, 6, 6, 0, 5, "Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.", "🔮"),
            Item("amulet_eye", "Mắt Ưng Tinh Anh", ItemSlot.AMULET, Rarity.EPIC, 480, 7, 10, 0, 16, 10, "Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mm.", "👁️"),
            Item("amulet_sun", "Thái Dương Cổ Thạch", ItemSlot.AMULET, Rarity.LEGENDARY, 1300, 30, 20, 20, 20, 25, "Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.", "☀️"),

            // VŨ KHÍ
            Item("weapon_stick", "Côn Gỗ Luyện Tập", ItemSlot.WEAPON, Rarity.COMMON, 50, 0, 4, 0, 2, 0, "Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.", "🪵"),
            Item("weapon_sword", "Thanh Kiếm Thép Đúc", ItemSlot.WEAPON, Rarity.RARE, 180, 0, 10, 0, 6, 0, "Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.", "⚔️"),
            Item("weapon_axe", "Rìu Chiến Berserker", ItemSlot.WEAPON, Rarity.EPIC, 460, 6, 22, 8, 0, 0, "Chiếc rìu khổng lồ của chiến binh cuồng nộ.", "🪓"),
            Item("weapon_excalibur", "Thánh Kiếm Excalibur", ItemSlot.WEAPON, Rarity.LEGENDARY, 1400, 30, 35, 0, 18, 15, "Bảo kiếm huyền thoại cắm sâu trong đá.", "🗡️")
        )

        itemsList.forEach { item ->
            val cv = ContentValues().apply {
                put("id", item.id)
                put("name", item.name)
                put("slot", item.slot.name)
                put("rarity", item.rarity.name)
                put("price_gold", item.priceGold)
                put("price_gems", item.priceGems)
                put("bonus_str", item.bonusStr)
                put("bonus_end", item.bonusEnd)
                put("bonus_pre", item.bonusPre)
                put("bonus_luck", item.bonusLuck)
                put("description", item.description)
                put("icon", item.icon)
            }
            db.insertWithOnConflict("items", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // ====================== TRUY VẤN DỮ LIỆU SQL ======================

    fun getAllItems(): List<Item> {
        val list = mutableListOf<Item>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM items ORDER BY price_gold ASC", null)
        while (cursor.moveToNext()) {
            list.add(parseItem(cursor))
        }
        cursor.close()
        return list
    }

    fun getInventory(userId: Int = 1): List<InventoryItem> {
        val list = mutableListOf<InventoryItem>()
        val db = readableDatabase
        val query = """
            SELECT ui.id as inv_id, ui.is_equipped, ui.acquired_at, i.*
            FROM user_inventory ui
            JOIN items i ON ui.item_id = i.id
            WHERE ui.user_id = ?
            ORDER BY ui.is_equipped DESC, ui.id DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val invId = cursor.getInt(cursor.getColumnIndexOrThrow("inv_id"))
            val isEquipped = cursor.getInt(cursor.getColumnIndexOrThrow("is_equipped")) == 1
            val acquiredAt = cursor.getString(cursor.getColumnIndexOrThrow("acquired_at")) ?: ""
            val item = parseItem(cursor)
            list.add(InventoryItem(invId, item, isEquipped, acquiredAt))
        }
        cursor.close()
        return list
    }

    fun buyItem(userId: Int = 1, item: Item): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val userCursor = db.rawQuery("SELECT gold, gems FROM users WHERE id = ?", arrayOf(userId.toString()))
            if (!userCursor.moveToNext()) { userCursor.close(); return false }
            val currentGold = userCursor.getInt(0)
            val currentGems = userCursor.getInt(1)
            userCursor.close()

            if (currentGold < item.priceGold || currentGems < item.priceGems) {
                return false
            }

            db.execSQL("UPDATE users SET gold = gold - ?, gems = gems - ? WHERE id = ?",
                arrayOf(item.priceGold, item.priceGems, userId))

            val invCv = ContentValues().apply {
                put("user_id", userId)
                put("item_id", item.id)
                put("is_equipped", 0)
                put("acquired_at", LocalDate.now().toString())
            }
            db.insert("user_inventory", null, invCv)

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun equipItem(userId: Int = 1, invId: Int, slot: ItemSlot) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Tháo các món cùng slot ra trước
            db.execSQL("""
                UPDATE user_inventory 
                SET is_equipped = 0 
                WHERE user_id = ? AND item_id IN (SELECT id FROM items WHERE slot = ?)
            """.trimIndent(), arrayOf(userId.toString(), slot.name))

            // Trang bị món được chọn
            db.execSQL("UPDATE user_inventory SET is_equipped = 1 WHERE id = ?", arrayOf(invId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun unequipItem(invId: Int) {
        val db = writableDatabase
        db.execSQL("UPDATE user_inventory SET is_equipped = 0 WHERE id = ?", arrayOf(invId.toString()))
    }

    fun unequipAll(userId: Int = 1) {
        val db = writableDatabase
        db.execSQL("UPDATE user_inventory SET is_equipped = 0 WHERE user_id = ?", arrayOf(userId.toString()))
    }

    fun sellItem(userId: Int = 1, invId: Int, sellPrice: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM user_inventory WHERE id = ? AND user_id = ?", arrayOf(invId.toString(), userId.toString()))
            db.execSQL("UPDATE users SET gold = gold + ? WHERE id = ?", arrayOf(sellPrice.toString(), userId.toString()))
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            return false
        } finally {
            db.endTransaction()
        }
    }

    /**
     * TÍNH TOÁN RỚT ĐỒ (Loot Drop Rate System) và Hoàn thành bài tập
     */
    fun finishWorkoutAndRollDrop(
        userId: Int = 1,
        exercise: String,
        reps: Int,
        holdSec: Int,
        score: Int,
        isBoss: Boolean = false,
        isFreeTraining: Boolean = false,
        monsterStageId: Int? = null
    ): WorkoutReward {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Lấy thông tin người dùng
            val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(userId.toString()))
            cursor.moveToFirst()
            var gold = cursor.getInt(cursor.getColumnIndexOrThrow("gold"))
            var gems = cursor.getInt(cursor.getColumnIndexOrThrow("gems"))
            var xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"))
            var level = cursor.getInt(cursor.getColumnIndexOrThrow("level"))
            var streak = cursor.getInt(cursor.getColumnIndexOrThrow("streak"))
            var totalReps = cursor.getInt(cursor.getColumnIndexOrThrow("total_reps"))
            var stage = cursor.getInt(cursor.getColumnIndexOrThrow("stage"))
            val lastWorkout = cursor.getString(cursor.getColumnIndexOrThrow("last_workout_date"))
            cursor.close()

            // Tính chuỗi ngày tập luyện (Streak) & tổng số Reps tập thể lực
            val todayStr = LocalDate.now().toString()
            val yesterdayStr = LocalDate.now().minusDays(1).toString()
            if (lastWorkout != todayStr) {
                streak = if (lastWorkout == yesterdayStr) streak + 1 else 1
            }
            totalReps += score

            var droppedItem: Item? = null
            var xpEarned = 0
            var goldEarned = 0
            var gemsEarned = 0
            var isBossFirstClear = false

            if (isFreeTraining) {
                // CHẾ ĐỘ TẬP TỰ DO: Hoàn toàn không có phần thưởng, không rơi đồ, không nhảy ải
                xpEarned = 0
                goldEarned = 0
                gemsEarned = 0
                droppedItem = null
            } else {
                // CHẾ ĐỘ CHIẾN DỊCH / BOSS:
                // 1. Tính tổng Vận May (Base Luck + Luck từ trang bị đang đeo)
                val luckCursor = db.rawQuery("""
                    SELECT SUM(i.bonus_luck) FROM user_inventory ui
                    JOIN items i ON ui.item_id = i.id
                    WHERE ui.user_id = ? AND ui.is_equipped = 1
                """.trimIndent(), arrayOf(userId.toString()))
                val bonusLuck = if (luckCursor.moveToFirst()) luckCursor.getInt(0) else 0
                luckCursor.close()

                val totalLuck = 5 + (totalReps / 5) + bonusLuck

                // 2. TÍNH TỶ LỆ RỚT ĐỒ (Giảm tỉ lệ rớt đồ theo yêu cầu cân bằng game):
                // Quái thường: Base 12% + (Luck * 0.2%) (Tối đa 25%)
                // Boss: Base 35% + (Luck * 0.3%) (Tối đa 50%)
                val baseRate = if (isBoss) 0.35 else 0.12
                val luckBonus = if (isBoss) totalLuck * 0.003 else totalLuck * 0.002
                val maxChance = if (isBoss) 0.50 else 0.25
                val dropChance = min(maxChance, baseRate + luckBonus)
                val roll = Random.nextDouble()

                if (roll <= dropChance) {
                    // Xác định phẩm cấp (Rarity) - giảm tỉ lệ Legendary/Epic rơi tràn lan
                    val rarityRoll = Random.nextDouble()
                    val targetRarity = if (isBoss) {
                        when {
                            rarityRoll < min(0.08, 0.02 + (totalLuck * 0.001)) -> Rarity.LEGENDARY
                            rarityRoll < min(0.25, 0.12 + (totalLuck * 0.002)) -> Rarity.EPIC
                            rarityRoll < 0.60 -> Rarity.RARE
                            else -> Rarity.COMMON
                        }
                    } else {
                        when {
                            rarityRoll < min(0.02, 0.005 + (totalLuck * 0.0005)) -> Rarity.LEGENDARY
                            rarityRoll < min(0.10, 0.04 + (totalLuck * 0.001)) -> Rarity.EPIC
                            rarityRoll < 0.35 -> Rarity.RARE
                            else -> Rarity.COMMON
                        }
                    }

                    // Chọn ngẫu nhiên 1 item thuộc phẩm cấp đó
                    val candidateCursor = db.rawQuery(
                        "SELECT * FROM items WHERE rarity = ? ORDER BY RANDOM() LIMIT 1",
                        arrayOf(targetRarity.name)
                    )
                    if (candidateCursor.moveToFirst()) {
                        droppedItem = parseItem(candidateCursor)
                    } else {
                        val anyCursor = db.rawQuery("SELECT * FROM items ORDER BY RANDOM() LIMIT 1", null)
                        if (anyCursor.moveToFirst()) droppedItem = parseItem(anyCursor)
                        anyCursor.close()
                    }
                    candidateCursor.close()

                    // Lưu vào kho đồ người chơi
                    if (droppedItem != null) {
                        val invCv = ContentValues().apply {
                            put("user_id", userId)
                            put("item_id", droppedItem.id)
                            put("is_equipped", 0)
                            put("acquired_at", todayStr)
                        }
                        db.insert("user_inventory", null, invCv)
                    }
                }

                // 3. Tính XP và Vàng
                xpEarned = if (isBoss) score * 15 else score * 8
                goldEarned = if (isBoss) score * 4 else score * 2
                xp += xpEarned
                gold += goldEarned

                // 4. KIỂM TRA PHẦN THƯỞNG GEM CHO LẦN ĐẦU TIÊN ĐÁNH BẠI BOSS (CÂN BẰNG GEM)
                if (isBoss && monsterStageId != null) {
                    val clearCursor = db.rawQuery(
                        "SELECT id FROM boss_clears WHERE user_id = ? AND stage_id = ?",
                        arrayOf(userId.toString(), monsterStageId.toString())
                    )
                    val alreadyCleared = clearCursor.moveToFirst()
                    clearCursor.close()

                    if (!alreadyCleared) {
                        isBossFirstClear = true
                        // Thưởng Gem giá trị cho lần đầu hạ gục Boss (15 Gem cơ bản + thêm theo ải)
                        gemsEarned = 15 + (monsterStageId / 4) * 5
                        gems += gemsEarned

                        val bCv = ContentValues().apply {
                            put("user_id", userId)
                            put("stage_id", monsterStageId)
                            put("cleared_at", todayStr)
                        }
                        db.insert("boss_clears", null, bCv)
                    }
                }

                // Chỉ tăng ải khi đánh bại đúng ải quái vật hiện tại
                if (monsterStageId != null && monsterStageId == stage) {
                    stage += 1
                }

                var xpNeeded = 80 + level * 160
                while (xp >= xpNeeded) {
                    xp -= xpNeeded
                    level += 1
                    xpNeeded = 80 + level * 160
                }
            }

            // Lưu cập nhật vào users
            val userCv = ContentValues().apply {
                put("gold", gold)
                put("gems", gems)
                put("xp", xp)
                put("level", level)
                put("streak", streak)
                put("total_reps", totalReps)
                put("stage", stage)
                put("last_workout_date", todayStr)
            }
            db.update("users", userCv, "id = ?", arrayOf(userId.toString()))

            // Ghi nhật ký vào workouts
            val wCv = ContentValues().apply {
                put("user_id", userId)
                put("exercise", exercise)
                put("reps", reps)
                put("hold_seconds", holdSec)
                put("score", score)
                put("xp_earned", xpEarned)
                put("gold_earned", goldEarned)
                put("dropped_item_id", droppedItem?.id)
                put("created_at", todayStr)
            }
            db.insert("workouts", null, wCv)

            db.setTransactionSuccessful()

            return WorkoutReward(
                score = score,
                xpEarned = xpEarned,
                goldEarned = goldEarned,
                newLevel = level,
                newGold = gold,
                newXp = xp,
                newStreak = streak,
                droppedItem = droppedItem,
                gemsEarned = gemsEarned,
                newGems = gems,
                isBossFirstClear = isBossFirstClear
            )
        } finally {
            db.endTransaction()
        }
    }

    private fun parseItem(c: Cursor): Item {
        return Item(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            slot = ItemSlot.valueOf(c.getString(c.getColumnIndexOrThrow("slot"))),
            rarity = Rarity.valueOf(c.getString(c.getColumnIndexOrThrow("rarity"))),
            priceGold = c.getInt(c.getColumnIndexOrThrow("price_gold")),
            priceGems = c.getInt(c.getColumnIndexOrThrow("price_gems")),
            bonusStr = c.getInt(c.getColumnIndexOrThrow("bonus_str")),
            bonusEnd = c.getInt(c.getColumnIndexOrThrow("bonus_end")),
            bonusPre = c.getInt(c.getColumnIndexOrThrow("bonus_pre")),
            bonusLuck = c.getInt(c.getColumnIndexOrThrow("bonus_luck")),
            description = c.getString(c.getColumnIndexOrThrow("description")) ?: "",
            icon = c.getString(c.getColumnIndexOrThrow("icon")) ?: "⚔️"
        )
    }

    // =========================================================================
    // XÁC THỰC: ĐĂNG KÝ, ĐĂNG NHẬP & QUẢN LÝ TÀI KHOẢN
    // =========================================================================

    /**
     * Đăng ký tài khoản mới.
     * Kiểm tra trùng tên, nếu chưa có thì tạo mới và cấp trang bị tân thủ.
     */
    fun registerUser(username: String, password: String, avatar: String = "🧑‍🎤"): Pair<Boolean, String> {
        val trimmed = username.trim()
        if (trimmed.length < 3) return Pair(false, "Tên tài khoản phải có ít nhất 3 ký tự!")
        if (password.length < 4) return Pair(false, "Mật khẩu phải có ít nhất 4 ký tự!")

        val db = writableDatabase
        val checkCursor = db.rawQuery("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(trimmed))
        val exists = checkCursor.moveToFirst()
        checkCursor.close()

        if (exists) {
            return Pair(false, "Tên tài khoản '$trimmed' đã có người sử dụng!")
        }

        val cv = ContentValues().apply {
            put("username", trimmed)
            put("password", password)
            put("avatar", avatar)
            put("gold", 0)
            put("gems", 0)
            put("xp", 0)
            put("level", 1)
            put("streak", 1)
            put("total_reps", 0)
            put("stage", 1)
            put("last_workout_date", LocalDate.now().toString())
        }
        val newId = db.insert("users", null, cv)
        if (newId != -1L) {
            // Cấp trang bị khởi đầu
            val invCv = ContentValues().apply {
                put("user_id", newId.toInt())
                put("item_id", "helm_bronze")
                put("is_equipped", 1)
                put("acquired_at", LocalDate.now().toString())
            }
            db.insert("user_inventory", null, invCv)
            return Pair(true, "Đăng ký thành công! Chào mừng hiệp sĩ $trimmed!")
        } else {
            return Pair(false, "Lỗi cơ sở dữ liệu khi tạo tài khoản!")
        }
    }

    /**
     * Đăng nhập bằng tài khoản và mật khẩu.
     * Trả về Pair(userId, thông báo).
     */
    fun loginUser(username: String, password: String): Pair<Int?, String> {
        val trimmed = username.trim()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, password FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(trimmed))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return Pair(null, "Không tìm thấy tài khoản '$trimmed'!")
        }

        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        val storedPw = cursor.getString(cursor.getColumnIndexOrThrow("password"))
        cursor.close()

        return if (storedPw == password) {
            Pair(id, "Đăng nhập thành công! Chào mừng trở lại!")
        } else {
            Pair(null, "Mật khẩu không chính xác!")
        }
    }

    fun getUserProfile(userId: Int): UserProfile? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(userId.toString()))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        val p = parseUserProfile(cursor)
        cursor.close()
        return p
    }

    fun getUserProfileByUsername(username: String): UserProfile? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(username.trim()))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        val p = parseUserProfile(cursor)
        cursor.close()
        return p
    }

    private fun parseUserProfile(cursor: Cursor): UserProfile {
        val level = cursor.getInt(cursor.getColumnIndexOrThrow("level"))
        val title = when {
            level < 3 -> "Tân Binh"
            level < 6 -> "Võ Tăng"
            level < 10 -> "Chiến Binh"
            else -> "Huyền Thoại"
        }
        return UserProfile(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
            gold = cursor.getInt(cursor.getColumnIndexOrThrow("gold")),
            gems = cursor.getInt(cursor.getColumnIndexOrThrow("gems")),
            xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp")),
            level = level,
            streak = cursor.getInt(cursor.getColumnIndexOrThrow("streak")),
            totalReps = cursor.getInt(cursor.getColumnIndexOrThrow("total_reps")),
            stage = cursor.getInt(cursor.getColumnIndexOrThrow("stage")),
            avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar")) ?: "🧑‍🎤",
            title = title
        )
    }

    // =========================================================================
    // HỆ THỐNG BẠN BÈ (KẾT BẠN BẰNG TÊN)
    // =========================================================================

    /**
     * Kết bạn với người chơi khác dựa trên Username.
     */
    fun addFriendByUsername(currentUserId: Int, friendUsername: String): Pair<Boolean, String> {
        val targetName = friendUsername.trim()
        val db = writableDatabase

        // Tìm người dùng mục tiêu
        val targetCursor = db.rawQuery("SELECT id, username FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(targetName))
        if (!targetCursor.moveToFirst()) {
            targetCursor.close()
            return Pair(false, "Không tìm thấy người chơi có tên '$targetName'!")
        }
        val friendId = targetCursor.getInt(targetCursor.getColumnIndexOrThrow("id"))
        val canonicalName = targetCursor.getString(targetCursor.getColumnIndexOrThrow("username"))
        targetCursor.close()

        if (friendId == currentUserId) {
            return Pair(false, "Bạn không thể tự kết bạn với chính mình!")
        }

        // Kiểm tra đã là bạn bè chưa
        val checkCursor = db.rawQuery(
            "SELECT id FROM friends WHERE user_id = ? AND friend_id = ?",
            arrayOf(currentUserId.toString(), friendId.toString())
        )
        val alreadyFriends = checkCursor.moveToFirst()
        checkCursor.close()

        if (alreadyFriends) {
            return Pair(false, "Bạn và '$canonicalName' đã là bạn bè rồi!")
        }

        // Tạo quan hệ bạn bè 2 chiều
        val nowStr = LocalDate.now().toString()
        val f1 = ContentValues().apply {
            put("user_id", currentUserId)
            put("friend_id", friendId)
            put("created_at", nowStr)
        }
        val f2 = ContentValues().apply {
            put("user_id", friendId)
            put("friend_id", currentUserId)
            put("created_at", nowStr)
        }
        db.insertWithOnConflict("friends", null, f1, SQLiteDatabase.CONFLICT_REPLACE)
        db.insertWithOnConflict("friends", null, f2, SQLiteDatabase.CONFLICT_REPLACE)

        return Pair(true, "Đã kết bạn thành công với $canonicalName! 🤝")
    }

    /**
     * Lấy danh sách bạn bè của người dùng hiện tại kèm theo chỉ số.
     */
    fun getFriends(userId: Int): List<FriendProfile> {
        val list = mutableListOf<FriendProfile>()
        val db = readableDatabase
        val query = """
            SELECT u.id, u.username, u.level, u.total_reps, u.streak, u.avatar
            FROM friends f
            JOIN users u ON f.friend_id = u.id
            WHERE f.user_id = ?
            ORDER BY u.level DESC, u.total_reps DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val fId = cursor.getInt(0)
            val uname = cursor.getString(1)
            val lvl = cursor.getInt(2)
            val reps = cursor.getInt(3)
            val strk = cursor.getInt(4)
            val avt = cursor.getString(5) ?: "🧑‍🎤"

            // Tính Lực Chiến CP
            val cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30)
            val title = when {
                lvl < 3 -> "Tân Binh"
                lvl < 6 -> "Võ Tăng"
                lvl < 10 -> "Chiến Binh"
                else -> "Huyền Thoại"
            }

            list.add(FriendProfile(
                id = fId,
                username = uname,
                level = lvl,
                combatPower = cp,
                totalReps = reps,
                streak = strk,
                avatar = avt,
                title = title,
                isOnline = true
            ))
        }
        cursor.close()
        return list
    }

    fun removeFriend(userId: Int, friendId: Int): Boolean {
        val db = writableDatabase
        db.delete("friends", "(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
            arrayOf(userId.toString(), friendId.toString(), friendId.toString(), userId.toString()))
        return true
    }

    // =========================================================================
    // BẢNG XẾP HẠNG THẾ GIỚI (WORLD LEADERBOARD THEO CÁC MỤC SỐ LIỆU)
    // =========================================================================

    /**
     * Lấy bảng xếp hạng người chơi toàn cầu theo tiêu chí:
     * - "CP": Lực chiến
     * - "LEVEL": Cấp độ
     * - "REPS": Tổng số Reps thể lực
     * - "STREAK": Chuỗi ngày kiên trì
     */
    fun getLeaderboard(currentUserId: Int, sortBy: String = "CP"): List<LeaderboardEntry> {
        val db = readableDatabase
        val friendIds = mutableSetOf<Int>()
        val fCursor = db.rawQuery("SELECT friend_id FROM friends WHERE user_id = ?", arrayOf(currentUserId.toString()))
        while (fCursor.moveToNext()) {
            friendIds.add(fCursor.getInt(0))
        }
        fCursor.close()

        val allUsers = mutableListOf<LeaderboardEntry>()
        val cursor = db.rawQuery("SELECT id, username, level, xp, total_reps, streak, avatar FROM users", null)
        while (cursor.moveToNext()) {
            val uId = cursor.getInt(0)
            val uname = cursor.getString(1)
            val lvl = cursor.getInt(2)
            val reps = cursor.getInt(4)
            val strk = cursor.getInt(5)
            val avt = cursor.getString(6) ?: "🧑‍🎤"

            val cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30)
            val title = when {
                lvl < 3 -> "Tân Binh"
                lvl < 6 -> "Võ Tăng"
                lvl < 10 -> "Chiến Binh"
                else -> "Huyền Thoại"
            }

            allUsers.add(
                LeaderboardEntry(
                    rank = 0,
                    id = uId,
                    username = uname,
                    level = lvl,
                    combatPower = cp,
                    totalReps = reps,
                    streak = strk,
                    avatar = avt,
                    title = title,
                    isFriend = friendIds.contains(uId),
                    isCurrentUser = (uId == currentUserId)
                )
            )
        }
        cursor.close()

        // Sắp xếp theo tiêu chí được chọn
        val sorted = when (sortBy.uppercase()) {
            "LEVEL" -> allUsers.sortedWith(compareByDescending<LeaderboardEntry> { it.level }.thenByDescending { it.combatPower })
            "REPS" -> allUsers.sortedWith(compareByDescending<LeaderboardEntry> { it.totalReps }.thenByDescending { it.combatPower })
            "STREAK" -> allUsers.sortedWith(compareByDescending<LeaderboardEntry> { it.streak }.thenByDescending { it.combatPower })
            else -> allUsers.sortedWith(compareByDescending<LeaderboardEntry> { it.combatPower }.thenByDescending { it.level })
        }

        return sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    // =========================================================================
    // HỆ THỐNG BANG HỘI (GUILD)
    // =========================================================================

    private fun seedSampleGuilds(db: SQLiteDatabase) {
        val sampleGuilds = listOf(
            Triple("Taiwan Fitness 🇹🇼", "🇹🇼", "Pushing for improvement every single rep!"),
            Triple("Chiến Binh Rồng 🐉", "🐉", "Ý chí rèn luyện tựa long thần!"),
            Triple("Titan Thép 🗿", "🗿", "Cơ bắp vững chắc như bàn thạch.")
        )
        val leaders = listOf(2, 5, 4) // ShadowBlade, DragonFit, IronTitan
        sampleGuilds.forEachIndexed { i, (name, badge, slogan) ->
            val gCv = ContentValues().apply {
                put("id", i + 1)
                put("name", name)
                put("badge", badge)
                put("slogan", slogan)
                put("leader_id", leaders[i])
                put("level", 2 + i)
                put("created_at", LocalDate.now().toString())
            }
            db.insertWithOnConflict("guilds", null, gCv, SQLiteDatabase.CONFLICT_IGNORE)

            val mCv = ContentValues().apply {
                put("guild_id", i + 1)
                put("user_id", leaders[i])
                put("role", "LEADER")
                put("joined_at", LocalDate.now().toString())
            }
            db.insertWithOnConflict("guild_members", null, mCv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun createGuild(userId: Int, name: String, badge: String, slogan: String): Pair<Boolean, String> {
        val trimmed = name.trim()
        if (trimmed.length < 3) return Pair(false, "Tên bang hội phải có ít nhất 3 ký tự!")
        if (slogan.isBlank()) return Pair(false, "Vui lòng nhập khẩu hiệu cho bang hội!")

        val db = writableDatabase
        // Kiểm tra người chơi đã có bang hội chưa
        val checkMember = db.rawQuery("SELECT id FROM guild_members WHERE user_id = ?", arrayOf(userId.toString()))
        val alreadyInGuild = checkMember.moveToFirst()
        checkMember.close()
        if (alreadyInGuild) return Pair(false, "Bạn đã tham gia một bang hội khác rồi! Hãy rời bang trước khi lập bang mới.")

        // Kiểm tra tên bang hội đã tồn tại chưa
        val checkName = db.rawQuery("SELECT id FROM guilds WHERE LOWER(name) = LOWER(?)", arrayOf(trimmed))
        val nameExists = checkName.moveToFirst()
        checkName.close()
        if (nameExists) return Pair(false, "Tên bang hội '$trimmed' đã có người sử dụng!")

        val gCv = ContentValues().apply {
            put("name", trimmed)
            put("badge", badge)
            put("slogan", slogan.trim())
            put("leader_id", userId)
            put("level", 1)
            put("created_at", LocalDate.now().toString())
        }
        val guildId = db.insert("guilds", null, gCv)
        if (guildId != -1L) {
            val mCv = ContentValues().apply {
                put("guild_id", guildId.toInt())
                put("user_id", userId)
                put("role", "LEADER")
                put("joined_at", LocalDate.now().toString())
            }
            db.insert("guild_members", null, mCv)
            return Pair(true, "👑 Thành lập bang hội '$trimmed' thành công!")
        }
        return Pair(false, "Có lỗi xảy ra khi tạo bang hội!")
    }

    fun joinGuild(userId: Int, guildId: Int): Pair<Boolean, String> {
        val db = writableDatabase
        val checkMember = db.rawQuery("SELECT id FROM guild_members WHERE user_id = ?", arrayOf(userId.toString()))
        val alreadyInGuild = checkMember.moveToFirst()
        checkMember.close()
        if (alreadyInGuild) return Pair(false, "Bạn đang là thành viên của bang hội khác!")

        val gCursor = db.rawQuery("SELECT name FROM guilds WHERE id = ?", arrayOf(guildId.toString()))
        if (!gCursor.moveToFirst()) {
            gCursor.close()
            return Pair(false, "Bang hội không tồn tại!")
        }
        val guildName = gCursor.getString(0)
        gCursor.close()

        val mCv = ContentValues().apply {
            put("guild_id", guildId)
            put("user_id", userId)
            put("role", "MEMBER")
            put("joined_at", LocalDate.now().toString())
        }
        val res = db.insert("guild_members", null, mCv)
        return if (res != -1L) {
            Pair(true, "⚔️ Chào mừng bạn gia nhập bang hội $guildName!")
        } else {
            Pair(false, "Không thể gia nhập bang hội!")
        }
    }

    fun leaveGuild(userId: Int): Pair<Boolean, String> {
        val db = writableDatabase
        val mCursor = db.rawQuery("SELECT guild_id, role FROM guild_members WHERE user_id = ?", arrayOf(userId.toString()))
        if (!mCursor.moveToFirst()) {
            mCursor.close()
            return Pair(false, "Bạn chưa gia nhập bang hội nào!")
        }
        val guildId = mCursor.getInt(0)
        val role = mCursor.getString(1)
        mCursor.close()

        if (role == "LEADER") {
            // Chủ bang giải tán bang hội
            db.delete("guild_members", "guild_id = ?", arrayOf(guildId.toString()))
            db.delete("guilds", "id = ?", arrayOf(guildId.toString()))
            return Pair(true, "Đã giải tán bang hội của bạn!")
        } else {
            db.delete("guild_members", "user_id = ?", arrayOf(userId.toString()))
            return Pair(true, "Đã rời khỏi bang hội!")
        }
    }

    fun getUserGuild(userId: Int): Guild? {
        val db = readableDatabase
        val query = """
            SELECT g.id, g.name, g.badge, g.slogan, g.leader_id, u.username, g.level, gm.role
            FROM guild_members gm
            JOIN guilds g ON gm.guild_id = g.id
            JOIN users u ON g.leader_id = u.id
            WHERE gm.user_id = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        val gId = cursor.getInt(0)
        val gName = cursor.getString(1)
        val badge = cursor.getString(2)
        val slogan = cursor.getString(3)
        val leaderId = cursor.getInt(4)
        val leaderName = cursor.getString(5)
        val level = cursor.getInt(6)
        val role = cursor.getString(7)
        cursor.close()

        val cCount = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", arrayOf(gId.toString()))
        val count = if (cCount.moveToFirst()) cCount.getInt(0) else 1
        cCount.close()

        val cReps = db.rawQuery("""
            SELECT SUM(u.total_reps) 
            FROM guild_members gm 
            JOIN users u ON gm.user_id = u.id 
            WHERE gm.guild_id = ?
        """.trimIndent(), arrayOf(gId.toString()))
        val reps = if (cReps.moveToFirst()) cReps.getInt(0) else 0
        cReps.close()

        return Guild(
            id = gId,
            name = gName,
            badge = badge,
            slogan = slogan,
            leaderId = leaderId,
            leaderName = leaderName,
            level = level,
            memberCount = count,
            totalReps = reps,
            isUserMember = true,
            isUserLeader = (role == "LEADER")
        )
    }

    fun getAllGuilds(userId: Int): List<Guild> {
        val db = readableDatabase
        val list = mutableListOf<Guild>()
        val userGuild = getUserGuild(userId)

        val cursor = db.rawQuery("""
            SELECT g.id, g.name, g.badge, g.slogan, g.leader_id, u.username, g.level
            FROM guilds g
            JOIN users u ON g.leader_id = u.id
            ORDER BY g.level DESC, g.id ASC
        """.trimIndent(), null)

        while (cursor.moveToNext()) {
            val gId = cursor.getInt(0)
            val gName = cursor.getString(1)
            val badge = cursor.getString(2)
            val slogan = cursor.getString(3)
            val leaderId = cursor.getInt(4)
            val leaderName = cursor.getString(5)
            val level = cursor.getInt(6)

            val cCount = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", arrayOf(gId.toString()))
            val count = if (cCount.moveToFirst()) cCount.getInt(0) else 1
            cCount.close()

            val cReps = db.rawQuery("""
                SELECT SUM(u.total_reps) 
                FROM guild_members gm 
                JOIN users u ON gm.user_id = u.id 
                WHERE gm.guild_id = ?
            """.trimIndent(), arrayOf(gId.toString()))
            val reps = if (cReps.moveToFirst()) cReps.getInt(0) else 0
            cReps.close()

            list.add(
                Guild(
                    id = gId,
                    name = gName,
                    badge = badge,
                    slogan = slogan,
                    leaderId = leaderId,
                    leaderName = leaderName,
                    level = level,
                    memberCount = count,
                    totalReps = reps,
                    isUserMember = (userGuild?.id == gId),
                    isUserLeader = (userGuild?.id == gId && userGuild.isUserLeader)
                )
            )
        }
        cursor.close()
        return list
    }

    fun getGuildMembers(guildId: Int): List<GuildMember> {
        val db = readableDatabase
        val list = mutableListOf<GuildMember>()
        val cursor = db.rawQuery("""
            SELECT u.id, u.username, u.avatar, u.level, gm.role, u.total_reps
            FROM guild_members gm
            JOIN users u ON gm.user_id = u.id
            WHERE gm.guild_id = ?
            ORDER BY CASE WHEN gm.role = 'LEADER' THEN 1 ELSE 2 END, u.total_reps DESC
        """.trimIndent(), arrayOf(guildId.toString()))

        while (cursor.moveToNext()) {
            list.add(
                GuildMember(
                    userId = cursor.getInt(0),
                    username = cursor.getString(1),
                    avatar = cursor.getString(2) ?: "🧑‍🎤",
                    level = cursor.getInt(3),
                    role = cursor.getString(4),
                    totalReps = cursor.getInt(5)
                )
            )
        }
        cursor.close()
        return list
    }

    // =========================================================================
    // HỆ THỐNG ĐIỂM DANH ĐĂNG NHẬP HÀNG TUẦN (WEEKLY LOGIN CHECK-IN)
    // =========================================================================

    private fun getCurrentWeekKey(): String {
        val now = LocalDate.now()
        val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.toString()
    }

    fun getWeeklyCheckInRewards(userId: Int): Pair<List<DailyCheckInReward>, Boolean> {
        val db = writableDatabase
        val todayStr = LocalDate.now().toString()
        val currentWeekKey = getCurrentWeekKey()

        val cursor = db.rawQuery("SELECT checkin_day, last_date, week_key FROM weekly_checkins WHERE user_id = ?", arrayOf(userId.toString()))
        var checkinDay = 0
        var lastDate: String? = null
        var weekKey: String? = null

        if (cursor.moveToFirst()) {
            checkinDay = cursor.getInt(0)
            lastDate = cursor.getString(1)
            weekKey = cursor.getString(2)
        }
        cursor.close()

        // Nếu sang tuần mới, reset lại số ngày điểm danh
        if (weekKey != currentWeekKey) {
            checkinDay = 0
            lastDate = null
            val cv = ContentValues().apply {
                put("user_id", userId)
                put("checkin_day", 0)
                put("last_date", null as String?)
                put("week_key", currentWeekKey)
            }
            db.insertWithOnConflict("weekly_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        val canClaimToday = (lastDate != todayStr && checkinDay < 7)
        val rewardsConfig = listOf(
            Pair(50, 0),   // Ngày 1: 50 Vàng
            Pair(0, 2),    // Ngày 2: 2 Gem
            Pair(100, 0),  // Ngày 3: 100 Vàng
            Pair(0, 5),    // Ngày 4: 5 Gem
            Pair(200, 0),  // Ngày 5: 200 Vàng
            Pair(0, 10),   // Ngày 6: 10 Gem
            Pair(500, 20)  // Ngày 7: 500 Vàng + 20 Gem
        )

        val list = (1..7).map { day ->
            val (gold, gems) = rewardsConfig[day - 1]
            val isClaimed = day <= checkinDay
            val isAvailableToday = canClaimToday && (day == checkinDay + 1)
            val isUpcoming = day > checkinDay + (if (canClaimToday) 1 else 0)

            DailyCheckInReward(
                day = day,
                gold = gold,
                gems = gems,
                isClaimed = isClaimed,
                isAvailableToday = isAvailableToday,
                isUpcoming = isUpcoming
            )
        }

        return Pair(list, canClaimToday)
    }

    fun claimWeeklyReward(userId: Int): Pair<Boolean, String> {
        val (rewards, canClaimToday) = getWeeklyCheckInRewards(userId)
        if (!canClaimToday) {
            return Pair(false, "Bạn đã điểm danh nhận quà hôm nay rồi! Hãy quay lại vào ngày mai nhé.")
        }
        val targetReward = rewards.firstOrNull { it.isAvailableToday } ?: return Pair(false, "Không có phần thưởng khả dụng.")

        val db = writableDatabase
        val todayStr = LocalDate.now().toString()
        val currentWeekKey = getCurrentWeekKey()

        // Cập nhật bảng weekly_checkins
        val cv = ContentValues().apply {
            put("user_id", userId)
            put("checkin_day", targetReward.day)
            put("last_date", todayStr)
            put("week_key", currentWeekKey)
        }
        db.insertWithOnConflict("weekly_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

        // Cộng vàng và gem cho người chơi
        db.execSQL(
            "UPDATE users SET gold = gold + ?, gems = gems + ? WHERE id = ?",
            arrayOf(targetReward.gold, targetReward.gems, userId)
        )

        val msg = buildString {
            append("🎉 Điểm danh Ngày ${targetReward.day} thành công! ")
            if (targetReward.gold > 0) append("+${targetReward.gold} 🪙 ")
            if (targetReward.gems > 0) append("+${targetReward.gems} ◆ ")
        }
        return Pair(true, msg)
    }

    // =========================================================================
    // HỆ THỐNG NHIỆM VỤ NGÀY, TUẦN, THÁNG (QUESTS SYSTEM)
    // =========================================================================

    fun getQuests(
        userId: Int,
        todayReps: Int,
        weekReps: Int,
        totalReps: Int,
        streak: Int,
        stage: Int,
        level: Int
    ): List<QuestItem> {
        val db = readableDatabase
        val todayStr = LocalDate.now().toString()
        val currentWeekKey = getCurrentWeekKey()
        val currentMonthKey = todayStr.substring(0, 7) // "YYYY-MM"

        // Đọc danh sách nhiệm vụ đã nhận trong kỳ
        val claimedIds = mutableSetOf<String>()
        val c = db.rawQuery("""
            SELECT quest_id FROM quest_claims 
            WHERE user_id = ? AND period_key IN (?, ?, ?)
        """.trimIndent(), arrayOf(userId.toString(), todayStr, currentWeekKey, currentMonthKey))
        while (c.moveToNext()) {
            claimedIds.add(c.getString(0))
        }
        c.close()

        // Số boss đã hạ gục
        val bc = db.rawQuery("SELECT COUNT(*) FROM boss_clears WHERE user_id = ?", arrayOf(userId.toString()))
        val bossClearedCount = if (bc.moveToFirst()) bc.getInt(0) else 0
        bc.close()

        val list = mutableListOf<QuestItem>()

        // 1. NHIỆM VỤ NGÀY (DAILY)
        list.add(
            QuestItem(
                id = "daily_1",
                name = "Chiến Binh Chăm Chỉ",
                quote = "“Ba mươi rep khởi động mỗi ngày giúp máu huyết lưu thông.”",
                current = todayReps,
                goal = 30,
                xp = 60,
                gold = 30,
                gems = 0,
                isClaimed = claimedIds.contains("daily_1"),
                category = "DAILY"
            )
        )
        list.add(
            QuestItem(
                id = "daily_2",
                name = "Khởi Động Năng Lượng",
                quote = "“Bắt đầu buổi tập đầu tiên trong ngày.”",
                current = if (todayReps > 0) 1 else 0,
                goal = 1,
                xp = 40,
                gold = 20,
                gems = 0,
                isClaimed = claimedIds.contains("daily_2"),
                category = "DAILY"
            )
        )
        list.add(
            QuestItem(
                id = "daily_3",
                name = "Bất Khả Chiến Bại",
                quote = "“Sáu mươi rep trong một ngày — chứng minh sự bền bỉ phi thường.”",
                current = todayReps,
                goal = 60,
                xp = 120,
                gold = 60,
                gems = 1,
                isClaimed = claimedIds.contains("daily_3"),
                category = "DAILY"
            )
        )

        // 2. NHIỆM VỤ TUẦN (WEEKLY)
        list.add(
            QuestItem(
                id = "weekly_1",
                name = "Bền Bỉ Trường Kỳ",
                quote = "“Hai trăm rep trong cả tuần — không chùn bước trước thử thách.”",
                current = weekReps,
                goal = 200,
                xp = 350,
                gold = 150,
                gems = 3,
                isClaimed = claimedIds.contains("weekly_1"),
                category = "WEEKLY"
            )
        )
        list.add(
            QuestItem(
                id = "weekly_2",
                name = "Chuỗi Ngày Thép",
                quote = "“Duy trì chuỗi tập 3 ngày liên tục trong tuần.”",
                current = streak.coerceAtMost(3),
                goal = 3,
                xp = 250,
                gold = 100,
                gems = 2,
                isClaimed = claimedIds.contains("weekly_2"),
                category = "WEEKLY"
            )
        )
        list.add(
            QuestItem(
                id = "weekly_3",
                name = "Đồ Tể Quái Vật",
                quote = "“Vượt qua 3 ải chiến dịch trong tuần này.”",
                current = stage.coerceAtMost(3),
                goal = 3,
                xp = 400,
                gold = 200,
                gems = 5,
                isClaimed = claimedIds.contains("weekly_3"),
                category = "WEEKLY"
            )
        )

        // 3. NHIỆM VỤ THÁNG (MONTHLY)
        list.add(
            QuestItem(
                id = "monthly_1",
                name = "Huyền Thoại Thể Lực",
                quote = "“Một nghìn rep tích lũy — cột mốc của những nhà vô địch.”",
                current = totalReps.coerceAtMost(1000),
                goal = 1000,
                xp = 1500,
                gold = 600,
                gems = 15,
                isClaimed = claimedIds.contains("monthly_1"),
                category = "MONTHLY"
            )
        )
        list.add(
            QuestItem(
                id = "monthly_2",
                name = "Chinh Phục Đỉnh Cao",
                quote = "“Đạt Cấp độ 5 trở lên để mở khóa tiềm năng vô hạn.”",
                current = level.coerceAtMost(5),
                goal = 5,
                xp = 1000,
                gold = 500,
                gems = 10,
                isClaimed = claimedIds.contains("monthly_2"),
                category = "MONTHLY"
            )
        )
        list.add(
            QuestItem(
                id = "monthly_3",
                name = "Sát Thủ Trùm",
                quote = "“Đánh bại ít nhất 1 Boss hùng mạnh trong tháng.”",
                current = bossClearedCount.coerceAtMost(1),
                goal = 1,
                xp = 2000,
                gold = 800,
                gems = 20,
                isClaimed = claimedIds.contains("monthly_3"),
                category = "MONTHLY"
            )
        )

        return list
    }

    fun claimQuestReward(userId: Int, quest: QuestItem): Pair<Boolean, String> {
        val todayStr = LocalDate.now().toString()
        val periodKey = when (quest.category) {
            "DAILY" -> todayStr
            "WEEKLY" -> getCurrentWeekKey()
            else -> todayStr.substring(0, 7)
        }

        val db = writableDatabase
        val check = db.rawQuery(
            "SELECT id FROM quest_claims WHERE user_id = ? AND quest_id = ? AND period_key = ?",
            arrayOf(userId.toString(), quest.id, periodKey)
        )
        val already = check.moveToFirst()
        check.close()
        if (already) return Pair(false, "Bạn đã nhận phần thưởng nhiệm vụ này rồi!")

        val cv = ContentValues().apply {
            put("user_id", userId)
            put("quest_id", quest.id)
            put("period_key", periodKey)
            put("claimed_at", todayStr)
        }
        val ins = db.insert("quest_claims", null, cv)
        if (ins != -1L) {
            // Cộng thưởng XP, Vàng, Gems và thăng cấp nếu đủ
            val uCursor = db.rawQuery("SELECT xp, level, gold, gems FROM users WHERE id = ?", arrayOf(userId.toString()))
            if (uCursor.moveToFirst()) {
                var xp = uCursor.getInt(0) + quest.xp
                var lvl = uCursor.getInt(1)
                val gold = uCursor.getInt(2) + quest.gold
                val gems = uCursor.getInt(3) + quest.gems

                var xpNeeded = 80 + lvl * 160
                while (xp >= xpNeeded) {
                    xp -= xpNeeded
                    lvl += 1
                    xpNeeded = 80 + lvl * 160
                }

                val uCv = ContentValues().apply {
                    put("xp", xp)
                    put("level", lvl)
                    put("gold", gold)
                    put("gems", gems)
                }
                db.update("users", uCv, "id = ?", arrayOf(userId.toString()))
            }
            uCursor.close()

            val msg = buildString {
                append("🎁 Đã nhận: +${quest.xp} XP")
                if (quest.gold > 0) append(", +${quest.gold} 🪙")
                if (quest.gems > 0) append(", +${quest.gems} ◆")
            }
            return Pair(true, msg)
        }
        return Pair(false, "Không thể nhận thưởng nhiệm vụ!")
    }
}

