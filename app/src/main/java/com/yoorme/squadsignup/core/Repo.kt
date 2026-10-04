package com.yoorme.squadsignup.core

import retrofit2.HttpException

class AuthRequiredException : Exception("登录已过期")

// 统一仓库层：负责按当前战队切换 Retrofit 实例、错误包装
class Repo(private val store: SessionStore) {

    private var cachedServer: SquadServer? = null
    private var cachedApi: SquadApi? = null

    private suspend fun api(): SquadApi {
        val server = store.currentServer()
        if (server != cachedServer || cachedApi == null) {
            cachedApi = ApiClient.build(server) { store.currentToken() }
            cachedServer = server
        }
        return cachedApi!!
    }

    private suspend fun <T> call(
        expiredOn401: Boolean = true,
        block: suspend SquadApi.() -> ApiEnvelope<T>,
    ): T {
        val env = try {
            block(api())
        } catch (e: HttpException) {
            if (e.code() == 401 && expiredOn401) {
                // 会话失效：直接清除本地会话（token 变空 → UI 自动回到登录页）
                store.clear()
                throw AuthRequiredException()
            }
            val serverMessage = runCatching {
                e.response()?.errorBody()?.string()?.let { body ->
                    org.json.JSONObject(body).optString("error").takeIf { it.isNotBlank() }
                }
            }.getOrNull()
            throw ApiException(serverMessage ?: "请求失败（${e.code()}）", e.code())
        }
        if (!env.ok) throw ApiException(env.error ?: "请求失败", 400)
        return env.data ?: throw ApiException("响应数据为空", 500)
    }

    // ---- 认证 ----
    suspend fun login(username: String, password: String): LoginResponse =
        call(expiredOn401 = false) { appLogin(LoginRequest(username, password)) }

    suspend fun register(invitationCode: String, nickname: String, password: String): RegisterResponse =
        call { register(RegisterRequest(invitationCode, nickname, password)) }

    // ---- 赛事 ----
    suspend fun events(status: String): List<EventSummary> = call { events(status) }
    suspend fun eventDetail(id: String): EventDetail = call { eventDetail(id) }
    suspend fun createEvent(body: EventCreateRequest): String =
        call { createEvent(body) }.id

    suspend fun manageEvent(body: EventManageRequest) {
        call { manageEvent(body) }
    }

    suspend fun deleteEvent(id: String) {
        call { deleteEvent(id) }
    }

    suspend fun assign(eventId: String, moves: List<AssignMove>) {
        call { assign(AssignRequest(eventId = eventId, moves = moves)) }
    }

    suspend fun registerEvent(eventId: String, squadId: String?, asSubstitute: Boolean): RegisterEventResponse =
        call { registerEvent(RegisterEventRequest(eventId, squadId, asSubstitute)) }

    suspend fun cancelRegistration(registrationId: String) {
        call { cancelRegistration(registrationId) }
    }

    // ---- 公告 ----
    suspend fun announcements(status: String): List<AnnouncementSummary> = call { announcements(status) }
    suspend fun announcementDetail(id: String): AnnouncementDetail =
        call { announcementDetail(status = null, mode = "detail", id = id) }

    suspend fun createAnnouncement(title: String, contentMarkdown: String) {
        call { saveAnnouncement(AnnouncementSaveRequest(title = title, contentMarkdown = contentMarkdown, images = emptyList())) }
    }

    // 编辑必须走 PATCH（POST 是新建接口，传 id 会被服务端忽略导致重复创建）
    suspend fun updateAnnouncement(id: String, title: String, contentMarkdown: String) {
        call { patchAnnouncement(AnnouncementSaveRequest(id = id, title = title, contentMarkdown = contentMarkdown)) }
    }

    suspend fun archiveAnnouncement(id: String, archived: Boolean) {
        call { patchAnnouncement(AnnouncementSaveRequest(id = id, isArchived = archived)) }
    }

    suspend fun deleteAnnouncement(id: String) {
        call { deleteAnnouncement(id) }
    }

    suspend fun comments(announcementId: String): List<Comment> = call { comments(announcementId) }
    suspend fun postComment(announcementId: String, content: String): Comment =
        call { postComment(CommentRequest(announcementId, content)) }

    suspend fun deleteComment(id: String) {
        call { deleteComment(id) }
    }

    // ---- 成员 ----
    suspend fun members(): List<MemberProfile> = call { members() }
    suspend fun member(id: String): MemberProfile = call { member(id) }

    // ---- 我的 ----
    suspend fun me(): MemberProfile = call { me() }

    suspend fun patchMe(body: MePatchRequest) {
        call { patchMe(body) }
    }

    suspend fun notificationSettings(): NotificationSettings =
        try {
            call { notificationSettings() }
        } catch (e: AuthRequiredException) {
            // 会话失效必须向上传递（调用方据此回登录页），不能伪装成默认设置
            throw e
        } catch (e: Exception) {
            NotificationSettings() // 网络失败等场景按默认值处理
        }

    suspend fun patchNotificationSettings(body: NotificationSettingsPatch): NotificationSettings =
        call { patchNotificationSettings(body) }

    suspend fun registerDevice(body: DeviceRegisterRequest) {
        call { registerDevice(body) }
    }

    suspend fun unregisterDevice(registrationId: String) {
        call { unregisterDevice(registrationId) }
    }

    // ---- 标签 / 选项 ----
    suspend fun options(): OptionsResponse = call { options() }
    suspend fun tags(): TagsResponse = call { tags() }
    suspend fun adminTags(type: String): List<AdminTag> = call { adminTags(type) }
    suspend fun mutateTag(body: TagMutation) {
        call { mutateTag(body) }
    }

    // ---- 管理员 ----
    suspend fun adminUsers(): List<AdminUser> = call { adminUsers() }

    suspend fun patchUser(id: String, role: String?, disabled: Boolean?) {
        call { patchUser(UserPatchRequest(id, role, disabled)) }
    }

    suspend fun resetPassword(id: String, password: String) {
        call { resetPassword(ResetPasswordRequest(id, password)) }
    }

    suspend fun deleteUser(id: String) {
        call { deleteUser(id) }
    }

    suspend fun invitations(): List<InvitationCode> = call { invitations() }

    suspend fun createInvitation(maxUses: Int): InvitationCode =
        call { createInvitation(InvitationCreateRequest(maxUses)) }

    suspend fun deleteInvitation(id: String) {
        call { deleteInvitation(id) }
    }
}
