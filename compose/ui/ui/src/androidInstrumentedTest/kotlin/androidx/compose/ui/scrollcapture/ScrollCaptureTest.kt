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

import android.graphics.Canvas
import android.graphics.Rect
import android.view.ScrollCaptureSession
import android.view.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Tests the scroll capture implementation's integration with semantics. Tests in this class should
 * not use any scrollable components from Foundation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScrollCaptureTest {

    @get:Rule
    val rule = createComposeRule()

    private val captureTester = ScrollCaptureTester(rule)

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = true
    }

    @After
    fun tearDown() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = false
    }

    @Test
    fun search_findsScrollableTarget() {
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
    fun search_usesTargetsCoordinates() {
        lateinit var coordinates: LayoutCoordinates
        val padding = 15
        captureTester.setContent {
            Box(Modifier
                .onPlaced { coordinates = it }
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
                (coordinates.positionInWindow() +
                    Offset(padding.toFloat(), padding.toFloat())
                    ).roundToPoint()
            )
        assertThat(target.scrollBounds)
            .isEqualTo(Rect(padding, padding, padding + 10, padding + 10))
    }

    @Test
    fun search_findsLargestTarget_whenMultipleMatches() {
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
    fun search_findsDeepestTarget() {
        captureTester.setContent {
            TestVerticalScrollable(size = 11) {
                TestVerticalScrollable(size = 10)
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 10, 10))
    }

    @Test
    fun search_findsDeepestTarget_whenLargerParentSibling() {
        captureTester.setContent {
            Column {
                TestVerticalScrollable(size = 10) {
                    TestVerticalScrollable(size = 9)
                }
                TestVerticalScrollable(size = 11)
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 9, 9))
    }

    @Test
    fun search_findsDeepestLargestTarget_whenMultipleMatches() {
        captureTester.setContent {
            Column {
                TestVerticalScrollable(size = 10) {
                    TestVerticalScrollable(size = 9)
                }
                TestVerticalScrollable(size = 10) {
                    TestVerticalScrollable(size = 8)
                }
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 9, 9))
    }

    @Test
    fun search_usesClippedSize() {
        captureTester.setContent {
            TestVerticalScrollable(size = 10) {
                TestVerticalScrollable(size = 100)
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect).isEqualTo(Rect(0, 0, 10, 10))
    }

    @Test
    fun search_doesNotFindTarget_whenFeatureFlagDisabled() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = false

        captureTester.setContent {
            TestVerticalScrollable()
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).isEmpty()
    }

    @Test
    fun search_doesNotFindTarget_whenInvisibleToUser() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = false

        captureTester.setContent {
            TestVerticalScrollable(Modifier.semantics {
                invisibleToUser()
            })
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).isEmpty()
    }

    @Test
    fun search_doesNotFindTarget_whenZeroSize() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = false

        captureTester.setContent {
            TestVerticalScrollable(Modifier.size(0.dp))
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).isEmpty()
    }

    @Test
    fun search_doesNotFindTarget_whenZeroMaxValue() {
        captureTester.setContent {
            TestVerticalScrollable(maxValue = 0f)
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).isEmpty()
    }

    @Test
    fun search_doesNotFindTarget_whenNoScrollAxisRange() {
        captureTester.setContent {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics {
                        scrollByOffset { Offset.Zero }
                    }
            )
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).isEmpty()
    }

    @Test
    fun search_doesNotFindTarget_whenNoVerticalScrollAxisRange() {
        captureTester.setContent {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics {
                        scrollByOffset { Offset.Zero }
                        horizontalScrollAxisRange = ScrollAxisRange(
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
    fun search_doesNotFindTarget_whenNoScrollByImmediately() {
        captureTester.setContent {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics {
                        verticalScrollAxisRange = ScrollAxisRange(
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
    fun callbackOnSearch_returnsViewportBounds() = runTest {
        lateinit var coordinates: LayoutCoordinates
        val padding = 15
        captureTester.setContent {
            Box(Modifier
                .onPlaced { coordinates = it }
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
            assertThat(result).isEqualTo(
                Rect(
                    coordinates.positionInWindow().x.roundToInt() + padding,
                    coordinates.positionInWindow().y.roundToInt() + padding,
                    coordinates.positionInWindow().x.roundToInt() + padding + 10,
                    coordinates.positionInWindow().y.roundToInt() + padding + 10
                )
            )
        }
    }

    // TODO this is flaky, figure out why
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun callbackOnImageCapture_scrollsBackwardsThenForwards() = runTest {
        data class ScrollRequest(
            val requestedOffset: Offset,
            val consumeScroll: (Offset) -> Unit
        )

        val scrollRequests = Channel<ScrollRequest>(capacity = Channel.RENDEZVOUS)
        suspend fun expectScrollRequest(expectedOffset: Offset, consume: Offset = expectedOffset) {
            val request = select {
                scrollRequests.onReceive { it }
                onTimeout(1000) { fail("No scroll request received after 1000ms") }
            }
            assertThat(request.requestedOffset).isEqualTo(expectedOffset)
            request.consumeScroll(consume)
            // Allow the scroll request to be consumed.
            rule.awaitIdle()
        }

        suspend fun expectNoScrollRequests() {
            rule.awaitIdle()
            if (!scrollRequests.isEmpty) {
                val requests = buildList {
                    do {
                        val request = scrollRequests.tryReceive()
                        request.getOrNull()?.let(::add)
                    } while (request.isSuccess)
                }
                fail("Expected no scroll requests, but had ${requests.size}: " +
                    requests.joinToString { it.requestedOffset.toString() })
            }
        }

        val size = 10
        captureTester.setContent {
            TestVerticalScrollable(
                size = size,
                onScrollByImmediately = { offset ->
                    val result = CompletableDeferred<Offset>(parent = currentCoroutineContext().job)
                    scrollRequests.send(ScrollRequest(offset, consumeScroll = result::complete))
                    result.await()
                }
            )
        }

        val callback = captureTester.findCaptureTargets().single().callback
        val canvas = mock<Canvas>()
        val surface = mock<Surface> {
            on(it.lockHardwareCanvas()).thenReturn(canvas)
        }
        val session = mock<ScrollCaptureSession> {
            on(it.surface).thenReturn(surface)
        }

        launch {
            callback.onScrollCaptureStart(session)

            // First request is at origin, no scrolling required.
            async { callback.onScrollCaptureImageRequest(session, Rect(0, 0, 10, 10)) }
                .let { captureResult ->
                    expectNoScrollRequests()
                    assertThat(captureResult.await()).isEqualTo(Rect(0, 0, 10, 10))
                }

            // Back one half-page, but only respond to part of it.
            async { callback.onScrollCaptureImageRequest(session, Rect(0, -5, 10, 0)) }
                .let { captureResult ->
                    expectScrollRequest(Offset(0f, -5f), consume = Offset(0f, -4f))
                    assertThat(captureResult.await()).isEqualTo(Rect(0, -4, 10, 0))
                }

            // Forward one half-page – already in viewport, no scrolling required.
            async { callback.onScrollCaptureImageRequest(session, Rect(0, 0, 10, 5)) }
                .let { captureResult ->
                    expectNoScrollRequests()
                    assertThat(captureResult.await()).isEqualTo(Rect(0, 0, 10, 5))
                }

            // Forward another half-page. This time we need to scroll.
            async { callback.onScrollCaptureImageRequest(session, Rect(0, 5, 10, 10)) }
                .let { captureResult ->
                    expectScrollRequest(Offset(0f, 4f))
                    assertThat(captureResult.await()).isEqualTo(Rect(0, 5, 10, 10))
                }

            // Forward another half-page, scroll again so now we're past the original viewport.
            async { callback.onScrollCaptureImageRequest(session, Rect(0, 10, 10, 15)) }
                .let { captureResult ->
                    expectScrollRequest(Offset(0f, 5f))
                    assertThat(captureResult.await()).isEqualTo(Rect(0, 10, 10, 15))
                }

            launch { callback.onScrollCaptureEnd() }
            // One last scroll request to reset to original offset.
            expectScrollRequest(Offset(0f, -5f))
            expectNoScrollRequests()
        }
    }

    /**
     * A component that publishes all the right semantics to be considered a scrollable.
     */
    @Composable
    private fun TestVerticalScrollable(
        modifier: Modifier = Modifier,
        size: Int = 10,
        maxValue: Float = 1f,
        onScrollByImmediately: suspend (Offset) -> Offset = { Offset.Zero },
        content: (@Composable () -> Unit)? = null
    ) {
        with(LocalDensity.current) {
            val updatedMaxValue by rememberUpdatedState(maxValue)
            val scrollAxisRange = remember {
                ScrollAxisRange(
                    value = { 0f },
                    maxValue = { updatedMaxValue },
                )
            }
            Box(
                modifier
                    .size(size.toDp())
                    .semantics {
                        verticalScrollAxisRange = scrollAxisRange
                        scrollByOffset(onScrollByImmediately)
                    },
                content = { content?.invoke() }
            )
        }
    }
}
