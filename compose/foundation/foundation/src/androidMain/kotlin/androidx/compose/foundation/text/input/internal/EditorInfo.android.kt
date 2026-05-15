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

package androidx.compose.foundation.text.input.internal

import android.os.Build
import android.text.InputType
import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.DeleteRangeGesture
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InsertGesture
import android.view.inputmethod.JoinOrSplitGesture
import android.view.inputmethod.RemoveSpaceGesture
import android.view.inputmethod.SelectGesture
import android.view.inputmethod.SelectRangeGesture
import androidx.annotation.RequiresApi
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.core.view.inputmethod.EditorInfoCompat

/** Fills necessary info of EditorInfo. */
internal fun EditorInfo.update(
    text: CharSequence,
    selection: TextRange,
    imeOptions: ImeOptions,
    contentMimeTypes: Array<String>? = null,
) {
    this.imeOptions =
        when (imeOptions.imeAction) {
            ImeAction.Default -> {
                if (imeOptions.singleLine) {
                    // this is the last resort to enable single line
                    // Android IME still shows return key even if multi line is not send
                    // TextView.java#onCreateInputConnection
                    EditorInfo.IME_ACTION_DONE
                } else {
                    EditorInfo.IME_ACTION_UNSPECIFIED
                }
            }
            ImeAction.None -> EditorInfo.IME_ACTION_NONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            else -> error("invalid ImeAction")
        }

    imeOptions.platformImeOptions?.privateImeOptions?.let { privateImeOptions = it }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleListHelper.setHintLocales(this, imeOptions.hintLocales)
    }

    this.inputType =
        when (imeOptions.keyboardType) {
            KeyboardType.Text -> InputType.TYPE_CLASS_TEXT
            KeyboardType.Ascii -> {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
                InputType.TYPE_CLASS_TEXT
            }
            KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
            KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
            KeyboardType.Uri -> InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
            KeyboardType.Email ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            KeyboardType.Password ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            KeyboardType.NumberPassword ->
                InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
            KeyboardType.Decimal ->
                InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
            KeyboardType.PasswordVisible ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            KeyboardType.PostalAddress ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            KeyboardType.PersonName ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME
            KeyboardType.EmailSubject ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT
            KeyboardType.ShortMessage ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE
            KeyboardType.LongMessage ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE
            KeyboardType.Filter ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_FILTER
            KeyboardType.Phonetic ->
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PHONETIC
            KeyboardType.DateTime ->
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_NORMAL
            KeyboardType.Date ->
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            KeyboardType.Time ->
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
            KeyboardType.NumberSigned ->
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            KeyboardType.DecimalSigned ->
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            KeyboardType.DecimalPassword ->
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            KeyboardType.NumberPasswordSigned ->
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            KeyboardType.DecimalPasswordSigned ->
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            else -> error("Invalid Keyboard Type")
        }

    if (!imeOptions.singleLine) {
        if ((this.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            // TextView.java#setInputTypeSingleLine
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (imeOptions.imeAction == ImeAction.Default) {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        }
    }

    if ((this.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
        when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
            KeyboardCapitalization.Words -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            KeyboardCapitalization.Sentences -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            else -> {
                /* do nothing */
            }
        }

        if (imeOptions.autoCorrect) {
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            EditorInfoApi37.setEnableTextSuggestionSelectedInputType(this)
        }
    }

    this.initialSelStart = selection.start
    this.initialSelEnd = selection.end

    EditorInfoCompat.setInitialSurroundingText(this, text)

    if (contentMimeTypes != null) {
        EditorInfoCompat.setContentMimeTypes(this, contentMimeTypes)
    }

    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN

    if (shouldEnableStylusHandwriting(imeOptions)) {
        EditorInfoCompat.setStylusHandwritingEnabled(this, true)
        EditorInfoApi34.setHandwritingGestures(this)
    } else {
        EditorInfoCompat.setStylusHandwritingEnabled(this, false)
    }
}

/**
 * Enable stylus handwriting if it is supported by the platform and if the keyboard type is not a
 * password.
 */
private fun EditorInfo.shouldEnableStylusHandwriting(imeOptions: ImeOptions) =
    isStylusHandwritingSupported &&
        imeOptions.keyboardType != KeyboardType.Password &&
        imeOptions.keyboardType != KeyboardType.PasswordVisible &&
        imeOptions.keyboardType != KeyboardType.NumberPassword &&
        imeOptions.keyboardType != KeyboardType.DecimalPassword &&
        imeOptions.keyboardType != KeyboardType.NumberPasswordSigned &&
        imeOptions.keyboardType != KeyboardType.DecimalPasswordSigned

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(24)
internal object LocaleListHelper {
    @RequiresApi(24)
    fun setHintLocales(editorInfo: EditorInfo, localeList: LocaleList) {
        when (localeList) {
            LocaleList.Empty -> {
                editorInfo.hintLocales = null
            }
            else -> {
                editorInfo.hintLocales =
                    android.os.LocaleList(*localeList.map { it.platformLocale }.toTypedArray())
            }
        }
    }
}

@RequiresApi(34)
private object EditorInfoApi34 {
    fun setHandwritingGestures(editorInfo: EditorInfo) {
        editorInfo.supportedHandwritingGestures =
            listOf(
                SelectGesture::class.java,
                DeleteGesture::class.java,
                SelectRangeGesture::class.java,
                DeleteRangeGesture::class.java,
                JoinOrSplitGesture::class.java,
                InsertGesture::class.java,
                RemoveSpaceGesture::class.java,
            )
        editorInfo.supportedHandwritingGesturePreviews =
            setOf(
                SelectGesture::class.java,
                DeleteGesture::class.java,
                SelectRangeGesture::class.java,
                DeleteRangeGesture::class.java,
            )
    }
}

@RequiresApi(37)
private object EditorInfoApi37 {
    fun setEnableTextSuggestionSelectedInputType(editorInfo: EditorInfo) {
        editorInfo.inputType =
            editorInfo.inputType or EditorInfo.TYPE_TEXT_FLAG_ENABLE_TEXT_SUGGESTION_SELECTED
    }
}
