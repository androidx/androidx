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

package androidx.text.style;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import android.text.TextPaint;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SdkSuppress(minSdkVersion = 29)
@RunWith(JUnit4.class)
@SmallTest
public class WordSpacingSpanTest {
    @Test
    public void getWordSpacing_sameAsGiven() {
        final float wordSpacing = 2.0f;
        final WordSpacingSpan span = new WordSpacingSpan(wordSpacing);
        assertThat(span.getWordSpacing(), equalTo(wordSpacing));
    }

    @Test
    public void updateDrawState_doesNotThrowException() {
        final WordSpacingSpan span = new WordSpacingSpan(2.0f);
        final TextPaint paint = new TextPaint();

        span.updateDrawState(paint);
    }

    @Test
    public void updateDrawState_increaseWordSpacing() {
        final float wordSpacing = 2.0f;
        final WordSpacingSpan span = new WordSpacingSpan(wordSpacing);
        final TextPaint paint = new TextPaint();

        final float widthBefore = paint.measureText("a b");
        span.updateDrawState(paint);
        final float widthAfter = paint.measureText("a b");

        assertThat(widthAfter - widthBefore, equalTo(wordSpacing));
    }

    @Test
    public void updateDrawState_decreaseWordSpacing() {
        final float wordSpacing = -2.0f;
        final WordSpacingSpan span = new WordSpacingSpan(wordSpacing);
        final TextPaint paint = new TextPaint();

        final float widthBefore = paint.measureText("a b");
        span.updateDrawState(paint);
        final float widthAfter = paint.measureText("a b");

        assertThat(widthAfter - widthBefore, equalTo(wordSpacing));
    }

    @Test
    public void updateMeasureState_doesNotThrowException() {
        final WordSpacingSpan span = new WordSpacingSpan(2.0f);
        final TextPaint paint = new TextPaint();

        span.updateMeasureState(paint);
    }

    @Test
    public void updateMeasureState_increaseWordSpacing() {
        final float wordSpacing = 2.0f;
        final WordSpacingSpan span = new WordSpacingSpan(wordSpacing);
        final TextPaint paint = new TextPaint();

        final float widthBefore = paint.measureText("a b");
        span.updateMeasureState(paint);
        final float widthAfter = paint.measureText("a b");

        assertThat(widthAfter - widthBefore, equalTo(wordSpacing));
    }

    @Test
    public void updateMeasureState_decreaseWordSpacing() {
        final float wordSpacing = -2.0f;
        final WordSpacingSpan span = new WordSpacingSpan(wordSpacing);
        final TextPaint paint = new TextPaint();

        final float widthBefore = paint.measureText("a b");
        span.updateMeasureState(paint);
        final float widthAfter = paint.measureText("a b");

        assertThat(widthAfter - widthBefore, equalTo(wordSpacing));
    }
}
