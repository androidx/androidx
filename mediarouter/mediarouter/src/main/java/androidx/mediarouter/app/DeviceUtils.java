/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.mediarouter.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.R;

/** Utility methods for checking properties of the current device. */
final class DeviceUtils {
    @Nullable
    private static Boolean sIsPhone;
    @Nullable
    private static Boolean sIsTablet;
    @Nullable
    private static Boolean sIsFoldable;
    @Nullable
    private static Boolean sIsSevenInchTablet;
    @Nullable
    private static Boolean sIsWearable;
    @Nullable
    private static Boolean sIsAuto;
    @Nullable
    private static Boolean sIsTv;

    /** The feature name for Auto devices. */
    private static final String FEATURE_AUTO = "android.hardware.type.automotive";

    /** The feature names for TV devices, either of which will qualify. */
    private static final String FEATURE_TV_1 = "com.google.android.tv";

    private static final String FEATURE_TV_2 = "android.hardware.type.television";
    private static final String FEATURE_TV_3 = "android.software.leanback";

    /** The minimum screen width for a 7-inch tablet. */
    private static final @Dimension(unit = Dimension.DP) int
            SEVEN_INCH_TABLET_MINIMUM_SCREEN_WIDTH_DP = 600;

    private DeviceUtils() {
    }

    /**
     * Returns a device specific string representation of wifi warning description. It can be one
     * of the following values: {@link #{R.string.mr_chooser_wifi_warning_description_phone} and
     * @link #{R.string.mr_chooser_wifi_warning_description_tablet} and
     * @link #{R.string.mr_chooser_wifi_warning_description_tv} and
     * @link #{R.string.mr_chooser_wifi_warning_description_watch} and
     * @link #{R.string.mr_chooser_wifi_warning_description_car} and
     * @link #{R.string.mr_chooser_wifi_warning_description_unknown}}
     */
    static String getDialogChooserWifiWarningDescription(@NonNull Context context) {
        if (isPhone(context) || isFoldable(context)) {
            return context.getString(R.string.mr_chooser_wifi_warning_description_phone);
        } else if (isTablet(context) || isSevenInchTablet(context)) {
            return context.getString(R.string.mr_chooser_wifi_warning_description_tablet);
        } else if (isTv(context)) {
            return context.getString(R.string.mr_chooser_wifi_warning_description_tv);
        } else if (isWearable(context)) {
            return context.getString(R.string.mr_chooser_wifi_warning_description_watch);
        } else if (isAuto(context)) {
            return context.getString(R.string.mr_chooser_wifi_warning_description_car);
        } else {
            return context.getString(R.string.mr_chooser_wifi_warning_description_unknown);
        }
    }

    /** Returns {@code true} if the current device is considered a phone. */
    private static boolean isPhone(@NonNull Context context) {
        if (sIsPhone == null) {
            sIsPhone =
                    (!isTablet(context)
                            && !isWearable(context)
                            && !isAuto(context)
                            && !isTv(context));
        }
        return sIsPhone;
    }

    /** Returns {@code true} if the current device considered a tablet (Honeycomb+ and XLarge). */
    private static boolean isTablet(@NonNull Context context) {
        return isTablet(context.getResources());
    }

    /** Returns {@code true} if the current device considered a tablet (Honeycomb+ and XLarge). */
    private static boolean isTablet(@NonNull Resources resources) {
        if (resources == null) {
            return false;
        }
        if (sIsTablet == null) {
            // Consider it to be tablet if it's either a) xlarge-v11, or b) sw600dp-v13.
            boolean isXlarge =
                    (resources.getConfiguration().screenLayout
                            & Configuration.SCREENLAYOUT_SIZE_MASK)
                            > Configuration.SCREENLAYOUT_SIZE_LARGE;
            sIsTablet = isXlarge || isSevenInchTablet(resources);
        }
        return sIsTablet;
    }

    /**
     * Returns {@code true} if the current device considered a foldable (R+ and has a hinge).
     *
     * <p>Note: this is not mutually exclusive with {@link #sIsPhone}
     */
    private static boolean isFoldable(@NonNull Context context) {
        if (sIsFoldable == null) {
            SensorManager sensorManager =
                    (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sIsFoldable =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            && sensorManager != null
                            && sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) != null;
        }
        return sIsFoldable;
    }

    /**
     * Returns {@code true} if the current device is considered to be a 7-inch tablet (e.g. and not
     * a 10-inch tablet).
     */
    private static boolean isSevenInchTablet(@NonNull Context context) {
        return isSevenInchTablet(context.getResources());
    }

    /**
     * Returns {@code true} if the current device is considered to be a 7-inch tablet (e.g. and not
     * a 10-inch tablet).
     */
    private static boolean isSevenInchTablet(@NonNull Resources resources) {
        if (resources == null) {
            return false;
        }
        if (sIsSevenInchTablet == null) {
            Configuration configuration = resources.getConfiguration();
            sIsSevenInchTablet =
                    (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                            <= Configuration.SCREENLAYOUT_SIZE_LARGE
                            && configuration.smallestScreenWidthDp
                            >= SEVEN_INCH_TABLET_MINIMUM_SCREEN_WIDTH_DP;
        }
        return sIsSevenInchTablet;
    }

    /**
     * Returns {@code true} if the current device is a wearable. As of 2019Q4, that is synonymous
     * with being a watch.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code WEARABLE}
     * and {@code false} for all other build types.
     */
    private static boolean isWearable(@NonNull Context context) {
        return isWearable(context.getPackageManager());
    }

    /**
     * Returns {@code true} if the current device is a wearable. As of 2019Q4, that is synonymous
     * with being a watch.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code WEARABLE}
     * and {@code false} for all other build types.
     */
    private static boolean isWearable(@NonNull PackageManager packageManager) {
        if (sIsWearable == null) {
            sIsWearable =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                            && packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
        }
        return sIsWearable;
    }

    /**
     * Returns {@code true} if the device should be considered as a car.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code AUTO} and
     * {@code false} for all other build types.
     */
    private static boolean isAuto(@NonNull Context context) {
        return isAuto(context.getPackageManager());
    }

    /**
     * Returns {@code true} if the device should be considered as a car.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code AUTO} and
     * {@code false} for all other build types.
     */
    private static boolean isAuto(@NonNull PackageManager packageManager) {
        if (sIsAuto == null) {
            sIsAuto = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && packageManager.hasSystemFeature(FEATURE_AUTO);
        }
        return sIsAuto;
    }

    /**
     * Returns {@code true} if the device should be considered as a TV.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code ATV} and
     * {@code false} for all other build types except {@code PHONE_PRE_LMP} (aka {@code PROD}).
     */
    private static boolean isTv(@NonNull Context context) {
        return DeviceUtils.isTv(context.getPackageManager());
    }

    /**
     * Returns {@code true} if the device should be considered as a TV.
     *
     * <p>For container release builds, returns {@code true} if the build type is {@code ATV} and
     * {@code false} for all other build types except {@code PHONE_PRE_LMP} (aka {@code PROD}).
     */
    private static boolean isTv(@NonNull PackageManager packageManager) {
        if (sIsTv == null) {
            sIsTv =
                    packageManager.hasSystemFeature(FEATURE_TV_1)
                            || packageManager.hasSystemFeature(FEATURE_TV_2)
                            || packageManager.hasSystemFeature(FEATURE_TV_3);
        }
        return sIsTv;
    }
}
