/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardAppearanceDefault
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIKeyboardTypeASCIICapable
import platform.UIKit.UIKeyboardTypeDecimalPad
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIKeyboardTypeEmailAddress
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIKeyboardTypePhonePad
import platform.UIKit.UIKeyboardTypeURL
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextContentTypeEmailAddress
import platform.UIKit.UITextContentTypePassword
import platform.UIKit.UITextContentTypeTelephoneNumber
import platform.UIKit.UITextSmartDashesType
import platform.UIKit.UITextSmartInsertDeleteType
import platform.UIKit.UITextSmartQuotesType
import platform.UIKit.UITextSpellCheckingType

internal interface SkikoUITextInputTraits {
    fun keyboardType(): UIKeyboardType =
        UIKeyboardTypeDefault

    fun keyboardAppearance(): UIKeyboardAppearance =
        UIKeyboardAppearanceDefault

    fun returnKeyType(): UIReturnKeyType =
        UIReturnKeyType.UIReturnKeyDefault

    fun textContentType(): UITextContentType? =
        null

    fun isSecureTextEntry(): Boolean =
        false

    fun enablesReturnKeyAutomatically(): Boolean =
        false

    fun autocapitalizationType(): UITextAutocapitalizationType =
        UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences

    fun autocorrectionType(): UITextAutocorrectionType =
        UITextAutocorrectionType.UITextAutocorrectionTypeYes

    fun spellCheckingType(): UITextSpellCheckingType =
        UITextSpellCheckingType.UITextSpellCheckingTypeDefault

    fun smartQuotesType(): UITextSmartQuotesType =
        UITextSmartQuotesType.UITextSmartQuotesTypeDefault

    fun smartDashesType(): UITextSmartDashesType =
        UITextSmartDashesType.UITextSmartDashesTypeDefault

    fun smartInsertDeleteType(): UITextSmartInsertDeleteType =
        UITextSmartInsertDeleteType.UITextSmartInsertDeleteTypeDefault

}

internal object EmptyInputTraits : SkikoUITextInputTraits

internal fun getUITextInputTraits(currentImeOptions: ImeOptions?) =
    object : SkikoUITextInputTraits {
        override fun keyboardType(): UIKeyboardType =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Text -> UIKeyboardTypeDefault
                KeyboardType.Ascii -> UIKeyboardTypeASCIICapable
                KeyboardType.Number -> UIKeyboardTypeNumberPad
                KeyboardType.Phone -> UIKeyboardTypePhonePad
                KeyboardType.Uri -> UIKeyboardTypeURL
                KeyboardType.Email -> UIKeyboardTypeEmailAddress
                KeyboardType.Password -> UIKeyboardTypeASCIICapable // TODO Correct?
                KeyboardType.NumberPassword -> UIKeyboardTypeNumberPad // TODO Correct?
                KeyboardType.Decimal -> UIKeyboardTypeDecimalPad
                else -> UIKeyboardTypeDefault
            }

        override fun keyboardAppearance(): UIKeyboardAppearance = UIKeyboardAppearanceDefault
        override fun returnKeyType(): UIReturnKeyType =
            when (currentImeOptions?.imeAction) {
                ImeAction.Default -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.None -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.Go -> UIReturnKeyType.UIReturnKeyGo
                ImeAction.Search -> UIReturnKeyType.UIReturnKeySearch
                ImeAction.Send -> UIReturnKeyType.UIReturnKeySend
                ImeAction.Previous -> UIReturnKeyType.UIReturnKeyDefault
                ImeAction.Next -> UIReturnKeyType.UIReturnKeyNext
                ImeAction.Done -> UIReturnKeyType.UIReturnKeyDone
                else -> UIReturnKeyType.UIReturnKeyDefault
            }

        override fun textContentType(): UITextContentType? =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Password, KeyboardType.NumberPassword -> UITextContentTypePassword
                KeyboardType.Email -> UITextContentTypeEmailAddress
                KeyboardType.Phone -> UITextContentTypeTelephoneNumber
                else -> null
            }

        override fun isSecureTextEntry(): Boolean =
            when (currentImeOptions?.keyboardType) {
                KeyboardType.Password, KeyboardType.NumberPassword -> true
                else -> false
            }

        override fun enablesReturnKeyAutomatically(): Boolean = false

        override fun autocapitalizationType(): UITextAutocapitalizationType =
            when (currentImeOptions?.capitalization) {
                KeyboardCapitalization.None ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeNone

                KeyboardCapitalization.Characters ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters

                KeyboardCapitalization.Words ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeWords

                KeyboardCapitalization.Sentences ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences

                else ->
                    UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            }

        override fun autocorrectionType(): UITextAutocorrectionType =
            when (currentImeOptions?.autoCorrect) {
                true -> UITextAutocorrectionType.UITextAutocorrectionTypeYes
                false -> UITextAutocorrectionType.UITextAutocorrectionTypeNo
                else -> UITextAutocorrectionType.UITextAutocorrectionTypeDefault
            }

    }

