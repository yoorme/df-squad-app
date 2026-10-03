package com.yoorme.squadsignup.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ 通用包装 ============

// 服务端统一返回 { ok, data?, error? }
@Serializable
data class ApiEnvelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null,
)

class ApiException(override val message: String, val code: Int) : Exception(message)

// ============ 认证 ============

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginUser(
    val id: String,
    val username: String,
    val nickname: String,
    val role: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresIn: Long,
    val user: LoginUser,
)

@Serializable
data class RegisterRequest(
    val invitationCode: String,
    val nickname: String,
    val password: String,
)

@Serializable
data class RegisterResponse(val id: String, val username: String)

// ============ 标签/选项 ============

@Serializable
data class EventNature(val id: String, val name: String, val disabled: Boolean = false)

@Serializable
data class EventName(val id: String, val name: String, val disabled: Boolean = false)

@Serializable
data class EventMap(val id: String, val name: String, val disabled: Boolean = false)

@Serializable
data class SquadNature(val id: String, val name: String, val disabled: Boolean = false)

@Serializable
data class Ability(
    val id: String,
    val name: String,
    val category: String, // INFANTRY | VEHICLE
    val disabled: Boolean = false,
)

@Serializable
data class Duty(val id: String, val name: String, val disabled: Boolean = false)

@Serializable
data class Operator(
    val id: String,
    val name: String,
    val faction: String? = null,
    val disabled: Boolean = false,
)

@Serializable
data class OptionsResponse(
    val abilities: List<Ability> = emptyList(),
    val duties: List<Duty> = emptyList(),
    val operators: List<Operator> = emptyList(),
)

@Serializable
data class TagsResponse(
    val natures: List<EventNature> = emptyList(),
    val names: List<EventName> = emptyList(),
    val squadNatures: List<SquadNature> = emptyList(),
    val maps: List<EventMap> = emptyList(),
)

// admin/tags 列表项（含禁用状态与使用数）
@Serializable
data class AdminTag(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val disabled: Boolean = false,
    val usedCount: Int = 0,
    val category: String? = null,
    val faction: String? = null,
)

@Serializable
data class TagMutation(
    val type: String,
    val op: String,
    val id: String? = null,
    val name: String? = null,
    val disabled: Boolean? = null,
    val orderedIds: List<String>? = null,
    val category: String? = null,
    val faction: String? = null,
)

// ============ 赛事 ============

@Serializable
data class MyRegistration(
    val eventId: String? = null,
    val squadId: String? = null,
    val isSubstitute: Boolean = false,
)

@Serializable
data class SquadBrief(
    val id: String,
    val index: Int,
    val capacity: Int,
    val nature: SquadNature,
    val registeredCount: Int,
)

@Serializable
data class EventSummary(
    val id: String,
    val title: String,
    val eventTime: String,
    val status: String, // UPCOMING | ARCHIVED
    val requiredCount: Int,
    val format: String? = null, // BO3 | BO5 | R2
    val nature: EventNature,
    val name: EventName? = null,
    val customName: String? = null,
    val opponent: String? = null,
    val map: EventMap? = null,
    val createdAt: String,
    val isRead: Boolean = false,
    val squads: List<SquadBrief> = emptyList(),
    val totalRegistered: Int = 0,
    val totalSubstitutes: Int = 0,
    val myRegistration: MyRegistration? = null,
)

@Serializable
data class SquadMember(
    val registrationId: String,
    val userId: String,
    val username: String,
    val nickname: String,
    val abilities: List<Ability> = emptyList(),
    val duties: List<Duty> = emptyList(),
)

@Serializable
data class SquadDetail(
    val id: String,
    val index: Int,
    val capacity: Int,
    val nature: SquadNature,
    val registeredCount: Int,
    val members: List<SquadMember> = emptyList(),
)

@Serializable
data class EventDetail(
    val id: String,
    val title: String,
    val eventTime: String,
    val status: String,
    val requiredCount: Int,
    val format: String? = null,
    val nature: EventNature,
    val name: EventName? = null,
    val customName: String? = null,
    val opponent: String? = null,
    val map: EventMap? = null,
    val createdAt: String,
    val version: String = "",
    val squads: List<SquadDetail> = emptyList(),
    val substitutes: List<SquadMember> = emptyList(),
    val totalRegistered: Int = 0,
    val totalSubstitutes: Int = 0,
    val myRegistration: MyRegistration? = null,
)

@Serializable
data class EventCreateRequest(
    val eventTime: String, // YYYY-MM-DDTHH:mm（北京时间）
    val natureId: String,
    val nameId: String? = null,
    val customName: String? = null,
    val mapId: String? = null,
    val opponent: String,
    val requiredCount: Int,
    val squadNatures: List<String>,
    val format: String? = null,
)

@Serializable
data class EventManageRequest(
    val id: String,
    val status: String? = null,
    val natureId: String? = null,
    val nameId: String? = null,
    val customName: String? = null,
    val mapId: String? = null,
    val opponent: String? = null,
    val format: String? = null,
    val eventTime: String? = null,
    val squads: List<SquadNatureUpdate>? = null,
)

@Serializable
data class SquadNatureUpdate(val id: String, val natureId: String)

@Serializable
data class AssignRequest(
    val eventId: String? = null,
    val registrationId: String? = null,
    val targetSquadId: String? = null,
    val moves: List<AssignMove>? = null,
)

@Serializable
data class AssignMove(val registrationId: String, val targetSquadId: String?)

@Serializable
data class RegisterEventRequest(
    val eventId: String,
    val squadId: String? = null,
    val asSubstitute: Boolean = false,
)

@Serializable
data class RegisterEventResponse(
    val success: Boolean,
    val registrationId: String? = null,
    val isSubstitute: Boolean = false,
    val fellbackToSubstitute: Boolean = false,
    val message: String? = null,
)

// ============ 公告 ============

@Serializable
data class AnnouncementAuthor(
    val id: String? = null,
    val username: String,
    val nickname: String,
)

@Serializable
data class AnnouncementSummary(
    val id: String,
    val title: String,
    val author: AnnouncementAuthor,
    val createdAt: String,
    val updatedAt: String,
    val isArchived: Boolean = false,
    val isRead: Boolean = false,
    val commentCount: Int = 0,
)

@Serializable
data class AnnouncementImage(val id: String, val path: String, val sortOrder: Int = 0)

@Serializable
data class Comment(
    val id: String,
    val content: String,
    val createdAt: String,
    val user: AnnouncementAuthor,
    val isMine: Boolean = false,
)

@Serializable
data class AnnouncementDetail(
    val id: String,
    val title: String,
    val contentMarkdown: String,
    val isArchived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    // 新建接口的响应不含 author/comments/isRead，均可缺省
    val author: AnnouncementAuthor? = null,
    val images: List<AnnouncementImage> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val isRead: Boolean = false,
)

@Serializable
data class CommentRequest(val announcementId: String, val content: String)

@Serializable
data class AnnouncementSaveRequest(
    val id: String? = null,
    val title: String? = null,
    val contentMarkdown: String? = null,
    val images: List<String>? = null,
    val isArchived: Boolean? = null,
)

// ============ 成员 ============

@Serializable
data class MemberProfile(
    val id: String,
    val username: String,
    val nickname: String,
    val role: String,
    val createdAt: String,
    val abilities: List<Ability> = emptyList(),
    val duties: List<Duty> = emptyList(),
    val operators: List<Operator> = emptyList(),
    val teamPrefix: String? = null,
)

@Serializable
data class AdminUserCount(val registrations: Int = 0)

@Serializable
data class AdminUser(
    val id: String,
    val username: String,
    val nickname: String,
    val role: String,
    val disabled: Boolean,
    val createdAt: String,
    @SerialName("_count") val count: AdminUserCount? = null,
) {
    val registrationsCount: Int get() = count?.registrations ?: 0
}

@Serializable
data class UserPatchRequest(
    val id: String,
    val role: String? = null,
    val disabled: Boolean? = null,
)

@Serializable
data class ResetPasswordRequest(val id: String, val password: String)

// ============ 邀请码 ============

@Serializable
data class InvitationCode(
    val id: String,
    val code: String,
    val maxUses: Int,
    val usedCount: Int,
    val remaining: Int,
    val createdAt: String,
    val createdBy: AnnouncementAuthor,
)

@Serializable
data class InvitationCreateRequest(val maxUses: Int)

// ============ 我的 / 通知 ============

@Serializable
data class MePatchRequest(
    val nickname: String? = null,
    val password: String? = null,
    val oldPassword: String? = null,
    val abilityIds: List<String>? = null,
    val dutyIds: List<String>? = null,
    val operatorIds: List<String>? = null,
)

@Serializable
data class NotificationSettings(
    val userId: String? = null,
    val notifyNewEvent: Boolean = true,
    val notifyEventReminder: Boolean = true,
    val reminderLeadMinutes: Int = 60,
    val notifyAnnouncement: Boolean = true,
)

@Serializable
data class NotificationSettingsPatch(
    val notifyNewEvent: Boolean? = null,
    val notifyEventReminder: Boolean? = null,
    val reminderLeadMinutes: Int? = null,
    val notifyAnnouncement: Boolean? = null,
)

@Serializable
data class DeviceRegisterRequest(
    val registrationId: String,
    val model: String? = null,
    val appVersion: String? = null,
)

@Serializable
data class SimpleOk(val success: Boolean = true)
