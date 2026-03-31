plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "org.jetbrains.kotlin.library.sample"
version = "1.0-SNAPSHOT"

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KotlinLibrary"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"

        val appleLibraryDir = layout.projectDirectory.dir("../AppleLibrary")
        localSwiftPackage(
            directory = appleLibraryDir,
            products = listOf("AppleLibrary"),
        )

        // Specify the Xcode project path for integration tasks
        xcodeProjectPathForKmpIJPlugin.set(
            layout.projectDirectory.file("../iosApp/iosApp.xcodeproj")
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

