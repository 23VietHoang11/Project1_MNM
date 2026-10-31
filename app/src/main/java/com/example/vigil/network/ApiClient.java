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
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import kotlin.Pair;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Client kết nối REST API Backend MySQL của Vigil viết bằng Java.
 * Hỗ trợ tự động dò tìm Server nội bộ (Emulator + LAN Wi-Fi) kết hợp Cloud Relay toàn cầu
 * giúp 2 thiết bị Android bất kỳ luôn ghép trận và truyền Video Call 1v1 trực tiếp.
 */
public class ApiClient {
    public static String baseUrl = "http://10.0.2.2:3000";
    private static final String[] LOCAL_CANDIDATE_URLS = new String[] {
            "http://10.0.2.2:3000",
            "http://192.168.1.152:3000",
            "http://127.0.0.1:3000"
    };
    private static final String TUNNEL_CONFIG_URL = "https://ntfy.sh/vigil_active_server_url_v4";
    private static final String CLOUD_LOBBY_URL = "https://ntfy.sh/vigil_pvp_global_lobby_v7";
    public static final String DEVICE_ID = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    private static volatile String discoveredTunnelUrl = "";
    private static volatile long lastTunnelLookupMs = 0L;
    private static String currentSeekToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private static final Set<String> consumedSeekTokens = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> seenCloudRoomMsgIds = Collections.synchronizedSet(new HashSet<>());
    private static long lastCloudSeekPostMs = 0L;
    private static long lastCloudRoomPostMs = 0L;
    private static int cloudVideoTopicIdx = 0;
    private static int lastPublishedReps = -1;
    private static int lastPublishedScore = -1;
    private static String activeSyncRoomId = "";
    private static int cachedOppReps = 0;
    private static int cachedOppScore = 0;
    private static int cachedOppAngle = 165;
    private static String cachedOppHint = "Đang giữ nhịp tập";
    private static String cachedOppFrame = null;

    private static OkHttpClient buildTrustAllClient(int connectMs, int readMs) {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAll)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
                    .readTimeout(readMs, TimeUnit.MILLISECONDS)
                    .build();
        } catch (Exception e) {
            return new OkHttpClient.Builder()
                    .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
                    .readTimeout(readMs, TimeUnit.MILLISECONDS)
                    .build();
        }
    }

    private static final OkHttpClient client = buildTrustAllClient(3000, 3500);
    private static final OkHttpClient fastLocalClient = buildTrustAllClient(750, 1200);

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

    public static boolean updateGuildMemberRole(String actorUsername, int targetUserId, String newRole) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("actorUsername", actorUsername);
            body.addProperty("targetUserId", targetUserId);
            body.addProperty("newRole", newRole);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/member/role").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean kickGuildMember(String actorUsername, int targetUserId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("actorUsername", actorUsername);
            body.addProperty("targetUserId", targetUserId);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/guilds/member/kick").post(reqBody).build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (Exception e) {
            return false;
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
        return joinPvpQueue(username, avatar, level, attackDamage, exercise, durationSec, false);
    }

    private static void refreshTunnelUrlIfNeeded() {
        long now = System.currentTimeMillis();
        if (!discoveredTunnelUrl.isEmpty() && (now - lastTunnelLookupMs) < 30000L) {
            return;
        }
        lastTunnelLookupMs = now;
        try {
            Request req = new Request.Builder()
                    .url(TUNNEL_CONFIG_URL + "/json?poll=1&since=12h")
                    .get()
                    .build();
            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    String[] lines = res.body().string().split("\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        try {
                            JsonObject env = gson.fromJson(trimmed, JsonObject.class);
                            if (env != null && env.has("message") && !env.get("message").isJsonNull()) {
                                JsonObject payload = gson.fromJson(env.get("message").getAsString(), JsonObject.class);
                                if (payload != null && payload.has("tunnelUrl") && !payload.get("tunnelUrl").isJsonNull()) {
                                    String url = payload.get("tunnelUrl").getAsString().trim();
                                    if (url.startsWith("http")) {
                                        discoveredTunnelUrl = url;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static PvpRoomSyncState joinPvpQueue(
            String username,
            String avatar,
            int level,
            int attackDamage,
            String exercise,
            int durationSec,
            boolean resetQueue
    ) {
        if (resetQueue) {
            currentSeekToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            lastCloudSeekPostMs = 0L;
            activeSyncRoomId = "";
            seenCloudRoomMsgIds.clear();
            cachedOppReps = 0;
            cachedOppScore = 0;
            cachedOppAngle = 165;
            cachedOppHint = "Đang giữ nhịp tập";
            cachedOppFrame = null;
        }

        // 1. Thử ghép trận qua Server Vigil (Emulator 10.0.2.2, LAN Wi-Fi 192.168.1.152 và Public HTTPS Tunnel)
        boolean localOnline = false;
        PvpRoomSyncState localState = tryLocalJoinPvpQueue(username, avatar, level, attackDamage, exercise, durationSec, resetQueue);
        if (localState.getServerOnline()) {
            localOnline = true;
            if (localState.getMatched()) {
                cancelCloudPvpQueue(username);
                return localState;
            }
        }

        // 2. Luôn đồng thời phát/kiểm tra tín hiệu trên Cloud Relay toàn cầu khi chưa ghép được ở bước 1
        //    Đảm bảo kể cả khi 1 máy kết nối LAN và 1 máy bật VPN (hoặc khác mạng hoàn toàn) vẫn luôn nhìn thấy nhau 100%!
        PvpRoomSyncState cloudState = tryCloudJoinPvpQueue(username, avatar, level, attackDamage, exercise, durationSec, resetQueue);
        if (cloudState.getMatched()) {
            if (localOnline) {
                cancelLocalPvpQueue(username);
            }
            return cloudState;
        }

        return new PvpRoomSyncState(
                false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0,
                localOnline || cloudState.getServerOnline()
        );
    }

    private static PvpRoomSyncState tryLocalJoinPvpQueue(
            String username,
            String avatar,
            int level,
            int attackDamage,
            String exercise,
            int durationSec,
            boolean resetQueue
    ) {
        refreshTunnelUrlIfNeeded();

        JsonObject body = new JsonObject();
        body.addProperty("deviceId", DEVICE_ID);
        body.addProperty("username", username);
        body.addProperty("avatar", avatar);
        body.addProperty("level", level);
        body.addProperty("attackDamage", attackDamage);
        body.addProperty("exercise", exercise);
        body.addProperty("durationSec", durationSec);
        body.addProperty("resetQueue", resetQueue);
        RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

        List<String> urlsToTry = new ArrayList<>();
        urlsToTry.add(baseUrl);
        if (!discoveredTunnelUrl.isEmpty() && !urlsToTry.contains(discoveredTunnelUrl)) {
            urlsToTry.add(discoveredTunnelUrl);
        }
        for (String candidate : LOCAL_CANDIDATE_URLS) {
            if (!urlsToTry.contains(candidate)) {
                urlsToTry.add(candidate);
            }
        }

        for (String candidateBase : urlsToTry) {
            try {
                OkHttpClient http = candidateBase.startsWith("https://") ? client : fastLocalClient;
                Request req = new Request.Builder()
                        .url(candidateBase + "/api/pvp/queue/join")
                        .post(reqBody)
                        .build();
                try (Response res = http.newCall(req).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        baseUrl = candidateBase;
                        JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                        return parsePvpRoomSync(obj);
                    }
                }
            } catch (Exception ignored) {}
        }
        return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
    }

    private static PvpRoomSyncState tryCloudJoinPvpQueue(
            String username,
            String avatar,
            int level,
            int attackDamage,
            String exercise,
            int durationSec,
            boolean resetQueue
    ) {
        try {
            long nowMs = System.currentTimeMillis();
            if (resetQueue || (nowMs - lastCloudSeekPostMs) >= 2200L) {
                JsonObject seekObj = new JsonObject();
                seekObj.addProperty("type", "SEEK");
                seekObj.addProperty("deviceId", DEVICE_ID);
                seekObj.addProperty("seekToken", currentSeekToken);
                seekObj.addProperty("username", username);
                seekObj.addProperty("avatar", avatar != null ? avatar : "🧑‍🎤");
                seekObj.addProperty("level", level);
                seekObj.addProperty("attackDamage", attackDamage);
                seekObj.addProperty("exercise", exercise != null ? exercise : "PUSHUP");
                seekObj.addProperty("durationSec", durationSec > 0 ? durationSec : 30);

                Request postReq = new Request.Builder()
                        .url(CLOUD_LOBBY_URL)
                        .post(RequestBody.create(jsonMediaType, seekObj.toString()))
                        .build();
                try (Response postRes = client.newCall(postReq).execute()) {
                    if (postRes.isSuccessful()) {
                        lastCloudSeekPostMs = nowMs;
                    }
                }
            }

            Request pollReq = new Request.Builder()
                    .url(CLOUD_LOBBY_URL + "/json?poll=1&since=20s")
                    .get()
                    .build();
            try (Response pollRes = client.newCall(pollReq).execute()) {
                if (!pollRes.isSuccessful() || pollRes.body() == null) {
                    return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
                }
                String rawLines = pollRes.body().string();
                String[] lines = rawLines.split("\n");
                long maxTime = 0L;
                List<Pair<Long, JsonObject>> events = new ArrayList<>();

                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    try {
                        JsonObject env = gson.fromJson(trimmed, JsonObject.class);
                        if (env == null || !env.has("event") || !"message".equals(env.get("event").getAsString())) {
                            continue;
                        }
                        if (!env.has("message") || env.get("message").isJsonNull()) continue;
                        long t = env.has("time") ? env.get("time").getAsLong() : 0L;
                        if (t > maxTime) maxTime = t;
                        JsonObject payload = gson.fromJson(env.get("message").getAsString(), JsonObject.class);
                        if (payload != null && payload.has("type")) {
                            events.add(new Pair<>(t, payload));
                        }
                    } catch (Exception ignored) {}
                }

                Map<String, JsonObject> activeSeekers = new LinkedHashMap<>();
                for (Pair<Long, JsonObject> ev : events) {
                    long t = ev.getFirst();
                    JsonObject msg = ev.getSecond();
                    if (maxTime > 0 && (maxTime - t) > 14L) continue;
                    String devId = msg.has("deviceId") && !msg.get("deviceId").isJsonNull()
                            ? msg.get("deviceId").getAsString() : "";
                    if (devId.isEmpty()) continue;
                    String type = msg.get("type").getAsString();
                    if ("SEEK".equals(type)) {
                        activeSeekers.put(devId, msg);
                    } else if ("CANCEL".equals(type)) {
                        activeSeekers.remove(devId);
                    }
                }

                for (Map.Entry<String, JsonObject> entry : activeSeekers.entrySet()) {
                    String otherDevId = entry.getKey();
                    JsonObject opp = entry.getValue();
                    if (otherDevId.equals(DEVICE_ID)) continue;
                    String oppSeekToken = opp.has("seekToken") && !opp.get("seekToken").isJsonNull()
                            ? opp.get("seekToken").getAsString() : otherDevId;
                    if (consumedSeekTokens.contains(oppSeekToken)) continue;

                    consumedSeekTokens.add(oppSeekToken);
                    boolean firstIsMe = DEVICE_ID.compareTo(otherDevId) <= 0;
                    String d1 = firstIsMe ? DEVICE_ID : otherDevId;
                    String d2 = firstIsMe ? otherDevId : DEVICE_ID;
                    String s1 = (firstIsMe ? currentSeekToken : oppSeekToken);
                    String s2 = (firstIsMe ? oppSeekToken : currentSeekToken);
                    if (s1.length() > 4) s1 = s1.substring(0, 4);
                    if (s2.length() > 4) s2 = s2.substring(0, 4);

                    String deterministicRoomId = "vigil_room_" + d1 + "_" + d2 + "_" + s1 + s2;
                    String oppName = opp.has("username") && !opp.get("username").isJsonNull()
                            ? opp.get("username").getAsString() : "Đối Thủ Online";
                    String oppAvt = opp.has("avatar") && !opp.get("avatar").isJsonNull()
                            ? opp.get("avatar").getAsString() : "🧑‍🎤";
                    int oppLvl = opp.has("level") && !opp.get("level").isJsonNull()
                            ? opp.get("level").getAsInt() : 1;
                    int oppDmg = opp.has("attackDamage") && !opp.get("attackDamage").isJsonNull()
                            ? opp.get("attackDamage").getAsInt() : 50;
                    String oppEx = opp.has("exercise") && !opp.get("exercise").isJsonNull()
                            ? opp.get("exercise").getAsString() : exercise;
                    int oppDur = opp.has("durationSec") && !opp.get("durationSec").isJsonNull()
                            ? opp.get("durationSec").getAsInt() : durationSec;

                    String roomEx = firstIsMe ? exercise : oppEx;
                    int roomDur = firstIsMe ? durationSec : oppDur;

                    return new PvpRoomSyncState(
                            true,
                            deterministicRoomId,
                            oppName,
                            oppAvt,
                            oppLvl,
                            oppDmg,
                            0,
                            0,
                            165,
                            "Đang vào tư thế",
                            null,
                            roomEx,
                            roomDur,
                            true
                    );
                }

                return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, true);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
        }
    }

    public static PvpRoomSyncState checkPvpQueueStatus(String username) {
        try {
            OkHttpClient http = baseUrl.startsWith("https://") ? client : fastLocalClient;
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/pvp/queue/status?username=" + username + "&deviceId=" + DEVICE_ID)
                    .get()
                    .build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
                JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                return parsePvpRoomSync(obj);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
        }
    }

    private static void cancelLocalPvpQueue(String username) {
        try {
            OkHttpClient http = baseUrl.startsWith("https://") ? client : fastLocalClient;
            JsonObject body = new JsonObject();
            body.addProperty("deviceId", DEVICE_ID);
            body.addProperty("username", username);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());
            Request req = new Request.Builder().url(baseUrl + "/api/pvp/queue/cancel").post(reqBody).build();
            try (Response ignored = http.newCall(req).execute()) {}
        } catch (Exception ignored) {}
    }

    private static void cancelCloudPvpQueue(String username) {
        try {
            JsonObject cancelObj = new JsonObject();
            cancelObj.addProperty("type", "CANCEL");
            cancelObj.addProperty("deviceId", DEVICE_ID);
            cancelObj.addProperty("seekToken", currentSeekToken);
            cancelObj.addProperty("username", username);
            Request req = new Request.Builder()
                    .url(CLOUD_LOBBY_URL)
                    .post(RequestBody.create(jsonMediaType, cancelObj.toString()))
                    .build();
            try (Response ignored = client.newCall(req).execute()) {}
        } catch (Exception ignored) {}
    }

    public static void cancelPvpQueue(String username) {
        cancelLocalPvpQueue(username);
        cancelCloudPvpQueue(username);
    }

    private static PvpRoomSyncState tryLocalSyncPvpRoom(
            String roomId,
            String username,
            int reps,
            int score,
            int angle,
            String hint,
            String frameBase64
    ) {
        try {
            OkHttpClient http = baseUrl.startsWith("https://") ? client : fastLocalClient;
            JsonObject body = new JsonObject();
            body.addProperty("roomId", roomId);
            body.addProperty("deviceId", DEVICE_ID);
            body.addProperty("username", username);
            body.addProperty("reps", reps);
            body.addProperty("score", score);
            body.addProperty("angle", angle);
            body.addProperty("hint", hint);
            if (frameBase64 != null) body.addProperty("frameBase64", frameBase64);
            RequestBody reqBody = RequestBody.create(jsonMediaType, body.toString());

            Request req = new Request.Builder().url(baseUrl + "/api/pvp/room/sync").post(reqBody).build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) {
                    return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
                }
                JsonObject obj = gson.fromJson(res.body().string(), JsonObject.class);
                return parsePvpRoomSync(obj);
            }
        } catch (Exception e) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
        }
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
        if (roomId != null && !roomId.equals(activeSyncRoomId)) {
            activeSyncRoomId = roomId;
            seenCloudRoomMsgIds.clear();
            lastCloudRoomPostMs = 0L;
            cloudVideoTopicIdx = 0;
            lastPublishedReps = -1;
            lastPublishedScore = -1;
            cachedOppReps = 0;
            cachedOppScore = 0;
            cachedOppAngle = 165;
            cachedOppHint = "Đang giữ nhịp tập";
            cachedOppFrame = null;
        }

        // 1. Luôn thử đồng bộ qua Server tốc độ cao trước (hỗ trợ cả room_ lẫn vigil_room_)
        PvpRoomSyncState localSync = tryLocalSyncPvpRoom(roomId, username, reps, score, angle, hint, frameBase64);
        boolean hasLocalHdFrame = false;
        if (localSync.getMatched()) {
            if (localSync.getOpponentReps() > cachedOppReps) cachedOppReps = localSync.getOpponentReps();
            if (localSync.getOpponentScore() > cachedOppScore) cachedOppScore = localSync.getOpponentScore();
            if (localSync.getOpponentFrameBase64() != null && !localSync.getOpponentFrameBase64().isEmpty()) {
                cachedOppFrame = localSync.getOpponentFrameBase64();
                cachedOppAngle = localSync.getOpponentAngle();
                cachedOppHint = localSync.getOpponentHint();
                hasLocalHdFrame = true;
            }
            if (roomId != null && !roomId.startsWith("vigil_room_")) {
                return localSync;
            }
        }

        // 2. Đồng bộ đa luồng qua Cloud (Tách riêng kênh Điểm Số + 6 kênh Video xoay vòng để không bao giờ bị nghẽn băng thông)
        return syncCloudPvpRoom(roomId, username, reps, score, angle, hint, frameBase64, hasLocalHdFrame);
    }

    private static PvpRoomSyncState syncCloudPvpRoom(
            String roomId,
            String username,
            int reps,
            int score,
            int angle,
            String hint,
            String frameBase64,
            boolean hasLocalHdFrame
    ) {

        long nowMs = System.currentTimeMillis();
        boolean scoreChanged = (reps > lastPublishedReps) || (score > lastPublishedScore);

        // A. Kênh Điểm Số chuyên biệt (_s): Chỉ gửi khi tăng Rep/Điểm, cam kết 100% không bao giờ bị giới hạn tốc độ
        if (scoreChanged) {
            try {
                JsonObject scorePayload = new JsonObject();
                scorePayload.addProperty("d", DEVICE_ID);
                scorePayload.addProperty("u", username);
                scorePayload.addProperty("r", reps);
                scorePayload.addProperty("s", score);
                scorePayload.addProperty("a", angle);
                scorePayload.addProperty("h", hint != null ? hint : "");
                Request scoreReq = new Request.Builder()
                        .url("https://ntfy.sh/" + roomId + "_s")
                        .post(RequestBody.create(jsonMediaType, scorePayload.toString()))
                        .build();
                try (Response scoreRes = client.newCall(scoreReq).execute()) {
                    if (scoreRes.isSuccessful()) {
                        lastPublishedReps = reps;
                        lastPublishedScore = score;
                    }
                }
            } catch (Exception ignored) {}
        }

        // B. 6 Kênh Video xoay vòng (_v0 .. _v5): Chia tải đều giúp truyền khung hình 220x300 sắc nét mượt mà
        if (frameBase64 != null && !frameBase64.isEmpty() && (nowMs - lastCloudRoomPostMs) >= 650L) {
            try {
                String compactFrame = frameBase64.length() <= 3650
                        ? frameBase64
                        : shrinkFrameForCloudRelay(frameBase64);
                if (compactFrame != null && compactFrame.length() <= 3750) {
                    cloudVideoTopicIdx = (cloudVideoTopicIdx + 1) % 6;
                    JsonObject vidPayload = new JsonObject();
                    vidPayload.addProperty("d", DEVICE_ID);
                    vidPayload.addProperty("u", username);
                    vidPayload.addProperty("r", reps);
                    vidPayload.addProperty("s", score);
                    vidPayload.addProperty("a", angle);
                    vidPayload.addProperty("h", hint != null ? hint : "");
                    vidPayload.addProperty("f", compactFrame);

                    Request vidReq = new Request.Builder()
                            .url("https://ntfy.sh/" + roomId + "_v" + cloudVideoTopicIdx)
                            .post(RequestBody.create(jsonMediaType, vidPayload.toString()))
                            .build();
                    try (Response vidRes = client.newCall(vidReq).execute()) {
                        if (vidRes.isSuccessful()) {
                            lastCloudRoomPostMs = nowMs;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // C. Đọc gộp cả kênh Điểm Số + 6 kênh Video chỉ bằng 1 HTTP GET duy nhất
        String multiTopics = roomId + "_s,"
                + roomId + "_v0,"
                + roomId + "_v1,"
                + roomId + "_v2,"
                + roomId + "_v3,"
                + roomId + "_v4,"
                + roomId + "_v5";
        try {
            Request pollReq = new Request.Builder()
                    .url("https://ntfy.sh/" + multiTopics + "/json?poll=1&since=15s")
                    .get()
                    .build();
            try (Response pollRes = client.newCall(pollReq).execute()) {
                if (pollRes.isSuccessful() && pollRes.body() != null) {
                    String[] lines = pollRes.body().string().split("\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        try {
                            JsonObject env = gson.fromJson(trimmed, JsonObject.class);
                            if (env == null || !env.has("event") || !"message".equals(env.get("event").getAsString())) {
                                continue;
                            }
                            String msgId = env.has("id") && !env.get("id").isJsonNull()
                                    ? env.get("id").getAsString() : "";
                            if (!msgId.isEmpty() && seenCloudRoomMsgIds.contains(msgId)) {
                                continue;
                            }
                            if (!env.has("message") || env.get("message").isJsonNull()) continue;
                            JsonObject msg = gson.fromJson(env.get("message").getAsString(), JsonObject.class);
                            if (msg == null || !msg.has("d")) continue;
                            String senderDev = msg.get("d").getAsString();
                            if (senderDev.equals(DEVICE_ID)) {
                                if (!msgId.isEmpty()) seenCloudRoomMsgIds.add(msgId);
                                continue;
                            }

                            if (!msgId.isEmpty()) seenCloudRoomMsgIds.add(msgId);
                            if (msg.has("r")) cachedOppReps = Math.max(cachedOppReps, msg.get("r").getAsInt());
                            if (msg.has("s")) cachedOppScore = Math.max(cachedOppScore, msg.get("s").getAsInt());
                            if (msg.has("a")) cachedOppAngle = msg.get("a").getAsInt();
                            if (msg.has("h") && !msg.get("h").isJsonNull()) cachedOppHint = msg.get("h").getAsString();
                            if (!hasLocalHdFrame && msg.has("f") && !msg.get("f").isJsonNull()) {
                                String f = msg.get("f").getAsString();
                                if (!f.isEmpty()) cachedOppFrame = f;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        return new PvpRoomSyncState(
                true,
                roomId,
                "Đối Thủ",
                "🧑‍🎤",
                1,
                50,
                cachedOppReps,
                cachedOppScore,
                cachedOppAngle,
                cachedOppHint,
                cachedOppFrame,
                "",
                0,
                true
        );
    }

    private static String shrinkFrameForCloudRelay(String frameBase64) {
        try {
            byte[] raw = android.util.Base64.decode(frameBase64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bmp == null) return null;
            android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(bmp, 220, 300, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 46, out);
            if (out.size() > 2720) {
                out.reset();
                small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 34, out);
            }
            if (small != bmp) small.recycle();
            bmp.recycle();
            return android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PvpRoomSyncState parsePvpRoomSync(JsonObject obj) {
        if (obj == null) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, false);
        }
        if (!obj.has("matched") || !obj.get("matched").getAsBoolean()) {
            return new PvpRoomSyncState(false, "", "", "🧑‍🎤", 1, 50, 0, 0, 165, "", null, "", 0, true);
        }
        String roomId = obj.has("roomId") && !obj.get("roomId").isJsonNull() ? obj.get("roomId").getAsString() : "";
        String roomEx = obj.has("exercise") && !obj.get("exercise").isJsonNull() ? obj.get("exercise").getAsString() : "";
        int roomDur = obj.has("durationSec") && !obj.get("durationSec").isJsonNull() ? obj.get("durationSec").getAsInt() : 0;
        JsonObject opp = obj.has("opponent") && obj.get("opponent").isJsonObject() ? obj.getAsJsonObject("opponent") : null;
        if (opp == null) {
            return new PvpRoomSyncState(true, roomId, "Đối Thủ", "🧑‍🎤", 1, 50, 0, 0, 165, "Đang tập", null, roomEx, roomDur, true);
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
        return new PvpRoomSyncState(true, roomId, uName, avt, lvl, dmg, reps, score, angle, hint, frame, roomEx, roomDur, true);
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

