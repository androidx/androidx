/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.view.VelocityTracker;

/**
 * Helper for accessing features in {@link VelocityTracker}.
 *
 * @deprecated Use {@link VelocityTracker} directly.
 */
@Deprecated
public final class VelocityTrackerCompat {
    /**
     * Call {@link VelocityTracker#getXVelocity(int)}.
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB} device,
     * returns {@link VelocityTracker#getXVelocity()}.
     *
     * @deprecated Use {@link VelocityTracker#getXVelocity(int)} directly.
     */
    @Deprecated
    public static float getXVelocity(VelocityTracker tracker, int pointerId) {
        return tracker.getXVelocity(pointerId);
    }

    /**
     * Call {@link VelocityTracker#getYVelocity(int)}.
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB} device,
     * returns {@link VelocityTracker#getYVelocity()}.
     *
     * @deprecated Use {@link VelocityTracker#getYVelocity(int)} directly.
     */
    @Deprecated
    public static float getYVelocity(VelocityTracker tracker, int pointerId) {
        return tracker.getYVelocity(pointerId);
    }

    private VelocityTrackerCompat() {}
}
