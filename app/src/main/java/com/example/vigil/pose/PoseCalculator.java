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
    private double baselineShoulderY = -1.0;
    private double baselineShoulderSpan = -1.0;

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
                        112.0, 145.0
                );
            case PULLUP:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_ELBOW : PoseLandmark.RIGHT_ELBOW,
                        L ? PoseLandmark.LEFT_WRIST : PoseLandmark.RIGHT_WRIST,
                        95.0, 142.0
                );
            case SQUAT:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        L ? PoseLandmark.LEFT_ANKLE : PoseLandmark.RIGHT_ANKLE,
                        120.0, 152.0
                );
            case SITUP:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        98.0, 125.0
                );
            case PLANK:
            default:
                return new PoseConfig(
                        L ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        L ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        L ? PoseLandmark.LEFT_ANKLE : PoseLandmark.RIGHT_ANKLE,
                        0.0, 145.0
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
            if (l == null || l.getInFrameLikelihood() <= 0.22f) {
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

        boolean preferLeft = ls >= rs;
        PoseConfig cfg = getConfig(exercise, preferLeft);
        PoseLandmark a = pose.getPoseLandmark(cfg.a);
        PoseLandmark b = pose.getPoseLandmark(cfg.b);
        PoseLandmark d = pose.getPoseLandmark(cfg.c);

        // Nếu nửa người bên ưu tiên bị khuất, tự động thử nửa người bên kia
        if (!isVisible(a, b, d)) {
            PoseConfig altCfg = getConfig(exercise, !preferLeft);
            PoseLandmark altA = pose.getPoseLandmark(altCfg.a);
            PoseLandmark altB = pose.getPoseLandmark(altCfg.b);
            PoseLandmark altD = pose.getPoseLandmark(altCfg.c);
            if (isVisible(altA, altB, altD)) {
                cfg = altCfg;
                a = altA;
                b = altB;
                d = altD;
            }
        }

        // Fallback thông minh khi đặt điện thoại trên bàn/sàn không lấy hết cổ chân hoặc cổ tay
        if (!isVisible(a, b, d)) {
            if (exercise == Exercise.SQUAT) {
                PoseConfig hipCfg = new PoseConfig(
                        preferLeft ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        preferLeft ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        preferLeft ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        122.0, 152.0
                );
                PoseLandmark ha = pose.getPoseLandmark(hipCfg.a);
                PoseLandmark hb = pose.getPoseLandmark(hipCfg.b);
                PoseLandmark hd = pose.getPoseLandmark(hipCfg.c);
                if (isVisible(ha, hb, hd)) {
                    cfg = hipCfg;
                    a = ha;
                    b = hb;
                    d = hd;
                }
            } else if (exercise == Exercise.PLANK) {
                PoseConfig plankKneeCfg = new PoseConfig(
                        preferLeft ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER,
                        preferLeft ? PoseLandmark.LEFT_HIP : PoseLandmark.RIGHT_HIP,
                        preferLeft ? PoseLandmark.LEFT_KNEE : PoseLandmark.RIGHT_KNEE,
                        0.0, 140.0
                );
                PoseLandmark pa = pose.getPoseLandmark(plankKneeCfg.a);
                PoseLandmark pb = pose.getPoseLandmark(plankKneeCfg.b);
                PoseLandmark pd = pose.getPoseLandmark(plankKneeCfg.c);
                if (isVisible(pa, pb, pd)) {
                    cfg = plankKneeCfg;
                    a = pa;
                    b = pb;
                    d = pd;
                }
            }
        }

        if (!isVisible(a, b, d)) {
            // Fallback 2: Khi người chơi đặt điện thoại trước mặt (Video Call) chỉ thấy đầu & 2 vai (cổ tay/cổ chân nằm ngoài khung hình)
            if (isVisible(leftShoulder, rightShoulder)) {
                double midY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y) * 0.5;
                double span = Math.hypot(
                        leftShoulder.getPosition().x - rightShoulder.getPosition().x,
                        leftShoulder.getPosition().y - rightShoulder.getPosition().y
                );
                if (span > 8.0) {
                    if (baselineShoulderY < 0.0 || midY < baselineShoulderY) {
                        baselineShoulderY = midY;
                    } else {
                        baselineShoulderY = baselineShoulderY * 0.992 + midY * 0.008;
                    }
                    if (baselineShoulderSpan < 0.0 || span < baselineShoulderSpan) {
                        baselineShoulderSpan = span;
                    } else {
                        baselineShoulderSpan = baselineShoulderSpan * 0.992 + span * 0.008;
                    }

                    double dropRatio = (midY - baselineShoulderY) / Math.max(span, 20.0);
                    double zoomRatio = (span - baselineShoulderSpan) / Math.max(baselineShoulderSpan, 20.0);
                    double motionRatio = Math.max(dropRatio / 0.22, zoomRatio / 0.16);
                    double clamped = Math.max(0.0, Math.min(1.0, motionRatio));
                    double virtualAngle = 165.0 - (clamped * 75.0); // 165° (đứng/đẩy thẳng) -> 90° (hạ người xuống)
                    angleNow = (int) virtualAngle;

                    if (exercise == Exercise.PLANK) {
                        long now = System.currentTimeMillis();
                        if (lastTs != 0L) {
                            holdMs += Math.min(now - lastTs, 500L);
                        }
                        lastTs = now;
                        int oldScore = holdSeconds / 2;
                        holdSeconds = (int) (holdMs / 1000L);
                        int newScore = holdSeconds / 2;
                        if (newScore > oldScore && newScore > 0 && onRepListener != null) {
                            onRepListener.onRep(newScore);
                        }
                        hint = "Giữ vững tư thế Plank!";
                        return;
                    }

                    if (virtualAngle <= 112.0 && !contracted) {
                        contracted = true;
                        hint = "Tốt! Giờ đẩy/nâng người lên";
                    } else if (virtualAngle >= 145.0 && contracted) {
                        long now = System.currentTimeMillis();
                        if (now - lastRepTime >= 380L) {
                            contracted = false;
                            reps++;
                            lastRepTime = now;
                            hint = "+1 rep!";
                            if (onRepListener != null) {
                                onRepListener.onRep(reps);
                            }
                        }
                    } else if (!contracted) {
                        hint = "Hạ người xuống (" + angleNow + "°)";
                    } else {
                        hint = "Đẩy/nâng người lên (" + angleNow + "°)";
                    }
                    return;
                }
            }

            hint = "Hãy để người vào khung hình";
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
            long now = System.currentTimeMillis();
            if (ang > cfg.extend) {
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
                hint = "Giữ thẳng người từ vai tới hông";
            }
            return;
        }

        if (ang <= cfg.contract && !contracted) {
            contracted = true;
            hint = "Tốt! Giờ đẩy/nâng người lên";
        } else if (ang >= cfg.extend && contracted) {
            long now = System.currentTimeMillis();
            if (now - lastRepTime >= 380L) {
                contracted = false;
                reps++;
                lastRepTime = now;
                hint = "+1 rep!";
                if (onRepListener != null) {
                    onRepListener.onRep(reps);
                }
            }
        } else if (!contracted) {
            hint = "Hạ xuống thêm chút nữa (" + angleNow + "° -> " + (int) cfg.contract + "°)";
        } else {
            hint = "Nâng thẳng lên (" + angleNow + "° -> " + (int) cfg.extend + "°)";
        }
    }
}
