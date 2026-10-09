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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.vigil.model.AdventureData
import com.example.vigil.model.Monster
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vigil.data.GameViewModel
import com.example.vigil.pose.Exercise
import com.example.vigil.pose.RepCounter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.min

private class PoseFrame(val pose: Pose, val w: Int, val h: Int)

private val BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
)

@Composable
fun WorkoutScreen(
    exercise: Exercise,
    vm: GameViewModel,
    stageId: Int? = null,
    isBoss: Boolean = false,
    isGuildBoss: Boolean = false,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(Modifier.fillMaxSize().background(C.Bg).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Cần quyền camera để nhận diện động tác", color = C.Text, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            BigButton("Cấp quyền") { launcher.launch(Manifest.permission.CAMERA) }
            Spacer(Modifier.height(8.dp))
            BigButton("Quay lại", C.Card, C.Text, onClose)
        }
        return
    }

    // Xác định thông tin Quái vật / Boss / Tập tự do cho trận chiến này
    val guildBoss = vm.guildBossInfo?.boss
    val isFreeTraining = stageId == null && !isBoss && !isGuildBoss
    val monster: Monster = remember(stageId, isBoss, isGuildBoss, exercise, guildBoss) {
        if (isGuildBoss && guildBoss != null) {
            Monster(
                8888,
                guildBoss.name,
                guildBoss.title,
                guildBoss.avatar,
                guildBoss.currentHp.coerceAtLeast(1000),
                exercise,
                true,
                guildBoss.rewardGold,
                guildBoss.rewardGems,
                "Siêu Boss Thế Giới của bang hội! Toàn bộ thành viên chung sức góp dame!"
            )
        } else if (stageId != null) {
            AdventureData.getMonsterByStage(stageId)
        } else if (isBoss) {
            Monster(
                999,
                "Ma Thần Hư Không",
                "TRÙM ĐẠI THỬ THÁCH",
                "👹",
                (vm.attackDamage * 25).coerceAtLeast(2000),
                exercise,
                true,
                400,
                70,
                "Thực thể bóng tối chỉ gục ngã trước những chiến binh kiên trì nhất!"
            )
        } else {
            Monster(
                0,
                "Bù Nhìn Luyện Võ",
                "Tập Tự Do · Không Phần Thưởng",
                "🎯",
                999999,
                exercise,
                false,
                0,
                0,
                "Hình nộm rơm vững chắc để bạn mài giũa động tác mà không tính phần thưởng."
            )
        }
    }


    val counter = remember { RepCounter(exercise) }
    var frame by remember { mutableStateOf<PoseFrame?>(null) }
    var front by remember { mutableStateOf(false) }

    // Quản lý máu và sát thương thực tế
    val maxHp = monster.maxHp
    var currentHp by remember(monster.maxHp) { mutableIntStateOf(monster.maxHp) }
    var totalDamageDealt by remember { mutableIntStateOf(0) }
    val hpProgress = if (isFreeTraining) 1f else (currentHp / maxHp.toFloat()).coerceIn(0f, 1f)
    val isDefeated = !isFreeTraining && currentHp == 0

    // Hiệu ứng đòn đánh & sát thương nảy số
    var hitEffect by remember { mutableStateOf(false) }
    var damageText by remember { mutableStateOf<String?>(null) }

    // Tích hợp Text-To-Speech đọc số rep và hiệu ứng chiến đấu
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(ctx) {
        var localTts: android.speech.tts.TextToSpeech? = null
        localTts = android.speech.tts.TextToSpeech(ctx) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                localTts?.language = java.util.Locale.getDefault()
            }
        }
        tts = localTts
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
        }
    }

    LaunchedEffect(counter, tts) {
        counter.onRepCompleted = { count ->
            val isCrit = kotlin.random.Random.nextFloat() < vm.critRate
            val baseDmg = vm.attackDamage
            val variance = if (baseDmg > 10) kotlin.random.Random.nextInt(-(baseDmg / 10), (baseDmg / 10) + 1) else 0
            val hitDmg = if (isCrit) {
                ((baseDmg + variance) * vm.critMultiplier).toInt().coerceAtLeast(1)
            } else {
                (baseDmg + variance).coerceAtLeast(1)
            }

            hitEffect = true
            damageText = if (isCrit) "💥 BẠO KÍCH! -$hitDmg HP" else "⚔️ -$hitDmg HP"
            totalDamageDealt += hitDmg

            if (!isFreeTraining) {
                currentHp = (currentHp - hitDmg).coerceAtLeast(0)
                val remaining = currentHp
                if (remaining > 0) {
                    val critVoice = if (isCrit) "Bạo kích! " else ""
                    tts?.speak("$count! ${critVoice}Trừ $hitDmg máu, còn $remaining máu", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "hit_$count")
                } else {
                    tts?.speak("Tuyệt vời! Đã hạ gục ${monster.name}!", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "ko_$count")
                }
            } else {
                tts?.speak("$count! Đòn đánh $hitDmg sát thương", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "hit_$count")
            }
        }
    }

    LaunchedEffect(hitEffect) {
        if (hitEffect) {
            delay(600)
            hitEffect = false
            damageText = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(front) { p, w, h -> counter.update(p); frame = PoseFrame(p, w, h) }

        // Vẽ khung xương
        Canvas(Modifier.fillMaxSize()) {
            val f = frame ?: return@Canvas
            val s = min(size.width / f.w, size.height / f.h)
            val ox = (size.width - f.w * s) / 2
            val oy = (size.height - f.h * s) / 2
            fun pt(t: Int): Offset? = f.pose.getPoseLandmark(t)?.takeIf { it.inFrameLikelihood > 0.5f }?.let {
                val x = if (front) f.w - it.position.x else it.position.x
                Offset(ox + x * s, oy + it.position.y * s)
            }
            BONES.forEach { (a, b) ->
                val pa = pt(a); val pb = pt(b)
                if (pa != null && pb != null) drawLine(C.Green, pa, pb, strokeWidth = 8f)
            }
            f.pose.allPoseLandmarks.forEach { l -> pt(l.landmarkType)?.let { drawCircle(C.Orange, 9f, it) } }
        }

        // ====================== PHẦN TRÊN: HEADER & BOSS HEALTH BAR ======================
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Thanh nút điều khiển
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip("✕ Thoát", C.Text, onClick = onClose)
                Text(exercise.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Chip("🔄 Camera", C.Text, onClick = { front = !front })
            }

            Spacer(Modifier.height(8.dp))

            // ====================== THANH MÁU QUÁI VẬT / BOSS HUD ======================
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xD9181530))
                    .border(
                        2.dp,
                        if (hitEffect) Color.Red else if (monster.isBoss) C.Yellow else Color(0xFF3F3B66),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar quái vật có hiệu ứng rung giật khi bị chém trúng
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (hitEffect) Color(0x66FF3366) else Color(0xFF0F0D22))
                            .border(1.dp, if (hitEffect) Color.Red else C.Border, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(monster.avatar, fontSize = 34.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(monster.name, color = C.Text, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            if (monster.isBoss) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "TRÙM",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(C.Yellow)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            } else if (isFreeTraining) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "TỰ DO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(C.Blue)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(monster.title, color = C.Muted, fontSize = 12.sp)
                    }

                    // Điểm số HP hiện tại
                    Text(
                        if (isFreeTraining) "Tự do · ${totalDamageDealt} DMG"
                        else if (isDefeated) "ĐÃ DIỆT 💀"
                        else "$currentHp / $maxHp HP",
                        color = if (isFreeTraining) C.Blue else if (isDefeated) C.Green else if (currentHp < maxHp / 3) Color.Red else C.Pink,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // THANH MÁU (Boss Health Bar)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF2B1522))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(hpProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isFreeTraining) listOf(Color(0xFF4DA8E8), Color(0xFF6B5BFF))
                                    else if (monster.isBoss) listOf(Color(0xFFFF5A7A), Color(0xFFFFC94D))
                                    else listOf(Color(0xFFFF3366), Color(0xFFFF6B35))
                                )
                            )
                    )
                }
            }

            // Popup sát thương nảy lên khi rep hoàn thành
            if (damageText != null) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        damageText!!,
                        color = C.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCCFF3366))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ====================== PHẦN DƯỚI: ĐẾM REP & NÚT NHẬN THƯỞNG ======================
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC0D0B1E))
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (exercise == Exercise.PLANK)
                Text("${counter.holdSeconds}s", color = C.Yellow, fontSize = 60.sp, fontWeight = FontWeight.Black)
            else
                Text("${counter.reps}", color = C.Yellow, fontSize = 68.sp, fontWeight = FontWeight.Black)

            Text(counter.hint, color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("Góc khớp: ${counter.angleNow}° · Sát thương: ${vm.attackDamage} DMG (+${vm.bonusStr} từ trang bị)", color = C.Muted, fontSize = 12.sp)

            Spacer(Modifier.height(14.dp))

            if (isGuildBoss) {
                // CHẾ ĐỘ BOSS THẾ GIỚI BANG HỘI: Mỗi rep đều đóng góp sát thương thực tế
                if (counter.score > 0) {
                    BigButton("🔥 GÓP SÁT THƯƠNG BANG HỘI (-$totalDamageDealt DMG)", C.Orange, Color.White) {
                        vm.attackGuildBoss(
                            score = counter.score,
                            exercise = exercise,
                            damageDealt = totalDamageDealt
                        )
                        onClose()
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đã hoàn thành ${counter.score} rep · Gây $totalDamageDealt sát thương lên Boss Bang Hội",
                        color = Color(0xFFFFD166),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF231E3D))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tập ít nhất 1 rep để góp sát thương diệt trùm!",
                            color = C.Muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Rời trận khiêu chiến",
                        color = Color(0xFF7E789B),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onClose() }
                    )
                }
            } else if (isFreeTraining) {
                // CHẾ ĐỘ TẬP TỰ DO: Không nhận phần thưởng
                BigButton("✅ HOÀN THÀNH TẬP LUYỆN (${counter.score} REP)", C.Blue, Color.White) {
                    vm.finishWorkout(
                        exercise = exercise,
                        score = counter.score,
                        isBoss = false,
                        isFreeTraining = true,
                        monsterStageId = null
                    )
                    onClose()
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tập tự do không nhận thưởng vàng, kinh nghiệm và vật phẩm",
                    color = Color(0xFF7E789B),
                    fontSize = 12.sp
                )
            } else if (isDefeated) {
                // CHỈ KHI ĐÁNH BẠI MỚI NHẬN ĐƯỢC PHẦN THƯỞNG
                BigButton("🎉 K.O! NHẬN CHIẾN LỢI PHẨM", C.Orange, Color.White) {
                    vm.finishWorkout(
                        exercise = exercise,
                        score = counter.score,
                        isBoss = monster.isBoss,
                        isFreeTraining = false,
                        monsterStageId = monster.stageId
                    )
                    onClose()
                }
            }
 else {
                // Chưa hạ gục quái thì không có thưởng
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF231E3D))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚔️ Quái vật còn $currentHp HP (${(hpProgress * 100).toInt()}%) · Đã đánh ${counter.reps} rep",
                            color = C.Muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bỏ chạy (Không nhận thưởng)",
                        color = Color(0xFF7E789B),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onClose() }
                    )
                }
            }
        }
    }
}


@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(
        indication = null,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ) { onClick() })

@Composable
private fun CameraPreview(front: Boolean, onPose: (Pose, Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val detector = remember {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
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
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
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
                preview, analysis
            )
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() } }
    }
    DisposableEffect(Unit) { onDispose { detector.close(); executor.shutdown() } }

    AndroidView({ previewView }, Modifier.fillMaxSize())
}
