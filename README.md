[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# kotlin-with-cocoapods-library-sample
This project demonstrates how to use a CocoaPods dependency in a Kotlin Multiplatform (KMP) project, including how to bridge a pure Swift library into Kotlin code using a custom CocoaPods wrapper. It also includes an iOS app to showcase the integration in action.

## Project contains several directories:
*   `kotlin-library` – A Kotlin Multiplatform module with a `cocoapods` section that includes a dependency on the local Swift pod `AppleLibrary`.
*   `AppleLibrary` – A Swift CocoaPods library created with `pod lib create AppleLibrary`. It wraps the pure Swift library `SwiftyJSON`, acting as a bridge to expose it to Kotlin code.
*   `iosApp` – A sample iOS app that demonstrates how the shared Kotlin code and the bridged Swift functionality can be used together.

Importing of this project to IntelliJ IDEA provides features like code-completion from these dependencies, 
highlighting and others
