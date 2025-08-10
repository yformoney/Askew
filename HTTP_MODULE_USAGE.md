# HTTP模块使用说明

## 概述
这是一个基于MVVM架构、专为Android应用设计的可扩展HTTP网络请求模块。支持RESTful API、自动Token管理、错误处理和安全存储。

## 模块结构
```
http/
├── api/                    # API接口定义层
│   ├── ApiService.kt       # 抽象API服务基类
│   └── AuthApiService.kt   # 认证相关API服务
├── client/                 # 网络客户端层
│   └── NetworkClient.kt    # OkHttp客户端配置
├── interceptor/            # 拦截器层
│   ├── AuthInterceptor.kt      # 自动Token认证拦截器
│   └── ResponseInterceptor.kt  # 响应处理拦截器
├── model/                  # 数据模型层
│   └── ApiResponse.kt      # API响应和结果封装
├── repository/             # 数据仓库层
│   └── AuthRepository.kt   # 认证数据仓库
├── exception/              # 异常处理层
│   └── NetworkException.kt # 网络异常定义
├── example/                # 使用示例
│   ├── AuthViewModel.kt    # 认证ViewModel示例
│   └── LoginScreen.kt      # 登录界面示例
└── HttpManager.kt          # HTTP模块管理器
```

## 核心特性

### 1. 类型安全的API结果封装
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    data class Loading(val isLoading: Boolean = true) : ApiResult<Nothing>()
}
```

### 2. 自动Token管理
- 自动添加Authorization头部
- Token过期自动刷新
- 安全的加密存储

### 3. 统一错误处理
- 网络异常分类处理
- HTTP状态码自动映射
- 用户友好的错误信息

### 4. 请求拦截器
- 日志记录（Debug模式）
- 认证信息自动注入
- 响应状态处理

## 快速开始

### 1. 初始化（在Application或MainActivity中）
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化HTTP模块
        HttpManager.initialize(this)
        
        // ... 其他代码
    }
}
```

### 2. 在ViewModel中使用
```kotlin
class AuthViewModel : ViewModel() {
    private val authRepository = HttpManager.getAuthRepository()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            when (val result = authRepository.login(username, password)) {
                is ApiResult.Success -> {
                    // 登录成功，处理token
                }
                is ApiResult.Error -> {
                    // 处理错误
                }
                is ApiResult.Loading -> {
                    // 显示加载状态
                }
            }
        }
    }
}
```

### 3. 在Compose界面中使用
```kotlin
@Composable
fun LoginScreen() {
    val viewModel: AuthViewModel = viewModel()
    val loginState by viewModel.loginState.collectAsState()
    
    when (loginState) {
        is ApiResult.Loading -> {
            // 显示加载指示器
        }
        is ApiResult.Success -> {
            // 登录成功，导航到主页
        }
        is ApiResult.Error -> {
            // 显示错误信息
        }
    }
}
```

## 扩展API服务

### 创建自定义API服务
```kotlin
class MyApiService : ApiService() {
    
    private val baseUrl = "https://api.example.com"
    
    // GET请求示例
    suspend fun getUsers(): ApiResult<List<User>> {
        return get("$baseUrl/users")
    }
    
    // POST请求示例
    suspend fun createUser(user: User): ApiResult<User> {
        return post("$baseUrl/users", user)
    }
    
    // 带自定义头部的请求
    suspend fun getProtectedData(): ApiResult<ProtectedData> {
        return get(
            url = "$baseUrl/protected",
            headers = mapOf("Custom-Header" to "value")
        )
    }
}
```

### 创建对应的Repository
```kotlin
class MyRepository {
    private val apiService = MyApiService()
    
    suspend fun getUsers(): ApiResult<List<User>> {
        return apiService.getUsers()
    }
    
    suspend fun createUser(user: User): ApiResult<User> {
        return apiService.createUser(user)
    }
}
```

## 配置说明

### 网络配置
在 `NetworkClient.kt` 中可以修改：
- 连接超时时间
- 读取超时时间
- 日志级别
- 自定义拦截器

### 认证配置
在 `AuthRepository.kt` 中可以修改：
- 客户端ID
- 服务器URL
- Token存储策略

### 错误处理配置
在 `NetworkException.kt` 中可以添加：
- 自定义异常类型
- 错误码映射
- 错误信息本地化

## 安全考虑

1. **Token安全存储**: 使用EncryptedSharedPreferences加密存储
2. **HTTPS**: 强制使用HTTPS协议
3. **证书验证**: 支持自定义证书验证
4. **请求签名**: 可扩展支持请求签名

## 性能优化

1. **连接池复用**: OkHttp自动管理连接池
2. **请求缓存**: 可配置HTTP缓存策略
3. **并发控制**: 基于Kotlin协程的异步处理
4. **内存优化**: 自动处理响应流

## 测试支持

模块支持单元测试和集成测试：
- MockWebServer集成
- Repository层测试
- ViewModel层测试
- UI层测试

## 注意事项

1. 必须在Application或Activity中调用 `HttpManager.initialize(context)`
2. 网络请求必须在协程中执行
3. Token会自动管理，无需手动处理
4. 错误处理已统一封装，建议使用ApiResult进行状态管理