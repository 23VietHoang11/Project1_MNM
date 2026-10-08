package com.example.vigil.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.WorkoutReward
import com.example.vigil.network.ApiClient
import com.example.vigil.pose.Exercise
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel kết hợp CSDL SQL cục bộ (SQLite) và Backend REST API (MySQL).
 * Quản lý trạng thái Anh Hùng, Cửa Hàng, Trang Bị, và Tỷ Lệ Rớt Đồ.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VigilSqlDb.get(app)

    var name by mutableStateOf("Hachimi")
    var gold by mutableIntStateOf(250)
    var gems by mutableIntStateOf(10)
    var xp by mutableIntStateOf(0)
    var level by mutableIntStateOf(1)
    var streak by mutableIntStateOf(1)
    var totalReps by mutableIntStateOf(0)
    var todayReps by mutableIntStateOf(0)
    var stage by mutableIntStateOf(1)

    // Danh sách Cửa Hàng & Kho đồ
    var shopItems by mutableStateOf<List<Item>>(emptyList())
    var inventory by mutableStateOf<List<InventoryItem>>(emptyList())

    // Phần thưởng vừa rớt từ trận đấu
    var lastReward by mutableStateOf<WorkoutReward?>(null)

    // Trạng thái thông báo
    var toastMessage by mutableStateOf<String?>(null)

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
        loadDataFromSql()
    }

    fun loadDataFromSql() {
        shopItems = db.getAllItems()
        inventory = db.getInventory(1)

        val readable = db.readableDatabase
        val cursor = readable.rawQuery("SELECT * FROM users WHERE id = 1", null)
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow("username"))
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
        val wCursor = readable.rawQuery("SELECT SUM(reps) FROM workouts WHERE user_id = 1 AND created_at = ?", arrayOf(todayStr))
        todayReps = if (wCursor.moveToFirst()) wCursor.getInt(0) else 0
        wCursor.close()
    }

    /**
     * Mua vật phẩm từ Cửa Hàng
     */
    fun buyItem(item: Item): Boolean {
        if (gold < item.priceGold || gems < item.priceGems) {
            toastMessage = "Không đủ tiền để mua ${item.name}!"
            return false
        }

        // Lưu vào SQL cục bộ
        val success = db.buyItem(1, item)
        if (success) {
            toastMessage = "Đã mua thành công ${item.name}!"
            loadDataFromSql()

            // Đồng bộ lên MySQL Backend nếu có mạng
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
        db.equipItem(1, invItem.invId, invItem.item.slot)
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
                    db.equipItem(1, best.invId, slot)
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
        db.unequipAll(1)
        toastMessage = "Đã tháo toàn bộ trang bị!"
        loadDataFromSql()
    }

    /**
     * Bán vật phẩm trong túi lấy vàng (50% giá trị)
     */
    fun sellItem(invItem: InventoryItem): Boolean {
        val sellPrice = invItem.item.sellPriceGold
        val success = db.sellItem(1, invItem.invId, sellPrice)
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
     * @param isFreeTraining Nếu là tập tự do thì KHÔNG nhận phần thưởng (vàng, xp, rơi đồ, không nhảy ải)
     * @param monsterStageId Ải quái vật đang đánh để tiến trình ải chỉ tăng khi thắng đúng ải
     */
    fun finishWorkout(
        exercise: Exercise,
        score: Int,
        isBoss: Boolean = false,
        isFreeTraining: Boolean = false,
        monsterStageId: Int? = null
    ): WorkoutReward? {
        if (score <= 0) return null

        // 1. Ghi nhận bài tập vào SQLite Database
        val reward = db.finishWorkoutAndRollDrop(
            userId = 1,
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

        // 2. Gửi đồng bộ lên MySQL server qua REST API (chạy ngầm)
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
                // Nếu server trả về thêm vật phẩm, đồng bộ lại
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
            val c = readable.rawQuery("SELECT SUM(reps) FROM workouts WHERE user_id = 1 AND created_at = ?", arrayOf(d.toString()))
            val reps = if (c.moveToFirst()) c.getInt(0) else 0
            c.close()
            list.add(d to reps)
        }
        return list
    }

    fun weekReps() = last7Days().sumOf { it.second }
}
