package com.example.vigil.model;

import com.example.vigil.pose.Exercise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdventureData {
    public static final List<Chapter> CHAPTERS;

    static {
        List<Chapter> chapters = new ArrayList<>();

        // Chương 1
        chapters.add(new Chapter(
                1,
                "Đồng Cỏ Ngập Nắng",
                "🌿",
                "Nơi các tân binh rèn luyện bước đầu trước khi tiến vào hiểm địa.",
                Arrays.asList(
                        new Monster(1, "Slime Gai Nhọn", "Quái Bụi Cỏ", "🟢", 160, Exercise.PUSHUP, false, 80, 16, "Sinh vật dẻo dai thường nấp trong bụi rậm. Cần tung những cú chống đẩy dứt khoát để đè bẹp nó."),
                        new Monster(2, "Sói Xám Đồng Cỏ", "Kẻ Săn Đêm", "🐺", 280, Exercise.SQUAT, false, 140, 25, "Tốc độ nhanh như gió. Hạ thấp trọng tâm bằng các rep squat chuẩn xác để né đòn cắn xé."),
                        new Monster(3, "Yêu Tinh Cung Thủ", "Thiện Xạ Bụi Cây", "👺", 420, Exercise.SITUP, false, 180, 32, "Bắn tên liên tiếp tầm thấp. Gập bụng dồn dập để tránh mưa tên và áp sát tiêu diệt nó."),
                        new Monster(4, "Hộ Vệ Đồng Cỏ", "TRÙM ĐỒNG CỎ", "🗿", 700, Exercise.PUSHUP, true, 350, 60, "Bức tượng khổng lồ canh giữ lối vào Rừng Già. Cần lực đẩy dũng mãnh và trang bị vững chắc để phá vỡ lớp đá giáp!", Rarity.EPIC, 0xFFFFC94DL)
                )
        ));

        // Chương 2
        chapters.add(new Chapter(
                2,
                "Rừng Già Hắc Ám",
                "🌲",
                "Khu rừng rậm rạp chìm trong sương độc và tà thuật cổ xưa.",
                Arrays.asList(
                        new Monster(5, "Nhện Độc Gai Đen", "Bóng Đen Giăng Tơ", "🕷️", 1000, Exercise.PUSHUP, false, 190, 35, "Giăng bẫy tơ độc trên cành cây. Cần hít đất liên hoàn để thoát khỏi mạng tơ dính và phá hủy tổ nhện."),
                        new Monster(6, "Quỷ Lùn Đầm Lầy", "Kẻ Đào Trộm Mộ", "🧟", 1400, Exercise.SQUAT, false, 220, 40, "Dưới bùn lầy lún sâu, đôi chân bạn phải vững chãi qua từng rep squat để đè bẹp nó."),
                        new Monster(7, "Ma Cây Cổ Thụ", "TRÙM RỪNG GIÀ", "🌳", 2200, Exercise.PLANK, true, 500, 85, "Rễ cây bóp nghẹt mặt đất. Giữ vững tư thế Plank kiên định để chống lại sức ép ngàn cân!", Rarity.EPIC, 0xFF9B7BFFL)
                )
        ));

        // Chương 3
        chapters.add(new Chapter(
                3,
                "Hẻm Núi Nham Thạch",
                "🌋",
                "Dòng dung nham cuộn trào nơi ngự trị của những quái vật hỏa ngục.",
                Arrays.asList(
                        new Monster(8, "Hỏa Lang Huyết Ngục", "Ngọn Lửa Săn Mồi", "🔥", 2800, Exercise.PUSHUP, false, 280, 50, "Lớp lông bốc cháy rừng rực. Đòi hỏi từng cú chống đẩy dứt khoát không ngơi nghỉ để dập tắt ngọn lửa."),
                        new Monster(9, "Golem Dung Nham", "Núi Lửa Di Động", "🌋", 4000, Exercise.SQUAT, false, 360, 65, "Mỗi bước đi chấn động dung nham. Chân trụ thép qua các rep squat dũng mãnh mới đánh bại được."),
                        new Monster(10, "Hắc Long Nham Thạch", "CHÚA TỂ HỎA NGỤC", "🐉", 6500, Exercise.PUSHUP, true, 800, 150, "Rồng thần ngủ say vạn năm. Đập tan đôi cánh lửa bằng chuỗi đòn chống đẩy thần thánh!", Rarity.LEGENDARY, 0xFFFF5A7AL)
                )
        ));

        // Chương 4
        chapters.add(new Chapter(
                4,
                "Tháp Băng Vực Thẳm",
                "❄️",
                "Đỉnh tháp băng giá vĩnh cửu, nơi chỉ huyền thoại mới có thể đặt chân.",
                Arrays.asList(
                        new Monster(11, "Hồn Ma Băng Giá", "Bóng Tối Đóng Băng", "👻", 7500, Exercise.SITUP, false, 450, 80, "Cơn gió buốt thấu xương. Gập bụng dồn dập để giữ ấm ngọn lửa chiến binh trong lòng."),
                        new Monster(12, "Chúa Tể Băng Cổ Đại", "BÁ CHỦ TUYỆT ĐỐI", "👑", 12000, Exercise.PUSHUP, true, 1200, 250, "Trùm cuối của thế giới Vigil! Chinh phục thử thách đỉnh cao để phong vương Huyền Thoại!", Rarity.LEGENDARY, 0xFF4DA8E8L)
                )
        ));

        CHAPTERS = Collections.unmodifiableList(chapters);
    }

    public static Monster getMonsterByStage(int stageId) {
        for (Chapter ch : CHAPTERS) {
            for (Monster m : ch.getMonsters()) {
                if (m.getStageId() == stageId) {
                    return m;
                }
            }
        }
        return CHAPTERS.get(0).getMonsters().get(0);
    }
}
