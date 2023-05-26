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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class TextEditFilterTest {

    @Test
    fun chainedFilters_areEqual() {
        val filter1 = TextEditFilter { _, _ ->
            // Noop
        }
        val filter2 = TextEditFilter { _, _ ->
            // Noop
        }

        val chain1 = filter1.then(
            filter2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        val chain2 = filter1.then(
            filter2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        assertThat(chain1).isEqualTo(chain2)
    }

    @Test
    fun chainedFilters_areNotEqual_whenKeyboardOptionsDifferent() {
        val filter1 = TextEditFilter { _, _ ->
            // Noop
        }
        val filter2 = TextEditFilter { _, _ ->
            // Noop
        }

        val chain1 = filter1.then(
            filter2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        val chain2 = filter1.then(
            filter2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        assertThat(chain1).isNotEqualTo(chain2)
    }

    @Test
    fun chainedFilters_areNotEqual_whenFiltersAreDifferent() {
        val filter1 = TextEditFilter { _, _ ->
            // Noop
        }
        val filter2 = TextEditFilter { _, _ ->
            // Noop
        }
        val filter3 = TextEditFilter { _, _ ->
            // Noop
        }

        val chain1 = filter1.then(filter2)
        val chain2 = filter1.then(filter3)

        assertThat(chain1).isNotEqualTo(chain2)
    }
}