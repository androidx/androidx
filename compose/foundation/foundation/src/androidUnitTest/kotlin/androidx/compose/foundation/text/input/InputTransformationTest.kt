/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class InputTransformationTest {

    @Test
    fun chainedFilters_areEqual() {
        val filter1 = InputTransformation {
            // Noop
        }
        val filter2 = InputTransformation {
            // Noop
        }

        val chain1 = filter1.then(filter2)
        val chain2 = filter1.then(filter2)

        assertThat(chain1).isEqualTo(chain2)
    }

    @Test
    fun chainedFilters_areNotEqual_whenFiltersAreDifferent() {
        val filter1 = InputTransformation {
            // Noop
        }
        val filter2 = InputTransformation {
            // Noop
        }
        val filter3 = InputTransformation {
            // Noop
        }

        val chain1 = filter1.then(filter2)
        val chain2 = filter1.then(filter3)

        assertThat(chain1).isNotEqualTo(chain2)
    }

    @Test
    fun chainedFilters_haveNullKeyboardOptions_whenBothOptionsAreNull() {
        val filter1 =
            object : InputTransformation {
                override val keyboardOptions = null

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override val keyboardOptions = null

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isNull()
    }

    @Test
    fun chainedFilters_takeFirstKeyboardOptions_whenSecondOptionsAreNull() {
        val options = KeyboardOptions()
        val filter1 =
            object : InputTransformation {
                override val keyboardOptions = options

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override val keyboardOptions = null

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options)
    }

    @Test
    fun chainedFilters_takeSecondKeyboardOptions_whenFirstOptionsAreNull() {
        val options = KeyboardOptions()
        val filter1 =
            object : InputTransformation {
                override val keyboardOptions = null

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override val keyboardOptions = options

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options)
    }

    @Test
    fun chainedFilters_takeSecondKeyboardOptions_whenFirstOptionsAreNotNull() {
        val options1 = KeyboardOptions()
        val options2 = KeyboardOptions()
        val filter1 =
            object : InputTransformation {
                override val keyboardOptions = options1

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override val keyboardOptions = options2

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions).isSameInstanceAs(options2)
    }

    @Test
    fun chainedFilters_mergeKeyboardOptions_withPrecedenceToNext() {
        val options1 =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.Sentences
            )
        val options2 =
            KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search)
        val filter1 =
            object : InputTransformation {
                override val keyboardOptions = options1

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override val keyboardOptions = options2

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        assertThat(chain.keyboardOptions)
            .isEqualTo(
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Search
                )
            )
    }

    @Test
    fun chainedFilters_applySecondSemantics_afterFirstSemantics() {
        val filter1 =
            object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    maxTextLength = 10
                }

                override fun TextFieldBuffer.transformInput() {}
            }
        val filter2 =
            object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    maxTextLength = 20
                }

                override fun TextFieldBuffer.transformInput() {}
            }

        val chain = filter1.then(filter2)

        val config = SemanticsConfiguration()
        with(chain) { config.applySemantics() }
        assertThat(config[SemanticsProperties.MaxTextLength]).isEqualTo(20)
    }

    @Test
    fun byValue_reverts_whenReturnsCurrent() {
        val transformation = InputTransformation.byValue { current, _ -> current }
        val current = TextFieldCharSequence("a")
        val proposed = TextFieldCharSequence("ab")
        val buffer = TextFieldBuffer(originalValue = current, initialValue = proposed)

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.changes.changeCount).isEqualTo(0)
        assertThat(buffer.toString()).isEqualTo(current.toString())
    }

    @Test
    fun byValue_appliesChanges_whenReturnsSameContentAsCurrent() {
        val transformation = InputTransformation.byValue { _, _ -> "a" }
        val current = TextFieldCharSequence("a")
        val proposed = TextFieldCharSequence("ab")
        val buffer = TextFieldBuffer(originalValue = current, initialValue = proposed)

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo(current.toString())
    }

    @Test
    fun byValue_noops_whenReturnsProposed() {
        val transformation = InputTransformation.byValue { _, _ -> "ab" }
        val current = TextFieldCharSequence("a")
        val proposed = TextFieldCharSequence("ab")
        val buffer = TextFieldBuffer(originalValue = current, initialValue = proposed)

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.changes.changeCount).isEqualTo(0)
        assertThat(buffer.toString()).isEqualTo(proposed.toString())
    }

    @Test
    fun byValue_appliesChanges_whenDifferentCharSequenceReturned() {
        val transformation = InputTransformation.byValue { _, _ -> "c" }
        val current = TextFieldCharSequence("a")
        val proposed = TextFieldCharSequence("ab")
        val buffer = TextFieldBuffer(originalValue = current, initialValue = proposed)

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("c")
    }
}
