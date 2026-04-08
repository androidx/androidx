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

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LegacyEditorInfoTest {

    @Test
    fun test_fill_editor_info_text() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_ascii() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_number() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_phone() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_PHONE)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_uri() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_URI)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_email() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_password() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_PASSWORD)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_number_password() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_decimal_number() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    @Test
    fun test_fill_editor_info_password_visible() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.PasswordVisible, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
    }

    @Test
    fun test_fill_editor_info_postal_address() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.PostalAddress, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS)
    }

    @Test
    fun test_fill_editor_info_person_name() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.PersonName, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
    }

    @Test
    fun test_fill_editor_info_email_subject() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.EmailSubject, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT)
    }

    @Test
    fun test_fill_editor_info_short_message() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.ShortMessage, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)
    }

    @Test
    fun test_fill_editor_info_long_message() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.LongMessage, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE)
    }

    @Test
    fun test_fill_editor_info_web_edit_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.WebEditText, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)
    }

    @Test
    fun test_fill_editor_info_filter() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Filter, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_FILTER)
    }

    @Test
    fun test_fill_editor_info_phonetic() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Phonetic, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_PHONETIC)
    }

    @Test
    fun test_fill_editor_info_web_email_address() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.WebEmailAddress, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
    }

    @Test
    fun test_fill_editor_info_web_password() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.WebPassword, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
    }

    @Test
    fun test_fill_editor_info_date_time() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.DateTime, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_DATETIME)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_DATETIME_VARIATION_NORMAL)
    }

    @Test
    fun test_fill_editor_info_date() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Date, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_DATETIME)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_DATETIME_VARIATION_DATE)
    }

    @Test
    fun test_fill_editor_info_time() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Time, imeAction = ImeAction.Default))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_DATETIME)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_DATETIME_VARIATION_TIME)
    }

    @Test
    fun test_fill_editor_info_number_signed() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.NumberSigned, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_SIGNED)
    }

    @Test
    fun test_fill_editor_info_decimal_signed() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.DecimalSigned, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    }

    @Test
    fun test_fill_editor_info_decimal_password() {
        val info = EditorInfo()
        info.update(
            ImeOptions(keyboardType = KeyboardType.DecimalPassword, imeAction = ImeAction.Default)
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_DECIMAL)
    }

    @Test
    fun test_fill_editor_info_number_password_signed() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.NumberPasswordSigned,
                imeAction = ImeAction.Default,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_SIGNED)
    }

    @Test
    fun test_fill_editor_info_decimal_password_signed() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.DecimalPasswordSigned,
                imeAction = ImeAction.Default,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
        assertThat(info.inputType and InputType.TYPE_MASK_VARIATION)
            .isEqualTo(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    }

    @Test
    fun test_fill_editor_info_action_none() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.None))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_NONE)
    }

    @Test
    fun test_fill_editor_info_action_go() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_GO)
    }

    @Test
    fun test_fill_editor_info_action_next() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_NEXT)
    }

    @Test
    fun test_fill_editor_info_action_previous() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Previous))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_PREVIOUS)
    }

    @Test
    fun test_fill_editor_info_action_search() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_SEARCH)
    }

    @Test
    fun test_fill_editor_info_action_send() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_SEND)
    }

    @Test
    fun test_fill_editor_info_action_done() {
        val info = EditorInfo()
        info.update(ImeOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done))

        assertThat(info.inputType and InputType.TYPE_MASK_CLASS)
            .isEqualTo(InputType.TYPE_CLASS_TEXT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII)
            .isEqualTo(EditorInfo.IME_FLAG_FORCE_ASCII)
        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_DONE)
    }

    @Test
    fun test_fill_editor_info_multi_line() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION).isEqualTo(0)
    }

    @Test
    fun test_fill_editor_info_multi_line_with_default_action() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION)
            .isEqualTo(EditorInfo.IME_FLAG_NO_ENTER_ACTION)
    }

    @Test
    fun test_fill_editor_info_single_line() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION).isEqualTo(0)
    }

    @Test
    fun test_fill_editor_info_single_line_changes_ime_from_unspecified_to_done() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            )
        )

        assertThat(info.imeOptions and EditorInfo.IME_MASK_ACTION)
            .isEqualTo(EditorInfo.IME_ACTION_DONE)
    }

    @Test
    fun test_fill_editor_info_multi_line_not_set_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS).isEqualTo(0)
        assertThat(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION).isEqualTo(0)
    }

    @Test
    fun test_fill_editor_info_capitalization_none() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
    }

    @Test
    fun test_fill_editor_info_capitalization_characters() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            )
    }

    @Test
    fun test_fill_editor_info_capitalization_words() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(
                InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            )
    }

    @Test
    fun test_fill_editor_info_capitalization_sentences() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            )
    }

    @Test
    fun test_fill_editor_info_capitalization_not_added_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS).isEqualTo(0)
    }

    @Test
    fun test_fill_editor_info_auto_correct_on() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
    }

    @Test
    fun test_fill_editor_info_auto_correct_off() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS)
            .isEqualTo(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
    }

    @Test
    fun autocorrect_not_added_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        )

        assertThat(info.inputType and InputType.TYPE_MASK_FLAGS).isEqualTo(0)
    }

    @Test
    fun initial_default_selection_info_is_set() {
        val info = EditorInfo()
        info.update(ImeOptions.Default)

        assertThat(info.initialSelStart).isEqualTo(0)
        assertThat(info.initialSelEnd).isEqualTo(0)
    }

    @Test
    fun initial_selection_info_is_set() {
        val selection = TextRange(1, 2)
        val info = EditorInfo()
        info.update("abc", selection, ImeOptions.Default)

        assertThat(info.initialSelStart).isEqualTo(selection.start)
        assertThat(info.initialSelEnd).isEqualTo(selection.end)
    }

    @Test
    fun test_privateImeOptions_is_set() {
        val info = EditorInfo()
        val privateImeOptions = "testOptions"
        info.update(ImeOptions(platformImeOptions = PlatformImeOptions(privateImeOptions)))

        assertThat(info.privateImeOptions).isEqualTo(privateImeOptions)
    }

    private fun EditorInfo.update(imeOptions: ImeOptions) {
        this.update("", TextRange.Zero, imeOptions)
    }
}
