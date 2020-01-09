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

package androidx.ui.text

import androidx.ui.core.LayoutDirection
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.unit.Density
import androidx.ui.unit.TextUnit
import androidx.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextDelegateDefaultStyleTest(
    val styleIsNull: Boolean,
    val fontSize: TextUnit,
    val textDirectionAlgorithm: TextDirectionAlgorithm?,
    val expectedStyle: TextStyle
) {
    private val density = Density(density = 1f)
    private val resourceLoader = mock<Font.ResourceLoader>()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters() = listOf(
            // style is null/not null, fontSize is inherit/set, textDirection is null/set,
            // expectation
            arrayOf(
                true, TextUnit.Inherit, null, TextStyle(
                    fontSize = 14.sp,
                    textDirectionAlgorithm = TextDirectionAlgorithm.ContentOrRtl
                )
            ),
            arrayOf(
                false, TextUnit.Inherit, null, TextStyle(
                    fontSize = 14.sp,
                    textDirectionAlgorithm = TextDirectionAlgorithm.ContentOrRtl
                )
            ),
            arrayOf(
                false, 8.sp, null, TextStyle(
                    fontSize = 8.sp,
                    textDirectionAlgorithm = TextDirectionAlgorithm.ContentOrRtl
                )
            ),
            arrayOf(
                false, TextUnit.Inherit, TextDirectionAlgorithm.ForceLtr, TextStyle(
                    fontSize = 14.sp,
                    textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr
                )
            ),
            arrayOf(
                false, 8.sp, TextDirectionAlgorithm.ForceLtr, TextStyle(
                    fontSize = 8.sp,
                    textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr
                )
            )

        )
    }

    @Test
    fun textDirectionAlgorithm_isDefault_whenTextDirectionAlgorithm_isNull() {
        val style = if (styleIsNull) {
            null
        } else {
            TextStyle(
                fontSize = fontSize,
                textDirectionAlgorithm = textDirectionAlgorithm
            )
        }

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = style,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Rtl
        )

        assertThat(textDelegate.style).isEqualTo(expectedStyle)
    }
}
