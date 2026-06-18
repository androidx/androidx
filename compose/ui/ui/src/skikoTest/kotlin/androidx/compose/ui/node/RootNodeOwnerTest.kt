/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.scene.ComposeSceneInputHandler
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class RootNodeOwnerTest {

    @Test
    fun textTextInputSession() = runTest {
        var sessionStarted = false
        var inputStarted = false
        var inputStopped = false

        @Suppress("DEPRECATION")
        val textInputService = object : androidx.compose.ui.text.input.PlatformTextInputService {
            override fun startInput(
                value: TextFieldValue,
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ) {
            }

            override fun startInput() {
                inputStarted = true
            }

            override fun stopInput() {
                inputStopped = true
            }

            override fun showSoftwareKeyboard() {}
            override fun hideSoftwareKeyboard() {}
            override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {}
        }
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                @Suppress("DEPRECATION")
                override val textInputService: androidx.compose.ui.text.input.PlatformTextInputService = textInputService
                override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
                    sessionStarted = true
                    awaitCancellation()
                }
            }
        )

        val job = CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
            owner.owner.textInputSession {
                startInputMethod(request = TestInputRequest())
            }
        }

        assertTrue(sessionStarted)
        assertTrue(inputStarted)
        assertFalse(inputStopped)

        job.cancel()

        assertTrue(sessionStarted)
        assertTrue(inputStarted)
        assertTrue(inputStopped)
    }

    @Test
    fun textKeyboardShowHide() = runTest {
        var keyboardShowCalled = false
        var keyboardHideCalled = false

        @Suppress("DEPRECATION")
        val textInputService = object : androidx.compose.ui.text.input.PlatformTextInputService {
            override fun startInput(
                value: TextFieldValue,
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ) = error("Should not be called")

            override fun startInput() {}
            override fun stopInput() {}
            override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {}
            override fun showSoftwareKeyboard() {
                keyboardShowCalled = true
            }

            override fun hideSoftwareKeyboard() {
                keyboardHideCalled = true
            }
        }
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                @Suppress("DEPRECATION")
                override val textInputService: androidx.compose.ui.text.input.PlatformTextInputService = textInputService
                override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
                    awaitCancellation()
                }
            }
        )
        owner.owner.softwareKeyboardController.show()

        assertFalse(keyboardShowCalled)
        assertFalse(keyboardHideCalled)

        val job = CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
            owner.owner.textInputSession {
                startInputMethod(request = TestInputRequest())
            }
        }

        owner.owner.softwareKeyboardController.show()

        assertTrue(keyboardShowCalled)
        assertFalse(keyboardHideCalled)

        job.cancel()

        owner.owner.softwareKeyboardController.hide()

        assertTrue(keyboardShowCalled)
        assertTrue(keyboardHideCalled)
    }

    @Test
    fun setSizeOnlyTriggersConstraintChangeWhenValueChanges() = runTest {
        var invalidationCount = 0
        
        val owner = RootNodeOwner(
            invalidate = { invalidationCount++ }
        )

        // Set the initial size
        owner.size = IntSize(100, 100)
        val initialCount = invalidationCount

        // Setting the same size should not trigger invalidation
        owner.size = IntSize(100, 100)
        
        // Count should remain the same
        assertEquals(invalidationCount, initialCount)

        // Setting a different size should trigger invalidation
        owner.size = IntSize(200, 200)
        
        // Count should increase
        assertTrue(invalidationCount > initialCount)
        val afterChangeCount = invalidationCount

        // Setting the same size again should not trigger another invalidation
        owner.size = IntSize(200, 200)
        
        // Count should remain the same
        assertEquals(invalidationCount, afterChangeCount)
    }
}

private fun RootNodeOwner(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    platformContext: PlatformContext = PlatformContext.Empty(),
    invalidate: () -> Unit = {},
) = RootNodeOwner(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    size = null,
    coroutineContext = coroutineContext,
    platformContext = platformContext,
    inputHandler = ComposeSceneInputHandler(
        prepareForPointerInputEvent = {},
        processPointerInputEvent = { PointerEventResult(false) },
        cancelPointerInput = {},
        processKeyEvent = { false },
    ),
    invalidate = invalidate,
    onChangedExecutor = { it() },
)

@ExperimentalComposeUiApi
private class TestInputRequest: PlatformTextInputMethodRequest {
    override val value: () -> TextFieldValue get() = error("Test method")
    override val state: TextEditorState get() = error("Test method")
    override val imeOptions: ImeOptions get() = error("Test method")
    override val onEditCommand: (List<EditCommand>) -> Unit get() = error("Test method")
    override val onImeAction: ((ImeAction) -> Unit)? get() = error("Test method")
    override val textLayoutResult: () -> TextLayoutResult? get() = error("Test method")
    override val focusedRectInRoot: () -> Rect? get() = error("Test method")
    override val textFieldRectInRoot: () -> Rect? get() = error("Test method")
    override val textClippingRectInRoot: () -> Rect? get() = error("Test method")
    override val editText: (TextEditingScope.() -> Unit) -> Unit get() = error("Test method")
    override val unclippedTextOffsetInRoot: () -> Offset? get() = error("Test method")
}