package com.example.vigil.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.Rarity
import com.example.vigil.model.WorkoutReward
import java.time.LocalDate
import kotlin.math.min
import kotlin.random.Random

/**
 * Cơ sở dữ liệu SQL cục bộ (SQLite) tương thích 100% với cấu trúc bảng MySQL của Vigil.
 * Chạy trực tiếp trên thiết bị Android, lưu trữ bền vững kể cả khi offline.
 */
class VigilSqlDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vigil_app.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: VigilSqlDb? = null

        fun get(context: Context): VigilSqlDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VigilSqlDb(context.applicationContext).also { INSTANCE = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Bảng users
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                gold INTEGER NOT NULL DEFAULT 150,
                gems INTEGER NOT NULL DEFAULT 10,
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

        // Nạp dữ liệu mặc định ban đầu
        seedItems(db)
        seedInitialUser(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS workouts")
        db.execSQL("DROP TABLE IF EXISTS user_inventory")
        db.execSQL("DROP TABLE IF EXISTS items")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    private fun seedInitialUser(db: SQLiteDatabase) {
        val cv = ContentValues().apply {
            put("id", 1)
            put("username", "Hachimi")
            put("gold", 250)
            put("gems", 10)
            put("xp", 0)
            put("level", 1)
            put("streak", 1)
            put("total_reps", 0)
            put("stage", 1)
            put("last_workout_date", LocalDate.now().toString())
        }
        db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_IGNORE)

        // Tặng tân binh 1 mũ đồng và 1 thanh kiếm thép cơ bản
        val invCv = ContentValues().apply {
            put("user_id", 1)
            put("item_id", "helm_bronze")
            put("is_equipped", 1)
            put("acquired_at", LocalDate.now().toString())
        }
        db.insert("user_inventory", null, invCv)
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

            if (isFreeTraining) {
                // CHẾ ĐỘ TẬP TỰ DO: Hoàn toàn không có phần thưởng, không rơi đồ, không nhảy ải
                xpEarned = 0
                goldEarned = 0
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
                droppedItem = droppedItem
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
}
