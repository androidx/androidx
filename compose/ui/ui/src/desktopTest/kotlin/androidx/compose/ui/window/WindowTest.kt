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

package androidx.compose.ui.window

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.LeakDetector
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.isLinux
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.toInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Assume.assumeFalse
import org.junit.Ignore

class WindowTest {

    @Test
    fun `open and close custom window`() = runApplicationTest {
        var window: ComposeWindow? = null

        launchTestApplication {
            var isOpen by remember { mutableStateOf(true) }

            fun createWindow() = ComposeWindow().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                SwingWindow(
                    create = ::createWindow,
                    dispose = ComposeWindow::dispose
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `update custom window`() = runApplicationTest {
        var window: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var title by mutableStateOf("Title1")

        launchTestApplication {
            fun createWindow() = ComposeWindow().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                SwingWindow(
                    create = ::createWindow,
                    dispose = ComposeWindow::dispose,
                    update = { it.title = title }
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()
        assertThat(window?.title).isEqualTo(title)

        title = "Title2"
        awaitIdle()
        assertThat(window?.title).isEqualTo(title)

        isOpen = false
    }

    @Test
    fun `open and close window`() = runApplicationTest {
        var window: ComposeWindow? = null

        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `disable closing window`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isCloseCalled by mutableStateOf(false)
        var window: ComposeWindow? = null

        launchTestApplication {
            if (isOpen) {
                Window(
                    onCloseRequest = {
                        isCloseCalled = true
                    }
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(isCloseCalled).isTrue()
        assertThat(window?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
    }

    @Test
    fun `show splash screen`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var isLoading by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                if (isLoading) {
                    Window(onCloseRequest = {}) {
                        window1 = this.window
                        Box(Modifier.size(32.dp).background(Color.Red))
                    }
                } else {
                    Window(onCloseRequest = {}) {
                        window2 = this.window
                        Box(Modifier.size(32.dp).background(Color.Blue))
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2).isNull()

        isLoading = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `open two windows`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }

                Window(onCloseRequest = {}) {
                    window2 = this.window
                    Box(Modifier.size(32.dp).background(Color.Blue))
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `open nested window`() = runApplicationTest(useDelay = true) {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var isNestedOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))

                    if (isNestedOpen) {
                        Window(
                            onCloseRequest = {},
                            state = rememberWindowState(
                                size = DpSize(300.dp, 300.dp),
                            )
                        ) {
                            window2 = this.window
                            Box(Modifier.size(32.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isNestedOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isFalse()

        isNestedOpen = true
        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `pass composition local to windows`() = runApplicationTest {
        var actualValue1: Int? = null
        var actualValue2: Int? = null
        var actualValue3: Int? = null

        var isOpen by mutableStateOf(true)
        val local1TestValue = compositionLocalOf { 0 }
        val local2TestValue = compositionLocalOf { 0 }
        var locals by mutableStateOf(arrayOf(local1TestValue provides 1))

        launchTestApplication {
            if (isOpen) {
                CompositionLocalProvider(*locals) {
                    Window(
                        onCloseRequest = {},
                        state = rememberWindowState(
                            size = DpSize(600.dp, 600.dp),
                        )
                    ) {
                        actualValue1 = local1TestValue.current
                        actualValue2 = local2TestValue.current
                        Box(Modifier.size(32.dp).background(Color.Red))

                        Window(
                            onCloseRequest = {},
                            state = rememberWindowState(
                                size = DpSize(300.dp, 300.dp),
                            )
                        ) {
                            actualValue3 = local1TestValue.current
                            Box(Modifier.size(32.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(actualValue1).isEqualTo(1)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(1)

        locals = arrayOf(local1TestValue provides 42)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(42)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(42)

        locals = arrayOf(local1TestValue provides 43)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(43)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(43)

        locals = arrayOf(local1TestValue provides 43, local2TestValue provides 12)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(43)
        assertThat(actualValue2).isEqualTo(12)
        assertThat(actualValue3).isEqualTo(43)

        locals = emptyArray()
        awaitIdle()
        assertThat(actualValue1).isEqualTo(0)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(0)

        isOpen = false
    }

    @Test
    fun `DisposableEffect call order`() = runApplicationTest {
        var initCount = 0
        var disposeCount = 0

        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    DisposableEffect(Unit) {
                        initCount++
                        onDispose {
                            disposeCount++
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(initCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        isOpen = false
        awaitIdle()
        assertThat(initCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(1)
    }

    @Test(timeout = 30000)
    fun `window dispose should not cause a memory leak`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val leakDetector = LeakDetector()

        val oldRecomposers = Recomposer.runningRecomposers.value

        runBlocking(MainUIDispatcher) {
            repeat(15) {
                val window = ComposeWindow()
                window.size = Dimension(200, 200)
                window.isVisible = true
                window.setContent {
                    Button({}) {}
                    Slider(0f, {})
                }
                window.dispose()
                leakDetector.observeObject(window)
            }

            while (Recomposer.runningRecomposers.value != oldRecomposers) {
                delay(100.milliseconds)
            }

            assertThat(leakDetector.hasAnyGarbageCollected()).isTrue()
        }
    }

    private fun testDrawingBeforeWindowIsVisible(
        windowState: WindowState,
        canvasSizeModifier: Modifier,
        expectedCanvasSize: FrameWindowScope.() -> DpSize
    ) = runApplicationTest {
        var isComposed = false
        var isDrawn = false
        var isVisibleOnFirstComposition = false
        var isVisibleOnFirstDraw = false
        var actualCanvasSize: IntSize? = null
        var expectedCanvasSizePx: IntSize? = null

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = windowState
            ) {
                if (!isComposed) {
                    isVisibleOnFirstComposition = window.isVisible
                    isComposed = true
                }

                Canvas(canvasSizeModifier) {
                    if (!isDrawn) {
                        isVisibleOnFirstDraw = window.isVisible
                        isDrawn = true

                        // toInt() because this is how the ComposeWindow rounds decimal sizes
                        // (see ComposeBridge.updateSceneSize)
                        actualCanvasSize = size.toInt()
                        expectedCanvasSizePx = expectedCanvasSize().toSize().roundToIntSize()
                    }
                }
            }
        }

        awaitIdle()

        assertThat(isComposed).isTrue()
        assertThat(isDrawn).isTrue()
        assertThat(isVisibleOnFirstComposition).isFalse()
        assertThat(isVisibleOnFirstDraw).isFalse()
        assertEquals(expectedCanvasSizePx, actualCanvasSize)
    }

    @Test(timeout = 30000)
    fun `should draw before window is visible`() {
        val windowSize = DpSize(400.dp, 300.dp)
        testDrawingBeforeWindowIsVisible(
            windowState = WindowState(size = windowSize),
            canvasSizeModifier = Modifier.fillMaxSize(),
            expectedCanvasSize = { windowSize - window.insets.toSize() }
        )
    }

    @Test(timeout = 30000)
    fun `should draw before window with unspecified size is visible`() {
        val canvasSize = DpSize(400.dp, 300.dp)
        testDrawingBeforeWindowIsVisible(
            windowState = WindowState(size = DpSize.Unspecified),
            canvasSizeModifier = Modifier.size(canvasSize),
            expectedCanvasSize = { canvasSize }
        )
    }

    // Unfortunately it doesn't appear to be possible to draw the first frame in a maximized
    // window before it's visible, while at the same time having the WindowState define the
    // "floating" size and position to which it will go when un-maximized.
    // The reason for that is that in order to draw the first frame in time, we need to `pack()` the
    // window before showing it, but this breaks setting the "floating" state.
    // So we don't attempt to draw the first frame in time.
    @Ignore
    @Test(timeout = 30000)
    fun `should draw before maximized window is visible`() {
        testDrawingBeforeWindowIsVisible(
            windowState = WindowState(
                size = DpSize(400.dp, 300.dp),
                placement = WindowPlacement.Maximized
            ),
            canvasSizeModifier = Modifier.fillMaxSize(),
            expectedCanvasSize = {
                val gfxConf = window.graphicsConfiguration
                val screenSize = gfxConf.screenSize()
                val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gfxConf).toSize()

                screenSize - screenInsets - window.insets.toSize()
            }
        )
    }

    @Test(timeout = 30000)
    fun `Window should override density provided by application`() = runApplicationTest {
        val customDensity = Density(3.14f)
        var actualDensity: Density? = null

        launchTestApplication {
            if (isOpen) {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Window(onCloseRequest = ::exitApplication) {
                        actualDensity = LocalDensity.current
                    }
                }
            }
        }

        awaitIdle()
        assertThat(actualDensity).isNotNull()
        assertThat(actualDensity).isNotEqualTo(customDensity)
    }

    @Test
    fun `LaunchedEffect should end before application exit`() = runApplicationTest {
        var isApplicationEffectEnded = false
        var isWindowEffectEnded = false

        val job = launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = ::exitApplication) {
                    LaunchedEffect(Unit) {
                        try {
                            delay(1000.seconds)
                        } finally {
                            isWindowEffectEnded = true
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                try {
                    delay(1000.seconds)
                } finally {
                    isApplicationEffectEnded = true
                }
            }
        }

        awaitIdle()
        exitTestApplication()
        job.cancelAndJoin()

        assertThat(isApplicationEffectEnded).isTrue()
        assertThat(isWindowEffectEnded).isTrue()
    }

    @Ignore("Flaky https://youtrack.jetbrains.com/issue/CMP-9422")
    @Test
    fun `undecorated resizable window with unspecified size`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(
                onCloseRequest = { },
                state = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified),
                undecorated = true,
                resizable = true,
            ) {
                window = this.window
                Box(Modifier.size(32.dp))
            }
        }

        awaitIdle()
        window.renderImmediately()
        assertEquals(32, window.width)
        assertEquals(32, window.height)
    }

    @Test
    fun `showing a window should measure content specified size`() = runApplicationTest {
        // TODO fix on Linux https://github.com/JetBrains/compose-multiplatform/issues/1297
        assumeFalse(isLinux)
        val constraintsList = mutableListOf<Constraints>()
        val windowSize = DpSize(400.dp, 300.dp)
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(
                onCloseRequest = { },
                state = rememberWindowState(size = windowSize),
            ) {
                window = this.window
                Layout(
                    measurePolicy = { _, constraints ->
                        constraintsList.add(constraints)
                        layout(0, 0) { }
                    }
                )
            }
        }

        awaitIdle()

        with(window.density) {
            val expectedSize = (windowSize - window.insets.toSize()).toSize()
            assertEquals(1, constraintsList.size)
            assertEquals(
                Constraints(
                    maxWidth = expectedSize.width.roundToInt(),
                    maxHeight = expectedSize.height.roundToInt()
                ),
                constraintsList.first()
            )
        }
    }

    @Test
    fun `pass LayoutDirection to Window`() = runApplicationTest {
        lateinit var localLayoutDirection: LayoutDirection

        var layoutDirection by mutableStateOf(LayoutDirection.Rtl)
        launchTestApplication {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Window(onCloseRequest = {}) {
                    localLayoutDirection = LocalLayoutDirection.current
                }
            }
        }
        awaitIdle()

        assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the local propagates it into the window
        layoutDirection = LayoutDirection.Ltr
        awaitIdle()
        assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Ltr)
    }

    @Test
    fun `pass LayoutDirection from Window to Popup`() = runApplicationTest {
        lateinit var windowLayoutDirectionResult: LayoutDirection
        lateinit var popupLayoutDirectionResult: LayoutDirection

        var windowLayoutDirection by mutableStateOf(LayoutDirection.Rtl)
        var popupLayoutDirection by mutableStateOf(LayoutDirection.Ltr)
        launchTestApplication {
            CompositionLocalProvider(LocalLayoutDirection provides windowLayoutDirection) {
                Window(onCloseRequest = {}) {
                    windowLayoutDirectionResult = LocalLayoutDirection.current
                    CompositionLocalProvider(LocalLayoutDirection provides popupLayoutDirection) {
                        Popup {
                            popupLayoutDirectionResult = LocalLayoutDirection.current
                        }
                    }
                }
            }
        }
        awaitIdle()

        assertThat(windowLayoutDirectionResult).isEqualTo(LayoutDirection.Rtl)
        assertThat(popupLayoutDirectionResult).isEqualTo(LayoutDirection.Ltr)

        // Test that changing the local propagates it into the window
        windowLayoutDirection = LayoutDirection.Ltr
        popupLayoutDirection = LayoutDirection.Rtl
        awaitIdle()
        assertThat(windowLayoutDirectionResult).isEqualTo(LayoutDirection.Ltr)
        assertThat(popupLayoutDirectionResult).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun `window does not move to front on recomposition`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var window1Title by mutableStateOf("Window 1")

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                title = window1Title,
            ) {
                window1 = this.window
                Box(Modifier.size(32.dp))
            }

            Window(
                onCloseRequest = ::exitApplication,
                title = "Window 2"
            ) {
                window2 = this.window
                Box(Modifier.size(32.dp))
                LaunchedEffect(Unit) {
                    window.toFront()
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()
        assertThat(window1?.isActive).isFalse()
        assertThat(window2?.isActive).isTrue()

        window1Title = "Retitled Window"
        awaitIdle()
        assertThat(window1?.isActive).isFalse()
        assertThat(window2?.isActive).isTrue()
    }

    @Test
    fun `compose empty window once`() = runApplicationTest {
        var compositions = 0
        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                compositions++
            }
        }
        awaitIdle()
        assertEquals(1, compositions)
    }

    @Test
    fun `swing frame init called before it is displayable`() = runApplicationTest {
        var isDisplayableInInit: Boolean? = null
        launchTestApplication {
            SwingWindow(
                onCloseRequest = ::exitApplication,
                init = {
                    isDisplayableInInit = it.isDisplayable
                }
            ) { }
        }

        awaitIdle()
        assertThat(isDisplayableInInit).isFalse()
    }

    @Test
    fun `window does not flash background when closed`() = runApplicationTest {
        lateinit var outerWindow: Window
        lateinit var innerWindow: Window
        var showInnerWindow by mutableStateOf(false)
        val windowSize = DpSize(800.dp, 800.dp)
        val outerWindowState = WindowState(size = windowSize)
        launchTestWindowApplication(state = outerWindowState) {
            outerWindow = this.window
            Box(Modifier.fillMaxSize().background(Color.Black))
            if (showInnerWindow) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = windowSize,
                        position = outerWindowState.position
                    ),
                ) {
                    innerWindow = this.window
                    Box(Modifier.fillMaxSize().background(Color.Black))
                    LaunchedEffect(Unit) {
                        innerWindow.location = outerWindow.location
                    }
                }
            }
        }
        awaitIdle()

        showInnerWindow = true
        awaitIdle()

        var nonBlackPixelDetected: java.awt.Color? = null
        val testLocation = innerWindow.bounds.let {
            Point(it.x + it.width / 2, it.y + it.height / 2)
        }
        val stopThread = java.util.concurrent.atomic.AtomicBoolean(false)
        val t = thread {
            val robot = Robot()
            while (!stopThread.get()) {
                val pixel = robot.getPixelColor(testLocation.x, testLocation.y)
                if (pixel != java.awt.Color.BLACK) {
                    nonBlackPixelDetected = pixel
                    return@thread
                }
            }
        }

        delay(500.milliseconds)
        showInnerWindow = false
        delay(500.milliseconds)
        assertFalse(innerWindow.isVisible)

        stopThread.getAndSet(true)
        t.join()

        assertThat(nonBlackPixelDetected).isNull()
    }

    @Test
    fun coroutineContextIsPropagatedToWindow() = coroutineContextIsPropagatedTo { content ->
        Window(onCloseRequest = ::exitApplication) {
            content()
        }
    }

    @Test
    fun animationsRunAtNonInfiniteRateInWindow() = animationsRunAtNonInfiniteRateIn { content ->
        Window(onCloseRequest = ::exitApplication) {
            content()
        }
    }

    @Test
    fun windowComposableProvidesLocalAwtWindow() = runApplicationTest {
        var localWindow: Window? = null
        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                localWindow = LocalAwtWindow.current
            }
        }
        awaitIdle()
        assertNotNull(localWindow)
    }
}

private object CtxElement : CoroutineContext.Element, CoroutineContext.Key<CtxElement> {
    override val key: CoroutineContext.Key<*> = this
}

internal fun coroutineContextIsPropagatedTo(
    window: @Composable ApplicationScope.(@Composable () -> Unit) -> Unit
) = runApplicationTest {
    var applicationContextElement: CtxElement? = null
    var windowContextElement: CtxElement? = null
    var innerWindowContextElement: CtxElement? = null
    val scope = this + CtxElement
    scope.launchTestApplication {
        LaunchedEffect(Unit) {
            applicationContextElement = currentCoroutineContext()[CtxElement]
        }
        window {
            LaunchedEffect(Unit) {
                windowContextElement = currentCoroutineContext()[CtxElement]
            }

            window {
                LaunchedEffect(Unit) {
                    innerWindowContextElement = currentCoroutineContext()[CtxElement]
                }
            }
        }
    }

    awaitIdle()

    assertThat(applicationContextElement).isNotNull()
    assertThat(windowContextElement).isNotNull()
    assertThat(innerWindowContextElement).isNotNull()
}

internal fun animationsRunAtNonInfiniteRateIn(
    window: @Composable ApplicationScope.(@Composable () -> Unit) -> Unit
) = runApplicationTest {
    suspend fun countFramesForOneSecond(onFrame: () -> Unit) {
        val startTime = System.nanoTime()
        while (System.nanoTime() - startTime < 1.seconds.inWholeNanoseconds) {
            withFrameNanos {
                onFrame()
            }
        }
    }

    var appFrameCount = 0
    var windowFrameCount = 0
    launchTestApplication {
        LaunchedEffect(Unit) {
            countFramesForOneSecond { appFrameCount++ }
        }
        window {
            LaunchedEffect(Unit) {
                countFramesForOneSecond { windowFrameCount++ }
            }
        }
    }

    awaitIdle()

    // Actually, just check that the application "frame rate" is significantly smaller than the window frame rate
    assertThat(windowFrameCount * 10).isLessThan(appFrameCount)
}
