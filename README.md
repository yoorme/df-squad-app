# df-squad-app — 战队报名原生安卓 App

Kotlin + Jetpack Compose 编写的原生安卓客户端，配合 `df-squad-web` 网站服务端使用。

## 环境要求

- JDK 17（本项目在本机使用 `C:\Users\14515\tools\jdk\jdk-17.0.13+11`）
- Android SDK（API 35，位于 `%LOCALAPPDATA%\Android\Sdk`，`local.properties` 已配置）
- Gradle 8.14.3（`C:\Users\14515\tools\gradle-8.14.3`）
- Android Studio（可选，用于打开工程）

## 构建命令

```bash
cd C:\Users\14515\Documents\MMR\df-squad-app
set JAVA_HOME=C:\Users\14515\tools\jdk\jdk-17.0.13+11
C:\Users\14515\tools\gradle-8.14.3\bin\gradle.bat assembleDebug     # 调试包
C:\Users\14515\tools\gradle-8.14.3\bin\gradle.bat assembleRelease   # 签名正式包
```

产物：`app/build/outputs/apk/debug|release/*.apk`

正式包签名密钥：`app/release.keystore`，密码在 `keystore.properties`（两者均已加入 .gitignore，**请自行备份**——丢失后无法向老用户发升级包，只能换包名重装）。

## 发新版本

### 方式 A：本地打包

1. 修改 `app/build.gradle.kts` 中的 `versionCode`（+1）与 `versionName`。
2. `gradle.bat assembleRelease`，把新 APK 发给队员覆盖安装。

### 方式 B：推送到 GitHub 自动出包（推荐）

push 到 `main` 后，GitHub Actions（`.github/workflows/build-apk.yml`）会自动构建并发布到
**Releases → latest**，队员直接下载安装：

```
https://github.com/yoorme/df-squad-app/releases/latest
```

签名所需的 keystore 与密码存放在仓库 Secrets（`RELEASE_KEYSTORE_BASE64` / `KEYSTORE_PASSWORD`）；
未配置时工作流降级为 debug 签名包（可安装但不适合正式分发）。
更换签名密钥后必须在 `keystore.properties` 与仓库 Secrets 两处同步更新。

## 目录结构

```
app/src/main/java/com/yoorme/squadsignup/
├── MainActivity.kt        # 单 Activity 入口 + 通知点击路由
├── SquadApp.kt            # Application：通知渠道 + 轮询调度
├── core/                  # API 定义 / 会话存储 / 仓库层 / 时间工具
├── notify/                # 本地通知 + WorkManager 轮询 + 开机自启
└── ui/                    # Compose 界面（auth/events/announcements/members/me/admin）
```

## 技术要点

- **认证**：`POST /api/auth/app-login` 换取 30 天 Bearer Token（DataStore 加密存储），401 自动回登录页
- **屏幕适配**：WindowSizeClass，≥Medium 宽度赛事页自动切换列表-详情双栏；Material 3 动态取色 + 深色模式
- **通知**：通知渠道「赛事通知」（高优先级）与「公告通知」；WorkManager 15 分钟轮询兜底；厂商推送见 `docs/JPush接入指南.md`
- **明文 HTTP**：`network_security_config.xml` 仅放行 `121.196.195.27`；站点配置 HTTPS 域名后删掉该文件并改 baseUrl 即可
