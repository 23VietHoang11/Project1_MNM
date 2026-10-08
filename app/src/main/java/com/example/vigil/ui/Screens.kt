package com.example.vigil.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vigil.data.GameViewModel
import com.example.vigil.model.AdventureData
import com.example.vigil.model.Monster
import com.example.vigil.model.Chapter
import com.example.vigil.model.FriendProfile
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.LeaderboardEntry
import com.example.vigil.model.Rarity
import com.example.vigil.model.WorkoutReward
import com.example.vigil.pose.Exercise
import java.time.DayOfWeek

// ====================== HÔM NAY ======================
@Composable
fun TodayScreen(vm: GameViewModel, nav: NavController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("CHÀO BUỔI TỐI", color = C.Muted, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(vm.name, color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("🪙 ${vm.gold}")
                Chip("◆ ${vm.gems}", C.Blue)
                Chip("📜", C.Text)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cấp ${vm.level} · ${vm.title}", color = C.Muted, fontWeight = FontWeight.Bold)
            Text("${vm.xp} / ${vm.xpNeeded} XP", color = C.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(vm.xp / vm.xpNeeded.toFloat(), C.Yellow)
        Spacer(Modifier.height(14.dp))


        if (vm.streak > 0 && vm.todayReps == 0) {
            Spacer(Modifier.height(14.dp))
            VCard(border = Color(0xFF3A2430)) {
                Text("Chuỗi ${vm.streak} ngày sắp đứt", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Tập hôm nay đi, không thì mất chuỗi.", color = C.Muted)
            }
        }

        // Trận tiếp theo (Liên kết thẳng tới quái vật ải hiện tại)
        val currentMonster = remember(vm.stage) { AdventureData.getMonsterByStage(vm.stage) }
        Spacer(Modifier.height(14.dp))
        VCard {
            Text("ẢI HIỆN TẠI · CHẶNG ${vm.stage}", color = C.Pink, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentMonster.avatar, fontSize = 38.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(currentMonster.name, color = C.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(currentMonster.title, color = C.Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Quote("“${currentMonster.lore}”")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("${currentMonster.maxHp} HP · ${currentMonster.exercise.label}", C.Text)
                Chip("🪙 +${currentMonster.goldReward}")
                Chip("★ +${currentMonster.expReward} XP")
            }
            Spacer(Modifier.height(12.dp))
            BigButton("⚔️ Khiêu chiến ngay") {
                nav.navigate("workout/${currentMonster.exercise.name}/${currentMonster.isBoss}/${currentMonster.stageId}")
            }
        }



        SectionLabel("Luyện tập tự do")
        val list = listOf(
            Exercise.PUSHUP to "Tập tự do · Không thưởng", Exercise.SQUAT to "Tập tự do · Không thưởng",
            Exercise.PLANK to "Tập tự do · Không thưởng", Exercise.SITUP to "Tập tự do · Không thưởng",
        )
        list.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (ex, sub) ->
                    Box(Modifier.weight(1f)) {
                        VCard(onClick = { nav.navigate("workout/${ex.name}") }) {
                            Text("📷 ${ex.label}", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(sub, color = C.Muted, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

// ====================== PHIÊU LƯU: BẢN ĐỒ CHIẾN DỊCH RPG ======================
@Composable
fun AdventureScreen(vm: GameViewModel, nav: NavController) {
    var selectedChapterId by remember { mutableIntStateOf(1) }
    val chapters = AdventureData.CHAPTERS
    val currentChapter = chapters.firstOrNull { it.id == selectedChapterId } ?: chapters[0]

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Tiêu đề
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CHIẾN DỊCH THÁM HIỂM", color = C.Muted, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Bản Đồ Thế Giới", color = C.Text, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Chip("Chặng ${vm.stage}", C.Yellow)
        }


        Spacer(Modifier.height(14.dp))

        // Thanh chọn Chương / Vùng đất (Chapter Tabs)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chapters.forEach { ch ->
                val isSelected = ch.id == selectedChapterId
                val isUnlocked = vm.stage >= (ch.id - 1) * 3 + 1
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) C.Orange else C.Card)
                        .border(1.dp, if (isSelected) C.Orange else C.Border, RoundedCornerShape(14.dp))
                        .clickable { selectedChapterId = ch.id }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ch.icon, fontSize = 20.sp)
                        Text(
                            ch.name.split(" ").take(2).joinToString(" "),
                            color = if (isSelected) Color.White else C.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Banner thông tin Vùng Đất đang chọn
        VCard(border = Color(0xFF3F3B66)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentChapter.icon, fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(currentChapter.name, color = C.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(currentChapter.description, color = C.Muted, fontSize = 12.sp)
                }
            }
        }

        SectionLabel("Các ải quái vật (${currentChapter.monsters.size} Ải)")

        // Danh sách các Ải Quái Vật / Boss trong Chương
        currentChapter.monsters.forEach { monster ->
            val isCompleted = monster.stageId < vm.stage
            val isCurrent = monster.stageId == vm.stage
            val isLocked = monster.stageId > vm.stage

            val borderColor = when {
                isCompleted -> C.Green
                isCurrent -> if (monster.isBoss) C.Yellow else C.Pink
                else -> C.Border
            }

            VCard(
                modifier = Modifier.padding(bottom = 12.dp),
                border = borderColor
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar quái
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isLocked) Color(0xFF141224) else Color(0xFF0F0D22))
                            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked) {
                            Text("🔒", fontSize = 24.sp)
                        } else {
                            Text(monster.avatar, fontSize = 34.sp)
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ẢI ${monster.stageId}: ${monster.name}",
                                color = if (isLocked) C.Muted else C.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (monster.isBoss) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "BOSS",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(C.Yellow)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(monster.title, color = C.Muted, fontSize = 12.sp)

                        Spacer(Modifier.height(4.dp))

                        // Thông tin bài tập và máu quái
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip("${monster.maxHp} HP", if (isLocked) C.Muted else C.Pink)
                            Chip(monster.exercise.label, if (isLocked) C.Muted else C.Blue)
                            Chip("★ +${monster.expReward} XP", if (isLocked) C.Muted else C.Yellow)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(monster.lore, color = C.Muted, fontSize = 12.sp)

                Spacer(Modifier.height(12.dp))

                when {
                    isCompleted -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓ ĐÃ CHINH PHỤC", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                "Đấu lại (Cày đồ)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF263345))
                                    .clickable {
                                        nav.navigate("workout/${monster.exercise.name}/${monster.isBoss}/${monster.stageId}")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    isCurrent -> {
                        BigButton(
                            if (monster.isBoss) "👑 KHIÊU CHIẾN TRÙM" else "⚔️ XUẤT KÍCH CHIẾN ĐẤU",
                            if (monster.isBoss) C.Yellow else C.Orange,
                            if (monster.isBoss) Color.Black else Color.White
                        ) {
                            nav.navigate("workout/${monster.exercise.name}/${monster.isBoss}/${monster.stageId}")
                        }
                    }
                    else -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141224))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔒 Vượt ải trước để mở khóa", color = C.Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}


// ====================== SẢNH / BANG HỘI ======================
@Composable
fun GuildScreen(vm: GameViewModel) {
    var tab by remember { mutableIntStateOf(1) } // Mặc định mở tab Thế Giới
    var friendNameInput by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Sảnh & Bang Hội", color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Bang hội", "Thế giới", "Bạn bè").forEachIndexed { i, t ->
                Text(
                    t,
                    color = if (tab == i) C.Text else C.Muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (tab == i) C.Card else Color.Transparent)
                        .clickable { tab = i }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        when (tab) {
            0 -> {
                // BANG HỘI (Guilds)
                Spacer(Modifier.height(12.dp))
                VCard {
                    Text("HẦU HẾT CÁC SẢNH ĐÃ ĐẦY", color = C.Yellow, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Lập sảnh riêng là đặc quyền Vigil — bạn cầm trịch, và bạn quyết ai được vào.", color = C.Muted)
                    Spacer(Modifier.height(10.dp))
                    BigButton("👑 Dẫn dắt bang hội của riêng bạn", C.Yellow, Color.Black) {}
                }
                Spacer(Modifier.height(12.dp))
                listOf(
                    Triple("Taiwan Fitness 🇹🇼", "5/10", "Pushing for improve..."),
                    Triple("Werkwerkwerkwe...", "4/10", "My joints ache"),
                    Triple("Bellpieces", "7/10", "Bunch of silly geezas...")
                ).forEach { (n, c, d) ->
                    VCard(Modifier.padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$c · chỉ qua lời mời", color = C.Muted, fontSize = 13.sp)
                                Quote(d)
                            }
                            Text("Xin vào", color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(C.Purple).padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            1 -> {
                // THẾ GIỚI - BẢNG XẾP HẠNG (World Leaderboard)
                Spacer(Modifier.height(8.dp))

                // Bộ lọc xếp hạng theo chỉ số (Multi-metric sorting)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val metrics = listOf(
                        Triple("CP", "⚡ Lực Chiến", C.Orange),
                        Triple("LEVEL", "👑 Cấp Độ", C.Yellow),
                        Triple("REPS", "🏋️ Tổng Reps", C.Green),
                        Triple("STREAK", "🔥 Chuỗi Ngày", C.Pink)
                    )
                    metrics.forEach { (key, label, accentColor) ->
                        val isSelected = vm.leaderboardSortBy == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else C.Card)
                                .border(1.dp, if (isSelected) accentColor else C.Border, RoundedCornerShape(14.dp))
                                .clickable { vm.setLeaderboardFilter(key) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) accentColor else C.Muted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Thẻ tóm tắt vị trí xếp hạng của người dùng hiện tại
                val myRankEntry = vm.leaderboardList.firstOrNull { it.isCurrentUser }
                if (myRankEntry != null) {
                    VCard(border = C.Purple) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF2C2450)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(myRankEntry.avatar, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(myRankEntry.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "BẠN",
                                            color = C.Yellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(C.Yellow.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text("Hạng hiện tại: #${myRankEntry.rank}", color = C.Muted, fontSize = 12.sp)
                                }
                            }
                            Text(
                                when (vm.leaderboardSortBy) {
                                    "LEVEL" -> "Cấp ${myRankEntry.level}"
                                    "REPS" -> "${myRankEntry.totalReps} rep"
                                    "STREAK" -> "${myRankEntry.streak} ngày"
                                    else -> "${myRankEntry.combatPower} CP"
                                },
                                color = C.Yellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Top 3 Podium (Bục vinh quang)
                if (vm.leaderboardList.size >= 3) {
                    val top1 = vm.leaderboardList.getOrNull(0)
                    val top2 = vm.leaderboardList.getOrNull(1)
                    val top3 = vm.leaderboardList.getOrNull(2)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Top 2 (Bạc)
                        if (top2 != null) {
                            LeaderboardPodiumCard(
                                modifier = Modifier.weight(1f),
                                entry = top2,
                                rankBadge = "🥈",
                                borderColor = Color(0xFFC0C0C0),
                                sortBy = vm.leaderboardSortBy
                            )
                        }
                        // Top 1 (Vàng)
                        if (top1 != null) {
                            LeaderboardPodiumCard(
                                modifier = Modifier.weight(1.15f),
                                entry = top1,
                                rankBadge = "👑",
                                borderColor = Color(0xFFFFD700),
                                sortBy = vm.leaderboardSortBy,
                                isTop1 = true
                            )
                        }
                        // Top 3 (Đồng)
                        if (top3 != null) {
                            LeaderboardPodiumCard(
                                modifier = Modifier.weight(1f),
                                entry = top3,
                                rankBadge = "🥉",
                                borderColor = Color(0xFFCD7F32),
                                sortBy = vm.leaderboardSortBy
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("Bảng xếp hạng toàn cầu")

                // Danh sách người chơi
                vm.leaderboardList.forEach { entry ->
                    VCard(
                        Modifier.padding(bottom = 8.dp),
                        border = if (entry.isCurrentUser) C.Purple else C.Border
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Số thứ tự xếp hạng
                            Box(
                                modifier = Modifier.width(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (entry.rank) {
                                        1 -> "🥇"
                                        2 -> "🥈"
                                        3 -> "🥉"
                                        else -> "#${entry.rank}"
                                    },
                                    color = if (entry.rank <= 3) Color.White else C.Muted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (entry.rank <= 3) 18.sp else 14.sp
                                )
                            }

                            // Avatar
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF242044)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.avatar, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(10.dp))

                            // Tên & Chi tiết
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        entry.username,
                                        color = C.Text,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (entry.isCurrentUser) {
                                        Text(" (Bạn)", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Cấp ${entry.level} • ${entry.title}", color = C.Muted, fontSize = 12.sp)
                            }

                            // Chỉ số theo bộ lọc
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    when (vm.leaderboardSortBy) {
                                        "LEVEL" -> "Cấp ${entry.level}"
                                        "REPS" -> "${entry.totalReps} rep"
                                        "STREAK" -> "${entry.streak} ngày"
                                        else -> "${entry.combatPower} CP"
                                    },
                                    color = when (vm.leaderboardSortBy) {
                                        "LEVEL" -> C.Yellow
                                        "REPS" -> C.Green
                                        "STREAK" -> C.Pink
                                        else -> C.Orange
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                // Nút kết bạn nhanh
                                if (!entry.isCurrentUser) {
                                    if (entry.isFriend) {
                                        Text(
                                            "✓ Bạn bè",
                                            color = C.Purple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(C.Orange.copy(alpha = 0.2f))
                                                .border(1.dp, C.Orange, RoundedCornerShape(8.dp))
                                                .clickable { vm.addFriend(entry.username) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                "+ Kết bạn",
                                                color = C.Orange,
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
            }
            else -> {
                // BẠN BÈ (Friends system)
                Spacer(Modifier.height(8.dp))

                // Ô nhập để kết bạn bằng tên
                VCard {
                    Text("KẾT BẠN BẰNG TÊN NHÂN VẬT", color = C.Muted, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = friendNameInput,
                            onValueChange = { friendNameInput = it },
                            placeholder = { Text("Nhập tên (VD: ShadowBlade...)", color = C.Muted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = C.Text,
                                unfocusedTextColor = C.Text,
                                focusedBorderColor = C.Orange,
                                unfocusedBorderColor = C.Border,
                                focusedContainerColor = Color(0xFF100E26),
                                unfocusedContainerColor = Color(0xFF100E26)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(C.Orange)
                                .clickable {
                                    if (friendNameInput.isNotBlank()) {
                                        val target = friendNameInput.trim()
                                        if (vm.addFriend(target)) {
                                            friendNameInput = ""
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("➕ Thêm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("Danh sách bạn bè (${vm.friendsList.size})")

                if (vm.friendsList.isEmpty()) {
                    VCard {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", fontSize = 38.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có bạn bè nào", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Nhập tên tài khoản ở trên hoặc kết bạn từ mục Bảng Xếp Hạng Thế Giới để cùng nhau so tài!",
                                color = C.Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    vm.friendsList.forEach { friend ->
                        VCard(Modifier.padding(bottom = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF2C2450)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(friend.avatar, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(friend.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Cấp ${friend.level} • ${friend.title}", color = C.Muted, fontSize = 12.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("⚡ ${friend.combatPower} CP", color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("🏋️ ${friend.totalReps} rep", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("🔥 ${friend.streak} ngày", color = C.Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF221F45))
                                        .border(1.dp, C.Purple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { vm.toastMessage = "Đã gửi lời cổ vũ tới ${friend.username}! 💪 Hăng hái lên nào!" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡ Cổ vũ", color = C.Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF301C24))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { vm.removeFriend(friend.id, friend.username) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Hủy bạn", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                BigButton("＋ Mời bạn bè ngoài đời vào app", C.Card, C.Orange) {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Tập cùng mình trên Vigil nhé! Cùng leo bảng xếp hạng thế giới! 💪")
                    }
                    ctx.startActivity(Intent.createChooser(i, "Mời bạn bè"))
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun LeaderboardPodiumCard(
    modifier: Modifier = Modifier,
    entry: LeaderboardEntry,
    rankBadge: String,
    borderColor: Color,
    sortBy: String,
    isTop1: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(C.Card)
            .border(if (isTop1) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(vertical = if (isTop1) 16.dp else 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(rankBadge, fontSize = if (isTop1) 26.sp else 20.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(if (isTop1) 48.dp else 40.dp).clip(CircleShape).background(Color(0xFF26214B)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.avatar, fontSize = if (isTop1) 26.sp else 22.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.username,
            color = C.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            when (sortBy) {
                "LEVEL" -> "Lv.${entry.level}"
                "REPS" -> "${entry.totalReps} rep"
                "STREAK" -> "${entry.streak}d"
                else -> "${entry.combatPower} CP"
            },
            color = borderColor,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
    }
}

// ====================== ANH HÙNG ======================
@Composable
fun HeroScreen(vm: GameViewModel, nav: NavController) {
    var selectedSlotToEquip by remember { mutableStateOf<ItemSlot?>(null) }
    var inspectingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("RARITY") }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Đồ Giám của bạn", color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2A66)), contentAlignment = Alignment.Center) {
                    Text(vm.avatar.ifEmpty { "🧑‍🎤" }, fontSize = 44.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(vm.name, color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("${vm.totalReps} rep tích lũy", color = C.Muted)
                    Chip("◎ ${vm.title}", C.Purple)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1C28))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { vm.logout() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Đăng xuất", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CẤP ${vm.level} → ${vm.level + 1}", color = C.Muted, fontSize = 13.sp)
                Text("${vm.xp} / ${vm.xpNeeded} XP", color = C.Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp)); ProgressBar(vm.xp / vm.xpNeeded.toFloat(), C.Yellow)
        }

        SectionLabel("7 ngày qua")
        VCard {
            Text("${vm.weekReps()} rep", color = C.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            val days = vm.last7Days()
            val max = (days.maxOf { it.second }).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                days.forEach { (d, r) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(26.dp).height((4 + 50 * r / max.toFloat()).dp).clip(RoundedCornerShape(3.dp)).background(if (r > 0) C.Orange else C.Border))
                        Text(when (d.dayOfWeek) {
                            DayOfWeek.MONDAY -> "T2"; DayOfWeek.TUESDAY -> "T3"; DayOfWeek.WEDNESDAY -> "T4"
                            DayOfWeek.THURSDAY -> "T5"; DayOfWeek.FRIDAY -> "T6"; DayOfWeek.SATURDAY -> "T7"; else -> "CN"
                        }, color = C.Muted, fontSize = 11.sp)
                    }
                }
            }
        }



        SectionLabel("Chỉ số chiến đấu")
        VCard {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("⚔️ Sát Thương Mỗi Rep", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Tỉ lệ bạo kích: ${(vm.critRate * 100).toInt()}% (x${String.format(java.util.Locale.US, "%.1f", vm.critMultiplier)})", color = C.Muted, fontSize = 12.sp)
                }
                Text("${vm.attackDamage} DMG", color = C.Orange, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Sức Mạnh", Pair(vm.totalStr, vm.bonusStr), C.Orange),
                Triple("Sức Bền", Pair(vm.totalEnd, vm.bonusEnd), C.Green),
                Triple("Chính Xác", Pair(vm.totalPre, vm.bonusPre), C.Blue),
                Triple("Vận May (Tăng Rớt Đồ)", Pair(vm.totalLuck, vm.bonusLuck), C.Yellow)
            ).forEach { (n, v, c) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(n, color = C.Text, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${v.first}", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (v.second > 0) {
                            Text(" (+${v.second} đồ)", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                ProgressBar(v.first / 100f, c)
                Spacer(Modifier.height(14.dp))
            }
        }

        // ====================== HỆ THỐNG TRANG BỊ RPG ======================
        SectionLabel("Trang bị nhân vật")
        VCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cột trái: 🪖 Mũ giáp, 🥋 Giáp ngực, 🥊 Găng tay
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(ItemSlot.HELMET, ItemSlot.ARMOR, ItemSlot.GLOVES).forEach { slot ->
                        EquipmentSlotBox(
                            slot = slot,
                            equipped = vm.equippedMap[slot],
                            onClick = {
                                val eq = vm.equippedMap[slot]
                                if (eq != null) inspectingItem = eq
                                else selectedSlotToEquip = slot
                            }
                        )
                    }
                }

                // Cột giữa: Bệ Tướng + Lực Chiến + Nút Thao Tác Nhanh
                Column(
                    Modifier.weight(1f).padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF4A2574), Color(0xFF1E1742), Color(0xFF0F0D22))
                                )
                            )
                            .border(2.5.dp, Brush.linearGradient(listOf(C.Orange, C.Purple, C.Yellow)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧑‍🎤", fontSize = 48.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Lực chiến (Combat Power) Badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF3B154D), Color(0xFF6B1D8C), Color(0xFF3B154D))))
                            .border(1.dp, C.Yellow.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡ LỰC CHIẾN", color = C.Yellow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Text("${vm.combatPower}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("+${vm.equipmentCombatPower} từ Đồ", color = C.Muted, fontSize = 9.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Thao tác nhanh: Mặc nhanh / Tháo hết
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Orange)
                                .clickable { vm.quickEquip() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("⚡ Mặc nhanh", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2B1425))
                                .border(1.dp, C.Pink.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable { vm.unequipAll() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Tháo hết", color = C.Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Cột phải: ⚔️ Vũ khí, 🪬 Bùa chú, 👟 Giày
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(ItemSlot.WEAPON, ItemSlot.AMULET, ItemSlot.BOOTS).forEach { slot ->
                        EquipmentSlotBox(
                            slot = slot,
                            equipped = vm.equippedMap[slot],
                            onClick = {
                                val eq = vm.equippedMap[slot]
                                if (eq != null) inspectingItem = eq
                                else selectedSlotToEquip = slot
                            }
                        )
                    }
                }
            }
        }

        // ====================== TÚI ĐỒ RPG DẠNG LƯỚI ======================
        SectionLabel("Túi đồ của bạn (${vm.inventory.size})")

        // Bộ lọc phân loại vật phẩm (Category Filter Chips)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "Tất cả",
                "WEAPON" to "⚔️ Vũ khí",
                "ARMOR" to "🥋 Giáp",
                "HELMET" to "🪖 Mũ",
                "GLOVES" to "🥊 Găng",
                "BOOTS" to "👟 Giày",
                "AMULET" to "🪬 Bùa"
            ).forEach { (key, label) ->
                val isSelected = selectedCategory == key
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) C.Orange else Color(0xFF141228))
                        .border(1.dp, if (isSelected) C.Orange else C.Border, RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = key }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.White else C.Muted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Lọc và Sắp xếp danh sách
        val filteredInventory = remember(vm.inventory, selectedCategory, sortBy) {
            val list = when (selectedCategory) {
                "WEAPON" -> vm.inventory.filter { it.item.slot == ItemSlot.WEAPON }
                "ARMOR" -> vm.inventory.filter { it.item.slot == ItemSlot.ARMOR }
                "HELMET" -> vm.inventory.filter { it.item.slot == ItemSlot.HELMET }
                "GLOVES" -> vm.inventory.filter { it.item.slot == ItemSlot.GLOVES }
                "BOOTS" -> vm.inventory.filter { it.item.slot == ItemSlot.BOOTS }
                "AMULET" -> vm.inventory.filter { it.item.slot == ItemSlot.AMULET }
                else -> vm.inventory
            }
            when (sortBy) {
                "CP" -> list.sortedByDescending { it.item.combatPower }
                "EQUIPPED" -> list.sortedWith(compareByDescending<InventoryItem> { it.isEquipped }.thenByDescending { it.item.combatPower })
                else -> list.sortedWith(compareByDescending<InventoryItem> { it.item.rarity.ordinal }.thenByDescending { it.item.combatPower })
            }
        }

        // Thanh tùy chọn sắp xếp & đếm số lượng
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filteredInventory.size} món đồ", color = C.Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "RARITY" to "⭐ Phẩm cấp",
                    "CP" to "⚡ Lực chiến",
                    "EQUIPPED" to "👑 Đang mặc"
                ).forEach { (sortKey, sortLabel) ->
                    val isCurrent = sortBy == sortKey
                    Text(
                        sortLabel,
                        color = if (isCurrent) C.Yellow else C.Muted,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) Color(0xFF262014) else Color.Transparent)
                            .clickable { sortBy = sortKey }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Lưới đồ (4 cột)
        if (filteredInventory.isEmpty()) {
            VCard {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎒", fontSize = 38.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Không có trang bị nào trong mục này", color = C.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Chip("🛒 Ghé Cửa Hàng Mua Đồ", C.Yellow) { nav.navigate("shop") }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredInventory.chunked(4).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { invItem ->
                            Box(Modifier.weight(1f)) {
                                InventoryGridSlot(
                                    invItem = invItem,
                                    onClick = { inspectingItem = invItem }
                                )
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    } // End Column

    // ====================== DIALOG SO SÁNH & THAO TÁC CHI TIẾT ======================
    if (inspectingItem != null) {
        val currentInv = inspectingItem!!
        val item = currentInv.item
        val equippedSameSlot = vm.equippedMap[item.slot]
        val isCurrentlyEquipped = currentInv.isEquipped

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable { inspectingItem = null },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(C.Card)
                    .border(2.dp, item.rarity.color, RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {}
                    .padding(18.dp)
            ) {
                // Header: Item Icon + Name + Rarity + Slot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.rarity.color.copy(alpha = 0.15f))
                            .border(2.dp, item.rarity.color, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 34.sp)
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = C.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Chip(item.rarity.label, item.rarity.color)
                            Chip(item.slot.label, C.Blue)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // So sánh chỉ số Lực Chiến
                if (isCurrentlyEquipped) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF132B20))
                            .border(1.dp, C.Green, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👑 ĐANG TRANG BỊ TRÊN NGƯỜI", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("+${item.combatPower} CP", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                } else if (equippedSameSlot != null) {
                    val oldItem = equippedSameSlot.item
                    val cpDiff = item.combatPower - oldItem.combatPower
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (cpDiff >= 0) Color(0xFF162B1D) else Color(0xFF2E1720))
                            .border(1.dp, if (cpDiff >= 0) C.Green else C.Pink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "So với: ${oldItem.name}",
                                    color = C.Muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (cpDiff > 0) "▲ Tăng +$cpDiff Lực Chiến"
                                    else if (cpDiff < 0) "▼ Giảm ${-cpDiff} Lực Chiến"
                                    else "= Ngang bằng Lực Chiến",
                                    color = if (cpDiff >= 0) C.Green else C.Pink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text("⚡ ${item.combatPower} CP", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF262040))
                            .border(1.dp, C.Purple, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ Ô ${item.slot.label} đang trống!", color = C.Purple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("+${item.combatPower} CP", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Chi tiết các thuộc tính (STR, END, PRE, LUCK)
                val oldItem = if (!isCurrentlyEquipped) equippedSameSlot?.item else null
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F0D22))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCompareRow("💪 Sức Mạnh (STR)", item.bonusStr, oldItem?.bonusStr, C.Orange)
                    StatCompareRow("🛡️ Sức Bền (END)", item.bonusEnd, oldItem?.bonusEnd, C.Green)
                    StatCompareRow("🎯 Chính Xác (PRE)", item.bonusPre, oldItem?.bonusPre, C.Blue)
                    StatCompareRow("🍀 Vận May (LUCK)", item.bonusLuck, oldItem?.bonusLuck, C.Yellow)
                }

                Spacer(Modifier.height(10.dp))

                // Cốt truyện / Mô tả
                if (item.description.isNotEmpty()) {
                    Text(
                        "\"${item.description}\"",
                        color = C.Muted,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Thao tác nút bấm
                if (isCurrentlyEquipped) {
                    BigButton("Tháo Món Này Ra", C.Pink) {
                        vm.unequipItem(item.slot)
                        inspectingItem = null
                    }
                    Spacer(Modifier.height(8.dp))
                    BigButton("Đóng", C.Border, C.Text) { inspectingItem = null }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2A1C20))
                                .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                                .clickable {
                                    vm.sellItem(currentInv)
                                    inspectingItem = null
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bán +${item.sellPriceGold} 🪙", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(C.Orange)
                                .clickable {
                                    vm.equipItem(currentInv)
                                    inspectingItem = null
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (equippedSameSlot != null) "Thay Thế ⚔️" else "Trang Bị ⚔️",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BigButton("Đóng", C.Border, C.Text) { inspectingItem = null }
                }
            }
        }
    }

    // ====================== DIALOG CHỌN TRANG BỊ CHO SLOT TRỐNG ======================
    if (selectedSlotToEquip != null) {
        val targetSlot = selectedSlotToEquip!!
        val candidates = vm.inventory
            .filter { it.item.slot == targetSlot }
            .sortedByDescending { it.item.combatPower }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable { selectedSlotToEquip = null },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(C.Card)
                    .border(1.5.dp, C.Border, RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {}
                    .padding(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(targetSlot.icon, fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Chọn ${targetSlot.label}", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("${candidates.size} món có sẵn", color = C.Muted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(14.dp))

                if (candidates.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F0D22))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chưa có món ${targetSlot.label} nào trong kho!", color = C.Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Chip("🛒 Mua Tại Cửa Hàng", C.Yellow) {
                                selectedSlotToEquip = null
                                nav.navigate("shop")
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidates.forEach { cand ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F0D22))
                                    .border(1.dp, cand.item.rarity.color.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        vm.equipItem(cand)
                                        selectedSlotToEquip = null
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cand.item.rarity.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cand.item.icon, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cand.item.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(cand.item.rarity.label, color = cand.item.rarity.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("⚡ +${cand.item.combatPower} CP", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    "Mặc ⚔️",
                                    color = C.Orange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(C.Orange.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                BigButton("Đóng", C.Border, C.Text) { selectedSlotToEquip = null }
            }
        }
    }
    } // End Box
}

// ====================== CÁC COMPOSABLE HỖ TRỢ TRANG BỊ & TÚI ĐỒ RPG ======================
@Composable
fun EquipmentSlotBox(
    slot: ItemSlot,
    equipped: InventoryItem?,
    onClick: () -> Unit
) {
    val borderColor = if (equipped != null) equipped.item.rarity.color else C.Border
    val bgColor = if (equipped != null) equipped.item.rarity.color.copy(alpha = 0.12f) else Color(0xFF0F0D22)

    Box(
        Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (equipped != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (equipped != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(equipped.item.icon, fontSize = 26.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    equipped.item.name,
                    color = equipped.item.rarity.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(equipped.item.rarity.color)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(slot.icon, fontSize = 22.sp, modifier = Modifier.alpha(0.35f))
                Spacer(Modifier.height(2.dp))
                Text(slot.label, color = C.Muted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                "+",
                color = C.Muted.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp)
            )
        }
    }
}

@Composable
fun InventoryGridSlot(
    invItem: InventoryItem,
    onClick: () -> Unit
) {
    val item = invItem.item
    val borderColor = if (invItem.isEquipped) C.Green else item.rarity.color
    val bgColor = if (invItem.isEquipped) Color(0xFF13231D) else Color(0xFF110E24)

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                if (invItem.isEquipped) 2.dp else 1.2.dp,
                borderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(item.icon, fontSize = 28.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                item.name,
                color = item.rarity.color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                "⚡+${item.combatPower}",
                color = C.Yellow,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (invItem.isEquipped) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(C.Green)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("E", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun StatCompareRow(
    label: String,
    newVal: Int,
    oldVal: Int?,
    color: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = C.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+$newVal", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (oldVal != null) {
                val diff = newVal - oldVal
                if (diff > 0) {
                    Text(" (▲ +$diff)", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp))
                } else if (diff < 0) {
                    Text(" (▼ $diff)", color = C.Pink, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp))
                } else {
                    Text(" (=)", color = C.Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}


// ====================== THỬ THÁCH ======================
@Composable
fun ChallengesScreen(vm: GameViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    data class Q(val name: String, val quote: String, val cur: Int, val goal: Int, val xp: Int, val gold: Int, val gem: Int = 0)
    val daily = listOf(
        Q("Tay Vững", "“Bốn mươi rep, chiến binh — cứ từ từ.”", vm.todayReps, 40, 100, 13),
        Q("Máu Đầu Tiên", "“Hạ một kẻ thù trong ngày.”", if (vm.stage > 1) 1 else 0, 1, 50, 10),
        Q("Bất Hoại", "“Trăm rep trong một ngày — huyền thoại.”", vm.todayReps, 100, 520, 55)
    )
    val weekly = listOf(
        Q("Vigil Trường Kỳ", "“200 rep trong cả tuần.”", vm.weekReps(), 200, 300, 60),
        Q("Không Ngơi Nghỉ", "“Hạ mười hai kẻ thù trong tuần này.”", 0, 12, 480, 90, 1),
        Q("Ý Chí Bất Khuất", "“500 rep trong bảy ngày. Một huyền thoại.”", vm.weekReps(), 500, 900, 150, 2)
    )
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            Text("‹", color = C.Text, fontSize = 32.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.Card).clickable { onBack() }.padding(horizontal = 16.dp, vertical = 4.dp))
            Spacer(Modifier.width(14.dp))
            Text("Thử thách của bạn", color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Hằng ngày", "Hằng tuần", "Trùm").forEachIndexed { i, t ->
                Text(t, color = if (tab == i) C.Text else C.Muted, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (tab == i) C.Card else Color.Transparent)
                        .clickable { tab = i }.padding(horizontal = 24.dp, vertical = 10.dp))
            }
        }
        val list = when (tab) { 0 -> daily; 1 -> weekly; else -> emptyList() }
        if (tab == 2) Quote("Thử thách trùm sẽ mở khi bạn đạt cấp cao hơn.")
        list.forEach { q ->
            VCard(Modifier.padding(bottom = 12.dp)) {
                Text(q.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Quote(q.quote)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ProgressBar(q.cur / q.goal.toFloat(), C.Orange) }
                    Text("  ${q.cur.coerceAtMost(q.goal)}/${q.goal}", color = C.Muted)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("★ +${q.xp} XP"); Chip("🪙 +${q.gold}"); if (q.gem > 0) Chip("◆ +${q.gem}", C.Blue)
                }
            }
        }
    }
}

// ====================== DIALOG RỚT CHIẾN LỢI PHẨM ======================
@Composable
fun LootDropDialog(reward: WorkoutReward, vm: GameViewModel, onDismiss: () -> Unit) {
    val dropped = reward.droppedItem

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(2.dp, if (dropped != null) dropped.rarity.color else C.Yellow, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (dropped != null) "🎉 CHIẾN THẮNG & RỚT ĐỒ!" else "🎉 HOÀN THÀNH BÀI TẬP!", color = C.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Bạn đã hoàn thành ${reward.score} rep kiên cường", color = C.Muted, fontSize = 13.sp)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("★ +${reward.xpEarned} XP", C.Yellow)
                Chip("🪙 +${reward.goldEarned} Vàng", C.Orange)
                Chip("🔥 Chuỗi: ${reward.newStreak} ngày", C.Pink)
            }

            Spacer(Modifier.height(18.dp))

            if (dropped != null) {
                Text("✨ CHIẾN LỢI PHẨM RỚT RA ✨", color = dropped.rarity.color, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0D22))
                        .border(1.5.dp, dropped.rarity.color, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dropped.icon, fontSize = 48.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(dropped.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(dropped.rarity.label, color = dropped.rarity.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(dropped.description, color = C.Muted, fontSize = 12.sp)

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (dropped.bonusStr > 0) StatBadge("+${dropped.bonusStr} STR", C.Orange)
                        if (dropped.bonusEnd > 0) StatBadge("+${dropped.bonusEnd} END", C.Green)
                        if (dropped.bonusPre > 0) StatBadge("+${dropped.bonusPre} PRE", C.Blue)
                        if (dropped.bonusLuck > 0) StatBadge("+${dropped.bonusLuck} LUCK", C.Yellow)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        BigButton("Vào kho", C.Border, C.Text) { onDismiss() }
                    }
                    Box(Modifier.weight(1.2f)) {
                        BigButton("Trang bị ngay", C.Orange, Color.White) {
                            val inv = vm.inventory.firstOrNull { it.item.id == dropped.id }
                            if (inv != null) vm.equipItem(inv)
                            onDismiss()
                        }
                    }
                }
            } else {
                Text(
                    "Lần này chưa rớt vật phẩm. Hãy tăng chỉ số Vận May hoặc đánh Boss ở chế độ Phiêu lưu để có tỷ lệ rớt lên đến 95%!",
                    color = C.Muted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                BigButton("Tuyệt vời", C.Orange) { onDismiss() }
            }
        }
    }
}

