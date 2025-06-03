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

        /**
         * Example of usage local Swift CocoaPods library.
         */
        pod("AppleLibrary") {
            version = "0.1.0"
            source = path(project.file("../AppleLibrary"))
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        framework {
            baseName = "KotlinLibrary"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}