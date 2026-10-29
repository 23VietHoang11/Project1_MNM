package com.example.vigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.vigil.model.AdminUserEntry
import com.example.vigil.model.CheatAlertItem
import com.example.vigil.model.ExerciseConfigItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.Monster
import com.example.vigil.model.Rarity

/**
 * Giao diện Bảng Quản Trị Hệ Thống (Admin Screen).
 * Tuân thủ Clean Architecture: Toàn bộ nghiệp vụ truy vấn CSDL SQLite và Backend REST API
 * được chuyển qua ViewModel và AdminRepository. Lớp UI thuần túy hiển thị và nhận sự kiện.
 */
@Composable
fun AdminScreen(vm: GameViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("USERS") } // USERS, ITEMS, MONSTERS, EXERCISES, CHEATS

    // Lấy danh sách người chơi thực tế được tạo trong CSDL (Chỉ Hachimi là Admin ban đầu, các acc khác là User)
    var usersList by remember { mutableStateOf(vm.getAdminUsers()) }
    var itemsList by remember { mutableStateOf(vm.getAdminItems()) }
    var selectedItemSlotFilter by remember { mutableStateOf<ItemSlot?>(null) }
    var itemSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedTab) {
        usersList = vm.getAdminUsers()
        itemsList = vm.getAdminItems()
    }

    var monstersList by remember { mutableStateOf(vm.getAdminMonsters()) }
    var exerciseConfigs by remember { mutableStateOf(vm.getAdminExerciseConfigs()) }
    var cheatAlerts by remember { mutableStateOf(vm.getAdminCheatAlerts()) }

    var showToastMessage by remember { mutableStateOf<String?>(null) }
    var editingMonster by remember { mutableStateOf<Monster?>(null) }
    var editingExercise by remember { mutableStateOf<ExerciseConfigItem?>(null) }
    var editingShopItem by remember { mutableStateOf<Item?>(null) }
    var isCreatingNewShopItem by remember { mutableStateOf(false) }

    // State cho việc Đổi Role & Cấp Admin
    var showingGrantAdminDialog by remember { mutableStateOf(false) }
    var editingRoleUser by remember { mutableStateOf<AdminUserEntry?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- 1. TOPBAR ---
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("🛡️ BẢNG QUẢN TRỊ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("Trung tâm kiểm soát & Cân bằng game", color = C.Muted, fontSize = 12.sp)
                }
            }

            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFC94D).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFFC94D).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text("👑 ROLE_ADMIN", color = Color(0xFFFFC94D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- 2. LIVE KPI METRICS CÂN ĐỐI ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminKpiCard(Modifier.weight(1f), "Tài Khoản", "${usersList.size}", Color(0xFF60A5FA))
            AdminKpiCard(Modifier.weight(1f), "Vật Phẩm", "${itemsList.size}", Color(0xFFFFC94D))
            AdminKpiCard(Modifier.weight(1f), "Tổng Reps", "${usersList.sumOf { it.totalReps }}", Color(0xFFF472B6))
            val unresolvedCheats = cheatAlerts.count { !it.isResolved }
            AdminKpiCard(
                Modifier.weight(1f),
                "Cảnh Báo",
                "$unresolvedCheats",
                if (unresolvedCheats > 0) Color(0xFFEF4444) else Color(0xFF34D399)
            )
        }

        Spacer(Modifier.height(14.dp))

        // --- 3. TAB SELECTION BAR (5 TABS: NGƯỜI CHƠI, VẬT PHẨM, QUÁI VẬT, GÓC AI, GIAN LẬN) ---
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13102B))
                .border(1.dp, Color(0xFF251F4A), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val tabs = listOf(
                "USERS" to "👥 Người Chơi",
                "ITEMS" to "🛒 Vật Phẩm",
                "MONSTERS" to "🐉 Quái Vật",
                "EXERCISES" to "📐 Góc AI",
                "CHEATS" to "🚨 Gian Lận"
            )
            tabs.forEach { (key, label) ->
                val isSelected = selectedTab == key
                val hasAlert = key == "CHEATS" && cheatAlerts.any { !it.isResolved }

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) C.Orange else Color.Transparent)
                        .clickable { selectedTab = key }
                        .padding(vertical = 9.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            color = if (isSelected) Color.White else C.Muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (hasAlert && !isSelected) {
                            Spacer(Modifier.width(3.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 4. NỘI DUNG TỪNG TAB ---
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==================== TAB 1: NGƯỜI CHƠI (CHỈ LẤY TỪ CSDL THỰC TẾ) ====================
            if (selectedTab == "USERS") {
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TÀI KHOẢN TRONG CSDL (${usersList.size})", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Chỉ Hachimi là Admin ban đầu", color = Color(0xFFFFC94D), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Nút hành động: Cấp Admin theo tên tài khoản & Làm mới
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .weight(1.3f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.Orange)
                                    .clickable { showingGrantAdminDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👑", fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cấp Quyền Admin Theo Tên", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                Modifier
                                    .weight(0.7f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF221A3E))
                                    .border(1.dp, Color(0xFF3B2F63), RoundedCornerShape(10.dp))
                                    .clickable {
                                        usersList = vm.getAdminUsers()
                                        showToastMessage = "Đã làm mới danh sách tài khoản từ CSDL!"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔄 Làm Mới", color = Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Nếu chỉ mới có duy nhất Hachimi (chưa tạo thêm acc nào khác)
                if (usersList.size <= 1) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF16122E))
                                .border(1.dp, Color(0xFF2E2657), RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("🛡️", fontSize = 24.sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Quy tắc quản trị vai trò:", color = Color(0xFFFFC94D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Hiện tại duy nhất Hachimi là Admin. Mọi người chơi bạn đăng ký trong ứng dụng sẽ tự động xuất hiện tại đây với vai trò User. Bạn có thể bấm Thăng Cấp ADMIN cho bất kỳ ai!",
                                    color = C.Muted,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Danh sách người chơi thực tế lấy từ CSDL
                items(usersList) { user ->
                    VCard {
                        Column(Modifier.fillMaxWidth()) {
                            // Dòng 1: Thông tin người chơi
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF221A3E))
                                        .border(1.dp, Color(0xFF3B2F63), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(user.avatar, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isHachimi = user.username.equals("Hachimi", ignoreCase = true)
                                        val isRoleAdmin = user.role.equals("Admin", ignoreCase = true)
                                        val isRoleMod = user.role.equals("Moderator", ignoreCase = true)

                                        Text(user.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.width(6.dp))
                                        StatBadge(
                                            when {
                                                isHachimi -> "👑 ROOT ADMIN"
                                                isRoleAdmin -> "👑 ADMIN"
                                                isRoleMod -> "⚔️ MODERATOR"
                                                else -> "🛡️ USER"
                                            },
                                            when {
                                                isHachimi || isRoleAdmin -> C.Yellow
                                                isRoleMod -> Color(0xFFA78BFA)
                                                else -> C.Blue
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "Lv.${user.level}  •  🪙 ${user.gold}  •  🔥 ${user.streak}d  •  💪 ${user.totalReps} reps",
                                        color = C.Muted,
                                        fontSize = 11.sp
                                    )
                                    if (user.isLocked) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            "🔒 Đang bị khóa: ${user.lockReason ?: "Vi phạm chính sách"}",
                                            color = Color(0xFFFF6B6B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF252047)))
                            Spacer(Modifier.height(10.dp))

                            // Dòng 2: Hàng nút bấm phân quyền (Admin / User / Mod) & Khóa nick
                            val isHachimi = user.username.equals("Hachimi", ignoreCase = true)
                            if (isHachimi) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFC94D).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFFFFC94D).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🛡️ TÀI KHOẢN ROOT ADMIN - BẤT KHẢ XÂM PHẠM", color = Color(0xFFFFC94D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isRoleAdmin = user.role.equals("Admin", ignoreCase = true)

                                    // Nút chuyển đổi: Thăng cấp ADMIN hoặc Hạ về USER
                                    Box(
                                        Modifier
                                            .weight(1.3f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isRoleAdmin) Color(0xFF6D28D9).copy(alpha = 0.35f) else Color(0xFFFF9800))
                                            .border(1.dp, if (isRoleAdmin) Color(0xFF8B5CF6) else Color(0xFFFFB74D), RoundedCornerShape(10.dp))
                                            .clickable {
                                                val newRole = if (isRoleAdmin) "User" else "Admin"
                                                vm.setAdminUserRole(user.id, user.username, newRole)
                                                usersList = vm.getAdminUsers()
                                                showToastMessage = if (isRoleAdmin) "Đã hạ quyền ${user.username} về User!" else "Đã cấp quyền ADMIN thành công cho ${user.username}!"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (isRoleAdmin) "🛡️" else "👑", fontSize = 12.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                if (isRoleAdmin) "Hạ Về User" else "Thăng ADMIN",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Nút đổi vai trò chi tiết (Mở hộp thoại chọn User / Admin / Moderator)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1E193C))
                                            .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                            .clickable { editingRoleUser = user },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⚙️ Đổi Role", color = Color(0xFFDDD6FE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Nút Khóa / Mở Khóa Acc
                                    Box(
                                        Modifier
                                            .weight(0.9f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (user.isLocked) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                            .border(1.dp, if (user.isLocked) Color(0xFF10B981) else Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                            .clickable {
                                                val willLock = !user.isLocked
                                                val reason = if (willLock) "Khóa bởi Quản trị viên" else null
                                                vm.setAdminUserLock(user.id, user.username, willLock, reason)
                                                usersList = vm.getAdminUsers()
                                                showToastMessage = if (willLock) "Đã khóa tài khoản ${user.username}!" else "Đã mở khóa tài khoản ${user.username}!"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (user.isLocked) "🔓 Mở" else "🔒 Khóa",
                                            color = if (user.isLocked) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== TAB 2: QUẢN LÝ VẬT PHẨM CỬA HÀNG (SHOP ITEMS) ====================
            else if (selectedTab == "ITEMS") {
                val filteredItems = itemsList.filter { item ->
                    val matchSlot = selectedItemSlotFilter == null || item.slot == selectedItemSlotFilter
                    val matchQuery = itemSearchQuery.isBlank() ||
                            item.name.contains(itemSearchQuery, ignoreCase = true) ||
                            item.id.contains(itemSearchQuery, ignoreCase = true)
                    matchSlot && matchQuery
                }

                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "VẬT PHẨM BÁN TRONG CỬA HÀNG (${filteredItems.size}/${itemsList.size})",
                                color = C.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text("Đồng bộ trực tiếp với Cửa hàng", color = Color(0xFFFFC94D), fontSize = 10.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Nút Thêm Vật Phẩm Mới & Khôi Phục Mặc Định
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .weight(1.3f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.Orange)
                                    .clickable {
                                        isCreatingNewShopItem = true
                                        editingShopItem = Item(
                                            "item_custom_${System.currentTimeMillis() % 10000}",
                                            "Trang Bị Mới",
                                            ItemSlot.WEAPON,
                                            Rarity.EPIC,
                                            500,
                                            10,
                                            25,
                                            15,
                                            15,
                                            10,
                                            "Vật phẩm đặc biệt do Quản trị viên chế tác.",
                                            "⚔️"
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("➕", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Thêm Vật Phẩm Mới", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                Modifier
                                    .weight(0.7f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF221A3E))
                                    .border(1.dp, Color(0xFF3B2F63), RoundedCornerShape(10.dp))
                                    .clickable {
                                        vm.resetAdminDefaultItems()
                                        itemsList = vm.getAdminItems()
                                        showToastMessage = "Đã khôi phục kho vật phẩm mặc định của Cửa hàng!"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔄 Khôi Phục Gốc", color = Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Thanh lọc theo Loại Trang Bị (ItemSlot)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                val isAll = selectedItemSlotFilter == null
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAll) C.Orange else Color(0xFF1A1533))
                                        .border(1.dp, if (isAll) C.Orange else C.Border, RoundedCornerShape(8.dp))
                                        .clickable { selectedItemSlotFilter = null }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Tất cả (${itemsList.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            items(ItemSlot.entries.toList()) { slot ->
                                val isSel = selectedItemSlotFilter == slot
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) C.Orange else Color(0xFF1A1533))
                                        .border(1.dp, if (isSel) C.Orange else C.Border, RoundedCornerShape(8.dp))
                                        .clickable { selectedItemSlotFilter = slot }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("${slot.icon} ${slot.label}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Ô tìm kiếm tên vật phẩm
                        OutlinedTextField(
                            value = itemSearchQuery,
                            onValueChange = { itemSearchQuery = it },
                            placeholder = { Text("🔍 Tìm kiếm tên hoặc mã vật phẩm...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = C.Orange,
                                unfocusedBorderColor = C.Border,
                                focusedContainerColor = Color(0xFF141029),
                                unfocusedContainerColor = Color(0xFF141029)
                            )
                        )
                    }
                }

                items(filteredItems, key = { it.id }) { shopItem ->
                    val rarityColor = Color(shopItem.rarity.colorHex)
                    VCard(border = rarityColor.copy(alpha = 0.45f)) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(rarityColor.copy(alpha = 0.15f))
                                        .border(1.5.dp, rarityColor, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(shopItem.icon, fontSize = 26.sp)
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            shopItem.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        StatBadge(shopItem.rarity.label, rarityColor)
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "${shopItem.slot.icon} ${shopItem.slot.label}  •  ⚡ ${shopItem.combatPower} CP  •  Mã: ${shopItem.id}",
                                        color = C.Muted,
                                        fontSize = 11.sp
                                    )
                                    if (shopItem.description.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            shopItem.description,
                                            color = C.Muted.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Giá bán trong Cửa hàng
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141029))
                                        .border(1.dp, Color(0xFF2E2657), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "🪙 ${shopItem.priceGold}",
                                        color = C.Yellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (shopItem.priceGems > 0) {
                                        Text(
                                            "💎 ${shopItem.priceGems}",
                                            color = C.Blue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Hàng chỉ số cộng thêm của vật phẩm
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AdminMetricChip(Modifier.weight(1f), "💪 Sức mạnh", "+${shopItem.bonusStr}", C.Orange)
                                AdminMetricChip(Modifier.weight(1f), "🛡️ Bền bỉ", "+${shopItem.bonusEnd}", C.Green)
                                AdminMetricChip(Modifier.weight(1f), "🎯 Chuẩn xác", "+${shopItem.bonusPre}", C.Blue)
                                AdminMetricChip(Modifier.weight(1f), "🍀 May mắn", "+${shopItem.bonusLuck}", C.Yellow)
                            }

                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF252047)))
                            Spacer(Modifier.height(10.dp))

                            // Hàng nút thao tác nhanh: Giảm giá, Tăng giá, Sửa chi tiết, Xóa khỏi Cửa hàng
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .weight(0.85f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E193C))
                                        .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newGold = maxOf(10, shopItem.priceGold - 50)
                                            val updated = Item(
                                                shopItem.id, shopItem.name, shopItem.slot, shopItem.rarity,
                                                newGold, shopItem.priceGems, shopItem.bonusStr, shopItem.bonusEnd,
                                                shopItem.bonusPre, shopItem.bonusLuck, shopItem.description, shopItem.icon
                                            )
                                            vm.saveAdminItem(updated)
                                            itemsList = vm.getAdminItems()
                                            showToastMessage = "Đã giảm giá ${shopItem.name} xuống $newGold Vàng!"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-50 🪙", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .weight(0.85f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E193C))
                                        .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newGold = shopItem.priceGold + 50
                                            val updated = Item(
                                                shopItem.id, shopItem.name, shopItem.slot, shopItem.rarity,
                                                newGold, shopItem.priceGems, shopItem.bonusStr, shopItem.bonusEnd,
                                                shopItem.bonusPre, shopItem.bonusLuck, shopItem.description, shopItem.icon
                                            )
                                            vm.saveAdminItem(updated)
                                            itemsList = vm.getAdminItems()
                                            showToastMessage = "Đã tăng giá ${shopItem.name} lên $newGold Vàng!"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+50 🪙", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .weight(1.1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(C.Orange)
                                        .clickable {
                                            isCreatingNewShopItem = false
                                            editingShopItem = shopItem
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✏️ Sửa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .weight(0.8f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val deleted = vm.deleteAdminItem(shopItem.id)
                                            if (deleted) {
                                                itemsList = vm.getAdminItems()
                                                showToastMessage = "Đã xóa vật phẩm '${shopItem.name}' khỏi Cửa hàng!"
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🗑️ Xóa", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ==================== TAB 3: QUÁI VẬT & BOSS ====================
            else if (selectedTab == "MONSTERS") {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("QUẢN LÝ QUÁI VẬT & BOSS (${monstersList.size})", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Chỉnh HP & phần thưởng", color = C.Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                items(monstersList) { monster ->
                    VCard {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF221A3E))
                                        .border(1.dp, Color(0xFF3B2F63), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(monster.avatar, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(monster.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (monster.isBoss) {
                                            Spacer(Modifier.width(6.dp))
                                            StatBadge("BOSS", C.Pink)
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "Chương ${monster.stageId} • ${monster.exercise.name}",
                                        color = C.Muted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "✨ +${monster.expReward} EXP  •  🪙 +${monster.goldReward} Vàng",
                                        color = Color(0xFFFFC94D),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("❤️ MÁU (HP)", color = Color(0xFFFCA5A5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("${monster.maxHp}", color = Color(0xFFFF6B6B), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF252047)))
                            Spacer(Modifier.height(10.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E193C))
                                        .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newHp = maxOf(100, monster.maxHp - 100)
                                            val updated = Monster(monster.stageId, monster.name, monster.title, monster.avatar, newHp, monster.exercise, monster.isBoss, monster.expReward, monster.goldReward, monster.lore, monster.dropRarity, monster.colorHex)
                                            monstersList = monstersList.map { if (it.name == monster.name) updated else it }
                                            vm.saveAdminMonster(updated)
                                            showToastMessage = "Đã giảm 100 HP cho ${monster.name} (còn $newHp HP)"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-100 HP", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E193C))
                                        .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newHp = monster.maxHp + 200
                                            val updated = Monster(monster.stageId, monster.name, monster.title, monster.avatar, newHp, monster.exercise, monster.isBoss, monster.expReward, monster.goldReward, monster.lore, monster.dropRarity, monster.colorHex)
                                            monstersList = monstersList.map { if (it.name == monster.name) updated else it }
                                            vm.saveAdminMonster(updated)
                                            showToastMessage = "Đã tăng 200 HP cho ${monster.name} (lên $newHp HP)"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+200 HP", color = C.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .weight(1.3f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(C.Orange)
                                        .clickable { editingMonster = monster },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✏️", fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sửa Chi Tiết", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== TAB 3: CẤU HÌNH GÓC AI ====================
            else if (selectedTab == "EXERCISES") {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NGƯỠNG GÓC KHỚP AI MEDIAPIPE (${exerciseConfigs.size})", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Độ chuẩn & Sát thương", color = C.Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                items(exerciseConfigs) { ex ->
                    VCard {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(ex.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                StatBadge(ex.code, C.Blue)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminMetricChip(Modifier.weight(1f), "Góc duỗi chuẩn", "${ex.extendAngle}°", C.Blue)
                                AdminMetricChip(Modifier.weight(1f), "Góc gập chuẩn", "${ex.contractAngle}°", C.Pink)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminMetricChip(Modifier.weight(1f), "Sát thương/Rep", "${ex.damageFactor}x", C.Yellow)
                                AdminMetricChip(Modifier.weight(1f), "Calo tiêu hao", "${ex.caloPerRep} kcal", C.Green)
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.Orange.copy(alpha = 0.15f))
                                    .border(1.dp, C.Orange, RoundedCornerShape(10.dp))
                                    .clickable { editingExercise = ex },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚙️ Hiệu Chỉnh Góc Khớp & Calo", color = C.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== TAB 4: GIAN LẬN (ANTI-CHEAT) ====================
            else if (selectedTab == "CHEATS") {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CẢNH BÁO BẤT THƯỜNG / GIAN LẬN", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Kiểm duyệt trực tiếp", color = C.Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                items(cheatAlerts) { alert ->
                    VCard(border = if (alert.isResolved) C.Border else Color(0xFFEF4444).copy(alpha = 0.6f)) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (alert.isResolved) "✅ ${alert.username}" else "🚨 ${alert.username}",
                                        color = if (alert.isResolved) Color(0xFF6EE7B7) else Color(0xFFFF6B6B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    StatBadge(alert.exercise, C.Blue)
                                }
                                if (alert.isResolved) {
                                    StatBadge("ĐÃ XỬ LÝ", C.Green)
                                } else {
                                    StatBadge("NGUY CƠ CAO", Color(0xFFEF4444))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Hành vi: Tập ${alert.reps} reps trong vỏn vẹn ${alert.durationSeconds} giây",
                                color = C.Text,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Phân tích AI: ${alert.reason}",
                                color = C.Muted,
                                fontSize = 11.sp
                            )

                            if (!alert.isResolved) {
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF252047)))
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1E193C))
                                            .border(1.dp, Color(0xFF382F63), RoundedCornerShape(10.dp))
                                            .clickable {
                                                cheatAlerts = cheatAlerts.map { if (it.id == alert.id) it.copy(isResolved = true) else it }
                                                showToastMessage = "Đã bỏ qua cảnh báo cho ${alert.username}."
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Bỏ Qua", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        Modifier
                                            .weight(1.5f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEF4444))
                                            .clickable {
                                                val targetUser = usersList.firstOrNull { it.username.equals(alert.username, ignoreCase = true) }
                                                if (targetUser != null) {
                                                    vm.setAdminUserLock(targetUser.id, targetUser.username, true, alert.reason)
                                                } else {
                                                    vm.resolveAdminCheat(alert.username, alert.reason)
                                                }
                                                cheatAlerts = cheatAlerts.map { if (it.id == alert.id) it.copy(isResolved = true) else it }
                                                usersList = vm.getAdminUsers()
                                                showToastMessage = "Đã khóa vĩnh viễn tài khoản ${alert.username} do gian lận!"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🚨 Khóa Nick Ngay", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. TOAST FEEDBACK NỔI BẬT ---
        showToastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                showToastMessage = null
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1740))
                    .border(1.dp, C.Orange.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // --- 6. DIALOG 1: CẤP QUYỀN ADMIN THEO TÊN NGƯỜI CHƠI TRONG CSDL ---
        if (showingGrantAdminDialog) {
            var targetUsername by remember { mutableStateOf("") }
            var chosenRole by remember { mutableStateOf("Admin") }

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0),
                focusedBorderColor = C.Orange,
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = C.Orange,
                unfocusedLabelColor = C.Muted,
                cursorColor = C.Orange
            )

            AlertDialog(
                onDismissRequest = { showingGrantAdminDialog = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF1B1530),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Cấp Quyền Admin / Phân Role", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Nhập chính xác tên tài khoản người chơi cần thăng cấp hoặc đổi vai trò:", color = C.Muted, fontSize = 12.sp)

                        OutlinedTextField(
                            value = targetUsername,
                            onValueChange = { targetUsername = it },
                            label = { Text("Tên tài khoản (Username)") },
                            placeholder = { Text("Nhập nick người chơi...") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Gợi ý từ các tài khoản thực tế đang có trong CSDL (trừ Hachimi)
                        val otherUsers = usersList.filter { !it.username.equals("Hachimi", ignoreCase = true) }
                        if (otherUsers.isNotEmpty()) {
                            Text("Hoặc chạm chọn nhanh từ tài khoản CSDL:", color = C.Muted, fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                otherUsers.take(3).forEach { u ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (targetUsername == u.username) C.Orange.copy(alpha = 0.25f) else Color(0xFF221A3E))
                                            .border(1.dp, if (targetUsername == u.username) C.Orange else Color(0xFF3B2F63), RoundedCornerShape(8.dp))
                                            .clickable { targetUsername = u.username }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(u.username, color = if (targetUsername == u.username) C.Orange else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Text("Chọn vai trò mới:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        val roles = listOf(
                            Triple("Admin", "👑 ADMIN", "Quản trị viên toàn quyền hệ thống"),
                            Triple("User", "🛡️ USER", "Người chơi tập luyện bình thường"),
                            Triple("Moderator", "⚔️ MODERATOR", "Kiểm duyệt viên & Giám sát AI")
                        )

                        roles.forEach { (roleKey, roleLabel, roleDesc) ->
                            val isSelected = chosenRole == roleKey
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) C.Orange.copy(alpha = 0.15f) else Color(0xFF141029))
                                    .border(1.dp, if (isSelected) C.Orange else Color(0xFF2E2657), RoundedCornerShape(10.dp))
                                    .clickable { chosenRole = roleKey }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(roleLabel, color = if (isSelected) C.Orange else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(roleDesc, color = C.Muted, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Text("✓", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = targetUsername.trim()
                            if (trimmed.length < 3) {
                                showToastMessage = "Tên tài khoản phải có ít nhất 3 ký tự!"
                            } else {
                                val success = vm.setAdminUserRoleByUsername(trimmed, chosenRole)
                                if (success) {
                                    usersList = vm.getAdminUsers()
                                    showToastMessage = "Đã cập nhật vai trò của '$trimmed' thành $chosenRole!"
                                    showingGrantAdminDialog = false
                                } else {
                                    showToastMessage = "Không thể cập nhật vai trò cho '$trimmed'!"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Orange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("XÁC NHẬN CẤP QUYỀN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showingGrantAdminDialog = false }) {
                        Text("HỦY", color = C.Muted)
                    }
                }
            )
        }

        // --- 7. DIALOG 2: ĐỔI VAI TRÒ CHO NGƯỜI CHƠI CỤ THỂ ---
        editingRoleUser?.let { targetUser ->
            var chosenRole by remember(targetUser) { mutableStateOf(targetUser.role) }

            AlertDialog(
                onDismissRequest = { editingRoleUser = null },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF1B1530),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(targetUser.avatar, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Đổi Vai Trò: ${targetUser.username}", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Chọn vai trò mới bạn muốn gán cho ${targetUser.username}:", color = C.Muted, fontSize = 12.sp)

                        val roles = listOf(
                            Triple("Admin", "👑 ADMIN", "Quản trị viên toàn quyền hệ thống"),
                            Triple("User", "🛡️ USER", "Người chơi tập luyện bình thường"),
                            Triple("Moderator", "⚔️ MODERATOR", "Kiểm duyệt viên & Giám sát AI")
                        )

                        roles.forEach { (roleKey, roleLabel, roleDesc) ->
                            val isSelected = chosenRole.equals(roleKey, ignoreCase = true)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) C.Orange.copy(alpha = 0.15f) else Color(0xFF141029))
                                    .border(1.dp, if (isSelected) C.Orange else Color(0xFF2E2657), RoundedCornerShape(10.dp))
                                    .clickable { chosenRole = roleKey }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(roleLabel, color = if (isSelected) C.Orange else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(roleDesc, color = C.Muted, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Text("✓", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.setAdminUserRole(targetUser.id, targetUser.username, chosenRole)
                            usersList = vm.getAdminUsers()
                            showToastMessage = "Đã cập nhật role của ${targetUser.username} thành $chosenRole!"
                            editingRoleUser = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Orange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ÁP DỤNG VAI TRÒ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingRoleUser = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                }
            )
        }

        // --- 8. DIALOG CHỈNH SỬA QUÁI VẬT ---
        editingMonster?.let { monster ->
            var hpText by remember(monster) { mutableStateOf(monster.maxHp.toString()) }
            var expText by remember(monster) { mutableStateOf(monster.expReward.toString()) }
            var goldText by remember(monster) { mutableStateOf(monster.goldReward.toString()) }

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0),
                focusedBorderColor = C.Orange,
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = C.Orange,
                unfocusedLabelColor = C.Muted,
                cursorColor = C.Orange
            )

            AlertDialog(
                onDismissRequest = { editingMonster = null },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF1B1530),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(monster.avatar, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Hiệu Chỉnh: ${monster.name}", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = hpText,
                            onValueChange = { hpText = it },
                            label = { Text("Máu tối đa (HP)") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = expText,
                            onValueChange = { expText = it },
                            label = { Text("Thưởng EXP") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = goldText,
                            onValueChange = { goldText = it },
                            label = { Text("Thưởng Vàng (Gold)") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
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
                        colors = ButtonDefaults.buttonColors(containerColor = C.Orange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("LƯU THÔNG SỐ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMonster = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                }
            )
        }

        // --- 9. DIALOG CHỈNH SỬA GÓC KHỚP AI ---
        editingExercise?.let { ex ->
            var extendText by remember(ex) { mutableStateOf(ex.extendAngle.toString()) }
            var contractText by remember(ex) { mutableStateOf(ex.contractAngle.toString()) }
            var damageText by remember(ex) { mutableStateOf(ex.damageFactor.toString()) }
            var caloText by remember(ex) { mutableStateOf(ex.caloPerRep.toString()) }

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0),
                focusedBorderColor = C.Blue,
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = C.Blue,
                unfocusedLabelColor = C.Muted,
                cursorColor = C.Blue
            )

            AlertDialog(
                onDismissRequest = { editingExercise = null },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF1B1530),
                title = {
                    Text("📐 Hiệu Chỉnh Ngưỡng: ${ex.name}", color = C.Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = extendText,
                            onValueChange = { extendText = it },
                            label = { Text("Góc duỗi chuẩn (°)") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contractText,
                            onValueChange = { contractText = it },
                            label = { Text("Góc gập chuẩn (°)") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = damageText,
                            onValueChange = { damageText = it },
                            label = { Text("Hệ số sát thương/Rep") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = caloText,
                            onValueChange = { caloText = it },
                            label = { Text("Calo tiêu hao mỗi rep (kcal)") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
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
                        colors = ButtonDefaults.buttonColors(containerColor = C.Blue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("LƯU CẤU HÌNH GÓC", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingExercise = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                }
            )
        }

        // --- 10. DIALOG THÊM MỚI / CHỈNH SỬA VẬT PHẨM CỬA HÀNG ---
        editingShopItem?.let { currentItem ->
            var itemName by remember(currentItem) { mutableStateOf(currentItem.name) }
            var itemIcon by remember(currentItem) { mutableStateOf(currentItem.icon) }
            var itemSlot by remember(currentItem) { mutableStateOf(currentItem.slot) }
            var itemRarity by remember(currentItem) { mutableStateOf(currentItem.rarity) }
            var priceGoldText by remember(currentItem) { mutableStateOf(currentItem.priceGold.toString()) }
            var priceGemsText by remember(currentItem) { mutableStateOf(currentItem.priceGems.toString()) }
            var strText by remember(currentItem) { mutableStateOf(currentItem.bonusStr.toString()) }
            var endText by remember(currentItem) { mutableStateOf(currentItem.bonusEnd.toString()) }
            var preText by remember(currentItem) { mutableStateOf(currentItem.bonusPre.toString()) }
            var luckText by remember(currentItem) { mutableStateOf(currentItem.bonusLuck.toString()) }
            var descText by remember(currentItem) { mutableStateOf(currentItem.description) }

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0),
                focusedBorderColor = C.Orange,
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = C.Orange,
                unfocusedLabelColor = C.Muted,
                cursorColor = C.Orange
            )

            AlertDialog(
                onDismissRequest = { editingShopItem = null },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF1B1530),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(itemIcon.ifBlank { "🛒" }, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isCreatingNewShopItem) "Thêm Vật Phẩm Cửa Hàng" else "Sửa Vật Phẩm: ${currentItem.name}",
                            color = C.Orange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Tên vật phẩm") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1.6f)
                            )
                            OutlinedTextField(
                                value = itemIcon,
                                onValueChange = { itemIcon = it },
                                label = { Text("Icon") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        // Chọn nhanh loại trang bị (ItemSlot)
                        Text("Loại trang bị (Slot):", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ItemSlot.entries.toList()) { slot ->
                                val sel = itemSlot == slot
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) C.Orange else Color(0xFF141029))
                                        .border(1.dp, if (sel) C.Orange else Color(0xFF2E2657), RoundedCornerShape(8.dp))
                                        .clickable {
                                            itemSlot = slot
                                            if (isCreatingNewShopItem) {
                                                itemIcon = slot.icon
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text("${slot.icon} ${slot.label}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Chọn nhanh Phẩm cấp (Rarity)
                        Text("Phẩm cấp (Rarity):", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(Rarity.entries.toList()) { rar ->
                                val sel = itemRarity == rar
                                val rColor = Color(rar.colorHex)
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) rColor.copy(alpha = 0.28f) else Color(0xFF141029))
                                        .border(1.5.dp, if (sel) rColor else Color(0xFF2E2657), RoundedCornerShape(8.dp))
                                        .clickable { itemRarity = rar }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(rar.label, color = if (sel) rColor else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Giá Vàng & Giá Kim Cương
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = priceGoldText,
                                onValueChange = { priceGoldText = it },
                                label = { Text("Giá Vàng (🪙)") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = priceGemsText,
                                onValueChange = { priceGemsText = it },
                                label = { Text("Giá Ngọc (💎)") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 4 Chỉ số cộng thêm (STR, END, PRE, LUCK)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = strText,
                                onValueChange = { strText = it },
                                label = { Text("💪 +STR") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endText,
                                onValueChange = { endText = it },
                                label = { Text("🛡️ +END") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = preText,
                                onValueChange = { preText = it },
                                label = { Text("🎯 +PRE") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = luckText,
                                onValueChange = { luckText = it },
                                label = { Text("🍀 +LUCK") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = descText,
                            onValueChange = { descText = it },
                            label = { Text("Mô tả vật phẩm") },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanName = itemName.trim().ifEmpty { "Vật Phẩm Mới" }
                            val cleanIcon = itemIcon.trim().ifEmpty { itemSlot.icon }
                            val newGold = (priceGoldText.toIntOrNull() ?: currentItem.priceGold).coerceAtLeast(0)
                            val newGems = (priceGemsText.toIntOrNull() ?: currentItem.priceGems).coerceAtLeast(0)
                            val newStr = (strText.toIntOrNull() ?: currentItem.bonusStr).coerceAtLeast(0)
                            val newEnd = (endText.toIntOrNull() ?: currentItem.bonusEnd).coerceAtLeast(0)
                            val newPre = (preText.toIntOrNull() ?: currentItem.bonusPre).coerceAtLeast(0)
                            val newLuck = (luckText.toIntOrNull() ?: currentItem.bonusLuck).coerceAtLeast(0)

                            val savedItem = Item(
                                currentItem.id,
                                cleanName,
                                itemSlot,
                                itemRarity,
                                newGold,
                                newGems,
                                newStr,
                                newEnd,
                                newPre,
                                newLuck,
                                descText.trim(),
                                cleanIcon
                            )
                            vm.saveAdminItem(savedItem)
                            itemsList = vm.getAdminItems()
                            showToastMessage = if (isCreatingNewShopItem) {
                                "Đã thêm vật phẩm '$cleanName' vào Cửa hàng!"
                            } else {
                                "Đã cập nhật vật phẩm '$cleanName' trong Cửa hàng!"
                            }
                            editingShopItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Orange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (isCreatingNewShopItem) "THÊM VÀO CỬA HÀNG" else "LƯU VẬT PHẨM",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingShopItem = null }) {
                        Text("HỦY", color = C.Muted)
                    }
                }
            )
        }
    }
}

@Composable
private fun AdminKpiCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16122E))
            .border(1.dp, Color(0xFF26204E), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AdminMetricChip(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141029))
            .border(1.dp, Color(0xFF26204E), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(label, color = C.Muted, fontSize = 9.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            .navigationBarsPadding()
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
