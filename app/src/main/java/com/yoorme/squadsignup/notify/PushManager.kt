package com.yoorme.squadsignup.notify

import android.content.Context
import cn.jiguang.api.utils.JCollectionAuth
import cn.jpush.android.api.JPushInterface

// 极光推送管理：初始化与 registration_id 获取
object PushManager {

    fun init(context: Context) {
        // 已同意隐私政策（本 App 为内部工具，安装即视为同意）
        JCollectionAuth.setAuth(context, true)
        JPushInterface.setDebugMode(false)
        JPushInterface.init(context)
    }

    // SDK 注册成功后才有值；启动初期可能为空串
    fun registrationId(context: Context): String =
        JPushInterface.getRegistrationID(context) ?: ""
}
