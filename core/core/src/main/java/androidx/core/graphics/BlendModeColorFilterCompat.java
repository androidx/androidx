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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for accessing ColorFilter APIs on various API levels of the platform
 */
public class BlendModeColorFilterCompat {

    /**
     * Convenience method to create ColorFilter in a backward
     * compatible way. This method falls back on PorterDuffColorFilter for API levels that
     * do not support BlendModeColorFilter. This method returns null if the BlendMode provided is
     * not supported on a given API level.
     */
    public static @Nullable ColorFilter createBlendModeColorFilterCompat(int color,
            @NonNull BlendModeCompat blendModeCompat) {
        if (Build.VERSION.SDK_INT >= 29) {
            BlendMode blendMode = BlendModeUtils.obtainBlendModeFromCompat(blendModeCompat);
            return blendMode != null
                    ? new BlendModeColorFilter(color, blendMode) : null;
        } else {
            PorterDuff.Mode porterDuffMode =
                    BlendModeUtils.obtainPorterDuffFromCompat(blendModeCompat);
            return porterDuffMode != null
                    ? new PorterDuffColorFilter(color, porterDuffMode) : null;
        }
    }

    private BlendModeColorFilterCompat() { }
}
