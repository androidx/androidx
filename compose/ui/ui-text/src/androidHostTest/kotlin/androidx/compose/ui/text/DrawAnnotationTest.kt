/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DrawAnnotationTest {

    @Test
    fun withDrawAnnotation_and_getDrawAnnotations() {
        val drawerBehind: DrawAnnotationScope.() -> Unit = {}
        val drawerOver: DrawAnnotationScope.() -> Unit = {}
        val annotation = DrawAnnotation(drawBehind = drawerBehind, drawOver = drawerOver)

        val text = buildAnnotatedString {
            append("Hello ")
            withDrawAnnotation(annotation) { append("World") }
            append("!")
        }

        assertThat(text.text).isEqualTo("Hello World!")
        assertThat(text.hasDrawAnnotations(0, text.length)).isTrue()
        assertThat(text.hasDrawAnnotations(0, 6)).isFalse()
        assertThat(text.hasDrawAnnotations(6, 11)).isTrue()

        val annotations = text.getDrawAnnotations(0, text.length)
        assertThat(annotations).containsExactly(Range(annotation, 6, 11))
    }

    @Test
    fun withDrawAnnotation_and_addDrawAnnotation() {
        val drawer1: DrawAnnotationScope.() -> Unit = {}
        val drawer2: DrawAnnotationScope.() -> Unit = {}

        val text = buildAnnotatedString {
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("First") }
            append(" ")
            val start = length
            append("Second")
            addDrawAnnotation(DrawAnnotation(drawBehind = drawer2), start, length)
        }

        val annotations = text.getDrawAnnotations(0, text.length)
        assertThat(annotations).hasSize(2)
        assertThat(annotations[0]).isEqualTo(Range(DrawAnnotation(drawBehind = drawer1), 0, 5))
        assertThat(annotations[1]).isEqualTo(Range(DrawAnnotation(drawBehind = drawer2), 6, 12))
    }

    @Test
    fun hasSameLayoutAffectingAndDrawAffectingAnnotations() {
        val drawer1: DrawAnnotationScope.() -> Unit = {}
        val drawer2: DrawAnnotationScope.() -> Unit = {}

        // Case 1: DrawAnnotation is the ONLY annotation present on the AnnotatedString.
        val plain = AnnotatedString("Hello World")
        val sameTextPlain = AnnotatedString("Different text, no annotations")
        val onlyDeco1 = buildAnnotatedString {
            append("Hello ")
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
        }
        val onlyDeco1Copy = buildAnnotatedString {
            append("Hello ")
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
        }
        val onlyDeco2 = buildAnnotatedString {
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer2)) { append("Hello World") }
        }

        // Reference identity and plain-vs-plain fast paths
        assertThat(onlyDeco1.hasSameLayoutAffectingAnnotations(onlyDeco1)).isTrue()
        assertThat(onlyDeco1.hasSameDrawAffectingAnnotations(onlyDeco1)).isTrue()
        assertThat(plain.hasSameLayoutAffectingAnnotations(sameTextPlain)).isTrue()
        assertThat(plain.hasSameDrawAffectingAnnotations(sameTextPlain)).isTrue()

        // Plain vs sole DrawAnnotation
        assertThat(plain.hasEqualAnnotations(onlyDeco1)).isFalse()
        assertThat(plain.hasSameLayoutAffectingAnnotations(onlyDeco1)).isTrue()
        assertThat(onlyDeco1.hasSameLayoutAffectingAnnotations(plain)).isTrue()
        assertThat(onlyDeco1.hasSameLayoutAffectingAnnotations(onlyDeco2)).isTrue()
        assertThat(plain.hasSameDrawAffectingAnnotations(onlyDeco1)).isFalse()
        assertThat(onlyDeco1.hasSameDrawAffectingAnnotations(plain)).isFalse()
        assertThat(onlyDeco1.hasSameDrawAffectingAnnotations(onlyDeco1Copy)).isTrue()
        assertThat(onlyDeco1.hasSameDrawAffectingAnnotations(onlyDeco2)).isFalse()
        assertThat(plain.hashCodeLayoutAffectingAnnotations()).isEqualTo(0)
        assertThat(onlyDeco1.hashCodeLayoutAffectingAnnotations()).isEqualTo(0)
        assertThat(onlyDeco2.hashCodeLayoutAffectingAnnotations()).isEqualTo(0)

        // Case 2: Mixed SpanStyle / Bullet / StringAnnotation alongside DrawAnnotation.
        val base = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("Hello ") }
            append("World")
        }

        val withDeco1 = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("Hello ") }
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
        }

        val withDeco2 = buildAnnotatedString {
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer2)) {
                withStyle(SpanStyle(color = Color.Red)) { append("Hello ") }
                append("World")
            }
        }

        val withDifferentSpan = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Blue)) { append("Hello ") }
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
        }

        val withAddedBullet = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("Hello ") }
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
            addBullet(Bullet.Default.copy(width = 6.sp), 0, length)
        }

        val withAddedStringAnnotation = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("Hello ") }
            withDrawAnnotation(DrawAnnotation(drawBehind = drawer1)) { append("World") }
            addStringAnnotation(tag = "meta", annotation = "v1", start = 0, end = 5)
        }

        assertThat(base.hasEqualAnnotations(withDeco1)).isFalse()
        assertThat(base.hasSameLayoutAffectingAnnotations(withDeco1)).isTrue()
        assertThat(withDeco1.hasSameLayoutAffectingAnnotations(withDeco2)).isTrue()
        assertThat(base.hasSameDrawAffectingAnnotations(withDeco1)).isFalse()
        assertThat(withDeco1.hasSameDrawAffectingAnnotations(withDeco2)).isFalse()
        assertThat(withDeco1.hasSameDrawAffectingAnnotations(withDifferentSpan)).isTrue()
        assertThat(withDeco1.hasSameDrawAffectingAnnotations(withAddedBullet)).isTrue()
        assertThat(withDeco1.hasSameDrawAffectingAnnotations(withAddedStringAnnotation)).isTrue()
        assertThat(base.hashCodeLayoutAffectingAnnotations())
            .isEqualTo(withDeco1.hashCodeLayoutAffectingAnnotations())
        assertThat(withDeco1.hashCodeLayoutAffectingAnnotations())
            .isEqualTo(withDeco2.hashCodeLayoutAffectingAnnotations())

        assertThat(withDeco1.hasSameLayoutAffectingAnnotations(withDifferentSpan)).isFalse()
        assertThat(withDeco1.hasSameLayoutAffectingAnnotations(withAddedBullet)).isFalse()
        assertThat(withDeco1.hasSameLayoutAffectingAnnotations(withAddedStringAnnotation)).isFalse()

        // Case 3: Reversed DrawAnnotation z-order is detected as different draw annotations.
        val orderAB = buildAnnotatedString {
            append("Hello")
            addDrawAnnotation(DrawAnnotation(drawBehind = drawer1), 0, 5)
            addDrawAnnotation(DrawAnnotation(drawBehind = drawer2), 0, 5)
        }
        val orderBA = buildAnnotatedString {
            append("Hello")
            addDrawAnnotation(DrawAnnotation(drawBehind = drawer2), 0, 5)
            addDrawAnnotation(DrawAnnotation(drawBehind = drawer1), 0, 5)
        }
        assertThat(orderAB.hasSameDrawAffectingAnnotations(orderBA)).isFalse()
        assertThat(orderAB.hasSameLayoutAffectingAnnotations(orderBA)).isTrue()
    }

    @Test
    fun annotatedString_subSequenceAndPlus_preservesAndShiftsDrawAnnotations() {
        val drawer1: DrawAnnotationScope.() -> Unit = {}
        val drawer2: DrawAnnotationScope.() -> Unit = {}
        val deco1 = DrawAnnotation(drawBehind = drawer1)
        val deco2 = DrawAnnotation(drawOver = drawer2)

        val first = buildAnnotatedString {
            append("AA")
            withDrawAnnotation(deco1) { append("BB") }
        }
        val second = buildAnnotatedString {
            append(" ")
            withDrawAnnotation(deco2) { append("CC") }
        }

        val combined = first + second
        assertThat(combined.getDrawAnnotations(0, combined.length))
            .containsExactly(Range(deco1, 2, 4), Range(deco2, 5, 7))

        val sliced = combined.subSequence(1, 6)
        assertThat(sliced.text).isEqualTo("ABB C")
        assertThat(sliced.getDrawAnnotations(0, sliced.length))
            .containsExactly(Range(deco1, 1, 3), Range(deco2, 4, 5))
    }

    @Test
    fun customDataClassSubclass_structuralEqualityAndAnnotatedStringIntegration() {
        val highlightA = CustomRoundedHighlightAnnotation(colorArgb = 0x55FFEB3B, radiusPx = 8f)
        val highlightB = CustomRoundedHighlightAnnotation(colorArgb = 0x55FFEB3B, radiusPx = 8f)
        val highlightDiff = CustomRoundedHighlightAnnotation(colorArgb = 0x5542A5F5, radiusPx = 8f)

        assertThat(highlightA).isEqualTo(highlightB)
        assertThat(highlightA.hashCode()).isEqualTo(highlightB.hashCode())
        assertThat(highlightA).isNotEqualTo(highlightDiff)

        val textA = buildAnnotatedString {
            append("Prefix ")
            withDrawAnnotation(highlightA) { append("Target") }
        }
        val textB = buildAnnotatedString {
            append("Prefix ")
            withDrawAnnotation(highlightB) { append("Target") }
        }

        assertThat(textA).isEqualTo(textB)
        assertThat(textA.hashCode()).isEqualTo(textB.hashCode())
        assertThat(textA.getDrawAnnotations(0, textA.length))
            .containsExactly(Range(highlightA, 7, 13))
    }

    @Test
    fun annotatedStringSaver_savesSaveableSubclass_andSkipsLambdaDrawAnnotation() {
        val saveableHighlight =
            CustomRoundedHighlightAnnotation(colorArgb = 0x55FFEB3B, radiusPx = 10f)
        val lambdaHighlight = DrawAnnotation(drawBehind = {})

        val saverScope = SaverScope { value ->
            // Simulate Android Bundle SaverScope where Parcelable data class subclasses can be
            // saved, while arbitrary lambdas/closures cannot.
            value !is Function<*>
        }

        val original = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("Styled ") }
            withDrawAnnotation(saveableHighlight) { append("Saveable") }
            append(" ")
            withDrawAnnotation(lambdaHighlight) { append("Transient") }
            withAnnotation(tag = "meta", annotation = "v1") { append(" Tagged") }
        }

        val saved = with(AnnotatedStringSaver) { saverScope.save(original) }
        assertThat(saved).isNotNull()

        val restored = AnnotatedStringSaver.restore(saved!!)!!
        assertThat(restored.text).isEqualTo("Styled Saveable Transient Tagged")
        assertThat(restored.spanStyles).containsExactly(Range(SpanStyle(color = Color.Red), 0, 7))
        assertThat(restored.getStringAnnotations(0, restored.length))
            .containsExactly(Range("v1", 25, 32, "meta"))
        // Saveable DrawAnnotation subclass is preserved; lambda-backed DrawAnnotation is skipped.
        assertThat(restored.getDrawAnnotations(0, restored.length))
            .containsExactly(Range(saveableHighlight, 7, 15))
    }

    private data class CustomRoundedHighlightAnnotation(val colorArgb: Int, val radiusPx: Float) :
        DrawAnnotation() {
        override fun DrawAnnotationScope.drawBehind() {
            val path = Path()
            textLayoutResult.fillPathForRange(
                start = start,
                end = end,
                target = path,
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )
            drawPath(path = path, color = Color(colorArgb))
        }
    }
}
