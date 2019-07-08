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

package androidx.text.style;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import android.graphics.Typeface;
import android.text.TextPaint;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class TypefaceSpanTest {
    @Test
    public void updateDrawState_changesTypeface() {
        final Typeface typeface = mock(Typeface.class);
        final TypefaceSpan span = new TypefaceSpan(typeface);
        final TextPaint paint = new TextPaint();

        span.updateDrawState(paint);

        assertThat(paint.getTypeface(), sameInstance(typeface));
    }

    @Test
    public void updateMeasureState_changesTypeface() {
        final Typeface typeface = mock(Typeface.class);
        final TypefaceSpan span = new TypefaceSpan(typeface);
        final TextPaint paint = new TextPaint();

        span.updateMeasureState(paint);

        assertThat(paint.getTypeface(), sameInstance(typeface));
    }
}
