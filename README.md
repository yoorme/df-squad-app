# df-squad-app — 三角洲行动战队管理（原生安卓 App）

Kotlin + Jetpack Compose 编写的原生安卓客户端，配合 `df-squad-web` 网站服务端使用。

## 环境要求

- JDK 17 及以上（CI 使用 17；本机可用更高版本）
- Android SDK：compileSdk 37 / build-tools 37.0.0（`local.properties` 已配置 SDK 路径）
- Gradle 无需单独安装：用项目自带的 wrapper（`gradlew` / `gradlew.bat`，9.8.0）
- Android Studio（可选，用于打开工程）
- 工程路径需为纯 ASCII（当前位于 `C:\Users\14515\Documents\MMR\df-squad-app`）

## 构建命令

```bash
cd C:\Users\14515\Documents\MMR\df-squad-app
gradlew.bat assembleDebug     # 调试包
gradlew.bat assembleRelease   # 签名正式包（R8 混淆裁剪）
gradlew.bat lintDebug         # 静态检查（当前基线 0 errors）
```

产物：`app/build/outputs/apk/debug|release/*.apk`

正式包签名密钥：`app/release.keystore`，密码在 `keystore.properties`（两者均已加入 .gitignore，**请自行备份**——丢失后无法向老用户发升级包，只能换包名重装）。

release 开启 R8 后带混淆：排障需用 `app/build/outputs/mapping/release/mapping.txt` 还原堆栈（该文件未入库，出包后请连同 APK 一起留存）。

## 发新版本

### 方式 A：本地打包

1. 修改 `app/build.gradle.kts` 中的 `versionCode`（+1）与 `versionName`。
2. `gradlew.bat assembleRelease`，把新 APK 发给队员覆盖安装。

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
├── MainActivity.kt        # 单 Activity 入口；启动等待主题与会话首读，通知点击路由
├── SquadApp.kt            # Application：通知渠道 + 轮询调度 + 应用级协程作用域
├── core/                  # API / 会话存储 / 仓库层 / 时间与拼音工具 / 图片压缩与保存
├── notify/                # 本地通知 + WorkManager 轮询 + 极光推送
└── ui/                    # Compose 界面（auth/events/announcements/members/me/admin）
    ├── theme/             # 配色（默认=网站同源 / 动态取色）+ 动效令牌
    └── components/        # Markdown 渲染、全屏图片查看、通用组件
```

## 技术要点

- **认证**：`POST /api/auth/app-login` 换取 30 天 Bearer Token，存于应用私有 DataStore；
  401 自动清会话回登录页（改密/重置后旧 token 立即失效）
- **主题**：默认配色与网站 `globals.css` 逐角色一致；「我的 → 外观」可切换动态取色（Android 12+），
  明暗跟随系统；页面转场与动效令牌与网站 CSS motion 参数同源
- **公告图片**：编辑页可拍照/相册上传（>5MB 自动压缩并纠正 EXIF 方向，接口与网站管理端相同）；
  正文图片双击全屏查看（拖动/捏合缩放），长按或按钮保存到相册 `Pictures/三角洲行动战队管理`
- **通知**：通知渠道「赛事通知」（高优先级）与「公告通知」；WorkManager 15 分钟轮询兜底
  （跨重启由 WorkManager 自行恢复）；厂商推送见网站仓库 `df-squad-web` 的 `docs/JPush接入指南.md`
- **出包**：release 开启 R8 代码裁剪（约 16.9MB → 5.2MB），keep 规则见 `app/proguard-rules.pro`
- **明文 HTTP**：`network_security_config.xml` 仅放行 `121.196.195.27`；站点配置 HTTPS 域名后删掉该文件并改 baseUrl 即可
