package com.example.vigil.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vigil.model.FriendProfile
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.LeaderboardEntry
import com.example.vigil.model.WorkoutReward
import com.example.vigil.network.ApiClient
import com.example.vigil.pose.Exercise
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel kết hợp CSDL SQL cục bộ (SQLite) và Backend REST API (MySQL).
 * Quản lý trạng thái Xác Thực, Bạn Bè, Bảng Xếp Hạng Thế Giới, Cửa Hàng, Trang Bị, và Tỷ Lệ Rớt Đồ.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VigilSqlDb.get(app)
    private val prefs = app.getSharedPreferences("vigil_auth", Context.MODE_PRIVATE)

    // Trạng thái tài khoản người dùng
    var currentUserId by mutableIntStateOf(1)
    var isLoggedIn by mutableStateOf(false)
    var name by mutableStateOf("Hachimi")
    var avatar by mutableStateOf("🦊")
    var gold by mutableIntStateOf(250)
    var gems by mutableIntStateOf(10)
    var xp by mutableIntStateOf(0)
    var level by mutableIntStateOf(1)
    var streak by mutableIntStateOf(1)
    var totalReps by mutableIntStateOf(0)
    var todayReps by mutableIntStateOf(0)
    var stage by mutableIntStateOf(1)

    // Danh sách Bạn Bè & Bảng Xếp Hạng Thế Giới
    var friendsList by mutableStateOf<List<FriendProfile>>(emptyList())
    var leaderboardList by mutableStateOf<List<LeaderboardEntry>>(emptyList())
    var leaderboardSortBy by mutableStateOf("CP") // "CP", "LEVEL", "REPS", "STREAK"

    // Danh sách Cửa Hàng & Kho đồ
    var shopItems by mutableStateOf<List<Item>>(emptyList())
    var inventory by mutableStateOf<List<InventoryItem>>(emptyList())

    // Phần thưởng vừa rớt từ trận đấu
    var lastReward by mutableStateOf<WorkoutReward?>(null)

    // Trạng thái thông báo & Lỗi xác thực
    var toastMessage by mutableStateOf<String?>(null)
    var authErrorMessage by mutableStateOf<String?>(null)

    val xpNeeded: Int get() = 80 + level * 160
    val title: String get() = when {
        level < 3 -> "Tân Binh"
        level < 6 -> "Võ Tăng"
        level < 10 -> "Chiến Binh"
        else -> "Huyền Thoại"
    }

    // Chỉ số gốc (Base Stats)
    val baseStr get() = 5 + totalReps / 3
    val baseEnd get() = 5 + totalReps / 4
    val basePre get() = 5 + totalReps / 3
    val baseLuck get() = 5 + totalReps / 5

    // Điểm cộng từ trang bị đang đeo
    val bonusStr get() = inventory.filter { it.isEquipped }.sumOf { it.item.bonusStr }
    val bonusEnd get() = inventory.filter { it.isEquipped }.sumOf { it.item.bonusEnd }
    val bonusPre get() = inventory.filter { it.isEquipped }.sumOf { it.item.bonusPre }
    val bonusLuck get() = inventory.filter { it.isEquipped }.sumOf { it.item.bonusLuck }

    // Tổng chỉ số thực tế sau khi tính trang bị
    val totalStr get() = baseStr + bonusStr
    val totalEnd get() = baseEnd + bonusEnd
    val totalPre get() = basePre + bonusPre
    val totalLuck get() = baseLuck + bonusLuck

    // Sát thương đòn đánh cơ bản mỗi rep (Base Attack Damage per Rep)
    // Scale trực tiếp từ Sức Mạnh (totalStr) và Chuẩn Xác (totalPre), hỗ trợ tối đa bởi Trang Bị
    val attackDamage: Int get() {
        val raw = (totalStr * 2.5 + totalPre * 1.0).toInt()
        return raw.coerceAtLeast(20)
    }

    // Tỉ lệ Bạo Kích (Critical Hit Rate) dựa trên Chuẩn xác (Pre) và May mắn (Luck)
    val critRate: Float get() = (0.05f + (totalPre * 0.003f) + (totalLuck * 0.002f)).coerceIn(0.05f, 0.50f)

    // Hệ số sát thương Bạo Kích (Critical Damage Multiplier: 1.5x - 2.0x)
    val critMultiplier: Float get() = (1.5f + (totalPre * 0.005f)).coerceAtMost(2.0f)

    // Lực Chiến (Combat Power - CP)
    val combatPower: Int get() = (totalStr * 10) + (totalEnd * 8) + (totalPre * 9) + (totalLuck * 6)
    val equipmentCombatPower: Int get() = (bonusStr * 10) + (bonusEnd * 8) + (bonusPre * 9) + (bonusLuck * 6)

    // Map các món đồ đang trang bị theo từng slot
    val equippedMap: Map<ItemSlot, InventoryItem?> get() {
        val map = mutableMapOf<ItemSlot, InventoryItem?>()
        ItemSlot.values().forEach { slot ->
            map[slot] = inventory.firstOrNull { it.isEquipped && it.item.slot == slot }
        }
        return map
    }

    init {
        // Kiểm tra phiên đăng nhập đã lưu trong SharedPreferences
        val savedUserId = prefs.getInt("logged_in_user_id", -1)
        if (savedUserId != -1) {
            val user = db.getUserProfile(savedUserId)
            if (user != null) {
                currentUserId = user.id
                name = user.username
                avatar = user.avatar
                isLoggedIn = true
                loadDataFromSql()
            } else {
                isLoggedIn = false
            }
        } else {
            // Mặc định cho phép người dùng vào ngay tài khoản mẫu Hachimi nếu chưa đăng ký tài khoản riêng
            val defaultUser = db.getUserProfile(1)
            if (defaultUser != null) {
                currentUserId = 1
                name = defaultUser.username
                avatar = defaultUser.avatar
                isLoggedIn = true
                loadDataFromSql()
            }
        }
    }

    fun loadDataFromSql() {
        shopItems = db.getAllItems()
        inventory = db.getInventory(currentUserId)

        val readable = db.readableDatabase
        val cursor = readable.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(currentUserId.toString()))
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow("username"))
            avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar")) ?: "🧑‍🎤"
            gold = cursor.getInt(cursor.getColumnIndexOrThrow("gold"))
            gems = cursor.getInt(cursor.getColumnIndexOrThrow("gems"))
            xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"))
            level = cursor.getInt(cursor.getColumnIndexOrThrow("level"))
            streak = cursor.getInt(cursor.getColumnIndexOrThrow("streak"))
            totalReps = cursor.getInt(cursor.getColumnIndexOrThrow("total_reps"))
            stage = cursor.getInt(cursor.getColumnIndexOrThrow("stage"))
        }
        cursor.close()

        val todayStr = LocalDate.now().toString()
        val wCursor = readable.rawQuery("SELECT SUM(reps) FROM workouts WHERE user_id = ? AND created_at = ?", arrayOf(currentUserId.toString(), todayStr))
        todayReps = if (wCursor.moveToFirst()) wCursor.getInt(0) else 0
        wCursor.close()

        // Nạp danh sách Bạn Bè và Bảng Xếp Hạng Thế Giới
        friendsList = db.getFriends(currentUserId)
        leaderboardList = db.getLeaderboard(currentUserId, leaderboardSortBy)
    }

    // =========================================================================
    // XÁC THỰC: ĐĂNG NHẬP, ĐĂNG KÝ & QUẢN LÝ PHIÊN
    // =========================================================================

    fun login(username: String, password: String): Boolean {
        authErrorMessage = null
        val (userId, msg) = db.loginUser(username, password)
        if (userId != null) {
            currentUserId = userId
            isLoggedIn = true
            prefs.edit().putInt("logged_in_user_id", userId).putString("username", username).apply()
            loadDataFromSql()
            toastMessage = "Đăng nhập thành công! Chào $name! ⚔️"

            viewModelScope.launch {
                ApiClient.login(username, password)
            }
            return true
        } else {
            authErrorMessage = msg
            toastMessage = msg
            return false
        }
    }

    fun register(username: String, password: String, chosenAvatar: String = "🧑‍🎤"): Boolean {
        authErrorMessage = null
        val (success, msg) = db.registerUser(username, password, chosenAvatar)
        if (success) {
            // Tự động đăng nhập luôn sau khi đăng ký
            login(username, password)
            toastMessage = "Đăng ký thành công! Chào mừng hiệp sĩ $username! 🎉"
            viewModelScope.launch {
                ApiClient.register(username, password, chosenAvatar)
            }
            return true
        } else {
            authErrorMessage = msg
            toastMessage = msg
            return false
        }
    }

    fun loginAsDemo(): Boolean {
        return login("Hachimi", "123456")
    }

    fun logout() {
        prefs.edit().remove("logged_in_user_id").remove("username").apply()
        isLoggedIn = false
        toastMessage = "Đã đăng xuất tài khoản!"
    }

    // =========================================================================
    // HỆ THỐNG BẠN BÈ & BẢNG XẾP HẠNG THẾ GIỚI
    // =========================================================================

    fun addFriend(friendUsername: String): Boolean {
        if (friendUsername.isBlank()) {
            toastMessage = "Vui lòng nhập tên người chơi!"
            return false
        }
        val (success, msg) = db.addFriendByUsername(currentUserId, friendUsername.trim())
        toastMessage = msg
        if (success) {
            friendsList = db.getFriends(currentUserId)
            leaderboardList = db.getLeaderboard(currentUserId, leaderboardSortBy)
            viewModelScope.launch {
                ApiClient.addFriend(name, friendUsername.trim())
            }
            return true
        }
        return false
    }

    fun removeFriend(friendId: Int, friendUsername: String): Boolean {
        val success = db.removeFriend(currentUserId, friendId)
        if (success) {
            toastMessage = "Đã hủy kết bạn với $friendUsername"
            friendsList = db.getFriends(currentUserId)
            leaderboardList = db.getLeaderboard(currentUserId, leaderboardSortBy)
            viewModelScope.launch {
                ApiClient.removeFriend(name, friendUsername)
            }
            return true
        }
        return false
    }

    fun setLeaderboardFilter(sortBy: String) {
        leaderboardSortBy = sortBy
        leaderboardList = db.getLeaderboard(currentUserId, sortBy)
    }

    /**
     * Mua vật phẩm từ Cửa Hàng
     */
    fun buyItem(item: Item): Boolean {
        if (gold < item.priceGold || gems < item.priceGems) {
            toastMessage = "Không đủ tiền để mua ${item.name}!"
            return false
        }

        val success = db.buyItem(currentUserId, item)
        if (success) {
            toastMessage = "Đã mua thành công ${item.name}!"
            loadDataFromSql()

            viewModelScope.launch {
                ApiClient.buyItem(name, item.id)
            }
            return true
        } else {
            toastMessage = "Giao dịch thất bại!"
            return false
        }
    }

    /**
     * Trang bị vật phẩm vào slot của Anh Hùng
     */
    fun equipItem(invItem: InventoryItem) {
        db.equipItem(currentUserId, invItem.invId, invItem.item.slot)
        toastMessage = "Đã trang bị ${invItem.item.name}!"
        loadDataFromSql()

        viewModelScope.launch {
            ApiClient.equipItem(name, invItem.invId)
        }
    }

    /**
     * Tháo trang bị
     */
    fun unequipItem(slot: ItemSlot) {
        val equipped = equippedMap[slot] ?: return
        db.unequipItem(equipped.invId)
        toastMessage = "Đã tháo ${equipped.item.name}!"
        loadDataFromSql()

        viewModelScope.launch {
            ApiClient.unequipItem(name, equipped.invId)
        }
    }

    /**
     * Tự động trang bị các món tốt nhất trong túi cho cả 6 slot (Auto Quick-Equip)
     */
    fun quickEquip() {
        var equippedCount = 0
        ItemSlot.values().forEach { slot ->
            val itemsForSlot = inventory.filter { it.item.slot == slot }
            if (itemsForSlot.isNotEmpty()) {
                val best = itemsForSlot.maxByOrNull { it.item.combatPower }
                if (best != null && !best.isEquipped) {
                    db.equipItem(currentUserId, best.invId, slot)
                    equippedCount++
                }
            }
        }
        if (equippedCount > 0) {
            toastMessage = "Đã tự động trang bị $equippedCount món tối ưu! ⚡"
        } else {
            toastMessage = "Bạn đã trang bị những món tốt nhất rồi! 🔥"
        }
        loadDataFromSql()
    }

    /**
     * Tháo toàn bộ trang bị
     */
    fun unequipAll() {
        db.unequipAll(currentUserId)
        toastMessage = "Đã tháo toàn bộ trang bị!"
        loadDataFromSql()
    }

    /**
     * Bán vật phẩm trong túi lấy vàng (50% giá trị)
     */
    fun sellItem(invItem: InventoryItem): Boolean {
        val sellPrice = invItem.item.sellPriceGold
        val success = db.sellItem(currentUserId, invItem.invId, sellPrice)
        if (success) {
            toastMessage = "Đã bán ${invItem.item.name} nhận +$sellPrice 🪙 Vàng!"
            loadDataFromSql()
            return true
        } else {
            toastMessage = "Bán thất bại!"
            return false
        }
    }

    /**
     * Hoàn thành bài tập & Tính toán Tỷ Lệ Rớt Đồ (Loot Drop Rate System)
     */
    fun finishWorkout(
        exercise: Exercise,
        score: Int,
        isBoss: Boolean = false,
        isFreeTraining: Boolean = false,
        monsterStageId: Int? = null
    ): WorkoutReward? {
        if (score <= 0) return null

        val reward = db.finishWorkoutAndRollDrop(
            userId = currentUserId,
            exercise = exercise.name,
            reps = score,
            holdSec = if (exercise == Exercise.PLANK) score * 2 else 0,
            score = score,
            isBoss = isBoss,
            isFreeTraining = isFreeTraining,
            monsterStageId = monsterStageId
        )

        if (isFreeTraining) {
            lastReward = null
            toastMessage = "Đã hoàn thành buổi tập tự do: +$score rep! (Không có phần thưởng)"
        } else {
            lastReward = reward
        }
        loadDataFromSql()

        viewModelScope.launch {
            val remoteReward = ApiClient.finishWorkout(
                username = name,
                exercise = exercise.name,
                reps = score,
                holdSeconds = if (exercise == Exercise.PLANK) score * 2 else 0,
                score = score,
                isBoss = isBoss,
                isFreeTraining = isFreeTraining
            )
            if (!isFreeTraining && remoteReward != null && remoteReward.droppedItem != null) {
                loadDataFromSql()
            }
        }

        return if (isFreeTraining) null else reward
    }

    fun dismissReward() {
        lastReward = null
    }

    fun clearToast() {
        toastMessage = null
    }

    fun last7Days(): List<Pair<LocalDate, Int>> {
        val list = mutableListOf<Pair<LocalDate, Int>>()
        val readable = db.readableDatabase
        for (i in 6 downTo 0) {
            val d = LocalDate.now().minusDays(i.toLong())
            val c = readable.rawQuery("SELECT SUM(reps) FROM workouts WHERE user_id = ? AND created_at = ?", arrayOf(currentUserId.toString(), d.toString()))
            val reps = if (c.moveToFirst()) c.getInt(0) else 0
            c.close()
            list.add(d to reps)
        }
        return list
    }

    fun weekReps() = last7Days().sumOf { it.second }
}
