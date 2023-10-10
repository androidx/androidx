// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import NativeModalWithNaviationExample
import androidx.compose.runtime.*
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController
import bugs.IosBugs
import bugs.StartRecompositionCheck


fun main(vararg args: String) {
    val arg = args.firstOrNull() ?: ""
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        IosDemo(arg)
    })
}

@Composable
fun IosDemo(arg: String) {
    val app = remember {
        App(
            extraScreens = listOf(
                IosBugs,
                NativeModalWithNaviationExample,
            )
        )
    }
    when (arg) {
        "demo=StartRecompositionCheck" ->
            // The issue tested by this demo can be properly reproduced/tested only right after app start
            StartRecompositionCheck.content()
        else -> app.Content()
    }
}
