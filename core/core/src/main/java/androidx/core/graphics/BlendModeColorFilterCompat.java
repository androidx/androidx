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

package androidx.core.graphics;

import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing ColorFilter APIs on various API levels of the platform
 */
public class BlendModeColorFilterCompat {

    /**
     * Convenience method to create ColorFilter in a backward-compatible way.
     *
     * This method falls back on PorterDuffColorFilter for API levels that do not support
     * BlendModeColorFilter. This method returns {@code null} if the BlendMode provided is not
     * supported on a given API level.
     */
    public static @Nullable ColorFilter createBlendModeColorFilterCompat(int color,
            @NonNull BlendModeCompat blendModeCompat) {
        if (Build.VERSION.SDK_INT >= 29) {
            Object blendMode =
                    BlendModeUtils.Api29Impl.obtainBlendModeFromCompat(blendModeCompat);
            return blendMode != null
                    ? Api29Impl.createBlendModeColorFilter(color, blendMode) : null;
        } else {
            PorterDuff.Mode porterDuffMode =
                    BlendModeUtils.obtainPorterDuffFromCompat(blendModeCompat);
            return porterDuffMode != null
                    ? new PorterDuffColorFilter(color, porterDuffMode) : null;
        }
    }

    private BlendModeColorFilterCompat() {
        // This class is not instantiable.
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static ColorFilter createBlendModeColorFilter(int color, Object mode) {
            return new BlendModeColorFilter(color, (BlendMode) mode);
        }
    }
}
