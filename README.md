# 💰 账单对账助手

解决痛点：多平台账单分散、手动对账麻烦。

截图账单 → OCR 提取 → AI 分类 → 月度报告。

## 功能

- 导入支付宝/微信/信用卡/花呗等账单截图
- ML Kit OCR 自动识别账单文字
- DeepSeek AI 智能解析交易（自动分类 11 种类型）
- 统计卡片（总支出/总收入/交易笔数）
- 交易明细列表 + 分类编辑
- 历史账单记录（Room 本地存储）
- 纯本地存储，隐私安全

## 构建

### 方式一：Android Studio

1. 用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 点击 **▶ Run** 或 **Build → Build APK**

### 方式二：命令行

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 方式三：GitHub Actions

推送代码到 GitHub，自动在云端构建 APK（见 `.github/workflows/build-apk.yml`）。

## 配置

首次使用需要设置 DeepSeek API Key：
1. 打开 App → 设置 → 填入 API Key
2. 新用户免费额度足够日常使用

## 技术栈

- Kotlin + Jetpack Compose + Material3
- Room 本地数据库
- ML Kit 中文 OCR
- DeepSeek API 智能解析
- MVVM + Repository 架构
