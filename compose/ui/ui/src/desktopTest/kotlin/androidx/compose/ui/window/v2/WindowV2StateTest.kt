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

package androidx.compose.ui.window.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.isLinux
import androidx.compose.ui.isMacOs
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.toDpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.topLeft
import androidx.compose.ui.unit.width
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.toDpOffset
import androidx.compose.ui.window.runApplicationTest
import androidx.compose.ui.window.toDpInsets
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import org.junit.Assume.assumeTrue

// Note that on Linux some tests are flaky. Swing event listeners on Linux have a non-deterministic
// nature. To avoid flakiness, we use delays (see description of the `delay` parameter in
// TestUtils.runApplicationTest).
// It is not a good solution, but it works.

// TODO(demin): figure out how can we fix flaky tests on Linux

class WindowV2StateTest {
    @Test
    fun `manually close window`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = { isOpen = false }, title = "manually close window") {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window.isShowing).isTrue()

        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(window.isShowing).isFalse()
    }

    @Test
    fun `programmatically close window`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(
                    onCloseRequest = { isOpen = false },
                    title = "programmatically close window"
                ) {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window.isShowing).isFalse()
    }

    @Test
    fun `programmatically open and close nested window`() = runApplicationTest(useDelay = true) {
        var parentWindow: ComposeWindow? = null
        var childWindow: ComposeWindow? = null
        var isParentOpen by mutableStateOf(true)
        var isChildOpen by mutableStateOf(false)

        launchTestApplication {
            if (isParentOpen) {
                Window(
                    onCloseRequest = {},
                    title = "(parent) programmatically open and close nested window"
                ) {
                    parentWindow = this.window

                    if (isChildOpen) {
                        Window(
                            onCloseRequest = {},
                            title = "(child) programmatically open and close nested window"
                        ) {
                            childWindow = this.window
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()

        isChildOpen = true
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isTrue()

        isChildOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isFalse()

        isParentOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isFalse()
    }

    @Test
    fun `set size and position before show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(242, 242)
        val state = WindowStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )

        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "set size and position before show") {
                window = this.window
            }
        }

        awaitIdle()
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)
    }

    @Test
    fun `change position after show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(200, 200)

        val state = WindowStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "change position after show") {
                window = this.window
            }
        }

        awaitIdle()

        val newPosition = Point(242, 242)
        state.requestPosition(newPosition.toDpOffset())
        awaitIdle()
        assertCoordinatesApproximatelyEqual(newPosition, window.location)
    }

    @Test
    fun `change size after show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(200, 200)

        val state = WindowStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "change size after show") {
                window = this.window
            }
        }

        awaitIdle()

        val newSize = Dimension(250, 200)
        state.requestSize(newSize.toDpSize())
        awaitIdle()
        assertSizesApproximatelyEqual(newSize, window.size)
    }

    @Test
    fun `center window`() = runApplicationTest {
        fun Rectangle.center() = Point(x + width / 2, y + height / 2)
        fun JFrame.center() = bounds.center()
        fun JFrame.screenCenter() = graphicsConfiguration.bounds.center()
        infix fun Point.maxDistance(other: Point) = max(abs(x - other.x), abs(y - other.y))

        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.CenteredOnScreen
            )
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "center window") {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.center() maxDistance window.screenCenter() < 250)
    }

    @Test
    fun `remember position after reattach`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null
        var isWindow1 by mutableStateOf(true)

        launchTestApplication {
            if (isWindow1) {
                Window(onCloseRequest = {}, state, title = "remember position after reattach 1") {
                    window1 = this.window
                }
            } else {
                Window(onCloseRequest = {}, state, title = "remember position after reattach 2") {
                    window2 = this.window
                }
            }
        }

        awaitIdle()

        val position = Point(242, 242)
        state.requestPosition(position.toDpOffset())
        awaitIdle()
        assertThat(window1?.location).isEqualTo(position)

        isWindow1 = false
        awaitIdle()
        assertThat(window2?.location).isEqualTo(position)
    }

    @Test
    fun `state bounds should be initialized after show`() = runApplicationTest(
        useDelay = isLinux
    ) {
        val state = WindowState()
        launchTestApplication {
            Window(
                onCloseRequest = {},
                state = state,
                title = "state bounds should be initialized after show"
            ) { }
        }

        assertThat(state.isInitialized).isFalse()

        awaitIdle()
        assertThat(state.isInitialized).isTrue()
        state.bounds  // Just make sure it doesn't crash
    }

    @Test
    fun `enter fullscreen`() = runApplicationTest(
        useDelay = isLinux || isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "enter fullscreen") {
                window = this.window
            }
        }

        awaitIdle()

        state.requestPlacement(WindowPlacement.Fullscreen)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Fullscreen)

        state.requestPlacement(WindowPlacement.Floating)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/3003
    @Test
    fun `WindowState placement after showing fullscreen window`() = runApplicationTest(
        useDelay = isLinux || isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowState(initialPlacement = WindowPlacement.Fullscreen)
        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "WindowState placement after showing fullscreen window") { }
        }

        awaitIdle()

        assertThat(state.placement).isEqualTo(WindowPlacement.Fullscreen)
    }

    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/3557): check this test on Linux CI
    @Test
    fun maximize() = runApplicationTest(useDelay = isMacOs) {
        assumeTrue(!isLinux)
        val state = WindowStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "maximize") {
                window = this.window
            }
        }

        awaitIdle()

        state.requestPlacement(WindowPlacement.Maximized)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)

        state.requestPlacement(WindowPlacement.Floating)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
    }

    @Test
    fun minimize() = runApplicationTest(useDelay = isMacOs, delayMillis = 1000) {
        val state = WindowStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "minimize") {
                window = this.window
            }
        }

        awaitIdle()

        state.requestMinimized(true)
        awaitIdle()
        assertThat(window.isMinimized).isTrue()

        state.requestMinimized(false)
        awaitIdle()
        assertThat(window.isMinimized).isFalse()
    }

    @Test
    fun `maximize and minimize`() = runApplicationTest {
        // macOS can't be maximized and minimized at the same time
        // Seems like it can't be on Linux too
        assumeTrue(!isMacOs && !isLinux)

        val state = WindowStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "maximize and minimize") {
                window = this.window
            }
        }

        awaitIdle()

        state.requestMinimized(true)
        state.requestPlacement(WindowPlacement.Maximized)
        awaitIdle()
        assertThat(window.isMinimized).isTrue()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
    }

    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/3557): check this test on Linux CI
    @Test
    fun `restore size and position after maximize`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000
    ) {
        assumeTrue(!isLinux)
        val size = Dimension(201, 203)
        val position = Point(196, 257)

        val state = WindowStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "1 restore size and position after maximize") {
                window = this.window
            }
        }

        awaitIdle()
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)

        state.requestPlacement(WindowPlacement.Maximized)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
        assertSizesNotApproximatelyEqual(size, window.size)
        assertCoordinatesNotApproximatelyEqual(position, window.location)

        state.requestPlacement(WindowPlacement.Floating)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)
    }

    @Test
    fun `restore size and position after fullscreen`() = runApplicationTest(
        useDelay = isMacOs || isLinux,
        delayMillis = 1000,
    ) {
        val size = Dimension(201, 203)
        val position = Point(196, 257)

        val state = WindowStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "2 restore size and position after fullscreen") {
                window = this.window
            }
        }

        awaitIdle()
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)

        state.requestPlacement(WindowPlacement.Fullscreen)
        awaitIdle()
        assertSizesNotApproximatelyEqual(size, window.size)
        assertCoordinatesNotApproximatelyEqual(position, window.location)
        assertThat(window.size).isNotEqualTo(size)

        state.requestPlacement(WindowPlacement.Floating)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)
    }

    @Test
    fun `window state size and position determine unmaximized state`() = runApplicationTest(
        useDelay = true,
        delayMillis = 1000
    ) {
        // This fails on our CI it fails because the initial placement fails to be Maximized.
        // The `maximize window before show` test fails the same way.
        // Haven't actually tested on Windows; if you run it, and it doesn't pass, replace with
        // assumeTrue(isMacOs), or investigate/fix.
        assumeTrue(!isLinux)

        val size = Dimension(201, 203)
        val position = Point(196, 257)

        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(size.toDpSize()),
                positionProvider = WindowPositionProvider.Absolute(position.toDpOffset())
            ),
            initialPlacement = WindowPlacement.Maximized
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "window state size and position determine unmaximized state") {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)

        state.requestPlacement(WindowPlacement.Floating)
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertSizesApproximatelyEqual(size, window.size)
        assertCoordinatesApproximatelyEqual(position, window.location)
    }

    @Test
    fun `maximize window before show`() = runApplicationTest(useDelay = isLinux) {
        // This fails on our Linux CI; the window reports WindowPlacement.Floating.
        // But testing in an actual Ubuntu 22 system, it succeeds.
        assumeTrue(!isLinux)

        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.CenteredOnScreen,
            ),
            initialPlacement = WindowPlacement.Maximized,
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "maximize window before show") {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
    }

    @Test
    fun `minimize window before show`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.CenteredOnScreen,
            ),
            initiallyMinimized = true
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "minimize window before show") {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.isMinimized).isTrue()
    }

    @Test
    fun `enter fullscreen before show`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000,
    ) {
        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.CenteredOnScreen,
            ),
            initialPlacement = WindowPlacement.Fullscreen,
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state, title = "enter fullscreen before show") {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Fullscreen)
    }

    @Test
    fun `set window preferred height`() = runApplicationTest(useDelay = isLinux) {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.PreferredHeight(width = 300.dp)
            )
        )

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set window preferred height"
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.contentSize.width).isEqualTo(300)
        assertThat(window.contentSize.height).isEqualTo(200)
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))
    }

    @Test
    fun `set window preferred width`() = runApplicationTest {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.PreferredWidth(height = 300.dp)
            )
        )
        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set window preferred width"
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.contentSize.height).isEqualTo(300)
        assertThat(window.contentSize.width).isEqualTo(400)
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))
    }

    @Test
    fun `set unconstrained window size by its content`() = runApplicationTest {
        assumeTrue(!isLinux) // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Unconstrained
            )
        )

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set unconstrained window size by its content"
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))
    }

    @Test
    fun `set window size by its content when window is visible`() = runApplicationTest(
        useDelay = isLinux || isMacOs
    ) {
        lateinit var window: ComposeWindow
        val state = WindowStateWithBounds(initialSize = DpSize(100.dp, 100.dp))

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set window size by its content when window is visible"
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()

        state.requestSize(WindowSizeProvider.Unconstrained)
        awaitIdle()
        assertThat(window.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))
    }

    @Test
    fun `change visibility`() = runApplicationTest {
        lateinit var window: ComposeWindow

        var visible by mutableStateOf(false)

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                visible = visible,
                title = "change visibility"
            ) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.isVisible).isEqualTo(false)

        visible = true
        awaitIdle()
        assertThat(window.isVisible).isEqualTo(true)
    }

    @Test
    fun `invisible window should be active`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                visible = false,
                title = "invisible window should be active"
            ) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `show invisible undecorated window`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                visible = false,
                decoration = WindowDecoration.Undecorated(),
                title = "show invisible undecorated window"
            ) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))
    }

    @Test
    fun windowStateIsPreservedWhenRemovingAndAddingComposable() = runApplicationTest {
        var showWindow by mutableStateOf(true)
        lateinit var windowState: WindowState
        var windowVisible = false
        launchTestApplication {
            val state = rememberWindowStateWithBounds()
            windowState = state
            if (showWindow) {
                Window(
                    state = state,
                    onCloseRequest = { },
                    title = "windowStateIsPreservedWhenRemovingAndAddingComposable"
                ) {
                    Box(Modifier.size(32.dp))
                    DisposableEffect(Unit) {
                        windowVisible = true
                        onDispose {
                            windowVisible = false
                        }
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }
        awaitIdle()

        windowState.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }
        awaitIdle()
        val windowBounds = windowState.bounds

        showWindow = false
        awaitIdle()
        assertFalse(windowVisible)

        showWindow = true
        awaitIdle()
        assertTrue(windowState.isInitialized)
        assertEquals(windowBounds, windowState.bounds)
    }

    @Test
    fun windowStateIsPreservedWhenSavingAndRestoring() = runApplicationTest {
        var showWindow by mutableStateOf(true)
        var windowState: WindowState? = null
        launchTestApplication {
            val stateHolder = rememberSaveableStateHolder()
            stateHolder.SaveableStateProvider(showWindow) {
                if (showWindow) {
                    val state = rememberWindowStateWithBounds()
                    DisposableEffect(state) {
                        windowState = state
                        onDispose {
                            windowState = null
                        }
                    }
                    Window(
                        state = state,
                        onCloseRequest = { },
                        title = "windowStateIsPreservedWhenSavingAndRestoring"
                    ) {
                        Box(Modifier.size(32.dp))
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }
        awaitIdle()

        windowState!!.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }
        awaitIdle()
        val windowBounds = windowState!!.bounds

        showWindow = false
        awaitIdle()
        assertNull(windowState)

        showWindow = true
        awaitIdle()
        assertTrue(windowState!!.isInitialized)
        assertEquals(windowBounds, windowState!!.bounds)
    }

    @Test
    fun windowIsShownCorrectlyIfStateSavedBeforeWindowIsShown() = runApplicationTest {
        var createWindowState by mutableStateOf(true)
        var showWindow by mutableStateOf(false)
        var windowState: WindowState? = null
        launchTestApplication {
            val stateHolder = rememberSaveableStateHolder()
            stateHolder.SaveableStateProvider(createWindowState) {
                if (createWindowState) {
                    val state = rememberWindowStateWithBounds(
                        initialSize = DpSize(300.dp, 300.dp)
                    )
                    DisposableEffect(state) {
                        windowState = state
                        onDispose {
                            windowState = null
                        }
                    }
                    if (showWindow) {
                        Window(
                            state = state,
                            onCloseRequest = { },
                            title = "windowIsShownCorrectlyIfStateSavedBeforeWindowIsShown"
                        ) {
                            Box(Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }

        awaitIdle()
        assertNotNull(windowState)
        assertFalse(windowState!!.isInitialized)
        windowState!!.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }

        createWindowState = false
        awaitIdle()
        assertNull(windowState)

        createWindowState = true
        showWindow = true
        awaitIdle()

        awaitIdle()
        assertNotNull(windowState)
        assertTrue(windowState!!.isInitialized)
        // Size should be as the one requested in rememberWindowStateWithBounds, not the one in
        // windowState!!.requestBounds above.
        assertEquals(DpSize(300.dp, 300.dp), windowState!!.bounds.size)
    }

    private fun runWindowSizeTest(
        testName: String,
        sizeProvider: WindowSizeProvider,
        content: @Composable () -> Unit,
        expectedWindowSizeSansInsets: DpSize,
    ) = runApplicationTest {
        val windowState = WindowState(
            initialBoundsProvider = WindowBoundsProvider(sizeProvider)
        )
        lateinit var window: ComposeWindow
        launchTestApplication {
            Window(
                state = windowState,
                onCloseRequest = {},
                title = testName
            ) {
                window = this.window
                content()
            }
        }
        awaitIdle()
        assertEquals(
            expectedWindowSizeSansInsets + window.insets.toDpInsets(),
            windowState.bounds.size
        )
    }

    @Test
    fun windowPreferredWidth() = runWindowSizeTest(
        testName = "windowPreferredWidth",
        sizeProvider = WindowSizeProvider.PreferredWidth(height = 500.dp),
        content = {
            BoxWithGivenSize(
                width = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(400.dp, 500.dp)
    )

    @Test
    fun windowPreferredHeight() = runWindowSizeTest(
        testName = "windowPreferredHeight",
        sizeProvider = WindowSizeProvider.PreferredHeight(width = 500.dp),
        content = {
            BoxWithGivenSize(
                height = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(500.dp, 400.dp)
    )

    @Test
    fun `preferred size is rounded up`() = runWindowSizeTest(
        testName = "preferred size is rounded up",
        sizeProvider = WindowSizeProvider.Unconstrained,
        content = {
            Layout { _, _ ->
                val size = (density * 100 + 1).toInt()
                layout(size, size) { }
            }
        },
        expectedWindowSizeSansInsets = DpSize(101.dp, 101.dp)
    )

    private fun runBoundsOverwriteTest(
        name: String,
        windowState: WindowState,
        expectedPosition: DpOffset,
        expectedSize: DpSize
    ) = runApplicationTest {
        launchTestApplication {
            Window(
                state = windowState,
                onCloseRequest = {},
                title = name
            ) {
                LaunchedEffect(Unit) {
                    window.addComponentListener(object: ComponentAdapter() {
                        // Verify that the bounds are set correctly immediately, not just at some
                        // point after the window is shown.
                        override fun componentShown(e: ComponentEvent) {
                            assertEquals(expectedSize, window.size.toDpSize())
                            assertEquals(expectedPosition, window.location.toDpOffset())
                        }
                    })
                }
            }
        }
        awaitIdle()

        assertEquals(expectedSize, windowState.bounds.size)
        assertEquals(expectedPosition, windowState.bounds.topLeft)
    }

    @Test
    fun `requesting size before initialization does not overwrite position`() {
        val position = DpOffset(300.dp, 300.dp)
        val size = DpSize(400.dp, 400.dp)
        val windowState = WindowStateWithBounds(
            initialPosition = position,
        )
        windowState.requestSize(size)

        runBoundsOverwriteTest(
            name = "requesting size before initialization does not overwrite position",
            windowState = windowState,
            expectedSize = size,
            expectedPosition = position,
        )
    }

    @Test
    fun `requesting position before initialization does not overwrite size`() {
        val position = DpOffset(300.dp, 300.dp)
        val size = DpSize(400.dp, 400.dp)
        val windowState = WindowStateWithBounds(
            initialSize = size,
        )
        windowState.requestPosition(position)

        runBoundsOverwriteTest(
            name = "requesting position before initialization does not overwrite size",
            windowState = windowState,
            expectedSize = size,
            expectedPosition = position,
        )
    }
}

private const val LinuxCoordinateTolerance = 10

private val CoordinateTolerance = if (isLinux) LinuxCoordinateTolerance else 0

internal fun assertCoordinatesApproximatelyEqual(
    expected: Point,
    actual: Point,
) {
    if (((expected.x - actual.x).absoluteValue > CoordinateTolerance) ||
        ((expected.y - actual.y).absoluteValue > CoordinateTolerance)
    ) {
        throw AssertionError(
            "Expected <$expected> with absolute tolerance" +
                " <$CoordinateTolerance>, actual <$actual>."
        )
    }
}

internal fun assertSizesApproximatelyEqual(
    expected: Dimension,
    actual: Dimension,
) {
    if (((expected.width - actual.width).absoluteValue > CoordinateTolerance) ||
        ((expected.height - actual.height).absoluteValue > CoordinateTolerance)
    ) {
        throw AssertionError(
            "Expected <$expected> with absolute tolerance" +
                " <$CoordinateTolerance>, actual <$actual>."
        )
    }
}

internal fun assertCoordinatesNotApproximatelyEqual(
    expected: Point,
    actual: Point,
) {
    if (((expected.x - actual.x).absoluteValue <= CoordinateTolerance) &&
        ((expected.y - actual.y).absoluteValue <= CoordinateTolerance)
    ) {
        throw AssertionError(
            "Expected <$expected> to not equal actual <$actual> with absolute" +
                " tolerance <$CoordinateTolerance>"
        )
    }
}

internal fun assertSizesNotApproximatelyEqual(
    expected: Dimension,
    actual: Dimension,
) {
    if (((expected.width - actual.width).absoluteValue <= CoordinateTolerance) &&
        ((expected.height - actual.height).absoluteValue <= CoordinateTolerance)
    ) {
        throw AssertionError(
            "Expected <$expected> to not equal actual <$actual> with absolute" +
                " tolerance <$CoordinateTolerance>"
        )
    }
}
