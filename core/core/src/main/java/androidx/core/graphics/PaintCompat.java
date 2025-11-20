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

package androidx.core.graphics;

import static androidx.core.graphics.BlendModeUtils.obtainPorterDuffFromCompat;

import android.graphics.BlendMode;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing features in {@link Paint}.
 */
public final class PaintCompat {

    /**
     * Determine whether the typeface set on the paint has a glyph supporting the
     * string in a backwards compatible way.
     *
     * @param paint the paint instance to check
     * @param string the string to test whether there is glyph support
     * @return true if the typeface set on the given paint has a glyph for the string
     */
    public static boolean hasGlyph(@NonNull Paint paint, @NonNull String string) {
        return paint.hasGlyph(string);
    }

    /**
     * Configure the corresponding BlendMode on the given paint. If the Android platform supports
     * the blend mode natively, it will fall back on the framework implementation of either
     * BlendMode or PorterDuff mode. If it is not supported then this method is a no-op
     * @param paint target Paint to which the BlendMode will be applied
     * @param blendMode BlendMode to configure on the paint if it is supported by the platform
     *                  version. A value of null removes the BlendMode from the Paint and restores
     *                  it to the default
     * @return true if the specified BlendMode as applied successfully, false if the platform
     * version does not support this BlendMode. If the BlendMode is not supported, this falls
     * back to the default BlendMode
     */
    public static boolean setBlendMode(@NonNull Paint paint, @Nullable BlendModeCompat blendMode) {
        if (Build.VERSION.SDK_INT >= 29) {
            Object blendModePlatform = blendMode != null
                    ? BlendModeUtils.Api29Impl.obtainBlendModeFromCompat(blendMode) : null;
            Api29Impl.setBlendMode(paint, blendModePlatform);
            // All blend modes supported in Q
            return true;
        } else if (blendMode != null) {
            PorterDuff.Mode mode = obtainPorterDuffFromCompat(blendMode);
            paint.setXfermode(mode != null ? new PorterDuffXfermode(mode) : null);
            // If the BlendMode has an equivalent PorterDuff mode, return true,
            // otherwise return false
            return mode != null;
        } else {
            // Configuration of a null BlendMode falls back to the default which is supported in
            // all platform levels
            paint.setXfermode(null);
            return true;
        }
    }

    private PaintCompat() {
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void setBlendMode(Paint paint, Object blendmode) {
            paint.setBlendMode((BlendMode) blendmode);
        }
    }
}
