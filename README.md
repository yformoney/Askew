# Askew 打车应用

基于Android + Kotlin + Jetpack Compose构建的现代化打车应用。

## 📱 功能特性

### 🚗 核心功能
- **快速叫车**: 输入起点和终点，一键叫车
- **订单管理**: 查看历史订单，实时跟踪订单状态
- **用户认证**: 安全的登录/注册系统
- **个人中心**: 用户信息管理和设置

### 🎨 界面设计
- **Material3设计**: 现代化的UI设计语言
- **响应式布局**: 适配不同屏幕尺寸
- **直观导航**: 底部导航栏，操作便捷
- **状态反馈**: 完整的加载和错误状态提示

## 🏗️ 技术架构

### 架构模式
- **MVVM**: Model-View-ViewModel架构模式
- **Repository模式**: 数据访问层抽象
- **依赖注入**: 通过HttpManager管理依赖

### 技术栈
- **开发语言**: Kotlin 100%
- **UI框架**: Jetpack Compose + Material3
- **网络请求**: OkHttp + Gson
- **异步处理**: Kotlin Coroutines + Flow
- **数据存储**: EncryptedSharedPreferences
- **构建工具**: Gradle (Kotlin DSL)

### 项目结构
```
app/src/main/java/com/yy/askew/
├── MainActivity.kt                    # 应用入口
├── HomeScreen.kt                     # 主要UI页面
├── http/                             # 网络通信层
│   ├── HttpManager.kt               # HTTP管理器
│   ├── api/                         # API接口定义
│   │   ├── AuthApiService.kt        # 用户认证API
│   │   └── OrderApiService.kt       # 订单管理API
│   ├── repository/                  # 数据仓库层
│   │   ├── AuthRepository.kt        # 用户认证数据仓库
│   │   └── OrderRepository.kt       # 订单数据仓库
│   ├── model/                       # 数据模型
│   │   └── ApiResponse.kt           # API响应模型
│   ├── client/                      # 网络客户端
│   │   └── NetworkClient.kt         # OkHttp配置
│   ├── interceptor/                 # 请求拦截器
│   │   ├── AuthInterceptor.kt       # 认证拦截器
│   │   └── ResponseInterceptor.kt   # 响应拦截器
│   └── exception/                   # 异常处理
│       └── NetworkException.kt      # 网络异常定义
├── viewmodel/                       # ViewModel层
│   └── OrderViewModel.kt           # 订单业务逻辑
├── ui/                             # UI组件
│   ├── TaxiOrderScreen.kt          # 创建订单界面
│   └── OrderListScreen.kt          # 订单列表界面
├── performance/                     # 性能监控
└── shadow/                          # UI效果组件
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17 或更高版本
- Android SDK API 34
- Kotlin 1.9.0 或更高版本

### 构建项目
```bash
# 克隆项目
git clone https://github.com/your-username/Askew.git
cd Askew

# 赋予gradlew执行权限
chmod +x ./gradlew

# 构建调试版本
./gradlew assembleDebug

# 运行单元测试
./gradlew test

# 安装到设备
./gradlew installDebug
```

### 配置说明
1. **网络配置**: 服务器地址配置在 `AuthApiService` 和 `OrderApiService` 中
2. **API密钥**: 如需要，在相应的配置文件中添加API密钥
3. **依赖管理**: 项目使用阿里云镜像源加速依赖下载

## 📋 API接口

### 后端服务
- **服务器地址**: `http://8.138.38.200/api`
- **认证方式**: Token认证
- **数据格式**: JSON

### 主要接口
- `POST /auth/login/` - 用户登录
- `POST /auth/logout/` - 用户登出
- `GET /auth/profile/` - 获取用户信息
- `POST /orders/` - 创建订单
- `GET /orders/` - 获取订单列表
- `GET /orders/{id}/` - 获取订单详情
- `POST /orders/{id}/cancel/` - 取消订单

## 📱 应用截图

### 主要界面
- **叫车页面**: 输入起点终点，快速叫车
- **订单页面**: 查看订单历史和状态
- **个人中心**: 用户信息和应用设置

## 🔧 开发指南

### 添加新功能
1. 在 `http/api/` 中定义API接口
2. 在 `http/model/` 中定义数据模型
3. 在 `repository/` 中实现数据访问逻辑
4. 在 `viewmodel/` 中实现业务逻辑
5. 在 `ui/` 中实现界面组件

### 代码规范
- 遵循Kotlin官方代码规范
- 使用有意义的变量和函数命名
- 添加必要的注释说明
- 保持代码整洁和可读性

## 🧪 测试

### 单元测试
```bash
./gradlew testDebugUnitTest
```

### UI测试
```bash
./gradlew connectedAndroidTest
```

## 📦 构建发布

### 调试版本
```bash
./gradlew assembleDebug
```

### 发布版本
```bash
./gradlew assembleRelease
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

- **开发者** - [Your Name](https://github.com/your-username)

## 🙏 致谢

- 感谢 Android 开发团队提供的优秀工具
- 感谢 Jetpack Compose 团队的现代化UI框架
- 感谢所有贡献者的支持

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件至 your-email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！