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

package androidx.compose.ui.interaction

import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.window.Dialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UIView

class TextFieldFocusOrderTest {
    @Test
    fun testModalTextFieldsFocusOnDialogAppear() = runUIKitInstrumentedTest {
        val dialogFocusRequester = FocusRequester()

        setContent {
            TextField("Text 0", {})

            Dialog({}) {
                TextField("Text 1", {}, modifier = Modifier.focusRequester(dialogFocusRequester))
                LaunchedEffect(Unit) {
                    dialogFocusRequester.requestFocus()
                }
            }
        }

        assertEquals("Text 1", findFocusedUITextInput()?.text)
    }

    @Test
    fun testModalTextFieldsRefocus() = runUIKitInstrumentedTest {
        val showDialog1 = mutableStateOf(false)
        val showDialog2 = mutableStateOf(false)
        val focusRequester0 = FocusRequester()
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        setContent {
            TextField("Text 0", {}, modifier = Modifier.focusRequester(focusRequester0))
            LaunchedEffect(Unit) {
                focusRequester0.requestFocus()
            }

            if (showDialog1.value) {
                Dialog({}) {
                    TextField("Text 1", {}, modifier = Modifier.focusRequester(focusRequester1))
                    LaunchedEffect(Unit) {
                        focusRequester1.requestFocus()
                    }
                }
            }

            if (showDialog2.value) {
                Dialog({}) {
                    TextField("Text 2", {}, modifier = Modifier.focusRequester(focusRequester2))
                    LaunchedEffect(Unit) {
                        focusRequester2.requestFocus()
                    }
                }
            }
        }

        assertEquals("Text 0", findFocusedUITextInput()?.text)

        showDialog1.value = true
        waitForIdle()
        assertEquals("Text 1", findFocusedUITextInput()?.text)

        showDialog2.value = true
        waitForIdle()
        assertEquals("Text 2", findFocusedUITextInput()?.text)

        showDialog2.value = false
        waitForIdle()
        assertEquals("Text 1", findFocusedUITextInput()?.text)

        showDialog1.value = false
        waitForIdle()
        assertEquals("Text 0", findFocusedUITextInput()?.text)
    }

    @Test
    fun testModalTextFieldsRefocusWhenParentDismissed() = runUIKitInstrumentedTest {
        val showTextField = mutableStateOf(true)
        val showDialog1 = mutableStateOf(false)
        val showDialog2 = mutableStateOf(false)
        val focusRequester0 = FocusRequester()
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        setContent {
            if (showTextField.value) {
                TextField("Text 0", {}, modifier = Modifier.focusRequester(focusRequester0))
                LaunchedEffect(Unit) {
                    focusRequester0.requestFocus()
                }
            }

            if (showDialog1.value) {
                Dialog({}) {
                    TextField("Text 1", {}, modifier = Modifier.focusRequester(focusRequester1))
                    LaunchedEffect(Unit) {
                        focusRequester1.requestFocus()
                    }
                }
            }

            if (showDialog2.value) {
                Dialog({}) {
                    TextField("Text 2", {}, modifier = Modifier.focusRequester(focusRequester2))
                    LaunchedEffect(Unit) {
                        focusRequester2.requestFocus()
                    }
                }
            }
        }

        showDialog1.value = true
        waitForIdle()
        showDialog2.value = true
        waitForIdle()

        assertEquals("Text 2", findFocusedUITextInput()?.text)

        showTextField.value = false
        waitForIdle()
        assertEquals("Text 2", findFocusedUITextInput()?.text)

        showDialog1.value = false
        waitForIdle()
        assertEquals("Text 2", findFocusedUITextInput()?.text)

        showDialog2.value = false
        waitForIdle()
        assertNull(findFocusedUITextInput())
    }

    private val UITextInputProtocol.text: String? get() {
        val range = textRangeFromPosition(beginningOfDocument, endOfDocument) ?: return null
        return textInRange(range)
    }

    private fun UIKitInstrumentedTest.findFocusedUITextInput(): UITextInputProtocol? {
        val windowScene = hostingViewController.view.window?.windowScene ?: return null

        fun findFirstResponder(view: UIView): UIView? {
            if (view.isFirstResponder) {
                return view
            }
            view.subviews.forEach {
                findFirstResponder(it as UIView)?.let { return it }
            }
            return null
        }

        return windowScene.windows.reversed().firstNotNullOfOrNull {
            findFirstResponder(view = it as UIView)
        } as? UITextInputProtocol
    }
}