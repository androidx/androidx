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

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.input.motionprediction.kalman.KalmanMotionEventPredictor;

/**
 * There is a gap between the time a user touches the screen and that information is reported to the
 * app; a motion predictor is a utility that provides predicted {@link android.view.MotionEvent}
 * based on the previously received ones. Obtain a new predictor instance using
 * {@link #newInstance(android.view.View)}; put the motion events you receive into it with
 * {@link #record(android.view.MotionEvent)}, and call {@link #predict()} to retrieve the
 * predicted  {@link android.view.MotionEvent} that would occur at the moment the next frame is
 * rendered on the display. Once no more predictions are needed, call {@link #close()} to stop it
 * and clean up resources.
 */
public interface MotionEventPredictor extends AutoCloseable {
    /**
     * Record a user's movement to the predictor. You should call this for every
     * {@link android.view.MotionEvent} that is received by the associated
     * {@link android.view.View}.
     * @param event the {@link android.view.MotionEvent} the associated view received and that
     *              needs to be recorded.
     */
    void record(@NonNull MotionEvent event);

    /**
     * Compute a prediction
     * @return the predicted {@link android.view.MotionEvent}, or null if not possible to make a
     * prediction.
     */
    @Nullable
    MotionEvent predict();

    /**
     * Notify the predictor that no more predictions are needed. Any subsequent call to
     * {@link #predict()} will return null.
     */
    @Override
    void close();

    /**
     * Create a new motion predictor associated to a specific {@link android.view.View}
     * @param view the view to associated to this predictor
     * @return the new predictor instance
     */
    static @NonNull MotionEventPredictor newInstance(@NonNull View view) {
        return new KalmanMotionEventPredictor();
    }
}
