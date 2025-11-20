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

package androidx.input.motionprediction;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.input.motionprediction.common.Configuration;
import androidx.input.motionprediction.kalman.KalmanMotionEventPredictor;
import androidx.input.motionprediction.system.SystemMotionEventPredictor;

/**
 * There is a gap between the time a user touches the screen and that information is reported to the
 * app; a motion predictor is a utility that provides predicted {@link android.view.MotionEvent}
 * based on the previously received ones. Obtain a new predictor instance using
 * {@link #newInstance(android.view.View)}; put the motion events you receive into it with
 * {@link #record(android.view.MotionEvent)}, and call {@link #predict()} to retrieve the
 * predicted  {@link android.view.MotionEvent} that would occur at the moment the next frame is
 * rendered on the display.
 */
public interface MotionEventPredictor {
    /**
     * Record a user's movement to the predictor. You should call this for every
     * {@link android.view.MotionEvent} that is received by the associated
     * {@link android.view.View}.
     *
     * @param event the {@link android.view.MotionEvent} the associated view received and that
     *              needs to be recorded.
     * @throws IllegalArgumentException if an inconsistent MotionEvent stream is sent.
     */
    void record(@NonNull MotionEvent event);

    /**
     * Compute a prediction
     *
     * @return the predicted {@link android.view.MotionEvent}, or null if not possible to make a
     * prediction.
     */
    @Nullable
    MotionEvent predict();

    /**
     * Create a new motion predictor associated to a specific {@link android.view.View}.
     *
     * For devices running Android versions before U, the predicions are provided by a library based
     * on a Kalman filter; from Android U, a system API is available, but predictions may not be
     * supported for all strokes (for instance, it may be limited to stylus events). In these cases,
     * the Kalman filter library will be used; to determine if a `MotionEvent` will be handled by
     * the system prediction, use {@link android.view.MotionPredictor#isPredictionAvailable}.
     *
     * @param view the view to associated to this predictor
     * @return the new predictor instance
     */
    @NonNull
    static MotionEventPredictor newInstance(@NonNull View view) {
        Context context = view.getContext();
        Configuration configuration = Configuration.getInstance();
        if (Build.VERSION.SDK_INT >= 34
                && !configuration.preferLibraryPrediction()) {
            return SystemMotionEventPredictor.newInstance(
                    context,
                    configuration.predictionStrategy());
        } else {
            return new KalmanMotionEventPredictor(context, configuration.predictionStrategy());
        }
    }
}
