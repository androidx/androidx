package androidx.compose.mpp.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        ComposeViewport(viewportContainer = "composeApplication") {
            val app = remember { App() }
            app.Content()
        }
    }
}
