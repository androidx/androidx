/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.text.intl.Locale
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class StringTest {

    @Test
    fun English_uppercase() {
        assertThat("aBcDe".toUpperCase(Locale("en-US"))).isEqualTo("ABCDE")
    }

    @Test
    fun English_lowercase() {
        assertThat("aBcDe".toLowerCase(Locale("en-US"))).isEqualTo("abcde")
    }

    @Test
    fun English_capitalize() {
        assertThat("abcde".capitalize(Locale("en-US"))).isEqualTo("Abcde")
    }

    @Test
    fun English_decapitalize() {
        assertThat("Abcde".decapitalize(Locale("en-US"))).isEqualTo("abcde")
    }

    @Test
    fun LocaleDependent_uppercase() {
        val upperI = "i".uppercase(Locale("tr").platformLocale)
        assertThat("hijkl".toUpperCase(Locale("tr"))).isEqualTo("H${upperI}JKL")
    }

    @Test
    fun LocaleDependent_lowercase() {
        val upperI = "i".uppercase(Locale("tr").platformLocale)
        assertThat("h${upperI}jkl".toLowerCase(Locale("tr"))).isEqualTo("hijkl")
    }
}
