/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.isLinux
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.awt.Frame
import java.awt.Point
import java.awt.Toolkit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class ApplicationTest {
    @Test
    fun `run application`() = runApplicationTest {
        var isInit = false
        var isDisposed = false

        val appJob = launchTestApplication {
            DisposableEffect(Unit) {
                isInit = true
                onDispose {
                    isDisposed = true
                }
            }
        }

        appJob.join()

        assertThat(isInit).isTrue()
        assertThat(isDisposed).isTrue()
    }

    @Test
    fun `run application with launched effect`() = runApplicationTest {
        val onEffectLaunch = CompletableDeferred<Unit>()
        val shouldEnd = CompletableDeferred<Unit>()

        launchTestApplication {
            LaunchedEffect(Unit) {
                onEffectLaunch.complete(Unit)
                shouldEnd.await()
            }
        }

        onEffectLaunch.await()
        shouldEnd.complete(Unit)
    }

    @Test
    fun `run two applications`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen1 by mutableStateOf(true)
        var isOpen2 by mutableStateOf(true)

        launchTestApplication {
            if (isOpen1) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        launchTestApplication {
            if (isOpen2) {
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

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen1 = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isTrue()

        isOpen2 = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun `window shouldn't use MonotonicFrameClock from application context`() = runApplicationTest {
        lateinit var appClock: MonotonicFrameClock
        lateinit var windowClock: MonotonicFrameClock

        launchTestApplication {
            LaunchedEffect(Unit) {
                appClock = coroutineContext.monotonicFrameClock
            }

            Window(
                onCloseRequest = {}
            ) {
                LaunchedEffect(Unit) {
                    windowClock = coroutineContext.monotonicFrameClock
                }
            }
        }

        awaitIdle()
        assertThat(windowClock).isNotEqualTo(appClock)
    }

    @Test
    fun `retrieving screen location immediately does not crash`() = runApplicationTest {
        launchTestApplication {
            Window(onCloseRequest = {}) {
                Box(
                    Modifier
                        .size(100.dp)
                        .onGloballyPositioned {
                            it.positionOnScreen()
                        }
                        .onPlaced {
                            it.positionOnScreen()
                        }
                )
            }
        }

        awaitIdle()
    }

    @Test
    fun `onGloballyPositioned is called when window is moved`() = runApplicationTest(useDelay = true) {
        lateinit var window: ComposeWindow
        lateinit var density: Density
        var positionOnScreen: Offset = Offset.Unspecified
        launchTestApplication {
            Window(onCloseRequest = {}) {
                density = LocalDensity.current
                window = this@Window.window

                Box(
                    Modifier
                        .size(100.dp)
                        .onGloballyPositioned {
                            positionOnScreen = it.positionOnScreen()
                        }
                )
            }
        }

        awaitIdle()
        assertTrue(positionOnScreen.isSpecified, "Initial position on screen is unspecified")
        val initialPositionOnScreen = positionOnScreen

        window.location = Point(window.x + 100, window.y + 100)
        awaitIdle()
        val finalPositionOnScreen = positionOnScreen

        assertEquals(100f * density.density, finalPositionOnScreen.x - initialPositionOnScreen.x)
    }

    @Test
    fun `positionOnScreen is Unspecified when window is iconified`() = runApplicationTest {
        assumeTrue(Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED))
        assumeFalse(isLinux)  // This test fails on CI for an unclear reason

        lateinit var window: ComposeWindow
        var positionOnScreen: Offset = Offset.Unspecified
        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this@Window.window

                Box(
                    Modifier
                        .size(100.dp)
                        .onGloballyPositioned {
                            positionOnScreen = it.positionOnScreen()
                        }
                )
            }
        }

        awaitIdle()
        assertTrue(positionOnScreen.isSpecified, "Initial position on screen is unspecified")

        window.state = Frame.ICONIFIED
        awaitIdle()
        delay(2000)  // Wait out the macOS iconify animation
        assertEquals(Frame.ICONIFIED, window.state, "Window is not iconified")
        assertFalse(positionOnScreen.isSpecified, "Position on screen is specified when window is iconified")
    }

    @Test
    fun `application does not crash if closed when window is becoming iconified`() = runApplicationTest {
        lateinit var window: ComposeWindow
        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this@Window.window
            }
        }

        awaitIdle()

        window.state = Frame.ICONIFIED
        awaitIdle()

        exitTestApplication()
    }

    @Test
    fun `onGloballyPositioned is not called repeatedly with same position on screen`() = runApplicationTest(useDelay = true) {
        lateinit var window: ComposeWindow
        val positionsOnScreen = mutableListOf<Offset>()
        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this@Window.window
                Box(
                    Modifier
                        .size(100.dp)
                        .onGloballyPositioned {
                            positionsOnScreen.add(it.positionOnScreen())
                        }
                )
            }
        }
        awaitIdle()

        window.location = Point(window.x + 100, window.y + 100)
        awaitIdle()

        assertTrue(
            positionsOnScreen.zipWithNext().none { it.first == it.second },
            message = "onGloballyPositioned is called repeatedly with same positionOnScreen: $positionsOnScreen"
        )
    }


}