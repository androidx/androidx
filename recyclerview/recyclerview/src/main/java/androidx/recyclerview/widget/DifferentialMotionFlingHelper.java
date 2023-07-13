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

package androidx.recyclerview.widget;

import android.view.MotionEvent;

/**
 * Helper for controlling differential motion flings.
 *
 * <p><b>Differential motion</b> here refers to motions that report change in position instead of
 * absolution position. For instance, differential data points of 2, -1, 5 represent: there was
 * a movement by "2" units, then by "-1" units, then by "5" units. Examples of motions reported
 * differentially include motions from {@link MotionEvent#AXIS_SCROLL}.
 *
 * <p>The client should call {@link #onMotionEvent} when a differential motion event happens on
 * the target View (that is, the View on which we want to fling), and this class processes the event
 * to orchestrate fling.
 *
 * <p>Note that this helper class currently works to control fling only in one direction at a time.
 * As such, it works independently of horizontal/vertical orientations. It requests its client to
 * start/stop fling, and it's up to the client to choose the fling direction based on its specific
 * internal configurations and/or preferences.
 */
class DifferentialMotionFlingHelper {
    // Suppress "unused". This will be used once the fling algo is implemented.
    @SuppressWarnings({"unused"})
    private final DifferentialMotionFlingTarget mTarget;

    /**
     * Represents an entity that may be flinged by a differential motion or an entity that initiates
     * fling on a target View.
     */
    interface DifferentialMotionFlingTarget {
        /**
         * Start flinging on the target View by a given velocity.
         *
         * @param velocity the fling velocity, in pixels/second.
         * @return {@code true} if fling was successfully initiated, {@code false} otherwise.
         */
        boolean startDifferentialMotionFling(int velocity);

        /** Stop any ongoing fling on the target View that is caused by a differential motion. */
        void stopDifferentialMotionFling();
    }

    /** Constructs an instance for a given {@link DifferentialMotionFlingTarget}. */
    DifferentialMotionFlingHelper(DifferentialMotionFlingTarget target) {
        mTarget = target;
    }

    /**
     * Called to report when a differential motion happens on the View that's the target for fling.
     *
     * @param event the {@link MotionEvent} being reported.
     * @param axis the axis being processed by the target View.
     */
    void onMotionEvent(MotionEvent event, int axis) {
        // TODO(b/290680625): implement logic to control differential motion fling.
    }
}
