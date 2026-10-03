package com.yoorme.squadsignup.core

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Retrofit API 定义（与服务端 src/types/index.ts 对应）
interface SquadApi {

    // ---- 认证 ----
    @POST("api/auth/app-login")
    suspend fun appLogin(@Body body: LoginRequest): ApiEnvelope<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<RegisterResponse>

    // ---- 赛事 ----
    @GET("api/events")
    suspend fun events(
        @Query("status") status: String = "UPCOMING",
        @Query("id") id: String? = null,
    ): ApiEnvelope<List<EventSummary>>

    @GET("api/events")
    suspend fun eventDetail(@Query("id") id: String): ApiEnvelope<EventDetail>

    @POST("api/events")
    suspend fun createEvent(@Body body: EventCreateRequest): ApiEnvelope<EventIdResponse>

    @PATCH("api/events/manage")
    suspend fun manageEvent(@Body body: EventManageRequest): ApiEnvelope<SimpleOk>

    @DELETE("api/events/manage")
    suspend fun deleteEvent(@Query("id") id: String): ApiEnvelope<SimpleOk>

    @PATCH("api/events/assign")
    suspend fun assign(@Body body: AssignRequest): ApiEnvelope<SimpleOk>

    @POST("api/events/register")
    suspend fun registerEvent(@Body body: RegisterEventRequest): ApiEnvelope<RegisterEventResponse>

    @DELETE("api/events/register")
    suspend fun cancelRegistration(@Query("registrationId") registrationId: String): ApiEnvelope<SimpleOk>

    // ---- 公告 ----
    @GET("api/announcements")
    suspend fun announcements(@Query("status") status: String = "normal"): ApiEnvelope<List<AnnouncementSummary>>

    @GET("api/announcements")
    suspend fun announcementDetail(
        @Query("status") status: String?,
        @Query("mode") mode: String,
        @Query("id") id: String,
    ): ApiEnvelope<AnnouncementDetail>

    @POST("api/announcements")
    suspend fun saveAnnouncement(@Body body: AnnouncementSaveRequest): ApiEnvelope<AnnouncementDetail>

    @PATCH("api/announcements")
    suspend fun patchAnnouncement(@Body body: AnnouncementSaveRequest): ApiEnvelope<SimpleOk>

    @DELETE("api/announcements")
    suspend fun deleteAnnouncement(@Query("id") id: String): ApiEnvelope<SimpleOk>

    @GET("api/announcements/comments")
    suspend fun comments(@Query("announcementId") announcementId: String): ApiEnvelope<List<Comment>>

    @POST("api/announcements/comments")
    suspend fun postComment(@Body body: CommentRequest): ApiEnvelope<Comment>

    @DELETE("api/announcements/comments")
    suspend fun deleteComment(@Query("id") id: String): ApiEnvelope<SimpleOk>

    // ---- 成员 ----
    @GET("api/members")
    suspend fun members(): ApiEnvelope<List<MemberProfile>>

    @GET("api/members/{id}")
    suspend fun member(@Path("id") id: String): ApiEnvelope<MemberProfile>

    // ---- 我的 ----
    @GET("api/me")
    suspend fun me(): ApiEnvelope<MemberProfile>

    @PATCH("api/me")
    suspend fun patchMe(@Body body: MePatchRequest): ApiEnvelope<SimpleOk>

    @GET("api/me/notifications")
    suspend fun notificationSettings(): ApiEnvelope<NotificationSettings>

    @PATCH("api/me/notifications")
    suspend fun patchNotificationSettings(
        @Body body: NotificationSettingsPatch,
    ): ApiEnvelope<NotificationSettings>

    @POST("api/me/devices")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest): ApiEnvelope<SimpleOk>

    @DELETE("api/me/devices")
    suspend fun unregisterDevice(@Query("registrationId") registrationId: String): ApiEnvelope<SimpleOk>

    // ---- 标签 / 选项 ----
    @GET("api/options")
    suspend fun options(@Query("only") only: String = "all"): ApiEnvelope<OptionsResponse>

    @GET("api/tags")
    suspend fun tags(): ApiEnvelope<TagsResponse>

    @GET("api/admin/tags")
    suspend fun adminTags(@Query("type") type: String): ApiEnvelope<List<AdminTag>>

    @POST("api/admin/tags")
    suspend fun mutateTag(@Body body: TagMutation): ApiEnvelope<SimpleOk>

    // ---- 管理员：用户 ----
    @GET("api/admin/users")
    suspend fun adminUsers(): ApiEnvelope<List<AdminUser>>

    @PATCH("api/admin/users")
    suspend fun patchUser(@Body body: UserPatchRequest): ApiEnvelope<SimpleOk>

    @POST("api/admin/users")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ApiEnvelope<SimpleOk>

    @DELETE("api/admin/users")
    suspend fun deleteUser(@Query("id") id: String): ApiEnvelope<SimpleOk>

    // ---- 管理员：邀请码 ----
    @GET("api/admin/invitations")
    suspend fun invitations(): ApiEnvelope<List<InvitationCode>>

    @POST("api/admin/invitations")
    suspend fun createInvitation(@Body body: InvitationCreateRequest): ApiEnvelope<InvitationCode>

    @DELETE("api/admin/invitations")
    suspend fun deleteInvitation(@Query("id") id: String): ApiEnvelope<SimpleOk>
}

@kotlinx.serialization.Serializable
data class EventIdResponse(val id: String)

// ============ 网络客户端构建 ============

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false
    }

    fun build(server: SquadServer, tokenProvider: suspend () -> String?): SquadApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = runCatching { kotlinx.coroutines.runBlocking { tokenProvider() } }.getOrNull()
                val request: Request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(server.baseUrl + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SquadApi::class.java)
    }
}
