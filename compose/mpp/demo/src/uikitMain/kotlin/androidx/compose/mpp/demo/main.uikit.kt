// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.platform.AccessibilityDebugLogger
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.mpp.demo.bugs.IosBugs
import androidx.compose.mpp.demo.bugs.StartRecompositionCheck
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeApi::class, ExperimentalComposeUiApi::class)
fun main(vararg args: String) {
    androidx.compose.ui.util.enableTraceOSLog()

    val arg = args.firstOrNull() ?: ""
    defaultUIKitMain("ComposeDemo", ComposeUIViewController(configure = {
        accessibilitySyncOptions = AccessibilitySyncOptions.WhenRequiredByAccessibilityServices(object: AccessibilityDebugLogger {
            override fun log(message: Any?) {
                if (message == null) {
                    println()
                } else {
                    println("[a11y]: $message")
                }
            }
        })

        delegate = object : ComposeUIViewControllerDelegate {
            override val preferredStatusBarStyle: UIStatusBarStyle?
                get() = preferredStatusBarStyleValue

            override val prefersStatusBarHidden: Boolean?
                get() = prefersStatusBarHiddenValue

            override val preferredStatysBarAnimation: UIStatusBarAnimation?
                get() = preferredStatysBarAnimationValue
        }
    }) {
        IosDemo(arg)
    })
}

@Composable
fun IosDemo(arg: String, makeHostingController: ((Int) -> UIViewController)? = null) {
    val app = remember {
        App(
            extraScreens = listOf(
                IosBugs,
                IosSpecificFeatures,
            ) + listOf(makeHostingController).mapNotNull {
                it?.let {
                    SwiftUIInteropExample(it)
                }
            }
        )
    }
    when (arg) {
        "demo=StartRecompositionCheck" ->
            // The issue tested by this demo can be properly reproduced/tested only right after app start
            StartRecompositionCheck.content()
        else -> app.Content()
    }
}
