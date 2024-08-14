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

package androidx.compose.foundation.lazy.staggeredgrid

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyStaggeredGridItemAppearanceAnimationTest {

    @get:Rule val rule = createComposeRule()

    private val itemSize: Int = 4
    private var itemSizeDp: Dp = Dp.Infinity
    private val crossAxisSize: Int = 2
    private var crossAxisSizeDp: Dp = Dp.Infinity
    private val containerSize: Float = itemSize * 2f
    private var containerSizeDp: Dp = Dp.Infinity
    private lateinit var state: LazyStaggeredGridState

    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        with(rule.density) {
            itemSizeDp = itemSize.toDp()
            crossAxisSizeDp = crossAxisSize.toDp()
            containerSizeDp = containerSize.toDp()
        }
    }

    @Test
    fun oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(mainAxisSize = itemSize) { _, _ -> Color.Black.copy(alpha = fraction) }
        }
    }

    @Test
    fun noAnimationForInitialList() {
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) {
                items(listOf(Color.Black), key = { it.toArgb() }) { Item(it) }
            }
        }

        assertPixels(itemSize) { _, _ -> Color.Black }
    }

    @Test
    fun oneExistTwoAdded() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 3) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Green) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Black
                    in itemSize until itemSize * 2 -> Color.Red.copy(alpha = fraction)
                    else -> Color.Green.copy(alpha = fraction)
                }
            }
        }
    }

    @Test
    fun onlyItemWithSpecsIsAnimating() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, animSpec = if (it == Color.Red) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 2) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Black
                    else -> Color.Red.copy(alpha = fraction)
                }
            }
        }
    }

    @Test
    fun itemAddedOutsideOfViewportIsNotAnimated() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread {
            // Blue is added before Green, both are outside the bounds
            list = listOf(Color.Black, Color.Red, Color.Blue, Color.Green)
        }

        rule.runOnIdle {
            runBlocking {
                // scroll 1.5 items so we now see half of Red, Blue and half of Green
                state.scrollBy(itemSize * 1.5f)
            }
        }

        onAnimationFrame {
            assertPixels(itemSize * 2) { _, offset ->
                when (offset) {
                    in 0 until itemSize / 2 -> Color.Red
                    in itemSize / 2 until itemSize * 3 / 2 -> Color.Blue
                    else -> Color.Green
                }
            }
        }
    }

    @Test
    fun animatedItemChangesTheContainerSize() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyGrid(containerSize = null) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.onNodeWithTag(ContainerTag).assertHeightIsEqualTo(itemSizeDp)

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { rule.onNodeWithTag(ContainerTag).assertHeightIsEqualTo(itemSizeDp * 2) }
    }

    @Test
    fun removeItemBeingAnimated() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = emptyList() }
                }
                assertPixels(itemSize) { _, _ -> Color.Transparent }
            }
        }
    }

    @Test
    fun scrollAwayFromAnimatedItem() {
        var list by mutableStateOf(listOf(Color.Black, Color.Green, Color.Blue, Color.Yellow))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread {
            // item at position 1 is new
            list = listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow)
        }

        onAnimationFrame { fraction ->
            if (fraction < 0.35f) {
                assertPixels(itemSize * 2) { _, offset ->
                    when (offset) {
                        in 0 until itemSize -> Color.Black
                        else -> Color.Red.copy(alpha = fraction)
                    }
                }
            } else if (fraction.isCloseTo(0.5f)) {
                rule.runOnUiThread {
                    runBlocking { state.scrollBy(itemSize * 2f) }
                    runBlocking { state.scrollBy(itemSize * 0.5f) }
                }
                assertPixels(itemSize * 2) { _, offset ->
                    // red item is not displayed anywhere
                    when (offset) {
                        in 0 until itemSize / 2 -> Color.Green
                        in itemSize / 2 until itemSize * 3 / 2 -> Color.Blue
                        else -> Color.Yellow
                    }
                }
            } else {
                if (fraction.isCloseTo(0.75f)) {
                    rule.runOnUiThread { runBlocking { state.scrollBy(-itemSize * 1.5f) } }
                }
                assertPixels(itemSize * 2) { _, offset ->
                    // red item is not displayed anywhere
                    when (offset) {
                        // the animation should be canceled so the red item has no alpha
                        in 0 until itemSize -> Color.Red
                        else -> Color.Green
                    }
                }
            }
        }
    }

    @Test
    fun theWholeLineIsAdded() {
        var list by mutableStateOf(listOf(Color.Green, Color.Red))
        val containerSize = itemSize * 2
        val containerSizeDp = itemSizeDp * 2
        rule.setContent {
            LazyGrid(cells = 2, containerSize = containerSizeDp, crossAxisSize = containerSizeDp) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Green, Color.Red, Color.Blue, Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(containerSize, containerSize) { x, y ->
                // red item is not displayed anywhere
                when {
                    // the animation should be canceled so the red item has no alpha
                    x < itemSize && y < itemSize -> Color.Green
                    x >= itemSize && y < itemSize -> Color.Red
                    x < itemSize && y >= itemSize -> Color.Blue.copy(alpha = fraction)
                    else -> Color.Black.copy(alpha = fraction)
                }
            }
        }
    }

    @Test
    fun snapToPosition_noAnimation() {
        val items = List(200) { Color.Black }
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) {
                items(items, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(200) } }

        assertPixels(itemSize) { _, _ -> Color.Black }
    }

    private fun assertPixels(
        mainAxisSize: Int,
        crossAxisSize: Int = this.crossAxisSize,
        expectedColorProvider: (x: Int, y: Int) -> Color?
    ) {
        rule.onNodeWithTag(ContainerTag).captureToImage().assertPixels(
            IntSize(crossAxisSize, mainAxisSize)
        ) {
            expectedColorProvider(it.x, it.y)?.compositeOver(Color.White)
        }
    }

    private fun onAnimationFrame(duration: Long = Duration, onFrame: (fraction: Float) -> Unit) {
        require(duration.mod(FrameDuration) == 0L)
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step FrameDuration) {
            val fraction = i / duration.toFloat()
            onFrame(fraction)
            if (i < duration) {
                rule.mainClock.advanceTimeBy(FrameDuration)
                expectedTime += FrameDuration
                assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            }
        }
    }

    @Composable
    private fun LazyGrid(
        cells: Int = 1,
        containerSize: Dp? = containerSizeDp,
        startIndex: Int = 0,
        crossAxisSize: Dp = crossAxisSizeDp,
        content: LazyStaggeredGridScope.() -> Unit
    ) {
        state = rememberLazyStaggeredGridState(startIndex)

        LazyVerticalStaggeredGrid(
            StaggeredGridCells.Fixed(cells),
            state = state,
            modifier =
                Modifier.then(
                        if (containerSize != null) {
                            Modifier.requiredHeight(containerSize)
                        } else {
                            Modifier
                        }
                    )
                    .background(Color.White)
                    .then(
                        if (crossAxisSize != Dp.Unspecified) {
                            Modifier.requiredWidth(crossAxisSize)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .testTag(ContainerTag),
            content = content
        )
    }

    @Composable
    private fun LazyStaggeredGridItemScope.Item(
        color: Color,
        size: Dp = itemSizeDp,
        crossAxisSize: Dp = crossAxisSizeDp,
        animSpec: FiniteAnimationSpec<Float>? = AnimSpec
    ) {
        Box(
            Modifier.animateItem(fadeInSpec = animSpec, placementSpec = null, fadeOutSpec = null)
                .background(color)
                .requiredHeight(size)
                .requiredWidth(crossAxisSize)
        )
    }
}

private val FrameDuration = 16L
private val Duration = 64L // 4 frames, so we get 0f, 0.25f, 0.5f, 0.75f and 1f fractions
private val AnimSpec = tween<Float>(Duration.toInt(), easing = LinearEasing)
private val ContainerTag = "container"

internal fun Float.isCloseTo(expected: Float) = abs(this - expected) < 0.01f
