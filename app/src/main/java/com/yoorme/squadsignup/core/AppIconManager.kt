package com.yoorme.squadsignup.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

// 按战队切换桌面启动图标：通过启停 activity-alias 实现
// （MMR → ic_mmr，YFD → ic_yfd，未选择 → 默认图标）
object AppIconManager {

    private val ALIASES = linkedMapOf(
        "default" to ".MainDefault",
        "mmr" to ".MainMmr",
        "yfd" to ".MainYfd",
    )

    fun applyForServer(context: Context, serverId: String?) {
        val target = if (serverId != null && ALIASES.containsKey(serverId)) serverId else "default"
        ALIASES.forEach { (id, cls) ->
            val component = ComponentName(context, "${context.packageName}$cls")
            val newState =
                if (id == target) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (context.packageManager.getComponentEnabledSetting(component) != newState) {
                runCatching {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }
    }
}
