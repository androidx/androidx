/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.compose.buildVerticalText
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmphasisPositionTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun withEmphasis_defaultPositionIsBefore() {
        val result = buildVerticalText(density) { withEmphasis(density) { text("強調") } }

        val span = result.getSpans(0, result.length, EmphasisSpan::class.java).single()
        assertThat(span.position).isEqualTo(AnnotationPosition.Before)
    }

    @Test
    fun withEmphasis_customPositionIsForwarded() {
        val result =
            buildVerticalText(density) {
                withEmphasis(density, position = AnnotationPosition.After) { text("強調") }
            }

        val span = result.getSpans(0, result.length, EmphasisSpan::class.java).single()
        assertThat(span.position).isEqualTo(AnnotationPosition.After)
    }

    @Test
    fun withEmphasis_wrapsTheWholeAppendedRange() {
        val result =
            buildVerticalText(density) {
                text("前")
                withEmphasis(density, position = AnnotationPosition.After) { text("強調") }
                text("後")
            }

        val span = result.getSpans(0, result.length, EmphasisSpan::class.java).single()
        assertThat(result.getSpanStart(span)).isEqualTo(1)
        assertThat(result.getSpanEnd(span)).isEqualTo(3)
    }
}
