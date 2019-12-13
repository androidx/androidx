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

import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.IntPxSize
import androidx.ui.core.LayoutDirection
import androidx.ui.core.em
import androidx.ui.core.ipx
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextOverflow
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextLayoutHelperTest {

    lateinit var resourceLoader: Font.ResourceLoader

    lateinit var referenceResult: TextLayoutResult

    @Before
    fun setUp() {
        resourceLoader = mock()

        referenceResult = TextLayoutResult(
            TextLayoutInput(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = Constraints.tightConstraintsForWidth(100.ipx)
            ),
            multiParagraph = mock(),
            size = IntPxSize(50.ipx, 50.ipx)
        )
    }

    @Test
    fun testCanResue_same() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isTrue()
    }

    @Test
    fun testCanResue_different_text() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, Android").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_style() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(fontSize = 1.5.em),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_maxLines() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_softWrap() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_overflow() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_density() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(2.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_layoutDirection() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Rtl,
            resourceLoader = resourceLoader,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_resourceLoader() {
        val constraints = Constraints.tightConstraintsForWidth(100.ipx)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = mock(),
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun testCanResue_different_constraints() {
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader,
            constraints = Constraints.tightConstraintsForWidth(200.ipx)
        )).isFalse()
    }
}
