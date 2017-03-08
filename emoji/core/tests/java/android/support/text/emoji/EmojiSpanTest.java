/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.text.emoji;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Paint.FontMetricsInt;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextPaint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiSpanTest {

    @Before
    public void setup() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Test
    public void testGetSize() throws Exception {
        final short dimensionX = 18;
        final short dimensionY = 20;
        final int fontHeight = 10;
        final float expectedRatio = fontHeight * 1.0f / dimensionY;
        final TextPaint paint = mock(TextPaint.class);

        // mock TextPaint to return test font metrics
        when(paint.getFontMetricsInt(any(FontMetricsInt.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final FontMetricsInt fontMetrics = (FontMetricsInt) invocation.getArguments()[0];
                fontMetrics.ascent = 0;
                fontMetrics.descent = -fontHeight;
                return null;
            }
        });

        final EmojiMetadata metadata = mock(EmojiMetadata.class);
        when(metadata.getWidth()).thenReturn(dimensionX);
        when(metadata.getHeight()).thenReturn(dimensionY);
        final EmojiSpan span = new TypefaceEmojiSpan(metadata);

        final int resultSize = span.getSize(paint, "", 0, 0, null);
        assertEquals((int) (dimensionX * expectedRatio), resultSize);
        assertEquals(expectedRatio, span.getRatio());
        assertEquals((int) (dimensionX * expectedRatio), span.getWidth());
        assertEquals((int) (dimensionY * expectedRatio), span.getHeight());
    }
}
