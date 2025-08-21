/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

/** Unit tests for {@link BitmapTextMeasure}. */
@RunWith(MockitoJUnitRunner.class)
public class BitmapTextMeasureTest {

    @Mock private RemoteContext mRemoteContext;
    @Mock private PaintContext mPaintContext;

    @Captor private ArgumentCaptor<Integer> mIdCaptor;
    @Captor private ArgumentCaptor<Float> mValueCaptor;

    private static final int FONT_ID = 1;
    private static final int TEXT_ID = 2;
    private static final int RESULT_ID = 3;
    private static final float DELTA = 0.01f;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mPaintContext.getContext()).thenReturn(mRemoteContext);
    }

    @Test
    public void testMeasureWidth_simple() {
        BitmapFontData.Glyph[] glyphs = {
            createGlyph("A", 10, 10, 20, 1, 2, 1, 2), createGlyph("B", 11, 12, 20, 2, 2, 2, 2)
        };
        BitmapFontData font = new BitmapFontData(FONT_ID, glyphs);
        when(mRemoteContext.getObject(FONT_ID)).thenReturn(font);
        when(mRemoteContext.getText(TEXT_ID)).thenReturn("AB");

        BitmapTextMeasure measureOp =
                new BitmapTextMeasure(RESULT_ID, TEXT_ID, FONT_ID, BitmapTextMeasure.MEASURE_WIDTH);
        measureOp.paint(mPaintContext);

        // Verify that the correct width is loaded into the context.
        // Total width = (1+10+1 for 'A') + (2+12+2 for 'B') = 12 + 16 = 28.
        verify(mRemoteContext).loadFloat(mIdCaptor.capture(), mValueCaptor.capture());
        assertEquals(RESULT_ID, mIdCaptor.getValue().intValue());
        assertEquals(28f, mValueCaptor.getValue(), DELTA);
    }

    @Test
    public void testMeasureWidth_withKerning() {
        BitmapFontData.Glyph[] glyphs = {
            createGlyph("A", 10, 10, 20, 1, 2, 1, 2), createGlyph("V", 11, 10, 20, 1, 2, 1, 2)
        };
        Map<String, Short> kerningTable = new HashMap<>();
        kerningTable.put("AV", (short) -2); // Negative kerning brings glyphs closer.
        BitmapFontData font = new BitmapFontData(FONT_ID, glyphs, (short) 1, kerningTable);

        when(mRemoteContext.getObject(FONT_ID)).thenReturn(font);
        when(mRemoteContext.getText(TEXT_ID)).thenReturn("AV");

        BitmapTextMeasure measureOp =
                new BitmapTextMeasure(RESULT_ID, TEXT_ID, FONT_ID, BitmapTextMeasure.MEASURE_WIDTH);
        measureOp.paint(mPaintContext);

        // Total width = (1+10+1) + (1+10+1) - 2 (kern) = 12 + 12 - 2 = 22.
        verify(mRemoteContext).loadFloat(mIdCaptor.capture(), mValueCaptor.capture());
        assertEquals(RESULT_ID, mIdCaptor.getValue().intValue());
        assertEquals(22f, mValueCaptor.getValue(), DELTA);
    }

    @Test
    public void testMeasureWidth_multiCharacterGlyph() {
        BitmapFontData.Glyph[] glyphs = {
            createGlyph("O", 10, 8, 20, 1, 2, 1, 2),
            createGlyph("K", 11, 9, 20, 1, 2, 1, 2),
            createGlyph("OK", 12, 15, 20, 2, 2, 2, 2)
        };
        BitmapFontData font = new BitmapFontData(FONT_ID, glyphs);
        when(mRemoteContext.getObject(FONT_ID)).thenReturn(font);
        when(mRemoteContext.getText(TEXT_ID)).thenReturn("OK");

        BitmapTextMeasure measureOp =
                new BitmapTextMeasure(RESULT_ID, TEXT_ID, FONT_ID, BitmapTextMeasure.MEASURE_WIDTH);
        measureOp.paint(mPaintContext);

        // Width should be based on the single "OK" glyph (2+15+2 = 19).
        verify(mRemoteContext).loadFloat(mIdCaptor.capture(), mValueCaptor.capture());
        assertEquals(RESULT_ID, mIdCaptor.getValue().intValue());
        assertEquals(19f, mValueCaptor.getValue(), DELTA);
    }

    @Test
    public void testMeasureHeight() {
        BitmapFontData.Glyph[] glyphs = {
            // yMin = marginTop, yMax = marginTop + height + marginBottom
            createGlyph("A", 10, 10, 20, 1, 5, 1, 5), // yMin=5, yMax=30
            createGlyph("B", 11, 12, 15, 2, 10, 2, 5) // yMin=10, yMax=30
        };
        BitmapFontData font = new BitmapFontData(FONT_ID, glyphs);
        when(mRemoteContext.getObject(FONT_ID)).thenReturn(font);
        when(mRemoteContext.getText(TEXT_ID)).thenReturn("AB");

        BitmapTextMeasure measureOp =
                new BitmapTextMeasure(
                        RESULT_ID, TEXT_ID, FONT_ID, BitmapTextMeasure.MEASURE_HEIGHT);
        measureOp.paint(mPaintContext);

        // Height = overall_yMax - overall_yMin = max(30,30) - min(5,10) = 25.
        verify(mRemoteContext).loadFloat(mIdCaptor.capture(), mValueCaptor.capture());
        assertEquals(RESULT_ID, mIdCaptor.getValue().intValue());
        assertEquals(25f, mValueCaptor.getValue(), DELTA);
    }

    @Test
    public void testMeasure_unknownCharacterIsSkipped() {
        BitmapFontData.Glyph[] glyphs = {
            createGlyph("A", 10, 10, 20, 1, 2, 1, 2), createGlyph("C", 11, 14, 20, 3, 2, 3, 2)
        };
        BitmapFontData font = new BitmapFontData(FONT_ID, glyphs);
        when(mRemoteContext.getObject(FONT_ID)).thenReturn(font);
        when(mRemoteContext.getText(TEXT_ID)).thenReturn("ABC"); // B is unknown.

        BitmapTextMeasure measureOp =
                new BitmapTextMeasure(RESULT_ID, TEXT_ID, FONT_ID, BitmapTextMeasure.MEASURE_WIDTH);
        measureOp.paint(mPaintContext);

        // Width for "A" (12) + width for "C" (20) = 32. 'B' is ignored.
        verify(mRemoteContext).loadFloat(mIdCaptor.capture(), mValueCaptor.capture());
        assertEquals(RESULT_ID, mIdCaptor.getValue().intValue());
        assertEquals(32f, mValueCaptor.getValue(), DELTA);
    }

    /** Helper to create a new glyph with specified metrics. */
    private BitmapFontData.Glyph createGlyph(
            String chars,
            int bitmapId,
            int width,
            int height,
            int marginLeft,
            int marginTop,
            int marginRight,
            int marginBottom) {
        return new BitmapFontData.Glyph(
                chars,
                bitmapId,
                (short) marginLeft,
                (short) marginTop,
                (short) marginRight,
                (short) marginBottom,
                (short) width,
                (short) height);
    }
}
