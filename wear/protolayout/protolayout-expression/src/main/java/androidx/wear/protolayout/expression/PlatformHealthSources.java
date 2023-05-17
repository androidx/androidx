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

package androidx.wear.protolayout.expression;

import static androidx.wear.protolayout.expression.DynamicBuilders.PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE;
import static androidx.wear.protolayout.expression.DynamicBuilders.PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.PlatformInt32Source;

/** Dynamic types for platform health sources. */
public class PlatformHealthSources {
    /** Data sources keys for platform health sources. */
    public static class Keys {
        private Keys() {}

        /** The data source key for heart rate bpm data from platform health sources. */
        @NonNull
        @RequiresPermission(Manifest.permission.BODY_SENSORS)
        public static final PlatformDataKey<DynamicFloat> HEART_RATE_BPM =
                new PlatformDataKey<>("HeartRate");

        /**
         * The data source key for daily step count data from platform health sources. This is the
         * total step count over a day and it resets when 00:00 is reached (in whatever is the
         * timezone set at that time). This can result in the DAILY period being greater than or
         * less than 24 hours when the timezone of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicInt32> DAILY_STEPS =
                new PlatformDataKey<>("Daily Steps");

        /**
         * The data source key for daily distance data (in meters) from platform health sources.
         * This is the total distance over a day and it resets when 00:00 is reached (in whatever
         * is the timezone set at that time). This can result in the DAILY period being greater
         * than or less than 24 hours when the timezone of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicFloat> DAILY_DISTANCE_M =
                new PlatformDataKey<>("Daily Distance");

        /**
         * The data source key for daily calories data from platform health sources. This is the
         * total number of calories over a day (including both BMR and active calories) and it
         * resets when 00:00 is reached (in whatever is the timezone set at that time). This can
         * result in the DAILY period being greater than or less than 24 hours when the timezone
         * of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicFloat> DAILY_CALORIES =
                new PlatformDataKey<>("Daily Calories");

        /**
         * The data source key for daily floors data from platform health sources. This is the total
         * number of floors climbed over a day and it resets when 00:00 is reached (in whatever
         * is the timezone set at that time). This can result in the DAILY period being greater
         * than or less than 24 hours when the timezone of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicFloat> DAILY_FLOORS =
                new PlatformDataKey<>("Daily Floors");
    }

    private PlatformHealthSources() {}

    /**
     * Creates a {@link DynamicFloat} which receives the current heat rate from platform sources.
     *
     * <p>This method provides backward compatibility and is preferred over using {@link
     * Keys#HEART_RATE_BPM} directly.
     */
    @RequiresPermission(Manifest.permission.BODY_SENSORS)
    @NonNull
    public static DynamicFloat heartRateBpm() {
        return new PlatformInt32Source.Builder()
                .setSourceType(PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE)
                .build()
                .asFloat();
    }

    /**
     * Creates a {@link DynamicInt32} which receives the current daily steps from platform health
     * sources. This is the total step count over a day and it resets when 00:00 is reached (in
     * whatever is the timezone set at that time). This can result in the DAILY period being
     * greater than or less than 24 hours when the timezone of the device is changed.
     *
     * <p>This method provides backward compatibility and is preferred over using {@link
     * Keys#DAILY_STEPS} directly.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicInt32 dailySteps() {
        return new PlatformInt32Source.Builder()
                .setSourceType(PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT)
                .build();
    }

    /**
     * Creates a {@link DynamicFloat} which receives the current daily floors from platform health
     * sources. This is the total number of floors climbed over a day and it resets when 00:00 is
     * reached (in whatever is the timezone set at that time). This can result in the DAILY
     * period being greater than or less than 24 hours when the timezone of the device is changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyFloors() {
        return DynamicFloat.from(Keys.DAILY_FLOORS);
    }

    /**
     * Creates a {@link DynamicFloat} which receives the current daily calories from platform health
     * sources. This is the total number of calories over a day (including both BMR and active
     * calories) and it resets when 00:00 is reached (in whatever is the timezone set at that
     * time). This can result in the DAILY period being greater than or less than 24 hours when
     * the timezone of the device is changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyCalories() {
        return DynamicFloat.from(Keys.DAILY_CALORIES);
    }

    /**
     * Creates a {@link DynamicFloat} which receives the current daily distance expressed in meters
     * from platform health sources. This is the total distance over a day and it resets when
     * 00:00 is reached (in whatever is the timezone set at that time). This can result in the
     * DAILY period being greater than or less than 24 hours when the timezone of the device is
     * changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyDistanceM() {
        return DynamicFloat.from(Keys.DAILY_DISTANCE_M);
    }
}
