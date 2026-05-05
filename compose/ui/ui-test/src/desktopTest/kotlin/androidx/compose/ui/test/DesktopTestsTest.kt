/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Tests desktop-specific Test APIs.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopTestsTest {

    @Test
    fun testNodeInDialogWindow() = runComposeUiTest {
        var show by mutableStateOf(true)
        setContent {
            if (show) {
                DialogWindow(
                    onCloseRequest = {},
                ) {
                    Text(
                        text = "Text",
                        modifier = Modifier.testTag("tag")
                    )
                }
            }
        }

        onNodeWithTag("tag").assertExists()

        show = false
        onNodeWithTag("tag").assertDoesNotExist()
    }

    @Test
    fun testNodeInWindow() = runComposeUiTest {
        var show by mutableStateOf(true)
        setContent {
            if (show) {
                Window(
                    onCloseRequest = {},
                ) {
                    Text(
                        text = "Text",
                        modifier = Modifier.testTag("tag")
                    )
                }
            }
        }

        onNodeWithTag("tag").assertExists()

        show = false
        onNodeWithTag("tag").assertDoesNotExist()
    }

    @Test
    fun testIsDialogOnDialogWindow() = runComposeUiTest {
        setContent {
            DialogWindow(
                onCloseRequest = {},
            ) {
                Text(
                    text = "Text",
                    modifier = Modifier.testTag("tag")
                )

            }
        }

        onNodeWithTag("tag").assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun testDrawSquare() = runDesktopComposeUiTest(10, 10) {
        setContent {
            Canvas(Modifier.size(10.dp)) {
                drawRect(Color.Blue, size = Size(10f, 10f))
            }
        }
        Image.makeFromBitmap(captureToImage().asSkiaBitmap()).use { img: Image ->
            val actualPng = Files.createTempFile("test-draw-square", ".png")
            val actualImage = img.encodeToData(EncodedImageFormat.PNG)
                ?: error("Could not encode image as png")
            actualPng.writeBytes(actualImage.bytes)

            val expectedPng =
                ClassLoader.getSystemResource("androidx/compose/ui/test/draw-square.png")

            assert(actualPng.readBytes().contentEquals(expectedPng.readBytes())) {
                "The actual image '$actualPng' does not match the expected image '$expectedPng'"
            }
        }
    }

    @Test
    fun testIdlingResource() = runDesktopComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            Text(
                text = text,
                modifier = Modifier.testTag("text")
            )
        }

        var isIdle = true
        val idlingResource = object : IdlingResource {
            override val isIdleNow: Boolean
                get() = isIdle
        }

        fun test(expectedValue: String) {
            text = "first"
            isIdle = false
            val job = CoroutineScope(Dispatchers.Default).launch {
                delay(1.seconds)
                text = "second"
                isIdle = true
            }
            try {
                onNodeWithTag("text").assertTextEquals(expectedValue)
            } finally {
                job.cancel()
            }
        }

        // With the idling resource registered, we expect the test to wait until the second value
        // has been set.
        registerIdlingResource(idlingResource)
        test(expectedValue = "second")

        // Without the idling resource registered, we expect the test to see the first value
        unregisterIdlingResource(idlingResource)
        test(expectedValue = "first")
    }
}