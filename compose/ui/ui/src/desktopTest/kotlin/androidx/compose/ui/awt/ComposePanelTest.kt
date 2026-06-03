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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LayerType
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.sendCharTypedEvents
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.ThrowUncaughtExceptionRule
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import androidx.compose.ui.window.waitForFocusGain
import androidx.savedstate.SavedState
import com.google.common.truth.Truth.assertThat
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
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
import org.junit.rules.Timeout

class ComposePanelTest {
    @get:Rule
    val timeout: Timeout = Timeout.seconds(60)

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

            val composePanel = ComposePanel(
                skiaLayerAnalytics = analytics,
            )
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

    @Test
    fun savedState() = runApplicationTest {
        var savedState: SavedState? = null
        var lastState = 0

        val frame = JFrame()

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

        suspend fun testPanel(savedState: SavedState? = null, verify: (ComposePanel) -> Unit) {
            val composePanel = ComposePanel(savedState = savedState)
            composePanel.setContent { testContent() }
            frame.contentPane.add(composePanel)
            awaitIdle()
            verify(composePanel)
            frame.contentPane.remove(composePanel)
        }

        try {
            frame.isVisible = true

            testPanel { composePanel ->
                assertThat(lastState).isEqualTo(3)

                savedState = composePanel.saveState()
            }

            testPanel(savedState) { _ ->
                assertThat(lastState).isEqualTo(6)
            }
        } finally {
            frame.dispose()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `compose state should not reset on panel remove and add with isDisposeOnRemove = false`() {
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
                delay(1.seconds)
                assertEquals(1, initialStateCounter)

                frame.contentPane.remove(composePanel)
                delay(1.seconds)
                assertEquals(1, initialStateCounter)

                frame.contentPane.add(composePanel)
                delay(1.seconds)
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
                delay(1.seconds)
                assertEquals(1, initialStateCounter)

                frame.contentPane.remove(composePanel)
                delay(1.seconds)
                assertEquals(1, initialStateCounter)

                frame.contentPane.add(composePanel)
                delay(1.seconds)
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
                delay(1.seconds)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.contentPane.remove(composePanel)
                delay(1.seconds)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.contentPane.add(composePanel)
                delay(1.seconds)
                assertEquals(Size(100f * density, 100f * density), size)

                frame.size = Dimension(200, 100)
                delay(1.seconds)
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
                delay(1.seconds)
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
                delay(1.seconds)
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
                delay(1.seconds)
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

    @Test
    fun `ComposePanel clears SwingPanels when removed`() = runApplicationTest {
        val jPanels = mutableListOf<JPanel>()
        val composePanel = ComposePanel()
        composePanel.setContent {
            SwingPanel(
                factory = {
                    JPanel().also {
                        it.size = Dimension(100, 100)
                        jPanels.add(it)
                    }
                },
                modifier = Modifier.size(100.dp)
            )
        }

        val window = JFrame()
        window.size = Dimension(200, 200)
        try {
            window.contentPane.add(composePanel, BorderLayout.CENTER)
            window.isVisible = true
            awaitIdle()
            window.contentPane.remove(composePanel)
            awaitIdle()
            window.contentPane.add(composePanel)
            awaitIdle()

            // Needed to complete the addition of SwingInteropContainer
            composePanel.renderImmediately()

            assertEquals(2, jPanels.size)
            assertFalse(composePanel.isAncestorOf(jPanels[0]))
            assertTrue(composePanel.isAncestorOf(jPanels[1]))
            assertTrue(jPanels[1].isShowing)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `ComposePanel returns non-null preferred size before added to hierarchy`() = runApplicationTest {
        val composePanel = ComposePanel()
        composePanel.setContent {
            Text("Hello")
        }

        assertNotNull(composePanel.preferredSize)
    }

    @Test
    fun focusablePopup_withComponentLayerType_inComposePanel_grabsFocus() {
        ComposeFeatureFlags.layerType.withOverride(LayerType.OnComponent) {
            ComposeFeatureFlags.useSwingGraphicsInComposePanel.withOverride(true) {
                val window = JFrame()
                try {
                    runApplicationTest {
                        var showPopup by mutableStateOf(false)
                        var dismissPopupRequested = false
                        val keyEventsOnParent = mutableListOf<KeyEvent>()

                        val composePanel = ComposePanel()
                        composePanel.setContent {
                            val focusRequester = remember { FocusRequester() }
                            Box(Modifier
                                .size(100.dp)
                                .background(Color.Yellow)
                                .focusRequester(focusRequester)
                                .focusable()
                                .onKeyEvent {
                                    keyEventsOnParent.add(it)
                                }
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            if (showPopup) {
                                Popup(
                                    properties = PopupProperties(
                                        focusable = true,
                                        dismissOnBackPress = true,
                                    ),
                                    onDismissRequest = {
                                        dismissPopupRequested = true
                                    }
                                ) {
                                    Box(Modifier.size(50.dp))
                                }
                            }
                        }

                        composePanel.windowContainer = window.layeredPane

                        window.contentPane.add(composePanel, BorderLayout.CENTER)
                        window.pack()
                        window.isVisible = true
                        composePanel.requestFocusInWindow()

                        awaitIdle()
                        window.sendCharTypedEvents('a')
                        awaitIdle()
                        assertTrue(keyEventsOnParent.isNotEmpty())

                        keyEventsOnParent.clear()
                        showPopup = true
                        awaitIdle()
                        window.sendKeyEvent(java.awt.event.KeyEvent.VK_ESCAPE)
                        awaitIdle()
                        assertTrue(keyEventsOnParent.isEmpty())
                        assertTrue(dismissPopupRequested)
                    }
                } finally {
                    window.dispose()
                }
            }
        }
    }

    @Test
    fun unfocusablePopupLayer_withComponentLayerType_inComposePanel_isSizedCorrectly() {
        ComposeFeatureFlags.layerType.withOverride(LayerType.OnComponent) {
            ComposeFeatureFlags.useSwingGraphicsInComposePanel.withOverride(true) {
                val window = JFrame()
                try {
                    var showPopup by mutableStateOf(false)
                    runApplicationTest {
                        val composePanel = ComposePanel()
                        composePanel.setContent {
                            Box(Modifier
                                .size(200.dp)
                                .background(Color.Yellow)
                            )
                            if (showPopup) {
                                Popup(
                                    properties = PopupProperties(focusable = false),
                                ) {
                                    Box(Modifier
                                        .size(50.dp)
                                        .background(Color.Blue)
                                    )
                                }
                            }
                        }

                        composePanel.windowContainer = window.layeredPane

                        window.contentPane.add(composePanel, BorderLayout.NORTH)
                        window.pack()
                        window.isVisible = true

                        awaitIdle()

                        val initialChildren = window.layeredPane.components
                        showPopup = true
                        awaitIdle()
                        val newLayer = (window.layeredPane.components.toSet() - initialChildren).single()
                        assertEquals(50, newLayer.size.width)
                        assertEquals(50, newLayer.size.height)
                    }
                } finally {
                    window.dispose()
                }
            }
        }
    }

    // https://youtrack.jetbrains.com/issue/CMP-8131/ComposePanel-doesnt-receive-initial-focus-in-JBR
    @Test
    fun `ComposePanel content receives initial focus`() = runApplicationTest {
        val composePanel = ComposePanel()
        var isTextFieldFocused = false
        composePanel.setContent {
            TextField(
                state = rememberTextFieldState(),
                Modifier.onFocusChanged {
                    isTextFieldFocused = it.isFocused
                }
            )
        }

        val window = JFrame()
        try {
            window.size = Dimension(200, 200)
            window.contentPane.add(composePanel, BorderLayout.CENTER)
            window.isVisible = true
            window.toFront()
            window.waitForFocusGain()

            awaitIdle()

            assertTrue(isTextFieldFocused)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `ComposePanel propagates unconsumed mouse wheel scroll events to parent`() =
        runApplicationTest {
            val composePanel = ComposePanel()
            composePanel.preferredSize = Dimension(200, 200)
            composePanel.redispatchUnconsumedMouseWheelEvents = true
            val scrollState = ScrollState(0)
            composePanel.setContent {
                Box(Modifier.size(200.dp).verticalScroll(scrollState).background(Color.Yellow)) {
                    Column(Modifier.fillMaxWidth().height(400.dp)) {
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                    }
                }
            }

            val window = JFrame()
            try {
                window.size = Dimension(200, 200)
                val scrollPane = JScrollPane(
                    JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(composePanel)
                        add(javax.swing.Box.createVerticalStrut(1000), BorderLayout.CENTER)
                    }
                )
                scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                window.contentPane.add(scrollPane, BorderLayout.CENTER)
                window.isVisible = true

                awaitIdle()

                // Scroll a little and check that compose content was scrolled
                window.sendMouseWheelEvent(wheelRotation = 1.0)
                awaitIdle()
                assertThat(scrollState.value).isGreaterThan(0)
                assertThat(scrollPane.viewport.viewPosition.y).isEqualTo(0)

                // Scroll a lot and check that the Swing JScrollPane was scrolled
                // Note that we need two scroll events for now because Compose can't partially consume
                // scroll events. So one event is needed to scroll Compose content to the end, and
                // another one to scroll JScrollPane.
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                println("New scroll position: ${scrollPane.viewport.viewPosition.y}")
                assertThat(scrollPane.viewport.viewPosition.y).isGreaterThan(0)

                // Scroll back to the top to reset the state
                window.sendMouseWheelEvent(wheelRotation = -1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = -1000.0)
                awaitIdle()
                assertThat(scrollState.value).isEqualTo(0)
                assertThat(scrollPane.viewport.viewPosition.y).isEqualTo(0)

                // Now set redispatchUnconsumedMouseWheelEvents = false and check that the scroll
                // event is *not* propagated.
                composePanel.redispatchUnconsumedMouseWheelEvents = false
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                assertThat(scrollPane.viewport.viewPosition.y).isEqualTo(0)
            } finally {
                window.dispose()
            }
        }

    @Test
    fun `ComposePanel propagates unconsumed mouse wheel scroll events to sibling`() =
        runApplicationTest {
            val composePanel = ComposePanel()
            composePanel.redispatchUnconsumedMouseWheelEvents = true
            val scrollState = ScrollState(0)
            composePanel.setContent {
                Box(Modifier.size(200.dp, 300.dp).verticalScroll(scrollState).background(Color.Green)) {
                    Column(Modifier.fillMaxWidth().height(400.dp)) {
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                        Text("Hello World")
                    }
                }
            }

            val container = JPanel(null)
            container.size = Dimension(200, 200)

            val scrollPane = JScrollPane(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(javax.swing.Box.createVerticalStrut(1000), BorderLayout.CENTER)
                }
            )
            scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

            composePanel.size = Dimension(200, 300)
            scrollPane.size = Dimension(200, 400)

            val window = JFrame()
            try {
                window.size = Dimension(200, 400)
                container.add(composePanel)
                container.add(scrollPane)

                window.contentPane.add(container, BorderLayout.CENTER)
                window.isVisible = true

                awaitIdle()

                // Scroll a little and check that compose content was scrolled
                window.sendMouseWheelEvent(wheelRotation = 1.0)
                awaitIdle()
                assertThat(scrollState.value).isGreaterThan(0)

                // Scroll a lot and check that the Swing JScrollPane was scrolled
                // Note that we need two scroll events for now because Compose can't partially consume
                // scroll events. So one event is needed to scroll Compose content to the end, and
                // another one to scroll JScrollPane.
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                assertThat(scrollPane.viewport.viewPosition.y).isGreaterThan(0)

                // Scroll back to the top to reset the state
                window.sendMouseWheelEvent(wheelRotation = -1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = -1000.0)
                awaitIdle()
                assertThat(scrollState.value).isEqualTo(0)
                assertThat(scrollPane.viewport.viewPosition.y).isEqualTo(0)

                // Now set redispatchUnconsumedMouseWheelEvents = false and check that the scroll
                // event is *not* propagated.
                composePanel.redispatchUnconsumedMouseWheelEvents = false
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                awaitIdle()
                window.sendMouseWheelEvent(wheelRotation = 1000.0)
                assertThat(scrollPane.viewport.viewPosition.y).isEqualTo(0)
            } finally {
                window.dispose()
            }
        }

    @Test
    fun testComposePanelClearFocusOnMouseDownEnabled() =
        testComposePanelClearFocusOnMouseDownEnabledFlag(true)

    @Test
    fun testComposePanelClearFocusOnMouseDownDisabled() =
        testComposePanelClearFocusOnMouseDownEnabledFlag(false)

    fun testComposePanelClearFocusOnMouseDownEnabledFlag(enabled: Boolean) = runApplicationTest {
        val focusRequester = FocusRequester()
        var textFieldIsFocused = false

        val window = JFrame()
        try {
            window.contentPane.add(ComposePanel().apply {
                isClearFocusOnMouseDownEnabled = enabled
                setContent {
                    Column(Modifier.size(300.dp, 400.dp)) {
                        BasicTextField(
                            state = rememberTextFieldState(),
                            modifier = Modifier
                                .testTag("textField")
                                .fillMaxWidth()
                                .height(100.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    textFieldIsFocused = it.isFocused
                                }
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        Box(Modifier.testTag("box").fillMaxWidth().weight(1f))
                    }
                }
            })
            window.size = Dimension(300, 400)
            window.isVisible = true

            awaitIdle()

            assertThat(textFieldIsFocused).isTrue()
            window.sendMousePress(x = 100, y = 300)
            window.sendMouseRelease(x = 100, y = 300)
            awaitIdle()

            assertThat(textFieldIsFocused).isEqualTo(!enabled)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun `ComposePanel draws background correctly`() = runApplicationTest {
        // Show a canvas and a `ComposePanel` with the same background and compare the two colors.
        // Simply comparing to the set color doesn't work because Robot returns the color after
        // the OS transforms it to the screen color space (which doesn't seem to be accessible from
        // the JVM).

        val bgColor = java.awt.Color.RED

        val canvas = java.awt.Canvas().apply {
            size = Dimension(300, 300)
            background = bgColor
        }

        val composePanel = ComposePanel().apply {
            size = Dimension(300, 300)
            background = bgColor
        }
        composePanel.setContent { }


        val frame = JFrame().apply {
            contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
            contentPane.add(canvas)
            contentPane.add(composePanel)
            size = Dimension(300, 600)
        }

        try {
            frame.isVisible = true
            awaitIdle()
            val robot = java.awt.Robot()
            val frameBounds = frame.bounds
            val canvasPixel = robot.getPixelColor(frameBounds.centerX.toInt(), (0.25 * frameBounds.centerY).toInt())
            val composePixel = robot.getPixelColor(frameBounds.centerX.toInt(), (0.75 * frameBounds.centerY).toInt())
            assertThat(composePixel).isEqualTo(canvasPixel)
        } finally {
            frame.dispose()
        }
    }

    @Test
    fun `ComposePanel provides LocalAwtWindow`() = runApplicationTest {
        var localWindow: Window? = null
        val composePanel = ComposePanel().apply {
            size = Dimension(300, 300)
            setContent {
                localWindow = LocalAwtWindow.current
            }
        }
        val frame = JFrame().apply {
            contentPane.add(composePanel)
            size = Dimension(300, 300)
        }

        try {
            frame.isVisible = true
            awaitIdle()
            assertNotNull(localWindow)
        } finally {
            frame.dispose()
        }
    }
}
