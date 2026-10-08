package com.example.vigil.pose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

enum class Exercise(val label: String, val desc: String) {
    PUSHUP("Hít đất", "Đặt điện thoại ngang sàn, quay nghiêng người"),
    SQUAT("Squat", "Đứng nghiêng, thấy toàn thân"),
    SITUP("Gập bụng", "Điện thoại đặt bên cạnh"),
    PLANK("Plank", "Giữ tư thế, đồng hồ tự đếm"),
    PULLUP("Hít xà", "Bạn cần xà đơn"),
    DIP("Dip", "Xà kép, vòng treo hoặc ghế")
}

/**
 * Đếm rep dựa vào góc khớp:
 *  - Mỗi bài có 1 bộ 3 điểm (A-B-C), góc tại B.
 *  - Góc nhỏ hơn [contract] => đã "co" (xuống thấp / gập).
 *  - Sau đó góc lớn hơn [extend] => duỗi ra => +1 rep.
 */
class RepCounter(val exercise: Exercise) {
    var reps by mutableIntStateOf(0); private set
    var holdSeconds by mutableIntStateOf(0); private set
    var hint by mutableStateOf("Đứng vào khung hình"); private set
    var angleNow by mutableIntStateOf(0); private set

    var onRepCompleted: ((Int) -> Unit)? = null

    private var contracted = false
    private val buffer = ArrayDeque<Double>()
    private var holdMs = 0L
    private var lastTs = 0L
    private var lastRepTime = 0L

    /** Điểm dùng để tính thưởng: plank = 1 điểm / 2 giây */
    val score: Int get() = if (exercise == Exercise.PLANK) holdSeconds / 2 else reps

    private data class Cfg(val a: Int, val b: Int, val c: Int, val contract: Double, val extend: Double)

    private fun cfg(left: Boolean): Cfg {
        val L = left
        return when (exercise) {
            Exercise.PUSHUP, Exercise.DIP -> Cfg(
                if (L) PoseLandmark.LEFT_SHOULDER else PoseLandmark.RIGHT_SHOULDER,
                if (L) PoseLandmark.LEFT_ELBOW else PoseLandmark.RIGHT_ELBOW,
                if (L) PoseLandmark.LEFT_WRIST else PoseLandmark.RIGHT_WRIST, 95.0, 155.0
            )
            Exercise.PULLUP -> Cfg(
                if (L) PoseLandmark.LEFT_SHOULDER else PoseLandmark.RIGHT_SHOULDER,
                if (L) PoseLandmark.LEFT_ELBOW else PoseLandmark.RIGHT_ELBOW,
                if (L) PoseLandmark.LEFT_WRIST else PoseLandmark.RIGHT_WRIST, 75.0, 150.0
            )
            Exercise.SQUAT -> Cfg(
                if (L) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP,
                if (L) PoseLandmark.LEFT_KNEE else PoseLandmark.RIGHT_KNEE,
                if (L) PoseLandmark.LEFT_ANKLE else PoseLandmark.RIGHT_ANKLE, 105.0, 160.0
            )
            Exercise.SITUP -> Cfg(
                if (L) PoseLandmark.LEFT_SHOULDER else PoseLandmark.RIGHT_SHOULDER,
                if (L) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP,
                if (L) PoseLandmark.LEFT_KNEE else PoseLandmark.RIGHT_KNEE, 80.0, 115.0
            )
            Exercise.PLANK -> Cfg(
                if (L) PoseLandmark.LEFT_SHOULDER else PoseLandmark.RIGHT_SHOULDER,
                if (L) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP,
                if (L) PoseLandmark.LEFT_ANKLE else PoseLandmark.RIGHT_ANKLE, 0.0, 160.0
            )
        }
    }

    private fun angle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val r = atan2((c.position.y - b.position.y).toDouble(), (c.position.x - b.position.x).toDouble()) -
                atan2((a.position.y - b.position.y).toDouble(), (a.position.x - b.position.x).toDouble())
        var d = abs(Math.toDegrees(r))
        if (d > 180) d = 360 - d
        return d
    }

    private fun visible(vararg l: PoseLandmark?) = l.all { it != null && it.inFrameLikelihood > 0.5f }

    fun update(pose: Pose) {
        // chọn bên trái/phải có độ tin cậy cao hơn
        val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.inFrameLikelihood ?: 0f
        val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.inFrameLikelihood ?: 0f
        val c = cfg(ls >= rs)
        val a = pose.getPoseLandmark(c.a)
        val b = pose.getPoseLandmark(c.b)
        val d = pose.getPoseLandmark(c.c)
        if (!visible(a, b, d)) {
            hint = "Hãy để cả người vào khung hình"
            lastTs = 0L
            return
        }
        // góc làm mượt (trung bình 3 khung)
        buffer.addLast(angle(a!!, b!!, d!!))
        if (buffer.size > 3) buffer.removeFirst()
        val ang = buffer.average()
        angleNow = ang.toInt()

        if (exercise == Exercise.PLANK) {
            val horizontal = abs(a.position.y - b.position.y) < abs(a.position.x - b.position.x)
            val now = System.currentTimeMillis()
            if (ang > c.extend && horizontal) {
                if (lastTs != 0L) holdMs += (now - lastTs).coerceAtMost(500)
                lastTs = now
                val oldScore = holdSeconds / 2
                holdSeconds = (holdMs / 1000).toInt()
                val newScore = holdSeconds / 2
                if (newScore > oldScore && newScore > 0) {
                    onRepCompleted?.invoke(newScore)
                }
                hint = "Giữ vững! Lưng thẳng"
            } else {
                lastTs = 0L
                hint = "Giữ thẳng người từ vai tới gót"
            }
            return
        }

        if (exercise == Exercise.PUSHUP) {
            val hip = pose.getPoseLandmark(
                if (c.a == PoseLandmark.LEFT_SHOULDER) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP
            )
            // thân người phải nằm ngang (vai và hông gần cùng độ cao)
            if (hip != null && abs(a.position.y - hip.position.y) > abs(a.position.x - hip.position.x)) {
                hint = "Vào tư thế chống đẩy (quay nghiêng người)"
                return
            }
        }

        when {
            ang < c.contract && !contracted -> {
                contracted = true
                hint = "Tốt! Giờ đẩy lên"
            }
            ang > c.extend && contracted -> {
                val now = System.currentTimeMillis()
                if (now - lastRepTime >= 600) {
                    contracted = false
                    reps++
                    lastRepTime = now
                    hint = "+1 rep!"
                    onRepCompleted?.invoke(reps)
                }
            }
            !contracted -> hint = "Hạ xuống thấp hơn"
        }
    }
}
