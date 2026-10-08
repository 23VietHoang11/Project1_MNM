# Vigil Backend & Database (MySQL / SQL)

Dự án cung cấp hệ thống Backend REST API và Cơ sở dữ liệu SQL hoàn chỉnh cho ứng dụng Vigil (Android).

## 1. Cấu trúc CSDL (`database.sql`)
Hỗ trợ đầy đủ MySQL / MariaDB (XAMPP, WampServer, Docker, MySQL Workbench) với các bảng:
- `users`: Thông tin anh hùng, XP, Cấp, Vàng, Kim Cương, Chuỗi ngày (Streak), Chặng phiêu lưu.
- `items`: Danh mục 24 vật phẩm RPG (Mũ giáp, Giáp ngực, Găng tay, Giày, Bùa chú, Vũ khí) với 4 bậc độ hiếm (`COMMON`, `RARE`, `EPIC`, `LEGENDARY`), hỗ trợ cộng chỉ số (STR, END, PRE, LUCK).
- `user_inventory`: Kho đồ của người chơi và trạng thái trang bị (`is_equipped`).
- `workouts`: Lịch sử các buổi tập, rep, calo, và chiến lợi phẩm rơi ra (`dropped_item_id`).
- `challenges`: Nhiệm vụ ngày / tuần / trùm.
- `user_challenges`: Tiến độ hoàn thành nhiệm vụ.

---

## 2. Cách chạy Backend

### Cách A: Chạy với Node.js + MySQL (Khuyên dùng khi có Node.js & MySQL/XAMPP)
1. Mở XAMPP hoặc MySQL Server, tạo Database `vigil_db` (hoặc import file `database.sql`).
2. Mở terminal tại thư mục `server`:
   ```bash
   npm install
   npm start
   ```
3. Server sẽ chạy tại `http://localhost:3000`.

### Cách B: Chạy nhanh bằng Python 3.11 (Có sẵn trên máy)
Nếu máy bạn chưa cài sẵn Node.js hoặc MySQL, bạn có thể chạy ngay server bằng Python:
```bash
python mock_server.py
```
Server sẽ tự động tạo cơ sở dữ liệu SQL SQLite `vigil_local.db` và nạp toàn bộ 24 items, sẵn sàng phục vụ API cho Android tại `http://localhost:3000`.

---

## 3. Các API chính
- `GET /api/user/:username`: Lấy thông tin người chơi và kho đồ trang bị.
- `GET /api/shop/items`: Lấy danh sách vật phẩm trong Cửa Hàng.
- `POST /api/shop/buy`: Mua vật phẩm bằng Vàng hoặc Kim Cương.
- `POST /api/inventory/equip`: Trang bị vật phẩm vào slot tương ứng của Anh Hùng.
- `POST /api/inventory/unequip`: Tháo trang bị ra khỏi slot.
- `POST /api/workout/finish`: Kết thúc bài tập, tự động tính toán **Tỷ lệ rớt đồ (Drop Rate)** dựa trên chỉ số Vận May (LUCK) và độ khó của trận đấu/Boss, sau đó lưu kết quả và trao thưởng vật phẩm rớt ra.
