package com.example.vigil.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.vigil.model.DailyCheckInReward;
import com.example.vigil.model.FriendProfile;
import com.example.vigil.model.FriendRequestEntry;
import com.example.vigil.model.Guild;
import com.example.vigil.model.GuildBoss;
import com.example.vigil.model.GuildBossContribution;
import com.example.vigil.model.GuildBossInfo;
import com.example.vigil.model.GuildInvitationEntry;
import com.example.vigil.model.GuildMember;
import com.example.vigil.model.InventoryItem;
import com.example.vigil.model.Item;
import com.example.vigil.model.ItemSlot;
import com.example.vigil.model.LeaderboardEntry;
import com.example.vigil.model.OutgoingGuildInvitation;
import com.example.vigil.model.QuestItem;
import com.example.vigil.model.Rarity;
import com.example.vigil.model.UserProfile;
import com.example.vigil.model.WorkoutHistoryEntry;
import com.example.vigil.model.WorkoutReward;
import com.example.vigil.model.WorkoutSummaryStats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import kotlin.Pair;

/**
 * Cơ sở dữ liệu SQLite cục bộ viết bằng Java tương thích hoàn toàn với Vigil.
 */
public class VigilSqlDb extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "vigil_app.db";
    public static final int DATABASE_VERSION = 5;


    private static volatile VigilSqlDb INSTANCE;

    public static synchronized VigilSqlDb get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new VigilSqlDb(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public VigilSqlDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Bảng users
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL DEFAULT '123456', " +
                "avatar TEXT NOT NULL DEFAULT '🧑‍🎤', " +
                "gold INTEGER NOT NULL DEFAULT 0, " +
                "gems INTEGER NOT NULL DEFAULT 0, " +
                "xp INTEGER NOT NULL DEFAULT 0, " +
                "level INTEGER NOT NULL DEFAULT 1, " +
                "streak INTEGER NOT NULL DEFAULT 0, " +
                "total_reps INTEGER NOT NULL DEFAULT 0, " +
                "stage INTEGER NOT NULL DEFAULT 1, " +
                "last_workout_date TEXT" +
                ");");

        // 2. Bảng items
        db.execSQL("CREATE TABLE IF NOT EXISTS items (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "slot TEXT NOT NULL, " +
                "rarity TEXT NOT NULL, " +
                "price_gold INTEGER NOT NULL DEFAULT 0, " +
                "price_gems INTEGER NOT NULL DEFAULT 0, " +
                "bonus_str INTEGER NOT NULL DEFAULT 0, " +
                "bonus_end INTEGER NOT NULL DEFAULT 0, " +
                "bonus_pre INTEGER NOT NULL DEFAULT 0, " +
                "bonus_luck INTEGER NOT NULL DEFAULT 0, " +
                "description TEXT, " +
                "icon TEXT NOT NULL DEFAULT '⚔️'" +
                ");");

        // 3. Bảng user_inventory
        db.execSQL("CREATE TABLE IF NOT EXISTS user_inventory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "item_id TEXT NOT NULL, " +
                "is_equipped INTEGER NOT NULL DEFAULT 0, " +
                "acquired_at TEXT, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (item_id) REFERENCES items(id)" +
                ");");

        // 4. Bảng workouts
        db.execSQL("CREATE TABLE IF NOT EXISTS workouts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "exercise TEXT NOT NULL, " +
                "reps INTEGER NOT NULL DEFAULT 0, " +
                "hold_seconds INTEGER NOT NULL DEFAULT 0, " +
                "score INTEGER NOT NULL, " +
                "xp_earned INTEGER NOT NULL, " +
                "gold_earned INTEGER NOT NULL, " +
                "dropped_item_id TEXT, " +
                "created_at TEXT" +
                ");");

        // 5. Bảng friends
        db.execSQL("CREATE TABLE IF NOT EXISTS friends (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "friend_id INTEGER NOT NULL, " +
                "created_at TEXT, " +
                "UNIQUE(user_id, friend_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (friend_id) REFERENCES users(id)" +
                ");");

        // 6. Bảng guilds
        db.execSQL("CREATE TABLE IF NOT EXISTS guilds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL, " +
                "badge TEXT NOT NULL DEFAULT '🛡️', " +
                "slogan TEXT NOT NULL, " +
                "leader_id INTEGER NOT NULL, " +
                "level INTEGER NOT NULL DEFAULT 1, " +
                "created_at TEXT" +
                ");");

        // 7. Bảng guild_members
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "guild_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL UNIQUE, " +
                "role TEXT NOT NULL DEFAULT 'MEMBER', " +
                "joined_at TEXT, " +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // 8. Bảng boss_clears
        db.execSQL("CREATE TABLE IF NOT EXISTS boss_clears (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "stage_id INTEGER NOT NULL, " +
                "cleared_at TEXT, " +
                "UNIQUE(user_id, stage_id)" +
                ");");

        // 9. Bảng weekly_checkins
        db.execSQL("CREATE TABLE IF NOT EXISTS weekly_checkins (" +
                "user_id INTEGER PRIMARY KEY, " +
                "checkin_day INTEGER NOT NULL DEFAULT 0, " +
                "last_date TEXT, " +
                "week_key TEXT" +
                ");");

        // 10. Bảng quest_claims
        db.execSQL("CREATE TABLE IF NOT EXISTS quest_claims (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "quest_id TEXT NOT NULL, " +
                "period_key TEXT NOT NULL, " +
                "claimed_at TEXT, " +
                "UNIQUE(user_id, quest_id, period_key)" +
                ");");

        // 11. Bảng friend_requests
        db.execSQL("CREATE TABLE IF NOT EXISTS friend_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sender_id INTEGER NOT NULL, " +
                "receiver_id INTEGER NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'PENDING', " +
                "created_at TEXT, " +
                "UNIQUE(sender_id, receiver_id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                "FOREIGN KEY (receiver_id) REFERENCES users(id)" +
                ");");

        // 12. Bảng guild_invitations
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_invitations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "guild_id INTEGER NOT NULL, " +
                "inviter_id INTEGER NOT NULL, " +
                "invitee_id INTEGER NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'PENDING', " +
                "created_at TEXT, " +
                "UNIQUE(guild_id, invitee_id, status), " +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                "FOREIGN KEY (inviter_id) REFERENCES users(id), " +
                "FOREIGN KEY (invitee_id) REFERENCES users(id)" +
                ");");

        // 13. Bảng guild_boss (Boss Thế Giới Bang Hội)
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_boss (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "guild_id INTEGER NOT NULL, " +
                "boss_id TEXT NOT NULL DEFAULT 'boss_nether_dragon', " +
                "boss_name TEXT NOT NULL DEFAULT 'Hắc Long Viễn Cổ - Nidhogg', " +
                "boss_title TEXT NOT NULL DEFAULT 'SIÊU TRÙM THẾ GIỚI BANG HỘI', " +
                "boss_avatar TEXT NOT NULL DEFAULT '🐉', " +
                "max_hp INTEGER NOT NULL DEFAULT 500000, " +
                "current_hp INTEGER NOT NULL DEFAULT 500000, " +
                "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
                "reward_gold INTEGER NOT NULL DEFAULT 15000, " +
                "reward_gems INTEGER NOT NULL DEFAULT 350, " +
                "reward_item_id TEXT DEFAULT 'weapon_dragon_slayer', " +
                "created_at TEXT, " +
                "defeated_at TEXT, " +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id)" +
                ");");

        // 14. Bảng guild_boss_damage (Đóng góp sát thương của thành viên)
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_boss_damage (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "guild_id INTEGER NOT NULL, " +
                "boss_db_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL, " +
                "damage INTEGER NOT NULL DEFAULT 0, " +
                "reps_contributed INTEGER NOT NULL DEFAULT 0, " +
                "last_attack_at TEXT, " +
                "has_claimed_defeat_reward INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(boss_db_id, user_id), " +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                "FOREIGN KEY (boss_db_id) REFERENCES guild_boss(id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        seedItems(db);
        seedInitialUser(db);
        seedSampleLeaderboardUsers(db);
        seedSampleGuilds(db);
        seedSampleGuildBosses(db);
        seedSampleRequests(db);
        seedSampleWorkouts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE users ADD COLUMN password TEXT NOT NULL DEFAULT '123456'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN avatar TEXT NOT NULL DEFAULT '🧑‍🎤'"); } catch (Exception ignored) {}
            db.execSQL("CREATE TABLE IF NOT EXISTS friends (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "friend_id INTEGER NOT NULL, " +
                    "created_at TEXT, " +
                    "UNIQUE(user_id, friend_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "FOREIGN KEY (friend_id) REFERENCES users(id)" +
                    ");");
            seedSampleLeaderboardUsers(db);
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS guilds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL, " +
                    "badge TEXT NOT NULL DEFAULT '🛡️', " +
                    "slogan TEXT NOT NULL, " +
                    "leader_id INTEGER NOT NULL, " +
                    "level INTEGER NOT NULL DEFAULT 1, " +
                    "created_at TEXT" +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS guild_members (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "guild_id INTEGER NOT NULL, " +
                    "user_id INTEGER NOT NULL UNIQUE, " +
                    "role TEXT NOT NULL DEFAULT 'MEMBER', " +
                    "joined_at TEXT, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");
            seedSampleGuilds(db);
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS friend_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sender_id INTEGER NOT NULL, " +
                    "receiver_id INTEGER NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'PENDING', " +
                    "created_at TEXT, " +
                    "UNIQUE(sender_id, receiver_id), " +
                    "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                    "FOREIGN KEY (receiver_id) REFERENCES users(id)" +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS guild_invitations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "guild_id INTEGER NOT NULL, " +
                    "inviter_id INTEGER NOT NULL, " +
                    "invitee_id INTEGER NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'PENDING', " +
                    "created_at TEXT, " +
                    "UNIQUE(guild_id, invitee_id, status), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                    "FOREIGN KEY (inviter_id) REFERENCES users(id), " +
                    "FOREIGN KEY (invitee_id) REFERENCES users(id)" +
                    ");");
            seedSampleRequests(db);
            seedSampleWorkouts(db);
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS guild_boss (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "guild_id INTEGER NOT NULL, " +
                    "boss_id TEXT NOT NULL DEFAULT 'boss_nether_dragon', " +
                    "boss_name TEXT NOT NULL DEFAULT 'Hắc Long Viễn Cổ - Nidhogg', " +
                    "boss_title TEXT NOT NULL DEFAULT 'SIÊU TRÙM THẾ GIỚI BANG HỘI', " +
                    "boss_avatar TEXT NOT NULL DEFAULT '🐉', " +
                    "max_hp INTEGER NOT NULL DEFAULT 500000, " +
                    "current_hp INTEGER NOT NULL DEFAULT 500000, " +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    "reward_gold INTEGER NOT NULL DEFAULT 15000, " +
                    "reward_gems INTEGER NOT NULL DEFAULT 350, " +
                    "reward_item_id TEXT DEFAULT 'weapon_dragon_slayer', " +
                    "created_at TEXT, " +
                    "defeated_at TEXT, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id)" +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS guild_boss_damage (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "guild_id INTEGER NOT NULL, " +
                    "boss_db_id INTEGER NOT NULL, " +
                    "user_id INTEGER NOT NULL, " +
                    "damage INTEGER NOT NULL DEFAULT 0, " +
                    "reps_contributed INTEGER NOT NULL DEFAULT 0, " +
                    "last_attack_at TEXT, " +
                    "has_claimed_defeat_reward INTEGER NOT NULL DEFAULT 0, " +
                    "UNIQUE(boss_db_id, user_id), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id), " +
                    "FOREIGN KEY (boss_db_id) REFERENCES guild_boss(id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");
            seedItems(db);
            seedSampleGuildBosses(db);
        }
    }


    private void seedInitialUser(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("id", 1);
        cv.put("username", "Hachimi");
        cv.put("password", "123456");
        cv.put("avatar", "🦊");
        cv.put("gold", 0);
        cv.put("gems", 0);
        cv.put("xp", 120);
        cv.put("level", 5);
        cv.put("streak", 4);
        cv.put("total_reps", 160);
        cv.put("stage", 3);
        cv.put("last_workout_date", LocalDate.now().toString());
        db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues invCv = new ContentValues();
        invCv.put("user_id", 1);
        invCv.put("item_id", "helm_bronze");
        invCv.put("is_equipped", 1);
        invCv.put("acquired_at", LocalDate.now().toString());
        db.insertWithOnConflict("user_inventory", null, invCv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static class SampleLeader {
        String name;
        int level;
        int reps;
        String avatar;
        int streak;
        int gold;
        SampleLeader(String name, int level, int reps, String avatar, int streak, int gold) {
            this.name = name; this.level = level; this.reps = reps;
            this.avatar = avatar; this.streak = streak; this.gold = gold;
        }
    }

    private void seedSampleLeaderboardUsers(SQLiteDatabase db) {
        List<SampleLeader> sampleUsers = Arrays.asList(
                new SampleLeader("ShadowBlade", 9, 380, "🥷", 12, 850),
                new SampleLeader("ValkyrieGym", 8, 310, "👑", 8, 700),
                new SampleLeader("IronTitan", 7, 260, "🗿", 5, 450),
                new SampleLeader("DragonFit", 6, 190, "🐉", 4, 320),
                new SampleLeader("Phoenix", 4, 110, "🔥", 2, 220)
        );

        for (int idx = 0; idx < sampleUsers.size(); idx++) {
            SampleLeader s = sampleUsers.get(idx);
            ContentValues uCv = new ContentValues();
            uCv.put("id", idx + 2);
            uCv.put("username", s.name);
            uCv.put("password", "123456");
            uCv.put("avatar", s.avatar);
            uCv.put("gold", s.gold);
            uCv.put("gems", 10);
            uCv.put("xp", 60);
            uCv.put("level", s.level);
            uCv.put("streak", s.streak);
            uCv.put("total_reps", s.reps);
            uCv.put("stage", s.level - 1);
            uCv.put("last_workout_date", LocalDate.now().toString());
            db.insertWithOnConflict("users", null, uCv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        ContentValues f1 = new ContentValues();
        f1.put("user_id", 1);
        f1.put("friend_id", 4);
        f1.put("created_at", LocalDate.now().toString());
        ContentValues f2 = new ContentValues();
        f2.put("user_id", 4);
        f2.put("friend_id", 1);
        f2.put("created_at", LocalDate.now().toString());
        db.insertWithOnConflict("friends", null, f1, SQLiteDatabase.CONFLICT_IGNORE);
        db.insertWithOnConflict("friends", null, f2, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void seedItems(SQLiteDatabase db) {
        List<Item> itemsList = Arrays.asList(
                // MŨ GIÁP
                new Item("helm_bronze", "Mũ Đồng Tân Binh", ItemSlot.HELMET, Rarity.COMMON, 50, 0, 0, 3, 0, 1, "Mũ đồng đúc thô sơ che chắn đầu khi tập nặng.", "🪖"),
                new Item("helm_iron", "Thiết Giáp Đầu", ItemSlot.HELMET, Rarity.RARE, 160, 0, 3, 7, 0, 2, "Rèn từ sắt non tôi luyện nhiệt độ cao.", "⛑️"),
                new Item("helm_valkyrie", "Mũ Lông Vũ Valkyrie", ItemSlot.HELMET, Rarity.EPIC, 420, 5, 5, 14, 0, 8, "Ban phước bởi nữ thần chiến trận phương Bắc.", "👑"),
                new Item("helm_dragon", "Vương Miện Long Thần", ItemSlot.HELMET, Rarity.LEGENDARY, 1000, 20, 18, 25, 0, 12, "Tỏa ra uy áp của loài rồng cổ đại, tăng cực đại thể lực.", "🐉"),

                // GIÁP NGỰC
                new Item("armor_leather", "Áo Da Dã Ngoại", ItemSlot.ARMOR, Rarity.COMMON, 60, 0, 0, 4, 2, 0, "Áo da bò mềm mại, thoáng mát cho buổi hít đất dài.", "🥋"),
                new Item("armor_plate", "Chiến Giáp Thép Nung", ItemSlot.ARMOR, Rarity.RARE, 180, 0, 5, 10, 0, 0, "Tấm giáp kiên cố bảo vệ cơ hoành và lưng dưới.", "🛡️"),
                new Item("armor_shadow", "Áo Choàng Bóng Đêm", ItemSlot.ARMOR, Rarity.EPIC, 450, 6, 10, 0, 12, 8, "Hòa mình vào bóng tối, tăng tập trung và chuẩn xác.", "🥷"),
                new Item("armor_celestial", "Thánh Giáp Quang Minh", ItemSlot.ARMOR, Rarity.LEGENDARY, 1200, 25, 20, 28, 15, 0, "Ánh hào quang chiếu rọi bảo bọc chiến binh bền bỉ.", "✨"),

                // GĂNG TAY
                new Item("gloves_cloth", "Băng Quấn Cổ Tay", ItemSlot.GLOVES, Rarity.COMMON, 40, 0, 3, 0, 2, 0, "Bảo vệ khớp cổ tay khi chống đẩy trên sàn cứng.", "🥊"),
                new Item("gloves_grip", "Găng Hít Đất Siêu Bám", ItemSlot.GLOVES, Rarity.RARE, 150, 0, 8, 0, 6, 0, "Đế cao su hạt kim cương chống trượt tay hoàn đối.", "🧤"),
                new Item("gloves_titan", "Găng Titan Siêu Lực", ItemSlot.GLOVES, Rarity.EPIC, 400, 5, 18, 0, 8, 3, "Khung titan trợ lực giúp bùng nổ lực đẩy cánh tay.", "🦾"),
                new Item("gloves_infinity", "Găng Tay Vô Cực", ItemSlot.GLOVES, Rarity.LEGENDARY, 1100, 22, 32, 0, 16, 14, "Nắm giữ sức mạnh vũ trụ gom tụ trong từng thớ cơ.", "🌌"),

                // GIÀY
                new Item("boots_runner", "Giày Chạy Phản Lực", ItemSlot.BOOTS, Rarity.COMMON, 45, 0, 0, 3, 0, 2, "Êm ái, giảm chấn gối khi squat hoặc bật nhảy.", "👟"),
                new Item("boots_iron", "Hộ Chân Chiến Binh", ItemSlot.BOOTS, Rarity.RARE, 150, 0, 6, 7, 0, 0, "Bọc thép mũi chân và ống quyển vững chãi.", "🥾"),
                new Item("boots_winged", "Hài Phong Thần Hermes", ItemSlot.BOOTS, Rarity.EPIC, 380, 5, 0, 15, 6, 12, "Đôi giày có cánh lướt đi nhẹ tựa lông hồng.", "🪽"),
                new Item("boots_abyss", "Bộ Bước Vực Thẳm", ItemSlot.BOOTS, Rarity.LEGENDARY, 950, 18, 18, 24, 0, 15, "Mỗi bước chân để lại uy chấn khiến kẻ thù khiếp đảm.", "⚡"),

                // BÙA CHÚ
                new Item("amulet_stone", "Bùa Đá May Mắn", ItemSlot.AMULET, Rarity.COMMON, 50, 0, 0, 0, 0, 4, "Hòn đá cuội ven suối đem lại vận may khi tập.", "🪬"),
                new Item("amulet_ruby", "Huyết Ngọc Hồi Phục", ItemSlot.AMULET, Rarity.RARE, 190, 0, 6, 6, 0, 5, "Viên hồng ngọc đẩy nhanh tốc độ phục hồi cơ bắp.", "🔮"),
                new Item("amulet_eye", "Mắt Ưng Tinh Anh", ItemSlot.AMULET, Rarity.EPIC, 480, 7, 10, 0, 16, 10, "Giúp nhìn rõ từng biên độ góc khớp chuẩn từng mm.", "👁️"),
                new Item("amulet_sun", "Thái Dương Cổ Thạch", ItemSlot.AMULET, Rarity.LEGENDARY, 1300, 30, 20, 20, 20, 25, "Cội nguồn sinh lực vĩnh cửu của mặt trời thiêu đốt.", "☀️"),

                // VŨ KHÍ
                new Item("weapon_stick", "Côn Gỗ Luyện Tập", ItemSlot.WEAPON, Rarity.COMMON, 50, 0, 4, 0, 2, 0, "Khúc gỗ sồi chắc nịch dùng để rèn luyện cổ tay.", "🪵"),
                new Item("weapon_sword", "Thanh Kiếm Thép Đúc", ItemSlot.WEAPON, Rarity.RARE, 180, 0, 10, 0, 6, 0, "Lưỡi kiếm sắc bén rèn từ lò luyện kim hoàng gia.", "⚔️"),
                new Item("weapon_axe", "Rìu Chiến Berserker", ItemSlot.WEAPON, Rarity.EPIC, 460, 6, 22, 8, 0, 0, "Chiếc rìu khổng lồ của chiến binh cuồng nộ.", "🪓"),
                new Item("weapon_excalibur", "Thánh Kiếm Excalibur", ItemSlot.WEAPON, Rarity.LEGENDARY, 1400, 30, 35, 0, 18, 15, "Bảo kiếm huyền thoại cắm sâu trong đá.", "🗡️"),

                // TRANG BỊ ĐẶC BIỆT TỪ BOSS THẾ GIỚI BANG HỘI
                new Item("weapon_dragon_slayer", "Đại Đao Trảm Long", ItemSlot.WEAPON, Rarity.LEGENDARY, 3500, 80, 55, 10, 25, 20, "Thần binh rèn từ vảy và răng Hắc Long, uy lực hủy thiên diệt địa.", "🗡️"),
                new Item("armor_dragon_scale", "Long Lân Thần Giáp", ItemSlot.ARMOR, Rarity.LEGENDARY, 3200, 75, 25, 45, 15, 20, "Lớp vảy rồng kiên cố bất khả xâm phạm bảo vệ toàn thân.", "🐲"),
                new Item("amulet_boss_heart", "Trái Tim Hắc Long", ItemSlot.AMULET, Rarity.LEGENDARY, 4000, 100, 25, 25, 25, 35, "Tinh hoa sinh mệnh của Siêu Trùm Thế Giới ban phước lành.", "💎")
        );

        for (Item item : itemsList) {
            ContentValues cv = new ContentValues();
            cv.put("id", item.getId());
            cv.put("name", item.getName());
            cv.put("slot", item.getSlot().name());
            cv.put("rarity", item.getRarity().name());
            cv.put("price_gold", item.getPriceGold());
            cv.put("price_gems", item.getPriceGems());
            cv.put("bonus_str", item.getBonusStr());
            cv.put("bonus_end", item.getBonusEnd());
            cv.put("bonus_pre", item.getBonusPre());
            cv.put("bonus_luck", item.getBonusLuck());
            cv.put("description", item.getDescription());
            cv.put("icon", item.getIcon());
            db.insertWithOnConflict("items", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void seedSampleGuilds(SQLiteDatabase db) {
        String[] guildNames = {"Hiệp Sĩ Bàn Tròn", "Chiến Binh Rồng", "Lôi Thần Điện"};
        String[] badges = {"🛡️", "🐉", "⚡"};
        String[] slogans = {
                "Danh dự, kỷ luật và sức mạnh vượt qua mọi giới hạn!",
                "Ngọn lửa kiên trì thiêu đốt mọi mệt mỏi và lười biếng!",
                "Nhanh như chớp, uy lực như sấm sét trong từng bài tập!"
        };
        int[] leaders = {2, 5, 3};

        for (int i = 0; i < guildNames.length; i++) {
            ContentValues gCv = new ContentValues();
            gCv.put("id", i + 1);
            gCv.put("name", guildNames[i]);
            gCv.put("badge", badges[i]);
            gCv.put("slogan", slogans[i]);
            gCv.put("leader_id", leaders[i]);
            gCv.put("level", 3 - i);
            gCv.put("created_at", LocalDate.now().toString());
            db.insertWithOnConflict("guilds", null, gCv, SQLiteDatabase.CONFLICT_IGNORE);

            ContentValues mCv = new ContentValues();
            mCv.put("guild_id", i + 1);
            mCv.put("user_id", leaders[i]);
            mCv.put("role", "LEADER");
            mCv.put("joined_at", LocalDate.now().toString());
            db.insertWithOnConflict("guild_members", null, mCv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void seedSampleGuildBosses(SQLiteDatabase db) {
        String[][] bosses = {
                {"1", "1", "boss_nether_dragon", "Hắc Long Viễn Cổ - Nidhogg", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐉", "500000", "385000", "ACTIVE", "15000", "350", "weapon_dragon_slayer"},
                {"2", "2", "boss_inferno_titan", "Cự Nhân Hỏa Ngục - Surtr", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🌋", "650000", "490000", "ACTIVE", "18000", "400", "armor_dragon_scale"},
                {"3", "3", "boss_void_behemoth", "Thần Thú Hư Không - Leviathan", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐲", "800000", "800000", "ACTIVE", "22000", "500", "amulet_boss_heart"}
        };
        for (String[] b : bosses) {
            ContentValues cv = new ContentValues();
            cv.put("id", Integer.parseInt(b[0]));
            cv.put("guild_id", Integer.parseInt(b[1]));
            cv.put("boss_id", b[2]);
            cv.put("boss_name", b[3]);
            cv.put("boss_title", b[4]);
            cv.put("boss_avatar", b[5]);
            cv.put("max_hp", Integer.parseInt(b[6]));
            cv.put("current_hp", Integer.parseInt(b[7]));
            cv.put("status", b[8]);
            cv.put("reward_gold", Integer.parseInt(b[9]));
            cv.put("reward_gems", Integer.parseInt(b[10]));
            cv.put("reward_item_id", b[11]);
            cv.put("created_at", LocalDate.now().toString());
            db.insertWithOnConflict("guild_boss", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        ContentValues dmgCv = new ContentValues();
        dmgCv.put("guild_id", 1);
        dmgCv.put("boss_db_id", 1);
        dmgCv.put("user_id", 2);
        dmgCv.put("damage", 115000);
        dmgCv.put("reps_contributed", 120);
        dmgCv.put("has_claimed_defeat_reward", 0);
        db.insertWithOnConflict("guild_boss_damage", null, dmgCv, SQLiteDatabase.CONFLICT_IGNORE);
    }


    private void seedSampleRequests(SQLiteDatabase db) {
        ContentValues frCv = new ContentValues();
        frCv.put("sender_id", 3);
        frCv.put("receiver_id", 1);
        frCv.put("status", "PENDING");
        frCv.put("created_at", LocalDate.now().toString());
        db.insertWithOnConflict("friend_requests", null, frCv, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues giCv = new ContentValues();
        giCv.put("guild_id", 2);
        giCv.put("inviter_id", 5);
        giCv.put("invitee_id", 1);
        giCv.put("status", "PENDING");
        giCv.put("created_at", LocalDate.now().toString());
        db.insertWithOnConflict("guild_invitations", null, giCv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static class SampleWorkout {
        String exercise; int reps; int holdSec; int score; int xp; int gold; String dropId; long daysAgo;
        SampleWorkout(String ex, int reps, int holdSec, int score, int xp, int gold, String dropId, long daysAgo) {
            this.exercise = ex; this.reps = reps; this.holdSec = holdSec; this.score = score;
            this.xp = xp; this.gold = gold; this.dropId = dropId; this.daysAgo = daysAgo;
        }
    }

    private void seedSampleWorkouts(SQLiteDatabase db) {
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM workouts WHERE user_id = 1", null);
        int count = countCursor.moveToFirst() ? countCursor.getInt(0) : 0;
        countCursor.close();

        if (count == 0) {
            LocalDate today = LocalDate.now();
            List<SampleWorkout> samples = Arrays.asList(
                    new SampleWorkout("SQUAT", 30, 0, 94, 45, 30, "helm_bronze", 3L),
                    new SampleWorkout("PUSHUP", 25, 0, 88, 38, 25, null, 2L),
                    new SampleWorkout("PLANK", 30, 60, 96, 50, 35, "gloves_cloth", 1L),
                    new SampleWorkout("SQUAT", 40, 0, 98, 70, 50, "armor_leather", 0L)
            );

            for (SampleWorkout sw : samples) {
                String dateStr = today.minusDays(sw.daysAgo).toString();
                ContentValues wCv = new ContentValues();
                wCv.put("user_id", 1);
                wCv.put("exercise", sw.exercise);
                wCv.put("reps", sw.reps);
                wCv.put("hold_seconds", sw.holdSec);
                wCv.put("score", sw.score);
                wCv.put("xp_earned", sw.xp);
                wCv.put("gold_earned", sw.gold);
                wCv.put("dropped_item_id", sw.dropId);
                wCv.put("created_at", dateStr);
                db.insert("workouts", null, wCv);
            }
        }
    }

    // =========================================================================
    // SHOP & INVENTORY OPERATIONS
    // =========================================================================

    public List<Item> getAllItems() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM items ORDER BY price_gold ASC", null);
        while (cursor.moveToNext()) {
            list.add(parseItem(cursor));
        }
        cursor.close();
        return list;
    }

    public List<InventoryItem> getInventory(int userId) {
        List<InventoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT ui.id as inv_id, ui.is_equipped, ui.acquired_at, i.* " +
                "FROM user_inventory ui " +
                "JOIN items i ON ui.item_id = i.id " +
                "WHERE ui.user_id = ? " +
                "ORDER BY ui.is_equipped DESC, ui.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            int invId = cursor.getInt(cursor.getColumnIndexOrThrow("inv_id"));
            boolean isEquipped = cursor.getInt(cursor.getColumnIndexOrThrow("is_equipped")) == 1;
            String acquiredAt = cursor.getString(cursor.getColumnIndexOrThrow("acquired_at"));
            Item item = parseItem(cursor);
            list.add(new InventoryItem(invId, item, isEquipped, acquiredAt != null ? acquiredAt : ""));
        }
        cursor.close();
        return list;
    }

    public boolean buyItem(int userId, Item item) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor userCursor = db.rawQuery("SELECT gold, gems FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
            if (!userCursor.moveToNext()) {
                userCursor.close();
                return false;
            }
            int currentGold = userCursor.getInt(0);
            int currentGems = userCursor.getInt(1);
            userCursor.close();

            if (currentGold < item.getPriceGold() || currentGems < item.getPriceGems()) {
                return false;
            }

            db.execSQL("UPDATE users SET gold = gold - ?, gems = gems - ? WHERE id = ?",
                    new Object[]{item.getPriceGold(), item.getPriceGems(), userId});

            ContentValues invCv = new ContentValues();
            invCv.put("user_id", userId);
            invCv.put("item_id", item.getId());
            invCv.put("is_equipped", 0);
            invCv.put("acquired_at", LocalDate.now().toString());
            db.insert("user_inventory", null, invCv);

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public void equipItem(int userId, int invId, ItemSlot slot) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE user_inventory SET is_equipped = 0 " +
                    "WHERE user_id = ? AND item_id IN (SELECT id FROM items WHERE slot = ?)",
                    new Object[]{userId, slot.name()});
            db.execSQL("UPDATE user_inventory SET is_equipped = 1 WHERE id = ?", new Object[]{invId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void unequipItem(int invId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE user_inventory SET is_equipped = 0 WHERE id = ?", new Object[]{invId});
    }

    public void unequipAll(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE user_inventory SET is_equipped = 0 WHERE user_id = ?", new Object[]{userId});
    }

    public boolean sellItem(int userId, int invId, int sellPrice) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM user_inventory WHERE id = ? AND user_id = ?", new Object[]{invId, userId});
            db.execSQL("UPDATE users SET gold = gold + ? WHERE id = ?", new Object[]{sellPrice, userId});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // WORKOUTS & LOOT DROP SYSTEM
    // =========================================================================

    public WorkoutReward finishWorkoutAndRollDrop(
            int userId,
            String exercise,
            int reps,
            int holdSec,
            int score,
            boolean isBoss,
            boolean isFreeTraining,
            Integer monsterStageId
    ) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
            cursor.moveToFirst();
            int gold = cursor.getInt(cursor.getColumnIndexOrThrow("gold"));
            int gems = cursor.getInt(cursor.getColumnIndexOrThrow("gems"));
            int xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"));
            int level = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
            int streak = cursor.getInt(cursor.getColumnIndexOrThrow("streak"));
            int totalReps = cursor.getInt(cursor.getColumnIndexOrThrow("total_reps"));
            int stage = cursor.getInt(cursor.getColumnIndexOrThrow("stage"));
            String lastWorkout = cursor.getString(cursor.getColumnIndexOrThrow("last_workout_date"));
            cursor.close();

            String todayStr = LocalDate.now().toString();
            String yesterdayStr = LocalDate.now().minusDays(1).toString();
            if (!todayStr.equals(lastWorkout)) {
                streak = yesterdayStr.equals(lastWorkout) ? streak + 1 : 1;
            }
            totalReps += score;

            Item droppedItem = null;
            int xpEarned = 0;
            int goldEarned = 0;
            int gemsEarned = 0;
            boolean isBossFirstClear = false;

            if (isFreeTraining) {
                xpEarned = 0;
                goldEarned = 0;
                gemsEarned = 0;
                droppedItem = null;
            } else {
                Cursor luckCursor = db.rawQuery("SELECT SUM(i.bonus_luck) FROM user_inventory ui " +
                        "JOIN items i ON ui.item_id = i.id " +
                        "WHERE ui.user_id = ? AND ui.is_equipped = 1", new String[]{String.valueOf(userId)});
                int bonusLuck = luckCursor.moveToFirst() ? luckCursor.getInt(0) : 0;
                luckCursor.close();

                int totalLuck = 5 + (totalReps / 5) + bonusLuck;
                double baseRate = isBoss ? 0.35 : 0.12;
                double luckBonus = isBoss ? totalLuck * 0.003 : totalLuck * 0.002;
                double maxChance = isBoss ? 0.50 : 0.25;
                double dropChance = Math.min(maxChance, baseRate + luckBonus);
                Random rng = new Random();
                double roll = rng.nextDouble();

                if (roll <= dropChance) {
                    double rarityRoll = rng.nextDouble();
                    Rarity targetRarity;
                    if (isBoss) {
                        if (rarityRoll < Math.min(0.08, 0.02 + (totalLuck * 0.001))) {
                            targetRarity = Rarity.LEGENDARY;
                        } else if (rarityRoll < Math.min(0.25, 0.12 + (totalLuck * 0.002))) {
                            targetRarity = Rarity.EPIC;
                        } else if (rarityRoll < 0.60) {
                            targetRarity = Rarity.RARE;
                        } else {
                            targetRarity = Rarity.COMMON;
                        }
                    } else {
                        if (rarityRoll < Math.min(0.02, 0.005 + (totalLuck * 0.0005))) {
                            targetRarity = Rarity.LEGENDARY;
                        } else if (rarityRoll < Math.min(0.10, 0.04 + (totalLuck * 0.001))) {
                            targetRarity = Rarity.EPIC;
                        } else if (rarityRoll < 0.35) {
                            targetRarity = Rarity.RARE;
                        } else {
                            targetRarity = Rarity.COMMON;
                        }
                    }

                    Cursor candidateCursor = db.rawQuery("SELECT * FROM items WHERE rarity = ? ORDER BY RANDOM() LIMIT 1",
                            new String[]{targetRarity.name()});
                    if (candidateCursor.moveToFirst()) {
                        droppedItem = parseItem(candidateCursor);
                        ContentValues invCv = new ContentValues();
                        invCv.put("user_id", userId);
                        invCv.put("item_id", droppedItem.getId());
                        invCv.put("is_equipped", 0);
                        invCv.put("acquired_at", todayStr);
                        db.insert("user_inventory", null, invCv);
                    }
                    candidateCursor.close();
                }

                if (isBoss && monsterStageId != null) {
                    Cursor clearCheck = db.rawQuery("SELECT id FROM boss_clears WHERE user_id = ? AND stage_id = ?",
                            new String[]{String.valueOf(userId), String.valueOf(monsterStageId)});
                    boolean alreadyCleared = clearCheck.moveToFirst();
                    clearCheck.close();

                    if (!alreadyCleared) {
                        isBossFirstClear = true;
                        gemsEarned = (monsterStageId == 12) ? 50 : 20;
                        gems += gemsEarned;

                        ContentValues clearCv = new ContentValues();
                        clearCv.put("user_id", userId);
                        clearCv.put("stage_id", monsterStageId);
                        clearCv.put("cleared_at", todayStr);
                        db.insert("boss_clears", null, clearCv);
                    }
                }

                int baseGoldPerRep = isBoss ? 4 : 2;
                goldEarned = (score * baseGoldPerRep) + (totalLuck / 2);
                xpEarned = score * 5;

                gold += goldEarned;
                xp += xpEarned;

                if (!isBoss || monsterStageId != null) {
                    if (monsterStageId != null && monsterStageId == stage) {
                        stage = Math.min(12, stage + 1);
                    }
                }

                int xpNeeded = 80 + level * 160;
                while (xp >= xpNeeded) {
                    xp -= xpNeeded;
                    level += 1;
                    xpNeeded = 80 + level * 160;
                }
            }

            ContentValues userCv = new ContentValues();
            userCv.put("gold", gold);
            userCv.put("gems", gems);
            userCv.put("xp", xp);
            userCv.put("level", level);
            userCv.put("streak", streak);
            userCv.put("total_reps", totalReps);
            userCv.put("stage", stage);
            userCv.put("last_workout_date", todayStr);
            db.update("users", userCv, "id = ?", new String[]{String.valueOf(userId)});

            ContentValues wCv = new ContentValues();
            wCv.put("user_id", userId);
            wCv.put("exercise", exercise);
            wCv.put("reps", reps);
            wCv.put("hold_seconds", holdSec);
            wCv.put("score", score);
            wCv.put("xp_earned", xpEarned);
            wCv.put("gold_earned", goldEarned);
            wCv.put("dropped_item_id", droppedItem != null ? droppedItem.getId() : null);
            wCv.put("created_at", todayStr);
            db.insert("workouts", null, wCv);

            db.setTransactionSuccessful();

            return new WorkoutReward(
                    score, xpEarned, goldEarned, level, gold, xp, streak, droppedItem, gemsEarned, gems, isBossFirstClear
            );
        } finally {
            db.endTransaction();
        }
    }

    public WorkoutReward finishWorkoutAndRollDrop(
            int userId,
            String exercise,
            int reps,
            int holdSec,
            int score,
            boolean isBoss,
            boolean isFreeTraining
    ) {
        return finishWorkoutAndRollDrop(userId, exercise, reps, holdSec, score, isBoss, isFreeTraining, null);
    }

    public Item parseItem(Cursor c) {
        String id = c.getString(c.getColumnIndexOrThrow("id"));
        String name = c.getString(c.getColumnIndexOrThrow("name"));
        ItemSlot slot = ItemSlot.valueOf(c.getString(c.getColumnIndexOrThrow("slot")));
        Rarity rarity = Rarity.valueOf(c.getString(c.getColumnIndexOrThrow("rarity")));
        int priceGold = c.getInt(c.getColumnIndexOrThrow("price_gold"));
        int priceGems = c.getInt(c.getColumnIndexOrThrow("price_gems"));
        int bonusStr = c.getInt(c.getColumnIndexOrThrow("bonus_str"));
        int bonusEnd = c.getInt(c.getColumnIndexOrThrow("bonus_end"));
        int bonusPre = c.getInt(c.getColumnIndexOrThrow("bonus_pre"));
        int bonusLuck = c.getInt(c.getColumnIndexOrThrow("bonus_luck"));
        String description = c.getString(c.getColumnIndexOrThrow("description"));
        String icon = c.getString(c.getColumnIndexOrThrow("icon"));

        return new Item(id, name, slot, rarity, priceGold, priceGems,
                bonusStr, bonusEnd, bonusPre, bonusLuck,
                description != null ? description : "", icon != null ? icon : "⚔️");
    }

    public List<WorkoutHistoryEntry> getWorkoutHistory(int userId, int limit) {
        List<WorkoutHistoryEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT w.id, w.exercise, w.reps, w.hold_seconds, w.score, w.xp_earned, w.gold_earned, " +
                "w.dropped_item_id, w.created_at, " +
                "i.name AS item_name, i.slot AS item_slot, i.rarity AS item_rarity, " +
                "i.price_gold AS item_gold, i.price_gems AS item_gems, " +
                "i.bonus_str AS item_str, i.bonus_end AS item_end, i.bonus_pre AS item_pre, i.bonus_luck AS item_luck, " +
                "i.description AS item_desc, i.icon AS item_icon " +
                "FROM workouts w " +
                "LEFT JOIN items i ON w.dropped_item_id = i.id " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.id DESC " +
                "LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(limit)});
        while (cursor.moveToNext()) {
            int wId = cursor.getInt(0);
            String ex = cursor.getString(1);
            int reps = cursor.getInt(2);
            int holdSec = cursor.getInt(3);
            int score = cursor.getInt(4);
            int xp = cursor.getInt(5);
            int gold = cursor.getInt(6);
            String dropId = cursor.getString(7);
            String createdAt = cursor.getString(8);

            Item droppedItem = null;
            if (dropId != null && !cursor.isNull(9)) {
                String itemName = cursor.getString(9);
                ItemSlot itemSlot = ItemSlot.valueOf(cursor.getString(10));
                Rarity itemRarity = Rarity.valueOf(cursor.getString(11));
                int itemGold = cursor.getInt(12);
                int itemGems = cursor.getInt(13);
                int itemStr = cursor.getInt(14);
                int itemEnd = cursor.getInt(15);
                int itemPre = cursor.getInt(16);
                int itemLuck = cursor.getInt(17);
                String itemDesc = cursor.getString(18);
                String itemIcon = cursor.getString(19);

                droppedItem = new Item(dropId, itemName, itemSlot, itemRarity, itemGold, itemGems,
                        itemStr, itemEnd, itemPre, itemLuck,
                        itemDesc != null ? itemDesc : "", itemIcon != null ? itemIcon : "⚔️");
            }

            int calBurned;
            if ("PLANK".equalsIgnoreCase(ex)) {
                calBurned = (int) Math.max(3, holdSec * 0.12);
            } else if ("SQUAT".equalsIgnoreCase(ex)) {
                calBurned = (int) Math.max(5, reps * 0.45);
            } else if ("PUSHUP".equalsIgnoreCase(ex)) {
                calBurned = (int) Math.max(4, reps * 0.38);
            } else {
                calBurned = (int) Math.max(4, reps * 0.40);
            }

            list.add(new WorkoutHistoryEntry(
                    wId, ex, reps, holdSec, score, xp, gold, droppedItem,
                    createdAt != null ? createdAt : "", calBurned
            ));
        }
        cursor.close();
        return list;
    }

    public List<WorkoutHistoryEntry> getWorkoutHistory(int userId) {
        return getWorkoutHistory(userId, 50);
    }

    public WorkoutSummaryStats getWorkoutStats(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*), SUM(reps), SUM(xp_earned), SUM(gold_earned), COUNT(dropped_item_id) " +
                "FROM workouts WHERE user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        int totalWorkouts = 0;
        int totalReps = 0;
        int totalXp = 0;
        int totalGold = 0;
        int droppedCount = 0;
        if (cursor.moveToFirst()) {
            totalWorkouts = cursor.getInt(0);
            totalReps = cursor.getInt(1);
            totalXp = cursor.getInt(2);
            totalGold = cursor.getInt(3);
            droppedCount = cursor.getInt(4);
        }
        cursor.close();

        List<WorkoutHistoryEntry> allHistory = getWorkoutHistory(userId, 500);
        int totalCalories = 0;
        for (WorkoutHistoryEntry entry : allHistory) {
            totalCalories += entry.getCaloriesBurned();
        }

        return new WorkoutSummaryStats(
                totalWorkouts, totalReps, totalCalories, totalXp, totalGold, droppedCount
        );
    }

    // =========================================================================
    // AUTH & USER PROFILES
    // =========================================================================

    public Pair<Boolean, String> registerUser(String username, String password, String avatar) {
        String trimmed = username.trim();
        if (trimmed.length() < 3) return new Pair<>(false, "Tên tài khoản phải có ít nhất 3 ký tự!");
        if (password.length() < 4) return new Pair<>(false, "Mật khẩu phải có ít nhất 4 ký tự!");

        SQLiteDatabase db = getWritableDatabase();
        Cursor checkCursor = db.rawQuery("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", new String[]{trimmed});
        boolean exists = checkCursor.moveToFirst();
        checkCursor.close();

        if (exists) {
            return new Pair<>(false, "Tên tài khoản '" + trimmed + "' đã có người sử dụng!");
        }

        ContentValues cv = new ContentValues();
        cv.put("username", trimmed);
        cv.put("password", password);
        cv.put("avatar", avatar != null ? avatar : "🧑‍🎤");
        cv.put("gold", 0);
        cv.put("gems", 0);
        cv.put("xp", 0);
        cv.put("level", 1);
        cv.put("streak", 1);
        cv.put("total_reps", 0);
        cv.put("stage", 1);
        cv.put("last_workout_date", LocalDate.now().toString());

        long newId = db.insert("users", null, cv);
        if (newId != -1L) {
            ContentValues invCv = new ContentValues();
            invCv.put("user_id", (int) newId);
            invCv.put("item_id", "helm_bronze");
            invCv.put("is_equipped", 1);
            invCv.put("acquired_at", LocalDate.now().toString());
            db.insert("user_inventory", null, invCv);
            return new Pair<>(true, "Đăng ký thành công! Chào mừng hiệp sĩ " + trimmed + "!");
        } else {
            return new Pair<>(false, "Lỗi cơ sở dữ liệu khi tạo tài khoản!");
        }
    }

    public Pair<Integer, String> loginUser(String username, String password) {
        String trimmed = username.trim();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, password FROM users WHERE LOWER(username) = LOWER(?)", new String[]{trimmed});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new Pair<>(null, "Không tìm thấy tài khoản '" + trimmed + "'!");
        }

        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String storedPw = cursor.getString(cursor.getColumnIndexOrThrow("password"));
        cursor.close();

        if (storedPw.equals(password)) {
            return new Pair<>(id, "Đăng nhập thành công! Chào mừng trở lại!");
        } else {
            return new Pair<>(null, "Mật khẩu không chính xác!");
        }
    }

    public UserProfile getUserProfile(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        UserProfile p = parseUserProfile(cursor);
        cursor.close();
        return p;
    }

    public UserProfile getUserProfileByUsername(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        UserProfile p = parseUserProfile(cursor);
        cursor.close();
        return p;
    }

    private UserProfile parseUserProfile(Cursor cursor) {
        int level = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
        String title;
        if (level < 3) title = "Tân Binh";
        else if (level < 6) title = "Võ Tăng";
        else if (level < 10) title = "Chiến Binh";
        else title = "Huyền Thoại";

        return new UserProfile(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getInt(cursor.getColumnIndexOrThrow("gold")),
                cursor.getInt(cursor.getColumnIndexOrThrow("gems")),
                cursor.getInt(cursor.getColumnIndexOrThrow("xp")),
                level,
                cursor.getInt(cursor.getColumnIndexOrThrow("streak")),
                cursor.getInt(cursor.getColumnIndexOrThrow("total_reps")),
                cursor.getInt(cursor.getColumnIndexOrThrow("stage")),
                cursor.getString(cursor.getColumnIndexOrThrow("avatar")),
                title
        );
    }

    // =========================================================================
    // FRIENDS & FRIEND REQUESTS
    // =========================================================================

    public Pair<Boolean, String> sendFriendRequest(int currentUserId, String targetUsername) {
        String targetName = targetUsername.trim();
        if (targetName.isEmpty()) return new Pair<>(false, "Vui lòng nhập tên người chơi!");

        SQLiteDatabase db = getWritableDatabase();
        Cursor targetCursor = db.rawQuery("SELECT id, username FROM users WHERE LOWER(username) = LOWER(?)", new String[]{targetName});
        if (!targetCursor.moveToFirst()) {
            targetCursor.close();
            return new Pair<>(false, "Không tìm thấy người chơi có tên '" + targetName + "'!");
        }
        int friendId = targetCursor.getInt(targetCursor.getColumnIndexOrThrow("id"));
        String canonicalName = targetCursor.getString(targetCursor.getColumnIndexOrThrow("username"));
        targetCursor.close();

        if (friendId == currentUserId) {
            return new Pair<>(false, "Bạn không thể tự kết bạn với chính mình!");
        }

        Cursor checkCursor = db.rawQuery("SELECT id FROM friends WHERE user_id = ? AND friend_id = ?",
                new String[]{String.valueOf(currentUserId), String.valueOf(friendId)});
        boolean alreadyFriends = checkCursor.moveToFirst();
        checkCursor.close();

        if (alreadyFriends) {
            return new Pair<>(false, "Bạn và '" + canonicalName + "' đã là bạn bè rồi!");
        }

        Cursor existingReq = db.rawQuery(
                "SELECT id FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'PENDING'",
                new String[]{String.valueOf(currentUserId), String.valueOf(friendId)}
        );
        if (existingReq.moveToFirst()) {
            existingReq.close();
            return new Pair<>(false, "Bạn đã gửi lời mời kết bạn cho '" + canonicalName + "' rồi, đang chờ chấp nhận!");
        }
        existingReq.close();

        Cursor reverseReq = db.rawQuery(
                "SELECT id FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'PENDING'",
                new String[]{String.valueOf(friendId), String.valueOf(currentUserId)}
        );
        if (reverseReq.moveToFirst()) {
            int reqId = reverseReq.getInt(0);
            reverseReq.close();
            return acceptFriendRequest(reqId, currentUserId);
        }
        reverseReq.close();

        ContentValues cv = new ContentValues();
        cv.put("sender_id", currentUserId);
        cv.put("receiver_id", friendId);
        cv.put("status", "PENDING");
        cv.put("created_at", LocalDate.now().toString());
        db.insertWithOnConflict("friend_requests", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        return new Pair<>(true, "Đã gửi lời mời kết bạn tới '" + canonicalName + "'! Đang chờ đối phương chấp nhận. ✉️");
    }

    public Pair<Boolean, String> addFriendByUsername(int currentUserId, String friendUsername) {
        return sendFriendRequest(currentUserId, friendUsername);
    }

    public Pair<Boolean, String> acceptFriendRequest(int requestId, int currentUserId) {
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT fr.sender_id, u.username FROM friend_requests fr " +
                "JOIN users u ON fr.sender_id = u.id " +
                "WHERE fr.id = ? AND fr.receiver_id = ? AND fr.status = 'PENDING'";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(requestId), String.valueOf(currentUserId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new Pair<>(false, "Không tìm thấy lời mời kết bạn hợp lệ!");
        }
        int senderId = cursor.getInt(0);
        String senderName = cursor.getString(1);
        cursor.close();

        String nowStr = LocalDate.now().toString();
        ContentValues f1 = new ContentValues();
        f1.put("user_id", currentUserId);
        f1.put("friend_id", senderId);
        f1.put("created_at", nowStr);

        ContentValues f2 = new ContentValues();
        f2.put("user_id", senderId);
        f2.put("friend_id", currentUserId);
        f2.put("created_at", nowStr);

        db.insertWithOnConflict("friends", null, f1, SQLiteDatabase.CONFLICT_REPLACE);
        db.insertWithOnConflict("friends", null, f2, SQLiteDatabase.CONFLICT_REPLACE);

        db.delete("friend_requests", "id = ?", new String[]{String.valueOf(requestId)});
        return new Pair<>(true, "Đã chấp nhận lời mời kết bạn từ '" + senderName + "'! 🤝");
    }

    public Pair<Boolean, String> declineFriendRequest(int requestId, int currentUserId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete("friend_requests", "id = ? AND receiver_id = ?",
                new String[]{String.valueOf(requestId), String.valueOf(currentUserId)});
        if (rows > 0) {
            return new Pair<>(true, "Đã từ chối lời mời kết bạn.");
        } else {
            return new Pair<>(false, "Không tìm thấy lời mời cần từ chối.");
        }
    }

    public boolean cancelFriendRequest(int requestId, int currentUserId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("friend_requests", "id = ? AND sender_id = ?",
                new String[]{String.valueOf(requestId), String.valueOf(currentUserId)}) > 0;
    }

    public List<FriendRequestEntry> getIncomingFriendRequests(int currentUserId) {
        List<FriendRequestEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT fr.id, fr.sender_id, u.username, u.level, u.total_reps, u.avatar, fr.created_at " +
                "FROM friend_requests fr " +
                "JOIN users u ON fr.sender_id = u.id " +
                "WHERE fr.receiver_id = ? AND fr.status = 'PENDING' " +
                "ORDER BY fr.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentUserId)});
        while (cursor.moveToNext()) {
            int reqId = cursor.getInt(0);
            int sId = cursor.getInt(1);
            String sName = cursor.getString(2);
            int lvl = cursor.getInt(3);
            int reps = cursor.getInt(4);
            String avt = cursor.getString(5);
            String createdAt = cursor.getString(6);

            int cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30);
            String title;
            if (lvl < 3) title = "Tân Binh";
            else if (lvl < 6) title = "Võ Tăng";
            else if (lvl < 10) title = "Chiến Binh";
            else title = "Huyền Thoại";

            list.add(new FriendRequestEntry(
                    reqId, sId, sName, lvl, avt, title, cp, reps, createdAt != null ? createdAt : ""
            ));
        }
        cursor.close();
        return list;
    }

    public List<FriendRequestEntry> getOutgoingFriendRequests(int currentUserId) {
        List<FriendRequestEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT fr.id, fr.receiver_id, u.username, u.level, u.total_reps, u.avatar, fr.created_at " +
                "FROM friend_requests fr " +
                "JOIN users u ON fr.receiver_id = u.id " +
                "WHERE fr.sender_id = ? AND fr.status = 'PENDING' " +
                "ORDER BY fr.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentUserId)});
        while (cursor.moveToNext()) {
            int reqId = cursor.getInt(0);
            int rId = cursor.getInt(1);
            String rName = cursor.getString(2);
            int lvl = cursor.getInt(3);
            int reps = cursor.getInt(4);
            String avt = cursor.getString(5);
            String createdAt = cursor.getString(6);

            int cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30);
            String title;
            if (lvl < 3) title = "Tân Binh";
            else if (lvl < 6) title = "Võ Tăng";
            else if (lvl < 10) title = "Chiến Binh";
            else title = "Huyền Thoại";

            list.add(new FriendRequestEntry(
                    reqId, rId, rName, lvl, avt, title, cp, reps, createdAt != null ? createdAt : ""
            ));
        }
        cursor.close();
        return list;
    }

    public List<FriendProfile> getFriends(int userId) {
        List<FriendProfile> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT u.id, u.username, u.level, u.total_reps, u.streak, u.avatar " +
                "FROM friends f " +
                "JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.level DESC, u.total_reps DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            int fId = cursor.getInt(0);
            String uname = cursor.getString(1);
            int lvl = cursor.getInt(2);
            int reps = cursor.getInt(3);
            int strk = cursor.getInt(4);
            String avt = cursor.getString(5);

            int cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30);
            String title;
            if (lvl < 3) title = "Tân Binh";
            else if (lvl < 6) title = "Võ Tăng";
            else if (lvl < 10) title = "Chiến Binh";
            else title = "Huyền Thoại";

            list.add(new FriendProfile(
                    fId, uname, lvl, cp, reps, strk, avt, title, true
            ));
        }
        cursor.close();
        return list;
    }

    public boolean removeFriend(int userId, int friendId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("friends", "(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
                new String[]{String.valueOf(userId), String.valueOf(friendId), String.valueOf(friendId), String.valueOf(userId)});
        return true;
    }

    // =========================================================================
    // LEADERBOARD
    // =========================================================================

    public List<LeaderboardEntry> getLeaderboard(int currentUserId, String sortBy) {
        SQLiteDatabase db = getReadableDatabase();
        Set<Integer> friendIds = new HashSet<>();
        Cursor fCursor = db.rawQuery("SELECT friend_id FROM friends WHERE user_id = ?", new String[]{String.valueOf(currentUserId)});
        while (fCursor.moveToNext()) {
            friendIds.add(fCursor.getInt(0));
        }
        fCursor.close();

        Set<Integer> pendingFriendIds = new HashSet<>();
        Cursor pCursor = db.rawQuery("SELECT receiver_id FROM friend_requests WHERE sender_id = ? AND status = 'PENDING'", new String[]{String.valueOf(currentUserId)});
        while (pCursor.moveToNext()) {
            pendingFriendIds.add(pCursor.getInt(0));
        }
        pCursor.close();

        List<LeaderboardEntry> allUsers = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id, username, level, xp, total_reps, streak, avatar FROM users", null);
        while (cursor.moveToNext()) {
            int uId = cursor.getInt(0);
            String uname = cursor.getString(1);
            int lvl = cursor.getInt(2);
            int reps = cursor.getInt(4);
            int strk = cursor.getInt(5);
            String avt = cursor.getString(6);

            int cp = ((5 + reps / 3) * 10) + ((5 + reps / 4) * 8) + ((5 + reps / 3) * 9) + ((5 + reps / 5) * 6) + (lvl * 30);
            String title;
            if (lvl < 3) title = "Tân Binh";
            else if (lvl < 6) title = "Võ Tăng";
            else if (lvl < 10) title = "Chiến Binh";
            else title = "Huyền Thoại";

            allUsers.add(new LeaderboardEntry(
                    0, uId, uname, lvl, cp, reps, strk, avt, title,
                    friendIds.contains(uId), uId == currentUserId, pendingFriendIds.contains(uId)
            ));
        }
        cursor.close();

        Collections.sort(allUsers, (a, b) -> {
            if ("LEVEL".equals(sortBy)) {
                return Integer.compare(b.getLevel(), a.getLevel());
            } else if ("REPS".equals(sortBy)) {
                return Integer.compare(b.getTotalReps(), a.getTotalReps());
            } else if ("STREAK".equals(sortBy)) {
                return Integer.compare(b.getStreak(), a.getStreak());
            } else {
                return Integer.compare(b.getCombatPower(), a.getCombatPower());
            }
        });

        List<LeaderboardEntry> rankedList = new ArrayList<>();
        for (int i = 0; i < allUsers.size(); i++) {
            rankedList.add(allUsers.get(i).copy(i + 1));
        }
        return rankedList;
    }

    public List<LeaderboardEntry> getLeaderboard(int currentUserId) {
        return getLeaderboard(currentUserId, "CP");
    }

    // =========================================================================
    // GUILDS & GUILD INVITATIONS
    // =========================================================================

    public Pair<Boolean, String> createGuild(int userId, String name, String badge, String slogan) {
        String trimmed = name.trim();
        if (trimmed.length() < 3) return new Pair<>(false, "Tên bang hội phải có ít nhất 3 ký tự!");
        if (slogan.trim().isEmpty()) return new Pair<>(false, "Vui lòng nhập khẩu hiệu cho bang hội!");

        SQLiteDatabase db = getWritableDatabase();
        Cursor checkMember = db.rawQuery("SELECT id FROM guild_members WHERE user_id = ?", new String[]{String.valueOf(userId)});
        boolean alreadyInGuild = checkMember.moveToFirst();
        checkMember.close();
        if (alreadyInGuild) return new Pair<>(false, "Bạn đã tham gia một bang hội khác rồi! Hãy rời bang trước khi lập bang mới.");

        Cursor checkName = db.rawQuery("SELECT id FROM guilds WHERE LOWER(name) = LOWER(?)", new String[]{trimmed});
        boolean nameExists = checkName.moveToFirst();
        checkName.close();
        if (nameExists) return new Pair<>(false, "Tên bang hội '" + trimmed + "' đã có người sử dụng!");

        ContentValues gCv = new ContentValues();
        gCv.put("name", trimmed);
        gCv.put("badge", badge != null ? badge : "🛡️");
        gCv.put("slogan", slogan.trim());
        gCv.put("leader_id", userId);
        gCv.put("level", 1);
        gCv.put("created_at", LocalDate.now().toString());

        long guildId = db.insert("guilds", null, gCv);
        if (guildId != -1L) {
            ContentValues mCv = new ContentValues();
            mCv.put("guild_id", (int) guildId);
            mCv.put("user_id", userId);
            mCv.put("role", "LEADER");
            mCv.put("joined_at", LocalDate.now().toString());
            db.insert("guild_members", null, mCv);
            return new Pair<>(true, "👑 Thành lập bang hội '" + trimmed + "' thành công!");
        }
        return new Pair<>(false, "Có lỗi xảy ra khi tạo bang hội!");
    }

    public Pair<Boolean, String> joinGuild(int userId, int guildId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor checkMember = db.rawQuery("SELECT id FROM guild_members WHERE user_id = ?", new String[]{String.valueOf(userId)});
        boolean alreadyInGuild = checkMember.moveToFirst();
        checkMember.close();
        if (alreadyInGuild) return new Pair<>(false, "Bạn đang là thành viên của bang hội khác!");

        Cursor gCursor = db.rawQuery("SELECT name FROM guilds WHERE id = ?", new String[]{String.valueOf(guildId)});
        if (!gCursor.moveToFirst()) {
            gCursor.close();
            return new Pair<>(false, "Bang hội không tồn tại!");
        }
        String guildName = gCursor.getString(0);
        gCursor.close();

        ContentValues mCv = new ContentValues();
        mCv.put("guild_id", guildId);
        mCv.put("user_id", userId);
        mCv.put("role", "MEMBER");
        mCv.put("joined_at", LocalDate.now().toString());

        long res = db.insert("guild_members", null, mCv);
        if (res != -1L) {
            return new Pair<>(true, "⚔️ Chào mừng bạn gia nhập bang hội " + guildName + "!");
        } else {
            return new Pair<>(false, "Không thể gia nhập bang hội!");
        }
    }

    public Pair<Boolean, String> leaveGuild(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor mCursor = db.rawQuery("SELECT guild_id, role FROM guild_members WHERE user_id = ?", new String[]{String.valueOf(userId)});
        if (!mCursor.moveToFirst()) {
            mCursor.close();
            return new Pair<>(false, "Bạn chưa gia nhập bang hội nào!");
        }
        int guildId = mCursor.getInt(0);
        String role = mCursor.getString(1);
        mCursor.close();

        if ("LEADER".equals(role)) {
            db.delete("guild_members", "guild_id = ?", new String[]{String.valueOf(guildId)});
            db.delete("guild_invitations", "guild_id = ?", new String[]{String.valueOf(guildId)});
            db.delete("guilds", "id = ?", new String[]{String.valueOf(guildId)});
            return new Pair<>(true, "👑 Bạn là chủ bang nên bang hội đã được giải tán thành công.");
        } else {
            db.delete("guild_members", "user_id = ?", new String[]{String.valueOf(userId)});
            return new Pair<>(true, "Đã rời khỏi bang hội.");
        }
    }

    public Guild getUserGuild(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT g.id, g.name, g.badge, g.slogan, g.leader_id, u.username, g.level, gm.role " +
                "FROM guild_members gm " +
                "JOIN guilds g ON gm.guild_id = g.id " +
                "JOIN users u ON g.leader_id = u.id " +
                "WHERE gm.user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        int guildId = cursor.getInt(0);
        String name = cursor.getString(1);
        String badge = cursor.getString(2);
        String slogan = cursor.getString(3);
        int leaderId = cursor.getInt(4);
        String leaderName = cursor.getString(5);
        int level = cursor.getInt(6);
        String role = cursor.getString(7);
        cursor.close();

        Cursor cCount = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", new String[]{String.valueOf(guildId)});
        int count = cCount.moveToFirst() ? cCount.getInt(0) : 1;
        cCount.close();

        Cursor cReps = db.rawQuery("SELECT SUM(u.total_reps) FROM guild_members gm " +
                "JOIN users u ON gm.user_id = u.id " +
                "WHERE gm.guild_id = ?", new String[]{String.valueOf(guildId)});
        int reps = cReps.moveToFirst() ? cReps.getInt(0) : 0;
        cReps.close();

        return new Guild(
                guildId, name, badge, slogan, leaderId, leaderName, level, count, reps,
                true, "LEADER".equals(role)
        );
    }

    public List<Guild> getAllGuilds(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        List<Guild> list = new ArrayList<>();
        Guild userGuild = getUserGuild(userId);

        String query = "SELECT g.id, g.name, g.badge, g.slogan, g.leader_id, u.username, g.level " +
                "FROM guilds g " +
                "JOIN users u ON g.leader_id = u.id " +
                "ORDER BY g.level DESC, g.id ASC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            int gId = cursor.getInt(0);
            String gName = cursor.getString(1);
            String badge = cursor.getString(2);
            String slogan = cursor.getString(3);
            int leaderId = cursor.getInt(4);
            String leaderName = cursor.getString(5);
            int level = cursor.getInt(6);

            Cursor cCount = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", new String[]{String.valueOf(gId)});
            int count = cCount.moveToFirst() ? cCount.getInt(0) : 1;
            cCount.close();

            Cursor cReps = db.rawQuery("SELECT SUM(u.total_reps) FROM guild_members gm " +
                    "JOIN users u ON gm.user_id = u.id WHERE gm.guild_id = ?", new String[]{String.valueOf(gId)});
            int reps = cReps.moveToFirst() ? cReps.getInt(0) : 0;
            cReps.close();

            list.add(new Guild(
                    gId, gName, badge, slogan, leaderId, leaderName, level, count, reps,
                    userGuild != null && userGuild.getId() == gId,
                    userGuild != null && userGuild.getId() == gId && userGuild.isUserLeader()
            ));
        }
        cursor.close();
        return list;
    }

    public List<GuildMember> getGuildMembers(int guildId) {
        SQLiteDatabase db = getReadableDatabase();
        List<GuildMember> list = new ArrayList<>();
        String query = "SELECT u.id, u.username, u.avatar, u.level, gm.role, u.total_reps " +
                "FROM guild_members gm " +
                "JOIN users u ON gm.user_id = u.id " +
                "WHERE gm.guild_id = ? " +
                "ORDER BY CASE WHEN gm.role = 'LEADER' THEN 1 ELSE 2 END, u.total_reps DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(guildId)});
        while (cursor.moveToNext()) {
            list.add(new GuildMember(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getString(4),
                    cursor.getInt(5)
            ));
        }
        cursor.close();
        return list;
    }

    public Pair<Boolean, String> inviteToGuild(int inviterId, String targetUsername) {
        String trimmed = targetUsername.trim();
        if (trimmed.isEmpty()) return new Pair<>(false, "Vui lòng nhập tên người chơi!");

        SQLiteDatabase db = getWritableDatabase();
        Guild myGuild = getUserGuild(inviterId);
        if (myGuild == null) return new Pair<>(false, "Bạn phải ở trong một bang hội để gửi lời mời gia nhập!");

        Cursor uCursor = db.rawQuery("SELECT id, username FROM users WHERE LOWER(username) = LOWER(?)", new String[]{trimmed});
        if (!uCursor.moveToFirst()) {
            uCursor.close();
            return new Pair<>(false, "Không tìm thấy người chơi có tên '" + trimmed + "'!");
        }
        int targetId = uCursor.getInt(0);
        String canonicalName = uCursor.getString(1);
        uCursor.close();

        if (targetId == inviterId) {
            return new Pair<>(false, "Bạn không thể tự mời chính mình vào bang!");
        }

        Guild targetGuild = getUserGuild(targetId);
        if (targetGuild != null) {
            if (targetGuild.getId() == myGuild.getId()) {
                return new Pair<>(false, "'" + canonicalName + "' đã là thành viên trong bang của bạn rồi!");
            } else {
                return new Pair<>(false, "'" + canonicalName + "' hiện đã gia nhập bang '" + targetGuild.getName() + "' rồi!");
            }
        }

        Cursor checkInv = db.rawQuery("SELECT id FROM guild_invitations WHERE guild_id = ? AND invitee_id = ? AND status = 'PENDING'",
                new String[]{String.valueOf(myGuild.getId()), String.valueOf(targetId)});
        boolean alreadyInvited = checkInv.moveToFirst();
        checkInv.close();
        if (alreadyInvited) {
            return new Pair<>(false, "Đã gửi lời mời gia nhập bang tới '" + canonicalName + "' rồi, đang chờ phản hồi!");
        }

        if (myGuild.getMemberCount() >= 30) {
            return new Pair<>(false, "Bang hội đã đủ 30 thành viên, không thể mời thêm!");
        }

        ContentValues cv = new ContentValues();
        cv.put("guild_id", myGuild.getId());
        cv.put("inviter_id", inviterId);
        cv.put("invitee_id", targetId);
        cv.put("status", "PENDING");
        cv.put("created_at", LocalDate.now().toString());

        long id = db.insert("guild_invitations", null, cv);
        if (id != -1L) {
            return new Pair<>(true, "Đã gửi lời mời gia nhập bang '" + myGuild.getName() + "' tới '" + canonicalName + "'! 🛡️");
        } else {
            return new Pair<>(false, "Lỗi khi gửi lời mời gia nhập bang!");
        }
    }

    public List<GuildInvitationEntry> getIncomingGuildInvitations(int userId) {
        List<GuildInvitationEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT gi.id, gi.guild_id, g.name, g.badge, g.level, g.slogan, gi.inviter_id, u.username, u.avatar, gi.created_at " +
                "FROM guild_invitations gi " +
                "JOIN guilds g ON gi.guild_id = g.id " +
                "JOIN users u ON gi.inviter_id = u.id " +
                "WHERE gi.invitee_id = ? AND gi.status = 'PENDING' " +
                "ORDER BY gi.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            int invId = cursor.getInt(0);
            int gId = cursor.getInt(1);
            String gName = cursor.getString(2);
            String badge = cursor.getString(3);
            int lvl = cursor.getInt(4);
            String slogan = cursor.getString(5);
            int inviterId = cursor.getInt(6);
            String inviterName = cursor.getString(7);
            String inviterAvatar = cursor.getString(8);
            String createdAt = cursor.getString(9);

            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", new String[]{String.valueOf(gId)});
            int mCount = countCursor.moveToFirst() ? countCursor.getInt(0) : 1;
            countCursor.close();

            list.add(new GuildInvitationEntry(
                    invId, gId, gName, badge, lvl, mCount, slogan, inviterId, inviterName, inviterAvatar, createdAt != null ? createdAt : ""
            ));
        }
        cursor.close();
        return list;
    }

    public List<OutgoingGuildInvitation> getOutgoingGuildInvitations(int guildId) {
        List<OutgoingGuildInvitation> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT gi.id, gi.guild_id, gi.invitee_id, u.username, u.avatar, u.level, gi.created_at " +
                "FROM guild_invitations gi " +
                "JOIN users u ON gi.invitee_id = u.id " +
                "WHERE gi.guild_id = ? AND gi.status = 'PENDING' " +
                "ORDER BY gi.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(guildId)});
        while (cursor.moveToNext()) {
            int invId = cursor.getInt(0);
            int gId = cursor.getInt(1);
            int inviteeId = cursor.getInt(2);
            String inviteeName = cursor.getString(3);
            String avatar = cursor.getString(4);
            int lvl = cursor.getInt(5);
            String createdAt = cursor.getString(6);

            list.add(new OutgoingGuildInvitation(
                    invId, gId, inviteeId, inviteeName, avatar, lvl, createdAt != null ? createdAt : ""
            ));
        }
        cursor.close();
        return list;
    }

    public Pair<Boolean, String> acceptGuildInvitation(int invitationId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        Guild existing = getUserGuild(userId);
        if (existing != null) {
            return new Pair<>(false, "Bạn đã ở trong bang '" + existing.getName() + "' rồi! Phải rời bang trước khi gia nhập bang mới.");
        }

        String query = "SELECT gi.guild_id, g.name FROM guild_invitations gi " +
                "JOIN guilds g ON gi.guild_id = g.id " +
                "WHERE gi.id = ? AND gi.invitee_id = ? AND gi.status = 'PENDING'";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(invitationId), String.valueOf(userId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new Pair<>(false, "Không tìm thấy lời mời gia nhập bang này!");
        }
        int guildId = cursor.getInt(0);
        String guildName = cursor.getString(1);
        cursor.close();

        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM guild_members WHERE guild_id = ?", new String[]{String.valueOf(guildId)});
        int count = countCursor.moveToFirst() ? countCursor.getInt(0) : 1;
        countCursor.close();
        if (count >= 30) {
            return new Pair<>(false, "Bang hội đã đủ 30 thành viên, không thể gia nhập!");
        }

        ContentValues mCv = new ContentValues();
        mCv.put("guild_id", guildId);
        mCv.put("user_id", userId);
        mCv.put("role", "MEMBER");
        mCv.put("joined_at", LocalDate.now().toString());
        db.insert("guild_members", null, mCv);

        db.delete("guild_invitations", "invitee_id = ?", new String[]{String.valueOf(userId)});
        return new Pair<>(true, "⚔️ Chào mừng bạn gia nhập bang hội " + guildName + "!");
    }

    public Pair<Boolean, String> declineGuildInvitation(int invitationId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete("guild_invitations", "id = ? AND invitee_id = ?",
                new String[]{String.valueOf(invitationId), String.valueOf(userId)});
        if (rows > 0) {
            return new Pair<>(true, "Đã từ chối lời mời gia nhập bang.");
        } else {
            return new Pair<>(false, "Không tìm thấy lời mời cần từ chối.");
        }
    }

    public boolean cancelGuildInvitation(int invitationId, int inviterId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("guild_invitations", "id = ?", new String[]{String.valueOf(invitationId)}) > 0;
    }

    // =========================================================================
    // WEEKLY LOGIN CHECK-IN
    // =========================================================================

    private String getCurrentWeekKey() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.toString();
    }

    public Pair<List<DailyCheckInReward>, Boolean> getWeeklyCheckInRewards(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        String todayStr = LocalDate.now().toString();
        String currentWeekKey = getCurrentWeekKey();

        Cursor cursor = db.rawQuery("SELECT checkin_day, last_date, week_key FROM weekly_checkins WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        int checkinDay = 0;
        String lastDate = null;
        String weekKey = null;

        if (cursor.moveToFirst()) {
            checkinDay = cursor.getInt(0);
            lastDate = cursor.getString(1);
            weekKey = cursor.getString(2);
        }
        cursor.close();

        if (!currentWeekKey.equals(weekKey)) {
            checkinDay = 0;
            lastDate = null;
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("checkin_day", 0);
            cv.putNull("last_date");
            cv.put("week_key", currentWeekKey);
            db.insertWithOnConflict("weekly_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }

        boolean canClaimToday = (!todayStr.equals(lastDate) && checkinDay < 7);
        int[][] rewardsConfig = {
                {50, 0},   // Ngày 1: 50 Vàng
                {0, 2},    // Ngày 2: 2 Gem
                {100, 0},  // Ngày 3: 100 Vàng
                {0, 5},    // Ngày 4: 5 Gem
                {200, 0},  // Ngày 5: 200 Vàng
                {0, 10},   // Ngày 6: 10 Gem
                {500, 20}  // Ngày 7: 500 Vàng + 20 Gem
        };

        List<DailyCheckInReward> list = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            int gold = rewardsConfig[day - 1][0];
            int gems = rewardsConfig[day - 1][1];
            boolean isClaimed = day <= checkinDay;
            boolean isAvailableToday = canClaimToday && (day == checkinDay + 1);
            boolean isUpcoming = day > checkinDay + (canClaimToday ? 1 : 0);

            list.add(new DailyCheckInReward(day, gold, gems, isClaimed, isAvailableToday, isUpcoming));
        }

        return new Pair<>(list, canClaimToday);
    }

    public Pair<Boolean, String> claimWeeklyReward(int userId) {
        Pair<List<DailyCheckInReward>, Boolean> checkInfo = getWeeklyCheckInRewards(userId);
        List<DailyCheckInReward> rewards = checkInfo.getFirst();
        boolean canClaimToday = checkInfo.getSecond();

        if (!canClaimToday) {
            return new Pair<>(false, "Bạn đã điểm danh nhận quà hôm nay rồi! Hãy quay lại vào ngày mai nhé.");
        }

        DailyCheckInReward targetReward = null;
        for (DailyCheckInReward r : rewards) {
            if (r.isAvailableToday()) {
                targetReward = r;
                break;
            }
        }
        if (targetReward == null) {
            return new Pair<>(false, "Không có phần thưởng khả dụng.");
        }

        SQLiteDatabase db = getWritableDatabase();
        String todayStr = LocalDate.now().toString();
        String currentWeekKey = getCurrentWeekKey();

        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("checkin_day", targetReward.getDay());
        cv.put("last_date", todayStr);
        cv.put("week_key", currentWeekKey);
        db.insertWithOnConflict("weekly_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        db.execSQL("UPDATE users SET gold = gold + ?, gems = gems + ? WHERE id = ?",
                new Object[]{targetReward.getGold(), targetReward.getGems(), userId});

        StringBuilder msg = new StringBuilder();
        msg.append("🎉 Điểm danh Ngày ").append(targetReward.getDay()).append(" thành công! ");
        if (targetReward.getGold() > 0) msg.append("+").append(targetReward.getGold()).append(" 🪙 ");
        if (targetReward.getGems() > 0) msg.append("+").append(targetReward.getGems()).append(" ◆ ");

        return new Pair<>(true, msg.toString());
    }

    // =========================================================================
    // QUESTS SYSTEM
    // =========================================================================

    public List<QuestItem> getQuests(
            int userId,
            int todayReps,
            int weekReps,
            int totalReps,
            int streak,
            int stage,
            int level
    ) {
        SQLiteDatabase db = getReadableDatabase();
        String todayStr = LocalDate.now().toString();
        String currentWeekKey = getCurrentWeekKey();
        String currentMonthKey = todayStr.substring(0, 7);

        Set<String> claimedIds = new HashSet<>();
        String query = "SELECT quest_id FROM quest_claims WHERE user_id = ? AND period_key IN (?, ?, ?)";
        Cursor c = db.rawQuery(query, new String[]{String.valueOf(userId), todayStr, currentWeekKey, currentMonthKey});
        while (c.moveToNext()) {
            claimedIds.add(c.getString(0));
        }
        c.close();

        Cursor bc = db.rawQuery("SELECT COUNT(*) FROM boss_clears WHERE user_id = ?", new String[]{String.valueOf(userId)});
        int bossClearedCount = bc.moveToFirst() ? bc.getInt(0) : 0;
        bc.close();

        List<QuestItem> list = new ArrayList<>();

        // 1. DAILY
        list.add(new QuestItem(
                "daily_1", "Chiến Binh Chăm Chỉ", "“Ba mươi rep khởi động mỗi ngày giúp máu huyết lưu thông.”",
                todayReps, 30, 60, 30, 0, claimedIds.contains("daily_1"), "DAILY"
        ));
        list.add(new QuestItem(
                "daily_2", "Khởi Động Năng Lượng", "“Bắt đầu buổi tập đầu tiên trong ngày.”",
                todayReps > 0 ? 1 : 0, 1, 40, 20, 0, claimedIds.contains("daily_2"), "DAILY"
        ));
        list.add(new QuestItem(
                "daily_3", "Bất Khả Chiến Bại", "“Sáu mươi rep trong một ngày — chứng minh sự bền bỉ phi thường.”",
                todayReps, 60, 120, 60, 1, claimedIds.contains("daily_3"), "DAILY"
        ));

        // 2. WEEKLY
        list.add(new QuestItem(
                "weekly_1", "Bền Bỉ Trường Kỳ", "“Hai trăm rep trong cả tuần — không chùn bước trước thử thách.”",
                weekReps, 200, 350, 150, 3, claimedIds.contains("weekly_1"), "WEEKLY"
        ));
        list.add(new QuestItem(
                "weekly_2", "Chuỗi Ngày Thép", "“Duy trì chuỗi tập 3 ngày liên tục trong tuần.”",
                Math.min(streak, 3), 3, 250, 100, 2, claimedIds.contains("weekly_2"), "WEEKLY"
        ));
        list.add(new QuestItem(
                "weekly_3", "Đồ Tể Quái Vật", "“Vượt qua 3 ải chiến dịch trong tuần này.”",
                Math.min(stage, 3), 3, 400, 200, 5, claimedIds.contains("weekly_3"), "WEEKLY"
        ));

        // 3. MONTHLY
        list.add(new QuestItem(
                "monthly_1", "Huyền Thoại Thể Lực", "“Một nghìn rep tích lũy — cột mốc của những nhà vô địch.”",
                Math.min(totalReps, 1000), 1000, 1500, 600, 15, claimedIds.contains("monthly_1"), "MONTHLY"
        ));
        list.add(new QuestItem(
                "monthly_2", "Chinh Phục Đỉnh Cao", "“Đạt Cấp độ 5 trở lên để mở khóa tiềm năng vô hạn.”",
                Math.min(level, 5), 5, 1000, 500, 10, claimedIds.contains("monthly_2"), "MONTHLY"
        ));
        list.add(new QuestItem(
                "monthly_3", "Sát Thủ Trùm", "“Đánh bại ít nhất 1 Boss hùng mạnh trong tháng.”",
                Math.min(bossClearedCount, 1), 1, 2000, 800, 20, claimedIds.contains("monthly_3"), "MONTHLY"
        ));

        return list;
    }

    public Pair<Boolean, String> claimQuestReward(int userId, QuestItem quest) {
        String todayStr = LocalDate.now().toString();
        String periodKey;
        if ("DAILY".equals(quest.getCategory())) {
            periodKey = todayStr;
        } else if ("WEEKLY".equals(quest.getCategory())) {
            periodKey = getCurrentWeekKey();
        } else {
            periodKey = todayStr.substring(0, 7);
        }

        SQLiteDatabase db = getWritableDatabase();
        Cursor check = db.rawQuery(
                "SELECT id FROM quest_claims WHERE user_id = ? AND quest_id = ? AND period_key = ?",
                new String[]{String.valueOf(userId), quest.getId(), periodKey}
        );
        boolean already = check.moveToFirst();
        check.close();
        if (already) return new Pair<>(false, "Bạn đã nhận phần thưởng nhiệm vụ này rồi!");

        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("quest_id", quest.getId());
        cv.put("period_key", periodKey);
        cv.put("claimed_at", todayStr);

        long ins = db.insert("quest_claims", null, cv);
        if (ins != -1L) {
            Cursor uCursor = db.rawQuery("SELECT xp, level, gold, gems FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
            if (uCursor.moveToFirst()) {
                int xp = uCursor.getInt(0) + quest.getXp();
                int lvl = uCursor.getInt(1);
                int gold = uCursor.getInt(2) + quest.getGold();
                int gems = uCursor.getInt(3) + quest.getGems();

                int xpNeeded = 80 + lvl * 160;
                while (xp >= xpNeeded) {
                    xp -= xpNeeded;
                    lvl += 1;
                    xpNeeded = 80 + lvl * 160;
                }

                ContentValues uCv = new ContentValues();
                uCv.put("xp", xp);
                uCv.put("level", lvl);
                uCv.put("gold", gold);
                uCv.put("gems", gems);
                db.update("users", uCv, "id = ?", new String[]{String.valueOf(userId)});
            }
            uCursor.close();

            StringBuilder msg = new StringBuilder();
            msg.append("🎁 Đã nhận: +").append(quest.getXp()).append(" XP");
            if (quest.getGold() > 0) msg.append(", +").append(quest.getGold()).append(" 🪙");
            if (quest.getGems() > 0) msg.append(", +").append(quest.getGems()).append(" ◆");

            return new Pair<>(true, msg.toString());
        }
        return new Pair<>(false, "Không thể nhận thưởng nhiệm vụ!");
    }

    // =========================================================================
    // GUILD WORLD BOSS RAID (BOSS THẾ GIỚI BANG HỘI)
    // =========================================================================

    public GuildBossInfo getGuildBossInfo(int guildId, int currentUserId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, guild_id, boss_id, boss_name, boss_title, boss_avatar, max_hp, current_hp, status, reward_gold, reward_gems, reward_item_id " +
                "FROM guild_boss WHERE guild_id = ? ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(guildId)}
        );

        int bId = 0, gId = guildId, maxHp = 500000, curHp = 500000, rGold = 15000, rGems = 350;
        String bossId = "boss_nether_dragon", bName = "Hắc Long Viễn Cổ - Nidhogg", bTitle = "SIÊU TRÙM THẾ GIỚI BANG HỘI", bAvatar = "🐉", status = "ACTIVE", rItemId = "weapon_dragon_slayer";

        if (cursor.moveToFirst()) {
            bId = cursor.getInt(0);
            gId = cursor.getInt(1);
            bossId = cursor.getString(2);
            bName = cursor.getString(3);
            bTitle = cursor.getString(4);
            bAvatar = cursor.getString(5);
            maxHp = cursor.getInt(6);
            curHp = cursor.getInt(7);
            status = cursor.getString(8);
            rGold = cursor.getInt(9);
            rGems = cursor.getInt(10);
            rItemId = cursor.getString(11);
            cursor.close();
        } else {
            cursor.close();
            // Tự động tạo boss mặc định nếu chưa có
            ContentValues cv = new ContentValues();
            cv.put("guild_id", guildId);
            cv.put("boss_id", bossId);
            cv.put("boss_name", bName);
            cv.put("boss_title", bTitle);
            cv.put("boss_avatar", bAvatar);
            cv.put("max_hp", maxHp);
            cv.put("current_hp", curHp);
            cv.put("status", status);
            cv.put("reward_gold", rGold);
            cv.put("reward_gems", rGems);
            cv.put("reward_item_id", rItemId);
            cv.put("created_at", LocalDate.now().toString());
            bId = (int) db.insert("guild_boss", null, cv);
        }

        // Tên của vật phẩm phần thưởng
        String rItemName = "Đại Đao Trảm Long";
        Cursor itCursor = db.rawQuery("SELECT name FROM items WHERE id = ?", new String[]{rItemId});
        if (itCursor.moveToFirst()) {
            rItemName = itCursor.getString(0);
        }
        itCursor.close();

        GuildBoss boss = new GuildBoss(bId, gId, bossId, bName, bTitle, bAvatar, maxHp, curHp, status, rGold, rGems, rItemId, rItemName);

        // Danh sách đóng góp sát thương
        List<GuildBossContribution> contributors = new ArrayList<>();
        Cursor cCursor = db.rawQuery(
                "SELECT gbd.user_id, u.username, u.avatar, u.level, gbd.damage, gbd.reps_contributed, gbd.has_claimed_defeat_reward " +
                "FROM guild_boss_damage gbd " +
                "JOIN users u ON gbd.user_id = u.id " +
                "WHERE gbd.boss_db_id = ? " +
                "ORDER BY gbd.damage DESC",
                new String[]{String.valueOf(bId)}
        );

        int totalDmg = 0;
        List<Object[]> rawList = new ArrayList<>();
        while (cCursor.moveToNext()) {
            int uId = cCursor.getInt(0);
            String uName = cCursor.getString(1);
            String avt = cCursor.getString(2);
            int lvl = cCursor.getInt(3);
            int dmg = cCursor.getInt(4);
            int reps = cCursor.getInt(5);
            boolean claimed = cCursor.getInt(6) == 1;
            totalDmg += dmg;
            rawList.add(new Object[]{uId, uName, avt, lvl, dmg, reps, claimed});
        }
        cCursor.close();

        int myDmg = 0, myReps = 0, myRank = 0;
        double myPct = 0.0;
        boolean myClaimed = false;

        for (int i = 0; i < rawList.size(); i++) {
            Object[] row = rawList.get(i);
            int uId = (int) row[0];
            String uName = (String) row[1];
            String avt = (String) row[2];
            int lvl = (int) row[3];
            int dmg = (int) row[4];
            int reps = (int) row[5];
            boolean claimed = (boolean) row[6];

            double pct = totalDmg > 0 ? Math.round((double) dmg / totalDmg * 1000.0) / 10.0 : 0.0;
            int rank = i + 1;
            contributors.add(new GuildBossContribution(uId, uName, avt, lvl, dmg, reps, pct, rank, claimed));

            if (uId == currentUserId) {
                myDmg = dmg;
                myReps = reps;
                myRank = rank;
                myPct = pct;
                myClaimed = claimed;
            }
        }

        return new GuildBossInfo(boss, contributors, myDmg, myReps, myPct, myRank, myClaimed);
    }

    public Pair<Boolean, String> attackGuildBoss(int guildId, int userId, int damage, int reps, String exercise) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, boss_name, current_hp FROM guild_boss WHERE guild_id = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(guildId)}
        );
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new Pair<>(false, "Boss hiện tại đã bị tiêu diệt hoặc chưa được triệu hồi!");
        }

        int bossId = cursor.getInt(0);
        String bossName = cursor.getString(1);
        int curHp = cursor.getInt(2);
        cursor.close();

        int newHp = Math.max(0, curHp - damage);
        boolean isDefeated = (newHp == 0);

        ContentValues bCv = new ContentValues();
        bCv.put("current_hp", newHp);
        if (isDefeated) {
            bCv.put("status", "DEFEATED");
            bCv.put("defeated_at", LocalDate.now().toString());
        }
        db.update("guild_boss", bCv, "id = ?", new String[]{String.valueOf(bossId)});

        // Cập nhật sát thương của thành viên
        Cursor dCursor = db.rawQuery(
                "SELECT id, damage, reps_contributed FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?",
                new String[]{String.valueOf(bossId), String.valueOf(userId)}
        );
        if (dCursor.moveToFirst()) {
            int dId = dCursor.getInt(0);
            int prevDmg = dCursor.getInt(1);
            int prevReps = dCursor.getInt(2);
            dCursor.close();

            ContentValues dCv = new ContentValues();
            dCv.put("damage", prevDmg + damage);
            dCv.put("reps_contributed", prevReps + reps);
            dCv.put("last_attack_at", LocalDate.now().toString());
            db.update("guild_boss_damage", dCv, "id = ?", new String[]{String.valueOf(dId)});
        } else {
            dCursor.close();
            ContentValues dCv = new ContentValues();
            dCv.put("guild_id", guildId);
            dCv.put("boss_db_id", bossId);
            dCv.put("user_id", userId);
            dCv.put("damage", damage);
            dCv.put("reps_contributed", reps);
            dCv.put("last_attack_at", LocalDate.now().toString());
            dCv.put("has_claimed_defeat_reward", 0);
            db.insert("guild_boss_damage", null, dCv);
        }

        // Thưởng nỗ lực mỗi lượt tập cho người chơi: vàng = reps * 2, xp = reps * 5
        int effortGold = reps * 2;
        int effortXp = reps * 5;
        Cursor uCursor = db.rawQuery("SELECT gold, xp, total_reps, level FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        if (uCursor.moveToFirst()) {
            int uGold = uCursor.getInt(0) + effortGold;
            int uXp = uCursor.getInt(1) + effortXp;
            int uReps = uCursor.getInt(2) + reps;
            int uLvl = uCursor.getInt(3);
            uCursor.close();

            int xpNeeded = 80 + uLvl * 160;
            while (uXp >= xpNeeded) {
                uXp -= xpNeeded;
                uLvl += 1;
                xpNeeded = 80 + uLvl * 160;
            }

            ContentValues uCv = new ContentValues();
            uCv.put("gold", uGold);
            uCv.put("xp", uXp);
            uCv.put("level", uLvl);
            uCv.put("total_reps", uReps);
            uCv.put("last_workout_date", LocalDate.now().toString());
            db.update("users", uCv, "id = ?", new String[]{String.valueOf(userId)});
        } else {
            uCursor.close();
        }

        // Ghi nhật ký vào workouts
        ContentValues wCv = new ContentValues();
        wCv.put("user_id", userId);
        wCv.put("exercise", exercise != null ? exercise : "SQUAT");
        wCv.put("reps", reps);
        wCv.put("hold_seconds", "PLANK".equalsIgnoreCase(exercise) ? reps * 2 : 0);
        wCv.put("score", reps);
        wCv.put("xp_earned", effortXp);
        wCv.put("gold_earned", effortGold);
        wCv.put("created_at", LocalDate.now().toString());
        db.insert("workouts", null, wCv);

        String msg = isDefeated
                ? "🎉 Tuyệt đỉnh! Bạn và bang hội đã kết liễu " + bossName + "!"
                : "⚔️ Đã gây " + damage + " sát thương lên " + bossName + "! (+" + effortGold + " 🪙, +" + effortXp + " XP)";
        return new Pair<>(true, msg);
    }

    public Pair<Boolean, String> claimGuildBossReward(int guildId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, boss_name, reward_gold, reward_gems, reward_item_id FROM guild_boss " +
                "WHERE guild_id = ? AND status = 'DEFEATED' ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(guildId)}
        );
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new Pair<>(false, "Boss chưa bị tiêu diệt, hãy cùng bang hội tiếp tục chiến đấu!");
        }

        int bossId = cursor.getInt(0);
        String bossName = cursor.getString(1);
        int rGold = cursor.getInt(2);
        int rGems = cursor.getInt(3);
        String rItemId = cursor.getString(4);
        cursor.close();

        Cursor dCursor = db.rawQuery(
                "SELECT id, has_claimed_defeat_reward FROM guild_boss_damage WHERE boss_db_id = ? AND user_id = ?",
                new String[]{String.valueOf(bossId), String.valueOf(userId)}
        );
        if (!dCursor.moveToFirst()) {
            dCursor.close();
            return new Pair<>(false, "Bạn chưa góp sát thương cho chiến dịch tiêu diệt Boss này!");
        }
        int dId = dCursor.getInt(0);
        int hasClaimed = dCursor.getInt(1);
        dCursor.close();

        if (hasClaimed == 1) {
            return new Pair<>(false, "Bạn đã nhận phần thưởng cho lần diệt Boss này rồi!");
        }

        // Cập nhật ví của người chơi
        Cursor uCursor = db.rawQuery("SELECT gold, gems FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        if (uCursor.moveToFirst()) {
            int newGold = uCursor.getInt(0) + rGold;
            int newGems = uCursor.getInt(1) + rGems;
            uCursor.close();

            ContentValues uCv = new ContentValues();
            uCv.put("gold", newGold);
            uCv.put("gems", newGems);
            db.update("users", uCv, "id = ?", new String[]{String.valueOf(userId)});
        } else {
            uCursor.close();
        }

        // Tặng vật phẩm huyền thoại vào kho đồ
        if (rItemId != null && !rItemId.isEmpty()) {
            ContentValues invCv = new ContentValues();
            invCv.put("user_id", userId);
            invCv.put("item_id", rItemId);
            invCv.put("is_equipped", 0);
            invCv.put("acquired_at", LocalDate.now().toString());
            db.insert("user_inventory", null, invCv);
        }

        // Đánh dấu đã nhận thưởng
        ContentValues dCv = new ContentValues();
        dCv.put("has_claimed_defeat_reward", 1);
        db.update("guild_boss_damage", dCv, "id = ?", new String[]{String.valueOf(dId)});

        String itName = "Vật Phẩm Huyền Thoại";
        Cursor itCursor = db.rawQuery("SELECT name FROM items WHERE id = ?", new String[]{rItemId});
        if (itCursor.moveToFirst()) {
            itName = itCursor.getString(0);
        }
        itCursor.close();

        return new Pair<>(true, "🎉 Chúc mừng! Bạn nhận được " + rGold + " Vàng, " + rGems + " Kim Cương và " + itName + "!");
    }

    public Pair<Boolean, String> summonGuildBoss(int guildId) {
        SQLiteDatabase db = getWritableDatabase();
        String[][] templates = {
                {"boss_nether_dragon", "Hắc Long Viễn Cổ - Nidhogg", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐉", "500000", "15000", "350", "weapon_dragon_slayer"},
                {"boss_inferno_titan", "Cự Nhân Hỏa Ngục - Surtr", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🌋", "650000", "18000", "400", "armor_dragon_scale"},
                {"boss_void_behemoth", "Thần Thú Hư Không - Leviathan", "SIÊU TRÙM THẾ GIỚI BANG HỘI", "🐲", "800000", "22000", "500", "amulet_boss_heart"}
        };
        String[] tpl = templates[new Random().nextInt(templates.length)];

        ContentValues cv = new ContentValues();
        cv.put("guild_id", guildId);
        cv.put("boss_id", tpl[0]);
        cv.put("boss_name", tpl[1]);
        cv.put("boss_title", tpl[2]);
        cv.put("boss_avatar", tpl[3]);
        cv.put("max_hp", Integer.parseInt(tpl[4]));
        cv.put("current_hp", Integer.parseInt(tpl[4]));
        cv.put("status", "ACTIVE");
        cv.put("reward_gold", Integer.parseInt(tpl[5]));
        cv.put("reward_gems", Integer.parseInt(tpl[6]));
        cv.put("reward_item_id", tpl[7]);
        cv.put("created_at", LocalDate.now().toString());

        long ins = db.insert("guild_boss", null, cv);
        if (ins != -1L) {
            return new Pair<>(true, "🔥 Tiếng gầm thét rung chuyển! " + tpl[1] + " đã giáng lâm khiêu chiến bang hội!");
        }
        return new Pair<>(false, "Không thể triệu hồi Boss!");
    }
}

