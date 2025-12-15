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

package androidx.compose.ui.keyboard

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIInputView
import platform.UIKit.UIInputViewStyle
import platform.UIKit.UIKeyboardAppearanceDark
import platform.UIKit.UIKeyboardAppearanceDefault
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIKeyboardTypeEmailAddress
import platform.UIKit.UIKeyboardTypeURL
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentTypeDateTime
import platform.UIKit.UITextContentTypeEmailAddress
import platform.UIKit.UITextContentTypePassword
import platform.UIKit.UITextContentTypeTelephoneNumber
import platform.UIKit.UITextContentTypeUsername
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UIView
import platform.UIKit.UIWritingToolsBehaviorDefault
import platform.UIKit.UIWritingToolsBehaviorLimited
import platform.UIKit.inputAccessoryView
import platform.UIKit.inputView

internal class ImeOptionsTest {
    @Test
    fun testKeyboardTypeDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(UIKeyboardTypeDefault, input.keyboardType)
    }

    @Test
    fun testKeyboardType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { keyboardType(UIKeyboardTypeURL) }
        )
        assertEquals(UIKeyboardTypeURL, input.keyboardType)
    }

    @Test
    fun testKeyboardAppearanceDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(UIKeyboardAppearanceDefault, input.keyboardAppearance)
    }

    @Test
    fun testKeyboardAppearance() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { keyboardAppearance(UIKeyboardAppearanceDark) }
        )
        assertEquals(UIKeyboardAppearanceDark, input.keyboardAppearance)
    }

    @Test
    fun testReturnKeyTypeDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(UIReturnKeyType.UIReturnKeyDefault, input.returnKeyType)
    }

    @Test
    fun testReturnKeyType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { returnKeyType(UIReturnKeyType.UIReturnKeyGo) }
        )
        assertEquals(UIReturnKeyType.UIReturnKeyGo, input.returnKeyType)
    }

    @Test
    fun testContentTypeDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertNull(input.textContentType)
    }

    @Test
    fun testContentType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { textContentType(UITextContentTypeDateTime) }
        )
        assertEquals(UITextContentTypeDateTime, input.textContentType)
    }

    @Test
    fun testContentTypeForCommonKeyboardTypePassword() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        assertEquals(UITextContentTypePassword, input.textContentType)
    }

    @Test
    fun testContentTypeForCommonKeyboardTypeEmail() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        assertEquals(UITextContentTypeEmailAddress, input.textContentType)
    }

    @Test
    fun testContentTypeForCommonKeyboardTypePhone() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        assertEquals(UITextContentTypeTelephoneNumber, input.textContentType)
    }

    @Test
    fun testIsSecureTextEntryDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertFalse(input.isSecureTextEntry())
    }

    @Test
    fun testIsSecureTextEntryFalse() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { isSecureTextEntry(false) }
        )
        assertFalse(input.isSecureTextEntry())
    }

    @Test
    fun testIsSecureTextEntryTrue() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { isSecureTextEntry(true) }
        )
        assertTrue(input.isSecureTextEntry())
    }

    @Test
    fun testIsSecureTextEntryForCommonKeyboardTypePassword() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        assertTrue(input.isSecureTextEntry())
    }

    @Test
    fun testIsSecureTextEntryForCommonKeyboardTypeNumberPassword() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        assertTrue(input.isSecureTextEntry())
    }

    @Test
    fun testEnablesReturnKeyAutomaticallyDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput()
        assertFalse(input.enablesReturnKeyAutomatically)
    }

    @Test
    fun testEnablesReturnKeyAutomaticallyFalse() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { enablesReturnKeyAutomatically(false) }
        )
        assertFalse(input.enablesReturnKeyAutomatically)
    }

    @Test
    fun testEnablesReturnKeyAutomaticallyTrue() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { enablesReturnKeyAutomatically(true) }
        )
        assertTrue(input.enablesReturnKeyAutomatically)
    }

    @Test
    fun testAutocapitalizationTypeDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(
            UITextAutocapitalizationType.UITextAutocapitalizationTypeNone,
            input.autocapitalizationType
        )
    }

    @Test
    fun testAutocapitalizationType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions {
                autocapitalizationType(UITextAutocapitalizationType.UITextAutocapitalizationTypeWords)
            }
        )
        assertEquals(
            UITextAutocapitalizationType.UITextAutocapitalizationTypeWords,
            input.autocapitalizationType
        )
    }

    @Test
    fun testAutocorrectionTypeDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(
            UITextAutocorrectionType.UITextAutocorrectionTypeYes,
            input.autocorrectionType
        )
    }

    @Test
    fun testAutocorrectionType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions {
                autocorrectionType(UITextAutocorrectionType.UITextAutocorrectionTypeNo)
            }
        )
        assertEquals(
            UITextAutocorrectionType.UITextAutocorrectionTypeNo,
            input.autocorrectionType
        )
    }

    @Test
    fun testPlatformOverridesCommonKeyboardType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                platformImeOptions = PlatformImeOptions { keyboardType(UIKeyboardTypeEmailAddress) }
            )
        )
        assertEquals(UIKeyboardTypeEmailAddress, input.keyboardType)
    }

    @Test
    fun testPlatformOverridesCommonReturnKeyType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                platformImeOptions = PlatformImeOptions { returnKeyType(UIReturnKeyType.UIReturnKeyGo) }
            )
        )
        assertEquals(UIReturnKeyType.UIReturnKeyGo, input.returnKeyType)
    }

    @Test
    fun testPlatformOverridesCommonContentType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                platformImeOptions = PlatformImeOptions { textContentType(UITextContentTypeDateTime) }
            )
        )
        assertEquals(UITextContentTypeDateTime, input.textContentType)
    }

    @Test
    fun testPlatformOverridesCommonIsSecureTextEntry() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                platformImeOptions = PlatformImeOptions { isSecureTextEntry(false) }
            )
        )
        assertFalse(input.isSecureTextEntry())
    }

    @Test
    fun testPlatformOverridesCommonAutocapitalizationType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                platformImeOptions = PlatformImeOptions { autocapitalizationType(UITextAutocapitalizationType.UITextAutocapitalizationTypeWords) }
            )
        )
        assertEquals(UITextAutocapitalizationType.UITextAutocapitalizationTypeWords, input.autocapitalizationType)
    }

    @Test
    fun testPlatformOverridesCommonAutocorrectionType() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                platformImeOptions = PlatformImeOptions { autocorrectionType(UITextAutocorrectionType.UITextAutocorrectionTypeYes) }
            )
        )
        assertEquals(UITextAutocorrectionType.UITextAutocorrectionTypeYes, input.autocorrectionType)
    }

    @Test
    fun testInputViewDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInputView(keyboardOptions = KeyboardOptions.Default)

        assertNull(input.inputAccessoryView)
        assertNull(input.inputView)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testInputViewCustom() = runUIKitInstrumentedTest {
        val customInputView = object: UIInputView(frame = CGRectZero.readValue(), inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard) {}

        val input = setContentAndFindInputView(keyboardOptions = KeyboardOptions(
            platformImeOptions = PlatformImeOptions { inputView(customInputView) }
        ))

        assertNull(input.inputAccessoryView)
        assertSame(customInputView, input.inputView)
    }

    @Test
    fun testInputAccessoryViewDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInputView(keyboardOptions = KeyboardOptions.Default)

        assertNull(input.inputView)
        assertNull(input.inputAccessoryView)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testInputAccessoryViewCustom() = runUIKitInstrumentedTest {
        val customInputAccessoryView = object: UIView(frame = CGRectZero.readValue()) {}

        val input = setContentAndFindInputView(keyboardOptions = KeyboardOptions(
            platformImeOptions = PlatformImeOptions { inputAccessoryView(customInputAccessoryView) }
        ))

        assertNull(input.inputView)
        assertSame(customInputAccessoryView, input.inputAccessoryView)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testInputAccessoryViewCustomAndInputViewCustom() = runUIKitInstrumentedTest {
        val customInputView = object: UIInputView(frame = CGRectZero.readValue(), inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard) {}
        val customInputAccessoryView = object: UIView(frame = CGRectZero.readValue()) {}

        val input = setContentAndFindInputView(keyboardOptions = KeyboardOptions(
            platformImeOptions = PlatformImeOptions {
                inputView(customInputView)
                inputAccessoryView(customInputAccessoryView)
            }
        ))

        assertSame(customInputView, input.inputView)
        assertSame(customInputAccessoryView, input.inputAccessoryView)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testTextInputSessionDoesNotRestartWithChangedInput() = runUIKitInstrumentedTest {
        val inputState = mutableStateOf("")
        var startInputCount = 0

        val testTextInputSession = object : PlatformTextInputSession {
                override suspend fun startInputMethod(
                    request: PlatformTextInputMethodRequest
                ): Nothing {
                    startInputCount++
                    suspendCancellableCoroutine<Nothing> { }
                }
            }

        setContent {
            var input by rememberSaveable { inputState }

            InterceptPlatformTextInput(
                interceptor = { request, _ ->
                    testTextInputSession.startInputMethod(request)
                },
                content = {
                    TextField(
                        modifier = Modifier.testTag("TextField"),
                        value = input,
                        onValueChange = { input = it },
                        keyboardOptions = KeyboardOptions(platformImeOptions = PlatformImeOptions {
                            keyboardType(UIKeyboardTypeEmailAddress)
                            textContentType(UITextContentTypeUsername)
                        })
                    )
                }
            )
        }

        findNodeWithTag("TextField").tap()

        waitForIdle()

        assertEquals(1, startInputCount)

        var input = ""
        for (i in 1..4) {
            input += "$i"
            inputState.value = input
            waitForIdle()
            assertEquals(1, startInputCount)
        }
    }

    @Test
    fun testWritingToolsBehaviorDefault() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions()
        )
        assertEquals(UIWritingToolsBehaviorDefault, input.writingToolsBehavior)
    }

    @Test
    fun testWritingToolsBehavior() = runUIKitInstrumentedTest {
        val input = setContentAndFindInput(
            imeOptions = PlatformImeOptions { writingToolsBehavior(UIWritingToolsBehaviorLimited) }
        )
        assertEquals(UIWritingToolsBehaviorLimited, input.writingToolsBehavior)
    }

    private fun UIKitInstrumentedTest.setContentAndFindInputView(
        keyboardOptions: KeyboardOptions
    ): UIView {
        val focusRequester = FocusRequester()

        setContent {
            TextField(
                value = "",
                onValueChange = {},
                keyboardOptions = keyboardOptions,
                modifier = Modifier.focusRequester(focusRequester)
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        
        return assertNotNull(findFirstUITextInput())
    }

    private fun UIKitInstrumentedTest.setContentAndFindInput(
        keyboardOptions: KeyboardOptions
    ): UITextInputProtocol {
        setContentAndFindInputView(keyboardOptions = keyboardOptions)
        return assertNotNull(findFirstUITextInput() as? UITextInputProtocol)
    }

    private fun UIKitInstrumentedTest.setContentAndFindInput(
        imeOptions: PlatformImeOptions? = null
    ): UITextInputProtocol = setContentAndFindInput(keyboardOptions = KeyboardOptions(platformImeOptions = imeOptions))

    private fun UIKitInstrumentedTest.findFirstUITextInput(): UIView? {
        val windowScene = viewController.view.window?.windowScene ?: return null

        fun traverseSubviews(view: UIView): UIView? {
            if (view as? UITextInputProtocol != null) {
                return view
            }

            view.subviews.forEach {
                traverseSubviews(it as UIView)?.let { return it }
            }

            return null
        }

        return windowScene.windows.reversed().firstNotNullOfOrNull {
            traverseSubviews(view = it as UIView)
        }
    }
}