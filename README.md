# CarFloatPanel 车机悬浮面板
> 安卓车机悬浮窗APP，适配安卓8.0（2+32G低配车机）
功能：高德地图悬浮显示、USB胎压读取、音乐播放器快捷入口、7个自定义快捷启动APP、窗口缩放拖动、内存保护自动隐藏地图、开机自启动悬浮面板

## 📱 目标设备
- Android 8.0 (API26)
- 内存：2GB RAM + 32GB存储
- 架构：armeabi-v7a

## ✨ 功能清单
1. 悬浮窗口，可自由拖动、放大缩小
2. 内嵌高德地图SDK
3. USB串口胎压读取（CH340/CP2102）
4. 自定义7个快捷应用启动图标
5. 音乐播放器、胎压APP一键选择
6. 内存检测：内存过低自动隐藏地图，防止APP闪退
7. 开机自动启动悬浮面板

## 🛠️ 编译方式
GitHub Actions 云编译，直接触发Workflow自动打包Debug APK
1. Fork / Clone 本仓库
2. 进入仓库 `Actions`
3. 选择 `Build APK`，点击 `Run workflow` 手动启动编译
4. 编译完成后，在Artifacts下载 `car-float-apk` 压缩包，内含 `app-debug.apk`

## ⚠️ 重要配置修改（编译前必须）
1. 修改 `app/src/main/AndroidManifest.xml`
   将高德SDK API_KEY替换为你自己在高德开放平台申请的安卓Key
2. 编译成功后，提取APK的SHA1签名指纹，填入高德开放平台对应的Key配置，否则地图空白无法加载

## 📥 车机安装与授权
### 1. ADB安装APK
```bash
adb connect 车机IP:5555
adb install app-debug.apk
