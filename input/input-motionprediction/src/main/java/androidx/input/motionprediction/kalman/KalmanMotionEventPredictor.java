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

import android.content.Context;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.input.motionprediction.MotionEventPredictor;
import androidx.input.motionprediction.utils.PredictionEstimator;

/**
 */
@RestrictTo(LIBRARY)
public class KalmanMotionEventPredictor implements MotionEventPredictor {
    private final MultiPointerPredictor mMultiPointerPredictor = new MultiPointerPredictor();
    private final PredictionEstimator mPredictionEstimator;

    public KalmanMotionEventPredictor(@NonNull Context context) {
        mPredictionEstimator = new PredictionEstimator(context);
    }

    @Override
    public void record(@NonNull MotionEvent event) {
        mPredictionEstimator.record(event);
        mMultiPointerPredictor.onTouchEvent(event);
    }

    @Nullable
    @Override
    public MotionEvent predict() {
        final int predictionTimeDelta = mPredictionEstimator.estimate();
        return mMultiPointerPredictor.predict(predictionTimeDelta);
    }
}
