package com.example.vigil.network

import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.Rarity
import com.example.vigil.model.WorkoutReward
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Client kết nối REST API Backend MySQL của Vigil.
 * Tự động đồng bộ với cơ sở dữ liệu MySQL trên máy chủ.
 */
object ApiClient {
    // 10.0.2.2 trỏ về localhost của máy tính chạy Android Emulator
    var baseUrl: String = "http://10.0.2.2:3000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun isServerOnline(): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/health").build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun buyItem(username: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("itemId", itemId)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/shop/buy").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun equipItem(username: String, invId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("invId", invId)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/inventory/equip").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unequipItem(username: String, invId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("invId", invId)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/inventory/unequip").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun finishWorkout(
        username: String,
        exercise: String,
        reps: Int,
        holdSeconds: Int,
        score: Int,
        isBoss: Boolean,
        isFreeTraining: Boolean = false
    ): WorkoutReward? = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("exercise", exercise)
                addProperty("reps", reps)
                addProperty("holdSeconds", holdSeconds)
                addProperty("score", score)
                addProperty("isBoss", isBoss)
                addProperty("isFreeTraining", isFreeTraining)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/workout/finish").post(body).build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val json = res.body?.string() ?: return@withContext null
                val obj = gson.fromJson(json, JsonObject::class.java)

                var droppedItem: Item? = null
                if (obj.has("droppedItem") && !obj.get("droppedItem").isJsonNull) {
                    val dObj = obj.getAsJsonObject("droppedItem")
                    droppedItem = Item(
                        id = dObj.get("id").asString,
                        name = dObj.get("name").asString,
                        slot = ItemSlot.valueOf(dObj.get("slot").asString),
                        rarity = Rarity.valueOf(dObj.get("rarity").asString),
                        priceGold = dObj.get("price_gold").asInt,
                        priceGems = dObj.get("price_gems").asInt,
                        bonusStr = dObj.get("bonus_str").asInt,
                        bonusEnd = dObj.get("bonus_end").asInt,
                        bonusPre = dObj.get("bonus_pre").asInt,
                        bonusLuck = dObj.get("bonus_luck").asInt,
                        description = dObj.get("description").asString,
                        icon = dObj.get("icon").asString
                    )
                }

                WorkoutReward(
                    score = obj.get("score").asInt,
                    xpEarned = obj.get("xpEarned").asInt,
                    goldEarned = obj.get("goldEarned").asInt,
                    newLevel = obj.get("newLevel").asInt,
                    newGold = obj.get("newGold").asInt,
                    newXp = obj.get("newXp").asInt,
                    newStreak = obj.get("newStreak").asInt,
                    droppedItem = droppedItem
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(username: String, password: String, avatar: String = "🧑‍🎤"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("password", password)
                addProperty("avatar", avatar)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/auth/register").post(body).build()
            client.newCall(req).execute().use { res ->
                val json = res.body?.string() ?: ""
                val obj = try { gson.fromJson(json, JsonObject::class.java) } catch (e: Exception) { null }
                if (res.isSuccessful) {
                    Pair(true, obj?.get("message")?.asString ?: "Đăng ký thành công!")
                } else {
                    Pair(false, obj?.get("error")?.asString ?: "Đăng ký thất bại!")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Không thể kết nối máy chủ: ${e.message}")
        }
    }

    suspend fun login(username: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("password", password)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/auth/login").post(body).build()
            client.newCall(req).execute().use { res ->
                val json = res.body?.string() ?: ""
                val obj = try { gson.fromJson(json, JsonObject::class.java) } catch (e: Exception) { null }
                if (res.isSuccessful) {
                    Pair(true, obj?.get("message")?.asString ?: "Đăng nhập thành công!")
                } else {
                    Pair(false, obj?.get("error")?.asString ?: "Đăng nhập thất bại!")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Không thể kết nối máy chủ: ${e.message}")
        }
    }

    suspend fun addFriend(username: String, friendUsername: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("friendUsername", friendUsername)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/friends/add").post(body).build()
            client.newCall(req).execute().use { res ->
                val json = res.body?.string() ?: ""
                val obj = try { gson.fromJson(json, JsonObject::class.java) } catch (e: Exception) { null }
                if (res.isSuccessful) {
                    Pair(true, obj?.get("message")?.asString ?: "Đã kết bạn thành công!")
                } else {
                    Pair(false, obj?.get("error")?.asString ?: "Không thể kết bạn!")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun removeFriend(username: String, friendUsername: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("username", username)
                addProperty("friendUsername", friendUsername)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url("$baseUrl/api/friends/remove").delete(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
