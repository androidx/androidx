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

package androidx.core.view;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Represents an entity that may be flung by a differential motion or an entity that initiates
 * fling on a target View.
 */
public interface DifferentialMotionFlingTarget {
    /**
     * Start flinging on the target View by a given velocity.
     *
     * @param velocity the fling velocity, in pixels/second.
     * @return {@code true} if fling was successfully initiated, {@code false} otherwise.
     */
    boolean startDifferentialMotionFling(float velocity);

    /** Stop any ongoing fling on the target View that is caused by a differential motion. */
    void stopDifferentialMotionFling();

    /**
     * Returns the scaled scroll factor to be used for differential motions. This is the
     * value that the raw {@link MotionEvent} values should be multiplied with to get pixels.
     *
     * <p>This usually is one of the values provided by {@link ViewConfigurationCompat}. It is
     * up to the client to choose and provide any value as per its internal configuration.
     *
     * @see ViewConfigurationCompat#getScaledHorizontalScrollFactor(ViewConfiguration, Context)
     * @see ViewConfigurationCompat#getScaledVerticalScrollFactor(ViewConfiguration, Context)
     */
    float getScaledScrollFactor();
}
