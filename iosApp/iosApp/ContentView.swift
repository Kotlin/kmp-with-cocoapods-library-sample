import SwiftUI
import KotlinLibrary

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)

            // Call the Kotlin function and show result
            Text(AKt.parseJson())
                .padding()
        }
    }
}

#Preview {
    ContentView()
}
