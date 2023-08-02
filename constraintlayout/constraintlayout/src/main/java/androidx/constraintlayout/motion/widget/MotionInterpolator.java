/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import android.view.animation.Interpolator;

/**
 * Defines an interpolator that can return velocity
 */
public abstract class MotionInterpolator implements Interpolator {

    /**
     * Gets the interpolated given the original interpolation
     * @param v
     * @return
     */
    @Override
    public abstract float getInterpolation(float v);

    /**
     * Gets the velocity at the last interpolated point
     * @return
     */
    public abstract float getVelocity();
}
