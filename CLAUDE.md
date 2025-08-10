# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 所有交互回答都使用中文

## Project Architecture

This is an Android application built with Kotlin and Jetpack Compose, using MVVM architecture. The app appears to be called "Askew" and includes performance monitoring capabilities and OAuth authentication.

### Key Components

- **MainActivity.kt**: Entry point with bottom navigation setup (Home, Search, Profile screens)
- **HomeScreen.kt**: Contains main UI components including navigation host and individual page implementations
- **MainViewModel.kt**: Basic ViewModel with memory debugging capabilities
- **Performance Module** (`app/src/main/java/com/yy/askew/performance/`):
  - `PerformanceDashboard.kt`: System performance monitoring UI
  - `PerformanceViewModel.kt`: Manages CPU, memory, and battery metrics with real-time updates
  - `PerformanceTable.kt`: Table component for displaying metrics
  - `data/PerformanceMetric.kt`: Data class for performance metrics
- **HTTP Module** (`app/src/main/java/com/yy/askew/http/`):
  - `requestInitialToken.kt`: OAuth token management for Django backend integration
- **UI Theme** (`app/src/main/java/com/yy/askew/ui/theme/`): Material3 theming
- **Shadow Effects** (`app/src/main/java/com/yy/askew/shadow/`): Custom path shadow implementations

### Architecture Patterns

- **Navigation**: Bottom navigation with Material3 NavigationBar and NavHost
- **MVVM**: ViewModels with Compose integration for state management
- **UI State**: Uses `mutableStateListOf` for reactive performance metrics updates
- **Async Operations**: Coroutines with structured concurrency (5-second intervals in performance monitoring)
- **Screen Structure**: Sealed class hierarchy for type-safe navigation routes
- **Compose**: Declarative UI with Material3 design system and extended icons

## Build System

- **Build Tool**: Gradle with Kotlin DSL
- **Target SDK**: 34 (Android API 34)
- **Min SDK**: 24 (Android 7.0)
- **Compose**: Enabled with BOM version 2023.08.00
- **Repository Setup**: Uses Aliyun mirrors for faster dependency resolution in China

### Key Dependencies

- Jetpack Compose (UI framework) with BOM 2023.08.00
- Navigation Compose 2.8.0 (bottom navigation)
- Material3 with extended icons (design system)
- OkHttp 4.10.0 with logging interceptor (networking)
- Gson 2.9.0 (JSON parsing)
- AppAuth 0.11.1 (OAuth2 client)
- Lifecycle ViewModel Compose 2.6.0

## Common Commands

### Building
```bash
# Make gradlew executable first
chmod +x ./gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test module
./gradlew testDebugUnitTest
```

### Installation
```bash
# Install debug build to connected device/emulator
./gradlew installDebug

# Build and install in one command
./gradlew assembleDebug installDebug
```

### Debugging
```bash
# Generate debug build with ProGuard disabled
./gradlew assembleDebug

# Run with verbose logging for network requests (OkHttp logging interceptor enabled)
```

**注意**: 需要配置 Java/JDK 环境变量 JAVA_HOME。项目使用阿里云镜像源以提高中国地区的构建速度。

## Development Notes

### Language and Localization
- All UI components use Chinese language strings
- UI text should maintain consistency with existing Chinese terminology

### Performance Monitoring
- Real-time metrics collection runs on 5-second intervals via coroutines
- Tracks CPU usage, memory consumption, and battery metrics
- Performance data is managed through reactive state in PerformanceViewModel

### Authentication & Networking
- OAuth2 integration configured for Django backend (placeholder URL needs updating)
- OkHttp with logging interceptor for network debugging
- JSON processing handled by Gson library

### UI Implementation
- Material3 design system with custom shadow effects
- Bottom navigation with proper state management and type-safe routes
- HorizontalPager implementation for swipeable content areas

### Code Organization
- Performance-related code is isolated in the `performance/` package
- HTTP utilities are centralized in the `http/` package
- Custom UI components use the established shadow system in `shadow/` package