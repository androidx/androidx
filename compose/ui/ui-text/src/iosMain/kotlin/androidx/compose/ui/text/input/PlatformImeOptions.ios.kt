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

package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardAppearanceDefault
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UIView
import platform.UIKit.UIWritingToolsBehavior
import platform.UIKit.UIWritingToolsBehaviorDefault

@Immutable
private data class PlatformImeOptionsImpl(
    val keyboardType: UIKeyboardType?,
    val keyboardAppearance: UIKeyboardAppearance,
    val returnKeyType: UIReturnKeyType?,
    val textContentType: UITextContentType,
    val isSecureTextEntry: Boolean?,
    val enablesReturnKeyAutomatically: Boolean,
    val autocapitalizationType: UITextAutocapitalizationType?,
    val autocorrectionType: UITextAutocorrectionType?,
    val hasExplicitTextContentType: Boolean,
    val inputView: UIView?,
    val inputAccessoryView: UIView?,
    val writingToolsBehavior: UIWritingToolsBehavior,
): PlatformImeOptions()

/**
 * Configuration for creating instances of PlatformImeOptions.
 */
@ExperimentalComposeUiApi
class PlatformImeOptionsConfiguration internal constructor() {
    private var keyboardType: UIKeyboardType? = null
    private var keyboardAppearance: UIKeyboardAppearance = UIKeyboardAppearanceDefault
    private var returnKeyType: UIReturnKeyType? = null
    private var textContentType: UITextContentType? = null
    private var isSecureTextEntry: Boolean? = null
    private var enablesReturnKeyAutomatically: Boolean = false
    private var autocapitalizationType: UITextAutocapitalizationType? = null
    private var autocorrectionType: UITextAutocorrectionType? = null
    private var hasExplicitTextContentType: Boolean = false
    private var inputView: UIView? = null
    private var inputAccessoryView: UIView? = null
    private var writingToolsBehavior: UIWritingToolsBehavior = UIWritingToolsBehaviorDefault
    /**
     * Sets the keyboard type to be used for the text input field.
     * If not set, the value will be derived from [ImeOptions].
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/keyboardtype).
     */
    @ExperimentalComposeUiApi
    fun keyboardType(value: UIKeyboardType?): PlatformImeOptionsConfiguration = apply {
        keyboardType = value
    }

    /**
     * Sets the appearance of the keyboard.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/keyboardappearance).
     */
    @ExperimentalComposeUiApi
    fun keyboardAppearance(value: UIKeyboardAppearance): PlatformImeOptionsConfiguration = apply {
        keyboardAppearance = value
    }

    /**
     * Sets the type of return key to display on the keyboard.
     * If not set, the value will be derived from [ImeOptions].
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/returnkeytype).
     */
    @ExperimentalComposeUiApi
    fun returnKeyType(value: UIReturnKeyType?): PlatformImeOptionsConfiguration = apply {
        returnKeyType = value
    }

    /**
     * Sets the semantic meaning of the text being entered.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/textcontenttype).
     */
    @ExperimentalComposeUiApi
    fun textContentType(value: UITextContentType): PlatformImeOptionsConfiguration = apply {
        textContentType = value
        hasExplicitTextContentType = true
    }

    /**
     * Sets whether the text being entered should be treated as secure input.
     * If not set, the value will be derived from [ImeOptions].
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/issecuretextentry).
     */
    @ExperimentalComposeUiApi
    fun isSecureTextEntry(value: Boolean?): PlatformImeOptionsConfiguration = apply {
        isSecureTextEntry = value
    }

    /**
     * Sets whether the return key should be automatically enabled when text is present.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/enablesreturnkeyautomatically).
     */
    @ExperimentalComposeUiApi
    fun enablesReturnKeyAutomatically(value: Boolean): PlatformImeOptionsConfiguration = apply {
        enablesReturnKeyAutomatically = value
    }

    /**
     * Sets the autocapitalization style to apply.
     * If not set, the value will be derived from [ImeOptions].
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/autocapitalizationtype).
     */
    @ExperimentalComposeUiApi
    fun autocapitalizationType(value: UITextAutocapitalizationType?): PlatformImeOptionsConfiguration = apply {
        autocapitalizationType = value
    }

    /**
     * Sets the autocorrection behavior to apply.
     * If not set, the value will be derived from [ImeOptions].
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/autocorrectiontype).
     */
    @ExperimentalComposeUiApi
    fun autocorrectionType(value: UITextAutocorrectionType?): PlatformImeOptionsConfiguration = apply {
        autocorrectionType = value
    }

    /**
     * Sets a custom input view to be presented instead of the system keyboard when IME becomes first responder.
     * Default value is `null`.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uiresponder/inputview)
     */
    @ExperimentalComposeUiApi
    fun inputView(value: UIView?): PlatformImeOptionsConfiguration = apply {
        inputView = value
    }

    /**
     * Sets a custom accessory view to be attached to system keyboard or custom [inputView] when IME becomes first responder.
     * Default value is `null`.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uiresponder/inputaccessoryview)
     */
    @ExperimentalComposeUiApi
    fun inputAccessoryView(value: UIView?): PlatformImeOptionsConfiguration = apply {
        inputAccessoryView = value
    }

    /**
     * Sets the writing tools behavior to apply to the IME.
     *
     * See [UIKit documentation](https://developer.apple.com/documentation/uikit/uitextinputtraits/writingtoolsbehavior)
     */
    @ExperimentalComposeUiApi
    fun writingToolsBehavior(value: UIWritingToolsBehavior): PlatformImeOptionsConfiguration = apply {
        writingToolsBehavior = value
    }

    /**
     * Builds the final PlatformImeOptions instance with the configured values.
     */
    internal fun build(): PlatformImeOptions {
        return PlatformImeOptionsImpl(
            keyboardType = keyboardType,
            keyboardAppearance = keyboardAppearance,
            returnKeyType = returnKeyType,
            textContentType = textContentType,
            isSecureTextEntry = isSecureTextEntry,
            enablesReturnKeyAutomatically = enablesReturnKeyAutomatically,
            autocapitalizationType = autocapitalizationType,
            autocorrectionType = autocorrectionType,
            hasExplicitTextContentType = hasExplicitTextContentType,
            inputView = inputView,
            inputAccessoryView = inputAccessoryView,
            writingToolsBehavior = writingToolsBehavior,
        )
    }
}
/**
 * Used to configure iOS platform IME options.
 *
 * Allows specifying UIKit-specific input method editor options. Note that any values
 * explicitly specified here will override the corresponding settings from the enclosing [ImeOptions].
 */
@ExperimentalComposeUiApi
fun PlatformImeOptions(configure: (PlatformImeOptionsConfiguration.() -> Unit)? = null): PlatformImeOptions {
    val configuration = PlatformImeOptionsConfiguration()
    if (configure != null) {
        configuration.apply(configure)
    }
    return configuration.build()
}

@ExperimentalComposeUiApi
val PlatformImeOptions.keyboardType: UIKeyboardType?
    get() = (this as? PlatformImeOptionsImpl)?.keyboardType

@ExperimentalComposeUiApi
val PlatformImeOptions.keyboardAppearance: UIKeyboardAppearance
    get() = (this as? PlatformImeOptionsImpl)?.keyboardAppearance ?: UIKeyboardAppearanceDefault

@ExperimentalComposeUiApi
val PlatformImeOptions.returnKeyType: UIReturnKeyType?
    get() = (this as? PlatformImeOptionsImpl)?.returnKeyType

@ExperimentalComposeUiApi
val PlatformImeOptions.textContentType: UITextContentType
    get() = (this as? PlatformImeOptionsImpl)?.textContentType

@ExperimentalComposeUiApi
val PlatformImeOptions.isSecureTextEntry: Boolean?
    get() = (this as? PlatformImeOptionsImpl)?.isSecureTextEntry

@ExperimentalComposeUiApi
val PlatformImeOptions.enablesReturnKeyAutomatically: Boolean
    get() = (this as? PlatformImeOptionsImpl)?.enablesReturnKeyAutomatically ?: false

@ExperimentalComposeUiApi
val PlatformImeOptions.autocapitalizationType: UITextAutocapitalizationType?
    get() = (this as? PlatformImeOptionsImpl)?.autocapitalizationType

@ExperimentalComposeUiApi
val PlatformImeOptions.autocorrectionType: UITextAutocorrectionType?
    get() = (this as? PlatformImeOptionsImpl)?.autocorrectionType

@ExperimentalComposeUiApi
val PlatformImeOptions.hasExplicitTextContentType: Boolean
    get() = (this as? PlatformImeOptionsImpl)?.hasExplicitTextContentType ?: false

@ExperimentalComposeUiApi
val PlatformImeOptions.inputView: UIView?
    get() = (this as? PlatformImeOptionsImpl)?.inputView

@ExperimentalComposeUiApi
val PlatformImeOptions.inputAccessoryView: UIView?
    get() = (this as? PlatformImeOptionsImpl)?.inputAccessoryView

@ExperimentalComposeUiApi
val PlatformImeOptions.writingToolsBehavior: UIWritingToolsBehavior
    get() = (this as? PlatformImeOptionsImpl)?.writingToolsBehavior ?: UIWritingToolsBehaviorDefault