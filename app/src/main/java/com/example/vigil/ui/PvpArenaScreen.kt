package com.example.vigil.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vigil.data.GameViewModel
import com.example.vigil.model.PvpMatchRecord
import com.example.vigil.model.PvpOpponent
import com.example.vigil.network.ApiClient
import com.example.vigil.pose.Exercise
import com.example.vigil.pose.RepCounter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class PvpStage {
    LOBBY,
    MATCHMAKING,
    VIDEO_CALL_BATTLE,
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.refreshPvpData()
    }

    val realOpponents = vm.pvpOpponents
    var selectedExercise by remember { mutableStateOf(Exercise.PUSHUP) }
    var selectedDurationSec by remember { mutableIntStateOf(30) }
    var stage by remember { mutableStateOf(PvpStage.LOBBY) }

    // Thông tin phòng đấu & đối thủ thật được ghép
    var activeRoomId by remember { mutableStateOf("") }
    var matchedOpponent by remember { mutableStateOf<PvpOpponent?>(null) }

    // Điểm số & số Rep của 2 người chơi trong trận
    var p1Reps by remember { mutableIntStateOf(0) }
    var p1Score by remember { mutableIntStateOf(0) }
    var p2Reps by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var p2Angle by remember { mutableIntStateOf(165) }
    var p2Hint by remember { mutableStateOf("Đang giữ nhịp tập") }
    var p2FrameBase64 by remember { mutableStateOf<String?>(null) }

    var matchRecord by remember { mutableStateOf<PvpMatchRecord?>(null) }

    fun calcPointsForRep(currentReps: Int, isPlayer1: Boolean): Int {
        val base = 100
        val comboBonus = (currentReps / 3) * 15
        val oppLvl = matchedOpponent?.level ?: 1
        val statBonus = if (isPlayer1) (vm.totalPre * 2) else (oppLvl * 6)
        return base + comboBonus + statBonus
    }

    fun startMatchedBattle(opponent: PvpOpponent, roomId: String = "") {
        matchedOpponent = opponent
        activeRoomId = roomId
        p1Reps = 0
        p1Score = 0
        p2Reps = 0
        p2Score = 0
        p2Angle = 165
        p2Hint = "Đang vào tư thế ${selectedExercise.label}"
        p2FrameBase64 = null
        matchRecord = null
        stage = PvpStage.VIDEO_CALL_BATTLE
    }

    when (stage) {
        PvpStage.LOBBY -> {
            PvpModernLobbyView(
                vm = vm,
                realOpponents = realOpponents,
                selectedExercise = selectedExercise,
                onSelectExercise = { selectedExercise = it },
                selectedDurationSec = selectedDurationSec,
                onSelectDuration = { selectedDurationSec = it },
                onStartMatchmaking = {
                    activeRoomId = ""
                    matchedOpponent = null
                    stage = PvpStage.MATCHMAKING
                },
                onDirectCallOpponent = { opp ->
                    // Vào màn hình chờ kết nối với tài khoản người chơi thật được chọn
                    matchedOpponent = opp
                    stage = PvpStage.MATCHMAKING
                },
                onClose = onClose
            )
        }

        PvpStage.MATCHMAKING -> {
            PvpMatchmakingWaitingView(
                vm = vm,
                selectedExercise = selectedExercise,
                selectedDurationSec = selectedDurationSec,
                realOpponents = realOpponents,
                invitedOpponent = matchedOpponent,
                onMatched = { opp, roomId ->
                    startMatchedBattle(opp, roomId)
                },
                onCancelQueue = {
                    scope.launch(Dispatchers.IO) {
                        ApiClient.cancelPvpQueue(vm.name)
                    }
                    stage = PvpStage.LOBBY
                }
            )
        }

        PvpStage.VIDEO_CALL_BATTLE -> {
            val opp = matchedOpponent ?: PvpOpponent(0, "Người Chơi 2", "🧑‍🎤", 1, 50, true)
            PvpMessengerVideoBattleView(
                vm = vm,
                roomId = activeRoomId,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p2Name = opp.username,
                p2Avatar = opp.avatar,
                p2Level = opp.level,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Reps = p2Reps,
                p2Score = p2Score,
                p2Angle = p2Angle,
                p2Hint = p2Hint,
                p2FrameBase64 = p2FrameBase64,
                onP1Rep = {
                    p1Reps += 1
                    p1Score += calcPointsForRep(p1Reps, isPlayer1 = true)
                },
                onP2RepManual = {
                    p2Reps += 1
                    p2Score += calcPointsForRep(p2Reps, isPlayer1 = false)
                    p2Angle = if (p2Reps % 2 == 0) 88 else 165
                    p2Hint = "Hoàn thành $p2Reps rep!"
                },
                onRemoteOpponentSync = { rReps, rScore, rAngle, rHint, rFrame ->
                    if (rReps > p2Reps) p2Reps = rReps
                    if (rScore > p2Score) p2Score = rScore
                    p2Angle = rAngle
                    if (rHint.isNotBlank()) p2Hint = rHint
                    if (rFrame != null) p2FrameBase64 = rFrame
                },
                onFinishCall = {
                    val rec = vm.finishPvpMatch(
                        player2Name = opp.username,
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
                onCancelCall = {
                    stage = PvpStage.LOBBY
                }
            )
        }

        PvpStage.RESULT -> {
            val opp = matchedOpponent ?: PvpOpponent(0, "Người Chơi 2", "🧑‍🎤", 1, 50, true)
            PvpModernResultView(
                p1Name = vm.name,
                p1Avatar = vm.avatar,
                p2Name = opp.username,
                p2Avatar = opp.avatar,
                exercise = selectedExercise,
                durationSec = selectedDurationSec,
                p1Reps = p1Reps,
                p1Score = p1Score,
                p2Reps = p2Reps,
                p2Score = p2Score,
                record = matchRecord,
                onRematchQueue = {
                    stage = PvpStage.MATCHMAKING
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
// 1. MÀN HÌNH SẢNH SOLO PVP HIỆN ĐẠI (MODERN LOBBY)
// ============================================================================
@Composable
private fun PvpModernLobbyView(
    vm: GameViewModel,
    realOpponents: List<PvpOpponent>,
    selectedExercise: Exercise,
    onSelectExercise: (Exercise) -> Unit,
    selectedDurationSec: Int,
    onSelectDuration: (Int) -> Unit,
    onStartMatchmaking: () -> Unit,
    onDirectCallOpponent: (PvpOpponent) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Navigation Bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Card)
                    .border(1.dp, C.Border, RoundedCornerShape(12.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("← Quay lại", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "📹 ĐẤU TRƯỜNG VIDEO 1v1",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                Text(
                    "Ghép trận người chơi thật · Call trực tiếp",
                    color = C.Muted,
                    fontSize = 11.sp
                )
            }

            Chip("🏆 ${vm.pvpHistory.size}", C.Yellow)
        }

        Spacer(Modifier.height(16.dp))

        // ======================== HERO BANNER: HỒ SƠ CHIẾN BINH & TRẠNG THÁI ONLINE ========================
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF311042), Color(0xFF0F172A))
                    )
                )
                .border(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, C.Blue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(vm.avatar, fontSize = 30.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(C.Green))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "SẴN SÀNG GHÉP ĐỘI",
                                    color = C.Green,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                vm.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            Text(
                                "Cấp ${vm.level} · ⚡ ${vm.combatPower} Lực Chiến",
                                color = C.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Badge chế độ Video Call
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                            .border(1.dp, C.Blue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("📹", fontSize = 20.sp)
                        Text(
                            "LIVE CALL",
                            color = C.Blue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Khi vào trận, màn hình hiển thị trực tiếp Camera của đối phương và khung hình nhỏ (PiP) của bạn giống cuộc gọi Messenger để hai bên giám sát động tác của nhau.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ======================== CHỌN BÀI TẬP & THỜI GIAN THI ĐẤU ========================
        VCard {
            Text(
                "1. CHỌN BÀI TẬP THI ĐẤU",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            val mainExercises = listOf(Exercise.PUSHUP, Exercise.SQUAT, Exercise.SITUP, Exercise.PLANK)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mainExercises.forEach { ex ->
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Color(0xFF31255A) else Color(0xFF141129))
                            .border(
                                if (sel) 2.dp else 1.dp,
                                if (sel) C.Orange else C.Border,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectExercise(ex) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(icon, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ex.label,
                            color = if (sel) C.Yellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "2. THỜI GIAN SOLO TÍNH ĐIỂM",
                color = C.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            val durations = listOf(
                15 to "15 Giây",
                30 to "30 Giây",
                60 to "60 Giây",
                90 to "90 Giây"
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
                            .background(if (sel) C.Orange else Color(0xFF141129))
                            .border(1.dp, if (sel) C.Yellow else C.Border, RoundedCornerShape(12.dp))
                            .clickable { onSelectDuration(sec) }
                            .padding(vertical = 11.dp),
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

        Spacer(Modifier.height(14.dp))

        // NÚT TÌM TRẬN (GHÉP ĐỘI NGƯỜI CHƠI THẬT)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFEA580C), Color(0xFFDB2777), Color(0xFF7C3AED))
                    )
                )
                .border(1.5.dp, C.Yellow, RoundedCornerShape(18.dp))
                .clickable { onStartMatchmaking() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔍", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "TÌM ĐỐI THỦ GHÉP TRẬN (${selectedDurationSec}s)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        "Chờ người chơi thật vào phòng · Không đấu với máy",
                        color = C.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ======================== DANH SÁCH TÀI KHOẢN NGƯỜI CHƠI THẬT TRONG HỆ THỐNG ========================
        VCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "👥 TÀI KHOẢN NGƯỜI CHƠI THẬT (${realOpponents.size})",
                    color = C.Text,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    "Mời Call Solo trực tiếp",
                    color = C.Blue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            if (realOpponents.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141129))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Hiện chỉ có tài khoản của bạn trong CSDL. Hãy đăng ký thêm tài khoản người chơi thứ 2 hoặc bấm 'Tìm Đối Thủ Ghép Trận' để chờ máy khác kết nối!",
                        color = C.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                realOpponents.forEach { opp ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141129))
                            .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF221A3E))
                                .border(1.5.dp, C.Blue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(opp.avatar, fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                opp.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Cấp ${opp.level} · ⚔️ Sát thương ${opp.attackDamage}",
                                color = C.Muted,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Blue.copy(alpha = 0.2f))
                                .border(1.dp, C.Blue, RoundedCornerShape(10.dp))
                                .clickable { onDirectCallOpponent(opp) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "📹 Mời Solo",
                                color = C.Blue,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ======================== LỊCH SỬ ĐẤU PVP ========================
        if (vm.pvpHistory.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "📜 LỊCH SỬ TRẬN ĐẤU SOLO",
                color = C.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            vm.pvpHistory.take(6).forEach { match ->
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
                            .background(badgeColor.copy(alpha = 0.18f))
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
                            "${match.exercise} · ${match.durationSec}s · Rep: ${match.p1Reps} - ${match.p2Reps}",
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

        Spacer(Modifier.height(30.dp))
    }
}

// ============================================================================
// 2. MÀN HÌNH CHỜ GHÉP TRẬN NGƯỜI CHƠI THẬT (WAIT UNTIL MATCHED OR CANCELLED)
// ============================================================================
@Composable
private fun PvpMatchmakingWaitingView(
    vm: GameViewModel,
    selectedExercise: Exercise,
    selectedDurationSec: Int,
    realOpponents: List<PvpOpponent>,
    invitedOpponent: PvpOpponent?,
    onMatched: (PvpOpponent, String) -> Unit,
    onCancelQueue: () -> Unit
) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    var statusText by remember {
        mutableStateOf(
            if (invitedOpponent != null) "Đang đổ chuông mời ${invitedOpponent.username} vào phòng Video Call..."
            else "Đang tìm kiếm người chơi thật trong hàng đợi..."
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Liên tục chờ ghép trận trên Server (Không tự ý ghép với bot, chờ đến khi có đối thủ thật hoặc bấm Hủy)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val joinRes = ApiClient.joinPvpQueue(
                vm.name,
                vm.avatar,
                vm.level,
                vm.attackDamage,
                selectedExercise.name,
                selectedDurationSec
            )
            if (joinRes.matched && joinRes.opponentName.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    onMatched(
                        PvpOpponent(
                            id = 999,
                            username = joinRes.opponentName,
                            avatar = joinRes.opponentAvatar,
                            level = joinRes.opponentLevel,
                            attackDamage = joinRes.opponentDamage,
                            isRealUser = true
                        ),
                        joinRes.roomId
                    )
                }
                return@withContext
            }
        }

        while (true) {
            delay(1000L)
            elapsedSec += 1
            val checkRes = withContext(Dispatchers.IO) {
                ApiClient.checkPvpQueueStatus(vm.name)
            }
            if (checkRes.matched && checkRes.opponentName.isNotBlank()) {
                onMatched(
                    PvpOpponent(
                        id = 999,
                        username = checkRes.opponentName,
                        avatar = checkRes.opponentAvatar,
                        level = checkRes.opponentLevel,
                        attackDamage = checkRes.opponentDamage,
                        isRealUser = true
                    ),
                    checkRes.roomId
                )
                break
            }
        }
    }

    val mm = elapsedSec / 60
    val ss = elapsedSec % 60
    val timeFormatted = String.format("%02d:%02d", mm, ss)

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0B1E), Color(0xFF1A1438), Color(0xFF0D0B1E))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Chip("📹 PHÒNG CHỜ SOLO TRỰC TUYẾN", C.Blue)
            Spacer(Modifier.height(10.dp))
            Text(
                "ĐANG CHỜ ĐỐI THỦ...",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Bài tập: ${selectedExercise.label} · Thời gian: ${selectedDurationSec} giây",
                color = C.Yellow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Radar Animation + Avatar người chơi
        Box(
            Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = (size.minDimension / 2f) * pulseScale.coerceAtMost(1.2f)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = (1.35f - pulseScale).coerceIn(0.05f, 0.45f)),
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = 0.25f),
                    radius = size.minDimension * 0.36f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(3.dp, C.Blue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(vm.avatar, fontSize = 44.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "⏱️ $timeFormatted",
                    color = C.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    statusText,
                    color = C.Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Khu vực ghép tài khoản người chơi thật & Nút Hủy Tìm Trận
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (invitedOpponent != null) {
                VCard(border = C.Blue) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(invitedOpponent.avatar, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Đang gọi ${invitedOpponent.username}...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Tài khoản người chơi thật trong hệ thống",
                                    color = C.Muted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Green)
                                .clickable { onMatched(invitedOpponent, "local_room_${System.currentTimeMillis()}") }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "📞 Bắt Máy Ngay",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (realOpponents.isNotEmpty()) {
                VCard {
                    Text(
                        "📲 TÀI KHOẢN NGƯỜI CHƠI THẬT SẴN SÀNG KẾT NỐI:",
                        color = C.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hệ thống đang chờ máy thứ 2 ghép trận, hoặc chạm để tài khoản người chơi thứ 2 chấp nhận vào phòng Video Call:",
                        color = C.Muted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    realOpponents.take(3).forEach { opp ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141129))
                                .border(1.dp, C.Border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(opp.avatar, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${opp.username} (Lv.${opp.level})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(C.Green)
                                    .clickable {
                                        onMatched(opp, "room_${System.currentTimeMillis()}")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "✅ Vào Phòng",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Nút Hủy Tìm Trận
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                    .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                    .clickable { onCancelQueue() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✕ HỦY CHỜ GHÉP TRẬN",
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ============================================================================
// 3. MÀN HÌNH SOLO VIDEO CALL 1V1 KIỂU MESSENGER (OPPONENT MAIN VIEW + SELF PIP)
// ============================================================================
@Composable
private fun PvpMessengerVideoBattleView(
    vm: GameViewModel,
    roomId: String,
    exercise: Exercise,
    durationSec: Int,
    p1Name: String,
    p1Avatar: String,
    p2Name: String,
    p2Avatar: String,
    p2Level: Int,
    p1Reps: Int,
    p1Score: Int,
    p2Reps: Int,
    p2Score: Int,
    p2Angle: Int,
    p2Hint: String,
    p2FrameBase64: String?,
    onP1Rep: () -> Unit,
    onP2RepManual: () -> Unit,
    onRemoteOpponentSync: (Int, Int, Int, String, String?) -> Unit,
    onFinishCall: () -> Unit,
    onCancelCall: () -> Unit
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

    var remainingSec by remember(durationSec) { mutableIntStateOf(durationSec) }
    var useFrontCamera by remember { mutableStateOf(false) } // Mặc định Back Camera an toàn trên mọi Emulator
    var swapPipView by remember { mutableStateOf(false) }   // false = Đối phương ở màn hình lớn, Bản thân ở góc PiP (chuẩn Messenger)

    val counter = remember(exercise) { RepCounter(exercise) }
    var myPoseFrame by remember { mutableStateOf<PvpPoseFrame?>(null) }
    var scorePopup by remember { mutableStateOf<String?>(null) }

    // Nhận diện rep từ Camera AI của bản thân
    LaunchedEffect(counter) {
        counter.onRepCompleted = {
            onP1Rep()
            scorePopup = "⚡ $p1Name +1 REP!"
        }
    }

    LaunchedEffect(counter.holdSeconds) {
        if (exercise == Exercise.PLANK && counter.holdSeconds > 0 && counter.holdSeconds % 2 == 0) {
            onP1Rep()
            scorePopup = "⚡ $p1Name +2s PLANK!"
        }
    }

    LaunchedEffect(scorePopup) {
        if (scorePopup != null) {
            delay(700)
            scorePopup = null
        }
    }

    // Đồng hồ đếm ngược & Đồng bộ dữ liệu phòng đấu thời gian thực với đối phương (KHÔNG tự cộng điểm bằng bot!)
    LaunchedEffect(remainingSec) {
        if (remainingSec <= 0) {
            onFinishCall()
            return@LaunchedEffect
        }
        delay(1000L)
        remainingSec -= 1

        if (roomId.isNotBlank()) {
            val syncState = withContext(Dispatchers.IO) {
                ApiClient.syncPvpRoom(
                    roomId,
                    p1Name,
                    p1Reps,
                    p1Score,
                    counter.angleNow,
                    counter.hint,
                    null
                )
            }
            if (syncState.matched) {
                onRemoteOpponentSync(
                    syncState.opponentReps,
                    syncState.opponentScore,
                    syncState.opponentAngle,
                    syncState.opponentHint,
                    syncState.opponentFrameBase64
                )
            }
        }
    }

    val totalScore = p1Score + p2Score
    val p1Ratio = if (totalScore == 0) 0.5f else (p1Score.toFloat() / totalScore.toFloat()).coerceIn(0.12f, 0.88f)

    Box(Modifier.fillMaxSize().background(Color(0xFF080711))) {
        // =========================================================================
        // A. KHUNG HÌNH VIDEO CHÍNH TOÀN MÀN HÌNH (MẶC ĐỊNH: HÌNH ẢNH CỦA ĐỐI PHƯƠNG)
        // =========================================================================
        if (!swapPipView) {
            OpponentLiveVideoFeed(
                opponentName = p2Name,
                opponentAvatar = p2Avatar,
                opponentLevel = p2Level,
                exercise = exercise,
                opponentReps = p2Reps,
                opponentScore = p2Score,
                opponentAngle = p2Angle,
                opponentHint = p2Hint,
                frameBase64 = p2FrameBase64,
                isFullScreen = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            SelfLiveCameraFeed(
                cameraGranted = cameraGranted,
                useFrontCamera = useFrontCamera,
                playerName = p1Name,
                playerAvatar = p1Avatar,
                counter = counter,
                poseFrame = myPoseFrame,
                onPoseDetected = { pose, w, h ->
                    counter.update(pose)
                    myPoseFrame = PvpPoseFrame(pose, w, h)
                },
                isFullScreen = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // =========================================================================
        // B. THANH TRẠNG THÁI CUỘC GỌI & TỈ SỐ PHÍA TRÊN (FLOATING TOP HUD)
        // =========================================================================
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Hàng 1: Rời phòng - Đồng hồ đếm ngược - Ping
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB3141129))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable { onCancelCall() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("← Rời phòng", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                val isUrgent = remainingSec <= 5
                Row(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isUrgent) Color(0xFFDC2626) else Color(0xCC141129))
                        .border(1.5.dp, if (isUrgent) Color.White else C.Yellow, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isUrgent) Color.White else C.Green))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format("00:%02d", remainingSec),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB3141129))
                        .border(1.dp, C.Green.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🟢 HD Call", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Hàng 2: Thanh Tỉ Số Kéo Co Gọn Gàng (Sleek Glassmorphism Score Pill)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xCC110E24))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bạn (Người chơi 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, C.Blue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p1Avatar, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "$p1Name (Bạn)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "$p1Score đ · $p1Reps rep",
                                color = C.Blue,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text("⚡ VS ⚡", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 13.sp)

                    // Đối phương (Người chơi 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                p2Name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "$p2Score đ · $p2Reps rep",
                                color = C.Pink,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B1E2E))
                                .border(1.5.dp, C.Pink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p2Avatar, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Thanh tiến trình kéo co (Tug of War)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF3B1E2E))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(p1Ratio)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF38BDF8))))
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF4D6D), Color(0xFFE11D48))))
                    )
                }
            }
        }

        // =========================================================================
        // C. KHUNG HÌNH NHỎ CỦA BẢN THÂN ĐANG QUAY (FLOATING PIP WINDOW NHƯ MESSENGER)
        // =========================================================================
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 136.dp, end = 14.dp)
                .size(width = 124.dp, height = 178.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141129))
                .border(2.dp, C.Blue, RoundedCornerShape(20.dp))
                .clickable { swapPipView = !swapPipView }
        ) {
            if (!swapPipView) {
                // Khung nhỏ hiển thị Camera thật đang quay của chính bản thân
                SelfLiveCameraFeed(
                    cameraGranted = cameraGranted,
                    useFrontCamera = useFrontCamera,
                    playerName = p1Name,
                    playerAvatar = p1Avatar,
                    counter = counter,
                    poseFrame = myPoseFrame,
                    onPoseDetected = { pose, w, h ->
                        counter.update(pose)
                        myPoseFrame = PvpPoseFrame(pose, w, h)
                    },
                    isFullScreen = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Nếu đã hoán đổi thì khung nhỏ hiển thị Đối phương
                OpponentLiveVideoFeed(
                    opponentName = p2Name,
                    opponentAvatar = p2Avatar,
                    opponentLevel = p2Level,
                    exercise = exercise,
                    opponentReps = p2Reps,
                    opponentScore = p2Score,
                    opponentAngle = p2Angle,
                    opponentHint = p2Hint,
                    frameBase64 = p2FrameBase64,
                    isFullScreen = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Nhãn tên ở góc dưới khung PiP + Nút thu phóng
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xB3000000))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!swapPipView) "📹 Bạn ($p1Name)" else "📹 $p2Name",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("⤢", color = C.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }

        // Hiệu ứng thông báo khi lên Rep
        if (scorePopup != null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6EA580C))
                    .border(1.5.dp, C.Yellow, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    scorePopup!!,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        // =========================================================================
        // D. THANH ĐIỀU KHIỂN CUỘC GỌI PHÍA DƯỚI (MESSENGER CALL BOTTOM DOCK)
        // =========================================================================
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xE6110E24))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thông tin góc khớp & hướng dẫn AI của bản thân
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ĐỘNG TÁC CỦA BẠN (${exercise.label})",
                            color = C.Blue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${counter.hint} · Góc khớp: ${counter.angleNow}°",
                            color = C.Text,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "$p1Reps REP",
                        color = C.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Hàng nút chấm điểm nhanh cho 2 người chơi (hỗ trợ test trực tiếp & chấm điểm chéo)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.Blue)
                            .clickable {
                                onP1Rep()
                                scorePopup = "⚡ $p1Name +1 REP!"
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚡ +1 Rep ($p1Name)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.Pink)
                            .clickable {
                                onP2RepManual()
                                scorePopup = "🔥 $p2Name +1 REP!"
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔥 +1 Rep ($p2Name)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Hàng nút điều khiển Video Call chuẩn Messenger (Đổi Cam, Đổi Khung, Bật Cam, Kết Thúc)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Đổi Camera Trước/Sau
                    CallControlButton(
                        icon = "🔄",
                        label = "Đổi Cam",
                        bgColor = Color(0xFF262147),
                        onClick = {
                            if (cameraGranted) {
                                useFrontCamera = !useFrontCamera
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )

                    // Nút Đổi Khung Hình (Hoán đổi màn hình chính <-> PiP)
                    CallControlButton(
                        icon = "🖼️",
                        label = "Đổi Khung",
                        bgColor = Color(0xFF262147),
                        onClick = { swapPipView = !swapPipView }
                    )

                    // Nút Kết thúc trận / Cúp máy
                    CallControlButton(
                        icon = "📞",
                        label = "Kết Thúc",
                        bgColor = Color(0xFFEF4444),
                        onClick = onFinishCall
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = C.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================================================================
// KHUNG HÌNH VIDEO TRỰC TIẾP CỦA ĐỐI PHƯƠNG (HIỂN THỊ ĐỐI THỦ ĐANG TẬP NHƯ THẾ NÀO)
// ============================================================================
@Composable
private fun OpponentLiveVideoFeed(
    opponentName: String,
    opponentAvatar: String,
    opponentLevel: Int,
    exercise: Exercise,
    opponentReps: Int,
    opponentScore: Int,
    opponentAngle: Int,
    opponentHint: String,
    frameBase64: String?,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier
) {
    // Giải mã khung hình Camera từ máy đối phương nếu có
    val remoteBitmap = remember(frameBase64) {
        if (frameBase64.isNullOrBlank()) null
        else runCatching {
            val bytes = Base64.decode(frameBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    // Chuyển động mô phỏng khung xương trực tiếp của đối phương theo nhịp tập thực tế
    val infiniteTransition = rememberInfiniteTransition(label = "opp_pose")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF13112C), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
    ) {
        if (remoteBitmap != null) {
            Image(
                bitmap = remoteBitmap.asImageBitmap(),
                contentDescription = "Opponent Video Feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Vẽ khung hình Camera AI Pose trực tiếp của đối phương đang thực hiện bài tập
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.42f
                val cy = h * 0.48f
                val scale = if (isFullScreen) min(w, h) * 0.28f else min(w, h) * 0.35f

                // Lưới nền Camera AI
                val gridStep = if (isFullScreen) 48.dp.toPx() else 20.dp.toPx()
                var gx = 0f
                while (gx < w) {
                    drawLine(
                        Color.White.copy(alpha = 0.04f),
                        Offset(gx, 0f),
                        Offset(gx, h),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(
                        Color.White.copy(alpha = 0.04f),
                        Offset(0f, gy),
                        Offset(w, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }

                // Tọa độ các khớp xương của đối phương theo bài tập (Squat / Pushup / Situp / Plank)
                val dip = animPhase * scale * 0.38f
                val head = Offset(cx, cy - scale * 0.75f + dip)
                val neck = Offset(cx, cy - scale * 0.50f + dip)
                val lShoulder = Offset(cx - scale * 0.35f, cy - scale * 0.45f + dip)
                val rShoulder = Offset(cx + scale * 0.35f, cy - scale * 0.45f + dip)
                val lElbow = Offset(cx - scale * 0.55f, cy - scale * 0.15f + dip * 0.6f)
                val rElbow = Offset(cx + scale * 0.55f, cy - scale * 0.15f + dip * 0.6f)
                val lWrist = Offset(cx - scale * 0.45f, cy + scale * 0.10f)
                val rWrist = Offset(cx + scale * 0.45f, cy + scale * 0.10f)
                val lHip = Offset(cx - scale * 0.22f, cy + scale * 0.12f + dip)
                val rHip = Offset(cx + scale * 0.22f, cy + scale * 0.12f + dip)
                val lKnee = Offset(cx - scale * 0.32f - dip * 0.2f, cy + scale * 0.52f + dip * 0.4f)
                val rKnee = Offset(cx + scale * 0.32f + dip * 0.2f, cy + scale * 0.52f + dip * 0.4f)
                val lAnkle = Offset(cx - scale * 0.26f, cy + scale * 0.92f)
                val rAnkle = Offset(cx + scale * 0.26f, cy + scale * 0.92f)

                val segments = listOf(
                    lShoulder to rShoulder,
                    lShoulder to lElbow, lElbow to lWrist,
                    rShoulder to rElbow, rElbow to rWrist,
                    lShoulder to lHip, rShoulder to rHip,
                    lHip to rHip,
                    lHip to lKnee, lKnee to lAnkle,
                    rHip to rKnee, rKnee to rAnkle
                )

                // Vẽ bóng cơ thể đối phương
                drawCircle(Color(0xFFFF5A7A).copy(alpha = 0.22f), radius = scale * 0.22f, center = head)

                // Vẽ xương AI Pose của đối phương
                val boneStroke = if (isFullScreen) 9f else 4f
                segments.forEach { (a, b) ->
                    drawLine(
                        color = Color(0xFF4CC38A),
                        start = a,
                        end = b,
                        strokeWidth = boneStroke,
                        cap = StrokeCap.Round
                    )
                }

                // Vẽ khớp nối
                val joints = listOf(head, neck, lShoulder, rShoulder, lElbow, rElbow, lWrist, rWrist, lHip, rHip, lKnee, rKnee, lAnkle, rAnkle)
                val jointRadius = if (isFullScreen) 10f else 5f
                joints.forEach { pt ->
                    drawCircle(Color(0xFFFF6B35), radius = jointRadius, center = pt)
                }
            }
        }

        // Nhãn thông tin Camera của đối phương khi ở chế độ màn hình lớn
        if (isFullScreen) {
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xA60F172A))
                    .border(1.dp, C.Pink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(opponentAvatar, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "📹 CAMERA ĐỐI THỦ: $opponentName",
                            color = C.Pink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Cấp $opponentLevel · Đang tập ${exercise.label}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Góc khớp đối thủ: ${opponentAngle}° · $opponentHint",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ============================================================================
// KHUNG HÌNH CAMERA TRỰC TIẾP CỦA CHÍNH BẢN THÂN ĐANG QUAY (SELF CAMERA FEED)
// ============================================================================
@Composable
private fun SelfLiveCameraFeed(
    cameraGranted: Boolean,
    useFrontCamera: Boolean,
    playerName: String,
    playerAvatar: String,
    counter: RepCounter,
    poseFrame: PvpPoseFrame?,
    onPoseDetected: (Pose, Int, Int) -> Unit,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(Color(0xFF0F172A))) {
        if (cameraGranted) {
            PvpSafeCameraPreview(
                preferFrontCamera = useFrontCamera,
                onPose = onPoseDetected
            )
            // Vẽ khung xương AI Pose lên trên Camera của bản thân
            Canvas(Modifier.fillMaxSize()) {
                val f = poseFrame ?: return@Canvas
                if (f.w <= 0 || f.h <= 0) return@Canvas
                val s = min(size.width / f.w, size.height / f.h)
                val ox = (size.width - f.w * s) / 2
                val oy = (size.height - f.h * s) / 2
                fun pt(t: Int): Offset? = f.pose.getPoseLandmark(t)?.takeIf { it.inFrameLikelihood > 0.5f }?.let {
                    val x = if (useFrontCamera) f.w - it.position.x else it.position.x
                    Offset(ox + x * s, oy + it.position.y * s)
                }
                val strokeW = if (isFullScreen) 8f else 4f
                val radiusJoint = if (isFullScreen) 9f else 4.5f
                PVP_BONES.forEach { (a, b) ->
                    val pa = pt(a)
                    val pb = pt(b)
                    if (pa != null && pb != null) drawLine(C.Green, pa, pb, strokeWidth = strokeW)
                }
                f.pose.allPoseLandmarks.forEach { l ->
                    pt(l.landmarkType)?.let { drawCircle(C.Orange, radiusJoint, it) }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(playerAvatar, fontSize = if (isFullScreen) 48.sp else 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Camera của bạn",
                    color = C.Muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================================================
// 4. MÀN HÌNH KẾT QUẢ CHUNG CUỘC & TRAO THƯỞNG SOLO 1v1
// ============================================================================
@Composable
private fun PvpModernResultView(
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
    onRematchQueue: () -> Unit,
    onBackToLobby: () -> Unit,
    onExit: () -> Unit
) {
    val isP1Win = p1Score > p2Score
    val isDraw = p1Score == p2Score

    val bannerTitle = when {
        isDraw -> "🤝 KẾT QUẢ HÒA KỊCH TÍNH!"
        isP1Win -> "🏆 CHIẾN THẮNG ÁP ĐẢO!"
        else -> "💪 $p2Name CHIẾN THẮNG!"
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
        Spacer(Modifier.height(10.dp))
        Chip("📹 KẾT THÚC CUỘC GỌI SOLO 1v1", C.Blue)
        Spacer(Modifier.height(10.dp))
        Text(
            bannerTitle,
            color = bannerColor,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Bài tập: ${exercise.label} · Thời gian thi đấu: ${durationSec} giây",
            color = C.Muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thẻ Người chơi 1
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isP1Win) Color(0xFF231F4F) else C.Card)
                    .border(
                        width = if (isP1Win) 2.5.dp else 1.dp,
                        color = if (isP1Win) C.Yellow else C.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isP1Win) {
                    Text("👑 MVP", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Text(p1Avatar, fontSize = 42.sp)
                Spacer(Modifier.height(6.dp))
                Text(p1Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("$p1Score", color = C.Blue, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text("ĐIỂM SỐ", color = C.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("$p1Reps Rep", color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Thẻ Người chơi 2
            val isP2Win = p2Score > p1Score
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isP2Win) Color(0xFF3A1A35) else C.Card)
                    .border(
                        width = if (isP2Win) 2.5.dp else 1.dp,
                        color = if (isP2Win) C.Yellow else C.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isP2Win) {
                    Text("👑 MVP", color = C.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Text(p2Avatar, fontSize = 42.sp)
                Spacer(Modifier.height(6.dp))
                Text(p2Name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("$p2Score", color = C.Pink, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text("ĐIỂM SỐ", color = C.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("$p2Reps Rep", color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        val goldEarned = record?.goldReward ?: 0
        val xpEarned = record?.xpReward ?: 0
        VCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🎁 PHẦN THƯỞNG TRẬN SOLO",
                    color = C.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
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

        Spacer(Modifier.height(20.dp))

        BigButton("🔍 TÌM TRẬN MỚI", C.Orange, Color.White, onRematchQueue)
        Spacer(Modifier.height(10.dp))
        BigButton("⚙️ VỀ SẢNH SOLO", C.Card, C.Text, onBackToLobby)
        Spacer(Modifier.height(10.dp))
        BigButton("🏠 QUAY LẠI MÀN HÌNH ẢI", C.Card, C.Muted, onExit)
    }
}

// ============================================================================
// CAMERAX PREVIEW AN TOÀN TUYỆT ĐỐI (CHỐNG CRASH KHI THIẾU FRONT/BACK CAMERA TRÊN MÁY ẢO)
// ============================================================================
@Composable
private fun PvpSafeCameraPreview(
    preferFrontCamera: Boolean,
    onPose: (Pose, Int, Int) -> Unit
) {
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
    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val latestOnPose by rememberUpdatedState(onPose)

    DisposableEffect(preferFrontCamera) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
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

                // Chọn Camera an toàn: Kiểm tra hasCamera trước khi bind để không bao giờ bị văng IllegalArgumentException
                val primarySelector = if (preferFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val fallbackSelector = if (preferFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                val chosenSelector = when {
                    runCatching { provider.hasCamera(primarySelector) }.getOrDefault(false) -> primarySelector
                    runCatching { provider.hasCamera(fallbackSelector) }.getOrDefault(false) -> fallbackSelector
                    else -> null
                }

                if (chosenSelector != null) {
                    provider.bindToLifecycle(owner, chosenSelector, preview, analysis)
                }
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { detector.close() }
            runCatching { executor.shutdown() }
        }
    }

    AndroidView({ previewView }, Modifier.fillMaxSize())
}
