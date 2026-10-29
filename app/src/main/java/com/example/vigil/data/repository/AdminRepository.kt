package com.example.vigil.data.repository

import com.example.vigil.data.VigilSqlDb
import com.example.vigil.model.AdminUserEntry
import com.example.vigil.model.AdventureData
import com.example.vigil.model.CheatAlertItem
import com.example.vigil.model.ExerciseConfigItem
import com.example.vigil.model.Item
import com.example.vigil.model.Monster
import com.example.vigil.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Repository phụ trách toàn bộ truy vấn dữ liệu (SQLite CSDL) và kết nối mạng (API Backend)
 * cho chức năng Quản Trị Viên (Admin Panel).
 * Tách biệt hoàn toàn tầng Dữ liệu / Backend ra khỏi Giao diện UI (Jetpack Compose).
 */
class AdminRepository(private val db: VigilSqlDb) {

    private val bgScope = CoroutineScope(Dispatchers.IO)

    /**
     * Lấy danh sách tài khoản người dùng thực tế được tạo trong cơ sở dữ liệu SQLite.
     * Quy tắc phân quyền:
     * - Chỉ duy nhất nick "Hachimi" mặc định là "Admin" ban đầu.
     * - Tất cả các tài khoản còn lại do người dùng tạo trong CSDL mặc định là "User" (hoặc role được Admin cấp).
     */
    fun getUsersFromDb(): List<AdminUserEntry> {
        val list = mutableListOf<AdminUserEntry>()
        val readable = db.readableDatabase
        val cursor = readable.rawQuery(
            "SELECT id, username, avatar, level, gold, streak, total_reps, role, status, lock_reason FROM users ORDER BY id ASC",
            null
        )
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val uName = cursor.getString(1) ?: continue
            val uAvatar = cursor.getString(2) ?: "🧑‍🎤"
            val uLevel = cursor.getInt(3)
            val uGold = cursor.getInt(4)
            val uStreak = cursor.getInt(5)
            val uReps = cursor.getInt(6)
            val rawRole = cursor.getString(7)
            // Chỉ Hachimi là Admin ban đầu, các acc khác lấy đúng role lưu trong CSDL (mặc định User)
            val uRole = if (uName.equals("Hachimi", ignoreCase = true)) {
                "Admin"
            } else {
                rawRole ?: "User"
            }
            val uStatus = cursor.getInt(8)
            val uReason = cursor.getString(9)
            list.add(AdminUserEntry(id, uName, uAvatar, uLevel, uGold, uStreak, uReps, uRole, uStatus == 0, uReason))
        }
        cursor.close()
        return list
    }

    /**
     * Cập nhật vai trò (Role: Admin / User / Moderator) của một người chơi theo ID và Username.
     * Tự động đồng bộ CSDL SQLite và Backend API.
     */
    fun updateUserRole(userId: Int, username: String, newRole: String) {
        db.updateUserRole(userId, newRole)
        bgScope.launch {
            try {
                ApiClient.updateUserRole(username, newRole)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Cập nhật vai trò của người chơi theo tên Username (hoặc đăng ký mới vào CSDL nếu chưa có).
     */
    fun updateUserRoleByUsername(username: String, newRole: String): Boolean {
        val trimmed = username.trim()
        if (trimmed.length < 3) return false

        val readable = db.readableDatabase
        val cursor = readable.rawQuery("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(trimmed))
        var foundId = -1
        if (cursor.moveToFirst()) {
            foundId = cursor.getInt(0)
        }
        cursor.close()

        if (foundId != -1) {
            updateUserRole(foundId, trimmed, newRole)
            return true
        } else {
            val res = db.registerUser(trimmed, "123456", "🧑‍🎤")
            if (res.first) {
                val c2 = db.readableDatabase.rawQuery("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", arrayOf(trimmed))
                if (c2.moveToFirst()) {
                    val newId = c2.getInt(0)
                    updateUserRole(newId, trimmed, newRole)
                }
                c2.close()
                return true
            }
        }
        return false
    }

    /**
     * Khóa hoặc mở khóa tài khoản người chơi.
     */
    fun setUserLock(userId: Int, username: String, lock: Boolean, reason: String?) {
        db.updateUserStatus(userId, if (lock) 0 else 1, reason)
        bgScope.launch {
            try {
                if (lock) {
                    ApiClient.banUser(username, reason ?: "Khóa bởi Admin")
                } else {
                    ApiClient.unbanUser(username)
                }
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Lấy danh sách quái vật & Boss của tất cả các chương.
     */
    fun getMonsters(): List<Monster> {
        return AdventureData.CHAPTERS.flatMap { it.monsters }.map { m ->
            Monster(m.stageId, m.name, m.title, m.avatar, m.maxHp, m.exercise, m.isBoss, m.expReward, m.goldReward, m.lore, m.dropRarity, m.colorHex)
        }
    }

    /**
     * Lưu thông số quái vật và đồng bộ tới Backend.
     */
    fun saveMonster(monster: Monster) {
        bgScope.launch {
            try {
                ApiClient.saveMonster(
                    monster.stageId,
                    monster.name,
                    monster.stageId,
                    monster.maxHp,
                    monster.exercise.name,
                    monster.expReward,
                    monster.goldReward,
                    monster.isBoss
                )
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Lấy danh sách cấu hình góc chuẩn bài tập AI MediaPipe.
     */
    fun getExerciseConfigs(): List<ExerciseConfigItem> {
        return listOf(
            ExerciseConfigItem("PUSHUP", "Hít đất (Push-up)", 165f, 90f, 1.2f, 0.8f),
            ExerciseConfigItem("SQUAT", "Squat", 170f, 90f, 1.0f, 1.0f),
            ExerciseConfigItem("SITUP", "Gập bụng (Sit-up)", 160f, 75f, 1.1f, 0.6f),
            ExerciseConfigItem("PLANK", "Plank", 180f, 170f, 1.5f, 0.1f),
            ExerciseConfigItem("PULLUP", "Hít xà (Pull-up)", 160f, 70f, 2.0f, 1.5f),
            ExerciseConfigItem("DIP", "Xà kép (Dip)", 160f, 90f, 1.4f, 0.9f)
        )
    }

    /**
     * Lưu cấu hình góc AI và đồng bộ tới Backend.
     */
    fun saveExerciseConfig(ex: ExerciseConfigItem) {
        bgScope.launch {
            try {
                ApiClient.saveExercise(ex.code, ex.name, ex.extendAngle, ex.contractAngle, ex.damageFactor, ex.caloPerRep)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Lấy danh sách cảnh báo gian lận thời gian thực.
     */
    fun getCheatAlerts(): List<CheatAlertItem> {
        return listOf(
            CheatAlertItem(101, "SpeedyHacker99", "PUSHUP", 150, 12, "Tốc độ 80ms/rep (Bất thường < 200ms)"),
            CheatAlertItem(102, "BotFarmMaster", "SQUAT", 280, 25, "Spam dữ liệu cảm biến liên tục")
        )
    }

    /**
     * Xử lý cảnh báo gian lận: Khóa tài khoản và thông báo Backend.
     */
    fun resolveCheatAlert(username: String, reason: String) {
        bgScope.launch {
            try {
                ApiClient.banUser(username, reason)
            } catch (ignored: Exception) {}
        }
    }

    // =========================================================================
    // QUẢN LÝ VẬT PHẨM BÁN TRONG CỬA HÀNG (SHOP ITEMS CRUD)
    // =========================================================================

    fun getShopItems(): List<Item> {
        return db.allItems
    }

    fun saveShopItem(item: Item): Boolean {
        val ok = db.saveShopItem(item)
        if (ok) {
            bgScope.launch {
                try {
                    ApiClient.saveAdminItem(item)
                } catch (ignored: Exception) {}
            }
        }
        return ok
    }

    fun deleteShopItem(itemId: String): Boolean {
        val ok = db.deleteShopItem(itemId)
        if (ok) {
            bgScope.launch {
                try {
                    ApiClient.deleteAdminItem(itemId)
                } catch (ignored: Exception) {}
            }
        }
        return ok
    }

    fun resetDefaultShopItems() {
        db.resetDefaultShopItems()
    }
}
