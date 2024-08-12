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

package androidx.compose.foundation.lazy.grid

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyGridItemDisappearanceAnimationTest {

    @get:Rule val rule = createComposeRule()

    // the numbers should be divisible by 8 to avoid the rounding issues as we run 4 or 8 frames
    // of the animation.
    private val itemSize: Int = 4
    private var itemSizeDp: Dp = Dp.Infinity
    private val crossAxisSize: Int = 2
    private var crossAxisSizeDp: Dp = Dp.Infinity
    private val containerSize: Float = itemSize * 2f
    private var containerSizeDp: Dp = Dp.Infinity
    private lateinit var state: LazyGridState

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
    fun oneItemRemoved() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertPixels(mainAxisSize = itemSize) { _, _ ->
                Color.Black.copy(alpha = 1f - fraction)
            }
        }
    }

    @Test
    fun threeExistTwoRemoved() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 3) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Black
                    in itemSize until itemSize * 2 -> Color.Red.copy(alpha = 1f - fraction)
                    else -> Color.Green.copy(alpha = 1f - fraction)
                }
            }
        }
    }

    // todo copy with content padding and horizontal
    @Test
    fun threeExistTwoRemoved_reverseLayout() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 3, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Green.copy(alpha = 1f - fraction)
                    in itemSize until itemSize * 2 -> Color.Red.copy(alpha = 1f - fraction)
                    else -> Color.Black
                }
            }
        }
    }

    @Test
    fun oneRemoved_reverseLayout_contentPadding() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyGrid(
                containerSize = itemSizeDp * 3,
                reverseLayout = true,
                contentPadding = PaddingValues(bottom = itemSizeDp)
            ) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Red.copy(alpha = 1f - fraction)
                    in itemSize until itemSize * 2 -> Color.Black
                    else -> Color.Transparent
                }
            }
        }
    }

    @Test
    fun onlyItemWithSpecsIsAnimating() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, disappearanceSpec = if (it == Color.Red) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 2) { _, offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Transparent
                    else -> Color.Red.copy(alpha = 1f - fraction)
                }
            }
        }
    }

    @Test
    fun itemRemovedOutsideOfViewportIsNotAnimated() {
        var list by
            mutableStateOf(listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread {
            // Blue is removed before Green, both are outside the bounds
            list = listOf(Color.Black, Color.Red, Color.Green, Color.Yellow)
        }

        rule.runOnIdle {
            runBlocking {
                // scroll 0.5 items so we now see half of Black, Red and half of Green
                state.scrollBy(itemSize * 0.5f)
            }
        }

        onAnimationFrame {
            assertPixels(itemSize * 2) { _, offset ->
                when (offset) {
                    in 0 until itemSize / 2 -> Color.Black
                    in itemSize / 2 until itemSize * 3 / 2 -> Color.Red
                    else -> Color.Green
                }
            }
        }
    }

    @Test
    fun itemMovingAwayToOutOfViewPort_shouldNotTriggerPrefetching() {
        var list by
            mutableStateOf(
                listOf(
                    Color.Black,
                    Color.Red,
                    Color.Blue,
                    Color.Green,
                    Color.Yellow,
                    Color.DarkGray
                )
            )
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp * 2, startIndex = 2) {
                items(list, span = { GridItemSpan(1) }, key = { it.toArgb() }) {
                    Item(it, placementSpec = tween(Duration.toInt(), easing = LinearEasing))
                }
            }
        }

        rule.runOnUiThread {
            // Blue is removed before Green, both are outside the bounds
            list =
                listOf(
                    Color.Yellow,
                    Color.Green,
                    Color.Red,
                    Color.DarkGray,
                    Color.Black,
                    Color.Blue
                )
        }

        val result = runCatching {
            rule.runOnIdle {
                runBlocking {
                    // scroll 0.5 items so we now see half of Black, Red and half of Green
                    state.scrollBy(itemSize * -1.0f)
                }
            }
        }

        assertThat(result.isFailure).isFalse()
    }

    @Test
    fun itemsBeingRemovedAreAffectingTheContainerSizeForTheDurationOfAnimation() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyGrid(containerSize = null) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.onNodeWithTag(ContainerTag).assertHeightIsEqualTo(itemSizeDp * 2)

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            val heightDp = rule.onNodeWithTag(ContainerTag).getBoundsInRoot().height
            val heightPx = with(rule.density) { heightDp.roundToPx() }
            Truth.assertWithMessage("Height on fraction=$fraction")
                .that(heightPx)
                .isEqualTo(if (fraction < 1f) itemSize * 2 else itemSize)

            if (fraction < 1f) {
                assertPixels(itemSize * 2) { _, offset ->
                    when (offset) {
                        in 0 until itemSize -> Color.Black
                        else -> Color.Red.copy(1f - fraction)
                    }
                }
            } else {
                assertPixels(itemSize) { _, _ -> Color.Black }
            }
        }
    }

    @Test
    fun itemsBeingRemovedAreAffectingTheContainerSizeForTheDurationOfAnimation_reverseLayout() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyGrid(containerSize = null, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.onNodeWithTag(ContainerTag).assertHeightIsEqualTo(itemSizeDp * 2)

        assertPixels(itemSize * 2) { _, offset ->
            when (offset) {
                in 0 until itemSize -> Color.Red
                else -> Color.Black
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            val heightDp = rule.onNodeWithTag(ContainerTag).getBoundsInRoot().height
            val heightPx = with(rule.density) { heightDp.roundToPx() }
            Truth.assertWithMessage("Height on fraction=$fraction")
                .that(heightPx)
                .isEqualTo(if (fraction < 1f) itemSize * 2 else itemSize)

            if (fraction < 1f) {
                assertPixels(itemSize * 2) { _, offset ->
                    when (offset) {
                        in 0 until itemSize -> Color.Red.copy(1f - fraction)
                        else -> Color.Black
                    }
                }
            } else {
                assertPixels(itemSize) { _, _ -> Color.Black }
            }
        }
    }

    @Test
    fun reAddItemBeingAnimated_withoutAppearanceAnimation() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = 1f - fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = listOf(Color.Black) }
                }
                assertPixels(itemSize) { _, _ -> Color.Black }
            }
        }
    }

    @Test
    fun reAddItemBeingAnimated_withAppearanceAnimation() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) {
                items(list, key = { it.toArgb() }) {
                    Item(it, appearanceSpec = HalfDurationAnimSpec)
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = 1f - fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = listOf(Color.Black) }
                }
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = fraction) }
            }
        }
    }

    @Test
    fun removeItemBeingAnimatedForAppearance() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyGrid(containerSize = itemSizeDp) {
                items(list, key = { it.toArgb() }) {
                    Item(it, appearanceSpec = AnimSpec, disappearanceSpec = HalfDurationAnimSpec)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = emptyList() }
                }
                assertPixels(itemSize) { _, _ -> Color.Black.copy(alpha = 1f - fraction) }
            }
        }
    }

    @Test
    fun wholeLineRemoved() {
        var list by mutableStateOf(listOf(Color.Black, Color.Green))
        rule.setContent {
            LazyGrid(cells = 2, containerSize = itemSizeDp, crossAxisSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertPixels(mainAxisSize = itemSize, crossAxisSize = itemSize * 2) { x, _ ->
                if (x < itemSize) {
                    Color.Black.copy(alpha = 1f - fraction)
                } else {
                    Color.Green.copy(alpha = 1f - fraction)
                }
            }
        }
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
                Truth.assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            }
        }
    }

    @Composable
    private fun LazyGrid(
        cells: Int = 1,
        containerSize: Dp? = containerSizeDp,
        startIndex: Int = 0,
        crossAxisSize: Dp = crossAxisSizeDp,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        content: LazyGridScope.() -> Unit
    ) {
        state = rememberLazyGridState(startIndex)

        LazyVerticalGrid(
            GridCells.Fixed(cells),
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
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            content = content
        )
    }

    @Composable
    private fun LazyGridItemScope.Item(
        color: Color,
        size: Dp = itemSizeDp,
        crossAxisSize: Dp = crossAxisSizeDp,
        disappearanceSpec: FiniteAnimationSpec<Float>? = AnimSpec,
        appearanceSpec: FiniteAnimationSpec<Float>? = null,
        placementSpec: FiniteAnimationSpec<IntOffset>? = null
    ) {
        Box(
            Modifier.animateItem(
                    fadeInSpec = appearanceSpec,
                    placementSpec = placementSpec,
                    fadeOutSpec = disappearanceSpec
                )
                .background(color)
                .requiredHeight(size)
                .requiredWidth(crossAxisSize)
        )
    }
}

private val FrameDuration = 16L
private val Duration = 64L // 4 frames, so we get 0f, 0.25f, 0.5f, 0.75f and 1f fractions
private val AnimSpec = tween<Float>(Duration.toInt(), easing = LinearEasing)
private val HalfDurationAnimSpec = tween<Float>(Duration.toInt() / 2, easing = LinearEasing)
private val ContainerTag = "container"
