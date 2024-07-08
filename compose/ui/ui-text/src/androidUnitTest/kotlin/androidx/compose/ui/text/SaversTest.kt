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

package androidx.compose.ui.text

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SaversTest {
    private val defaultSaverScope = SaverScope { true }

    @Test
    fun test_ParagraphStyle_with_no_null_value() {
        val original =
            ParagraphStyle(
                textAlign = TextAlign.Justify,
                textDirection = TextDirection.Rtl,
                lineHeight = 10.sp,
                textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp),
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
                textMotion = TextMotion.Animated
            )
        val saved = save(original, ParagraphStyleSaver, defaultSaverScope)
        val restored: ParagraphStyle? = restore(saved, ParagraphStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_PlatformParagraphStyle() {
        val original = PlatformParagraphStyle.Default
        val saved = save(original, PlatformParagraphStyle.Saver, defaultSaverScope)
        val restored: PlatformParagraphStyle? = restore(saved, PlatformParagraphStyle.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_LineBreak() {
        val original = LineBreak.Paragraph
        val saved = save(original, LineBreak.Saver, defaultSaverScope)
        val restored: LineBreak? = restore(saved, LineBreak.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextMotion() {
        val original = TextMotion.Animated
        val saved = save(original, TextMotion.Saver, defaultSaverScope)
        val restored: TextMotion? = restore(saved, TextMotion.Saver)

        assertThat(restored).isEqualTo(original)
    }
}
