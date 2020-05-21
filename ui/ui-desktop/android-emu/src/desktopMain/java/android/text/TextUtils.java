/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.text;

import android.compat.annotation.UnsupportedAppUsage;

import com.android.internal.util.ArrayUtils;

/**
 * TextUtils.
 */
public class TextUtils {
    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)
    private static final String ELLIPSIS_TWO_DOTS = "\u2025"; // TWO DOT LEADER (‥)

    /**
     * getChars
     */
    public static void getChars(CharSequence s, int start, int end,
            char[] dest, int destoff) {
        Class<? extends CharSequence> c = s.getClass();
        if (c == String.class) {
            ((String) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuffer.class) {
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuilder.class) {
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        } else if (s instanceof GetChars) {
            ((GetChars) s).getChars(start, end, dest, destoff);
        } else {
            for (int i = start; i < end; i++) {
                dest[destoff++] = s.charAt(i);
            }
        }
    }

    /**
     * ellipsize.
     */
    public static CharSequence ellipsize(CharSequence text,
            TextPaint p,
            float avail, TruncateAt where) {
        return ellipsize(text, p, avail, where, false, null);
    }

    /**
     * ellipsize.
     */
    public static CharSequence ellipsize(CharSequence text,
            TextPaint paint,
            float avail, TruncateAt where,
            boolean preserveLength,
            EllipsizeCallback callback) {
        return ellipsize(text, paint, avail, where, preserveLength, callback,
                TextDirectionHeuristics.FIRSTSTRONG_LTR,
                getEllipsisString(where));
    }

    /**
     * ellipsize
     */
    public static CharSequence ellipsize(
            CharSequence text,
            TextPaint paint,
            float avail, TruncateAt where,
            boolean preserveLength,
            EllipsizeCallback callback,
            TextDirectionHeuristic textDir, String ellipsis) {
        // TODO: incorrect
        return text;
    }

    /**
     * getEllipsisString
     */
    public static String getEllipsisString(TextUtils.TruncateAt method) {
        return (method == TextUtils.TruncateAt.END_SMALL) ? ELLIPSIS_TWO_DOTS : ELLIPSIS_NORMAL;
    }

    /**
     * obtain
     */
    static char[] obtain(int len) {
        return ArrayUtils.newUnpaddedCharArray(len);
    }

    /**
     * recycle
     */
    static void recycle(char[] temp) {
    }

    /**
     * couldAffectRtl
     */
    static boolean couldAffectRtl(char c) {
        return (0x0590 <= c && c <= 0x08FF) ||  // RTL scripts
                c == 0x200E ||  // Bidi format character
                c == 0x200F ||  // Bidi format character
                (0x202A <= c && c <= 0x202E) ||  // Bidi format characters
                (0x2066 <= c && c <= 0x2069) ||  // Bidi format characters
                (0xD800 <= c && c <= 0xDFFF) ||  // Surrogate pairs
                (0xFB1D <= c && c <= 0xFDFF) ||  // Hebrew and Arabic presentation forms
                (0xFE70 <= c && c <= 0xFEFE);  // Arabic presentation forms
    }

    /**
     * TruncateAt.
     */
    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
        @UnsupportedAppUsage
        END_SMALL
    }

    /**
     * EllipsizeCallback
     */
    public interface EllipsizeCallback {
        /**
         * This method is called to report that the specified region of
         * text was ellipsized away by a call to {@link #ellipsize}.
         */
        void ellipsized(int start, int end);
    }
}
