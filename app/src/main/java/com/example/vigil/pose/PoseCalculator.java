package com.example.vigil.pose;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import java.util.ArrayDeque;
import java.util.Deque;

public class PoseCalculator {

    public static class PoseConfig {
        public final int a;
        public final int b;
        public final int c;
        public final double contract;
        public final double extend;

        public PoseConfig(int a, int b, int c, double contract, double extend) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.contract = contract;
            this.extend = extend;
        }
    }

    public interface OnRepListener {
        void onRep(int count);
    }

    private final Exercise exercise;
    private int reps = 0;
    private int holdSeconds = 0;
    private String hint = "Đứng vào khung hình";
    private int angleNow = 0;
    private boolean contracted = false;

    private final Deque<Double> buffer = new ArrayDeque<>();
    private long holdMs = 0L;
    private long lastTs = 0L;
    private long lastRepTime = 0L;

    private OnRepListener onRepListener;

    public PoseCalculator(Exercise exercise) {
        this.exercise = exercise;
    }

    public void setOnRepListener(OnRepListener listener) {
        this.onRepListener = listener;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getReps() {
        return reps;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public String getHint() {
        return hint;
    }

    public int getAngleNow() {
        return angleNow;
    }

    public int getScore() {
        return (exercise == Exercise.PLANK) ? holdSeconds / 2 : reps;
    }

    public static PoseConfig getConfig(Exercise exercise, boolean left) {
        boolean L = left;
        switch (exercise) {
            case PUSHUP:
            case DIP:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_ELBOW : PoseLandmark.RIGHT_ELBOW,
                        L ? PoseLandmark.LEFT_WRIST : PoseLandmark.RIGHT_WRIST,
                        95.0, 155.0
                );
            case PULLUP:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_ELBOW : PoseLandmark.RIGHT_ELBOW,
                        L ? PoseLandmark.LEFT_WRIST : PoseLandmark.RIGHT_WRIST,
                        75.0, 150.0
                );
            case SQUAT:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        L ? PoseLandmark.LEFT_ANKLE : PoseLandmark.RIGHT_ANKLE,
                        105.0, 160.0
                );
            case SITUP:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        80.0, 115.0
                );
            case PLANK:
            default:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_ANKLE : PoseLandmark.RIGHT_ANKLE,
                        0.0, 160.0
                );
        }
    }

    public static double calculateAngle(PoseLandmark a, PoseLandmark b, PoseLandmark c) {
        double r = Math.atan2((double) (c.getPosition().y - b.getPosition().y), (double) (c.getPosition().x - b.getPosition().x)) -
                Math.atan2((double) (a.getPosition().y - b.getPosition().y), (double) (a.getPosition().x - b.getPosition().x));
        double d = Math.abs(Math.toDegrees(r));
        if (d > 180.0) {
            d = 360.0 - d;
        }
        return d;
    }

    public static boolean isVisible(PoseLandmark... landmarks) {
        for (PoseLandmark l : landmarks) {
            if (l == null || l.getInFrameLikelihood() <= 0.5f) {
                return false;
            }
        }
        return true;
    }

    public void processPose(Pose pose) {
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        float ls = (leftShoulder != null) ? leftShoulder.getInFrameLikelihood() : 0f;
        float rs = (rightShoulder != null) ? rightShoulder.getInFrameLikelihood() : 0f;

        PoseConfig cfg = getConfig(exercise, ls >= rs);
        PoseLandmark a = pose.getPoseLandmark(cfg.a);
        PoseLandmark b = pose.getPoseLandmark(cfg.b);
        PoseLandmark d = pose.getPoseLandmark(cfg.c);

        if (!isVisible(a, b, d)) {
            hint = "Hãy để cả người vào khung hình";
            lastTs = 0L;
            return;
        }

        buffer.addLast(calculateAngle(a, b, d));
        if (buffer.size() > 3) {
            buffer.removeFirst();
        }

        double sum = 0.0;
        for (double val : buffer) {
            sum += val;
        }
        double ang = sum / buffer.size();
        angleNow = (int) ang;

        if (exercise == Exercise.PLANK) {
            boolean horizontal = Math.abs(a.getPosition().y - b.getPosition().y) < Math.abs(a.getPosition().x - b.getPosition().x);
            long now = System.currentTimeMillis();
            if (ang > cfg.extend && horizontal) {
                if (lastTs != 0L) {
                    holdMs += Math.min(now - lastTs, 500L);
                }
                lastTs = now;
                int oldScore = holdSeconds / 2;
                holdSeconds = (int) (holdMs / 1000L);
                int newScore = holdSeconds / 2;
                if (newScore > oldScore && newScore > 0) {
                    if (onRepListener != null) {
                        onRepListener.onRep(newScore);
                    }
                }
                hint = "Giữ vững! Lưng thẳng";
            } else {
                lastTs = 0L;
                hint = "Giữ thẳng người từ vai tới gót";
            }
            return;
        }

        if (exercise == Exercise.PUSHUP) {
            PoseLandmark hip = pose.getPoseLandmark(
                    (cfg.a == PoseLandmark.LEFT_SHOULDER) ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP
            );
            if (hip != null && Math.abs(a.getPosition().y - hip.getPosition().y) > Math.abs(a.getPosition().x - hip.getPosition().x)) {
                hint = "Vào tư thế chống đẩy (quay nghiêng người)";
                return;
            }
        }

        if (ang < cfg.contract && !contracted) {
            contracted = true;
            hint = "Tốt! Giờ đẩy lên";
        } else if (ang > cfg.extend && contracted) {
            long now = System.currentTimeMillis();
            if (now - lastRepTime >= 600L) {
                contracted = false;
                reps++;
                lastRepTime = now;
                hint = "+1 rep!";
                if (onRepListener != null) {
                    onRepListener.onRep(reps);
                }
            }
        } else if (!contracted) {
            hint = "Hạ xuống thấp hơn";
        }
    }
}
