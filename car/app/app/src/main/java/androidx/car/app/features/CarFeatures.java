/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.car.app.features;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

/**
 * Class for checking for whether car related features are available on the system
 */
public final class CarFeatures {

    /**
     * Flag value to check whether or not audio is allowed to play while driving
     */
    public static final String FEATURE_BACKGROUND_AUDIO_WHILE_DRIVING =
            "background_audio_while_driving";

    // Do not instantiate
    private CarFeatures() {}

    // Create an open api surface to support future system features that may need to be
    // backported without needing to create a new api
    /**
     *  @return  whether the system enables a given feature
     */
    public static boolean isFeatureEnabled(@NonNull Context context, @NonNull String feature) {
        PackageManager packageManager = context.getPackageManager();

        return packageManager.hasSystemFeature(feature);
    }
}
