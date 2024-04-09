/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.intl.LocaleList
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeyboardOptionsTest {

    @Test
    fun toImeOptions_copiesRelevantProperties() {
        val platformImeOptions = PlatformImeOptions("privateImeOptions")

        val keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Go,
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            platformImeOptions = platformImeOptions
        )

        assertThat(keyboardOptions.toImeOptions(singleLine = true)).isEqualTo(
            ImeOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go,
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = false,
                singleLine = true,
                platformImeOptions = platformImeOptions
            )
        )
    }

    @Test
    fun toImeOptions_replacesUnspecifiedValues() {
        assertThat(KeyboardOptions().toImeOptions()).isEqualTo(
            ImeOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = true,
                singleLine = false,
                platformImeOptions = null,
                hintLocales = LocaleList.Empty
            )
        )
    }

    @Test
    fun fillUnspecifiedValuesWith_takesReceiverWhenOtherNull() {
        // Specify at least one property so we don't hit the "all unspecified" case.
        val receiver = KeyboardOptions(keyboardType = KeyboardType.Password)
        assertThat(receiver.fillUnspecifiedValuesWith(null)).isSameInstanceAs(receiver)
    }

    @Test
    fun fillUnspecifiedValuesWith_takesReceiverWhenOtherEqual() {
        // Specify at least one property so we don't hit the "all unspecified" case.
        val receiver = KeyboardOptions(keyboardType = KeyboardType.Password)
        val other = KeyboardOptions(keyboardType = KeyboardType.Password)
        assertThat(receiver.fillUnspecifiedValuesWith(other)).isSameInstanceAs(receiver)
    }

    @Test
    fun fillUnspecifiedValuesWith_takesReceiverWhenOtherAllUnspecified() {
        val receiver = KeyboardOptions(keyboardType = KeyboardType.Password)
        val other = KeyboardOptions()
        assertThat(receiver.fillUnspecifiedValuesWith(other)).isSameInstanceAs(receiver)
    }

    @Test
    fun fillUnspecifiedValuesWith_takesOtherWhenReceiverAllUnspecified() {
        val receiver = KeyboardOptions()
        val other = KeyboardOptions(keyboardType = KeyboardType.Password)
        assertThat(receiver.fillUnspecifiedValuesWith(other)).isSameInstanceAs(other)
    }

    @Test
    fun fillUnspecifiedValuesWith_prefersReceiv3er() {
        val receiver = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Search,
            platformImeOptions = PlatformImeOptions("receiver"),
            showKeyboardOnFocus = false,
            hintLocales = LocaleList("fr")
        )
        // All properties must be different.
        val other = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search,
            platformImeOptions = PlatformImeOptions("other"),
            showKeyboardOnFocus = true,
            hintLocales = LocaleList("fr")
        )

        assertThat(receiver.fillUnspecifiedValuesWith(other)).isEqualTo(receiver)
    }
}
