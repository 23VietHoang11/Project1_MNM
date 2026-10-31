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
    private boolean readyForRep = false;
    private int contractStreak = 0;
    private int extendStreak = 0;

    private final Deque<Double> buffer = new ArrayDeque<>();
    private long holdMs = 0L;
    private long lastTs = 0L;
    private long lastRepTime = 0L;
    private long contractedAtMs = 0L;

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
                        80.0, 125.0
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
            if (l == null || l.getInFrameLikelihood() <= 0.55f) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidLimbLength(PoseLandmark a, PoseLandmark b, PoseLandmark c) {
        double ab = Math.hypot(a.getPosition().x - b.getPosition().x, a.getPosition().y - b.getPosition().y);
        double bc = Math.hypot(c.getPosition().x - b.getPosition().x, c.getPosition().y - b.getPosition().y);
        return ab >= 8.0 && bc >= 8.0;
    }

    public void processPose(Pose pose) {
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        float ls = (leftShoulder != null) ? leftShoulder.getInFrameLikelihood() : 0f;
        float rs = (rightShoulder != null) ? rightShoulder.getInFrameLikelihood() : 0f;

        boolean preferLeft = ls >= rs;
        PoseConfig cfg = getConfig(exercise, preferLeft);
        PoseLandmark a = pose.getPoseLandmark(cfg.a);
        PoseLandmark b = pose.getPoseLandmark(cfg.b);
        PoseLandmark d = pose.getPoseLandmark(cfg.c);

        if (!isVisible(a, b, d) || !hasValidLimbLength(a, b, d)) {
            PoseConfig altCfg = getConfig(exercise, !preferLeft);
            PoseLandmark altA = pose.getPoseLandmark(altCfg.a);
            PoseLandmark altB = pose.getPoseLandmark(altCfg.b);
            PoseLandmark altD = pose.getPoseLandmark(altCfg.c);
            if (isVisible(altA, altB, altD) && hasValidLimbLength(altA, altB, altD)) {
                cfg = altCfg;
                a = altA;
                b = altB;
                d = altD;
            } else {
                buffer.clear();
                contractStreak = 0;
                extendStreak = 0;
                hint = "Hãy để cả người vào khung hình";
                lastTs = 0L;
                return;
            }
        }

        // Kiểm tra tư thế chuẩn theo từng bài tập để tránh vung tay / di chuyển nhẹ bị tính nhầm rep
        if (exercise == Exercise.PUSHUP || exercise == Exercise.DIP) {
            // Khi chống đẩy hoặc Dip, cổ tay chống dưới sàn/xà phải nằm thấp hơn khuỷu tay và vai trên khung hình
            if (d.getPosition().y <= b.getPosition().y || d.getPosition().y <= a.getPosition().y) {
                contractStreak = 0;
                extendStreak = 0;
                hint = "Chống hai tay xuống sàn để bắt đầu";
                return;
            }
        } else if (exercise == Exercise.PULLUP) {
            // Khi kéo xà, cổ tay nắm xà phải nằm cao hơn khuỷu tay
            if (d.getPosition().y >= b.getPosition().y) {
                contractStreak = 0;
                extendStreak = 0;
                hint = "Hai tay nắm xà phía trên đầu";
                return;
            }
        } else if (exercise == Exercise.SQUAT) {
            // Khi Squat, mắt cá chân phải thấp hơn đầu gối và đầu gối thấp hơn hông
            if (d.getPosition().y <= b.getPosition().y || b.getPosition().y < a.getPosition().y - 10f) {
                contractStreak = 0;
                extendStreak = 0;
                hint = "Đứng thẳng toàn thân trong khung hình";
                return;
            }
        }

        buffer.addLast(calculateAngle(a, b, d));
        if (buffer.size() > 5) {
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

        // Phải vào tư thế chuẩn bị (duỗi thẳng tay/chân) trước khi bắt đầu tính chu kỳ rep
        if (!readyForRep) {
            if (ang >= cfg.extend - 8.0) {
                extendStreak++;
                if (extendStreak >= 2) {
                    readyForRep = true;
                    extendStreak = 0;
                }
            } else {
                extendStreak = 0;
            }
            hint = "Duỗi thẳng vào tư thế chuẩn bị (" + angleNow + "°)";
            return;
        }

        if (!contracted) {
            if (ang < cfg.contract) {
                contractStreak++;
                if (contractStreak >= 2) {
                    contracted = true;
                    contractStreak = 0;
                    extendStreak = 0;
                    contractedAtMs = System.currentTimeMillis();
                    hint = "Tốt! Giờ đẩy/nâng thẳng lên";
                }
            } else {
                contractStreak = 0;
                hint = "Hạ xuống thấp hơn (" + angleNow + "° -> " + (int) cfg.contract + "°)";
            }
        } else {
            if (ang > cfg.extend) {
                extendStreak++;
                if (extendStreak >= 2) {
                    long now = System.currentTimeMillis();
                    if (now - contractedAtMs >= 300L && now - lastRepTime >= 850L) {
                        contracted = false;
                        contractStreak = 0;
                        extendStreak = 0;
                        reps++;
                        lastRepTime = now;
                        hint = "+1 rep!";
                        if (onRepListener != null) {
                            onRepListener.onRep(reps);
                        }
                    }
                }
            } else {
                extendStreak = 0;
                hint = "Đẩy/nâng thẳng hết cỡ (" + angleNow + "° -> " + (int) cfg.extend + "°)";
            }
        }
    }
}
