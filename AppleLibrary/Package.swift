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
            targets: ["AppleLibrary"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/SwiftyJSON/SwiftyJSON.git", from: "5.0.1")
    ],
    targets: [
        .target(
            name: "AppleLibrary",
            dependencies: [
                .product(name: "SwiftyJSON", package: "SwiftyJSON")
            ],
            path: "AppleLibrary/Classes"
        )
    ]
)