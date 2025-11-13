/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.ui.interaction

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.assertVisibleInContainer
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.firstNodeOrNull
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.hold
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIPasteboard

class BasicInteractionTest {
    /**
     *  Distance in pixels a touch can wander before we think the user is scrolling.
     *  https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/uikitMain/kotlin/androidx/compose/ui/platform/Constants.uikit.kt#L22
     */
    private val CUPERTINO_TOUCH_SLOP = 10.dp

    @Test
    fun testButtonClick() = runUIKitInstrumentedTest {
        var clicks = 0
        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = { clicks++ },
                    modifier = Modifier
                        .testTag("Button")
                        .align(Alignment.Center)
                ) {
                    Text("Click me")
                }
            }
        }

        assertEquals(0, clicks)
        this.findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(1, clicks)
        this.findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(2, clicks)
        this.findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(3, clicks)
    }

    @Test
    fun testScroll() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRect(DpOffset.Zero, DpSize.Zero)
        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Red)
                        .testTag("Hidden after scroll box")
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Green)
                    .testTag("Box")
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenSize.height)
                        .background(Color.White)
                )
            }
        }

        touchDown(screenSize.center)
            .dragBy(dy = -(100.dp + CUPERTINO_TOUCH_SLOP))

        waitForIdle()

        assertEquals(100 * density.density, state.value.toFloat())
        assertEquals(DpRect(DpOffset.Zero, DpSize(screenSize.width, 100.dp)), boxRect)
    }

    @Test
    fun testBasicTextFieldToolbar() = runContextMenuTest(false) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField("Hello-LongLongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextField2Toolbar() = runContextMenuTest(false) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextFieldToolbarNewContextMenu() = runContextMenuTest(true) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                TextField("Hello-LongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextField2ToolbarNewContextMenu() = runContextMenuTest(true) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun textBasicTextFieldToolbarInteraction() = runUIKitInstrumentedTest {
        val textFieldValue = mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = Modifier.testTag("TextField")
                )
            }
        }

        fun MutableState<TextFieldValue>.isFullySelected(): Boolean =
            value.selection.start == 0 && value.selection.end == value.text.length

        openToolbar(textFieldTag = "TextField")

        waitForContextMenu()
        assertFalse(textFieldValue.isFullySelected())

        tapContextMenuButton("Select All")

        waitUntil("Text field should be fully selected") {
            textFieldValue.isFullySelected()
        }
    }

    @Test
    fun textBasicTextField2ToolbarInteraction() = runUIKitInstrumentedTest {
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        fun TextFieldState.isFullySelected(): Boolean =
            selection.start == 0 && selection.end == text.length

        openToolbar(textFieldTag = "TextField")

        waitForContextMenu()
        assertFalse(textFieldState.isFullySelected())

        tapContextMenuButton("Select All")

        waitUntil("Text field should be fully selected") {
            textFieldState.isFullySelected()
        }
    }

    private fun UIKitInstrumentedTest.openToolbar(textFieldTag: String) {
        findNodeWithTag("TextField").tap()
        delay(500)
        findNodeWithTag("TextField").doubleTap()
        waitForContextMenu()
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun runContextMenuTest(
        newContextMenuEnabled: Boolean,
        testBlock: UIKitInstrumentedTest.() -> Unit
    ) = runUIKitInstrumentedTest {
        val oldValue = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = newContextMenuEnabled
        try {
            testBlock()
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = oldValue
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIKitInstrumentedTest.verifyFullToolbarPresent() {
        findNodeWithLabel("Cut").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Copy").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Paste").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Select All").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
    }

    private fun UIKitInstrumentedTest.tapContextMenuButton(label: String) {
        if (available(OS.Ios to OSVersion(16))) {
            findNodeWithLabel(label).tap()
        } else {
            // Because on iOS < 16 the context menu is shown in a separate window,
            // it's not fully interactive with the default Tap action.
            findNodeWithLabel(label)
                .touchDown(useNodeWindow = true)
                .hold()
                .also { delay(100) }
                .up()
        }
    }

    private fun UIKitInstrumentedTest.waitForContextMenu() {
        val menuClassName = if (available(OS.Ios to OSVersion(16))) {
            "_UIEditMenuContainerView"
        } else {
            "UICalloutBar"
        }
        waitForIdle()
        waitUntil {
            firstNodeOrNull { node ->
                node.element?.let { it::class.simpleName } == menuClassName
            } != null
        }
        // Additional delay to wait until toolbar animation ends
        delay(500)
    }
}
