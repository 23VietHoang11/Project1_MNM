package com.example.vigil.network;

import com.example.vigil.model.GuildBoss;
import com.example.vigil.model.GuildBossContribution;
import com.example.vigil.model.GuildBossInfo;
import com.example.vigil.model.GuildJoinApplication;
import com.example.vigil.model.Item;
import com.example.vigil.model.ItemSlot;
import com.example.vigil.model.PvpRoomSyncState;
import com.example.vigil.model.Rarity;
import com.example.vigil.model.WorkoutReward;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kotlin.Pair;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Client kết nối REST API Backend MySQL của Vigil viết bằng Java.
 */
public class ApiClient {
    public static String baseUrl = "http://10.0.2.2:3000";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();
    private static final MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");

    public static boolean isServerOnline() {
        try {
            Request req = new Request.Builder().url(baseUrl + "/api/health").build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean buyItem(String username, String itemId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("itemId", itemId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/shop/buy").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean equipItem(String username, int invId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("invId", invId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/inventory/equip").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean unequipItem(String username, int invId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("invId", invId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/inventory/unequip").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static WorkoutReward finishWorkout(String username, String exercise, int reps,
                                              int holdSeconds, int score, boolean isBoss,
                                              boolean isFreeTraining) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("exercise", exercise);
            body.addProperty("reps", reps);
            body.addProperty("holdSeconds", holdSeconds);
            body.addProperty("score", score);
            body.addProperty("isBoss", isBoss);
            body.addProperty("isFreeTraining", isFreeTraining);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/workout/finish").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return null;
                String json = res.body().string();
                JsonObject obj = gson.fromJson(json, JsonObject.class);

                Item droppedItem = null;
                if (obj.has("droppedItem") && !obj.get("droppedItem").isJsonNull()) {
                    JsonObject dObj = obj.getAsJsonObject("droppedItem");
                    droppedItem = new Item(
                            dObj.get("id").getAsString(),
                            dObj.get("name").getAsString(),
                            ItemSlot.valueOf(dObj.get("slot").getAsString()),
                            Rarity.valueOf(dObj.get("rarity").getAsString()),
                            dObj.get("price_gold").getAsInt(),
                            dObj.get("price_gems").getAsInt(),
                            dObj.get("bonus_str").getAsInt(),
                            dObj.get("bonus_end").getAsInt(),
                            dObj.get("bonus_pre").getAsInt(),
                            dObj.get("bonus_luck").getAsInt(),
                            dObj.get("description").getAsString(),
                            dObj.get("icon").getAsString()
                    );
                }

                return new WorkoutReward(
                        obj.get("score").getAsInt(),
                        obj.get("xpEarned").getAsInt(),
                        obj.get("goldEarned").getAsInt(),
                        obj.get("newLevel").getAsInt(),
                        obj.get("newGold").getAsInt(),
                        obj.get("newXp").getAsInt(),
                        obj.get("newStreak").getAsInt(),
                        droppedItem
                );
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Pair<Boolean, String> register(String username, String password, String avatar) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("password", password);
            body.addProperty("avatar", avatar != null ? avatar : "🧑‍🎤");
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/auth/register").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = null;
                try { obj = gson.fromJson(json, JsonObject.class); } catch (Exception ignored) {}
                if (res.isSuccessful()) {
                    String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Đăng ký thành công!";
                    return new Pair<>(true, msg);
                } else {
                    String err = (obj != null && obj.has("error")) ? obj.get("error").getAsString() : "Đăng ký thất bại!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Không thể kết nối máy chủ: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> login(String username, String password) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("password", password);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/auth/login").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = null;
                try { obj = gson.fromJson(json, JsonObject.class); } catch (Exception ignored) {}
                if (res.isSuccessful()) {
                    String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Đăng nhập thành công!";
                    return new Pair<>(true, msg);
                } else {
                    String err = (obj != null && obj.has("error")) ? obj.get("error").getAsString() : "Đăng nhập thất bại!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Không thể kết nối máy chủ: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> addFriend(String username, String friendUsername) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("friendUsername", friendUsername);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/friends/add").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = null;
                try { obj = gson.fromJson(json, JsonObject.class); } catch (Exception ignored) {}
                if (res.isSuccessful()) {
                    String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Đã kết bạn thành công!";
                    return new Pair<>(true, msg);
                } else {
                    String err = (obj != null && obj.has("error")) ? obj.get("error").getAsString() : "Không thể kết bạn!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static boolean removeFriend(String username, String friendUsername) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("friendUsername", friendUsername);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/friends/remove").delete(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static Pair<Boolean, String> sendFriendRequest(String username, String targetUsername) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("targetUsername", targetUsername);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/friends/request").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = null;
                try { obj = gson.fromJson(json, JsonObject.class); } catch (Exception ignored) {}
                if (res.isSuccessful()) {
                    String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Đã gửi lời mời kết bạn!";
                    return new Pair<>(true, msg);
                } else {
                    String err = (obj != null && obj.has("error")) ? obj.get("error").getAsString() : "Lỗi gửi lời mời!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static boolean respondFriendRequest(String username, int requestId, String action) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("requestId", requestId);
            body.addProperty("action", action);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/friends/respond").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static Pair<Boolean, String> inviteToGuild(String inviterUsername, String targetUsername, int guildId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("inviterUsername", inviterUsername);
            body.addProperty("targetUsername", targetUsername);
            body.addProperty("guildId", guildId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/invite").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = null;
                try { obj = gson.fromJson(json, JsonObject.class); } catch (Exception ignored) {}
                if (res.isSuccessful()) {
                    String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Đã gửi lời mời gia nhập bang!";
                    return new Pair<>(true, msg);
                } else {
                    String err = (obj != null && obj.has("error")) ? obj.get("error").getAsString() : "Lỗi gửi lời mời!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static GuildBossInfo getGuildBoss(int guildId, String username) {
        try {
            String url = baseUrl + "/api/guild/boss?guildId=" + guildId + "&username=" + (username != null ? username : "");
            Request req = new Request.Builder().url(url).build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful()) return null;
                String json = res.body() != null ? res.body().string() : "";
                JsonObject root = gson.fromJson(json, JsonObject.class);
                if (root == null || !root.get("success").getAsBoolean()) return null;

                JsonObject bObj = root.getAsJsonObject("boss");
                GuildBoss boss = new GuildBoss(
                        bObj.get("id").getAsInt(),
                        bObj.get("guildId").getAsInt(),
                        bObj.get("bossId").getAsString(),
                        bObj.get("name").getAsString(),
                        bObj.get("title").getAsString(),
                        bObj.get("avatar").getAsString(),
                        bObj.get("maxHp").getAsInt(),
                        bObj.get("currentHp").getAsInt(),
                        bObj.get("status").getAsString(),
                        bObj.get("rewardGold").getAsInt(),
                        bObj.get("rewardGems").getAsInt(),
                        bObj.get("rewardItemId").getAsString(),
                        bObj.has("rewardItemName") ? bObj.get("rewardItemName").getAsString() : "Vật Phẩm Huyền Thoại"
                );

                List<GuildBossContribution> list = new ArrayList<>();
                if (root.has("contributors")) {
                    JsonArray arr = root.getAsJsonArray("contributors");
                    for (JsonElement el : arr) {
                        JsonObject cObj = el.getAsJsonObject();
                        list.add(new GuildBossContribution(
                                cObj.get("userId").getAsInt(),
                                cObj.get("username").getAsString(),
                                cObj.get("avatar").getAsString(),
                                cObj.get("level").getAsInt(),
                                cObj.get("damage").getAsInt(),
                                cObj.get("reps").getAsInt(),
                                cObj.get("percentage").getAsDouble(),
                                cObj.get("rank").getAsInt(),
                                cObj.has("hasClaimed") && cObj.get("hasClaimed").getAsBoolean()
                        ));
                    }
                }

                int myDmg = 0, myReps = 0, myRank = 0;
                double myPct = 0.0;
                boolean myClaimed = false;
                if (root.has("myContribution")) {
                    JsonObject mObj = root.getAsJsonObject("myContribution");
                    myDmg = mObj.get("damage").getAsInt();
                    myReps = mObj.get("reps").getAsInt();
                    myRank = mObj.get("rank").getAsInt();
                    myPct = mObj.get("percentage").getAsDouble();
                    myClaimed = mObj.has("hasClaimedDefeatReward") && mObj.get("hasClaimedDefeatReward").getAsBoolean();
                }

                return new GuildBossInfo(boss, list, myDmg, myReps, myPct, myRank, myClaimed);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Pair<Boolean, String> attackGuildBoss(int guildId, String username, int damage, int reps, String exercise) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("guildId", guildId);
            body.addProperty("username", username);
            body.addProperty("damage", damage);
            body.addProperty("reps", reps);
            body.addProperty("exercise", exercise);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guild/boss/attack").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã tấn công Boss!";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi tấn công Boss!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> claimGuildBossReward(int guildId, String username) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("guildId", guildId);
            body.addProperty("username", username);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guild/boss/claim").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã nhận thưởng diệt Boss!";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi nhận thưởng!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> summonGuildBoss(int guildId, String username) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("guildId", guildId);
            body.addProperty("username", username);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guild/boss/summon").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã triệu hồi Boss mới!";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi triệu hồi!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> applyToGuild(String username, int guildId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("guildId", guildId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/apply").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã gửi đơn xin gia nhập bang!";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi gửi đơn!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> cancelGuildApplication(String username, Integer guildId, Integer applicationId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            if (guildId != null) body.addProperty("guildId", guildId);
            if (applicationId != null) body.addProperty("applicationId", applicationId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/cancel-application").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã hủy đơn xin gia nhập.";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi hủy đơn!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static List<GuildJoinApplication> getPendingGuildApplications(int guildId) {
        List<GuildJoinApplication> list = new ArrayList<>();
        try {
            Request req = new Request.Builder().url(baseUrl + "/api/guilds/applications/" + guildId).get().build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful()) return list;
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (obj != null && obj.has("applications")) {
                    JsonArray arr = obj.getAsJsonArray("applications");
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        list.add(new GuildJoinApplication(
                                o.get("id").getAsInt(),
                                o.get("guild_id").getAsInt(),
                                o.has("guild_name") ? o.get("guild_name").getAsString() : "",
                                o.get("user_id").getAsInt(),
                                o.has("username") ? o.get("username").getAsString() : "Chiến Binh",
                                o.has("avatar") ? o.get("avatar").getAsString() : "🧑‍🎤",
                                o.has("level") ? o.get("level").getAsInt() : 1,
                                o.has("total_reps") ? o.get("total_reps").getAsInt() : 0,
                                o.has("status") ? o.get("status").getAsString() : "PENDING",
                                o.has("created_at") ? o.get("created_at").getAsString() : ""
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static Pair<Boolean, String> approveGuildApplication(String leaderUsername, int applicationId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("leaderUsername", leaderUsername);
            body.addProperty("applicationId", applicationId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/approve-application").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã duyệt đơn gia nhập!";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi duyệt đơn!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static Pair<Boolean, String> rejectGuildApplication(String leaderUsername, int applicationId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("leaderUsername", leaderUsername);
            body.addProperty("applicationId", applicationId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/reject-application").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                String json = res.body() != null ? res.body().string() : "";
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (res.isSuccessful() && obj != null && obj.get("success").getAsBoolean()) {
                    String msg = obj.has("message") ? obj.get("message").getAsString() : "Đã từ chối đơn gia nhập.";
                    return new Pair<>(true, msg);
                } else {
                    String err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "Lỗi từ chối đơn!";
                    return new Pair<>(false, err);
                }
            }
        } catch (Exception e) {
            return new Pair<>(false, "Lỗi kết nối: " + e.getMessage());
        }
    }

    public static boolean updateUserRole(String username, String role) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("role", role);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/users/update").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean banUser(String username, String reason) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("reason", reason);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/anti-cheat/ban").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean unbanUser(String username) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("status", 1);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/users/lock").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean saveMonster(int id, String name, int stageId, int maxHp, String exercise, int expReward, int goldReward, boolean isBoss) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", id);
            body.addProperty("name", name);
            body.addProperty("stageId", stageId);
            body.addProperty("maxHp", maxHp);
            body.addProperty("exercise", exercise);
            body.addProperty("expReward", expReward);
            body.addProperty("goldReward", goldReward);
            body.addProperty("isBoss", isBoss);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/monsters/save").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean saveExercise(String id, String name, float extendAngle, float contractAngle, float damageFactor, float caloriesPerRep) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", id);
            body.addProperty("name", name);
            body.addProperty("extendAngle", extendAngle);
            body.addProperty("contractAngle", contractAngle);
            body.addProperty("damageFactor", damageFactor);
            body.addProperty("caloriesPerRep", caloriesPerRep);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/exercises/save").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean savePvpMatch(
            String player1Name,
            String player2Name,
            String exercise,
            int durationSec,
            int p1Reps,
            int p1Score,
            int p2Reps,
            int p2Score,
            String winnerName
    ) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("player1Name", player1Name);
            body.addProperty("player2Name", player2Name);
            body.addProperty("exercise", exercise);
            body.addProperty("durationSec", durationSec);
            body.addProperty("p1Reps", p1Reps);
            body.addProperty("p1Score", p1Score);
            body.addProperty("p2Reps", p2Reps);
            body.addProperty("p2Score", p2Score);
            body.addProperty("winnerName", winnerName);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/pvp/finish").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static PvpRoomSyncState joinPvpQueue(
            String username,
            String avatar,
            int level,
            int attackDamage,
            String exercise,
            int durationSec
    ) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("avatar", avatar);
            body.addProperty("level", level);
            body.addProperty("attackDamage", attackDamage);
            body.addProperty("exercise", exercise);
            body.addProperty("durationSec", durationSec);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/pvp/queue/join").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
                JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                return parsePvpRoomSync(obj);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
        }
    }

    public static PvpRoomSyncState checkPvpQueueStatus(String username) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/pvp/queue/status?username=" + username)
                    .get()
                    .build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
                JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                return parsePvpRoomSync(obj);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
        }
    }

    public static void cancelPvpQueue(String username) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());
            Request req = new Request.Builder().url(baseUrl + "/api/pvp/queue/cancel").post(reqBody).build();
            try (Response ignored = client.newCall(req).execute()) {}
        } catch (Exception ignored) {}
    }

    public static PvpRoomSyncState syncPvpRoom(
            String roomId,
            String username,
            int reps,
            int score,
            int angle,
            String hint,
            String frameBase64
    ) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("roomId", roomId);
            body.addProperty("username", username);
            body.addProperty("reps", reps);
            body.addProperty("score", score);
            body.addProperty("angle", angle);
            body.addProperty("hint", hint);
            if (frameBase64 != null) body.addProperty("frameBase64", frameBase64);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/pvp/room/sync").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
                JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                return parsePvpRoomSync(obj);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
        }
    }

    private static PvpRoomSyncState parsePvpRoomSync(JsonObject obj) {
        if (obj == null || !obj.has("matched") || !obj.get("matched").getAsBoolean()) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null);
        }
        String roomId = obj.has("roomId") && !obj.get("roomId").isJsonNull() ? obj.get("roomId").getAsString() : "";
        JsonObject opp = obj.has("opponent") && obj.get("opponent").isJsonObject() ? obj.getAsJsonObject("opponent") : null;
        if (opp == null) {
            return new PvpRoomSyncState(true, roomId, "Đối Thủ", "🧑‍🎤", 1, 50, 0, 0, 165, "Đang tập", null);
        }
        String uName = opp.has("username") && !opp.get("username").isJsonNull() ? opp.get("username").getAsString() : "Đối Thủ";
        String avt = opp.has("avatar") && !opp.get("avatar").isJsonNull() ? opp.get("avatar").getAsString() : "🧑‍🎤";
        int lvl = opp.has("level") && !opp.get("level").isJsonNull() ? opp.get("level").getAsInt() : 1;
        int dmg = opp.has("attackDamage") && !opp.get("attackDamage").isJsonNull() ? opp.get("attackDamage").getAsInt() : 50;
        int reps = opp.has("reps") && !opp.get("reps").isJsonNull() ? opp.get("reps").getAsInt() : 0;
        int score = opp.has("score") && !opp.get("score").isJsonNull() ? opp.get("score").getAsInt() : 0;
        int angle = opp.has("angle") && !opp.get("angle").isJsonNull() ? opp.get("angle").getAsInt() : 165;
        String hint = opp.has("hint") && !opp.get("hint").isJsonNull() ? opp.get("hint").getAsString() : "Đang giữ nhịp tập";
        String frame = opp.has("frameBase64") && !opp.get("frameBase64").isJsonNull() ? opp.get("frameBase64").getAsString() : null;
        return new PvpRoomSyncState(true, roomId, uName, avt, lvl, dmg, reps, score, angle, hint, frame);
    }

    public static boolean saveAdminItem(Item item) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", item.getId());
            body.addProperty("name", item.getName());
            body.addProperty("slot", item.getSlot().name());
            body.addProperty("rarity", item.getRarity().name());
            body.addProperty("priceGold", item.getPriceGold());
            body.addProperty("priceGems", item.getPriceGems());
            body.addProperty("bonusStr", item.getBonusStr());
            body.addProperty("bonusEnd", item.getBonusEnd());
            body.addProperty("bonusPre", item.getBonusPre());
            body.addProperty("bonusLuck", item.getBonusLuck());
            body.addProperty("description", item.getDescription());
            body.addProperty("icon", item.getIcon());
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/items/save").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean deleteAdminItem(String itemId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", itemId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/admin/items/delete").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}

