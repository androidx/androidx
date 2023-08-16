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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class InputTransformationTest {

    @Test
    fun chainedFilters_areEqual() {
        val filter1 = InputTransformation { _, _ ->
            // Noop
        }
        val filter2 = InputTransformation { _, _ ->
            // Noop
        }

        val chain1 = filter1.then(filter2)
        val chain2 = filter1.then(filter2)

        assertThat(chain1).isEqualTo(chain2)
    }

    @Test
    fun chainedFilters_areNotEqual_whenFiltersAreDifferent() {
        val filter1 = InputTransformation { _, _ ->
            // Noop
        }
        val filter2 = InputTransformation { _, _ ->
            // Noop
        }
        val filter3 = InputTransformation { _, _ ->
            // Noop
        }

        val chain1 = filter1.then(filter2)
        val chain2 = filter1.then(filter3)

        assertThat(chain1).isNotEqualTo(chain2)
    }

    @Test
    fun chainedFilters_haveNullKeyboardOptions_whenBothOptionsAreNull() {
        val filter1 = object : InputTransformation {
            override val keyboardOptions = null

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }
        val filter2 = object : InputTransformation {
            override val keyboardOptions = null

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isNull()
    }

    @Test
    fun chainedFilters_takeFirstKeyboardOptions_whenSecondOptionsAreNull() {
        val options = KeyboardOptions()
        val filter1 = object : InputTransformation {
            override val keyboardOptions = options

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }
        val filter2 = object : InputTransformation {
            override val keyboardOptions = null

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options)
    }

    @Test
    fun chainedFilters_takeSecondKeyboardOptions_whenFirstOptionsAreNull() {
        val options = KeyboardOptions()
        val filter1 = object : InputTransformation {
            override val keyboardOptions = null

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }
        val filter2 = object : InputTransformation {
            override val keyboardOptions = options

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options)
    }

    @Test
    fun chainedFilters_takeSecondKeyboardOptions_whenFirstOptionsAreNotNull() {
        val options1 = KeyboardOptions()
        val options2 = KeyboardOptions()
        val filter1 = object : InputTransformation {
            override val keyboardOptions = options1

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }
        val filter2 = object : InputTransformation {
            override val keyboardOptions = options2

            override fun transformInput(
                originalValue: TextFieldCharSequence,
                valueWithChanges: TextFieldBuffer
            ) {
            }
        }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options2)
    }
}
