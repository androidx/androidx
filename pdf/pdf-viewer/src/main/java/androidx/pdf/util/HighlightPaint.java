/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.annotation.RestrictTo;

/**
 * Paint objects used for highlighting in various contexts.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class HighlightPaint {

    private static final int DARKEN_ALPHA = 0x80;

    /** Used when the user selects some text. Light blue as per default Android selection color. */
    public static final Paint SELECTION = createHighlightPaint(160, 215, 255);

    /**
     * Used to highlight every match of the query text. Yellow as per default Android highlight
     * color.
     */
    public static final Paint MATCH = createHighlightPaint(255, 255, 0);

    /**
     * Used to highlight the current match of the query text, from which the user can navigate
     * between
     * matches. Orange.
     */
    public static final Paint CURRENT_MATCH = createHighlightPaint(255, 150, 50);

    /** Used to highlight every filled comment anchor. Yellowish brown. */
    public static final Paint COMMENT_MATCH = createFilledPaint(255, 225, 104, 89);

    /** Used to highlight currently focused, filled comment anchor. Darker yellowish brown. */
    public static final Paint CURRENT_COMMENT_MATCH = createFilledPaint(255, 225, 104, 166);

    /** Used to provide a semi-transparent, filled, darkening effect. */
    public static final Paint DARKEN_PAINT = createDarkenPaint();

    /** Used to highlight every triangle corner comment anchor in a spreadsheet. Dark Yellow. */
    public static final Paint TRIANGLE_CORNER_COMMENT_MATCH = createFilledPaint(243, 165, 55, 255);

    /** Creates a filled paint. */
    private static Paint createFilledPaint(int r, int g, int b, int a) {
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setARGB(a, r, g, b);
        paint.setAntiAlias(true);
        paint.setDither(true);
        return paint;
    }

    /**
     * Creates a Paint that uses PorterDuff.Mode.MULTIPLY, meaning it darkens the light areas of the
     * destination but leaves dark areas mostly unchanged, like a fluorescent highlighter.
     */
    private static Paint createHighlightPaint(int r, int g, int b) {
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        paint.setARGB(255, r, g, b);
        paint.setAntiAlias(true);
        paint.setDither(true);
        return paint;
    }

    /** Creates a semi-transparent, filled Paint that provides a darkening effect. */
    private static Paint createDarkenPaint() {
        Paint darkenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkenPaint.setColor(Color.BLACK);
        darkenPaint.setAlpha(DARKEN_ALPHA);
        darkenPaint.setStyle(Style.FILL);
        return darkenPaint;
    }

    private HighlightPaint() {
        // Static constants.
    }
}
