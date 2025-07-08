/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.interaction

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.assertVisibleInContainer
import androidx.compose.ui.test.findNodeOrNull
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(1, clicks)
        findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(2, clicks)
        findNodeWithLabel(label = "Click me")
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

    @Ignore // https://youtrack.jetbrains.com/issue/CMP-8537/Fix-toolbar-tests
    @Test
    fun testBasicTextFieldToolbar() = runUIKitInstrumentedTest {
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField("Hello-LongLongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").doubleTap()

        verifyFullToolbarPresent()
    }

    @Ignore // https://youtrack.jetbrains.com/issue/CMP-8537/Fix-toolbar-tests
    @Test
    fun testBasicTextField2Toolbar() = runUIKitInstrumentedTest {
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").doubleTap()

        verifyFullToolbarPresent()
    }

    @Ignore // https://youtrack.jetbrains.com/issue/CMP-8537/Fix-toolbar-tests
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testBasicTextFieldToolbarNewContextMenu() = runUIKitInstrumentedTest {
        ComposeFoundationFlags.isNewContextMenuEnabled = true
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                TextField("Hello-LongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").doubleTap()

        verifyFullToolbarPresent()
        ComposeFoundationFlags.isNewContextMenuEnabled = false
    }

    @Ignore // https://youtrack.jetbrains.com/issue/CMP-8537/Fix-toolbar-tests
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testBasicTextField2ToolbarNewContextMenu() = runUIKitInstrumentedTest {
        ComposeFoundationFlags.isNewContextMenuEnabled = true
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").doubleTap()

        verifyFullToolbarPresent()
        ComposeFoundationFlags.isNewContextMenuEnabled = false
    }

    private fun UIKitInstrumentedTest.verifyFullToolbarPresent() {
        // Verify elements from context menu present
        waitForContextMenu()

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

    private fun UIKitInstrumentedTest.waitForContextMenu() {
        waitForIdle()
        waitUntil {
            findNodeOrNull { node ->
                node.element?.let { it::class.simpleName } == "_UIEditMenuContainerView"
            } != null
        }
    }
}
