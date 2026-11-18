# 健康运动记录 APP（选题 11）

Android 原生 Java + SQLite，最低 Android 8.0（API 26）。完全离线，无第三方库。

## 快速开始
首次打开输入 3–20 位字母、数字或下划线用户名及 6–64 位密码，点击注册；注册成功后进入运动统计。没有预设账号和内置个人数据。

每日保存一条汇总：日期、全天步数、全天运动分钟数、主要运动类型、备注。相同日期不能重复保存，请到记录列表修改。统计分别显示步数、分钟和记录天数，不将不同单位相加。本周从周一到今天，本月从月初到今天。

记录列表支持日期和类型筛选，点击记录选择修改或删除，删除前需确认。日期使用选择器，备注支持多行输入。界面使用原生控件和简短文字。


| 内容 | 本项目实现 |
| --- | --- |
| 注册、登录 | 用户名检查、密码校验、账号数据隔离 |
| 增删改查 | 每日运动记录、日期及类型筛选、删除确认 |
| 统计 | 今日、本周、本月的步数、分钟和记录天数 |
| 数据存储 | SQLiteOpenHelper、用户表和记录表、参数化查询 |
| 基础组件 | TextView、EditText、Button、LinearLayout、ScrollView |
| 列表和选择 | ListView、ArrayAdapter、Spinner、DatePickerDialog |
| 操作反馈 | Toast、AlertDialog、登录时的 ProgressBar |
| 页面状态 | Bundle 保存页面、账号、筛选条件；固定控件 ID 恢复表单 |

## Android Studio
打开本目录，安装 SDK Platform 35，推荐使用 JDK 21，使用工程附带的 Gradle Wrapper 8.11.1（首次同步需联网下载 Gradle 和 Android Gradle Plugin 8.9.1）。也可运行 gradlew.bat assembleDebug，或直接运行下述独立构建脚本，完全不依赖 Gradle。

### Gradle JVM 设置
在 Android Studio 的 Settings → Build, Execution, Deployment → Build Tools → Gradle 中，将 Gradle JDK 设为 JDK 21。本机路径为 `D:\java\jkd-21`，可通过 Add JDK 选择该目录。不要选择当前 Android Studio 自带的 JDK 25：Gradle 8.11.1 无法在 JDK 25 上运行。设置后点击 Apply 并重新 Sync Project with Gradle Files。

当前工作目录的 `.gradle/config.properties` 已将 `java.home` 设为本机 JDK 21，并由 Android Studio 的 `#GRADLE_LOCAL_JAVA_HOME` 设置引用。源码压缩包不包含本机 IDE 配置与缓存，重新解压或换电脑后请按上述步骤选择 JDK 21。命令行构建时，也请将 `JAVA_HOME` 指向 JDK 21。

## 离线编译
需要 Python 3、JDK 17+ 和完整 Android SDK。Windows 示例：

    python tools/build_offline.py --sdk D:/Android/sdk --jdk D:/java/jkd-21 --platform android-35 --build-tools 35.0.0


## 项目结构
- MainActivity.java：登录、概览、筛选、编辑和删除确认界面。
- Store.java：SQLite 数据访问、参数化查询、PBKDF2 密码校验。
- Rules.java：纯 Java 日期边界和输入验证。
- tests/RulesTest.java：34 条核心规则断言。
- tests/test_database.py：6 个 SQLite 结构与查询测试。

## 测试命令
    javac -encoding UTF-8 -d build-tests app/src/main/java/cn/edu/healthtrack/Rules.java tests/RulesTest.java
    java -cp build-tests RulesTest
    python tests/test_database.py

2026-11-17 验证结果：34 条规则断言、6 项 SQLite 测试通过；Gradle 离线 assembleDebug 构建成功；独立构建脚本的 javac、D8、aapt2、zipalign 和 APK 签名验证通过。

## 设计边界
手工录入，不包含传感器自动计步、定位、云同步或医疗建议。数据留在应用私有目录，卸载会删除数据；当前版本不提供备份导出。屏幕旋转等 Activity 重建时恢复页面与表单，不保存登录页密码；主动退出应用后重新登录。ListView 复用行视图，查询仍一次读取匹配记录，适合课程演示数据量。
