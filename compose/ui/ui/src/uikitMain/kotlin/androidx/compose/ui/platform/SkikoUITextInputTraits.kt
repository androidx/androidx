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

import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardAppearanceDefault
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
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