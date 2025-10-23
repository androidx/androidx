/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.utils.dpRectInWindow
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewController

class InteropInsidePopupsTest {
    @Test
    fun uiKitViewControllerInsideDialog() = runUIKitInstrumentedTestWithInterop { overlay ->
        val controller = UIViewController()
        controller.view.backgroundColor = UIColor.redColor
        setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                UIKitViewController(
                    factory = { controller },
                    modifier = Modifier.fillMaxSize(),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(safeDrawingRect, controller.view.dpRectInWindow())
        assertNotNull(controller.view.window)
    }

    @Test
    fun uiKitViewControllerInsidePopup() = runUIKitInstrumentedTestWithInterop { overlay ->
        val controller = UIViewController()
        controller.view.backgroundColor = UIColor.redColor

        setContent {
            Popup(onDismissRequest = {}) {
                UIKitViewController(
                    factory = { controller },
                    modifier = Modifier.fillMaxSize(),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(safeDrawingRect, controller.view.dpRectInWindow())
        assertNotNull(controller.view.window)
    }

    @Test
    fun uiKitViewInsideDialog() = runUIKitInstrumentedTestWithInterop { overlay ->
        val view = UIView()
        view.backgroundColor = UIColor.redColor
        setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                UIKitView(
                    factory = { view },
                    modifier = Modifier.fillMaxSize(),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(safeDrawingRect, view.dpRectInWindow())
        assertNotNull(view.window)
    }

    @Test
    fun uiKitViewInsidePopup() = runUIKitInstrumentedTestWithInterop { overlay ->
        val view = UIView()
        view.backgroundColor = UIColor.redColor

        setContent {
            Popup(onDismissRequest = {}) {
                UIKitView(
                    factory = { view },
                    modifier = Modifier.fillMaxSize(),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(safeDrawingRect, view.dpRectInWindow())
        assertNotNull(view.window)
    }
}
