# FolatKiroeru 悬浮歌词（FolatKiroeru）

基于 WebView 的 Android 悬浮歌词应用：加载音乐网页，将网页上的歌词抓取为悬浮窗或全屏字幕显示，边听歌边看词。

## 起源与致谢（Attribution）

**本项目原始代码由 [KirieHaruna/FloatLrc](https://github.com/KirieHaruna/FloatLrc) 的作者提供，特此致谢。**

本仓库在原始代码的基础上继续开发，新增/修改内容：

- **悬浮歌词颜色设置**：自绘 HSV 色彩轮盘（`ColorWheelView`），选色持久化保存，悬浮窗与字幕实时生效
- **字幕播放模式**：底部「字幕」按钮一键进入全屏字幕播放 —— 背景复用网页自带的音频可视化，底部大字歌词随音乐实时刷新；✕ 按钮或返回键退出
- **防息屏**：进入字幕播放模式时保持屏幕常亮（`FLAG_KEEP_SCREEN_ON`），退出后恢复系统默认息屏行为
- **共享歌词轮询**：抽取 `LyricPoller`，悬浮窗与字幕模式共用同一套轮询逻辑
- **底部控制条重构**：由绝对偏移布局改为水平 LinearLayout（[Lrc][字幕][颜色][字号]）

## 功能

| 按钮 | 功能 |
|---|---|
| Lrc | 开关悬浮歌词按钮（可拖动；字号由右侧输入框设置） |
| 字幕 | 进入/退出字幕播放模式（全屏、防息屏、网页可视化作背景） |
| 颜色 | 打开色彩轮盘，设置悬浮歌词颜色（重启后保留） |

首次启动会要求输入音乐网页的 URL（保存于本机 SharedPreferences）；歌词取自网页中 id 为 `lyric` 的元素。

## 使用

1. 首次启动输入音乐网页 URL
2. 按提示授予「显示在其他应用上层」权限（悬浮窗必需）
3. 用「Lrc」开启悬浮歌词，「颜色」换色，「字幕」进入字幕播放模式

## 构建

### 本地构建

- Android Studio 打开项目直接运行，或：
  ```bash
  gradle assembleDebug   # 调试版
  gradle assembleRelease # 正式版（未签名）
  ```

### GitHub Actions 自动构建

本仓库已配置 [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)：
推送代码或手动触发后自动构建 Debug/Release APK，产物在 Actions 页面的 **Artifacts** 中下载。

## 技术要点

- 语言：Java；minSdk 26 / targetSdk 31 / compileSdk 31；AGP 7.1
- 悬浮窗：`WindowManager` + `TYPE_APPLICATION_OVERLAY`
- 歌词抓取：`WebView.evaluateJavascript` 每 100ms 轮询 `#lyric` 的 `innerText`
- 依赖：`com.github.lzyzsd:jsbridge:1.0.4`（JitPack）
- 字幕防息屏：`WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`（进入字幕模式添加、退出清除）

## 权限

- `INTERNET`：加载音乐网页
- `SYSTEM_ALERT_WINDOW` / `SYSTEM_OVERLAY_WINDOW`：悬浮窗

## 许可

原始代码版权归 [KirieHaruna/FloatLrc](https://github.com/KirieHaruna/FloatLrc) 原作者所有；本仓库新增代码在原项目基础上修改而来，请遵守原项目许可并保留上述署名。
