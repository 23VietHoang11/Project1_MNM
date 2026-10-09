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
import com.example.vigil.model.DailyCheckInReward
import com.example.vigil.model.FriendProfile
import com.example.vigil.model.FriendRequestEntry
import com.example.vigil.model.Guild
import com.example.vigil.model.GuildInvitationEntry
import com.example.vigil.model.GuildMember
import com.example.vigil.model.InventoryItem
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.LeaderboardEntry
import com.example.vigil.model.OutgoingGuildInvitation
import com.example.vigil.model.QuestItem
import com.example.vigil.model.Rarity
import com.example.vigil.model.GuildBoss
import com.example.vigil.model.GuildBossContribution
import com.example.vigil.model.GuildBossInfo
import com.example.vigil.model.WorkoutHistoryEntry
import com.example.vigil.model.WorkoutReward
import com.example.vigil.model.WorkoutSummaryStats
import com.example.vigil.pose.Exercise
import java.time.DayOfWeek
import java.util.Locale

// ====================== HÔM NAY ======================
@Composable
fun TodayScreen(vm: GameViewModel, nav: NavController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("HELLO!", color = C.Muted, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(vm.name, color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("🪙 ${vm.gold}")
                Chip("◆ ${vm.gems}", C.Blue)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (vm.canClaimCheckInToday) Color(0xFF382A10) else Color(0xFF221E36))
                        .border(1.5.dp, if (vm.canClaimCheckInToday) C.Yellow else C.Border, RoundedCornerShape(12.dp))
                        .clickable { vm.showQuestScrollDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📜", fontSize = 16.sp)
                        if (vm.canClaimCheckInToday) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(C.Yellow)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cấp ${vm.level} · ${vm.title}", color = C.Muted, fontWeight = FontWeight.Bold)
            Text("${vm.xp} / ${vm.xpNeeded} XP", color = C.Muted, fontSize = 13.sp)
        }




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
fun GuildScreen(vm: GameViewModel, nav: NavController? = null) {
    var tab by remember { mutableIntStateOf(0) }
    var friendNameInput by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadGuildBoss()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Sảnh & Bang Hội", color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Bang hội", "Boss Bang", "Thế giới", "Bạn bè").forEachIndexed { i, t ->
                Text(
                    t,
                    color = if (tab == i) C.Text else C.Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (tab == i) C.Card else Color.Transparent)
                        .clickable { tab = i }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        when (tab) {
            0 -> {
                // BANG HỘI (Guilds)
                Spacer(Modifier.height(12.dp))

                val myGuild = vm.userGuild
                if (myGuild != null) {
                    // Hiển thị bang hội hiện tại của người chơi
                    VCard(border = C.Yellow) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2C2450)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(myGuild.badge, fontSize = 32.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(myGuild.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Spacer(Modifier.width(6.dp))
                                    if (myGuild.isUserLeader) {
                                        Text(
                                            "CHỦ BANG",
                                            color = C.Yellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(C.Yellow.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Cấp ${myGuild.level} • Chủ bang: ${myGuild.leaderName}", color = C.Muted, fontSize = 12.sp)
                                if (myGuild.slogan.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Quote(myGuild.slogan)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Chip("👥 ${myGuild.memberCount}/30 Thành viên", C.Purple)
                            Chip("🏋️ ${myGuild.totalReps} Tổng Reps", C.Green)
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(C.Orange)
                                    .clickable { vm.showInviteMemberDialog = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✉️ Mời hảo hữu", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF331C26))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { vm.leaveGuild() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(if (myGuild.isUserLeader) "Giải tán bang" else "Rời bang", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Banner Boss Thế Giới Bang Hội (Truy cập nhanh)
                        val bossBanner = vm.guildBossInfo?.boss
                        Spacer(Modifier.height(14.dp))
                        VCard(
                            border = if (bossBanner != null && bossBanner.isDefeated) C.Green else Color(0xFFEF4444),
                            modifier = Modifier.clickable { tab = 1 }
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF38151E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(bossBanner?.avatar ?: "🐉", fontSize = 28.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(bossBanner?.name ?: "Hắc Long Viễn Cổ - Nidhogg", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.width(6.dp))
                                        if (bossBanner != null && bossBanner.isDefeated) {
                                            Text("🏆 ĐÃ HẠ", color = C.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("🔥 BOSS THẾ GIỚI", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (bossBanner != null) {
                                        Text("${bossBanner.currentHp}/${bossBanner.maxHp} HP (${String.format(Locale.US, "%.1f%%", bossBanner.hpRatio * 100)})", color = C.Muted, fontSize = 12.sp)
                                    } else {
                                        Text("Cùng bang hội tập luyện săn Boss nhận đồ Thần Thoại!", color = C.Muted, fontSize = 12.sp)
                                    }
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (bossBanner != null && bossBanner.isDefeated) C.Green else Color(0xFFEF4444))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(if (bossBanner != null && bossBanner.isDefeated) "Xem quà" else "Vào Săn", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Danh sách đơn xin gia nhập bang hội cần phê duyệt (Dành cho Chủ Bang)
                        if (myGuild.isUserLeader && vm.pendingGuildApplications.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            SectionLabel("📋 Đơn xin gia nhập bang (${vm.pendingGuildApplications.size})")
                            vm.pendingGuildApplications.forEach { app ->
                                VCard(border = C.Yellow, modifier = Modifier.padding(bottom = 8.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2C2450)), contentAlignment = Alignment.Center) {
                                            Text(app.avatar, fontSize = 22.sp)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(app.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Cấp ${app.level} • ${app.totalReps} Reps", color = C.Muted, fontSize = 12.sp)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(C.Green)
                                                    .clickable { vm.approveGuildApplication(app.id) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("✓ Duyệt", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF301C24))
                                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable { vm.rejectGuildApplication(app.id) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("✕ Từ chối", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        SectionLabel("Thành viên bang hội (${vm.guildMembers.size})")

                    vm.guildMembers.forEach { member ->
                        VCard(Modifier.padding(bottom = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF26214B)), contentAlignment = Alignment.Center) {
                                    Text(member.avatar, fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(member.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (member.role == "LEADER") {
                                            Spacer(Modifier.width(6.dp))
                                            Text("👑 Chủ bang", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("Cấp ${member.level} • ${member.totalReps} reps", color = C.Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (vm.outgoingGuildInvitations.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        SectionLabel("Lời mời đã gửi (${vm.outgoingGuildInvitations.size})")
                        vm.outgoingGuildInvitations.forEach { inv ->
                            VCard(Modifier.padding(bottom = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF26214B)), contentAlignment = Alignment.Center) {
                                        Text(inv.inviteeAvatar, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(inv.inviteeName, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Cấp ${inv.inviteeLevel} • Đang chờ chấp nhận...", color = C.Yellow, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF301C24))
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { vm.cancelGuildInvitation(inv.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Thu hồi", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Lời mời gia nhập bang hội gửi tới người chơi (Pending)
                    if (vm.incomingGuildInvitations.isNotEmpty()) {
                        SectionLabel("Lời mời gia nhập bang hội (${vm.incomingGuildInvitations.size})")
                        vm.incomingGuildInvitations.forEach { inv ->
                            VCard(border = C.Yellow, modifier = Modifier.padding(bottom = 10.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF2C2450)), contentAlignment = Alignment.Center) {
                                        Text(inv.guildBadge, fontSize = 28.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(inv.guildName, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Text("Cấp ${inv.guildLevel} • ${inv.guildMemberCount}/30 thành viên", color = C.Muted, fontSize = 12.sp)
                                        Text("Người mời: ${inv.inviterName} ${inv.inviterAvatar}", color = C.Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (inv.guildSlogan.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Quote(inv.guildSlogan)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(C.Green)
                                            .clickable { vm.acceptGuildInvitation(inv.id) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓ Gia nhập", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF301C24))
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .clickable { vm.declineGuildInvitation(inv.id) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✕ Từ chối", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Chưa có bang hội: Nút tạo bang + Danh sách bang để gia nhập
                    VCard {
                        Text("BẠN CHƯA CÓ BANG HỘI", color = C.Yellow, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text("Tạo bang hội của riêng bạn để tập hợp hảo hữu, hoặc gia nhập các bang hội bên dưới!", color = C.Muted)
                        Spacer(Modifier.height(12.dp))
                        BigButton("👑 Thành lập bang hội của riêng bạn", C.Yellow, Color.Black) {
                            vm.showCreateGuildDialog = true
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Danh sách bang hội (${vm.allGuilds.size})")

                    vm.allGuilds.forEach { g ->
                        VCard(Modifier.padding(bottom = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(g.badge, fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(g.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Cấp ${g.level} · ${g.memberCount}/30 thành viên · Chủ: ${g.leaderName}", color = C.Muted, fontSize = 12.sp)
                                    if (g.slogan.isNotBlank()) Quote(g.slogan)
                                }
                                if (g.isHasPendingApplication) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("⏳ Chờ duyệt", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF331C26))
                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .clickable { vm.cancelGuildApplication(g.id) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Hủy đơn", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(C.Purple)
                                            .clickable { vm.applyToGuild(g.id) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("Xin vào", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // BOSS THẾ GIỚI BANG HỘI (Guild World Boss Raid)
                GuildWorldBossTab(vm = vm, nav = nav, onGoToGuildTab = { tab = 0 })
            }
            2 -> {
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
                                    } else if (entry.hasPendingFriendRequest) {
                                        Text(
                                            "⏳ Chờ duyệt",
                                            color = C.Yellow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(C.Orange.copy(alpha = 0.2f))
                                                .border(1.dp, C.Orange, RoundedCornerShape(8.dp))
                                                .clickable { vm.sendFriendRequest(entry.username) }
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

                // Ô nhập để gửi lời mời kết bạn
                VCard {
                    Text("GỬI LỜI MỜI KẾT BẠN", color = C.Muted, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = friendNameInput,
                            onValueChange = { friendNameInput = it },
                            placeholder = { Text("Nhập tên (VD: ValkyrieGym...)", color = C.Muted, fontSize = 13.sp) },
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
                                        if (vm.sendFriendRequest(target)) {
                                            friendNameInput = ""
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✉️ Mời", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // LỜI MỜI KẾT BẠN ĐẾN (Incoming Friend Requests)
                if (vm.incomingFriendRequests.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Lời mời kết bạn (${vm.incomingFriendRequests.size})")

                    vm.incomingFriendRequests.forEach { req ->
                        VCard(border = C.Yellow, modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF2C2450)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(req.senderAvatar, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(req.senderUsername, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Cấp ${req.senderLevel} • ${req.senderTitle}", color = C.Muted, fontSize = 12.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("⚡ ${req.senderCombatPower} CP", color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("🏋️ ${req.senderTotalReps} rep", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        .background(C.Green)
                                        .clickable { vm.acceptFriendRequest(req.id) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓ Chấp nhận", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF301C24))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { vm.declineFriendRequest(req.id) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕ Từ chối", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // LỜI MỜI ĐÃ GỬI (Outgoing Friend Requests)
                if (vm.outgoingFriendRequests.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Lời mời đã gửi (${vm.outgoingFriendRequests.size})")

                    vm.outgoingFriendRequests.forEach { req ->
                        VCard(Modifier.padding(bottom = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF26214B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(req.senderAvatar, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(req.senderUsername, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Cấp ${req.senderLevel} • Đang chờ phản hồi...", color = C.Yellow, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF301C24))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { vm.cancelFriendRequest(req.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Thu hồi", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
                                "Nhập tên tài khoản ở trên hoặc gửi lời mời từ mục Bảng Xếp Hạng Thế Giới để cùng nhau so tài!",
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

                                if (vm.userGuild != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(C.Orange.copy(alpha = 0.2f))
                                            .border(1.dp, C.Orange, RoundedCornerShape(12.dp))
                                            .clickable { vm.inviteToGuild(friend.username) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🛡️ Mời bang", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
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

    if (vm.showCreateGuildDialog) {
        CreateGuildDialog(vm = vm) {
            vm.showCreateGuildDialog = false
        }
    }

    if (vm.showInviteMemberDialog) {
        InviteGuildMemberDialog(vm = vm) {
            vm.showInviteMemberDialog = false
        }
    }

    if (vm.showGuildBossExerciseDialog) {
        GuildBossExerciseDialog(
            onDismiss = { vm.showGuildBossExerciseDialog = false },
            onSelectExercise = { ex ->
                vm.showGuildBossExerciseDialog = false
                nav?.navigate("workout/guild_boss/${ex.name}")
            }
        )
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
    var selectedExerciseFilter by remember { mutableStateOf("ALL") }
    var isHistoryExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Đồ Giám của bạn", color = C.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2A66)), contentAlignment = Alignment.Center) {
                    if (vm.avatar == "🦊" || vm.name.equals("Hachimi", ignoreCase = true)) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.vigil.R.drawable.app_avatar),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(vm.avatar.ifEmpty { "🧑‍🎤" }, fontSize = 44.sp)
                    }
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

        // ====================== LỊCH SỬ RÈN LUYỆN & THỐNG KÊ CHI TIẾT ======================
        SectionLabel("Lịch sử rèn luyện chi tiết")

        // 1. Lưới thống kê tổng hợp (KPI Summary Grid)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tổng buổi tập
            VCard(Modifier.weight(1f).padding(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🏋️", fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${vm.workoutStats.totalWorkouts}", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Buổi tập", color = C.Muted, fontSize = 11.sp)
                }
            }
            // Calo tiêu hao
            VCard(Modifier.weight(1f).padding(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🔥", fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("~${vm.workoutStats.totalCalories}", color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Kcal", color = C.Muted, fontSize = 11.sp)
                }
            }
            // Tổng XP
            VCard(Modifier.weight(1f).padding(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🌟", fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("+${vm.workoutStats.totalXp}", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("XP nhận", color = C.Muted, fontSize = 11.sp)
                }
            }
            // Đồ rớt
            VCard(Modifier.weight(1f).padding(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎁", fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${vm.workoutStats.itemsDroppedCount}", color = C.Purple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Đồ nhặt", color = C.Muted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Biểu đồ tuần
        VCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TỔNG REP TUẦN NÀY", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${vm.weekReps()} rep", color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Chip("🔥 Streak ${vm.streak} ngày", C.Orange)
            }
            Spacer(Modifier.height(12.dp))
            val days = vm.last7Days()
            val max = (days.maxOf { it.second }).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                days.forEach { (d, r) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (r > 0) "$r" else "", color = C.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.width(26.dp).height((4 + 50 * r / max.toFloat()).dp).clip(RoundedCornerShape(4.dp)).background(if (r > 0) C.Orange else C.Border))
                        Spacer(Modifier.height(4.dp))
                        Text(when (d.dayOfWeek) {
                            DayOfWeek.MONDAY -> "T2"; DayOfWeek.TUESDAY -> "T3"; DayOfWeek.WEDNESDAY -> "T4"
                            DayOfWeek.THURSDAY -> "T5"; DayOfWeek.FRIDAY -> "T6"; DayOfWeek.SATURDAY -> "T7"; else -> "CN"
                        }, color = C.Muted, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. Nhật ký chi tiết các buổi tập (Detailed Training Logs)
        VCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NHẬT KÝ CHI TIẾT TỪNG BUỔI", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("${vm.workoutHistory.size} buổi", color = C.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            // Bộ lọc loại bài tập
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "Tất cả",
                    "SQUAT" to "🏋️ Squat",
                    "PUSHUP" to "💪 Push-up",
                    "PLANK" to "🧘 Plank",
                    "JUMPING_JACKS" to "🤸 Bật nhảy"
                ).forEach { (k, label) ->
                    val isSel = selectedExerciseFilter == k
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) C.Orange else Color(0xFF16142B))
                            .border(1.dp, if (isSel) C.Orange else C.Border, RoundedCornerShape(8.dp))
                            .clickable { selectedExerciseFilter = k }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(label, color = if (isSel) Color.White else C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val filteredHistory = if (selectedExerciseFilter == "ALL") {
                vm.workoutHistory
            } else {
                vm.workoutHistory.filter { it.exercise.equals(selectedExerciseFilter, ignoreCase = true) }
            }

            if (filteredHistory.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧘", fontSize = 32.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Chưa có buổi tập nào phù hợp", color = C.Muted, fontSize = 13.sp)
                }
            } else {
                val displayList = if (isHistoryExpanded) filteredHistory else filteredHistory.take(4)

                displayList.forEach { log ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131126))
                            .border(1.dp, Color(0xFF262348), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Dòng tiêu đề buổi tập
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (icon, name) = when (log.exercise.uppercase()) {
                                        "SQUAT" -> "🏋️" to "Squat (Gánh đùi)"
                                        "PUSHUP" -> "💪" to "Push-up (Chống đẩy)"
                                        "PLANK" -> "🧘" to "Plank (Siết cơ lõi)"
                                        "JUMPING_JACKS" -> "🤸" to "Jumping Jacks"
                                        else -> "⚡" to log.exercise
                                    }
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("📅 ${log.createdAt}", color = C.Muted, fontSize = 11.sp)
                            }

                            Spacer(Modifier.height(8.dp))

                            // Chỉ số rep, điểm chuẩn tư thế, calo
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val repText = if (log.exercise.equals("PLANK", ignoreCase = true)) {
                                    "${log.holdSeconds}s giữ"
                                } else {
                                    "${log.reps} rep"
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(C.Orange.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(repText, color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val scoreGrade = when {
                                    log.score >= 90 -> "Xuất sắc 🌟"
                                    log.score >= 75 -> "Rất tốt ✨"
                                    else -> "Đạt chuẩn 👍"
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(C.Green.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${log.score}/100 • $scoreGrade", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF281C38))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("🔥 ~${log.caloriesBurned} kcal", color = Color(0xFFF472B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Phần thưởng nhận được
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("+${log.xpEarned} XP", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("+${log.goldEarned} 🪙 Vàng", color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (log.droppedItem != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(log.droppedItem.rarity.color.copy(alpha = 0.15f))
                                            .border(1.dp, log.droppedItem.rarity.color, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(log.droppedItem.icon, fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(log.droppedItem.name, color = log.droppedItem.rarity.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredHistory.size > 4) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1A38))
                            .clickable { isHistoryExpanded = !isHistoryExpanded }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isHistoryExpanded) "▲ Thu gọn lịch sử" else "▼ Xem thêm ${filteredHistory.size - 4} buổi tập",
                            color = C.Orange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("★ +${reward.xpEarned} XP", C.Yellow)
                Chip("🪙 +${reward.goldEarned} Vàng", C.Orange)
                if (reward.gemsEarned > 0) {
                    Chip("💎 +${reward.gemsEarned} Gem", C.Blue)
                }
                Chip("🔥 Chuỗi: ${reward.newStreak} ngày", C.Pink)
            }

            if (reward.isBossFirstClear) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2A4A))
                        .border(1.dp, C.Blue, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("👑", fontSize = 16.sp)
                        Text(
                            "LẦN ĐẦU HẠ BOSS: Thưởng thêm +${reward.gemsEarned} Gem quý!",
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

// ====================== DIALOG TẠO BANG HỘI ======================
@Composable
fun CreateGuildDialog(
    vm: GameViewModel,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var sloganInput by remember { mutableStateOf("") }
    var selectedBadge by remember { mutableStateOf("🛡️") }
    val badges = listOf("🛡️", "⚔️", "🐉", "🦅", "🐺", "🦁", "🔥", "⚡", "👑", "🏰", "💎", "🌟")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(1.5.dp, C.Yellow, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Thành Lập Bang Hội", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "✕",
                    color = C.Muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Chọn Huy Hiệu
            Text("Chọn biểu tượng bang:", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { b ->
                    val isSelected = selectedBadge == b
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) C.Yellow.copy(alpha = 0.2f) else Color(0xFF13102A))
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) C.Yellow else C.Border, RoundedCornerShape(12.dp))
                            .clickable { selectedBadge = b },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(b, fontSize = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Tên Bang Hội
            Text("Tên bang hội:", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = { Text("VD: Long Thần Hội, Hắc Báo...", color = C.Muted, fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = C.Text,
                    unfocusedTextColor = C.Text,
                    focusedBorderColor = C.Yellow,
                    unfocusedBorderColor = C.Border,
                    focusedContainerColor = Color(0xFF100E26),
                    unfocusedContainerColor = Color(0xFF100E26)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Khẩu hiệu / Tuyên ngôn
            Text("Khẩu hiệu bang hội:", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = sloganInput,
                onValueChange = { sloganInput = it },
                placeholder = { Text("VD: Cùng nhau rèn luyện, chinh phục đỉnh cao!", color = C.Muted, fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = C.Text,
                    unfocusedTextColor = C.Text,
                    focusedBorderColor = C.Yellow,
                    unfocusedBorderColor = C.Border,
                    focusedContainerColor = Color(0xFF100E26),
                    unfocusedContainerColor = Color(0xFF100E26)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            BigButton("✨ Xác Nhận Thành Lập", C.Yellow, Color.Black) {
                if (nameInput.isNotBlank()) {
                    val success = vm.createGuild(nameInput.trim(), selectedBadge, sloganInput.trim())
                    if (success) {
                        onDismiss()
                    }
                } else {
                    vm.toastMessage = "Vui lòng nhập tên bang hội!"
                }
            }

            Spacer(Modifier.height(8.dp))
            BigButton("Hủy", C.Border, C.Text) { onDismiss() }
        }
    }
}

// ====================== DIALOG MỜI THÀNH VIÊN VÀO BANG ======================
@Composable
fun InviteGuildMemberDialog(
    vm: GameViewModel,
    onDismiss: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(1.5.dp, C.Orange, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✉️", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Mời Vào Bang Hội", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "✕",
                    color = C.Muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            Text("Nhập tên hiệp sĩ bạn muốn mời gia nhập bang:", color = C.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = { Text("Tên người chơi (VD: DragonFit...)", color = C.Muted, fontSize = 13.sp) },
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Danh sách gợi ý từ hảo hữu
            if (vm.friendsList.isNotEmpty()) {
                Text("Gợi ý từ bạn bè:", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vm.friendsList.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF221F45))
                                .clickable { usernameInput = f.username }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.avatar, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(f.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("Cấp ${f.level} • Chọn", color = C.Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            BigButton("🚀 Gửi lời mời", C.Orange, Color.White) {
                if (usernameInput.isNotBlank()) {
                    vm.inviteToGuild(usernameInput.trim())
                }
            }

            Spacer(Modifier.height(8.dp))
            BigButton("Hủy", C.Border, C.Text) { onDismiss() }
        }
    }
}

// ====================== BOSS THẾ GIỚI BANG HỘI (GUILD WORLD BOSS RAID) ======================
@Composable
fun GuildWorldBossTab(
    vm: GameViewModel,
    nav: NavController?,
    onGoToGuildTab: () -> Unit
) {
    val myGuild = vm.userGuild
    if (myGuild == null) {
        Spacer(Modifier.height(20.dp))
        VCard(border = C.Purple) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🐉", fontSize = 56.sp)
                Spacer(Modifier.height(10.dp))
                Text("SIÊU TRÙM THẾ GIỚI BANG HỘI", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Boss Thế Giới là chiến dịch quy mô lớn dành riêng cho các Bang Hội. Toàn bộ anh em trong bang cùng hợp lực tập luyện (Squat, Hít đất, Gập bụng, Plank) qua camera AI để dồn sát thương hạ gục Boss và nhận trang bị Thần Thoại!",
                    color = C.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.Purple)
                        .clickable { onGoToGuildTab() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("🛡️ Gia nhập Bang Hội ngay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        return
    }

    val bossInfo = vm.guildBossInfo
    val boss = bossInfo?.boss

    if (boss == null) {
        Spacer(Modifier.height(24.dp))
        VCard(border = C.Purple) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚔️", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("Đang tải dữ liệu Boss Bang Hội...", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Purple)
                        .clickable { vm.loadGuildBoss() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Tải lại", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        return
    }

    Spacer(Modifier.height(8.dp))

    // 1. Thẻ Boss chính (Boss Showcase Card)
    VCard(
        border = if (boss.isDefeated) C.Green else Color(0xFFEF4444)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFF5E1325), Color(0xFF1E101A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(boss.avatar, fontSize = 38.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(boss.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(boss.title, color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (boss.isDefeated) {
                            Text(
                                "🏆 ĐÃ TIÊU DIỆT",
                                color = C.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(C.Green.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        } else {
                            Text(
                                "⚔️ ĐANG HOẠT ĐỘNG",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Text(
                                "MÁU KHỦNG: 500K",
                                color = C.Orange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(C.Orange.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Thanh Máu HP Boss (Gradient Health Bar)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Máu Siêu Trùm", color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${boss.currentHp} / ${boss.maxHp} HP (${String.format(Locale.US, "%.1f%%", boss.hpRatio * 100)})",
                    color = if (boss.isDefeated) C.Green else Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF231826))
                    .border(1.dp, Color(0xFF3D2133), RoundedCornerShape(8.dp))
            ) {
                if (boss.hpRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(boss.hpRatio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24))
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Thẻ phần thưởng chiến dịch
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1D1830))
                    .border(1.dp, Color(0xFF382D60), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "🎁 PHẦN THƯỞNG CHIẾN THẮNG HẬU HĨNH",
                    color = C.Yellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF282142))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💰 +${boss.rewardGold} Vàng", color = C.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF282142))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💎 +${boss.rewardGems} Gem", color = Color(0xFF67E8F9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3A1C2C))
                        .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚔️ ${boss.rewardItemName} (Trang Bị Thần Thoại)",
                        color = Color(0xFFF472B6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Nút Thao Tác
            if (!boss.isDefeated) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEA580C))))
                        .clickable { vm.showGuildBossExerciseDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚔️ KHIÊU CHIẾN GÓP DAME (AI CAMERA) ⚔️",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            } else {
                if (!bossInfo.isHasClaimedDefeatReward) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF16A34A), Color(0xFF059669))))
                            .clickable { vm.claimGuildBossReward() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🎁 NHẬN PHẦN THƯỞNG CHIẾN DỊCH NGAY 🎁",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B382B))
                            .border(1.dp, C.Green, RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✅ BẠN ĐÃ NHẬN PHẦN THƯỞNG CHIẾN DỊCH",
                            color = C.Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (myGuild.isUserLeader) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF3B2456))
                            .border(1.dp, C.Purple, RoundedCornerShape(14.dp))
                            .clickable { vm.summonNewGuildBoss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "👑 TRIỆU HỒI BOSS MỚI (DÀNH CHO CHỦ BANG)",
                            color = Color(0xFFD8B4FE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // 2. Thẻ Đóng Góp Của Bản Thân
    VCard(border = C.Purple) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥 CỐNG HIẾN CỦA BẠN", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    if (bossInfo.myRank > 0) "Hạng #${bossInfo.myRank} trong Bang" else "Chưa xếp hạng",
                    color = C.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
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
                        .background(Color(0xFF281F38))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Sát Thương", color = C.Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("${bossInfo.myDamage}", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF281F38))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Tổng Reps", color = C.Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("${bossInfo.myReps}", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF281F38))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Tỉ Lệ Góp", color = C.Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            String.format(Locale.US, "%.1f%%", bossInfo.myPercentage),
                            color = C.Yellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // 3. Bảng Vàng Sát Thương Bang Hội (Guild Raid Leaderboard)
    SectionLabel("Bảng vàng sát thương Bang Hội (${bossInfo.contributors.size})")

    if (bossInfo.contributors.isEmpty()) {
        VCard {
            Text(
                "Chưa có thành viên nào tấn công Boss trong đợt này. Hãy nhấn 'Khiêu chiến góp dame' để mở màn trận chiến!",
                color = C.Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }
    } else {
        bossInfo.contributors.forEach { c ->
            val isMe = c.userId == vm.currentUserId
            VCard(
                Modifier.padding(bottom = 8.dp),
                border = if (isMe) C.Purple else C.Border
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            when (c.rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> "#${c.rank}"
                            },
                            fontSize = if (c.rank <= 3) 18.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (c.rank <= 3) Color.White else C.Muted
                        )
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF26214B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.avatar, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.username, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (isMe) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "BẠN",
                                    color = C.Yellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(C.Yellow.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("Cấp ${c.level} • ${c.reps} reps • ${String.format(Locale.US, "%.1f%%", c.percentage)}", color = C.Muted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${c.damage} DMG", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (c.isHasClaimed) {
                            Text("✓ Đã nhận quà", color = C.Green, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ====================== DIALOG CHỌN BÀI TẬP ĐÁNH BOSS ======================
@Composable
fun GuildBossExerciseDialog(
    onDismiss: () -> Unit,
    onSelectExercise: (Exercise) -> Unit
) {
    val exercises = listOf(
        Triple(Exercise.SQUAT, "Ngồi Xổm (Squat)", "Sát thương x1.0 · Đòn cận chiến vũ bão"),
        Triple(Exercise.PUSHUP, "Hít Đất (Push-up)", "Sát thương x1.2 · Uy lực công phá cao"),
        Triple(Exercise.SITUP, "Gập Bụng (Sit-up)", "Sát thương x1.0 · Tấn công cốt lõi"),
        Triple(Exercise.PLANK, "Plank Thể Lực", "Sát thương theo giây · Chống đỡ tuyệt đối")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐉", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("CHỌN BÀI TẬP SĂN BOSS", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text(
                    "✕",
                    color = C.Muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Mỗi rep chuẩn qua AI Pose Detection sẽ chuyển hóa thành đòn tấn công thực tế lên HP của Boss Bang!",
                color = C.Muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            exercises.forEach { (ex, title, desc) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF261D36))
                        .border(1.dp, Color(0xFF4C3668), RoundedCornerShape(16.dp))
                        .clickable { onSelectExercise(ex) }
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (ex) {
                                Exercise.SQUAT -> "🦵"
                                Exercise.PUSHUP -> "💪"
                                Exercise.SITUP -> "🔥"
                                Exercise.PLANK -> "🛡️"
                                else -> "⚡"
                            },
                            fontSize = 28.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(desc, color = C.Yellow, fontSize = 11.sp)
                        }
                        Text("⚔️ Đấu", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ====================== PHẦN THƯỞNG ĐIỂM DANH HÀNG TUẦN ======================
@Composable
fun WeeklyRewardItemCard(
    reward: DailyCheckInReward,
    onClaim: () -> Unit
) {
    val isSpecial = reward.day == 7
    val borderColor = when {
        reward.isClaimed -> C.Green
        reward.isAvailableToday -> if (isSpecial) C.Yellow else C.Orange
        else -> C.Border
    }
    val bgColor = when {
        reward.isClaimed -> Color(0xFF14241B)
        reward.isAvailableToday -> if (isSpecial) Color(0xFF2E2412) else Color(0xFF261D15)
        else -> Color(0xFF121024)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (reward.isAvailableToday) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                reward.isClaimed -> C.Green.copy(alpha = 0.2f)
                                reward.isAvailableToday -> C.Yellow.copy(alpha = 0.2f)
                                else -> Color(0xFF1E1A38)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when {
                            reward.isClaimed -> "✓"
                            isSpecial -> "👑"
                            reward.gems > 0 -> "💎"
                            else -> "🪙"
                        },
                        fontSize = 20.sp,
                        color = if (reward.isClaimed) C.Green else Color.Unspecified
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Ngày ${reward.day}",
                            color = C.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (isSpecial) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "ĐẶC BIỆT",
                                color = C.Yellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(C.Yellow.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (reward.gold > 0) {
                            Chip("🪙 +${reward.gold}", C.Orange)
                        }
                        if (reward.gems > 0) {
                            Chip("💎 +${reward.gems}", C.Blue)
                        }
                    }
                }
            }

            // Trạng thái / Nút nhận
            when {
                reward.isClaimed -> {
                    Text(
                        "Đã nhận",
                        color = C.Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(C.Green.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                reward.isAvailableToday -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSpecial) C.Yellow else C.Orange)
                            .clickable { onClaim() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nhận ngay 🎁",
                            color = if (isSpecial) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Text(
                        "Chưa mở 🔒",
                        color = C.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B1730))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ====================== THẺ NHIỆM VỤ ======================
@Composable
fun QuestItemCard(
    quest: QuestItem,
    onClaim: () -> Unit
) {
    val canClaim = quest.current >= quest.goal && !quest.isClaimed
    val borderColor = when {
        quest.isClaimed -> C.Green.copy(alpha = 0.5f)
        canClaim -> C.Yellow
        else -> C.Border
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121026))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quest.name,
                    color = C.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (quest.isClaimed) {
                    Text("✓ Đã nhận", color = C.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else if (canClaim) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.Yellow)
                            .clickable { onClaim() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nhận thưởng 🎁", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "${quest.current}/${quest.goal}",
                        color = C.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (quest.quote.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    quest.quote,
                    color = C.Muted,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            val progress = (quest.current.toFloat() / quest.goal.toFloat()).coerceIn(0f, 1f)
            ProgressBar(progress, if (canClaim) C.Yellow else C.Orange)

            Spacer(Modifier.height(10.dp))

            // Rewards
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip("★ +${quest.xp} XP", C.Yellow)
                Chip("🪙 +${quest.gold}", C.Orange)
                if (quest.gems > 0) {
                    Chip("💎 +${quest.gems}", C.Blue)
                }
            }
        }
    }
}

// ====================== DIALOG KINH THƯ ĐIỂM DANH & NHIỆM VỤ ======================
@Composable
fun ScrollQuestDialog(
    vm: GameViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Đăng nhập tuần, 1: Ngày, 2: Tuần, 3: Tháng

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(2.dp, C.Yellow, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            // Header with scroll icon and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📜", fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Kinh Thư Điểm Danh & Nhiệm Vụ",
                            color = C.Text,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            "Tích lũy rep, thu thập Vàng & Gem quý",
                            color = C.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242040))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Tab bar: 4 Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF100E26))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    0 to "🎁 Đăng Nhập",
                    1 to "📅 Ngày",
                    2 to "📆 Tuần",
                    3 to "🏆 Tháng"
                ).forEach { (idx, label) ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) C.Yellow else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.Black else C.Muted,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {


                        vm.weeklyCheckInRewards.forEach { rew ->
                            WeeklyRewardItemCard(
                                reward = rew,
                                onClaim = { vm.claimWeeklyCheckIn() }
                            )
                        }
                    }
                    1 -> {
                        // TAB NHIỆM VỤ NGÀY
                        val dailyQuests = vm.questsList.filter { it.category == "DAILY" }
                        if (dailyQuests.isEmpty()) {
                            Text("Chưa có nhiệm vụ ngày nào.", color = C.Muted, fontSize = 13.sp)
                        } else {
                            dailyQuests.forEach { q ->
                                QuestItemCard(quest = q, onClaim = { vm.claimQuest(q) })
                            }
                        }
                    }
                    2 -> {
                        // TAB NHIỆM VỤ TUẦN
                        val weeklyQuests = vm.questsList.filter { it.category == "WEEKLY" }
                        if (weeklyQuests.isEmpty()) {
                            Text("Chưa có nhiệm vụ tuần nào.", color = C.Muted, fontSize = 13.sp)
                        } else {
                            weeklyQuests.forEach { q ->
                                QuestItemCard(quest = q, onClaim = { vm.claimQuest(q) })
                            }
                        }
                    }
                    3 -> {
                        // TAB NHIỆM VỤ THÁNG
                        val monthlyQuests = vm.questsList.filter { it.category == "MONTHLY" }
                        if (monthlyQuests.isEmpty()) {
                            Text("Chưa có nhiệm vụ tháng nào.", color = C.Muted, fontSize = 13.sp)
                        } else {
                            monthlyQuests.forEach { q ->
                                QuestItemCard(quest = q, onClaim = { vm.claimQuest(q) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nút đóng hoặc nhận nhanh
            if (selectedTab == 0 && vm.canClaimCheckInToday) {
                BigButton("🎁 Nhận Thưởng Hôm Nay Ngay!", C.Yellow, Color.Black) {
                    vm.claimWeeklyCheckIn()
                }
                Spacer(Modifier.height(8.dp))
            }

            BigButton("Đóng Kinh Thư", C.Border, C.Text) {
                onDismiss()
            }
        }
    }
}


