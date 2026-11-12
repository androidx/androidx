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
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.BackgroundAnnotation.SpreadValues
import androidx.compose.ui.unit.dp
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

    @Test
    fun spreadValues_constructorsGettersEqualityAndToString() {
        val zero = SpreadValues.Zero
        assertThat(zero.left).isEqualTo(0.dp)
        assertThat(zero.top).isEqualTo(0.dp)
        assertThat(zero.right).isEqualTo(0.dp)
        assertThat(zero.bottom).isEqualTo(0.dp)
        assertThat(SpreadValues()).isEqualTo(zero)
        assertThat(SpreadValues(all = 0.dp)).isEqualTo(zero)

        val fourEdge = SpreadValues(left = 1.dp, top = 2.dp, right = 3.dp, bottom = 4.dp)
        assertThat(fourEdge.left).isEqualTo(1.dp)
        assertThat(fourEdge.top).isEqualTo(2.dp)
        assertThat(fourEdge.right).isEqualTo(3.dp)
        assertThat(fourEdge.bottom).isEqualTo(4.dp)
        assertThat(fourEdge.toString())
            .isEqualTo("SpreadValues(left=1.0.dp, top=2.0.dp, right=3.0.dp, bottom=4.0.dp)")

        val symmetric = SpreadValues(horizontal = 4.dp, vertical = 2.dp)
        assertThat(symmetric)
            .isEqualTo(SpreadValues(left = 4.dp, top = 2.dp, right = 4.dp, bottom = 2.dp))
        assertThat(symmetric.hashCode())
            .isEqualTo(
                SpreadValues(left = 4.dp, top = 2.dp, right = 4.dp, bottom = 2.dp).hashCode()
            )

        val uniform = SpreadValues(all = 6.dp)
        assertThat(uniform)
            .isEqualTo(SpreadValues(left = 6.dp, top = 6.dp, right = 6.dp, bottom = 6.dp))

        val negative = SpreadValues(horizontal = (-2).dp, vertical = (-1.5).dp)
        assertThat(negative.left).isEqualTo((-2).dp)
        assertThat(negative.top).isEqualTo((-1.5).dp)
        assertThat(negative.right).isEqualTo((-2).dp)
        assertThat(negative.bottom).isEqualTo((-1.5).dp)
    }

    @Test
    fun backgroundAnnotation_equalsHashCodeAndToString() {
        val brush1 = SolidColor(Color.Cyan)
        val brush2 = SolidColor(Color.Green)
        val producer1 = ColorProducer { Color.Red }
        val producer2 = ColorProducer { Color.Blue }

        val borderColor1 = BackgroundAnnotation.Border(width = 1.dp, color = Color.Blue)
        val borderColor2 = BackgroundAnnotation.Border(width = 1.dp, color = Color.Blue)
        val borderProducer = BackgroundAnnotation.Border(width = 1.dp, colorProducer = producer1)
        val borderBrush = BackgroundAnnotation.Border(width = 1.dp, brush = brush2)

        assertThat(borderColor1).isEqualTo(borderColor2)
        assertThat(borderColor1.hashCode()).isEqualTo(borderColor2.hashCode())
        assertThat(borderColor1).isNotEqualTo(borderProducer)
        assertThat(borderColor1).isNotEqualTo(borderBrush)
        assertThat(borderColor1.toString()).contains("Border")

        val bg1 =
            BackgroundAnnotation(
                color = Color.Red,
                cornerRadius = 6.dp,
                spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                mergeLines = false,
                border = borderColor1,
                alpha = 0.75f,
            )
        val bg2 =
            BackgroundAnnotation(
                color = Color.Red,
                cornerRadius = 6.dp,
                spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                mergeLines = false,
                border = borderColor2,
                alpha = 0.75f,
            )
        val bgDifferentRadius =
            BackgroundAnnotation(
                color = Color.Red,
                cornerRadius = 10.dp,
                spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                mergeLines = false,
                border = borderColor1,
                alpha = 0.75f,
            )
        val bgDefaults = BackgroundAnnotation(color = Color.Red)
        assertThat(bgDefaults.cornerRadius).isEqualTo(0.dp)
        assertThat(bgDefaults.spread).isEqualTo(SpreadValues.Zero)
        assertThat(bgDefaults.mergeLines).isTrue()
        assertThat(bgDefaults.border).isNull()
        assertThat(bgDefaults.alpha).isEqualTo(1.0f)

        assertThat(bg1.path).isNull()
        assertThat(bg1.color).isEqualTo(Color.Red)
        assertThat(bg1.colorProducer).isNull()
        assertThat(bg1.brush).isNull()
        assertThat(bg1.cornerRadius).isEqualTo(6.dp)
        assertThat(bg1.spread).isEqualTo(SpreadValues(horizontal = 4.dp, vertical = 2.dp))
        assertThat(bg1.mergeLines).isFalse()
        assertThat(bg1.border).isEqualTo(borderColor1)
        assertThat(bg1.alpha).isEqualTo(0.75f)
        assertThat(borderColor1.width).isEqualTo(1.dp)
        assertThat(borderColor1.color).isEqualTo(Color.Blue)
        assertThat(borderColor1.colorProducer).isNull()
        assertThat(borderColor1.brush).isNull()
        assertThat(bg1).isEqualTo(bg2)
        assertThat(bg1.hashCode()).isEqualTo(bg2.hashCode())
        assertThat(bg1).isNotEqualTo(bgDifferentRadius)
        assertThat(bg1.toString()).contains("BackgroundAnnotation(")
        assertThat(bg1.toString()).contains("spread=SpreadValues(")
        assertThat(bg1.toString()).doesNotContain("colorFilter")
        assertThat(bg1.toString()).doesNotContain("blendMode")

        val bgBrush =
            BackgroundAnnotation(
                brush = brush1,
                cornerRadius = 6.dp,
                border = borderBrush,
            )
        assertThat(bgBrush.color).isEqualTo(Color.Unspecified)
        assertThat(bgBrush.colorProducer).isNull()
        assertThat(bgBrush.brush).isEqualTo(brush1)
        assertThat(bgBrush.border).isEqualTo(borderBrush)
        assertThat(bgBrush.border!!.color).isEqualTo(Color.Unspecified)
        assertThat(bgBrush.border!!.brush).isEqualTo(brush2)

        val bgProducer1 =
            BackgroundAnnotation(
                colorProducer = producer1,
                cornerRadius = 6.dp,
                border = borderProducer,
            )
        val bgProducer2 = BackgroundAnnotation(colorProducer = producer2, cornerRadius = 6.dp)
        assertThat(bgProducer1.color).isEqualTo(Color.Unspecified)
        assertThat(bgProducer1.brush).isNull()
        assertThat(bgProducer1.colorProducer).isEqualTo(producer1)
        assertThat(bgProducer1.border!!.colorProducer).isEqualTo(producer1)
        assertThat(bgProducer1).isNotEqualTo(bgProducer2)
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
