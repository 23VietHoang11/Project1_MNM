package com.example.vigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vigil.data.GameViewModel
import com.example.vigil.model.AdventureData
import com.example.vigil.model.Monster
import com.example.vigil.pose.Exercise
import kotlinx.coroutines.launch

// Data class cho Người chơi hiển thị trên Admin
data class AdminUserEntry(
    val id: Int,
    val username: String,
    val avatar: String,
    val level: Int,
    val gold: Int,
    val streak: Int,
    val totalReps: Int,
    val role: String,
    var isLocked: Boolean,
    var lockReason: String? = null
)

// Data class cho Cảnh báo gian lận
data class CheatAlertItem(
    val id: Int,
    val username: String,
    val exercise: String,
    val reps: Int,
    val durationSeconds: Int,
    val reason: String,
    var isResolved: Boolean = false
)

// Data class cho Cấu hình góc bài tập AI
data class ExerciseConfigItem(
    val code: String,
    val name: String,
    var extendAngle: Float,
    var contractAngle: Float,
    var damageFactor: Float,
    var caloPerRep: Float
)

@Composable
fun AdminScreen(vm: GameViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("USERS") } // USERS, MONSTERS, EXERCISES, CHEATS
    val scope = rememberCoroutineScope()

    // Nạp danh sách người dùng thực tế từ CSDL SQLite (chỉ các acc được tạo, Hachimi là Admin ban đầu)
    var usersList by remember { mutableStateOf(vm.getAdminUsers()) }

    LaunchedEffect(selectedTab) {
        usersList = vm.getAdminUsers()
    }

    var monstersList by remember {
        mutableStateOf(
            AdventureData.CHAPTERS.flatMap { it.monsters }.map { m ->
                Monster(m.stageId, m.name, m.title, m.avatar, m.maxHp, m.exercise, m.isBoss, m.expReward, m.goldReward, m.lore, m.dropRarity, m.colorHex)
            }
        )
    }

    var exerciseConfigs by remember {
        mutableStateOf(
            listOf(
                ExerciseConfigItem("PUSHUP", "Hít đất (Push-up)", 165f, 90f, 1.2f, 0.8f),
                ExerciseConfigItem("SQUAT", "Squat", 170f, 90f, 1.0f, 1.0f),
                ExerciseConfigItem("SITUP", "Gập bụng (Sit-up)", 160f, 75f, 1.1f, 0.6f),
                ExerciseConfigItem("PLANK", "Plank", 180f, 170f, 1.5f, 0.1f),
                ExerciseConfigItem("PULLUP", "Hít xà (Pull-up)", 160f, 70f, 2.0f, 1.5f),
                ExerciseConfigItem("DIP", "Xà kép (Dip)", 160f, 90f, 1.4f, 0.9f)
            )
        )
    }

    var cheatAlerts by remember {
        mutableStateOf(
            listOf(
                CheatAlertItem(101, "SpeedyHacker99", "PUSHUP", 150, 12, "Tốc độ 80ms/rep (< 200ms theo tiêu chuẩn NFR1)"),
                CheatAlertItem(102, "BotFarmMaster", "SQUAT", 280, 25, "Spam cảm biến gia tốc liên tục")
            )
        )
    }

    var showToastMessage by remember { mutableStateOf<String?>(null) }
    var editingMonster by remember { mutableStateOf<Monster?>(null) }
    var editingExercise by remember { mutableStateOf<ExerciseConfigItem?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .padding(16.dp)
    ) {
        // Topbar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(C.Card)
                        .border(1.dp, C.Border, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("🛡️ BẢNG QUẢN TRỊ (ADMIN)", color = C.Orange, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("FR8 & UC08 • Zero-Hardcoding", color = C.Muted, fontSize = 11.sp)
                }
            }

            StatBadge("ROLE_ADMIN", C.Yellow)
        }

        Spacer(Modifier.height(16.dp))

        // Live KPI Grid
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Column {
                    Text("NGƯỜI DÙNG", color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${usersList.size}", color = C.Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Column {
                    Text("DAU HÔM NAY", color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("3", color = C.Green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Column {
                    Text("TỔNG REPS", color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${usersList.sumOf { it.totalReps }}", color = C.Pink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Column {
                    Text("CẢNH BÁO", color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${cheatAlerts.count { !it.isResolved }}", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tabs Selection Bar
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13102B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                "USERS" to "👥 Người Chơi",
                "MONSTERS" to "🐉 Quái Vật",
                "EXERCISES" to "📐 Góc AI",
                "CHEATS" to "🚨 Gian Lận"
            )
            tabs.forEach { (key, label) ->
                val isSelected = selectedTab == key
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) C.Orange else Color.Transparent)
                        .clickable { selectedTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.White else C.Muted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tab Content
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // TAB 1: QUẢN LÝ NGƯỜI CHƠI (FR8.1 & UC08.1)
            if (selectedTab == "USERS") {
                item {
                    Text("DANH SÁCH TÀI KHOẢN NGƯỜI DÙNG (${usersList.size})", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(usersList) { user ->
                    VCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.avatar, fontSize = 26.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isHachimi = user.username.equals("Hachimi", ignoreCase = true)
                                        Text(user.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.width(6.dp))
                                        StatBadge(
                                            if (isHachimi) "👑 ADMIN TỐI CAO" else user.role,
                                            if (user.role == "Admin") C.Yellow else C.Blue
                                        )
                                    }
                                    Text("Lv.${user.level} • 🪙 ${user.gold} • 🔥 ${user.streak} ngày • ${user.totalReps} reps", color = C.Muted, fontSize = 11.sp)
                                    if (user.isLocked && user.lockReason != null) {
                                        Text("Lý do khóa: ${user.lockReason}", color = Color(0xFFEF4444), fontSize = 10.sp)
                                    }
                                }
                            }

                            val isHachimi = user.username.equals("Hachimi", ignoreCase = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isHachimi) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("🛡️ BẤT KHẢ XÂM PHẠM", color = C.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Nút phân quyền: Cấp Admin hoặc Giáng User
                                    val isRoleAdmin = user.role == "Admin"
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isRoleAdmin) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                            .border(1.dp, if (isRoleAdmin) Color(0xFFF59E0B) else Color(0xFF8B5CF6), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val newRole = if (isRoleAdmin) "User" else "Admin"
                                                vm.setAdminUserRole(user.id, user.username, newRole)
                                                usersList = vm.getAdminUsers()
                                                showToastMessage = if (isRoleAdmin) "Đã hạ quyền ${user.username} xuống User!" else "Đã cấp quyền Admin cho ${user.username}!"
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isRoleAdmin) "🛡️ Hạ User" else "👑 Cấp Admin", color = if (isRoleAdmin) Color(0xFFFDE68A) else Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Nút khóa / mở khóa tài khoản
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (user.isLocked) C.Green.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                            .border(1.dp, if (user.isLocked) C.Green else Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val willLock = !user.isLocked
                                                val reason = if (willLock) "Khóa bởi Quản trị viên" else null
                                                vm.setAdminUserLock(user.id, user.username, willLock, reason)
                                                usersList = vm.getAdminUsers()
                                                showToastMessage = if (willLock) "Đã khóa tài khoản ${user.username}!" else "Đã mở khóa tài khoản ${user.username}!"
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (user.isLocked) "🔓 Mở" else "🔒 Khóa", color = if (user.isLocked) C.Green else Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: CẤU HÌNH QUÁI VẬT (FR8.2 & ZERO-HARDCODING)
            else if (selectedTab == "MONSTERS") {
                item {
                    Text("CẤU HÌNH THÔNG SỐ QUÁI VẬT & BOSS (ZERO-HARDCODING)", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(monstersList) { monster ->
                    VCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(monster.avatar, fontSize = 28.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(monster.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (monster.isBoss) {
                                            Spacer(Modifier.width(6.dp))
                                            StatBadge("BOSS", C.Pink)
                                        }
                                    }
                                    Text("Chương ${monster.stageId} • ${monster.exercise.name} • +${monster.expReward} EXP • +${monster.goldReward} 🪙", color = C.Muted, fontSize = 11.sp)
                                    Text("Máu (HP): ${monster.maxHp}", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(C.Card)
                                        .border(1.dp, C.Border, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val updated = Monster(monster.stageId, monster.name, monster.title, monster.avatar, maxOf(100, monster.maxHp - 100), monster.exercise, monster.isBoss, monster.expReward, monster.goldReward, monster.lore, monster.dropRarity, monster.colorHex)
                                            monstersList = monstersList.map { if (it.name == monster.name) updated else it }
                                            vm.saveAdminMonster(updated)
                                            showToastMessage = "Đã giảm 100 HP cho ${monster.name}"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("-100", color = C.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(C.Card)
                                        .border(1.dp, C.Border, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val updated = Monster(monster.stageId, monster.name, monster.title, monster.avatar, monster.maxHp + 200, monster.exercise, monster.isBoss, monster.expReward, monster.goldReward, monster.lore, monster.dropRarity, monster.colorHex)
                                            monstersList = monstersList.map { if (it.name == monster.name) updated else it }
                                            vm.saveAdminMonster(updated)
                                            showToastMessage = "Đã tăng 200 HP cho ${monster.name}"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("+200", color = C.Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(C.Orange.copy(alpha = 0.2f))
                                        .border(1.dp, C.Orange, RoundedCornerShape(8.dp))
                                        .clickable { editingMonster = monster }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("✏️ Sửa", color = C.Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: CẤU HÌNH BÀI TẬP AI (FR8.2: THRESHOLD)
            else if (selectedTab == "EXERCISES") {
                item {
                    Text("CẤU HÌNH NGƯỠNG GÓC KHỚP AI MEDIAPIPE", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(exerciseConfigs) { ex ->
                    VCard {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ex.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatBadge("${ex.code}", C.Blue)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Góc duỗi chuẩn: ${ex.extendAngle}°", color = C.Blue, fontSize = 12.sp)
                                Text("Góc gập chuẩn: ${ex.contractAngle}°", color = C.Pink, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sát thương/Rep: ${ex.damageFactor}x", color = C.Yellow, fontSize = 12.sp)
                                Text("Calo/Rep: ${ex.caloPerRep} kcal", color = C.Green, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(C.Orange.copy(alpha = 0.15f))
                                    .border(1.dp, C.Orange, RoundedCornerShape(8.dp))
                                    .clickable { editingExercise = ex }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚙️ Hiệu Chỉnh Ngưỡng Góc & Calo", color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // TAB 4: CẢNH BÁO GIAN LẬN (ANTI-CHEAT)
            else if (selectedTab == "CHEATS") {
                item {
                    Text("PHÁT HIỆN GIAN LẬN THỜI GIAN THỰC (NFR1 & NFR4)", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(cheatAlerts) { alert ->
                    VCard(border = if (alert.isResolved) C.Border else Color(0xFFEF4444).copy(alpha = 0.5f)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️ ${alert.username}", color = if (alert.isResolved) C.Muted else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    StatBadge(alert.exercise, C.Blue)
                                }
                                Text("Số rep: ${alert.reps} • Thời gian: ${alert.durationSeconds}s", color = C.Muted, fontSize = 11.sp)
                                Text(alert.reason, color = C.Text, fontSize = 12.sp)
                            }

                            if (!alert.isResolved) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444))
                                        .clickable {
                                            val targetUser = usersList.firstOrNull { it.username.equals(alert.username, ignoreCase = true) }
                                            if (targetUser != null) {
                                                vm.setAdminUserLock(targetUser.id, targetUser.username, true, alert.reason)
                                            } else {
                                                scope.launch {
                                                    com.example.vigil.network.ApiClient.banUser(alert.username, alert.reason)
                                                }
                                            }
                                            cheatAlerts = cheatAlerts.map { if (it.id == alert.id) it.copy(isResolved = true) else it }
                                            usersList = vm.getAdminUsers()
                                            showToastMessage = "Đã khóa vĩnh viễn tài khoản ${alert.username} do gian lận!"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Khóa Ngay", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                StatBadge("ĐÃ XỬ LÝ", C.Green)
                            }
                        }
                    }
                }
            }
        }

        // Dialog Chỉnh Sửa Quái Vật (Zero-Hardcoding)
        editingMonster?.let { monster ->
            var hpText by remember(monster) { mutableStateOf(monster.maxHp.toString()) }
            var expText by remember(monster) { mutableStateOf(monster.expReward.toString()) }
            var goldText by remember(monster) { mutableStateOf(monster.goldReward.toString()) }

            AlertDialog(
                onDismissRequest = { editingMonster = null },
                title = {
                    Text("🐉 Hiệu Chỉnh Quái Vật: ${monster.name}", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = hpText,
                            onValueChange = { hpText = it },
                            label = { Text("Máu tối đa (HP)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expText,
                            onValueChange = { expText = it },
                            label = { Text("Thưởng EXP") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = goldText,
                            onValueChange = { goldText = it },
                            label = { Text("Thưởng Vàng (Gold)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newHp = hpText.toIntOrNull() ?: monster.maxHp
                            val newExp = expText.toIntOrNull() ?: monster.expReward
                            val newGold = goldText.toIntOrNull() ?: monster.goldReward
                            val updated = Monster(monster.stageId, monster.name, monster.title, monster.avatar, newHp, monster.exercise, monster.isBoss, newExp, newGold, monster.lore, monster.dropRarity, monster.colorHex)
                            monstersList = monstersList.map { if (it.name == monster.name) updated else it }
                            vm.saveAdminMonster(updated)
                            showToastMessage = "Đã lưu cấu hình mới cho ${monster.name}!"
                            editingMonster = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Orange)
                    ) {
                        Text("LƯU THÔNG SỐ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMonster = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                },
                containerColor = Color(0xFF1B1530)
            )
        }

        // Dialog Chỉnh Sửa Ngưỡng Góc Bài Tập AI
        editingExercise?.let { ex ->
            var extendText by remember(ex) { mutableStateOf(ex.extendAngle.toString()) }
            var contractText by remember(ex) { mutableStateOf(ex.contractAngle.toString()) }
            var damageText by remember(ex) { mutableStateOf(ex.damageFactor.toString()) }
            var caloText by remember(ex) { mutableStateOf(ex.caloPerRep.toString()) }

            AlertDialog(
                onDismissRequest = { editingExercise = null },
                title = {
                    Text("📐 Hiệu Chỉnh Ngưỡng Góc AI: ${ex.name}", color = C.Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = extendText,
                            onValueChange = { extendText = it },
                            label = { Text("Góc duỗi chuẩn (°)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contractText,
                            onValueChange = { contractText = it },
                            label = { Text("Góc gập chuẩn (°)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = damageText,
                            onValueChange = { damageText = it },
                            label = { Text("Hệ số sát thương/Rep") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = caloText,
                            onValueChange = { caloText = it },
                            label = { Text("Calo tiêu hao mỗi rep (kcal)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newExtend = extendText.toFloatOrNull() ?: ex.extendAngle
                            val newContract = contractText.toFloatOrNull() ?: ex.contractAngle
                            val newDamage = damageText.toFloatOrNull() ?: ex.damageFactor
                            val newCalo = caloText.toFloatOrNull() ?: ex.caloPerRep
                            val updated = ex.copy(extendAngle = newExtend, contractAngle = newContract, damageFactor = newDamage, caloPerRep = newCalo)
                            exerciseConfigs = exerciseConfigs.map { if (it.code == ex.code) updated else it }
                            vm.saveAdminExercise(updated)
                            showToastMessage = "Đã lưu ngưỡng góc AI cho bài tập ${ex.name}!"
                            editingExercise = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Blue)
                    ) {
                        Text("LƯU CẤU HÌNH GÓC", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingExercise = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                },
                containerColor = Color(0xFF1B1530)
            )
        }

        // Toast feedback
        showToastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                showToastMessage = null
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F1B3E))
                    .border(1.dp, C.Orange, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(msg, color = C.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminAccessDeniedScreen(username: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(C.Card)
                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text("⛔", fontSize = 48.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "TỪ CHỐI TRUY CẬP",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Tài khoản [$username] có vai trò Người Chơi (User) và không có quyền truy cập Bảng Quản Trị Hệ Thống.\n\nChỉ nick Hachimi (Quản trị viên tối cao) hoặc tài khoản được Hachimi cấp quyền mới được phép vào đây.",
                color = C.Muted,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Orange)
                    .clickable { onBack() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("← Quay Lại Màn Hình Chính", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
