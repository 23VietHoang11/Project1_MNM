package com.example.vigil.pose;

public enum Exercise {
    PUSHUP("Hít đất", "Đặt điện thoại ngang sàn, quay nghiêng người"),
    SQUAT("Squat", "Đứng nghiêng, thấy toàn thân"),
    SITUP("Gập bụng", "Điện thoại đặt bên cạnh"),
    PLANK("Plank", "Giữ tư thế, đồng hồ tự đếm"),
    PULLUP("Hít xà", "Bạn cần xà đơn"),
    DIP("Dip", "Xà kép, vòng treo hoặc ghế");

    private final String label;
    private final String desc;

    Exercise(String label, String desc) {
        this.label = label;
        this.desc = desc;
    }

    public String getLabel() {
        return label;
    }

    public String getDesc() {
        return desc;
    }
}
