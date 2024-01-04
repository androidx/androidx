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
import androidx.compose.foundation.text2.input.internal.OffsetMappingCalculator
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class CodepointTransformationTest {

    @Test
    fun toVisualText_codepointIndices() {
        val source =
            TextFieldCharSequence("a${SurrogateCodepointString}c$SurrogateCodepointString")
        val offsetMapping = OffsetMappingCalculator()
        val codepointTransformation = CodepointTransformation { i, codepoint ->
            val expectedCodePoint = when (i) {
                0 -> 'a'.code
                1 -> SurrogateCodepoint
                2 -> 'c'.code
                3 -> SurrogateCodepoint
                else -> fail("Invalid codepoint index: $i")
            }
            assertThat(codepoint).isEqualTo(expectedCodePoint)
            codepoint
        }

        source.toVisualText(codepointTransformation, offsetMapping)
    }

    @Test
    fun toVisualText_mapsOffsetsForward() {
        val source = TextFieldCharSequence("a${SurrogateCodepointString}c")
        val offsetMapping = OffsetMappingCalculator()
        val codepointTransformation = CodepointTransformation { i, codepoint ->
            when (codepoint) {
                'a'.code, 'c'.code -> SurrogateCodepoint
                SurrogateCodepoint -> 'b'.code
                else -> fail(
                    "codepointIndex=$i, codepoint=\"${
                        String(intArrayOf(codepoint), 0, 1)
                    }\""
                )
            }
        }
        val visual = source.toVisualText(codepointTransformation, offsetMapping)

        assertThat(visual.toString())
            .isEqualTo("${SurrogateCodepointString}b$SurrogateCodepointString")

        listOf(
            0 to TextRange(0),
            1 to TextRange(2),
            2 to TextRange(2, 3),
            3 to TextRange(3),
            4 to TextRange(5),
        ).forEach { (source, dest) ->
            assertWithMessage("Mapping from untransformed offset $source")
                .that(offsetMapping.mapFromSource(source)).isEqualTo(dest)
        }
    }

    @Test
    fun toVisualText_mapsOffsetsBackward() {
        val source = TextFieldCharSequence("a${SurrogateCodepointString}c")
        val offsetMapping = OffsetMappingCalculator()
        val codepointTransformation = CodepointTransformation { i, codepoint ->
            when (codepoint) {
                'a'.code, 'c'.code -> SurrogateCodepoint
                SurrogateCodepoint -> 'b'.code
                else -> fail(
                    "codepointIndex=$i, codepoint=\"${
                        String(intArrayOf(codepoint), 0, 1)
                    }\""
                )
            }
        }
        val visual = source.toVisualText(codepointTransformation, offsetMapping)

        assertThat(visual.toString())
            .isEqualTo("${SurrogateCodepointString}b$SurrogateCodepointString")

        listOf(
            0 to TextRange(0),
            1 to TextRange(0, 1),
            2 to TextRange(1),
            3 to TextRange(3),
            4 to TextRange(3, 4),
            5 to TextRange(4),
        ).forEach { (dest, source) ->
            assertWithMessage("Mapping from transformed offset $dest")
                .that(offsetMapping.mapFromDest(dest)).isEqualTo(source)
        }
    }

    private companion object {
        /** This is "𐐷", a surrogate codepoint. */
        val SurrogateCodepoint = Character.toCodePoint('\uD801', '\uDC37')
        const val SurrogateCodepointString = "\uD801\uDC37"
    }
}
