package com.yoorme.squadsignup.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "squad_session")

// 内置战队站点（同一套模板部署在不同端口）；后续新增站点在此追加
data class SquadServer(val id: String, val label: String, val baseUrl: String, val prefixHint: String)

object Servers {
    val ALL = listOf(
        SquadServer("mmr", "MMR 战队", "http://121.196.195.27:3000", "MMR丨"),
        SquadServer("yfd", "YFD 战队", "http://121.196.195.27:3001", "YFD丨"),
    )
    fun byId(id: String?): SquadServer = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

@kotlinx.serialization.Serializable
data class SessionUser(
    val id: String,
    val username: String,
    val nickname: String,
    val role: String, // ADMIN | MEMBER
) {
    val isAdmin: Boolean get() = role == "ADMIN"
}

// 会话与本地状态存储（DataStore）
class SessionStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_USER = stringPreferencesKey("user")
    private val KEY_SERVER = stringPreferencesKey("server")
    private val KEY_LAST_EVENT_SEEN = stringPreferencesKey("last_event_seen")
    private val KEY_LAST_ANN_SEEN = stringPreferencesKey("last_ann_seen")
    private val KEY_NOTIFIED_KEYS = stringSetPreferencesKey("notified_keys")

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val user: Flow<SessionUser?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER]?.let { runCatching { json.decodeFromString<SessionUser>(it) }.getOrNull() }
    }
    val serverId: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER] }

    suspend fun currentToken(): String? = token.first()
    suspend fun currentUser(): SessionUser? = user.first()
    suspend fun currentServer(): SquadServer = Servers.byId(serverId.first())

    suspend fun saveLogin(token: String, user: LoginUser) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER] = json.encodeToString(
                SessionUser(user.id, user.username, user.nickname, user.role)
            )
        }
    }

    suspend fun updateNickname(nickname: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER]?.let { raw ->
                runCatching {
                    val u = json.decodeFromString<SessionUser>(raw)
                    prefs[KEY_USER] = json.encodeToString(u.copy(nickname = nickname))
                }
            }
        }
    }

    // 同步服务端角色（管理员提权/降权后，进入「我的」页即刷新本地会话）
    suspend fun updateRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER]?.let { raw ->
                runCatching {
                    val u = json.decodeFromString<SessionUser>(raw)
                    prefs[KEY_USER] = json.encodeToString(u.copy(role = role))
                }
            }
        }
    }

    suspend fun setServer(id: String) {
        context.dataStore.edit { it[KEY_SERVER] = id }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER)
            prefs.remove(KEY_NOTIFIED_KEYS)
            prefs.remove(KEY_LAST_EVENT_SEEN)
            prefs.remove(KEY_LAST_ANN_SEEN)
        }
    }

    // ---- 轮询通知的本地状态 ----

    suspend fun lastEventSeen(): String? = context.dataStore.data.map { it[KEY_LAST_EVENT_SEEN] }.first()
    suspend fun markEventSeen(createdAt: String) {
        context.dataStore.edit { it[KEY_LAST_EVENT_SEEN] = createdAt }
    }

    suspend fun lastAnnouncementSeen(): String? =
        context.dataStore.data.map { it[KEY_LAST_ANN_SEEN] }.first()

    suspend fun markAnnouncementSeen(createdAt: String) {
        context.dataStore.edit { it[KEY_LAST_ANN_SEEN] = createdAt }
    }

    suspend fun notifiedKeys(): Set<String> =
        context.dataStore.data.map { it[KEY_NOTIFIED_KEYS] ?: emptySet() }.first()

    suspend fun addNotifiedKey(key: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[KEY_NOTIFIED_KEYS] ?: emptySet()
            // 控制体积：只保留最近 200 条
            prefs[KEY_NOTIFIED_KEYS] = (set + key).toList().takeLast(200).toSet()
        }
    }
}
