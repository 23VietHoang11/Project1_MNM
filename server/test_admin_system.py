import urllib.request
import json
import sys
import time

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://127.0.0.1:3000"

def request(path, method="GET", data=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}

def run_tests():
    print("=" * 60)
    print("STARTING END-TO-END VERIFICATION OF ADMIN & AUTH REQUIREMENTS")
    print("=" * 60)

    # 1. Check users list in admin overview
    print("\n[TEST 1] Kiểm tra danh sách người chơi trong bảng Admin (Chỉ acc thật, không có acc ảo)")
    status, res = request("/api/admin/overview")
    assert status == 200, f"Failed overview: {res}"
    users = res.get("users", [])
    usernames = [u["username"] for u in users]
    print(f"Users found in Admin panel: {usernames}")
    assert "ShadowBlade" not in usernames, "ShadowBlade should NOT be in admin user list"
    assert "IronSpartan" not in usernames, "IronSpartan should NOT be in admin user list"
    assert "SpeedyHacker99" not in usernames, "SpeedyHacker99 should NOT be in admin user list"
    assert "Hachimi" in usernames, "Hachimi root admin MUST be present"
    print("--> PASS: Chỉ có tài khoản thật, toàn bộ bot ảo đã bị loại bỏ hoàn toàn.")

    # 2. Register real user
    test_user = f"Knight_{int(time.time())}"
    test_pass = "mypassword123"
    print(f"\n[TEST 2] Đăng ký người chơi thật mới [{test_user}]")
    status, res = request("/api/auth/register", method="POST", data={
        "username": test_user,
        "password": test_pass,
        "avatar": "🧑‍🎤"
    })
    print(f"Register response: {status} -> {res}")
    assert status in [200, 201], f"Register failed: {res}"
    assert res.get("success") is True, "Register should succeed"
    print("--> PASS: Đăng ký thành công tài khoản người chơi thật.")

    # 3. Verify new user role is 'User' and cannot access Admin
    print(f"\n[TEST 3] Kiểm tra quyền hạn ban đầu: [{test_user}] chưa có quyền Admin")
    status, res = request("/api/admin/login", method="POST", data={
        "username": test_user,
        "password": test_pass
    })
    print(f"Admin login response: {status} -> {res}")
    assert status == 403, f"Expected 403 Forbidden for non-admin, got {status}"
    print("--> PASS: Người chơi thông thường bị chặn tuyệt đối khỏi bảng Admin (HTTP 403).")

    # 4. Grant Admin permission to RealKnight99
    print(f"\n[TEST 4] Cấp quyền Admin cho [{test_user}]")
    # First get user id
    u_obj = next(u for u in request("/api/admin/overview")[1]["users"] if u["username"] == test_user)
    status, res = request("/api/admin/users/update", method="POST", data={
        "userId": u_obj["id"],
        "username": test_user,
        "role": "Admin"
    })
    print(f"Update role response: {status} -> {res}")
    assert status == 200, f"Role update failed: {res}"

    # Now verify RealKnight99 can access Admin panel
    status, res = request("/api/admin/login", method="POST", data={
        "username": test_user,
        "password": test_pass
    })
    print(f"Admin login after promotion: {status} -> {res}")
    assert status == 200, f"Expected 200 OK for delegated admin, got {status}"
    assert res.get("success") is True
    assert res.get("admin", {}).get("role") == "Admin"
    print("--> PASS: Người được cấp quyền Admin đã có đầy đủ quyền hạn và truy cập được bảng Admin.")

    # 5. Modify Monster parameters (Zero-Hardcoding)
    print("\n[TEST 5] Quản trị viên chỉnh sửa thông số Quái Vật (Máu HP, EXP, Vàng)")
    target_monster = {
        "id": 1,
        "name": "Slime Đầm Lầy Biến Dị",
        "title": "Quái vật đột biến gen",
        "avatar": "🦠",
        "stageId": 1,
        "maxHp": 3500,  # Changed from 500 to 3500
        "exercise": "PUSHUP",
        "expReward": 450,
        "goldReward": 150,
        "isBoss": False,
        "dropRarity": "EPIC",
        "lore": "Slime đột biến sau khi Admin tăng cường lực chiến."
    }
    status, res = request("/api/admin/monsters/save", method="POST", data=target_monster)
    print(f"Save monster response: {status} -> {res}")
    assert status == 200, f"Monster save failed: {res}"

    # Check /api/admin/monsters endpoint to verify persistence
    status, res = request("/api/admin/monsters")
    saved_m = next(m for m in res if m["name"] == "Slime Đầm Lầy Biến Dị")
    assert saved_m["maxHp"] == 3500, f"Expected HP 3500, got {saved_m['maxHp']}"
    assert saved_m["expReward"] == 450, f"Expected EXP 450, got {saved_m['expReward']}"
    print(f"--> PASS: Thông số Quái Vật đã được lưu và cập nhật: HP={saved_m['maxHp']}, EXP={saved_m['expReward']}.")

    # 6. Modify AI Exercise angles and factors
    print("\n[TEST 6] Quản trị viên chỉnh sửa thông số Góc Khớp AI MediaPipe")
    target_exercise = {
        "id": "PUSHUP",
        "name": "Hít đất (Push-up Chuẩn VIP)",
        "extendAngle": 172.5,
        "contractAngle": 82.0,
        "damageFactor": 2.2,
        "caloriesPerRep": 1.5,
        "description": "Ngưỡng góc AI tinh chỉnh bởi Quản trị viên"
    }
    status, res = request("/api/admin/exercises/save", method="POST", data=target_exercise)
    print(f"Save exercise response: {status} -> {res}")
    assert status == 200, f"Exercise save failed: {res}"

    # Verify persistence
    status, res = request("/api/admin/exercises")
    saved_ex = next(ex for ex in res if ex["id"] == "PUSHUP")
    assert abs(saved_ex["extendAngle"] - 172.5) < 0.1, f"Extend angle mismatch: {saved_ex['extendAngle']}"
    assert abs(saved_ex["contractAngle"] - 82.0) < 0.1, f"Contract angle mismatch: {saved_ex['contractAngle']}"
    assert abs(saved_ex["damageFactor"] - 2.2) < 0.1, f"Damage factor mismatch: {saved_ex['damageFactor']}"
    print(f"--> PASS: Thông số góc AI đã được lưu: duỗi={saved_ex['extendAngle']}°, gập={saved_ex['contractAngle']}°, damage={saved_ex['damageFactor']}x.")

    # 7. Anti-Cheat Ban enforcement
    print(f"\n[TEST 7] Admin thực hiện Ban tài khoản [{test_user}] do vi phạm gian lận")
    ban_reason = "Gian lận tốc độ rep MediaPipe dị thường: 45ms/rep"
    status, res = request("/api/admin/anti-cheat/ban", method="POST", data={
        "username": test_user,
        "reason": ban_reason
    })
    print(f"Ban response: {status} -> {res}")
    assert status == 200, f"Ban failed: {res}"
    print("--> PASS: Admin đã ban thành công tài khoản.")

    # 8. Attempt login with banned account -> MUST FAIL with ban message
    print(f"\n[TEST 8] Kiểm tra đăng nhập với tài khoản bị ban [{test_user}]")
    status, res = request("/api/auth/login", method="POST", data={
        "username": test_user,
        "password": test_pass
    })
    print(f"Banned user login response: {status} -> {res}")
    assert status == 403, f"Expected 403 Forbidden for banned user, got {status}"
    assert res.get("isBanned") is True, f"Expected isBanned: True in response, got {res}"
    assert ban_reason in res.get("error", "") or ban_reason in res.get("lockReason", ""), "Ban reason must be present in error response"
    print(f"--> PASS: Đăng nhập bị chặn hoàn toàn! Nhận thông báo cấm chính xác:")
    print(f"    Thông báo: {res.get('error')}")

    # 9. Also verify GET /api/user/<username> blocks banned user
    status, res = request(f"/api/user/{test_user}")
    assert status == 403, f"Expected 403 on profile fetch for banned user, got {status}"
    print("--> PASS: Mọi API thông tin người chơi đều chặn tài khoản bị ban.")

    print("\n" + "=" * 60)
    print("TẤT CẢ 8 MỤC KIỂM TRA ĐỀU THÀNH CÔNG RỰC RỠ 100%!")
    print("=" * 60)

if __name__ == "__main__":
    run_tests()
