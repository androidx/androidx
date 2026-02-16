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

package androidx.compose.ui.layers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureScreenshot
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.forEachPixel
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class LayersRenderingTest {
    @Test
    fun testLayerContentOnFirstRender() = runUIKitInstrumentedTest {
        var showRed by mutableStateOf(false)
        var showGreen by mutableStateOf(false)
        var onRender = {}
        var frameImage: UIImage? = null

        fun prepareForCaptureNextFrame() {
            frameImage = null
            onRender = {
                dispatch_async(dispatch_get_main_queue()) {
                    frameImage = captureScreenshot()
                }
                onRender = {}
            }
        }

        setContent {
            Box(Modifier.fillMaxSize().background(Color.Blue).drawBehind {
                onRender()
            })
            if (showRed) {
                Popup(
                    onDismissRequest = {},
                    properties = PopupProperties(usePlatformInsets = false)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
            if (showGreen) {
                Popup(
                    onDismissRequest = {},
                    properties = PopupProperties(usePlatformInsets = false)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Green))
                }
            }
        }

        fun assertNextFrameColor(expectedColor: Color) {
            prepareForCaptureNextFrame()
            waitForIdle()

            assertNotNull(frameImage)
            frameImage!!.forEachPixel(step = 4) { _, _, actualColor ->
                assertEquals(
                    expectedColor,
                    actualColor,
                    "Expected to draw $expectedColor background"
                )
            }
        }

        showRed = true
        assertNextFrameColor(Color.Red)

        showRed = false
        assertNextFrameColor(Color.Blue)

        showGreen = true
        assertNextFrameColor(Color.Green)

        showGreen = false
        assertNextFrameColor(Color.Blue)
    }
}
