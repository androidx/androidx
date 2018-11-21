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

package androidx.text;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import android.app.Instrumentation;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class StaticLayoutCompatTest {
    private Typeface mSampleFont;

    @Before
    public void setUp() {
        final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mSampleFont = Typeface.createFromAsset(mInstrumentation.getContext().getAssets(),
                "sample_font.ttf");
    }

    @Test
    public void builder_constructor_returnsGivenParameters() {
        final CharSequence text = "hello";
        final TextPaint paint = new TextPaint();
        final int width = 10;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .build();

        assertThat(staticLayout.getText(), equalTo(text));
        assertThat(staticLayout.getPaint(), equalTo(paint));
        assertThat(staticLayout.getWidth(), equalTo(width));
    }

    @Test(expected = NullPointerException.class)
    public void builder_constructor_withTextNull_throwsNPE() {
        new StaticLayoutCompat.Builder(null, new TextPaint(), 0);
    }

    @Test(expected = NullPointerException.class)
    public void builder_constructor_withPaintNull_throwsNPE() {
        new StaticLayoutCompat.Builder("", null, 0);
    }

    @Test
    public void builder_build_returnNotNull() {
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .build();

        assertNotNull(staticLayout);
    }

    @Test
    public void builder_setText_returnsGivenText() {
        final CharSequence text = "hello";
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setText(text)
                .build();

        assertThat(staticLayout.getText(), equalTo(text));
    }

    @Test(expected = NullPointerException.class)
    public void builder_setText_withNull_throwsNPE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setStart_withNegative_throwsIAE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setStart(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setEnd_withNegative_throwsIAE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setEnd(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setStart_setEnd_withStartGreaterThanEnd_throwsIAE() {
        new StaticLayoutCompat.Builder("a", new TextPaint(), 0)
                .setStart(1)
                .setEnd(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setEnd_withEndGreaterThanLength_throwsIAE() {
        new StaticLayoutCompat.Builder("a", new TextPaint(), 0)
                .setEnd("a".length() + 1)
                .build();
    }

    @Test
    public void builder_defaultStart_isZero() {
        final CharSequence text = "abc";
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, new TextPaint(), 50)
                .build();

        // Check start is 0
        assertThat(staticLayout.getLineStart(0), equalTo(0));
    }

    @Test
    public void builder_defaultEnd_isTextLength() {
        final CharSequence str = "abc";

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(str, new TextPaint(), 50)
                .build();

        int lineNum = staticLayout.getLineCount();
        // Check the end is str.length()
        assertThat(staticLayout.getLineStart(lineNum), equalTo(str.length()));
    }

    @Test
    public void builder_setPaint_returnsGivenPaint() {
        final TextPaint paint = new TextPaint();
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", paint, 0)
                .setPaint(paint)
                .build();

        assertThat(staticLayout.getPaint(), equalTo(paint));
    }

    @Test(expected = NullPointerException.class)
    public void builder_setPaint_withNull_throwsNPE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setPaint(null);
    }

    @Test
    public void builder_setWidth_returnsGivenWidth() {
        final int width = 10;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setWidth(width)
                .build();

        assertThat(staticLayout.getWidth(), equalTo(width));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setWidth_withNegativeInt_throwsIAE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setWidth(-1);
    }

    @Test
    public void builder_setAlignment_returnsGivenAlignment() {
        final Layout.Alignment align = Layout.Alignment.ALIGN_OPPOSITE;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setAlignment(align)
                .build();

        assertThat(staticLayout.getAlignment(), equalTo(align));
    }

    @Test(expected = NullPointerException.class)
    public void builder_setAlignment_withNull_throwsNPE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setAlignment(null);
    }

    @Test
    public void builder_defaultAlignment_returnsAlignNormal() {
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .build();

        assertThat(staticLayout.getAlignment(), equalTo(Layout.Alignment.ALIGN_NORMAL));
    }

    @Test
    public void builder_setTextDirection_returnsGivenTextDirection() {
        final TextDirectionHeuristic textDir = TextDirectionHeuristics.RTL;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setTextDirection(textDir)
                .build();

        assertThat(staticLayout.getParagraphDirection(0), equalTo(Layout.DIR_RIGHT_TO_LEFT));
    }

    @Test(expected = NullPointerException.class)
    public void builder_setTextDirection_withNull_throwsNPE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0).setTextDirection(null);
    }

    @Test
    public void builder_defaultTextDirection_withTextStrongLTR_returnLTR() {
        final CharSequence text = "ab";
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, new TextPaint(), 0)
                .build();

        assertThat(staticLayout.getParagraphDirection(0), equalTo(Layout.DIR_LEFT_TO_RIGHT));
    }

    @Test
    public void builder_defaultTextDirection_withTextStrongRTL_returnRTL() {
        final CharSequence text = "\u05D0\u05D1";
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, new TextPaint(), 0)
                .build();

        assertThat(staticLayout.getParagraphDirection(0), equalTo(Layout.DIR_RIGHT_TO_LEFT));
    }

    @Test
    public void builder_defaultTextDirection_withoutStrongChar_returnLTR() {
        final CharSequence text = "..";
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text,
                new TextPaint(), 0).build();

        assertThat(staticLayout.getParagraphDirection(0), equalTo(Layout.DIR_LEFT_TO_RIGHT));
    }

    @Test
    public void builder_setLineSpacingExtra_returnsGivenValues() {
        final float lineSpacingExtra = 1.0f;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setLineSpacingExtra(lineSpacingExtra)
                .build();

        assertThat(staticLayout.getSpacingAdd(), equalTo(lineSpacingExtra));
    }

    @Test
    public void builder_setLineSpacingMultiplier_returnsGivenValues() {
        final float lineSpacingMultiplier = 2.0f;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setLineSpacingMultiplier(lineSpacingMultiplier)
                .build();

        assertThat(staticLayout.getSpacingMultiplier(), equalTo(lineSpacingMultiplier));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setLineSpacingMultiplier_withNegativeFloat_throwsIAE() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setLineSpacingMultiplier(-1.0f)
                .build();
    }

    @Test
    public void builder_defaultLineSpacing_lineSpacingExtraEqualsZero() {
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .build();

        assertThat(staticLayout.getSpacingAdd(), equalTo(0.0f));
    }

    @Test
    public void builder_defaultLineSpacing_lineSpacingMultiplierEqualsOne() {
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .build();

        assertThat(staticLayout.getSpacingMultiplier(), equalTo(1.0f));
    }

    @Test
    public void builder_setEllipsize_withMaxLinesEqualsOne_withShortText_isNotEllipsized() {
        final String text = "abc";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        int width = (int) Math.floor(charWidth * text.length()) + 10;
        int ellipsizedWidth = width;
        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(ellipsizedWidth)
                .setMaxLines(1)
                .build();

        // Ellipsized char in the first line should be zero
        assertThat(staticLayout.getEllipsisCount(0), equalTo(0));
    }

    @Test
    public void builder_setEllipsize_withMaxLinesEqualsOne_withLongText_isEllipsized() {
        final String text = "abc";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        int width = (int) Math.floor(charWidth * text.length());
        int ellipsizedWidth = width - 1;

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(ellipsizedWidth)
                .setMaxLines(1)
                .build();

        assertThat(staticLayout.getEllipsisCount(0), greaterThan(0));
    }

    // Testing of BreakStrategy is non-trivial, only test if it will crash.
    @Test
    public void builder_setBreakStrategySimple_notCrash() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                .build();
    }

    @Test
    public void builder_setBreakStrategyHighQuality_notCrash() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .build();
    }

    @Test
    public void builder_setBreakStrategyBalanced_notCrash() {
        new StaticLayoutCompat.Builder("", new TextPaint(), 0)
                .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                .build();
    }

    // TODO(Migration/haoyuchang): Hyphenation is not working proper before API 28, need to support
    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void builder_setHyphenationFrequencyNone_isNotHyphenated() {
        final String text = "writing";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float width = ("writ".length() + 2) * charWidth;

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, (int) width)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .build();

        // If hyphenation is off, "writing" will become "writin" + "\n" +"g".
        // The second line should start with 'g'.
        assertThat(staticLayout.getLineStart(1), equalTo("writin".length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void builder_setHyphenationFrequencyNormal_isHyphenated() {
        final String text = "writing";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float width = ("writ".length() + 2) * charWidth;

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, (int) width)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
                .build();

        assertThat(staticLayout.getLineCount(), equalTo(2));
        // If hyphenation is on, "writing" will become "writ-" + "\n" + "ing".
        // The second line should start with 'i'.
        assertThat(staticLayout.getLineStart(1), equalTo("writ".length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void builder_setHyphenationFrequencyFull_isHyphenated() {
        final String text = "writing";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float width = ("writ".length() + 2) * charWidth;

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, (int) width)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
                .build();

        assertThat(staticLayout.getLineCount(), equalTo(2));
        // If hyphenation is on, "writing" will become "writ-" + "\n" + "ing".
        // The second line should start with 'i'.
        assertThat(staticLayout.getLineStart(1), equalTo("writ".length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void builder_defaultHyphenationFrequency_isNone() {
        final String text = "writing";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float width = ("writ".length() + 2) * charWidth;

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, (int) width)
                .build();

        assertThat(staticLayout.getLineCount(), equalTo(2));
        // If hyphenation is off, "writing" will become "writin" + "\n" +"g".
        // The second line should start with 'g'.
        assertThat(staticLayout.getLineStart(1), equalTo("writin".length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void builder_setJustificationModeNone_noJustification() {
        final String text = "a b c";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float extra = charWidth / 2;
        final int width = (int) Math.floor("a b".length() * charWidth + extra);

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .setJustificationMode(Layout.JUSTIFICATION_MODE_NONE)
                .build();

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount(), greaterThan(1));
        // If there is no justification, first line width should not be changed.
        final float firstLineWidth = "a b".length() * charWidth;
        assertThat(staticLayout.getLineRight(0), equalTo(firstLineWidth));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void builder_setJustificationModeInterWord_isJustified() {
        final String text = "a b c";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float extra = charWidth / 2;
        final int width = (int) Math.floor("a b".length() * charWidth + extra);

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                .build();

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount(), greaterThan(1));
        // The line right much be greater than text length when justification is on.
        assertThat(staticLayout.getLineRight(0), greaterThan("a b".length() * charWidth));
        // The line right ideally should be width. But before API 28, justification shows an extra
        // space at the end of each line. So we tolerate those cases by make sure light right is
        // bigger than width - sizeOfSpace, where sizeOfSpace equals extra / spaceNum.
        final int spaceNum = "a b".split(" ").length - 1;
        final float lineRightLowerBoundary = width - extra / (spaceNum + 1);
        assertThat(staticLayout.getLineRight(0), greaterThanOrEqualTo(lineRightLowerBoundary));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void builder_defaultJustificationMode_isNone() {
        final String text = "a b c";
        final float charWidth = 20.0f;
        final TextPaint paint = getPaintWithCharWidth(charWidth);

        final float extra = charWidth / 2;
        final int width = (int) Math.floor("a b".length() * charWidth + extra);

        final StaticLayout staticLayout = new StaticLayoutCompat.Builder(text, paint, width)
                .build();

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount(), greaterThan(1));
        // If there is no justification, first line width should not be changed.
        final float firstLineWidth = "a b".length() * charWidth;
        assertThat(staticLayout.getLineRight(0), equalTo(firstLineWidth));
    }

    // Returns a paint which render characters with width equals to given charWidth.
    TextPaint getPaintWithCharWidth(float charWidth) {
        final TextPaint paint = new TextPaint();
        paint.setTypeface(mSampleFont);
        paint.setTextSize(charWidth);
        return paint;
    }
}
