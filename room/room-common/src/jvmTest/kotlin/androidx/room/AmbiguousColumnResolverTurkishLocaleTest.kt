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

package androidx.room

import androidx.kruth.assertThat
import java.util.Locale
import org.junit.Test

class AmbiguousColumnResolverTurkishLocaleTest {
    @Test
    fun case_insensitive_tr() {
        val originalLocale = Locale.getDefault()
        try {
            // Turkish has special upper/lowercase i chars
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val result =
                AmbiguousColumnResolver.resolve(
                    arrayOf("i̇", "B", "İ", "C", "D"),
                    arrayOf(arrayOf("İ", "b"), arrayOf("i̇", "C", "d"))
                )
            assertThat(result)
                .isEqualTo(
                    arrayOf(
                        intArrayOf(0, 1),
                        intArrayOf(2, 3, 4),
                    )
                )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
