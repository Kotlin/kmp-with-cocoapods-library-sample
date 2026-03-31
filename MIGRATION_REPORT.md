# CocoaPods to SwiftPM Migration Report

**Date:** 2026-03-31
**Kotlin Version:** 2.3.20-titan-224
**Xcode:** 26.2
**Gradle:** 9.3.1
**iOS Deployment Target:** 16.0

---

## Pre-Migration State

### CocoaPods Configuration (kotlin-library/build.gradle.kts)

```kotlin
plugins {
    kotlin("native.cocoapods")
}

cocoapods {
    summary = "..."
    homepage = "..."
    version = "1.0"
    ios.deploymentTarget = "16.0"
    pod("AppleLibrary") { version = "~> 1.0" }
    framework { baseName = "kotlin_library" }
}
```

### Pod Dependencies

| Pod | Version | linkOnly |
|-----|---------|----------|
| AppleLibrary | ~> 1.0 (local) | No |

AppleLibrary is a local Swift package that depends on SwiftyJSON.

### Kotlin Imports (Before)

| File | Import |
|------|--------|
| `kotlin-library/src/iosMain/kotlin/A.kt` | `import cocoapods.AppleLibrary.AppleLibrary` |

### Podfile Dependencies

| Pod | Source |
|-----|--------|
| kotlin_library | local (../kotlin-library) |
| AppleLibrary | local (../AppleLibrary) |

---

## Migration Steps

### Phase 2: Gradle Configuration

- **Kotlin version** updated to `2.3.20-titan-224` (already done in prior commit)
- **JetBrains dev Maven repo** added to `settings.gradle.kts` `pluginManagement` and `dependencyResolutionManagement`
- **Xcode 26 workaround** added to `gradle.properties`:
  ```properties
  org.gradle.daemon.environment.CLANG_ENABLE_EXPLICIT_MODULES=NO
  ```

### Phase 3: Add swiftPMDependencies

Added `swiftPMDependencies` block to `kotlin-library/build.gradle.kts`:

```kotlin
group = "org.jetbrains.kotlin.library.sample"

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KotlinLibrary"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        localSwiftPackage(
            directory = layout.projectDirectory.dir("../AppleLibrary"),
            products = listOf("AppleLibrary"),
        )
        xcodeProjectPathForKmpIJPlugin.set(
            layout.projectDirectory.file("../iosApp/iosApp.xcodeproj")
        )
    }
}
```

### Phase 4: Import Transformations

| File | Before | After |
|------|--------|-------|
| `kotlin-library/src/iosMain/kotlin/A.kt` | `import cocoapods.AppleLibrary.AppleLibrary` | `import swiftPMImport.org.jetbrains.kotlin.library.sample.kotlin.library.AppleLibrary` |

Namespace formula: `swiftPMImport.{group}.{module}` = `swiftPMImport.org.jetbrains.kotlin.library.sample.kotlin.library`

### Phase 5: iOS Project Reconfiguration

1. Ran `integrateEmbedAndSign` and `integrateLinkagePackage` to configure Xcode project
2. Disabled User Script Sandboxing (`ENABLE_USER_SCRIPT_SANDBOXING = NO`)
3. Ran `pod deintegrate` to remove CocoaPods integration from `.xcodeproj`

### Phase 6: Remove CocoaPods from Gradle

1. Removed `kotlin("native.cocoapods")` plugin from `kotlin-library/build.gradle.kts`
2. Removed `alias(libs.plugins.kotlinCocoapods).apply(false)` from root `build.gradle.kts`
3. Removed `kotlinCocoapods` plugin alias from `gradle/libs.versions.toml`
4. Removed entire `cocoapods { }` block
5. Deleted `kotlin-library/kotlin_library.podspec`
6. Deleted `iosApp/Podfile`, `iosApp/Podfile.lock`, `iosApp/iosApp.xcworkspace`
7. Deleted `iosApp/Pods/` directory (all CocoaPods-managed files)

---

## Errors Encountered

### Error #1: Xcode 26 Explicit Modules Incompatibility

- **Phase:** 7 (Verification — Kotlin compilation)
- **Symptom:** `convertSyntheticImportProjectIntoDefFileIphonesimulator` fails with:
  ```
  error: Unable to find module dependency: 'SwiftyJSON'
  note: Explicit modules is enabled but the compiler was not recognized
  ```
- **Root Cause:** Xcode 26+ enables explicit modules by default. The Kotlin Gradle plugin passes custom `CC` and `LD` wrapper scripts (`clangDump.sh`, `ldDump.sh`) to xcodebuild. Xcode's explicit modules system doesn't recognize these custom compilers, causing SPM dependency resolution to fail.
- **Fix:** Added `org.gradle.daemon.environment.CLANG_ENABLE_EXPLICIT_MODULES=NO` to `gradle.properties`. Gradle 9.x passes `org.gradle.daemon.environment.*` entries as environment variables to daemon child processes, which xcodebuild picks up as a build setting.
- **Generalizable:** Yes — any KMP project using `swiftPMDependencies` with Xcode 26+ will need this workaround until the Kotlin plugin adds `CLANG_ENABLE_EXPLICIT_MODULES=NO` to its xcodebuild invocation.

### Error #2: Missing `kotlinCocoapods` Plugin Reference in Root build.gradle.kts

- **Phase:** 7 (Verification — Xcode build)
- **Symptom:** Xcode build fails with `Unresolved reference 'kotlinCocoapods'` in root `build.gradle.kts`
- **Root Cause:** The `kotlinCocoapods` alias was removed from `libs.versions.toml` but the root `build.gradle.kts` still referenced it with `apply(false)`.
- **Fix:** Removed the `alias(libs.plugins.kotlinCocoapods).apply(false)` line from root `build.gradle.kts`.
- **Generalizable:** Yes — always check root build scripts for CocoaPods plugin references.

---

## Non-Trivial Decisions

1. **`isStatic = true`** — chosen for the framework configuration as recommended for SwiftPM import to avoid dynamic framework edge cases.
2. **`localSwiftPackage`** — AppleLibrary was a local CocoaPods pod, mapped to `localSwiftPackage(directory = ...)` instead of a remote `swiftPackage(url = ...)`.
3. **Xcode 26 workaround via `org.gradle.daemon.environment`** — used Gradle 9.x daemon environment property propagation instead of modifying build scripts or requiring manual env var setup.

---

## Files Changed

### Gradle
- `build.gradle.kts` — removed `kotlinCocoapods` plugin apply
- `kotlin-library/build.gradle.kts` — replaced `cocoapods {}` with `swiftPMDependencies {}`, added `binaries.framework`, added `group`
- `gradle/libs.versions.toml` — updated Kotlin version, removed `kotlinCocoapods` plugin alias
- `gradle.properties` — added `org.gradle.daemon.environment.CLANG_ENABLE_EXPLICIT_MODULES=NO`
- `settings.gradle.kts` — added JetBrains dev Maven repository

### Kotlin Source
- `kotlin-library/src/iosMain/kotlin/A.kt` — updated import from `cocoapods.*` to `swiftPMImport.*`

### Xcode / iOS
- `iosApp/iosApp.xcodeproj/project.pbxproj` — updated by `integrateEmbedAndSign`/`integrateLinkagePackage`, CocoaPods references removed

### Created
- `iosApp/_internal_linkage_SwiftPMImport/` — generated linkage Swift package
- `AppleLibrary/Package.swift` — Swift package manifest for AppleLibrary (existed as local CocoaPods pod)

### Deleted
- `iosApp/Podfile`
- `iosApp/Podfile.lock`
- `iosApp/Pods/` (entire directory)
- `iosApp/iosApp.xcworkspace/`
- `kotlin-library/kotlin_library.podspec`
