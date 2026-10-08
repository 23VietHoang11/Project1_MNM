package com.example.vigil.model

import androidx.compose.ui.graphics.Color
import com.example.vigil.pose.Exercise

data class Monster(
    val stageId: Int,
    val name: String,
    val title: String,
    val avatar: String, // Emoji hoặc icon quái vật
    val maxHp: Int, // Số rep cần để tiêu diệt
    val exercise: Exercise,
    val isBoss: Boolean = false,
    val expReward: Int,
    val goldReward: Int,
    val lore: String,
    val dropRarity: Rarity = Rarity.RARE,
    val color: Color = Color(0xFFFF5A7A)
)

data class Chapter(
    val id: Int,
    val name: String,
    val icon: String,
    val description: String,
    val monsters: List<Monster>
)

object AdventureData {
    val CHAPTERS = listOf(
        Chapter(
            id = 1,
            name = "Đồng Cỏ Ngập Nắng",
            icon = "🌿",
            description = "Nơi các tân binh rèn luyện bước đầu trước khi tiến vào hiểm địa.",
            monsters = listOf(
                Monster(
                    stageId = 1,
                    name = "Slime Gai Nhọn",
                    title = "Quái Bụi Cỏ",
                    avatar = "🟢",
                    maxHp = 160,
                    exercise = Exercise.PUSHUP,
                    isBoss = false,
                    expReward = 80,
                    goldReward = 16,
                    lore = "Sinh vật dẻo dai thường nấp trong bụi rậm. Cần tung những cú chống đẩy dứt khoát để đè bẹp nó."
                ),
                Monster(
                    stageId = 2,
                    name = "Sói Xám Đồng Cỏ",
                    title = "Kẻ Săn Đêm",
                    avatar = "🐺",
                    maxHp = 280,
                    exercise = Exercise.SQUAT,
                    isBoss = false,
                    expReward = 140,
                    goldReward = 25,
                    lore = "Tốc độ nhanh như gió. Hạ thấp trọng tâm bằng các rep squat chuẩn xác để né đòn cắn xé."
                ),
                Monster(
                    stageId = 3,
                    name = "Yêu Tinh Cung Thủ",
                    title = "Thiện Xạ Bụi Cây",
                    avatar = "👺",
                    maxHp = 420,
                    exercise = Exercise.SITUP,
                    isBoss = false,
                    expReward = 180,
                    goldReward = 32,
                    lore = "Bắn tên liên tiếp tầm thấp. Gập bụng dồn dập để tránh mưa tên và áp sát tiêu diệt nó."
                ),
                Monster(
                    stageId = 4,
                    name = "Hộ Vệ Đồng Cỏ",
                    title = "TRÙM ĐỒNG CỎ",
                    avatar = "🗿",
                    maxHp = 700,
                    exercise = Exercise.PUSHUP,
                    isBoss = true,
                    expReward = 350,
                    goldReward = 60,
                    lore = "Bức tượng khổng lồ canh giữ lối vào Rừng Già. Cần lực đẩy dũng mãnh và trang bị vững chắc để phá vỡ lớp đá giáp!",
                    dropRarity = Rarity.EPIC,
                    color = Color(0xFFFFC94D)
                )
            )
        ),
        Chapter(
            id = 2,
            name = "Rừng Già Hắc Ám",
            icon = "🌲",
            description = "Khu rừng rậm rạp chìm trong sương độc và tà thuật cổ xưa.",
            monsters = listOf(
                Monster(
                    stageId = 5,
                    name = "Nhện Độc Gai Đen",
                    title = "Bóng Đen Giăng Tơ",
                    avatar = "🕷️",
                    maxHp = 1000,
                    exercise = Exercise.PUSHUP,
                    isBoss = false,
                    expReward = 190,
                    goldReward = 35,
                    lore = "Giăng bẫy tơ độc trên cành cây. Cần hít đất liên hoàn để thoát khỏi mạng tơ dính và phá hủy tổ nhện."
                ),
                Monster(
                    stageId = 6,
                    name = "Quỷ Lùn Đầm Lầy",
                    title = "Kẻ Đào Trộm Mộ",
                    avatar = "🧟",
                    maxHp = 1400,
                    exercise = Exercise.SQUAT,
                    isBoss = false,
                    expReward = 220,
                    goldReward = 40,
                    lore = "Dưới bùn lầy lún sâu, đôi chân bạn phải vững chãi qua từng rep squat để đè bẹp nó."
                ),
                Monster(
                    stageId = 7,
                    name = "Ma Cây Cổ Thụ",
                    title = "TRÙM RỪNG GIÀ",
                    avatar = "🌳",
                    maxHp = 2200,
                    exercise = Exercise.PLANK,
                    isBoss = true,
                    expReward = 500,
                    goldReward = 85,
                    lore = "Rễ cây bóp nghẹt mặt đất. Giữ vững tư thế Plank kiên định để chống lại sức ép ngàn cân!",
                    dropRarity = Rarity.EPIC,
                    color = Color(0xFF9B7BFF)
                )
            )
        ),
        Chapter(
            id = 3,
            name = "Hẻm Núi Nham Thạch",
            icon = "🌋",
            description = "Dòng dung nham cuộn trào nơi ngự trị của những quái vật hỏa ngục.",
            monsters = listOf(
                Monster(
                    stageId = 8,
                    name = "Hỏa Lang Huyết Ngục",
                    title = "Ngọn Lửa Săn Mồi",
                    avatar = "🔥",
                    maxHp = 2800,
                    exercise = Exercise.PUSHUP,
                    isBoss = false,
                    expReward = 280,
                    goldReward = 50,
                    lore = "Lớp lông bốc cháy rừng rực. Đòi hỏi từng cú chống đẩy dứt khoát không ngơi nghỉ để dập tắt ngọn lửa."
                ),
                Monster(
                    stageId = 9,
                    name = "Golem Dung Nham",
                    title = "Núi Lửa Di Động",
                    avatar = "🌋",
                    maxHp = 4000,
                    exercise = Exercise.SQUAT,
                    isBoss = false,
                    expReward = 360,
                    goldReward = 65,
                    lore = "Mỗi bước đi chấn động dung nham. Chân trụ thép qua các rep squat dũng mãnh mới đánh bại được."
                ),
                Monster(
                    stageId = 10,
                    name = "Hắc Long Nham Thạch",
                    title = "CHÚA TỂ HỎA NGỤC",
                    avatar = "🐉",
                    maxHp = 6500,
                    exercise = Exercise.PUSHUP,
                    isBoss = true,
                    expReward = 800,
                    goldReward = 150,
                    lore = "Rồng thần ngủ say vạn năm. Đập tan đôi cánh lửa bằng chuỗi đòn chống đẩy thần thánh!",
                    dropRarity = Rarity.LEGENDARY,
                    color = Color(0xFFFF5A7A)
                )
            )
        ),
        Chapter(
            id = 4,
            name = "Tháp Băng Vực Thẳm",
            icon = "❄️",
            description = "Đỉnh tháp băng giá vĩnh cửu, nơi chỉ huyền thoại mới có thể đặt chân.",
            monsters = listOf(
                Monster(
                    stageId = 11,
                    name = "Hồn Ma Băng Giá",
                    title = "Bóng Tối Đóng Băng",
                    avatar = "👻",
                    maxHp = 7500,
                    exercise = Exercise.SITUP,
                    isBoss = false,
                    expReward = 450,
                    goldReward = 80,
                    lore = "Cơn gió buốt thấu xương. Gập bụng dồn dập để giữ ấm ngọn lửa chiến binh trong lòng."
                ),
                Monster(
                    stageId = 12,
                    name = "Chúa Tể Băng Cổ Đại",
                    title = "BÁ CHỦ TUYỆT ĐỐI",
                    avatar = "👑",
                    maxHp = 12000,
                    exercise = Exercise.PUSHUP,
                    isBoss = true,
                    expReward = 1200,
                    goldReward = 250,
                    lore = "Trùm cuối của thế giới Vigil! Chinh phục thử thách đỉnh cao để phong vương Huyền Thoại!",
                    dropRarity = Rarity.LEGENDARY,
                    color = Color(0xFF4DA8E8)
                )
            )
        )
    )

    fun getMonsterByStage(stageId: Int): Monster {
        for (ch in CHAPTERS) {
            for (m in ch.monsters) {
                if (m.stageId == stageId) return m
            }
        }
        // Fallback default
        return CHAPTERS[0].monsters[0]
    }
}
