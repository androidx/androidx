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

package androidx.input.motionprediction.system;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.MotionPredictor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.input.motionprediction.MotionEventPredictor;
import androidx.input.motionprediction.kalman.MultiPointerPredictor;
import androidx.input.motionprediction.utils.PredictionEstimator;

import java.util.concurrent.TimeUnit;

/**
 */
@RestrictTo(LIBRARY)
@RequiresApi(34)
public class SystemMotionEventPredictor implements MotionEventPredictor {
    private MultiPointerPredictor mKalmanPredictor = null;
    private final MotionPredictor mSystemPredictor;
    private final PredictionEstimator mPredictionEstimator;
    private boolean mUsingSystemPredictor = true;
    // Source is composed by flags, and -1 is not a valid value
    private int mLastRecordedSource = -1;
    // As of Android U, -2 is used internally as an invalid device id. Even if this would change
    // at some point, the source is checked first, which means that it will never be read before
    // it has been written with a valid id.
    private int mLastRecordedDeviceId = -2;

    public SystemMotionEventPredictor(@NonNull Context context) {
        mPredictionEstimator = new PredictionEstimator(context);
        mSystemPredictor = new MotionPredictor(context);
    }

    @Override
    public void record(@NonNull MotionEvent event) {
        mPredictionEstimator.record(event);
        int source = event.getSource();
        int deviceId = event.getDeviceId();
        if (mLastRecordedSource != source || mLastRecordedDeviceId != deviceId) {
            mUsingSystemPredictor = mSystemPredictor.isPredictionAvailable(deviceId, source);
            mLastRecordedDeviceId = deviceId;
            mLastRecordedSource = source;
        }
        if (mUsingSystemPredictor) {
            mSystemPredictor.record(event);
        } else {
            getKalmanPredictor().onTouchEvent(event);
        }
    }

    @Nullable
    @Override
    public MotionEvent predict() {
        final int predictionTimeDelta = mPredictionEstimator.estimate();
        if (mUsingSystemPredictor) {
            return mSystemPredictor.predict(
                TimeUnit.MILLISECONDS.toNanos(SystemClock.uptimeMillis() + predictionTimeDelta)
            );
        } else {
            return getKalmanPredictor().predict(predictionTimeDelta);
        }
    }

    private MultiPointerPredictor getKalmanPredictor() {
        if (mKalmanPredictor == null) {
            mKalmanPredictor = new MultiPointerPredictor();
        }
        return mKalmanPredictor;
    }

    /**
     * Builds a new instance of the system motion event prediction
     *
     * @param context the application context
     * @return the new instance
     */
    @NonNull
    public static SystemMotionEventPredictor newInstance(@NonNull Context context) {
        return new SystemMotionEventPredictor(context);
    }
}

