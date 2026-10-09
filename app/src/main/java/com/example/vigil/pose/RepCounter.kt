package com.example.vigil.pose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.mlkit.vision.pose.Pose

/**
 * RepCounter kết nối bộ tính toán tư thế Java [PoseCalculator] với Jetpack Compose state.
 */
class RepCounter(val exercise: Exercise) {
    private val calculator = PoseCalculator(exercise)

    var reps by mutableIntStateOf(0); private set
    var holdSeconds by mutableIntStateOf(0); private set
    var hint by mutableStateOf("Đứng vào khung hình"); private set
    var angleNow by mutableIntStateOf(0); private set

    var onRepCompleted: ((Int) -> Unit)? = null

    init {
        calculator.setOnRepListener { count ->
            onRepCompleted?.invoke(count)
        }
    }

    /** Điểm dùng để tính thưởng: plank = 1 điểm / 2 giây */
    val score: Int get() = calculator.score

    fun update(pose: Pose) {
        calculator.processPose(pose)
        reps = calculator.reps
        holdSeconds = calculator.holdSeconds
        hint = calculator.hint
        angleNow = calculator.angleNow
    }
}
