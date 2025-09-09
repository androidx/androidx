import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        SwiftHelper().getViewController { index in
            let viewController = UIHostingController(rootView: NestedContentView(index: index.intValue))
            return viewController
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct NestedContentView: View {
    let index: Int

    var body: some View {
        Text("Hello from SwiftUI #\(index)")
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

