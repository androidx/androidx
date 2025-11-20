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

package androidx.input.motionprediction.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 */
@RestrictTo(LIBRARY)
public class Configuration {
    public static final int STRATEGY_BALANCED = 0;
    public static final int STRATEGY_SAFE = 1;
    public static final int STRATEGY_AGGRESSIVE = 2;

    private static volatile Configuration sInstance = null;
    private static final Object sLock = new Object();

    private final boolean mPredictLift;
    private final boolean mPreferLibraryPrediction;
    private final int mPredictionOffset;
    private final int mPredictionStrategy;

    /**
     * Returns the configuration for prediction in this system.
     *
     * @return the prediction configuration
     */
    public static @NonNull Configuration getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new Configuration();
                }
            }
        }
        return sInstance;
    }

    private Configuration() {
        // This class is non-instantiable externally.
        mPreferLibraryPrediction = SystemProperty
                .getBoolean("debug.input.androidx_prefer_library_prediction");
        mPredictionOffset = SystemProperty.getInt("debug.input.androidx_prediction_offset");
        mPredictLift = SystemProperty.getBoolean("debug.input.androidx_predict_lift");
        mPredictionStrategy = SystemProperty.getInt("debug.input.androidx_prediction_strategy");
    }

    /**
     * Returns whether or not the library should prefer its own prediction even when system
     * prediction is available
     *
     * @return true if the library prediction should always be used
     */
    public boolean preferLibraryPrediction() {
        return mPreferLibraryPrediction;
    }

    /**
     * Returns the number of milliseconds to add to the computed prediction target
     *
     * @return number of additional milliseconds to predict
     */
    public int predictionOffset() {
        return mPredictionOffset;
    }

    /**
     * Returns whether or not the pressure should be used to adjust the distance of the prediction
     *
     * @return true if the pressure should be used to determine the prediction length
     */
    public boolean predictLift() {
        return mPredictLift;
    }

    /**
     * Returns the default prediction strategy
     *
     * @return the strategy to use as default; 0 is balanced
     */
    public int predictionStrategy() {
        return mPredictionStrategy;
    }
}
