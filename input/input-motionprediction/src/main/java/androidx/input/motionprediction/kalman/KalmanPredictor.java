/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.input.motionprediction.kalman;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Simple interface for predicting motion points.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public interface KalmanPredictor {

    /** Gets the current prediction target */
    int getPredictionTarget();

    /** Sets the current prediction target */
    void setPredictionTarget(int predictionTargetMillis);

    /** Sets the report rate */
    void setReportRate(int reportRateMs);

    /** Reports the motion events */
    boolean onTouchEvent(@NonNull MotionEvent event);

    /** @return null if not possible to make a prediction. */
    @Nullable
    MotionEvent predict();
}
