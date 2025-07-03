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

package androidx.compose.ui.scene

import androidx.compose.testutils.assertPixelColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.utils.captureToImage
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

class ComposeHostingViewControllerTest {
    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testOpaqueBackground() = runBlocking {
        val size = 100.dp
        val appDelegate = makeOpacityTestAppDelegate(size = size, opaque = true)
        val window = appDelegate.window ?: error("Failed to get window")

        val image = window.captureToImage().toPixelMap()

        with(window.density) {
            image.assertPixelColor(Color.White, 0, 0)
            image.assertPixelColor(Color.White, size.roundToPx() - 1, size.roundToPx() - 1)
            image.assertPixelColor(Color.White, size.roundToPx() / 2, size.roundToPx() / 2)
            image.assertPixelColor(Color.White, 0, size.roundToPx() - 1)
            image.assertPixelColor(Color.White, size.roundToPx() - 1, 0)

            image.assertPixelColor(Color.Red, size.roundToPx(), size.roundToPx())
            image.assertPixelColor(Color.Red, 0, size.roundToPx())
            image.assertPixelColor(Color.Red, size.roundToPx(), 0)
        }

        appDelegate.cleanUp()
    }

    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testTransparentBackground() = runBlocking {
        val size = 100.dp
        val appDelegate = makeOpacityTestAppDelegate(size = size, opaque = false)
        val window = appDelegate.window ?: error("Failed to get window")

        val image = window.captureToImage().toPixelMap()

        with(window.density) {
            image.assertPixelColor(Color.Red, 0, 0)
            image.assertPixelColor(Color.Red, size.roundToPx() - 1, size.roundToPx() - 1)
            image.assertPixelColor(Color.Red, size.roundToPx() / 2, size.roundToPx() / 2)
            image.assertPixelColor(Color.Red, 0, size.roundToPx() - 1)
            image.assertPixelColor(Color.Red, size.roundToPx() - 1, 0)

            image.assertPixelColor(Color.Red, size.roundToPx(), size.roundToPx())
            image.assertPixelColor(Color.Red, 0, size.roundToPx())
            image.assertPixelColor(Color.Red, size.roundToPx(), 0)
        }

        appDelegate.cleanUp()
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun makeOpacityTestAppDelegate(size: Dp, opaque: Boolean): MockAppDelegate {
        val appDelegate = MockAppDelegate()
        val contentController = UIViewController().also {
            it.view.backgroundColor = UIColor.redColor
        }
        appDelegate.setUpWindow(contentController)

        val composeController = ComposeUIViewController(configure = {
            this.opaque = opaque
            this.enforceStrictPlistSanityCheck = false
        }) {} as ComposeHostingViewController
        val frame = CGRectMake(0.0, 0.0, size.value.toDouble(), size.value.toDouble())
        composeController.view.setFrame(frame)

        contentController.addChildViewController(composeController)
        contentController.view.addSubview(composeController.view)
        composeController.didMoveToParentViewController(contentController)

        while (composeController.hasInvalidations()) {
            delay(100)
            NSRunLoop.currentRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.1))
        }

        return appDelegate
    }
}
