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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awaitEDT
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.google.common.truth.Truth.assertThat
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.FocusEvent
import java.awt.event.FocusEvent.Cause
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Assume.assumeFalse

class ComposeFocusTest {

    @Test
    fun `compose window`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton1.Button()
                    composeButton2.Button()
                    composeButton3.Button()
                    composeButton4.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton1, composeButton2, composeButton3, composeButton4
        )
    }

    @Test
    fun `compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(
            ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton1.Button()
                            composeButton2.Button()
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                }
            }
        )
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton1, composeButton2, composeButton3, composeButton4
        )
    }

    @Test
    fun `compose panel in the end`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(outerButton1)
            add(outerButton2)

            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton1.Button()
                            composeButton2.Button()
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                }
            })
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            outerButton1, outerButton2, composeButton1, composeButton2, composeButton3, composeButton4
        )
    }

    @Test
    fun `compose panel in the beginning`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton1.Button()
                            composeButton2.Button()
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                }
            })
            add(outerButton3)
            add(outerButton4)
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton1, composeButton2, composeButton3, composeButton4, outerButton3, outerButton4
        )
    }

    @Test
    fun `swing panel in the middle of compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton3.Button()
                            SwingPanel(
                                modifier = Modifier.size(100.dp),
                                factory = {
                                    javax.swing.Box.createVerticalBox().apply {
                                        add(innerButton1)
                                    }
                                }
                            )
                            composeButton4.Button()
                        }
                    }
                }
            })
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, innerButton1, composeButton4,
        )
    }

    @Test
    fun `swing panel in the middle of compose window`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton3.Button()
                    SwingPanel(
                        modifier = Modifier.size(100.dp),
                        factory = {
                            javax.swing.Box.createVerticalBox().apply {
                                add(innerButton1)
                            }
                        }
                    )
                    composeButton4.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, innerButton1, composeButton4
        )
    }

    @Test
    fun `swing panel in the end of compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(outerButton1)
            add(outerButton2)

            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton1.Button()
                            composeButton2.Button()
                            SwingPanel(
                                modifier = Modifier.size(100.dp),
                                factory = {
                                    javax.swing.Box.createVerticalBox().apply {
                                        add(innerButton1)
                                        add(innerButton2)
                                        add(innerButton3)
                                    }
                                }
                            )
                        }
                    }
                }
            })
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            outerButton1, outerButton2, composeButton1, composeButton2, innerButton1,
            innerButton2, innerButton3
        )
    }

    @Test
    fun `swing panel in the beginning of compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            SwingPanel(
                                modifier = Modifier.size(100.dp),
                                factory = {
                                    javax.swing.Box.createVerticalBox().apply {
                                        add(innerButton1)
                                        add(innerButton2)
                                        add(innerButton3)
                                    }
                                }
                            )
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                }
            })
            add(outerButton3)
            add(outerButton4)
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            innerButton1,
            innerButton2, innerButton3, composeButton3, composeButton4, outerButton3, outerButton4
        )
    }

    @Test
    fun `swing panel without compose and outer buttons`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            SwingPanel(
                                modifier = Modifier.size(100.dp),
                                factory = {
                                    javax.swing.Box.createVerticalBox().apply {
                                        add(innerButton1)
                                        add(innerButton2)
                                        add(innerButton3)
                                    }
                                }
                            )
                        }
                    }
                }
            })
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(innerButton1, innerButton2, innerButton3)
    }

    @Test
    fun `empty compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(outerButton1)
            add(outerButton2)
            add(ComposePanel().apply {
                setContent {
                }
            })
            add(outerButton3)
            add(outerButton4)
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(outerButton1, outerButton2, outerButton3, outerButton4)
    }

    @Test
    fun `empty swing panel`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton3.Button()
                    SwingPanel(
                        modifier = Modifier.size(100.dp),
                        factory = {
                            javax.swing.Box.createVerticalBox()
                        }
                    )
                    composeButton4.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, composeButton4
        )
    }

    @Test
    fun `only empty swing panel in a compose window`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            SwingPanel(
                modifier = Modifier.size(100.dp).offset(50.dp, 50.dp),
                factory = { JPanel() }
            )
        }
        //window.pack()
        window.isVisible = true

        // no assert, just check that there is no crash or StackOverflow
    }

    @Test
    fun `only empty swing panel in a compose panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    SwingPanel(
                        modifier = Modifier.size(100.dp),
                        factory = {
                            javax.swing.Box.createVerticalBox()
                        }
                    )
                }
            })
        })
        window.pack()
        window.isVisible = true

        // no assert, just check that there is no crash or StackOverflow
    }

    @Test
    fun `popup inside window`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton1.Button()
                    composeButton2.Button()
                    Popup(focusable = true) {
                        composeButton3.Button()
                        composeButton4.Button()
                    }
                    composeButton5.Button()
                    composeButton6.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, composeButton4
        )
    }

    @Test
    fun `popup inside panel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(outerButton1)
            add(outerButton2)

            add(ComposePanel().apply {
                setContent {
                    MaterialTheme {
                        Column(Modifier.fillMaxSize()) {
                            composeButton1.Button()
                            composeButton2.Button()
                            Popup(focusable = true) {
                                composeButton3.Button()
                                composeButton4.Button()
                            }
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                }
            })

            add(outerButton3)
            add(outerButton4)
        })
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, composeButton4
        )
    }

    @Test
    fun `popup with swingPanel`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton1.Button()
                    composeButton2.Button()
                    Popup(focusable = true) {
                        Column(Modifier.fillMaxSize()) {
                            composeButton3.Button()
                            SwingPanel(
                                modifier = Modifier.size(100.dp),
                                factory = {
                                    javax.swing.Box.createVerticalBox().apply {
                                        add(innerButton1)
                                    }
                                }
                            )
                            composeButton4.Button()
                        }
                    }
                    composeButton5.Button()
                    composeButton6.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, innerButton1, composeButton4
        )
    }

    @Test
    fun `popup with swingPanel before it`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton1.Button()
                    composeButton2.Button()
                    SwingPanel(
                        modifier = Modifier.size(100.dp),
                        factory = {
                            javax.swing.Box.createVerticalBox().apply {
                                add(innerButton1)
                            }
                        }
                    )
                    Popup(focusable = true) {
                        Column(Modifier.fillMaxSize()) {
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                    composeButton5.Button()
                    composeButton6.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, composeButton4
        )
    }

    @Test
    fun `popup with swingPanel after it`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    composeButton1.Button()
                    composeButton2.Button()
                    Popup(focusable = true) {
                        Column(Modifier.fillMaxSize()) {
                            composeButton3.Button()
                            composeButton4.Button()
                        }
                    }
                    SwingPanel(
                        modifier = Modifier.size(100.dp),
                        factory = {
                            javax.swing.Box.createVerticalBox().apply {
                                add(innerButton1)
                            }
                        }
                    )
                    composeButton5.Button()
                    composeButton6.Button()
                }
            }
        }
        window.pack()
        window.isVisible = true

        testRandomFocus(
            composeButton3, composeButton4
        )
    }

    @Test
    fun `initial focus in ComposeWindow`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.setContent {
            composeButton1.Button()
        }
        window.pack()
        window.isVisible = true

        assertThat(composeButton1.isFocused).isFalse()

        awaitEdtAfterDelay()
        pressNextFocusKey()
        awaitEdtAfterDelay()
        assertThat(composeButton1.isFocused).isTrue()
    }

    @Test
    fun `initial focus in ComposePanel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent {
                    composeButton1.Button()
                }
            })
        })
        window.pack()
        window.isVisible = true

        assertThat(composeButton1.isFocused).isFalse()

        awaitEdtAfterDelay()
        pressNextFocusKey()
        awaitEdtAfterDelay()
        assertThat(composeButton1.isFocused).isTrue()
    }

    @Test
    fun `change focus in empty ComposeWindow`() = runFocusTest {
        val window = ComposeWindow().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)
        window.setContent { }
        window.pack()
        window.isVisible = true

        awaitEdtAfterDelay()
        pressNextFocusKey()
    }

    @Test
    fun `change focus in empty ComposePanel`() = runFocusTest {
        val window = JFrame().disposeOnEnd()
        window.preferredSize = Dimension(500, 500)

        window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
            add(ComposePanel().apply {
                setContent { }
            })
        })
        window.pack()
        window.isVisible = true

        awaitEdtAfterDelay()
        pressNextFocusKey()
    }

    // Feature https://github.com/JetBrains/compose-multiplatform/issues/3888
    @Test
    fun `requestFocus assigns focus to first focusable element in ComposePanel`() = runFocusTest {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val composePanel = ComposePanel()
        composePanel.setBounds(0, 25, 100, 100)
        composePanel.setContent {
            Column {
                composeButton1.Button()
                composeButton2.Button()
            }
        }

        val frame = JFrame().disposeOnEnd()
        frame.size = Dimension(500, 500)
        frame.contentPane.add(outerButton1, BorderLayout.NORTH)
        frame.contentPane.add(composePanel, BorderLayout.CENTER)
        frame.isVisible = true

        awaitEdtAfterDelay()
        assertEquals(outerButton1, focusedComponent)

        // The first requestFocus sends a focusGained(Cause.ACTIVATION) event
        composePanel.requestFocus()
        awaitEdtAfterDelay()
        assertEquals(composeButton1, focusedComponent)

        // Switch focus back to Swing
        outerButton1.requestFocus()
        awaitEdtAfterDelay()
        assertEquals(outerButton1, focusedComponent)

        // The 2nd requestFocus sends a focusGained(Cause.UNKNOWN) event; we want to test both
        composePanel.requestFocus()
        awaitEdtAfterDelay()
        assertEquals(composeButton1, focusedComponent)
    }

    // Feature for changing the current behavior https://github.com/JetBrains/compose-multiplatform/issues/4917
    @Test
    fun `requestFocus doesn't assign focus to first focusable element in ComposeWindow`() = runFocusTest {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val frame = ComposeWindow().disposeOnEnd()
        frame.size = Dimension(500, 500)
        frame.setContent {
            Column {
                composeButton1.Button()
                composeButton2.Button()
            }
        }
        frame.isVisible = true

        awaitEdtAfterDelay()
        assertEquals(null, focusedComponent)
    }

    @Test
    fun `requestFocus shouldn't change already focused element`() = runFocusTest {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val composePanel = ComposePanel()
        composePanel.setBounds(0, 25, 100, 100)
        composePanel.setContent {
            composeButton1.Button()
            composeBox1.Box {
                composeButton2.Button()
            }
        }

        val frame = JFrame().disposeOnEnd()
        frame.size = Dimension(500, 500)
        frame.contentPane.add(composePanel, BorderLayout.CENTER)
        frame.isVisible = true

        composeBox1.requestFocus()
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        composePanel.requestFocus(Cause.UNKNOWN)
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        composePanel.requestFocus(Cause.ACTIVATION)
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        composePanel.requestFocus(Cause.TRAVERSAL_FORWARD)
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        composePanel.requestFocus(Cause.TRAVERSAL_BACKWARD)
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)
    }

    // Bug https://github.com/JetBrains/compose-multiplatform/issues/4919
    @Test
    fun `temporary deactivation shouldn't reset focus`() = runFocusTest {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val composePanel = ComposePanel()
        composePanel.setBounds(0, 25, 100, 100)
        composePanel.setContent {
            composeButton1.Button()
            composeBox1.Box {
                composeButton2.Button()
            }
        }

        val frame = JFrame().disposeOnEnd()
        frame.size = Dimension(500, 500)
        frame.contentPane.add(composePanel, BorderLayout.CENTER)
        frame.isVisible = true

        composeBox1.requestFocus()
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        // show a second window, so the first loses focus with "event.isTemporary = true"
        val temporaryFrame = JFrame().disposeOnEnd()
        temporaryFrame.size = Dimension(500, 500)
        temporaryFrame.isVisible = true
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)

        temporaryFrame.dispose()
        awaitEdtAfterDelay()
        assertEquals(composeBox1, focusedComponent)
    }

    @Test
    fun `custom focused component at start`() = runFocusTest {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val frame = ComposeWindow().disposeOnEnd()
        frame.setContent {
            composeButton1.Button()
            composeButton2.Button()
            composeButton3.Button()

            LaunchedEffect(Unit) {
                composeButton2.requestFocus()
            }
        }
        frame.size = Dimension(500, 500)
        frame.isVisible = true

        awaitEdtAfterDelay()
        assertEquals(composeButton2, focusedComponent)
    }

    private val components = mutableListOf<TestComponent>()
    private val focusedComponent: TestComponent?
        get() {
            val focused = components.filter { it.isFocused }
            check(focused.size <= 1) {
                "Too many components are focused: $focused"
            }
            return focused.firstOrNull()
        }

    private val outerButton1 = TestJButton("outerButton1").also(components::add)
    private val outerButton2 = TestJButton("outerButton2").also(components::add)
    private val outerButton3 = TestJButton("outerButton3").also(components::add)
    private val outerButton4 = TestJButton("outerButton4").also(components::add)
    private val innerButton1 = TestJButton("innerButton1").also(components::add)
    private val innerButton2 = TestJButton("innerButton2").also(components::add)
    private val innerButton3 = TestJButton("innerButton3").also(components::add)
    private val composeButton1 = ComposeComponent("composeButton1").also(components::add)
    private val composeButton2 = ComposeComponent("composeButton2").also(components::add)
    private val composeButton3 = ComposeComponent("composeButton3").also(components::add)
    private val composeButton4 = ComposeComponent("composeButton4").also(components::add)
    private val composeButton5 = ComposeComponent("composeButton5").also(components::add)
    private val composeButton6 = ComposeComponent("composeButton6").also(components::add)
    private val composeBox1 = ComposeComponent("composeBox1").also(components::add)

    private suspend fun FocusTestScope.testRandomFocus(vararg buttons: TestComponent) {
        suspend fun cycleForward() {
            var focusedIndex = buttons.indexOfFirst { it.isFocused }
            println("cycleForward from ${buttons[focusedIndex]}")

            repeat(2 * buttons.size) {
                pressNextFocusKey()
                focusedIndex = (focusedIndex + 1).mod(buttons.size)
                assertEquals(buttons[focusedIndex], focusedComponent)
            }
        }

        suspend fun cycleBackward() {
            var focusedIndex = buttons.indexOfFirst { it.isFocused }
            println("cycleBackward from ${buttons[focusedIndex]}")

            repeat(2 * buttons.size) {
                pressPreviousFocusKey()
                focusedIndex = (focusedIndex - 1).mod(buttons.size)
                assertEquals(buttons[focusedIndex], focusedComponent)
            }
        }

        suspend fun cycleRandom() {
            var focusedIndex = buttons.indexOfFirst { it.isFocused }
            println("cycleRandom from ${buttons[focusedIndex]}")

            repeat(4 * buttons.size) {
                @Suppress("LiftReturnOrAssignment")
                if (Random.nextBoolean()) {
                    pressNextFocusKey()
                    focusedIndex = (focusedIndex + 1).mod(buttons.size)
                } else {
                    pressPreviousFocusKey()
                    focusedIndex = (focusedIndex - 1).mod(buttons.size)
                }
                assertEquals(buttons[focusedIndex], focusedComponent)
            }
        }

        suspend fun randomRequest() {
            println("randomRequest")

            val button = buttons.toList().random()
            button.requestFocus()
            awaitEdtAfterDelay()
            assertEquals(button, focusedComponent)
        }

        suspend fun randomPress() {
            println("randomPress")

            val button = buttons.filterIsInstance<TestJButton>().randomOrNull()
            if (button != null) {
                button.performClick()
                awaitEdtAfterDelay()
                assertEquals(button, focusedComponent)
            }
        }

        awaitEdtAfterDelay()
        println("firstButton")
        buttons.first().requestFocus()
        awaitEdtAfterDelay()
        assertEquals(buttons.first(), focusedComponent)

        repeat(10) {
            when (Random.nextInt(5)) {
                0 -> cycleForward()
                1 -> cycleBackward()
                2 -> cycleRandom()
                3 -> randomRequest()
                4 -> randomPress()
            }
        }
    }
}

fun runFocusTest(action: suspend FocusTestScope.() -> Unit) {
    assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
    runBlocking(MainUIDispatcher) {
        val scope = FocusTestScope()
        try {
            scope.action()
        } finally {
            scope.onEnd()
        }
    }
}

class FocusTestScope {
    private val windows = mutableListOf<Window>()

    fun <T : Window> T.disposeOnEnd() : T {
        windows.add(this)
        return this
    }

    fun onEnd() {
        windows.forEach { it.dispose() }
        windows.clear()
    }

    suspend fun pressNextFocusKey() {
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_TAB, '\t'))
        focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_TAB, '\t'))
        awaitEdtAfterDelay()
    }

    suspend fun pressPreviousFocusKey() {
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, 0, KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_TAB, '\t'))
        focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_RELEASED, 0, KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_TAB, '\t'))
        awaitEdtAfterDelay()
    }
}

interface TestComponent {
    fun requestFocus()
    val isFocused: Boolean
}

private class ComposeComponent(val name: String): TestComponent {
    override var isFocused = false
    private val focusRequester = FocusRequester()
    override fun requestFocus() = focusRequester.requestFocus()
    override fun toString() = name

    val modifier = Modifier
        .onFocusChanged { isFocused = it.isFocused }
        .focusRequester(focusRequester)
}

@Composable
private fun ComposeComponent.Button() {
    Button({}, modifier = modifier) {}
}

@Composable
private fun ComposeComponent.Box(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.focusTarget(), content = content)
}

private class TestJButton(name: String) : JButton(name), TestComponent {
    override val isFocused: Boolean get() = hasFocus()
    override fun toString(): String = text
}

private suspend fun awaitEdtAfterDelay() {
    delay(100)
    awaitEDT()
}