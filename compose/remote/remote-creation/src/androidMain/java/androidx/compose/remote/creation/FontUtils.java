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
package androidx.compose.remote.creation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;

import androidx.compose.remote.core.operations.BitmapFontData;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FontUtils {
    private FontUtils() {}

    /**
     * Set the font on the paint
     *
     * @param activity
     * @param paint
     * @param fontId
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    public static void setFontOnPaint(
            @NonNull Activity activity, @NonNull Paint paint, int fontId) {
        Typeface typeface = activity.getResources().getFont(fontId);
        paint.setTypeface(typeface);
    }

    /**
     * Create glyphs
     *
     * @param rc
     * @param str
     * @param paint
     * @return
     */
    public static BitmapFontData.Glyph @NonNull [] createGlyphs(
            @NonNull RemoteComposeWriter rc, @NonNull String str, @NonNull Paint paint) {
        char[] chars = str.toCharArray();

        Arrays.sort(chars);
        int count = 1;
        for (int i = 1; i < chars.length; i++) {
            if (chars[count - 1] != chars[i]) {
                chars[count++] = chars[i];
            }
        }
        str = new String(chars, 0, count);
        ArrayList<BitmapFontData.Glyph> glyphs = new ArrayList<>();
        for (int i = 0; i < str.length(); i++) {
            glyphs.add(createGlyph(rc, "" + str.charAt(i), paint));
        }
        return glyphs.toArray(new BitmapFontData.Glyph[0]);
    }

    /**
     * Extracts the kerning table for the glyphs.
     *
     * @param glyphs The array of {@link BitmapFontData.Glyph}s for which the kerning table will be
     *     extracted.
     * @param paint The {@link Paint} from which kerning data will be extracted.
     * @return The kerning table for these glyphs.
     */
    public static @NonNull HashMap<String, Short> extractKerningTable(
            BitmapFontData.Glyph @NonNull [] glyphs, @NonNull Paint paint) {
        HashMap<String, Short> kerningTable = new HashMap<>();
        for (BitmapFontData.Glyph a : glyphs) {
            for (BitmapFontData.Glyph b : glyphs) {
                String glyphPair = a.mChars + b.mChars;
                int sizeAB = (int) paint.measureText(glyphPair, 0, glyphPair.length());
                int kerningAdjustment = sizeAB - a.mBitmapWidth - b.mBitmapWidth;
                if (kerningAdjustment != 0) {
                    kerningTable.put(glyphPair, (short) kerningAdjustment);
                }
            }
        }
        return kerningTable;
    }

    private static BitmapFontData.@NonNull Glyph createGlyph(
            @NonNull RemoteComposeWriter rc, @NonNull String str, @NonNull Paint paint) {
        Rect rect = new Rect();
        paint.setStrokeWidth(2);
        Paint.FontMetrics fm = paint.getFontMetrics();
        rect.top = 0;
        rect.left = 0;
        rect.bottom = (int) (fm.descent - fm.ascent);
        rect.right = (int) paint.measureText(str, 0, 1);

        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(str, 0, -fm.ascent, paint);

        int id = rc.addBitmap(bitmap);
        return new BitmapFontData.Glyph(
                str,
                id,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) rect.width(),
                (short) rect.height());
    }
}
