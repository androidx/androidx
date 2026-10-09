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

package androidx.compose.foundation.lazy.list

import android.os.Build
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyListItemSlideAnimationTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: LazyListState

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @Test
    fun slideInHorizontally_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, x = interpolate(-ItemWidth, 0, fraction)),
            )
        }
    }

    @Test
    fun slideInVertically_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInVertically(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black, y = interpolate(-ItemHeight, 0, fraction)),
            )
        }
    }

    @Test
    fun slideInHorizontally_defaultOffsetIsBasedOnEachItemSize() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        width = if (it == Color.Red) ItemWidth / 2 else ItemWidth,
                        enter = slideInHorizontally(SlideSpec),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black, x = interpolate(-ItemWidth / 2, 0, fraction)),
                ExpectedItem(
                    Color.Red,
                    x = interpolate(-ItemWidth / 4, 0, fraction),
                    y = ItemHeight,
                    width = ItemWidth / 2,
                ),
            )
        }
    }

    @Test
    fun noAnimationForInitialItems() {
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(listOf(Color.Black, Color.Red), key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        onAnimationFrame {
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, y = ItemHeight),
            )
        }
    }

    @Test
    fun oneExistsOneAdded_onlyAddedItemSlides() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, x = interpolate(-ItemWidth, 0, fraction), y = ItemHeight),
            )
        }
    }

    @Test
    fun onlyItemWithEnterTransitionSlides() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter =
                            if (it == Color.Red) slideInHorizontally(SlideSpec) { -it } else null,
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, x = interpolate(-ItemWidth, 0, fraction), y = ItemHeight),
            )
        }
    }

    @Test
    fun slideOutHorizontally_oneItemRemoved() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOutHorizontally(SlideSpec) { it })
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, x = interpolate(0, ItemWidth, fraction)),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerHeight = ItemHeight)
    }

    @Test
    fun lastItemRemoved_otherItemsAreNotAffected() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOutHorizontally(SlideSpec) { it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, x = interpolate(0, ItemWidth, fraction), y = ItemHeight),
            )
        }
    }

    @Test
    fun exitTransitionNone_itemIsRemovedImmediately() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) { Item(it, exit = ExitTransition.None) }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        assertItems(containerHeight = ItemHeight)
    }

    @Test
    fun slideOutWithZeroOffset_itemIsRemovedAfterOneFrame() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOut(SlideSpec) { IntOffset.Zero })
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        assertItems(containerHeight = ItemHeight, ExpectedItem(Color.Black))
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerHeight = ItemHeight)
    }

    @Test
    fun slideInCombinedWithFadeIn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = fadeIn(FadeSpec) + slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(
                    Color.Black.copy(alpha = fraction),
                    x = interpolate(-ItemWidth, 0, fraction),
                ),
            )
        }
    }

    @Test
    fun slideInCombinedWithPlacementAnimationOfOtherItems() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInVertically(SlideSpec) { -it },
                        placementSpec = SlideSpec,
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Red, Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Red, y = interpolate(-ItemHeight, 0, fraction)),
                ExpectedItem(Color.Black, y = interpolate(0, ItemHeight, fraction)),
            )
        }
    }

    @Test
    fun reAddItemBeingSlidOut_slidesBackFromCurrentOffsetWithEnterSpec() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInHorizontally(SlideSpec) { it },
                        exit = slideOutHorizontally(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        val interruptedOffset = ItemWidth / 2
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = listOf(Color.Black) }
            val x =
                if (step <= Steps / 2) {
                    interpolate(0, ItemWidth, step / Steps.toFloat())
                } else {
                    interpolate(interruptedOffset, 0, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(containerHeight = ItemHeight, ExpectedItem(Color.Black, x = x))
        }
    }

    @Test
    fun removeItemBeingSlidIn_slidesOutFromCurrentOffsetWithExitSpec() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInHorizontally(SlideSpec) { -it },
                        exit = slideOutHorizontally(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        val interruptedOffset = -ItemWidth / 2
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
            val x =
                if (step <= Steps / 2) {
                    interpolate(-ItemWidth, 0, step / Steps.toFloat())
                } else {
                    interpolate(interruptedOffset, ItemWidth, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(containerHeight = ItemHeight, ExpectedItem(Color.Black, x = x))
        }
    }

    @Test
    fun removeItemBeingSlidIn_withoutExitTransition_itemIsRemovedImmediately() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationStep { step ->
            if (step < Steps / 2) {
                assertItems(
                    containerHeight = ItemHeight,
                    ExpectedItem(
                        Color.Black,
                        x = interpolate(-ItemWidth, 0, step / Steps.toFloat()),
                    ),
                )
            } else {
                if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
                assertItems(containerHeight = ItemHeight)
            }
        }
    }

    @Test
    fun reAddItemAfterSlideOutFinished_slidesInFromInitialOffset() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerHeight = ItemHeight) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInHorizontally(SlideSpec) { -it },
                        exit = slideOutHorizontally(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }
        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, x = interpolate(0, ItemWidth, fraction)),
            )
        }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, x = interpolate(-ItemWidth, 0, fraction)),
            )
        }
    }

    @Test
    fun itemAddedOutsideOfViewport_isNotAnimated() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Blue, Color.Green) }

        rule.runOnIdle { runBlocking { state.scrollBy(ItemHeight * 1.5f) } }

        onAnimationFrame {
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Red, y = -ItemHeight / 2),
                ExpectedItem(Color.Blue, y = ItemHeight / 2),
                ExpectedItem(Color.Green, y = ItemHeight * 3 / 2),
            )
        }
    }

    @Test
    fun scrollingDuringSlideIn_slideContinuesRelativeToItemPosition() {
        var list by mutableStateOf(listOf(Color.Black, Color.Green, Color.Blue))
        rule.setContent {
            LazyList(containerHeight = ItemHeight * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Green, Color.Blue) }

        val scrollAmount = ItemHeight / 2
        onAnimationStep { step ->
            if (step == Steps / 2) {
                rule.runOnIdle { runBlocking { state.scrollBy(scrollAmount.toFloat()) } }
            }
            val scrolled = if (step >= Steps / 2) scrollAmount else 0
            assertItems(
                containerHeight = ItemHeight * 2,
                ExpectedItem(Color.Black, y = -scrolled),
                ExpectedItem(
                    Color.Red,
                    x = interpolate(-ItemWidth, 0, step / Steps.toFloat()),
                    y = ItemHeight - scrolled,
                ),
                ExpectedItem(Color.Green, y = ItemHeight * 2 - scrolled),
            )
        }
    }

    @Test
    fun reverseLayout_verticalSlideIsNotMirrored() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerHeight = ItemHeight, reverseLayout = true) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInVertically(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, y = interpolate(-ItemHeight, 0, fraction)),
            )
        }
    }

    @Test
    fun rtl_horizontalSlideIsNotMirrored() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(containerHeight = ItemHeight, containerWidth = ItemWidth * 2) {
                    items(list, key = { it.toArgb() }) {
                        Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerHeight = ItemHeight,
                ExpectedItem(Color.Black, x = ItemWidth + interpolate(-ItemWidth, 0, fraction)),
                containerWidth = ItemWidth * 2,
            )
        }
    }

    private class ExpectedItem(
        val color: Color,
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = ItemWidth,
        val height: Int = ItemHeight,
    ) {
        fun contains(position: IntOffset) =
            position.x >= x && position.x < x + width && position.y >= y && position.y < y + height
    }

    private fun assertItems(
        containerHeight: Int,
        vararg items: ExpectedItem,
        containerWidth: Int = ItemWidth,
    ) {
        rule.onNodeWithTag(ContainerTag).captureToImage().assertPixels(
            IntSize(containerWidth, containerHeight)
        ) { position ->
            (items.lastOrNull { it.contains(position) }?.color ?: Color.White).compositeOver(
                Color.White
            )
        }
    }

    private fun onAnimationStep(steps: Int = Steps, onFrame: (step: Int) -> Unit) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        for (step in 0..steps) {
            onFrame(step)
            if (step < steps) rule.mainClock.advanceTimeBy(FrameDuration)
        }
    }

    private fun onAnimationFrame(onFrame: (fraction: Float) -> Unit) {
        onAnimationStep { step -> onFrame(step / Steps.toFloat()) }
    }

    @Composable
    private fun LazyList(
        containerHeight: Int,
        containerWidth: Int = ItemWidth,
        reverseLayout: Boolean = false,
        content: LazyListScope.() -> Unit,
    ) {
        state = rememberLazyListState()
        val density = rule.density
        LazyColumn(
            state = state,
            reverseLayout = reverseLayout,
            modifier =
                Modifier.requiredHeight(with(density) { containerHeight.toDp() })
                    .requiredWidth(with(density) { containerWidth.toDp() })
                    .background(Color.White)
                    .testTag(ContainerTag),
            content = content,
        )
    }

    @Composable
    private fun LazyItemScope.Item(
        color: Color,
        width: Int = ItemWidth,
        enter: EnterTransition? = null,
        exit: ExitTransition? = null,
        placementSpec: FiniteAnimationSpec<IntOffset>? = null,
    ) {
        val density = rule.density
        Box(
            Modifier.animateItemWithTransitions(
                    enterTransition = enter,
                    exitTransition = exit,
                    placementSpec = placementSpec,
                )
                .background(color)
                .requiredHeight(with(density) { ItemHeight.toDp() })
                .requiredWidth(with(density) { width.toDp() })
        )
    }
}

private const val FrameDuration = 16L
private const val Steps = 4
private const val DurationMillis = (FrameDuration * Steps).toInt()
private const val ItemHeight = 8
private const val ItemWidth = 16
private const val ContainerTag = "container"

private fun interpolate(start: Int, stop: Int, fraction: Float): Int =
    start + ((stop - start) * fraction).roundToInt()

private val SlideSpec = tween<IntOffset>(DurationMillis, easing = LinearEasing)
private val FadeSpec = tween<Float>(DurationMillis, easing = LinearEasing)
