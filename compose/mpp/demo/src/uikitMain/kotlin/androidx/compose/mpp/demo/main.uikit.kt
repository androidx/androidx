// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import androidx.compose.mpp.demo.bugs.IosBugs
import androidx.compose.mpp.demo.bugs.StartRecompositionCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

/**
 * To run the demo project:
 * - install the latest version of the XCode
 * - in terminal, navigate to the directory "compose/mpp/demo"
 * - run the `./regenerate_xcode_project.sh` command
 * - XCode will open this project automatically
 * - press the Run (Cmd+R) button in the XCode
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(vararg args: String) {
    androidx.compose.ui.util.enableTraceOSLog()

    val arg = args.firstOrNull() ?: ""
    UIKitMain {
        ComposeUIViewController(
            configure = {
                parallelRendering = true
            }
        ) {
            IosDemo(arg)
        }
    }
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
            // The issue tested by this demo can be properly reproduced/tested only right after app
            // start
            StartRecompositionCheck.content()
        else -> app.Content()
    }
}

private lateinit var MakeRootViewController: () -> UIViewController
@OptIn(BetaInteropApi::class)
private fun UIKitMain(makeRootViewController: () -> UIViewController) {
    MakeRootViewController = makeRootViewController
    memScoped {
        val argc = 1
        val argv = arrayOf("ComposeDemo").map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(IOSAppDelegate))
        }
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private class IOSAppDelegate : UIResponder, UIApplicationDelegateProtocol {
    companion object Companion : UIResponderMeta(), UIApplicationDelegateProtocolMeta

    @OptIn(BetaInteropApi::class)
    @OverrideInit
    constructor() : super()

    private var _window: UIWindow? = null
    override fun window() = _window
    override fun setWindow(window: UIWindow?) {
        _window = window
    }

    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?
    ): Boolean {
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window!!.rootViewController = MakeRootViewController()
        window!!.makeKeyAndVisible()
        return true
    }
}
