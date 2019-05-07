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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import android.text.BoringLayout;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SmallTest
@RunWith(JUnit4.class)
public class BoringLayoutCompatTest {

    @Test
    public void builder_constructor_returnsGivenValues() {
        final CharSequence text = "abc";
        final TextPaint paint = new TextPaint();
        final int width = 100;
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);

        BoringLayout boringLayout = new BoringLayoutCompat.Builder(text, paint, width, metrics)
                .build();

        assertThat(boringLayout.getText(), equalTo(text));
        assertThat(boringLayout.getPaint(), equalTo(paint));
        // The width and height of the boringLayout is the same in metrics, indicating metrics is
        // passed correctly.
        assertThat((int) boringLayout.getLineWidth(0), equalTo(metrics.width));
        assertThat(boringLayout.getLineBottom(0) - boringLayout.getLineTop(0),
                equalTo(metrics.bottom - metrics.top));
        assertThat(boringLayout.getWidth(), equalTo(width));
    }

    @Test(expected = NullPointerException.class)
    public void builder_constructor_withPaintNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", null, 0, new BoringLayout.Metrics());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_constructor_withWidthNegative_throwsIAE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), -1, new BoringLayout.Metrics());
    }

    @Test(expected = NullPointerException.class)
    public void builder_constructor_withMetricsNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, null);
    }

    @Test
    public void builder_build_returnNotNull() {
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics()).build();

        assertNotNull(boringLayout);
    }

    @Test(expected = NullPointerException.class)
    public void builder_setText_withNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setText(null);
    }

    @Test
    public void builder_setText_returnsGivenText() {
        CharSequence text = "abcd";
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics())
                .setText(text)
                .build();

        assertThat(boringLayout.getText(), equalTo(text));
    }

    @Test(expected = NullPointerException.class)
    public void builder_setPaint_withNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setPaint(null);
    }

    @Test
    public void builder_setPaint_returnsGivenPaint() {
        final TextPaint paint = new TextPaint();
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics())
                .setPaint(paint)
                .build();

        assertThat(boringLayout.getPaint(), equalTo(paint));
    }

    @Test
    public void builder_setWidth_returnsGivenWidth() {
        final int width = 90;
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics())
                .setWidth(90)
                .build();

        assertThat(boringLayout.getWidth(), equalTo(width));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setWidth_withNegative_throwsIAE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setWidth(-1);
    }

    @Test(expected = NullPointerException.class)
    public void builder_setMetrics_withNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setMetrics(null);
    }

    @Test(expected = NullPointerException.class)
    public void builder_setAlignment_withNull_throwsNPE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setAlignment(null);
    }

    @Test
    public void builder_setAlignment_returnGivenValue() {
        final Layout.Alignment align = Layout.Alignment.ALIGN_OPPOSITE;
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics())
                .setAlignment(align)
                .build();

        assertThat(boringLayout.getAlignment(), equalTo(align));
    }

    @Test
    public void builder_defaultAlignment_isAlignNormal() {
        final BoringLayout boringLayout = new BoringLayoutCompat.Builder("",
                new TextPaint(), 0, new BoringLayout.Metrics())
                .build();

        assertThat(boringLayout.getAlignment(), equalTo(Layout.Alignment.ALIGN_NORMAL));
    }

    @Test
    public void builder_setIncludePad_withTrue_useTopAndBottomAsAscendAndDescend() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, metrics.width, metrics)
                .setIncludePad(true)
                .build();

        assertThat(boringLayout.getLineAscent(0), equalTo(metrics.top));
        assertThat(boringLayout.getLineDescent(0), equalTo(metrics.bottom));
    }

    @Test
    public void builder_setIncludePad_withFalse_useGivenAscendAndDescend() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, metrics.width, metrics)
                .setIncludePad(false)
                .build();

        assertThat(boringLayout.getLineAscent(0), equalTo(metrics.ascent));
        assertThat(boringLayout.getLineDescent(0), equalTo(metrics.descent));
    }

    @Test
    public void builder_defaultIncludePad_isTrue() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, metrics.width, metrics)
                .setIncludePad(true)
                .build();

        final int topPad = boringLayout.getTopPadding();
        final int bottomPad = boringLayout.getBottomPadding();
        // Top and bottom padding are not 0 at the same time, indicating includePad is true.
        assertThat(topPad * topPad + bottomPad * bottomPad, greaterThan(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_setEllipsizedWidth_withNegative_throwsIAE() {
        new BoringLayoutCompat.Builder("", new TextPaint(), 0, new BoringLayout.Metrics())
                .setEllipsizedWidth(-1);
    }

    @Test
    public void builder_setEllipsize_withShortText_isNotEllipsized() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);
        final int width = metrics.width;

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, width, metrics)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(width)
                .build();

        assertThat(boringLayout.getEllipsisCount(0), equalTo(0));
    }

    @Test
    public void builder_setEllipsize_withLongText_isEllipsized() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);
        final int width = metrics.width;
        final int ellipsizedWidth = width / 2;

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, width, metrics)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(ellipsizedWidth)
                .build();

        assertThat(boringLayout.getEllipsisCount(0), greaterThan(0));
    }

    @Test
    public void builder_defaultEllipsize_isNull() {
        final CharSequence text = "abcdefghijk";
        final TextPaint paint = new TextPaint();
        final BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);
        // Don't give enough space, but boringLayout won't cut the text either.
        final int width = metrics.width / 2;

        final BoringLayout boringLayout = new BoringLayoutCompat.Builder(text,
                paint, width, metrics)
                .build();

        // EllipsisCount should be 0 indicating ellipsize is null.
        assertThat(boringLayout.getEllipsisCount(0), equalTo(0));
    }

}
