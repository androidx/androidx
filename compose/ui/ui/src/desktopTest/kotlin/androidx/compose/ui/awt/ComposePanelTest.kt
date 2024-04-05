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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.ThrowUncaughtExceptionRule
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

class ComposePanelTest {
    @get:Rule
    val throwUncaughtExceptionRule = ThrowUncaughtExceptionRule()

    @Test
    fun `don't override user preferred size`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.preferredSize = Dimension(234, 345)
            assertThat(composePanel.preferredSize).isEqualTo(Dimension(234, 345))

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                assertThat(composePanel.preferredSize).isEqualTo(Dimension(234, 345))

                frame.pack()
                assertThat(composePanel.size).isEqualTo(Dimension(234, 345))
                assertThat(frame.size).isEqualTo(Dimension(234, 345))
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `pack to Compose content`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                Box(Modifier.requiredSize(300.dp, 400.dp))
            }

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                frame.pack()
                assertThat(composePanel.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(frame.preferredSize).isEqualTo(Dimension(300, 400))

                frame.isVisible = true
                assertThat(composePanel.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(frame.preferredSize).isEqualTo(Dimension(300, 400))
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `a single layout pass at the window start`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val layoutPassConstraints = mutableListOf<Constraints>()

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                Box(Modifier.fillMaxSize().layout { _, constraints ->
                    layoutPassConstraints.add(constraints)
                    layout(0, 0) {}
                })
            }

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.size = Dimension(300, 400)
                frame.isUndecorated = true
                frame.isVisible = true
                frame.paint(frame.graphics)

                assertThat(layoutPassConstraints).isEqualTo(
                    listOf(
                        Constraints.fixed(
                            width = (300 * frame.density.density).toInt(),
                            height = (400 * frame.density.density).toInt()
                        )
                    )
                )
            } finally {
                frame.dispose()
            }
        }
    }

    @OptIn(ExperimentalSkikoApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun SkiaLayerAnalytics() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
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

            val composePanel = ComposePanel(skiaLayerAnalytics = analytics)
            composePanel.size = Dimension(100, 100)

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.size = Dimension(100, 100)
                frame.isUndecorated = true
                frame.isVisible = true
                frame.contentPane.paint(frame.graphics)
                assertThat(rendererIsCalled).isTrue()
            } finally {
                frame.dispose()
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `compose state shouldn't reset on panel remove and add with isDisposeOnRemove = false`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            var initialStateCounter = 0
            val composePanel = ComposePanel().apply {
                isDisposeOnRemove = false
            }
            composePanel.setContent {
                var state by remember { mutableStateOf(0) }

                LaunchedEffect(state) {
                    if (state == 0) {
                        state++
                        initialStateCounter++
                    }
                }
            }

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                frame.pack()

                frame.isVisible = true
                delay(1000)
                assertEquals(1, initialStateCounter)

                frame.contentPane.remove(composePanel)
                delay(1000)
                assertEquals(1, initialStateCounter)

                frame.contentPane.add(composePanel)
                delay(1000)
                assertEquals(1, initialStateCounter)
            } finally {
                frame.dispose()
                composePanel.dispose()
            }
        }
    }

    @Test
    fun `compose state should reset on panel remove and add`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            var initialStateCounter = 0
            val composePanel = ComposePanel()
            composePanel.setContent {
                var state by remember { mutableStateOf(0) }

                LaunchedEffect(state) {
                    if (state == 0) {
                        state++
                        initialStateCounter++
                    }
                }
            }

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                frame.pack()

                frame.isVisible = true
                delay(1000)
                assertEquals(1, initialStateCounter)

                frame.contentPane.remove(composePanel)
                delay(1000)
                assertEquals(1, initialStateCounter)

                frame.contentPane.add(composePanel)
                delay(1000)
                assertEquals(2, initialStateCounter)
            } finally {
                frame.dispose()
            }
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/4479
    @Test
    fun `add, removing, add, set size`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            var size = Size.Zero
            val composePanel = ComposePanel()
            composePanel.setContent {
                Box(Modifier.fillMaxSize().onGloballyPositioned {
                    size = it.size.toSize()
                })
            }

            val frame = JFrame()
            frame.isUndecorated = true
            frame.size = Dimension(100, 100)
            try {
                val density = frame.contentPane.density.density
                frame.contentPane.add(composePanel)
                frame.isVisible = true
                delay(1000)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.contentPane.remove(composePanel)
                delay(1000)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.contentPane.add(composePanel)
                delay(1000)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.size = Dimension(200, 100)
                delay(1000)
                assertEquals(Size(200f * density, 100f * density), size)
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `initial panel size with border layout`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                Text("Content")
            }

            val frame = JFrame()
            try {
                val content = JPanel(BorderLayout()).apply {
                    add(composePanel, BorderLayout.CENTER)
                }
                frame.contentPane.add(content)
                frame.pack()

                frame.isVisible = true
                delay(1000)
                assertTrue(content.size.height > 2)
                assertTrue(content.size.width > 2)
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `initial panel size of LazyColumn with border layout`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                LazyColumn(modifier = Modifier.sizeIn(maxHeight = 500.dp)) {
                    repeat(100_000) {
                        item {
                            Text("Text $it")
                        }
                    }
                }
            }

            val frame = JFrame()
            try {
                val content = JPanel(BorderLayout()).apply {
                    add(composePanel, BorderLayout.CENTER)
                }
                frame.contentPane.add(content)
                frame.pack()

                frame.isVisible = true
                delay(1000)
                assertTrue(content.size.width > 2)
                assertEquals(500, content.size.height)
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `initial panel size of LazyColumn with border layout and preferred frame size`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                LazyColumn {
                    repeat(100_000) {
                        item {
                            Text("Text $it")
                        }
                    }
                }
            }

            val frame = JFrame()
            try {
                val content = JPanel(BorderLayout()).apply {
                    add(composePanel, BorderLayout.CENTER)
                }
                frame.contentPane.add(content)
                frame.contentPane.preferredSize = Dimension(200, 300)
                frame.pack()

                frame.isVisible = true
                delay(1000)
                assertEquals(200, content.size.width)
                assertEquals(300, content.size.height)
            } finally {
                frame.dispose()
            }
        }
    }

    // Issue: https://github.com/JetBrains/compose-multiplatform/issues/4123
    @Test
    fun `hover events in panel with offset`() = runApplicationTest {
        var enterEvents = 0
        var exitEvents = 0

        val composePanel = ComposePanel()
        composePanel.setBounds(25, 25, 50, 50)
        composePanel.setContent {
            Box(Modifier.size(50.dp)
                .onPointerEvent(PointerEventType.Enter) { enterEvents++ }
                .onPointerEvent(PointerEventType.Exit) { exitEvents++ }
            )
        }

        val window = JFrame()
        window.size = Dimension(200, 200)
        try {
            val content = JPanel(BorderLayout()).apply {
                layout = null
                add(composePanel, BorderLayout.CENTER)
            }
            window.contentPane.add(content)
            window.isVisible = true

            composePanel.sendMouseEvent(MouseEvent.MOUSE_ENTERED, 20, 20)
            awaitIdle()
            composePanel.sendMouseEvent(MouseEvent.MOUSE_MOVED, 20, 20)
            awaitIdle()

            assertEquals(1, enterEvents)
            assertEquals(0, exitEvents)

            composePanel.sendMouseEvent(MouseEvent.MOUSE_MOVED, 50, 50)
            awaitIdle()

            assertEquals(1, enterEvents)
            assertEquals(1, exitEvents)
        } finally {
            window.dispose()
        }
    }
}