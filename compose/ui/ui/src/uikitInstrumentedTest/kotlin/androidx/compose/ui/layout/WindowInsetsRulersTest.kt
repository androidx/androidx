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

package androidx.compose.ui.layout

import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable.PlacementScope
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.DisplayCutout
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toPlatformInsets
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIDevice
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class WindowInsetsRulersTest {

    private var contentSize: IntSize = IntSize.Zero
    private var insetsRect: IntRect? = null
    private val displayCutoutRects = mutableObjectListOf<IntRect?>()

    @Test
    fun testDisplayCutoutsForPortrait() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS < 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskPortrait) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertNotNull(insetsRect)
        assertEquals(1, displayCutoutRects.size)
        assertEquals(
            IntRect(0, 0, screenSizePx.width, hostingViewController.view.safeAreaPlatformInsets.top),
            displayCutoutRects.first()
        )
    }

    @Test
    fun testDisplayCutoutsForLandscapeLeft() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskLandscapeLeft) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertNotNull(insetsRect)
        assertEquals(1, displayCutoutRects.size)
        assertEquals(
            IntRect(screenSizePx.width - hostingViewController.view.safeAreaPlatformInsets.right,0,screenSizePx.width,screenSizePx.height),
            displayCutoutRects.first()
        )
    }

    @Test
    fun testDisplayCutoutsForLandscapeRight() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskLandscapeRight) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertNotNull(insetsRect)
        assertEquals(1, displayCutoutRects.size)
        assertEquals(
            IntRect(0,0,hostingViewController.view.safeAreaPlatformInsets.left,screenSizePx.height),
            displayCutoutRects.first()
        )
    }

    @Test
    fun testDisplayCutoutWindowInsetsRulersBoundedByDisplayCutoutsPortrait() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskPortrait) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertEquals(boundingRectFromDisplayCutouts, insetsRect)
    }

    @Test
    fun testDisplayCutoutWindowInsetsRulersBoundedByDisplayCutoutsLandscapeLeft() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskLandscapeLeft) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertEquals(boundingRectFromDisplayCutouts, insetsRect)
    }

    @Test
    fun testDisplayCutoutWindowInsetsRulersBoundedByDisplayCutoutsLandscapeRight() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskLandscapeRight) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertEquals(boundingRectFromDisplayCutouts, insetsRect)
    }

    @Test
    fun testDisplayCutoutWindowInsetsRulersBoundedByDisplayCutoutsPortraitUpsideDown() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(16)) || UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        ignoreNotes = "Device rotation does not work for iOS <= 16 and iPad"
    ) {
        setContent(interfaceOrientation = UIInterfaceOrientationMaskPortraitUpsideDown) {
            SimpleRulerContent(rulerState = mutableStateOf(DisplayCutout))
        }

        assertEquals(boundingRectFromDisplayCutouts, insetsRect)
    }

    private val boundingRectFromDisplayCutouts: IntRect get() {
        var left = contentSize.width
        var top = contentSize.height
        var right = 0
        var bottom = 0

        for (rect in displayCutoutRects.asList()) {
            assertNotNull(rect)
            left = minOf(left, rect.right)
            top = minOf(top, rect.bottom)
            right = maxOf(right, rect.left)
            bottom = maxOf(bottom, rect.top)
        }

        if (left == contentSize.width) {
            left = 0
        }

        if (top == contentSize.height) {
            top = 0
        }

        if (right == 0) {
            right = contentSize.width
        }

        if (bottom == 0) {
            bottom = contentSize.height
        }

        return IntRect(left, top, right, bottom)
    }

    @Composable
    private fun SimpleRulerContent(rulerState: State<WindowInsetsRulers>) {
        Box(
            Modifier.fillMaxSize()
                .onPlaced {
                    contentSize = it.size
                }
                .rulerToRect(rulerState.value) {
                    insetsRect = it
                    displayCutoutRects.clear()
                    if (rulerState.value == DisplayCutout) {
                        val cutouts = getDisplayCutoutBounds()
                        cutouts.forEach { cutoutRulers ->
                            displayCutoutRects.add(readRulers(cutoutRulers))
                        }
                    }
                }
        )
    }

    private fun PlacementScope.readRulers(rulers: RectRulers): IntRect? {
        val left = rulers.left.current(-1f).roundToInt()
        val top = rulers.top.current(-1f).roundToInt()
        val right = rulers.right.current(-1f).roundToInt()
        val bottom = rulers.bottom.current(-1f).roundToInt()
        if (left == -1 || top == -1 || right == -1 || bottom == -1) {
            return null
        }
        return IntRect(left, top, right, bottom)
    }

    private fun Modifier.rulerToRect(
        ruler: WindowInsetsRulers,
        block: PlacementScope.(IntRect?) -> Unit,
    ): Modifier = layout { m, c ->
        val p = m.measure(c)
        layout(p.width, p.height) {
            p.place(0, 0)
            block(readRulers(ruler.current))
        }
    }

    private val UIKitInstrumentedTest.screenSizePx get() = with(density) {
        IntSize(screenSize.width.roundToPx(), screenSize.height.roundToPx())
    }

    private val UIView.safeAreaPlatformInsets: PlatformInsets get() = safeAreaInsets.toPlatformInsets(density)
}