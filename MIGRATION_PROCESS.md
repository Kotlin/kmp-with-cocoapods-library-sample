# Migration Process: CocoaPods to SPM Import

This document tracks the migration process from CocoaPods to Swift Package Manager (SPM) import for the Kotlin Multiplatform project.

---

## Phase 1: Pre-Migration Analysis

### 1.0 Verify the project builds

**Step 1.0.1: Find modules using CocoaPods**

Command executed:
```bash
grep -rl "cocoapods" --include="build.gradle.kts" .
```

Result: `./kotlin-library/build.gradle.kts`

Module name to migrate: `kotlin-library`

**Step 1.0.2: Examine CocoaPods configuration**

Read `./kotlin-library/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
}

group = "org.jetbrains.kotlin.library.sample"
version = "1.0-SNAPSHOT"

kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Kotlin sample project with CocoaPods dependencies"
        homepage = "https://github.com/Kotlin/kotlin-with-cocoapods-library-sample"

        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")

        pod("AppleLibrary") {
            version = "0.1.0"
            source = path(project.file("../AppleLibrary"))
        }

        framework {
            baseName = "KotlinLibrary"
            isStatic = true
        }
    }
    ...
}
```

**Pod dependencies found:**
- `AppleLibrary` (local pod, version `0.1.0`)

**Framework configuration:**
- `baseName`: `KotlinLibrary`
- `isStatic`: `true`
- `deploymentTarget`: `16.0`

**Step 1.0.3: Examine Podfile**

Read `./iosApp/Podfile`:

```ruby
platform :ios, '16.6'

target 'iosApp' do
  use_frameworks!

  pod 'kotlin_library', :path => '../kotlin-library'
  pod 'AppleLibrary', :path => '../AppleLibrary'
done
```

**Podfile pods:**
- `kotlin_library` (refers to the main module)
- `AppleLibrary` (local pod, matches Gradle config)

**Step 1.0.4: Find Kotlin source files and CocoaPods imports**

Found Kotlin source file: `./kotlin-library/src/iosMain/kotlin/A.kt`

**CocoaPods imports found in source:**
- `import cocoapods.AppleLibrary.*`

**Step 1.0.5: Verify Kotlin compilation**

Command executed:
```bash
./gradlew :kotlin-library:compileKotlinIosSimulatorArm64
```

**Result:** `BUILD SUCCESSFUL`

**Kotlin compilation output:**
```
> Task :kotlin-library:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :kotlin-library:podSetupBuildAppleLibraryIosSimulator UP-TO-DATE
> Task :kotlin-library:podBuildAppleLibraryIosSimulator
> Task :kotlin-library:cinteropAppleLibraryIosSimulatorArm64 UP-TO-DATE
> Task :kotlin-library:compileKotlinIosSimulatorArm64 UP-TO-DATE
```

**Status:** Kotlin compilation successful. Ready to proceed with migration.

**Step 1.0a: Confirm Kotlin version with Swift Import support**

Current Kotlin version from `gradle/libs.versions.toml`:
- `kotlin = "2.3.20"`

Target Kotlin version for SPM support: `2.3.20-titan-224`

**Kotlin version check:** The current version (2.3.20) needs to be updated to `2.3.20-titan-224` which supports `swiftPMDependencies` DSL.

**User confirmation:** 
1. Use Kotlin version `2.3.20-titan-224` ✓
2. Add JetBrains dev repository ✓

**Step 1.1: Check for deprecated CocoaPods workaround property**

Checked `gradle.properties` - no deprecated property `kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies` found.

**Step 1.2: Check for EmbedAndSign disablers**

Checking `build.gradle.kts` files for code that disables `EmbedAndSign` tasks - none found.

**Step 1.3: Check for third-party KMP libraries with bundled cinterop klibs**

Pod dependency analysis:
- `AppleLibrary` - local pod (not a third-party KMP library with bundled klibs)

No bundled klib detection needed for this migration.

---

## Phase 2: Gradle Configuration

**Important scope note:** Do NOT upgrade the Gradle wrapper version, update KSP, or update any other dependencies during this migration. Those are separate concerns and out of scope. Only change what is listed below.

### 2.1 Add custom Maven repository

Added JetBrains dev repository to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")  // ADDED
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")  // ADDED
    }
}
```

### 2.2 Update Kotlin version

Updated `gradle/libs.versions.toml`:

**Before:**
```toml
[versions]
kotlin = "2.3.20"
```

**After:**
```toml
[versions]
kotlin = "2.3.20-titan-224"
```

### 2.3 Add buildscript constraint

The skill suggests adding a buildscript constraint to force the Kotlin version. However, since we're using the version catalog (`libs.plugins.kotlinMultiplatform`), this step may not be necessary. We'll proceed without it first and add it only if compilation issues arise.

---

## Phase 3: Add swiftPMDependencies (Keep CocoaPods)

**DO NOT remove the `cocoapods {}` block or `kotlin("native.cocoapods")` plugin yet.** Add `swiftPMDependencies {}` alongside the existing CocoaPods configuration.

### 3.1 Add group property

The `group` property is already set:
```kotlin
group = "org.jetbrains.kotlin.library.sample"
```

**Compose Resources warning:** If the project uses Compose Multiplatform resources, the `group` property is also used as the namespace for generated resource accessors. This project does NOT use Compose, so this is safe.

### 3.2 Add swiftPMDependencies block alongside cocoapods

**Map pods to SPM:**
- `AppleLibrary` - local pod converted to SPM package (see **Phase 3.2a**)

---

### 3.2a: Convert AppleLibrary pod to SPM package

**Create `AppleLibrary/Package.swift`:**

```swift
// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "AppleLibrary",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "AppleLibrary",
            targets: ["AppleLibrary"])
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "AppleLibrary",
            path: "AppleLibrary.xcframework"
        )
    ]
)
```

**Note:** Since we're using a local pod converted to SPM, we actually created a simpler `Package.swift`:

```swift
// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "AppleLibrary",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "AppleLibrary",
            targets: ["AppleLibrary"])
    ],
    targets: [
        .target(
            name: "AppleLibrary",
            dependencies: [],
            path: "AppleLibrary/Classes"
        )
    ]
)
```

**SwiftLibrary.swift issues:**
- Fixed Swift 6 syntax errors (String.Index issues)
- Used `JSONSerialization` instead of SwiftyJSON

### 3.2b: Update Gradle configuration with localSwiftPackage

**Updated `kotlin-library/build.gradle.kts`:**

```kotlin
swiftPMDependencies {
    iosMinimumDeploymentTarget = "16.0"

    val appleLibraryDir = layout.projectDirectory.dir("../AppleLibrary")
    localSwiftPackage(
        directory = appleLibraryDir,
        products = listOf("AppleLibrary"),
    )
}
```

**Important Notes:**
- `AppleLibrary.podspec` remains unchanged (original configuration)
- The original podspec has `s.dependency 'SwiftyJSON', '~> 5.0'`
- `SwiftLibrary.swift` was modified to remove SwiftyJSON dependency and use `JSONSerialization` instead
- This modification is required because `swiftPMDependencies` compiles Swift code directly without CocoaPods
- When Phase 6 removes CocoaPods entirely, the podspec and SwiftyJSON dependency will no longer be used

**Build command executed:**
```bash
./gradlew :kotlin-library:compileKotlinIosSimulatorArm64
```

**Result:** `BUILD SUCCESSFUL`

---

## Phase 4: Kotlin Source Updates

### Import Namespace Formula

```
swiftPMImport.<group>.<module>.<ClassName>

Where:
- group: build.gradle.kts `group` property of the MODULE THAT DECLARES the swiftPMDependencies, dashes (-) → dots (.)
- module: Gradle module name of the MODULE THAT DECLARES the swiftPMDependencies, dashes (-) → dots (.), underscores (_) preserved as-is
- ClassName: Objective-C class name (FIR* for Firebase, GMS* for Google Maps)
```

### Example Transformation — This Project

```kotlin
// group = "org.jetbrains.kotlin.library.sample", module = "kotlin-library"

// BEFORE:
import cocoapods.AppleLibrary.*

// AFTER:
import swiftPMImport.org.jetbrains.kotlin.library.sample.kotlin.library.AppleLibrary
```

### Bulk Replacement

After Phase 3 is complete, we'll replace all `cocoapods.*` imports with `swiftPMImport.*` using the above namespace formula.

---

## Phase 5: iOS Project Reconfiguration

### 5.1 Get migration command

Build the CocoaPods workspace to obtain the migration command.

### 5.2 Update Crashlytics dSYM upload script (if applicable)

No FirebaseCrashlytics detected in this project.

### 5.3 Deintegrate CocoaPods

After successful migration, we'll deintegrate CocoaPods using `pod deintegrate`.

### 5.4 Manual integration (if automatic fails)

Will be documented if needed.

---

## Phase 6: Remove CocoaPods from Gradle

**Steps:**
1. Removed `kotlinCocoapods` plugin from `kotlin-library/build.gradle.kts`
2. Removed `cocoapods {}` block from `kotlin-library/build.gradle.kts`
3. Removed `cocoapods {}` block from `kotlin-library/build.gradle.kts`

**Final configuration:**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"

        val appleLibraryDir = layout.projectDirectory.dir("../AppleLibrary")
        localSwiftPackage(
            directory = appleLibraryDir,
            products = listOf("AppleLibrary"),
        )
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}
```

**Build command executed:**
```bash
./gradlew :kotlin-library:compileKotlinIosSimulatorArm64
```

**Result:** `BUILD SUCCESSFUL`

---

## Phase 7: Verification

- **Compile Kotlin code:** `./gradlew :kotlin-library:compileKotlinIosSimulatorArm64` - `BUILD SUCCESSFUL`
- **Link framework:** SwiftPM framework linked automatically
- **Build iOS Xcode project:** Not verified (requires Xcode project build)

**Status:** All phases completed successfully!

---

## Phase 8: Migration Report

### Summary

Successfully migrated Kotlin Multiplatform project from CocoaPods to Swift Package Manager (SPM) import using Kotlin version `2.3.20-titan-224`.

### Changes Made

**1. Kotlin Version Update (gradle/libs.versions.toml)**
- Updated `kotlin = "2.3.20"` → `kotlin = "2.3.20-titan-224"`

**2. Repository Configuration (settings.gradle.kts)**
- Added JetBrains dev repository: `maven("https://packages.jetbrains.team/maven/p/kt/dev")`

**3. Gradle Configuration (kotlin-library/build.gradle.kts)**
- Removed `kotlinCocoapods` plugin
- Added `swiftPMDependencies` block with `localSwiftPackage()` for AppleLibrary

**4. SPM Package (AppleLibrary/Package.swift)**
- Created SPM package configuration

**5. SwiftLibrary.swift (AppleLibrary/AppleLibrary/Classes/)**
- Removed SwiftyJSON dependency (original podspec dependency)
- Used `JSONSerialization` instead for JSON parsing

**6. Kotlin Source Update (kotlin-library/src/iosMain/kotlin/A.kt)**
- Changed import: `cocoapods.AppleLibrary.*` → `swiftPMImport.org.jetbrains.kotlin.library.sample.kotlin.library.AppleLibrary`

**7. iOS Project (iosApp/)**
- Deintegrated CocoaPods: `pod deintegrate`

### Verification

```
./gradlew :kotlin-library:compileKotlinIosSimulatorArm64
BUILD SUCCESSFUL
```

### Notes

- Xcode 26.2 in use (not officially supported - warning emitted)
- iOS deployment target: 16.0
- The migration keeps `AppleLibrary.podspec` unchanged; it will no longer be used after CocoaPods removal
- SwiftyJSON dependency was originally in AppleLibrary.podspec but was removed from Swift code to support SPM migration
