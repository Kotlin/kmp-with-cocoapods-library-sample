import Foundation
import SwiftyJSON

@objc
public class AppleLibrary: NSObject {
    @objc
    public func hello() -> String {
        return "Hello from Swift library!"
    }

    @objc
    public func parseUserJSON(jsonString: String) -> String {
        // Convert the JSON string to Data
        guard let data = jsonString.data(using: .utf8) else {
            return "Invalid JSON string"
        }

        // Parse using SwiftyJSON
        let json = JSON(data)

        // Access fields safely
        let name = json["user"]["name"].stringValue
        let age = json["user"]["age"].intValue

        return "Name: \(name), Age: \(age)"
    }
}
