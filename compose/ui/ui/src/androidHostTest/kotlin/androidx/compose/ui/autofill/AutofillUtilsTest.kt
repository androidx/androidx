/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.autofill

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.O)
class AutofillUtilsTest {
    private val maxLength = MAX_AUTOFILL_TEXT_LENGTH

    @Test
    fun getAutofillTextValue_lessThanMax() {
        val text = "a".repeat(maxLength - 1)
        val autofillValue = AutofillApi26Helper.getAutofillTextValue(text)
        assertThat(autofillValue.textValue.length).isEqualTo(text.length)
        assertThat(autofillValue.textValue.toString()).isEqualTo(text)
    }

    @Test
    fun getAutofillTextValue_equalToMax() {
        val text = "a".repeat(maxLength)
        val autofillValue = AutofillApi26Helper.getAutofillTextValue(text)
        assertThat(autofillValue.textValue.length).isEqualTo(maxLength)
        assertThat(autofillValue.textValue.toString()).isEqualTo(text)
    }

    @Test
    fun getAutofillTextValue_moreThanMax() {
        val text = "a".repeat(maxLength + 1)
        val autofillValue = AutofillApi26Helper.getAutofillTextValue(text)
        assertThat(autofillValue.textValue.length).isEqualTo(maxLength)
        assertThat(autofillValue.textValue.toString()).isEqualTo(text.take(maxLength))
    }

    @Test
    fun getAutofillTextValue_surrogatePairAtMax() {
        // High surrogate: \uD83D, Low surrogate: \uDE00. Emoji: 😀
        val prefix = "a".repeat(maxLength - 1)
        val surrogatePair = "\uD83D\uDE00"
        val text = prefix + surrogatePair + "b"

        val autofillValue = AutofillApi26Helper.getAutofillTextValue(text)

        assertThat(autofillValue.textValue.length).isEqualTo(maxLength - 1)
        assertThat(autofillValue.textValue.toString()).isEqualTo(prefix)
    }
}
