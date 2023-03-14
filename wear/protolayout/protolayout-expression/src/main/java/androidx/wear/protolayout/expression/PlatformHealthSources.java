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
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.PlatformInt32Source;

/** Utility class provides utils to access health data. */
@ProtoLayoutExperimental
public class PlatformHealthSources {
    private PlatformHealthSources() {
    }

    /** Creates a {@link DynamicInt32} which receives the current heat rate from the sensor. */
    @RequiresPermission(Manifest.permission.BODY_SENSORS)
    @ProtoLayoutExperimental
    @NonNull
    public static DynamicInt32 heartRateBpm() {
        return new PlatformInt32Source.Builder()
                .setSourceType(PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE)
                .build();
    }

    /**
     * Creates a {@link DynamicInt32} which receives the current daily steps from the sensor.
     * This is the total step count over a day, where the previous day ends and a new day begins at
     * 12:00 AM local time.
     */
    @RequiresApi(VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @ProtoLayoutExperimental
    @NonNull
    public static DynamicInt32 dailySteps() {
        return new PlatformInt32Source.Builder()
                .setSourceType(PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT)
                .build();
    }
}
