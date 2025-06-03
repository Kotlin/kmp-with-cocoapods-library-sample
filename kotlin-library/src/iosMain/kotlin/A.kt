import cocoapods.AppleLibrary.*

@Suppress("unused")
fun parseJson(): String {
    val jsonString = """
        {
          "user": {
            "name": "Andrey",
            "age": 36
          }
        }
    """.trimIndent()

    val library = AppleLibrary()
    return library.parseUserJSONWithJsonString(jsonString)
}

@Suppress("unused")
fun sayHello(): String {
    val library = AppleLibrary()
    return library.hello()
}