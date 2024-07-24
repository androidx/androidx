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

package androidx.compose.ui.scrollcapture

import android.content.Context
import android.graphics.Rect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the scroll capture implementation's integration with semantics. Tests in this class should
 * not use any scrollable components from Foundation.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScrollCaptureTest {

    @get:Rule val rule = createComposeRule()

    private val captureTester = ScrollCaptureTester(rule)

    @Test
    fun search_findsScrollableTarget() =
        captureTester.runTest {
            lateinit var coordinates: LayoutCoordinates
            captureTester.setContent {
                TestVerticalScrollable(
                    size = 10,
                    maxValue = 1f,
                    modifier = Modifier.onPlaced { coordinates = it }
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.hint).isEqualTo(0)
            assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 10, 10))
            assertThat(target.positionInWindow)
                .isEqualTo(coordinates.positionInWindow().roundToPoint())
            assertThat(target.scrollBounds).isEqualTo(Rect(0, 0, 10, 10))
        }

    @Test
    fun search_usesTargetsCoordinates() =
        captureTester.runTest {
            lateinit var coordinates: LayoutCoordinates
            val padding = 15
            captureTester.setContent {
                Box(
                    Modifier.onPlaced { coordinates = it }
                        .padding(with(LocalDensity.current) { padding.toDp() })
                ) {
                    TestVerticalScrollable(
                        size = 10,
                        maxValue = 1f,
                    )
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            // Relative to the View, i.e. the root of the composition.
            assertThat(target.localVisibleRect)
                .isEqualTo(Rect(padding, padding, padding + 10, padding + 10))
            assertThat(target.positionInWindow)
                .isEqualTo(
                    (coordinates.positionInWindow() + Offset(padding.toFloat(), padding.toFloat()))
                        .roundToPoint()
                )
            assertThat(target.scrollBounds)
                .isEqualTo(Rect(padding, padding, padding + 10, padding + 10))
        }

    @Test
    fun search_findsLargestTarget_whenMultipleMatches() =
        captureTester.runTest {
            val smallerSize = 10
            val largerSize = 11
            captureTester.setContent {
                Column {
                    TestVerticalScrollable(size = smallerSize)
                    TestVerticalScrollable(size = largerSize)
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect)
                .isEqualTo(Rect(0, smallerSize, largerSize, smallerSize + largerSize))
        }

    @Test
    fun search_findsDeepestTarget() =
        captureTester.runTest {
            captureTester.setContent {
                TestVerticalScrollable(size = 11) { TestVerticalScrollable(size = 10) }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 10, 10))
        }

    @Test
    fun search_findsDeepestTarget_whenLargerParentSibling() =
        captureTester.runTest {
            captureTester.setContent {
                Column {
                    TestVerticalScrollable(size = 10) { TestVerticalScrollable(size = 9) }
                    TestVerticalScrollable(size = 11)
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 9, 9))
        }

    @Test
    fun search_findsDeepestLargestTarget_whenMultipleMatches() =
        captureTester.runTest {
            captureTester.setContent {
                Column {
                    TestVerticalScrollable(size = 10) { TestVerticalScrollable(size = 9) }
                    TestVerticalScrollable(size = 10) { TestVerticalScrollable(size = 8) }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 9, 9))
        }

    @Test
    fun search_usesClippedSize() =
        captureTester.runTest {
            captureTester.setContent {
                TestVerticalScrollable(size = 10) { TestVerticalScrollable(size = 100) }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 10, 10))
        }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun search_doesNotFindTarget_whenInvisibleToUser() =
        captureTester.runTest {
            captureTester.setContent {
                TestVerticalScrollable(Modifier.semantics { invisibleToUser() })
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_doesNotFindTarget_whenZeroSize() =
        captureTester.runTest {
            captureTester.setContent { TestVerticalScrollable(Modifier.size(0.dp)) }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_doesNotFindTarget_whenZeroMaxValue() =
        captureTester.runTest {
            captureTester.setContent { TestVerticalScrollable(maxValue = 0f) }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_doesNotFindTarget_whenNoScrollAxisRange() =
        captureTester.runTest {
            captureTester.setContent {
                Box(Modifier.size(10.dp).semantics { scrollByOffset { Offset.Zero } })
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_doesNotFindTarget_whenNoVerticalScrollAxisRange() =
        captureTester.runTest {
            captureTester.setContent {
                Box(
                    Modifier.size(10.dp).semantics {
                        scrollByOffset { Offset.Zero }
                        horizontalScrollAxisRange =
                            ScrollAxisRange(
                                value = { 0f },
                                maxValue = { 1f },
                            )
                    }
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_doesNotFindTarget_whenNoScrollByImmediately() =
        captureTester.runTest {
            captureTester.setContent {
                Box(
                    Modifier.size(10.dp).semantics {
                        verticalScrollAxisRange =
                            ScrollAxisRange(
                                value = { 0f },
                                maxValue = { 1f },
                            )
                    }
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun callbackOnSearch_returnsViewportBounds() =
        captureTester.runTest {
            lateinit var coordinates: LayoutCoordinates
            val padding = 15
            captureTester.setContent {
                Box(
                    Modifier.onPlaced { coordinates = it }
                        .padding(with(LocalDensity.current) { padding.toDp() })
                ) {
                    TestVerticalScrollable(
                        size = 10,
                        maxValue = 1f,
                    )
                }
            }

            val callback = captureTester.findCaptureTargets().single().callback

            launch {
                val result = callback.onScrollCaptureSearch()

                // Search result is in window coordinates.
                assertThat(result)
                    .isEqualTo(
                        Rect(
                            coordinates.positionInWindow().x.roundToInt() + padding,
                            coordinates.positionInWindow().y.roundToInt() + padding,
                            coordinates.positionInWindow().x.roundToInt() + padding + 10,
                            coordinates.positionInWindow().y.roundToInt() + padding + 10
                        )
                    )
            }
        }

    @Test
    fun callbackOnImageCapture_scrollsBackwardsThenForwards() =
        captureTester.runTest {
            expectingScrolls(rule) {
                val size = 10
                val captureHeight = size / 2
                captureTester.setContent {
                    TestVerticalScrollable(
                        size = size,
                        // Can't be a reference, see https://youtrack.jetbrains.com/issue/KT-49665
                        onScrollByOffset = { respondToScrollExpectation(it) }
                    )
                }

                val target = captureTester.findCaptureTargets().single()
                captureTester.capture(target, captureHeight) {
                    // First request is at origin, no scrolling required.
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 0, 10, 5))
                    assertNoPendingScrollRequests()

                    // Back one half-page, but only respond to part of it.
                    expectScrollRequest(Offset(0f, -5f), consume = Offset(0f, -4f))
                    shiftWindowBy(-5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, -4, 10, 0))

                    // Forward one half-page – already in viewport, no scrolling required.
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 0, 10, 5))
                    assertNoPendingScrollRequests()

                    // Forward another half-page. This time we need to scroll.
                    expectScrollRequest(Offset(0f, 4f))
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 5, 10, 10))

                    // Forward another half-page, scroll again so now we're past the original
                    // viewport.
                    expectScrollRequest(Offset(0f, 5f))
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 10, 10, 15))

                    // When capture ends expect one last scroll request to reset to original offset.
                    // Note that this request will be made _after_ this capture{} lambda returns.
                    expectScrollRequest(Offset(0f, -5f))
                }
                assertNoPendingScrollRequests()
            }
        }

    @Test
    fun callbackOnImageCapture_scrollsBackwardsThenForwards_reverseScrolling() =
        captureTester.runTest {
            expectingScrolls(rule) {
                val size = 10
                val captureHeight = size / 2
                captureTester.setContent {
                    TestVerticalScrollable(
                        reverseScrolling = true,
                        size = size,
                        // Can't be a reference, see https://youtrack.jetbrains.com/issue/KT-49665
                        onScrollByOffset = { respondToScrollExpectation(it) }
                    )
                }

                val target = captureTester.findCaptureTargets().single()
                captureTester.capture(target, captureHeight) {
                    // First request is at origin, no scrolling required.
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 0, 10, 5))
                    assertNoPendingScrollRequests()

                    // Back one half-page, but only respond to part of it.
                    expectScrollRequest(Offset(0f, 5f), consume = Offset(0f, 4f))
                    shiftWindowBy(-5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, -4, 10, 0))

                    // Forward one half-page – already in viewport, no scrolling required.
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 0, 10, 5))
                    assertNoPendingScrollRequests()

                    // Forward another half-page. This time we need to scroll.
                    expectScrollRequest(Offset(0f, -4f))
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 5, 10, 10))

                    // Forward another half-page, scroll again so now we're past the original
                    // viewport.
                    expectScrollRequest(Offset(0f, -5f))
                    shiftWindowBy(5)
                    assertThat(performCaptureDiscardingBitmap()).isEqualTo(Rect(0, 10, 10, 15))

                    // When capture ends expect one last scroll request to reset to original offset.
                    // Note that this request will be made _after_ this capture{} lambda returns.
                    expectScrollRequest(Offset(0f, 5f))
                }
                assertNoPendingScrollRequests()
            }
        }

    @Test
    fun captureSession_setsScrollCaptureInProgress_inSameComposition() =
        captureTester.runTest {
            var isScrollCaptureInProgressValue = false
            captureTester.setContent {
                isScrollCaptureInProgressValue = LocalScrollCaptureInProgress.current
                TestVerticalScrollable(size = 10)
            }

            rule.awaitIdle()
            assertThat(isScrollCaptureInProgressValue).isFalse()

            val target = captureTester.findCaptureTargets().single()
            assertThat(isScrollCaptureInProgressValue).isFalse()
            captureTester.capture(target, captureWindowHeight = 5) {
                rule.awaitIdle()
                assertThat(isScrollCaptureInProgressValue).isTrue()
            }
            rule.awaitIdle()
            assertThat(isScrollCaptureInProgressValue).isFalse()
        }

    @Test
    fun captureSession_setsScrollCaptureInProgress_inSubComposition() =
        captureTester.runTest {
            var isScrollCaptureInProgressValue = false

            class ChildComposeView(context: Context) : AbstractComposeView(context) {
                @Composable
                override fun Content() {
                    isScrollCaptureInProgressValue = LocalScrollCaptureInProgress.current
                }
            }
            captureTester.setContent {
                AndroidView(::ChildComposeView)
                TestVerticalScrollable(size = 10)
            }

            rule.awaitIdle()
            assertThat(isScrollCaptureInProgressValue).isFalse()

            val target = captureTester.findCaptureTargets().single()
            assertThat(isScrollCaptureInProgressValue).isFalse()
            captureTester.capture(target, captureWindowHeight = 5) {
                rule.awaitIdle()
                assertThat(isScrollCaptureInProgressValue).isTrue()
            }
            rule.awaitIdle()
            assertThat(isScrollCaptureInProgressValue).isFalse()
        }

    /** A component that publishes all the right semantics to be considered a scrollable. */
    @Composable
    private fun TestVerticalScrollable(
        modifier: Modifier = Modifier,
        size: Int = 10,
        maxValue: Float = 1f,
        onScrollByOffset: suspend (Offset) -> Offset = { Offset.Zero },
        reverseScrolling: Boolean = false,
        content: (@Composable () -> Unit)? = null
    ) {
        with(LocalDensity.current) {
            val updatedMaxValue by rememberUpdatedState(maxValue)
            val scrollAxisRange = remember {
                ScrollAxisRange(
                    value = { 0f },
                    maxValue = { updatedMaxValue },
                    reverseScrolling = reverseScrolling,
                )
            }
            Box(
                modifier.size(size.toDp()).semantics {
                    verticalScrollAxisRange = scrollAxisRange
                    scrollByOffset(onScrollByOffset)
                },
                content = { content?.invoke() }
            )
        }
    }
}
