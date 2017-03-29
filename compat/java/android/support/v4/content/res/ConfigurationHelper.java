/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.content.res;

import static android.os.Build.VERSION.SDK_INT;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;

/**
 * Helper class which allows access to properties of {@link Configuration} in
 * a backward compatible fashion.
 */
public final class ConfigurationHelper {
    private ConfigurationHelper() {
    }

    /**
     * Returns the current height of the available screen space, in dp units.
     *
     * <p>Uses {@code Configuration.screenHeightDp} when available, otherwise an approximation
     * is computed and returned.</p>
     *
     * @deprecated Use {@link Configuration#screenHeightDp} directly.
     */
    @Deprecated
    public static int getScreenHeightDp(@NonNull Resources resources) {
        return resources.getConfiguration().screenHeightDp;
    }

    /**
     * Returns the current width of the available screen space, in dp units.
     *
     * <p>Uses {@code Configuration.screenWidthDp} when available, otherwise an approximation
     * is computed and returned.</p>
     *
     * @deprecated Use {@link Configuration#screenWidthDp} directly.
     */
    @Deprecated
    public static int getScreenWidthDp(@NonNull Resources resources) {
        return resources.getConfiguration().screenWidthDp;
    }

    /**
     * Returns The smallest screen size an application will see in normal operation, in dp units.
     *
     * <p>Uses {@code Configuration.smallestScreenWidthDp} when available, otherwise an
     * approximation is computed and returned.</p>
     *
     * @deprecated Use {@link Configuration#smallestScreenWidthDp} directly.
     */
    @Deprecated
    public static int getSmallestScreenWidthDp(@NonNull Resources resources) {
        return resources.getConfiguration().smallestScreenWidthDp;
    }

    /**
     * Returns the target screen density being rendered to.
     *
     * <p>Uses {@code Configuration.densityDpi} when available, otherwise an approximation
     * is computed and returned.</p>
     */
    public static int getDensityDpi(@NonNull Resources resources) {
        if (SDK_INT >= 17) {
            return resources.getConfiguration().densityDpi;
        } else {
            return resources.getDisplayMetrics().densityDpi;
        }
    }
}
