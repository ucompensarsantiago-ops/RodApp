# RodApp - Project Overview

RodApp is an Android application built with Kotlin, designed for an e-commerce or product catalog experience. It features user authentication, a product catalog, shopping cart functionality, and administrative controls.

## Technologies and Architecture

- **Language:** Kotlin
- **Platform:** Android (Min SDK 24, Target/Compile SDK 36)
- **Backend:** [Supabase](https://supabase.com/) (Auth and Postgrest for data)
- **Networking:** [Ktor](https://ktor.io/) (underlying client for Supabase)
- **UI:** XML Layouts with **ViewBinding**, Material Design components.
- **Navigation:** Single activity (`MainActivity`) managing multiple fragments via `BottomNavigationView` and `DrawerLayout`.
- **Security:** Biometric authentication and `EncryptedSharedPreferences` for secure credential storage.

## Directory Structure

- `app/src/main/java/com/example/rodapp/`: Root package.
    - `activities/`: Core activities for the initial flow (Splash, Onboarding, Login, Register).
    - `main/`: Main application logic after authentication.
        - `MainActivity.kt`: Orchestrates navigation between fragments.
        - `admin/`: Fragments for administrative tasks.
        - `perfil/`: User profile management.
        - `productos/`: Fragments for Home, Catalog, Favorites, and Cart.
    - `SupabaseClient.kt`: Singleton for Supabase configuration.
    - `SecurityManager.kt`: Helper for biometrics and encrypted storage.
- `app/src/main/res/`: Resources (layouts, drawables, values, menus).

## Building and Running

### Prerequisites
- Android Studio (latest stable version recommended).
- JDK 11 (as configured in `build.gradle.kts`).

### Key Commands
- **Build Project:** `./gradlew build`
- **Generate Debug APK:** `./gradlew assembleDebug`
- **Run Unit Tests:** `./gradlew test`
- **Run Instrumentation Tests:** `./gradlew connectedAndroidTest`

## Development Conventions

- **ViewBinding:** Always use ViewBinding for accessing layout components instead of `findViewById`.
- **Coroutines:** Use `lifecycleScope` or `viewModelScope` (if ViewModels are introduced) for asynchronous operations, especially when interacting with the Supabase client.
- **Supabase Integration:** All data persistence and authentication should go through the `SupabaseClient` singleton.
- **Fragment Management:** `MainActivity` is the primary host. When adding new main features, create a Fragment and register it in the `MainActivity` navigation logic (`BottomNav` or `Drawer`).
- **Security:** Never store sensitive data in plain text. Use `SecurityManager` for handling credentials and biometrics.
- **Styling:** Follow Material Design guidelines. Icons and themes are managed in `res/drawable` and `res/values/themes.xml`.
