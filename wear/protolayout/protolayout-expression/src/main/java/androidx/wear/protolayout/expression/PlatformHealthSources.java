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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.PlatformInt32Source;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Dynamic types for platform health sources. */
public class PlatformHealthSources {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        HEART_RATE_ACCURACY_UNKNOWN,
        HEART_RATE_ACCURACY_NO_CONTACT,
        HEART_RATE_ACCURACY_UNRELIABLE,
        HEART_RATE_ACCURACY_LOW,
        HEART_RATE_ACCURACY_MEDIUM,
        HEART_RATE_ACCURACY_HIGH,
    })
    public @interface HeartRateAccuracy {}

    /** Heart rate accuracy is unknown. */
    public static final int HEART_RATE_ACCURACY_UNKNOWN = 0;
    /** Heart rate cannot be acquired because the sensor is not properly contacting skin. */
    public static final int HEART_RATE_ACCURACY_NO_CONTACT = 1;
    /** Heart rate data is currently too unreliable to be used. */
    public static final int HEART_RATE_ACCURACY_UNRELIABLE = 2;
    /** Heart rate data is available but the accuracy is low. */
    public static final int HEART_RATE_ACCURACY_LOW = 3;
    /** Heart rate data is available and the accuracy is medium. */
    public static final int HEART_RATE_ACCURACY_MEDIUM = 4;
    /** Heart rate data is available with high accuracy. */
    public static final int HEART_RATE_ACCURACY_HIGH = 5;

    /** Data sources keys for platform health sources. */
    public static class Keys {
        private Keys() {}

        /** The data source key for heart rate bpm data from platform health sources. */
        @NonNull
        @RequiresPermission(Manifest.permission.BODY_SENSORS)
        public static final PlatformDataKey<DynamicFloat> HEART_RATE_BPM =
                new PlatformDataKey<>("HeartRate");

        /**
         * The data source key for heart rate sensor accuracy data from platform health sources. The
         * accuracy value is one of {@code HEART_RATE_ACCURACY_*} constants.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.BODY_SENSORS)
        public static final PlatformDataKey<DynamicHeartRateAccuracy> HEART_RATE_ACCURACY =
                new PlatformDataKey<>("HeartRate Accuracy");
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
         * This is the total distance over a day and it resets when 00:00 is reached (in whatever is
         * the timezone set at that time). This can result in the DAILY period being greater than or
         * less than 24 hours when the timezone of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicFloat> DAILY_DISTANCE_METERS =
                new PlatformDataKey<>("Daily Distance");

        /**
         * The data source key for daily calories (kcal) data from platform health sources. This is
         * the total number of kilocalories over a day (including both BMR and active calories)
         * and it resets when 00:00 is reached (in whatever is the timezone set at that time).
         * This can result in the DAILY period being greater than or less than 24 hours when the
         * timezone of the device is changed.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        public static final PlatformDataKey<DynamicFloat> DAILY_CALORIES =
                new PlatformDataKey<>("Daily Calories");

        /**
         * The data source key for daily floors data from platform health sources. This is the total
         * number of floors climbed over a day and it resets when 00:00 is reached (in whatever is
         * the timezone set at that time). This can result in the DAILY period being greater than or
         * less than 24 hours when the timezone of the device is changed.
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
     * Creates a {@link DynamicHeartRateAccuracy} which receives the current heat rate sensor
     * accuracy from platform sources.
     *
     * <p>The accuracy value is one of {@link DynamicHeartRateAccuracy} constants.
     */
    @RequiresPermission(Manifest.permission.BODY_SENSORS)
    @NonNull
    public static DynamicHeartRateAccuracy heartRateAccuracy() {
        return new DynamicHeartRateAccuracy(
                new DynamicBuilders.StateInt32Source.Builder()
                        .setSourceKey(Keys.HEART_RATE_ACCURACY.getKey())
                        .setSourceNamespace(Keys.HEART_RATE_ACCURACY.getNamespace())
                        .build());
    }

    /**
     * Creates a {@link DynamicInt32} which receives the current daily steps from platform health
     * sources. This is the total step count over a day and it resets when 00:00 is reached (in
     * whatever is the timezone set at that time). This can result in the DAILY period being greater
     * than or less than 24 hours when the timezone of the device is changed.
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
     * reached (in whatever is the timezone set at that time). This can result in the DAILY period
     * being greater than or less than 24 hours when the timezone of the device is changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyFloors() {
        return DynamicFloat.from(Keys.DAILY_FLOORS);
    }

    /**
     * Creates a {@link DynamicFloat} which receives the current daily calories from platform health
     * sources. This is the total number of calories over a day (including both BMR and active
     * calories) and it resets when 00:00 is reached (in whatever is the timezone set at that time).
     * This can result in the DAILY period being greater than or less than 24 hours when the
     * timezone of the device is changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyCalories() {
        return DynamicFloat.from(Keys.DAILY_CALORIES);
    }

    /**
     * Creates a {@link DynamicFloat} which receives the current daily distance expressed in meters
     * from platform health sources. This is the total distance over a day and it resets when 00:00
     * is reached (in whatever is the timezone set at that time). This can result in the DAILY
     * period being greater than or less than 24 hours when the timezone of the device is changed.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @NonNull
    public static DynamicFloat dailyDistanceMeters() {
        return DynamicFloat.from(Keys.DAILY_DISTANCE_METERS);
    }

    /** Dynamic heart rate sensor accuracy value. */
    public static final class DynamicHeartRateAccuracy implements DynamicInt32 {
        private final DynamicInt32 mImpl;

        DynamicHeartRateAccuracy(DynamicInt32 impl) {
            this.mImpl = impl;
        }

        /** Creates a constant-valued {@link DynamicHeartRateAccuracy}. */
        @NonNull
        public static DynamicHeartRateAccuracy constant(@HeartRateAccuracy int val) {
            return new DynamicHeartRateAccuracy(DynamicInt32.constant(val));
        }

        /** Creates a value to be provided from a {@code PlatformDataProvider}. */
        @NonNull
        @SuppressWarnings("unchecked") // DynamicHeartRateAccuracy acts like DynamicInt32.
        public static DynamicDataValue<DynamicHeartRateAccuracy> dynamicDataValueOf(
                @HeartRateAccuracy int val) {
            return (DynamicDataValue<DynamicHeartRateAccuracy>)
                    (DynamicDataValue<?>) DynamicDataValue.fromInt(val);
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return mImpl.toDynamicInt32Proto();
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Nullable
        @Override
        public Fingerprint getFingerprint() {
            return mImpl.getFingerprint();
        }
    }
}
