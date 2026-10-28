package com.example.vigil.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vigil.data.GameViewModel
import com.example.vigil.model.PvpBattleMode
import com.example.vigil.model.PvpMatchRecord
import com.example.vigil.model.PvpOpponent
import com.example.vigil.pose.Exercise
import com.example.vigil.pose.RepCounter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.random.Random

private enum class PvpStage {
    LOBBY,
    BATTLE_P1_OR_LIVE,
    TURN_TRANSITION,
    BATTLE_P2,
    RESULT
}

private class PvpPoseFrame(val pose: Pose, val w: Int, val h: Int)

private val PVP_BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
)

@Composable
fun PvpArenaScreen(
    vm: GameViewModel,
    onClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        vm.refreshPvpData()
    }

    val opponents = vm.pvpOpponents
    var selectedOpponent by remember(opponents) {
        mutableStateOf(
            opponents.firstOrNull() ?: PvpOpponent(
                id = 0,
                username = "Người Chơi 2",
                avatar = "🥷",
                level = vm.level,
                attackDamage = vm.attackDamage
            )
        )
    }
    var customPlayer2Name by remember { mutableStateOf("") }
    var battleMode by remember { mutableStateOf(PvpBattleMode.LIVE_SOLO) }
    var selectedExercise by remember { mutableStateOf(Exercise.PUSHUP) }
    var selectedDurationSec by remember { mutableIntStateOf(30) }

    var stage by remember { mutableStateOf(PvpStage.LOBBY) }

    // Điểm số & số Rep của 2 người chơi trong trận
    var p1Reps by remember { mutableIntStateOf(0) }
    var p1Score by remember { mutableIntStateOf(0) }
    var p2Reps by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }

    var matchRecord by remember { mutableStateOf<PvpMatchRecord?>(null) }

    val effectiveP2Name = customPlayer2Name.trim().ifEmpty { selectedOpponent.username }
    val effectiveP2Avatar = if (customPlayer2Name.isNotBlank()) "🥷" else selectedOpponent.avatar

    // Hàm tính điểm mỗi khi hoàn thành 1 rep (100 điểm cơ bản + điểm thưởng liên hoàn & chỉ số Chuẩn Xác)
    fun calcPointsForRep(currentReps: Int, isPlayer1: Boolean): Int {
        val base = 100
        val comboBonus = (currentReps / 3) * 15
        val statBonus = if (isPlayer1) (vm.totalPre * 2) else (selectedOpponent.level * 6)
        return base + comboBonus + statBonus
    }

    when (stage) {
        PvpStage.LOBBY -> {
            PvpLobbyView(
                vm = vm,
                opponents = opponents,
                selectedOpponent = selectedOpponent,
                onSelectOpponent = {
                    selectedOpponent = it
                    customPlayer2Name = ""
                },
                customPlayer2Name = customPlayer2Name,
                onCustomPlayer2NameChange = { customPlayer2Name = it },
                battleMode = battleMode,
                onSelectMode = { battleMode = it },
                selectedExercise = selectedExercise,
                onSelectExercise = { selectedExercise = it },
                selectedDurationSec = selectedDurationSec,
                onSelectDuration = { selectedDurationSec = it },
                onStartBattle = {
                    p1Reps = 0
                    p1Score = 0
                    p2Reps = 0
                    p2Score = 0
                    matchRecord = null
                    stage = PvpStage.BATTLE_P1_OR_LIVE
                },
                onClose = onClose
            )
        }

        PvpStage.BATTLE_P1_OR_LIVE -> {
            PvpActiveBattleView(
                vm = vm,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                battleMode = battleMode,
                isPlayer2Turn = false,
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p2Name = effectiveP2Name,
                p2Avatar = effectiveP2Avatar,
                p2Level = selectedOpponent.level,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Reps = p2Reps,
                p2Score = p2Score,
                onP1Rep = {
                    p1Reps += 1
                    p1Score += calcPointsForRep(p1Reps, isPlayer1 = true)
                },
                onP2Rep = {
                    p2Reps += 1
                    p2Score += calcPointsForRep(p2Reps, isPlayer1 = false)
                },
                onTimeUp = {
                    if (battleMode == PvpBattleMode.PASS_AND_PLAY) {
                        stage = PvpStage.TURN_TRANSITION
                    } else {
                        val rec = vm.finishPvpMatch(
                            player2Name = effectiveP2Name,
                            exercise = selectedExercise,
                            durationSec = selectedDurationSec,
                            p1Reps = p1Reps,
                            p1Score = p1Score,
                            p2Reps = p2Reps,
                            p2Score = p2Score
                        )
                        matchRecord = rec
                        stage = PvpStage.RESULT
                    }
                },
                onCancel = { stage = PvpStage.LOBBY }
            )
        }

        PvpStage.TURN_TRANSITION -> {
            PvpTurnTransitionView(
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Name = effectiveP2Name,
                p2Avatar = effectiveP2Avatar,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                onStartPlayer2Turn = {
                    stage = PvpStage.BATTLE_P2
                },
                onCancel = { stage = PvpStage.LOBBY }
            )
        }

        PvpStage.BATTLE_P2 -> {
            PvpActiveBattleView(
                vm = vm,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                battleMode = battleMode,
                isPlayer2Turn = true,
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p2Name = effectiveP2Name,
                p2Avatar = effectiveP2Avatar,
                p2Level = selectedOpponent.level,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Reps = p2Reps,
                p2Score = p2Score,
                onP1Rep = {
                    p1Reps += 1
                    p1Score += calcPointsForRep(p1Reps, isPlayer1 = true)
                },
                onP2Rep = {
                    p2Reps += 1
                    p2Score += calcPointsForRep(p2Reps, isPlayer1 = false)
                },
                onTimeUp = {
                    val rec = vm.finishPvpMatch(
                        player2Name = effectiveP2Name,
                        exercise = selectedExercise,
                        durationSec = selectedDurationSec,
                        p1Reps = p1Reps,
                        p1Score = p1Score,
                        p2Reps = p2Reps,
                        p2Score = p2Score
                    )
                    matchRecord = rec
                    stage = PvpStage.RESULT
                },
                onCancel = { stage = PvpStage.LOBBY }
            )
        }

        PvpStage.RESULT -> {
            PvpResultView(
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p2Name = effectiveP2Name,
                p2Avatar = effectiveP2Avatar,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Reps = p2Reps,
                p2Score = p2Score,
                record = matchRecord,
                onRematch = {
                    p1Reps = 0
                    p1Score = 0
                    p2Reps = 0
                    p2Score = 0
                    matchRecord = null
                    stage = PvpStage.BATTLE_P1_OR_LIVE
                },
                onBackToLobby = {
                    stage = PvpStage.LOBBY
                },
                onExit = onClose
            )
        }
    }
}

// ============================================================================
// 1. GIAO DIỆN PHÒNG CHỜ & THIẾT LẬP TRẬN SOLO 1v1 (LOBBY)
// ============================================================================
@Composable
private fun PvpLobbyView(
    vm: GameViewModel,
    opponents: List<PvpOpponent>,
    selectedOpponent: PvpOpponent,
    onSelectOpponent: (PvpOpponent) -> Unit,
    customPlayer2Name: String,
    onCustomPlayer2NameChange: (String) -> Unit,
    battleMode: PvpBattleMode,
    onSelectMode: (PvpBattleMode) -> Unit,
    selectedExercise: Exercise,
    onSelectExercise: (Exercise) -> Unit,
    selectedDurationSec: Int,
    onSelectDuration: (Int) -> Unit,
    onStartBattle: () -> Unit,
    onClose: () -> Unit
) {
    val effectiveP2Name = customPlayer2Name.trim().ifEmpty { selectedOpponent.username }
    val effectiveP2Avatar = if (customPlayer2Name.isNotBlank()) "🥷" else selectedOpponent.avatar
    val effectiveP2Level = if (customPlayer2Name.isNotBlank()) vm.level else selectedOpponent.level
    val effectiveP2Cp = if (customPlayer2Name.isNotBlank()) vm.attackDamage else selectedOpponent.attackDamage

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Thanh tiêu đề
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip("← Quay lại Ải", C.Text, onClick = onClose)
            Text(
                "⚔️ ĐẤU TRƯỜNG SOLO 1v1",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Chip("🏆 ${vm.pvpHistory.size} trận", C.Orange)
        }

        Spacer(Modifier.height(14.dp))

        // ======================== THẺ ĐỐI ĐẦU 1 VS 1 ========================
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF28194A), Color(0xFF431638), Color(0xFF1A1735))
                    )
                )
                .border(2.dp, C.Orange, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Người chơi 1
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, C.Blue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(vm.avatar, fontSize = 36.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "NGƯỜI CHƠI 1",
                        color = C.Blue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        vm.name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        "Cấp ${vm.level} · ⚡ ${vm.combatPower} CP",
                        color = C.Muted,
                        fontSize = 11.sp
                    )
                }

                // Huy hiệu VS ở giữa
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(C.Yellow, C.Orange, Color(0xFFD90429))
                                )
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "VS",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${selectedDurationSec}s · ${selectedExercise.label}",
                        color = C.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Người chơi 2
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B1E2E))
                            .border(2.dp, C.Pink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(effectiveP2Avatar, fontSize = 36.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "NGƯỜI CHƠI 2",
                        color = C.Pink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        effectiveP2Name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        "Cấp $effectiveP2Level · ⚡ $effectiveP2Cp CP",
                        color = C.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ======================== CHỌN ĐỐI THỦ (NGƯỜI CHƠI 2) ========================
        VCard {
            Text(
                "1. CHỌN NGƯỜI CHƠI 2 (ĐỐI THỦ SOLO)",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Chọn tài khoản người chơi trong CSDL hoặc nhập tên Người Chơi 2 để 2 bạn cùng thi đấu:",
                color = C.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            if (opponents.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(opponents) { opp ->
                        val isSelected = customPlayer2Name.isBlank() && selectedOpponent.username == opp.username
                        Column(
                            Modifier
                                .width(115.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF35285A) else Color(0xFF181530))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) C.Orange else C.Border,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectOpponent(opp) }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(opp.avatar, fontSize = 26.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                opp.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                "Lv.${opp.level} · ⚔️ ${opp.attackDamage}",
                                color = if (isSelected) C.Yellow else C.Muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = customPlayer2Name,
                onValueChange = onCustomPlayer2NameChange,
                label = { Text("Hoặc nhập tên Người Chơi 2 (Solo trực tiếp 2 người)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Orange,
                    unfocusedBorderColor = C.Border,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = C.Orange,
                    unfocusedLabelColor = C.Muted
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // ======================== CHỌN CHẾ ĐỘ THI ĐẤU 2 NGƯỜI ========================
        VCard {
            Text(
                "2. HÌNH THỨC THI ĐẤU 2 NGƯỜI",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PvpBattleMode.entries.forEach { mode ->
                    val sel = battleMode == mode
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Color(0xFF35285A) else Color(0xFF181530))
                            .border(
                                if (sel) 2.dp else 1.dp,
                                if (sel) C.Orange else C.Border,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(12.dp)
                    ) {
                        Text(
                            mode.label,
                            color = if (sel) C.Yellow else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            mode.desc,
                            color = C.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ======================== CHỌN KHOẢNG THỜI GIAN THI ĐẤU ========================
        VCard {
            Text(
                "3. THỜI GIAN THI ĐẤU TÍNH ĐIỂM",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Trong khoảng thời gian đếm ngược này, người chơi ghi được nhiều điểm hơn sẽ chiến thắng!",
                color = C.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            val durations = listOf(
                15 to "⚡ 15 Giây",
                30 to "🔥 30 Giây",
                60 to "💪 60 Giây",
                90 to "👑 90 Giây"
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { (sec, label) ->
                    val sel = selectedDurationSec == sec
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) C.Orange else Color(0xFF181530))
                            .border(1.dp, if (sel) C.Yellow else C.Border, RoundedCornerShape(12.dp))
                            .clickable { onSelectDuration(sec) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (sel) Color.White else C.Text,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ======================== CHỌN BÀI TẬP SOLO ========================
        VCard {
            Text(
                "4. BÀI TẬP SOLO ĐUA ĐIỂM",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Exercise.entries.forEach { ex ->
                    val sel = selectedExercise == ex
                    val icon = when (ex) {
                        Exercise.PUSHUP -> "💪"
                        Exercise.SQUAT -> "🦵"
                        Exercise.SITUP -> "🔥"
                        Exercise.PLANK -> "🧘"
                        else -> "🏋️"
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Color(0xFF35285A) else Color(0xFF181530))
                            .border(
                                if (sel) 2.dp else 1.dp,
                                if (sel) C.Orange else C.Border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectExercise(ex) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(icon, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ex.label,
                            color = if (sel) C.Yellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Nút bắt đầu trận đấu
        BigButton(
            text = "⚔️ BẮT ĐẦU SOLO (${selectedDurationSec} GIÂY)!",
            color = C.Orange,
            textColor = Color.White,
            onClick = onStartBattle
        )

        Spacer(Modifier.height(18.dp))

        // ======================== LỊCH SỬ ĐẤU TRƯỜNG SOLO PvP ========================
        if (vm.pvpHistory.isNotEmpty()) {
            Text(
                "📜 LỊCH SỬ TRẬN ĐẤU SOLO GẦN ĐÂY",
                color = C.Text,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))

            vm.pvpHistory.take(8).forEach { match ->
                val isWin = match.winnerName.equals(vm.name, ignoreCase = true)
                val isDraw = match.winnerName.equals("HÒA", ignoreCase = true)
                val badgeColor = when {
                    isDraw -> C.Blue
                    isWin -> C.Green
                    else -> C.Pink
                }
                val badgeText = when {
                    isDraw -> "🤝 HÒA"
                    isWin -> "🏆 THẮNG"
                    else -> "💀 THUA"
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "${match.player1Name} (${match.p1Score}đ) vs ${match.player2Name} (${match.p2Score}đ)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${match.exercise} · ${match.durationSec}s · Tỉ số Rep: ${match.p1Reps} - ${match.p2Reps}",
                            color = C.Muted,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        "+${match.goldReward}🪙",
                        color = C.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. MÀN HÌNH CHUYỂN LƯỢT KHI ĐẤU LUÂN PHIÊN 2 NGƯỜI (PASS & PLAY)
// ============================================================================
@Composable
private fun PvpTurnTransitionView(
    p1Name: String,
    p1Avatar: String,
    p1Reps: Int,
    p1Score: Int,
    p2Name: String,
    p2Avatar: String,
    exercise: Exercise,
    durationSec: Int,
    onStartPlayer2Turn: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏱️ HẾT GIỜ LƯỢT 1!", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Spacer(Modifier.height(12.dp))

        VCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(p1Avatar, fontSize = 48.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$p1Name đã hoàn thành phần thi!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$p1Score ĐIỂM ($p1Reps rep / ${durationSec}s)",
                    color = C.Green,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("👇 TIẾP THEO: LƯỢT CỦA NGƯỜI CHƠI 2 👇", color = C.Orange, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))

        VCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(p2Avatar, fontSize = 48.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    p2Name,
                    color = C.Pink,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Text(
                    "Mục tiêu cần vượt qua: > $p1Score điểm (${exercise.label} trong ${durationSec}s)",
                    color = C.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        BigButton("🔥 BẮT ĐẦU LƯỢT CỦA $p2Name (${durationSec}s)", C.Orange, Color.White, onStartPlayer2Turn)
        Spacer(Modifier.height(10.dp))
        BigButton("Hủy trận đấu", C.Card, C.Muted, onCancel)
    }
}

// ============================================================================
// 3. MÀN HÌNH ĐẤU TRƯỜNG TÍNH ĐIỂM TRONG THỜI GIAN NHẤT ĐỊNH (BATTLE HUD)
// ============================================================================
@Composable
private fun PvpActiveBattleView(
    vm: GameViewModel,
    exercise: Exercise,
    durationSec: Int,
    battleMode: PvpBattleMode,
    isPlayer2Turn: Boolean,
    p1Name: String,
    p1Avatar: String,
    p2Name: String,
    p2Avatar: String,
    p2Level: Int,
    p1Reps: Int,
    p1Score: Int,
    p2Reps: Int,
    p2Score: Int,
    onP1Rep: () -> Unit,
    onP2Rep: () -> Unit,
    onTimeUp: () -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }
    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var remainingSec by remember(durationSec, isPlayer2Turn) { mutableIntStateOf(durationSec) }
    var isPaused by remember { mutableStateOf(false) }
    var frontCamera by remember { mutableStateOf(true) }

    val counter = remember(exercise, isPlayer2Turn) { RepCounter(exercise) }
    var frame by remember { mutableStateOf<PvpPoseFrame?>(null) }
    var scorePopup by remember { mutableStateOf<String?>(null) }

    // Khi Camera AI nhận diện được 1 rep hoàn chỉnh
    LaunchedEffect(counter, isPlayer2Turn) {
        counter.onRepCompleted = {
            if (isPlayer2Turn) {
                onP2Rep()
                scorePopup = "🔥 $p2Name +1 REP!"
            } else {
                onP1Rep()
                scorePopup = "⚡ $p1Name +1 REP!"
            }
        }
    }

    // Nếu là bài Plank, mỗi 2 giây giữ chuẩn tư thế sẽ cộng 1 rep điểm
    LaunchedEffect(counter.holdSeconds) {
        if (exercise == Exercise.PLANK && counter.holdSeconds > 0 && counter.holdSeconds % 2 == 0) {
            if (isPlayer2Turn) {
                onP2Rep()
                scorePopup = "🔥 $p2Name +2s PLANK!"
            } else {
                onP1Rep()
                scorePopup = "⚡ $p1Name +2s PLANK!"
            }
        }
    }

    LaunchedEffect(scorePopup) {
        if (scorePopup != null) {
            delay(650)
            scorePopup = null
        }
    }

    // Đồng hồ đếm ngược từng giây + đối thủ tự động đua điểm trong chế độ LIVE_SOLO
    LaunchedEffect(remainingSec, isPaused, isPlayer2Turn, battleMode) {
        if (isPaused) return@LaunchedEffect
        if (remainingSec <= 0) {
            onTimeUp()
            return@LaunchedEffect
        }
        delay(1000L)
        remainingSec -= 1

        // Trong chế độ Đua Điểm Trực Tiếp (LIVE_SOLO), đối thủ cũng có nhịp ghi điểm cạnh tranh kịch tính
        if (battleMode == PvpBattleMode.LIVE_SOLO && remainingSec > 0) {
            val oppChance = (0.35f + (p2Level * 0.03f)).coerceIn(0.35f, 0.72f)
            if (Random.nextFloat() < oppChance) {
                onP2Rep()
            }
        }
    }

    // Tính tỷ lệ thanh Kéo Co Điểm Số (Tug-of-War)
    val totalScore = p1Score + p2Score
    val p1Ratio = if (totalScore == 0) 0.5f else (p1Score.toFloat() / totalScore.toFloat()).coerceIn(0.1f, 0.9f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Lớp nền Camera AI hoặc Đấu trường ảo nếu chưa cấp quyền Camera
        if (cameraGranted) {
            PvpCameraPreview(frontCamera) { pose, w, h ->
                counter.update(pose)
                frame = PvpPoseFrame(pose, w, h)
            }
            Canvas(Modifier.fillMaxSize()) {
                val f = frame ?: return@Canvas
                val s = min(size.width / f.w, size.height / f.h)
                val ox = (size.width - f.w * s) / 2
                val oy = (size.height - f.h * s) / 2
                fun pt(t: Int): Offset? = f.pose.getPoseLandmark(t)?.takeIf { it.inFrameLikelihood > 0.5f }?.let {
                    val x = if (frontCamera) f.w - it.position.x else it.position.x
                    Offset(ox + x * s, oy + it.position.y * s)
                }
                PVP_BONES.forEach { (a, b) ->
                    val pa = pt(a)
                    val pb = pt(b)
                    if (pa != null && pb != null) drawLine(C.Green, pa, pb, strokeWidth = 8f)
                }
                f.pose.allPoseLandmarks.forEach { l ->
                    pt(l.landmarkType)?.let { drawCircle(C.Orange, 9f, it) }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF14112A), Color(0xFF281845), Color(0xFF0E0C1D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚔️ ĐẤU TRƯỜNG SOLO 1v1",
                    color = Color.White.copy(alpha = 0.2f),
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                )
            }
        }

        // ======================== HUD PHÍA TRÊN: ĐỒNG HỒ & THANH KÉO CO ĐIỂM SỐ ========================
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip("✕ Rời trận", C.Text, onClick = onCancel)

                // ĐỒNG HỒ ĐẾM NGƯỢC THỜI GIAN THI ĐẤU
                val timerUrgent = remainingSec <= 5
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (timerUrgent) Color(0xFFD90429) else Color(0xE6181530))
                        .border(2.dp, if (timerUrgent) Color.White else C.Yellow, RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏱️ ${remainingSec}s",
                        color = if (timerUrgent) Color.White else C.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                if (cameraGranted) {
                    Chip("🔄 Cam", C.Text, onClick = { frontCamera = !frontCamera })
                } else {
                    Chip("📷 Bật Cam", C.Orange, onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
                }
            }

            Spacer(Modifier.height(10.dp))

            // BẢNG TỈ SỐ ĐỐI ĐẦU 2 NGƯỜI CHƠI + THANH TUG-OF-WAR
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xD9141129))
                    .border(1.5.dp, C.Border, RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Người chơi 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, C.Blue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p1Avatar, fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(p1Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("$p1Reps rep", color = C.Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Điểm số lớn ở giữa
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$p1Score",
                            color = C.Blue,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Text(
                            "  :  ",
                            color = C.Yellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            "$p2Score",
                            color = C.Pink,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }

                    // Người chơi 2
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(p2Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("$p2Reps rep", color = C.Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B1E2E))
                                .border(2.dp, C.Pink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p2Avatar, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Thanh kéo co tỉ số trực quan (Tug-of-War Progress Bar)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF3B1E2E))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(p1Ratio)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF38BDF8))
                                )
                            )
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF4D6D), Color(0xFFD90429))
                                )
                            )
                    )
                }

                Spacer(Modifier.height(6.dp))

                val leadText = when {
                    p1Score > p2Score -> "🔥 $p1Name đang dẫn trước +${p1Score - p2Score} điểm!"
                    p2Score > p1Score -> "⚠️ $p2Name đang dẫn trước +${p2Score - p1Score} điểm!"
                    else -> "⚡ Hai người chơi đang ngang điểm nhau!"
                }
                Text(
                    leadText,
                    color = C.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (scorePopup != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        scorePopup!!,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.Orange)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ======================== HUD PHÍA DƯỚI: BỘ ĐẾM REP & NÚT GHI ĐIỂM ========================
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xD90D0B1E))
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activePlayerLabel = if (battleMode == PvpBattleMode.PASS_AND_PLAY) {
                if (isPlayer2Turn) "LƯỢT THI ĐẤU: $p2Name (${exercise.label})"
                else "LƯỢT THI ĐẤU: $p1Name (${exercise.label})"
            } else {
                "ĐUA ĐIỂM TRỰC TIẾP: ${exercise.label} TRONG ${durationSec}s"
            }

            Text(
                activePlayerLabel,
                color = C.Orange,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(4.dp))

            val shownReps = if (isPlayer2Turn) p2Reps else p1Reps
            val shownScore = if (isPlayer2Turn) p2Score else p1Score
            Text(
                "$shownScore ĐIỂM ($shownReps REP)",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp
            )

            Text(
                "${counter.hint} · Góc khớp: ${counter.angleNow}°",
                color = C.Muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            // Nút bấm cộng Rep trực tiếp hỗ trợ cả thi đấu 2 người trên cùng máy & test trên máy ảo Emulator
            if (battleMode == PvpBattleMode.LIVE_SOLO) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.Blue)
                            .clickable {
                                onP1Rep()
                                scorePopup = "⚡ $p1Name +1 REP!"
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚡ +1 REP ($p1Name)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.Pink)
                            .clickable {
                                onP2Rep()
                                scorePopup = "🔥 $p2Name +1 REP!"
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔥 +1 REP ($p2Name)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                val activeName = if (isPlayer2Turn) p2Name else p1Name
                val btnColor = if (isPlayer2Turn) C.Pink else C.Blue
                BigButton(
                    text = "⚡ +1 REP CHO $activeName",
                    color = btnColor,
                    textColor = Color.White
                ) {
                    if (isPlayer2Turn) {
                        onP2Rep()
                        scorePopup = "🔥 $p2Name +1 REP!"
                    } else {
                        onP1Rep()
                        scorePopup = "⚡ $p1Name +1 REP!"
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Kết thúc sớm lượt này >>",
                color = C.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onTimeUp() }
            )
        }
    }
}

// ============================================================================
// 4. MÀN HÌNH KẾT QUẢ CHUNG CUỘC & TRAO THƯỞNG SOLO 1v1
// ============================================================================
@Composable
private fun PvpResultView(
    p1Name: String,
    p1Avatar: String,
    p2Name: String,
    p2Avatar: String,
    exercise: Exercise,
    durationSec: Int,
    p1Reps: Int,
    p1Score: Int,
    p2Reps: Int,
    p2Score: Int,
    record: PvpMatchRecord?,
    onRematch: () -> Unit,
    onBackToLobby: () -> Unit,
    onExit: () -> Unit
) {
    val isP1Win = p1Score > p2Score
    val isDraw = p1Score == p2Score

    val bannerTitle = when {
        isDraw -> "🤝 TRẬN SOLO HÒA KỊCH TÍNH!"
        isP1Win -> "🏆 $p1Name CHIẾN THẮNG!"
        else -> "🏆 $p2Name CHIẾN THẮNG!"
    }
    val bannerColor = when {
        isDraw -> C.Blue
        isP1Win -> C.Yellow
        else -> C.Pink
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Text("⚔️ KẾT QUẢ SOLO 1 VS 1 ⚔️", color = C.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            bannerTitle,
            color = bannerColor,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Bài tập: ${exercise.label} · Thời gian thi đấu: ${durationSec} giây",
            color = C.Text,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(20.dp))

        // Thẻ so sánh điểm số 2 người chơi
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cột Người Chơi 1
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isP1Win) Color(0xFF282354) else C.Card)
                    .border(
                        width = if (isP1Win) 2.5.dp else 1.dp,
                        color = if (isP1Win) C.Yellow else C.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isP1Win) {
                    Text("👑 WINNER", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Text(p1Avatar, fontSize = 44.sp)
                Spacer(Modifier.height(6.dp))
                Text(p1Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Text("$p1Score", color = C.Blue, fontWeight = FontWeight.Black, fontSize = 30.sp)
                Text("ĐIỂM SỐ", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("$p1Reps Rep hoàn thành", color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Cột Người Chơi 2
            val isP2Win = p2Score > p1Score
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isP2Win) Color(0xFF3D1B35) else C.Card)
                    .border(
                        width = if (isP2Win) 2.5.dp else 1.dp,
                        color = if (isP2Win) C.Yellow else C.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isP2Win) {
                    Text("👑 WINNER", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Text(p2Avatar, fontSize = 44.sp)
                Spacer(Modifier.height(6.dp))
                Text(p2Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Text("$p2Score", color = C.Pink, fontWeight = FontWeight.Black, fontSize = 30.sp)
                Text("ĐIỂM SỐ", color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("$p2Reps Rep hoàn thành", color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(18.dp))

        // Phần thưởng trận đấu PvP
        val goldEarned = record?.goldReward ?: 0
        val xpEarned = record?.xpReward ?: 0
        VCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🎁 PHẦN THƯỞNG ĐẤU TRƯỜNG SOLO",
                    color = C.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙 +$goldEarned Vàng", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("✨ +$xpEarned XP", color = C.Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        BigButton("🔄 TÁI ĐẤU NGAY (${durationSec}s)", C.Orange, Color.White, onRematch)
        Spacer(Modifier.height(10.dp))
        BigButton("⚙️ ĐỔI ĐỐI THỦ / THỜI GIAN", C.Card, C.Text, onBackToLobby)
        Spacer(Modifier.height(10.dp))
        BigButton("🏠 VỀ MÀN HÌNH ẢI", C.Card, C.Muted, onExit)
    }
}

@Composable
private fun PvpCameraPreview(front: Boolean, onPose: (Pose, Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val detector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val latestOnPose by rememberUpdatedState(onPose)

    DisposableEffect(front) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val rot = proxy.imageInfo.rotationDegrees
                val (w, h) = if (rot == 90 || rot == 270) proxy.height to proxy.width else proxy.width to proxy.height
                detector.process(InputImage.fromMediaImage(media, rot))
                    .addOnSuccessListener { latestOnPose(it, w, h) }
                    .addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                owner,
                if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() } }
    }
    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            executor.shutdown()
        }
    }

    AndroidView({ previewView }, Modifier.fillMaxSize())
}
