/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.ui.awt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.copy
import androidx.compose.ui.window.density
import androidx.compose.ui.window.plus
import androidx.compose.ui.window.runApplicationTest
import androidx.savedstate.SavedState
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.MouseEvent.BUTTON1
import java.awt.event.MouseEvent.MOUSE_ENTERED
import java.awt.event.MouseEvent.MOUSE_MOVED
import java.awt.event.WindowEvent
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

// A copy of ComposeWindowTest adapted for ComposeDialog. Don't change it, if it isn't specific for ComposeDialog.
// A copy because it is better to keep tests less abstract, and we can't properly abstract away from JFrame/JDialog.
@OptIn(ExperimentalComposeUiApi::class)
class ComposeDialogTest {
    @get:Rule
    val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun `catch exception on setContent`() = runApplicationTest {
        val caughtExceptions = mutableListOf<Throwable>()
        val window = ComposeDialog()
        try {
            window.isUndecorated = true
            window.size = Dimension(200, 200)
            window.exceptionHandler = WindowExceptionHandler {
                caughtExceptions.add(it)
                window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            }
            window.setContent {
                throw TestException()
            }

            window.isVisible = true
            awaitIdle()
            assertThat(caughtExceptions.size).isEqualTo(1)
            assertThat(caughtExceptions.last()).isInstanceOf(TestException::class.java)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `catch exception on render`() = runApplicationTest {
        val caughtExceptions = mutableListOf<Throwable>()
        val window = ComposeDialog()
        try {
            window.isUndecorated = true
            window.size = Dimension(200, 200)
            window.exceptionHandler = WindowExceptionHandler {
                caughtExceptions.add(it)
                window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            }
            window.setContent {
                Canvas(Modifier.fillMaxSize()) {
                    throw TestException()
                }
            }

            window.isVisible = true
            window.contentPane.paint(window.graphics)
            awaitIdle()
            assertThat(caughtExceptions.size).isAtMost(1)
            assertThat(caughtExceptions.last()).isInstanceOf(TestException::class.java)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `catch exception on event`() = runApplicationTest {
        val caughtExceptions = mutableListOf<Throwable>()
        val window = ComposeDialog()
        try {
            window.isUndecorated = true
            window.size = Dimension(200, 200)
            window.exceptionHandler = WindowExceptionHandler {
                caughtExceptions.add(it)
                window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            }
            window.setContent {
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                throw TestException()
                            }
                        }
                    }
                })
            }

            window.isVisible = true
            awaitIdle()
            window.sendMousePress(BUTTON1, x = 100, y = 50)
            awaitIdle()
            assertThat(caughtExceptions.size).isEqualTo(1)
            assertThat(caughtExceptions.last()).isInstanceOf(TestException::class.java)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `don't override user preferred size`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val window = ComposeDialog()
            try {
                window.preferredSize = Dimension(234, 345)
                window.isUndecorated = true
                assertThat(window.preferredSize).isEqualTo(Dimension(234, 345))
                window.pack()
                assertThat(window.size).isEqualTo(Dimension(234, 345))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `pack to Compose content`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val window = ComposeDialog()
            try {
                window.setContent {
                    Box(Modifier.requiredSize(300.dp, 400.dp))
                }
                window.isUndecorated = true

                window.pack()
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))

                window.isVisible = true
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `a single layout pass at the window start`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val layoutPassConstraints = mutableListOf<Constraints>()

        runBlocking(MainUIDispatcher) {
            val dialog = ComposeDialog()
            try {
                dialog.size = Dimension(300, 400)
                dialog.setContent {
                    Box(Modifier.fillMaxSize().layout { _, constraints ->
                        layoutPassConstraints.add(constraints)
                        layout(0, 0) {}
                    })
                }

                dialog.isUndecorated = true
                dialog.isVisible = true
                dialog.renderImmediately()

                assertThat(layoutPassConstraints).isEqualTo(
                    listOf(
                        Constraints.fixed(
                            width = (300 * dialog.density.density).toInt(),
                            height = (400 * dialog.density.density).toInt(),
                        )
                    )
                )
            } finally {
                dialog.dispose()
            }
        }
    }

    // bug https://youtrack.jetbrains.com/issue/CMP-5170
    @Test
    fun `dispose window in event handler`() = runApplicationTest {
        val window = ComposeDialog()
        try {
            var isClickHappened = false
            window.size = Dimension(300, 400)
            window.setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                    isClickHappened = true
                    window.dispose()
                })
            }
            window.isVisible = true
            window.sendMouseEvent(MOUSE_ENTERED, 100, 50)
            awaitIdle()
            window.sendMouseEvent(MOUSE_MOVED, 100, 50)
            awaitIdle()
            window.sendMousePress(BUTTON1, 100, 50)
            awaitIdle()
            window.sendMouseRelease(BUTTON1, 100, 50)
            awaitIdle()
            assertThat(isClickHappened).isTrue()
        } finally {
            window.dispose()
        }
    }

    @OptIn(ExperimentalSkikoApi::class)
    @Test
    fun skiaLayerAnalytics() = runApplicationTest {
        var rendererIsCalled = false
        val analytics = object : SkiaLayerAnalytics {
            override fun renderer(
                skikoVersion: String,
                os: OS,
                api: GraphicsApi
            ): SkiaLayerAnalytics.RendererAnalytics {
                rendererIsCalled = true
                return super.renderer(skikoVersion, os, api)
            }
        }
        val window = ComposeDialog(skiaLayerAnalytics = analytics)
        try {
            window.size = Dimension(100, 100)
            window.isVisible = true
            awaitIdle()
            assertThat(rendererIsCalled).isTrue()
        } finally {
            window.dispose()
        }
    }

    @Test
    fun savedState() = runApplicationTest {
        var savedState: SavedState? = null
        var lastState = 0

        @Composable
        fun testContent() {
            var state by rememberSaveable { mutableStateOf(0) }
            lastState = state
            LaunchedEffect(Unit) {
                repeat(3) {
                    state++
                    lastState = state
                }
            }
        }

        suspend fun testWindow(savedState: SavedState? = null, verify: (ComposeDialog) -> Unit) {
            val window = ComposeDialog(savedState = savedState)
            try {
                window.setContent { testContent() }
                window.isVisible = true
                awaitIdle()
                verify(window)
            } finally {
                window.dispose()
            }
        }

        testWindow { window ->
            assertThat(lastState).isEqualTo(3)
            savedState = window.saveState()
        }

        testWindow(savedState) {
            assertThat(lastState).isEqualTo(6)
        }
    }

    @OptIn(ExperimentalUnitApi::class)
    fun testComposeDialogSizeSetting(
        setSizeFunction: ComposeDialog.(Dimension) -> Unit
    ) = runApplicationTest {
        val intrinsicSize = Dimension(500, 400)
        val dialog = ComposeDialog().apply {
            setContent {
                Box(Modifier.fillMaxSize().size(intrinsicSize.width.dp, intrinsicSize.height.dp))
            }
        }

        try {
            val appliedSize = Dimension(300, 200)
            dialog.pack()
            val windowInsets = dialog.insets
            val intrinsicWindowSize = intrinsicSize + windowInsets

            dialog.setSizeFunction(appliedSize)
            awaitIdle()
            assertThat(dialog.size).isEqualTo(appliedSize)

            dialog.setSizeFunction(appliedSize.copy(height = UNSPECIFIED_DIMENSION_VALUE))
            awaitIdle()
            assertThat(dialog.size).isEqualTo(appliedSize.copy(height = intrinsicWindowSize.height))

            dialog.setSizeFunction(appliedSize.copy(width = UNSPECIFIED_DIMENSION_VALUE))
            awaitIdle()
            assertThat(dialog.size).isEqualTo(appliedSize.copy(width = intrinsicWindowSize.width))

            dialog.setSizeFunction(UnspecifiedDimension())
            awaitIdle()
            assertThat(dialog.size).isEqualTo(intrinsicWindowSize)
        } finally {
            dialog.dispose()
        }
    }

    @Test
    fun `ComposeDialog setPreferredSize`() = testComposeDialogSizeSetting {
        this.preferredSize = it
        pack()
    }

    @Test
    fun `ComposeDialog with popup prefSize`() = runApplicationTest {
        val dialog = ComposeDialog().apply {
            setContent {
                Box(Modifier.size(100.dp))
                Popup {
                    Box(Modifier.size(500.dp))
                }
            }
        }

        try {
            dialog.pack()
            val size = dialog.size
            assertThat(size).isEqualTo(Dimension(500, 500) + dialog.insets)
        } finally {
            dialog.dispose()
        }
    }

    private class TestException : Exception()
}