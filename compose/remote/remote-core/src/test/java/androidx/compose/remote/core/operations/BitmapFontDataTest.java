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
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BitmapFontDataTest {

    @Test
    public void matchDaysOfTheWeek() {
        BitmapFontData bitmapFontData = createSimpleDayAndDateBitmapFontData();

        BitmapFontData.Glyph resultMon = bitmapFontData.lookupGlyph("MON", 0);
        BitmapFontData.Glyph resultTue = bitmapFontData.lookupGlyph("TUE", 0);
        BitmapFontData.Glyph resultWed = bitmapFontData.lookupGlyph("WED", 0);
        BitmapFontData.Glyph resultThu = bitmapFontData.lookupGlyph("THU", 0);
        BitmapFontData.Glyph resultFri = bitmapFontData.lookupGlyph("FRI", 0);
        BitmapFontData.Glyph resultSat = bitmapFontData.lookupGlyph("SAT", 0);
        BitmapFontData.Glyph resultSun = bitmapFontData.lookupGlyph("SUN", 0);

        assertEquals(1, resultMon.mBitmapId);
        assertEquals(3, resultMon.mChars.length());
        assertEquals(2, resultTue.mBitmapId);
        assertEquals(3, resultTue.mChars.length());
        assertEquals(3, resultWed.mBitmapId);
        assertEquals(3, resultWed.mChars.length());
        assertEquals(4, resultThu.mBitmapId);
        assertEquals(3, resultThu.mChars.length());
        assertEquals(5, resultFri.mBitmapId);
        assertEquals(3, resultFri.mChars.length());
        assertEquals(6, resultSat.mBitmapId);
        assertEquals(3, resultSat.mChars.length());
        assertEquals(7, resultSun.mBitmapId);
        assertEquals(3, resultSun.mChars.length());
    }

    @Test
    public void dateFollowedByNumbers() {
        BitmapFontData bitmapFontData = createSimpleDayAndDateBitmapFontData();

        BitmapFontData.Glyph result = bitmapFontData.lookupGlyph("MON123", 0);

        assertEquals(1, result.mBitmapId);
        assertEquals(3, result.mChars.length());
    }

    @Test
    public void matchNumbers() {
        BitmapFontData bitmapFontData = createSimpleDayAndDateBitmapFontData();

        BitmapFontData.Glyph result1 = bitmapFontData.lookupGlyph("1", 0);
        BitmapFontData.Glyph result2 = bitmapFontData.lookupGlyph("2", 0);
        BitmapFontData.Glyph result3 = bitmapFontData.lookupGlyph("3", 0);
        BitmapFontData.Glyph result4 = bitmapFontData.lookupGlyph("4", 0);
        BitmapFontData.Glyph result5 = bitmapFontData.lookupGlyph("5", 0);
        BitmapFontData.Glyph result6 = bitmapFontData.lookupGlyph("6", 0);
        BitmapFontData.Glyph result7 = bitmapFontData.lookupGlyph("7", 0);
        BitmapFontData.Glyph result8 = bitmapFontData.lookupGlyph("8", 0);
        BitmapFontData.Glyph result9 = bitmapFontData.lookupGlyph("9", 0);

        assertEquals(11, result1.mBitmapId);
        assertEquals(1, result1.mChars.length());
        assertEquals(12, result2.mBitmapId);
        assertEquals(1, result2.mChars.length());
        assertEquals(13, result3.mBitmapId);
        assertEquals(1, result3.mChars.length());
        assertEquals(14, result4.mBitmapId);
        assertEquals(1, result4.mChars.length());
        assertEquals(15, result5.mBitmapId);
        assertEquals(1, result5.mChars.length());
        assertEquals(16, result6.mBitmapId);
        assertEquals(1, result6.mChars.length());
        assertEquals(17, result7.mBitmapId);
        assertEquals(1, result7.mChars.length());
        assertEquals(18, result8.mBitmapId);
        assertEquals(1, result8.mChars.length());
        assertEquals(19, result9.mBitmapId);
        assertEquals(1, result9.mChars.length());
    }

    @Test
    public void matchNonExistent() {
        BitmapFontData bitmapFontData = createSimpleDayAndDateBitmapFontData();

        assertNull(bitmapFontData.lookupGlyph("NO", 0));
    }

    @Test
    public void matchGlyphsWithCommonPrefix() {
        BitmapFontData bitmapFontData = createMondaysBitmapFontData();

        BitmapFontData.Glyph result1 = bitmapFontData.lookupGlyph("M", 0);
        BitmapFontData.Glyph result2 = bitmapFontData.lookupGlyph("MON", 0);
        BitmapFontData.Glyph result3 = bitmapFontData.lookupGlyph("MONDAY", 0);

        assertEquals(0, result1.mBitmapId);
        assertEquals(1, result1.mChars.length());
        assertEquals(1, result2.mBitmapId);
        assertEquals(3, result2.mChars.length());
        assertEquals(2, result3.mBitmapId);
        assertEquals(6, result3.mChars.length());
    }

    private BitmapFontData.Glyph glyph(String c, int id) {
        return new BitmapFontData.Glyph(
                c, id, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0);
    }

    private BitmapFontData createSimpleDayAndDateBitmapFontData() {
        BitmapFontData.Glyph[] glyphs = {
            glyph(" ", 0),
            glyph("MON", 1),
            glyph("TUE", 2),
            glyph("WED", 3),
            glyph("THU", 4),
            glyph("FRI", 5),
            glyph("SAT", 6),
            glyph("SUN", 7),
            glyph("0", 10),
            glyph("1", 11),
            glyph("2", 12),
            glyph("3", 13),
            glyph("4", 14),
            glyph("5", 15),
            glyph("6", 16),
            glyph("7", 17),
            glyph("8", 18),
            glyph("9", 19),
            glyph(":", 20)
        };
        return new BitmapFontData(1, glyphs);
    }

    private BitmapFontData createMondaysBitmapFontData() {
        BitmapFontData.Glyph[] glyphs = {
            glyph("M", 0), glyph("MON", 1), glyph("MONDAY", 2),
        };
        return new BitmapFontData(1, glyphs);
    }
}
