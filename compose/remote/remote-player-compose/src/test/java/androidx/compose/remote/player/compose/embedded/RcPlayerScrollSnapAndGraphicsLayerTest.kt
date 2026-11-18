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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.remoteComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w200dp-h200dp-xhdpi")
class RcPlayerScrollSnapAndGraphicsLayerTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun graphicsLayer_dependingTransitivelyOnScrollAndHeight_updatesWithoutAnimationLag() {
        rule.mainClock.autoAdvance = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizePx = 120f * density

        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(sizePx, sizePx)),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(120.dp).background(Color.Black).testTag("player")) {
                    content()
                }
            },
        ) {
            val scrollState = rememberRemoteScrollState(notches = 0)
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.TopCenter,
            ) {
                val hostHeight = max(remoteComponent().height, 1f.rf)
                val progress = clamp(scrollState.positionState / hostHeight, 0f.rf, 1f.rf)

                // Red indicator whose translationY and scaleX derive transitively from
                // scrollState.positionState and remoteComponent().height.
                RemoteBox(
                    modifier =
                        RemoteModifier.width(80.rdp)
                            .height(20.rdp)
                            .graphicsLayer(
                                scaleX = 1f.rf - progress * 0.5f.rf,
                                translationY = scrollState.positionState * 0.5f.rf,
                                transformOriginX = 0.5f.rf,
                                transformOriginY = 0f.rf,
                            )
                            .background(Color.Red.rc)
                )

                // Transparent scroll track on top so drag drives scrollState.positionState
                // directly.
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth().height(120.rdp).verticalScroll(scrollState)
                ) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(360.rdp))
                }
            }
        }

        val initialBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        fun redCenterYAndWidth(bmp: Bitmap): Pair<Int, Int> {
            val centerX = bmp.width / 2
            var sumY = 0
            var count = 0
            for (y in 0 until bmp.height) {
                val px = bmp.getPixel(centerX, y)
                if (AndroidColor.red(px) > 200 && AndroidColor.blue(px) < 40) {
                    sumY += y
                    count++
                }
            }
            assertThat(count).isGreaterThan(0)
            val centerY = sumY / count
            var width = 0
            for (x in 0 until bmp.width) {
                val px = bmp.getPixel(x, centerY)
                if (AndroidColor.red(px) > 200 && AndroidColor.blue(px) < 40) {
                    width++
                }
            }
            assertThat(width).isGreaterThan(0)
            return centerY to width
        }

        val (startCenterY, startWidth) = redCenterYAndWidth(initialBitmap)
        assertThat(startCenterY).isLessThan((25f * density).toInt())
        assertThat(startWidth).isAtLeast((75f * density).toInt())

        // Drag upward by ~120px across just two frames (32ms << 300ms animation tween).
        rule.onNodeWithTag("player").performTouchInput {
            down(center)
            moveBy(Offset(0f, -40f))
            moveBy(Offset(0f, -80f))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val dragFrameBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val (draggedCenterY, draggedWidth) = redCenterYAndWidth(dragFrameBitmap)
        // Expected shift is ~0.5 * scrollDelta (~40px on xhdpi = 20dp+), and scaleX shrinks
        // proportionally to scroll / remoteComponent().height.
        assertThat(draggedCenterY - startCenterY).isGreaterThan((12f * density).toInt())
        assertThat(draggedWidth).isLessThan(startWidth - (10f * density).toInt())

        // Continue dragging without releasing and verify immediate further update on next frame.
        rule.onNodeWithTag("player").performTouchInput {
            moveBy(Offset(0f, -80f))
        }
        rule.mainClock.advanceTimeByFrame()

        val secondDragBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val (secondCenterY, secondWidth) = redCenterYAndWidth(secondDragBitmap)
        assertThat(secondCenterY).isGreaterThan(draggedCenterY + (10f * density).toInt())
        assertThat(secondWidth).isLessThan(draggedWidth - (6f * density).toInt())

        rule.onNodeWithTag("player").performTouchInput { up() }
    }

    @Test
    fun verticalScroll_withNotches_snapsForwardBackwardAndSettlesFlingOnExactNotch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val pageHeightDp = 100
        val sizePx = pageHeightDp * density
        val pageColors =
            listOf(
                Color.Red,
                Color.Green,
                Color.Blue,
                Color.Yellow,
                Color.Magenta,
                Color.Cyan,
            )

        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(sizePx, sizePx)),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(pageHeightDp.dp).testTag("pager")) { content() }
            },
        ) {
            val scrollState = rememberRemoteScrollState(notches = pageColors.size - 1)
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .height(pageHeightDp.rdp)
                        .verticalScroll(scrollState)
            ) {
                for (color in pageColors) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth()
                                .height(pageHeightDp.rdp)
                                .background(color.rc)
                    )
                }
            }
        }

        fun detectSettledPageIndex(): Int {
            val bmp = rule.onNodeWithTag("pager").captureToImage().asAndroidBitmap()
            val x = bmp.width / 2
            val topPx = bmp.getPixel(x, (10f * density).toInt())
            val midPx = bmp.getPixel(x, bmp.height / 2)
            val bottomPx = bmp.getPixel(x, bmp.height - (10f * density).toInt())

            // On an exact notch, top, middle, and bottom strips all belong to the exact same page.
            fun classify(px: Int): Int {
                val r = AndroidColor.red(px)
                val g = AndroidColor.green(px)
                val b = AndroidColor.blue(px)
                return when {
                    r > 200 && g < 40 && b < 40 -> 0 // Red
                    r < 40 && g > 200 && b < 40 -> 1 // Green
                    r < 40 && g < 40 && b > 200 -> 2 // Blue
                    r > 200 && g > 200 && b < 40 -> 3 // Yellow
                    r > 200 && g < 40 && b > 200 -> 4 // Magenta
                    r < 40 && g > 200 && b > 200 -> 5 // Cyan
                    else -> -1
                }
            }

            val topPage = classify(topPx)
            val midPage = classify(midPx)
            val bottomPage = classify(bottomPx)
            assertThat(topPage).isAtLeast(0)
            assertThat(topPage).isEqualTo(midPage)
            assertThat(bottomPage).isEqualTo(midPage)
            return midPage
        }

        rule.waitForIdle()
        assertThat(detectSettledPageIndex()).isEqualTo(0)

        // Gentle upward drag past half-page snaps forward to page >= 1.
        rule.onNodeWithTag("pager").performTouchInput {
            swipeUp(startY = centerY + 60f, endY = centerY - 60f, durationMillis = 120)
        }
        rule.waitForIdle()
        val afterForward = detectSettledPageIndex()
        assertThat(afterForward).isGreaterThan(0)

        // Fast upward fling decays smoothly and settles squarely on an exact notch.
        rule.onNodeWithTag("pager").performTouchInput {
            swipeUp(startY = bottom - 10f, endY = top + 10f, durationMillis = 40)
        }
        rule.waitForIdle()
        val afterFling = detectSettledPageIndex()
        assertThat(afterFling).isAtLeast(afterForward)

        // Gentle downward flick snaps backward cleanly on an exact notch.
        rule.onNodeWithTag("pager").performTouchInput {
            swipeDown(startY = centerY - 60f, endY = centerY + 60f, durationMillis = 120)
        }
        rule.waitForIdle()
        val afterBackward = detectSettledPageIndex()
        assertThat(afterBackward).isLessThan(afterFling)
    }
}
