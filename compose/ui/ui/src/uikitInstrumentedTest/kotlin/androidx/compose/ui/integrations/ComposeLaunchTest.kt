/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.integrations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.utils.forEachPixel
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.window.ComposeUIView
import androidx.compose.ui.window.ComposeUIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIViewController

class ComposeLaunchTest {
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testViewControllerRenderFirstFrameWhenParallelRenderingDisabled() {
        runViewControllerRenderFirstFrameTest(parallelRenderingEnabled = false)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testViewControllerRenderFirstFrameWhenParallelRenderingEnabled() {
        runViewControllerRenderFirstFrameTest(parallelRenderingEnabled = true)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testViewRenderFirstFrameWhenParallelRenderingDisabled() {
        runViewRenderFirstFrameTest(parallelRenderingEnabled = false)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testViewRenderFirstFrameWhenParallelRenderingEnabled() {
        runViewRenderFirstFrameTest(parallelRenderingEnabled = true)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun runViewControllerRenderFirstFrameTest(parallelRenderingEnabled: Boolean) {
        val appDelegate = MockAppDelegate()
        var drawsCount = 0

        val controller = ComposeUIViewController({
            enforceStrictPlistSanityCheck = false
            parallelRendering = parallelRenderingEnabled
        }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Blue).drawWithContent {
                drawsCount++
                this.drawContent()
            })
        }

        appDelegate.setUpWindow(controller)
        val window = appDelegate.window!!
        controller.view.backgroundColor = UIColor.redColor
        window.backgroundColor = UIColor.yellowColor

        val renderer = UIGraphicsImageRenderer(bounds = window.bounds)
        val image = renderer.imageWithActions {
            window.drawViewHierarchyInRect(window.bounds, afterScreenUpdates = true)
        }

        assertEquals(1, drawsCount, "Expected to draw only one frame on startup")

        image.forEachPixel(step = 4) { _, _, color ->
            assertEquals(Color.Blue, color, "Expected to draw blue background")
        }

        appDelegate.cleanUp()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun runViewRenderFirstFrameTest(parallelRenderingEnabled: Boolean) {
        val appDelegate = MockAppDelegate()
        var drawsCount = 0

        val view = ComposeUIView({
            enforceStrictPlistSanityCheck = false
            parallelRendering = parallelRenderingEnabled
        }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Blue).drawWithContent {
                drawsCount++
                this.drawContent()
            })
        }

        val controller = UIViewController()
        appDelegate.setUpWindow(controller)
        val window = appDelegate.window!!
        controller.view.backgroundColor = UIColor.redColor
        window.backgroundColor = UIColor.yellowColor

        controller.view.embedSubview(view)

        val renderer = UIGraphicsImageRenderer(bounds = window.bounds)
        val image = renderer.imageWithActions {
            window.drawViewHierarchyInRect(window.bounds, afterScreenUpdates = true)
        }

        assertEquals(1, drawsCount, "Expected to draw only one frame on startup")

        image.forEachPixel(step = 4) { _, _, color ->
            assertEquals(Color.Blue, color, "Expected to draw blue background")
        }

        appDelegate.cleanUp()
    }
}
