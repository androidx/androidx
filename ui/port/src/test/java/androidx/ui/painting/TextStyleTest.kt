/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import android.graphics.Typeface
import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDecorationStyle
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.window.Locale
import androidx.ui.painting.basictypes.RenderComparison
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val FONT_SIZE = 10.0
private const val TEXT_STYLE_DEFAULT_FONT_SIZE = 14.0
private const val HEIGHT = 123.0
private val COLOR1 = Color(0xFF00FF00.toInt())
private val COLOR2 = Color(0x00FFFF00.toInt())

@RunWith(JUnit4::class)
class TextStyleTest {
    @Test
    fun `constructor with default values`() {
        val textStyle = TextStyle()

        assertThat(textStyle.inherit).isTrue()
        assertThat(textStyle.color).isNull()
        assertThat(textStyle.fontSize).isNull()
        assertThat(textStyle.fontWeight).isNull()
        assertThat(textStyle.fontStyle).isNull()
        assertThat(textStyle.letterSpacing).isNull()
        assertThat(textStyle.wordSpacing).isNull()
        assertThat(textStyle.textBaseline).isNull()
        assertThat(textStyle.height).isNull()
        assertThat(textStyle.locale).isNull()
        assertThat(textStyle.foreground).isNull()
        assertThat(textStyle.background).isNull()
        assertThat(textStyle.decoration).isNull()
        assertThat(textStyle.decorationColor).isNull()
        assertThat(textStyle.decorationStyle).isNull()
        assertThat(textStyle.debugLabel).isNull()
        assertThat(textStyle.fontFamily).isNull()
    }

    @Test
    fun `constructor with customized values`() {
        val fgPaint = Paint()
        fgPaint.color = COLOR1
        val bgPaint = Paint()
        bgPaint.color = COLOR2

        val letterSpacing = 1.0
        val wordSpacing = 2.0

        val debugLabel = "foo"
        val packageName = "p"

        val textStyle = TextStyle(
            inherit = false,
            color = null,
            fontSize = FONT_SIZE,
            fontWeight = FontWeight.w800,
            fontStyle = FontStyle.italic,
            letterSpacing = letterSpacing,
            wordSpacing = wordSpacing,
            textBaseline = TextBaseline.alphabetic,
            height = HEIGHT,
            locale = Locale("en", "US"),
            foreground = fgPaint,
            background = bgPaint,
            decoration = TextDecoration.overline,
            decorationColor = COLOR1,
            decorationStyle = TextDecorationStyle.dashed,
            debugLabel = debugLabel,
            fontFamily = FontFallback(typeface = Typeface.SANS_SERIF)
        )

        assertThat(textStyle.inherit).isFalse()
        assertThat(textStyle.color).isNull()
        assertThat(textStyle.fontSize).isEqualTo(FONT_SIZE)
        assertThat(textStyle.fontWeight).isEqualTo(FontWeight.w800)
        assertThat(textStyle.fontStyle).isEqualTo(FontStyle.italic)
        assertThat(textStyle.letterSpacing).isEqualTo(letterSpacing)
        assertThat(textStyle.wordSpacing).isEqualTo(wordSpacing)
        assertThat(textStyle.textBaseline).isEqualTo(TextBaseline.alphabetic)
        assertThat(textStyle.height).isEqualTo(HEIGHT)
        assertThat(textStyle.locale).isEqualTo(Locale("en", "US"))
        assertThat(textStyle.foreground).isEqualTo(fgPaint)
        assertThat(textStyle.background).isEqualTo(bgPaint)
        assertThat(textStyle.decoration).isEqualTo(TextDecoration.overline)
        assertThat(textStyle.decorationColor).isEqualTo(COLOR1)
        assertThat(textStyle.decorationStyle).isEqualTo(TextDecorationStyle.dashed)
        assertThat(textStyle.debugLabel).isEqualTo(debugLabel)
        assertThat(textStyle.fontFamily?.typeface).isEqualTo(Typeface.SANS_SERIF)
    }

    // TODO(Migration/qqd): Implement toString related tests.
//    @Test
//    fun `toString normal`() {
//    expect(
//        const TextStyle(inherit: false).toString(),
//    equals('TextStyle(inherit: false, <no style specified>)'),
//    );
//    expect(
//        const TextStyle(inherit: true).toString(),
//    equals('TextStyle(<all styles inherited>)'),
//    );
//    }

    // TODO(Migration/qqd): Implement apply related tests.
//    @Test
//    fun `apply`() {

//    final TextStyle s3 = s1.apply(fontSizeFactor: 2.0, fontSizeDelta: -2.0, fontWeightDelta: -4);
//    expect(s1.fontFamily, isNull);
//    expect(s1.fontSize, 10.0);
//    expect(s1.fontWeight, FontWeight.w800);
//    expect(s1.height, 123.0);
//    expect(s1.color, isNull);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, isNot(equals(s1)));
//
//    expect(s1.apply(fontWeightDelta: -10).fontWeight, FontWeight.w100);
//    expect(s1.apply(fontWeightDelta: 2).fontWeight, FontWeight.w900);
//    }

    // TODO(Migration/qqd): Implement merge related tests.
//    @Test
//    fun `merge`() {
//    expect(s1.merge(null), equals(s1));
//
//    final TextStyle s4 = s2.merge(s1);
//    expect(s1.fontFamily, isNull);
//    expect(s1.fontSize, 10.0);
//    expect(s1.fontWeight, FontWeight.w800);
//    expect(s1.height, 123.0);
//    expect(s1.color, isNull);
//    expect(s2.fontFamily, isNull);
//    expect(s2.fontSize, 10.0);
//    expect(s2.fontWeight, FontWeight.w800);
//    expect(s2.height, 100.0);
//    expect(s2.color, const Color(0xFF00FF00));
//    expect(s2, isNot(equals(s1)));
//    expect(s2, isNot(equals(s4)));
//    expect(s4.fontFamily, isNull);
//    expect(s4.fontSize, 10.0);
//    expect(s4.fontWeight, FontWeight.w800);
//    expect(s4.height, 123.0);
//    expect(s4.color, const Color(0xFF00FF00));
//
//    }

    // TODO(Migration/qqd): Implement lerp related tests.
//    @Test
//    fun `lerp`() {
//    final TextStyle s5 = TextStyle.lerp(s1, s3, 0.25);
//    expect(s1.fontFamily, isNull);
//    expect(s1.fontSize, 10.0);
//    expect(s1.fontWeight, FontWeight.w800);
//    expect(s1.height, 123.0);
//    expect(s1.color, isNull);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, isNot(equals(s1)));
//    expect(s3, isNot(equals(s5)));
//    expect(s5.fontFamily, isNull);
//    expect(s5.fontSize, 12.0);
//    expect(s5.fontWeight, FontWeight.w700);
//    expect(s5.height, 123.0);
//    expect(s5.color, isNull);
//
//    expect(TextStyle.lerp(null, null, 0.5), isNull);
//
//    final TextStyle s6 = TextStyle.lerp(null, s3, 0.25);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, isNot(equals(s6)));
//    expect(s6.fontFamily, isNull);
//    expect(s6.fontSize, isNull);
//    expect(s6.fontWeight, FontWeight.w400);
//    expect(s6.height, isNull);
//    expect(s6.color, isNull);
//
//    final TextStyle s7 = TextStyle.lerp(null, s3, 0.75);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, equals(s7));
//    expect(s7.fontFamily, isNull);
//    expect(s7.fontSize, 18.0);
//    expect(s7.fontWeight, FontWeight.w400);
//    expect(s7.height, 123.0);
//    expect(s7.color, isNull);
//
//    final TextStyle s8 = TextStyle.lerp(s3, null, 0.25);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, equals(s8));
//    expect(s8.fontFamily, isNull);
//    expect(s8.fontSize, 18.0);
//    expect(s8.fontWeight, FontWeight.w400);
//    expect(s8.height, 123.0);
//    expect(s8.color, isNull);
//
//    final TextStyle s9 = TextStyle.lerp(s3, null, 0.75);
//    expect(s3.fontFamily, isNull);
//    expect(s3.fontSize, 18.0);
//    expect(s3.fontWeight, FontWeight.w400);
//    expect(s3.height, 123.0);
//    expect(s3.color, isNull);
//    expect(s3, isNot(equals(s9)));
//    expect(s9.fontFamily, isNull);
//    expect(s9.fontSize, isNull);
//    expect(s9.fontWeight, FontWeight.w400);
//    expect(s9.height, isNull);
//    expect(s9.color, isNull);
//    }

    @Test
    fun `getTextStyle`() {
        val textStyle = TextStyle(
            fontSize = FONT_SIZE,
            fontWeight = FontWeight.w800,
            color = COLOR1,
            height = HEIGHT
        )

        assertThat(textStyle.fontFamily).isNull()
        assertThat(textStyle.fontSize).isEqualTo(FONT_SIZE)
        assertThat(textStyle.fontWeight).isEqualTo(FontWeight.w800)
        assertThat(textStyle.height).isEqualTo(HEIGHT)
        assertThat(textStyle.color).isEqualTo(COLOR1)

        val newTextStyle = textStyle.getTextStyle()
        assertThat(newTextStyle).isEqualTo(
            androidx.ui.engine.text.TextStyle(
                color = COLOR1,
                fontWeight = FontWeight.w800,
                fontSize = FONT_SIZE,
                height = HEIGHT
            )
        )
    }

    @Test
    fun `getParagraphStyle with text align`() {
        val textStyle = TextStyle(
            fontSize = FONT_SIZE,
            fontWeight = FontWeight.w800,
            color = COLOR1,
            height = HEIGHT
        )

        assertThat(textStyle.fontFamily).isNull()
        assertThat(textStyle.fontSize).isEqualTo(FONT_SIZE)
        assertThat(textStyle.fontWeight).isEqualTo(FontWeight.w800)
        assertThat(textStyle.height).isEqualTo(HEIGHT)
        assertThat(textStyle.color).isEqualTo(COLOR1)

        val paragraphStyle = textStyle.getParagraphStyle(textAlign = TextAlign.center)
        assertThat(paragraphStyle).isEqualTo(
            ParagraphStyle(
                textAlign = TextAlign.center,
                fontWeight = FontWeight.w800,
                fontSize = FONT_SIZE,
                lineHeight = HEIGHT
            )
        )
    }

    @Test
    fun `getParagraphStyle with LTR text direction`() {
        val paragraphStyleLTR = TextStyle().getParagraphStyle(textDirection = TextDirection.LTR)
        assertThat(paragraphStyleLTR).isEqualTo(
            ParagraphStyle(
                textDirection = TextDirection.LTR,
                fontSize = TEXT_STYLE_DEFAULT_FONT_SIZE
            )
        )
    }

    @Test
    fun `getParagraphStyle with RTL text direction`() {
        val paragraphStyleRTL = TextStyle().getParagraphStyle(textDirection = TextDirection.RTL)
        assertThat(paragraphStyleRTL).isEqualTo(
            ParagraphStyle(
                textDirection = TextDirection.RTL,
                fontSize = TEXT_STYLE_DEFAULT_FONT_SIZE
            )
        )
    }

    // TODO(Migration/qqd): Delete packageName since we don't plan to implement it.
//    test('TextStyle using package font', () {
//        const TextStyle s6 = TextStyle(fontFamily: 'test');
//        expect(s6.fontFamily, 'test');
//        expect(s6.getTextStyle().toString(), 'TextStyle(color: unspecified, decoration: unspecified, decorationColor: unspecified, decorationStyle: unspecified, fontWeight: unspecified, fontStyle: unspecified, textBaseline: unspecified, fontFamily: test, fontSize: unspecified, letterSpacing: unspecified, wordSpacing: unspecified, height: unspecified, locale: unspecified, background: unspecified, foreground: unspecified)');
//
//        const TextStyle s7 = TextStyle(fontFamily: 'test', package: 'p');
//        expect(s7.fontFamily, 'packages/p/test');
//        expect(s7.getTextStyle().toString(), 'TextStyle(color: unspecified, decoration: unspecified, decorationColor: unspecified, decorationStyle: unspecified, fontWeight: unspecified, fontStyle: unspecified, textBaseline: unspecified, fontFamily: packages/p/test, fontSize: unspecified, letterSpacing: unspecified, wordSpacing: unspecified, height: unspecified, locale: unspecified, background: unspecified, foreground: unspecified)');
//    });

    @Test
    fun `debugLabel with constructor default values`() {
        val unknown = TextStyle()

        assertThat(unknown.debugLabel).isNull()
    }

    @Test
    fun `debugLabel with constructor customized values`() {
        val foo = TextStyle(debugLabel = "foo", fontSize = 1.0)

        assertThat(foo.debugLabel).isEqualTo("foo")
    }

    // TODO(Migration/qqd): Implement apply related tests.
//    @Test
//    fun `debugLabel with apply`() {

//        expect(unknown.apply().debugLabel, null);
//        expect(foo.apply().debugLabel, '(foo).apply');
//    }

    // TODO(Migration/qqd): Implement merge related tests.
//    @Test
//    fun `debugLabel with merge`() {
//        val foo = TextStyle(debugLabel = "foo", fontSize = 1.0)
//        val bar: TextStyle = TextStyle(debugLabel = "bar", fontSize = 2.0)
//        val baz: TextStyle = TextStyle(debugLabel = "baz", fontSize = 3.0)
//        expect(foo.merge(bar).debugLabel, '(foo).merge(bar)');
//        expect(foo.merge(bar).merge(baz).debugLabel, '((foo).merge(bar)).merge(baz)');
//        assertEquals(foo.copy().debugLabel, "(foo).copyWith")
//    }

    // TODO(Migration/qqd): Implement lerp related tests.
//    @Test
//    fun `debugLabel with lerp`() {
//        val foo = TextStyle(debugLabel = "foo", fontSize = 1.0)
//        val bar: TextStyle = TextStyle(debugLabel = "bar", fontSize = 2.0)
//        val baz: TextStyle = TextStyle(debugLabel = "baz", fontSize = 3.0)
//        expect(TextStyle.lerp(foo, bar, 0.5).debugLabel, 'lerp(foo ⎯0.5→ bar)');
//        expect(TextStyle.lerp(foo.merge(bar), baz, 0.51).copyWith().debugLabel, '(lerp((foo).merge(bar) ⎯0.5→ baz)).copyWith');
//    }

    @Test
    fun `compareTo with same textStyle returns IDENTICAL`() {
        val textStyle = TextStyle()
        assertThat(textStyle.compareTo(textStyle)).isEqualTo(RenderComparison.IDENTICAL)
    }

    @Test
    fun `compareTo with identical textStyle returns IDENTICAL`() {
        val textStyle1 = TextStyle()
        val textStyle2 = TextStyle()
        assertThat(textStyle1.compareTo(textStyle2)).isEqualTo(RenderComparison.IDENTICAL)
    }

    @Test
    fun `compareTo textStyle with different layout returns LAYOUT`() {
        val fgPaint = Paint()
        fgPaint.color = COLOR1
        val bgPaint = Paint()
        bgPaint.color = COLOR2

        val textStyle = TextStyle(
            inherit = false,
            color = null,
            fontSize = FONT_SIZE,
            fontWeight = FontWeight.w800,
            fontStyle = FontStyle.italic,
            letterSpacing = 1.0,
            wordSpacing = 2.0,
            textBaseline = TextBaseline.alphabetic,
            height = HEIGHT,
            locale = Locale("en", "US"),
            foreground = fgPaint,
            background = bgPaint,
            decoration = TextDecoration.overline,
            decorationColor = COLOR1,
            decorationStyle = TextDecorationStyle.dashed,
            debugLabel = "foo",
            fontFamily = FontFallback(typeface = Typeface.SANS_SERIF)
        )

        assertThat(
            textStyle.compareTo(textStyle.copy(inherit = true)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(
                textStyle.copy(fontFamily = FontFallback(typeface = Typeface.MONOSPACE))))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(fontSize = 20.0)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(fontWeight = FontWeight.w100)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(fontStyle = FontStyle.normal)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(letterSpacing = 2.0)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(wordSpacing = 4.0)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(textBaseline = TextBaseline.ideographic)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(height = 20.0)))
            .isEqualTo(RenderComparison.LAYOUT)
        assertThat(
            textStyle.compareTo(textStyle.copy(locale = Locale("ja", "JP"))))
            .isEqualTo(RenderComparison.LAYOUT)

        assertThat(
            textStyle.compareTo(textStyle.copy(foreground = bgPaint)))
            .isEqualTo(RenderComparison.LAYOUT)

        assertThat(
            textStyle.compareTo(textStyle.copy(background = fgPaint)))
            .isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo textStyle with different paint returns PAINT`() {
        val textStyle = TextStyle(
            inherit = false,
            color = COLOR1,
            fontSize = FONT_SIZE,
            fontWeight = FontWeight.w800,
            fontStyle = FontStyle.italic,
            letterSpacing = 1.0,
            wordSpacing = 2.0,
            textBaseline = TextBaseline.alphabetic,
            height = HEIGHT,
            locale = Locale("en", "US"),
            decoration = TextDecoration.overline,
            decorationColor = COLOR1,
            decorationStyle = TextDecorationStyle.dashed,
            debugLabel = "foo",
            fontFamily = FontFallback(typeface = Typeface.SANS_SERIF)
        )

        assertThat(
            textStyle.compareTo(textStyle.copy(color = COLOR2)))
            .isEqualTo(RenderComparison.PAINT)
        assertThat(
            textStyle.compareTo(textStyle.copy(decoration = TextDecoration.lineThrough)))
            .isEqualTo(RenderComparison.PAINT)
        assertThat(
            textStyle.compareTo(textStyle.copy(decorationColor = COLOR2)))
            .isEqualTo(RenderComparison.PAINT)
        assertThat(
            textStyle.compareTo(textStyle.copy(decorationStyle = TextDecorationStyle.dotted)))
            .isEqualTo(RenderComparison.PAINT)
    }

    // TODO(Migration/qqd): Implement this test after merge, lerp and apply are implemented.
//    test('TextStyle foreground and color combos', () {
//        const Color red = Color.fromARGB(255, 255, 0, 0);
//        const Color blue = Color.fromARGB(255, 0, 0, 255);
//        const TextStyle redTextStyle = TextStyle(color: red);
//        const TextStyle blueTextStyle = TextStyle(color: blue);
//        final TextStyle redPaintTextStyle = TextStyle(foreground: Paint()..color = red);
//        final TextStyle bluePaintTextStyle = TextStyle(foreground: Paint()..color = blue);
//
//        // merge/copyWith
//        final TextStyle redBlueBothForegroundMerged = redTextStyle.merge(blueTextStyle);
//        expect(redBlueBothForegroundMerged.color, blue);
//        expect(redBlueBothForegroundMerged.foreground, isNull);
//
//        final TextStyle redBlueBothPaintMerged = redPaintTextStyle.merge(bluePaintTextStyle);
//        expect(redBlueBothPaintMerged.color, null);
//        expect(redBlueBothPaintMerged.foreground, bluePaintTextStyle.foreground);
//
//        final TextStyle redPaintBlueColorMerged = redPaintTextStyle.merge(blueTextStyle);
//        expect(redPaintBlueColorMerged.color, null);
//        expect(redPaintBlueColorMerged.foreground, redPaintTextStyle.foreground);
//
//        final TextStyle blueColorRedPaintMerged = blueTextStyle.merge(redPaintTextStyle);
//        expect(blueColorRedPaintMerged.color, null);
//        expect(blueColorRedPaintMerged.foreground, redPaintTextStyle.foreground);
//
//        // apply
//        expect(redPaintTextStyle.apply(color: blue).color, isNull);
//        expect(redPaintTextStyle.apply(color: blue).foreground.color, red);
//        expect(redTextStyle.apply(color: blue).color, blue);
//
//        // lerp
//        expect(TextStyle.lerp(redTextStyle, blueTextStyle, .25).color, Color.lerp(red, blue, .25));
//        expect(TextStyle.lerp(redTextStyle, bluePaintTextStyle, .25).color, isNull);
//        expect(TextStyle.lerp(redTextStyle, bluePaintTextStyle, .25).foreground.color, red);
//        expect(TextStyle.lerp(redTextStyle, bluePaintTextStyle, .75).foreground.color, blue);
//
//        expect(TextStyle.lerp(redPaintTextStyle, bluePaintTextStyle, .25).color, isNull);
//        expect(TextStyle.lerp(redPaintTextStyle, bluePaintTextStyle, .25).foreground.color, red);
//        expect(TextStyle.lerp(redPaintTextStyle, bluePaintTextStyle, .75).foreground.color, blue);
//    });
}
