/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test ensures that [BasicTextField] throws the right exceptions when the
 * [VisualTransformation]'s [OffsetMapping] is invalid. See b/229378536.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextFieldVisualTransformationSelectionBoundsTest : FocusedWindowTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun BasicTextField_doesOffsetMapChecks_inInitialComposition() {
        val error = assertFailsWith<IllegalStateException> {
            rule.setTextFieldTestContent {
                BasicTextField(
                    value = "12345689",
                    onValueChange = { /* ignore changes */ },
                    visualTransformation = { original ->
                        TransformedText(
                            text = original,
                            offsetMapping = object : OffsetMapping {
                                override fun originalToTransformed(offset: Int) = offset + 1
                                override fun transformedToOriginal(offset: Int) = offset - 1
                            }
                        )
                    }
                )
            }
        }
        assertThat(error).hasMessageThat()
            .startsWith("OffsetMapping.originalToTransformed returned invalid mapping: ")
    }

    @Test
    fun BasicTextField_doesOffsetMapChecks_inInitialComposition_veryLongTextOffBy1() {
        val error = assertFailsWith<IllegalStateException> {
            rule.setTextFieldTestContent {
                BasicTextField(
                    value = "12345689",
                    onValueChange = { /* ignore changes */ },
                    visualTransformation = { original ->
                        TransformedText(
                            text = original,
                            offsetMapping = object : OffsetMapping {
                                override fun originalToTransformed(offset: Int) = offset + 1
                                override fun transformedToOriginal(offset: Int) = offset - 1
                            }
                        )
                    }
                )
            }
        }
        assertThat(error).hasMessageThat()
            .startsWith("OffsetMapping.originalToTransformed returned invalid mapping: ")
    }
}
