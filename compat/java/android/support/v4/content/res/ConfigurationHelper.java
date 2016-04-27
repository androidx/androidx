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

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Helper class which allows access to properties of {@link android.content.res.Configuration} in
 * a backward compatible fashion.
 */
public final class ConfigurationHelper {

    private static final ConfigurationHelperImpl IMPL;

    static {
        final int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 17) {
            IMPL = new JellybeanMr1Impl();
        } else if (sdk >= 13) {
            IMPL = new HoneycombMr2Impl();
        } else {
            IMPL = new DonutImpl();
        }
    }

    private ConfigurationHelper() {}

    private interface ConfigurationHelperImpl {
        int getScreenHeightDp(@NonNull Resources resources);
        int getScreenWidthDp(@NonNull Resources resources);
        int getSmallestScreenWidthDp(@NonNull Resources resources);
        int getDensityDpi(@NonNull Resources resources);
    }

    private static class DonutImpl implements ConfigurationHelperImpl {
        @Override
        public int getScreenHeightDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getScreenHeightDp(resources);
        }

        @Override
        public int getScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getScreenWidthDp(resources);
        }

        @Override
        public int getSmallestScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getSmallestScreenWidthDp(resources);
        }

        @Override
        public int getDensityDpi(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getDensityDpi(resources);
        }
    }

    private static class HoneycombMr2Impl extends DonutImpl {
        @Override
        public int getScreenHeightDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getScreenHeightDp(resources);
        }

        @Override
        public int getScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getScreenWidthDp(resources);
        }

        @Override
        public int getSmallestScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getSmallestScreenWidthDp(resources);
        }
    }

    private static class JellybeanMr1Impl extends HoneycombMr2Impl {
        @Override
        public int getDensityDpi(@NonNull Resources resources) {
            return ConfigurationHelperJellybeanMr1.getDensityDpi(resources);
        }
    }

    /**
     * Returns the current height of the available screen space, in dp units.
     *
     * <p>Uses {@code Configuration.screenHeightDp} when available, otherwise an approximation
     * is computed and returned.</p>
     */
    public static int getScreenHeightDp(@NonNull Resources resources) {
        return IMPL.getScreenHeightDp(resources);
    }

    /**
     * Returns the current width of the available screen space, in dp units.
     *
     * <p>Uses {@code Configuration.screenWidthDp} when available, otherwise an approximation
     * is computed and returned.</p>
     */
    public static int getScreenWidthDp(@NonNull Resources resources) {
        return IMPL.getScreenWidthDp(resources);
    }

    /**
     * Returns The smallest screen size an application will see in normal operation, in dp units.
     *
     * <p>Uses {@code Configuration.smallestScreenWidthDp} when available, otherwise an
     * approximation is computed and returned.</p>
     */
    public static int getSmallestScreenWidthDp(@NonNull Resources resources) {
        return IMPL.getSmallestScreenWidthDp(resources);
    }

    /**
     * Returns the target screen density being rendered to.
     *
     * <p>Uses {@code Configuration.densityDpi} when available, otherwise an approximation
     * is computed and returned.</p>
     */
    public static int getDensityDpi(@NonNull Resources resources) {
        return IMPL.getDensityDpi(resources);
    }
}
