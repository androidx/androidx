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

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class FontFeatureSpanTest {
    @Test(expected = NullPointerException.class)
    public void constructor_withNull_throwsException() {
        new FontFeatureSpan(null);
    }

    @Test
    public void updateMeasureState_changePaintFontFeatureSettings() {
        final String fontFeatureSettings = "\"kern\" 0";
        final FontFeatureSpan span = new FontFeatureSpan(fontFeatureSettings);
        final TextPaint textPaint = new TextPaint();
        // Make sure baseline shift is zero when created
        assertThat(textPaint.getFontFeatureSettings(), equalTo(null));

        span.updateMeasureState(textPaint);
        assertThat(textPaint.getFontFeatureSettings(), equalTo(fontFeatureSettings));
    }

    @Test
    public void updateDrawState_changePaintBaselineShift() {
        final String fontFeatureSettings = "\"kern\" 0";
        final FontFeatureSpan span = new FontFeatureSpan(fontFeatureSettings);
        final TextPaint textPaint = new TextPaint();
        // Make sure baseline shift is zero when created
        assertThat(textPaint.getFontFeatureSettings(), equalTo(null));

        span.updateDrawState(textPaint);
        assertThat(textPaint.getFontFeatureSettings(), equalTo(fontFeatureSettings));
    }
}
