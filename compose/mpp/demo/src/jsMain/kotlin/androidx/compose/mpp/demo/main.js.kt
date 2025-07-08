package androidx.compose.mpp.demo

import androidx.compose.mpp.demo.bugs.BugsScreen
import androidx.compose.mpp.demo.interops.HtmlInteropDemos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.rememberNavController
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalBrowserHistoryApi
fun main() {
    onWasmReady {
        ComposeViewport(viewportContainerId = "composeApplication") {
            val navController = rememberNavController()

            val app = remember {
                App(
                    extraScreens = listOf(
                        BugsScreen,
                        Screen.Example("Web Clipboard API example") {
                            WebClipboardDemo()
                        },
                        HtmlInteropDemos
                    )
                )
            }
            app.Content(navController)

            LaunchedEffect(Unit) {
                navController.bindToBrowserNavigation()
            }
        }

        setupBackingTextAreaDebugHints()
    }
}
