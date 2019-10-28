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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.text.TextPaint;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class ShadowSpanTest {
    @Test
    public void updateDrawStateTest() {
        final int color = 0xFF00FF00;
        final float offsetX = 1f;
        final float offsetY = 2f;
        final float radius = 3f;
        final ShadowSpan shadowSpan = new ShadowSpan(color, offsetX, offsetY, radius);

        final TextPaint textPaint = mock(TextPaint.class);
        shadowSpan.updateDrawState(textPaint);
        verify(textPaint, times(1)).setShadowLayer(radius, offsetX, offsetY, color);
    }
}
