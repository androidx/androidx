/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Helper for accessing features in {@link VelocityTracker}. */
public final class VelocityTrackerCompat {
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(SOURCE)
    @IntDef(value = {
            MotionEvent.AXIS_X,
            MotionEvent.AXIS_Y,
            MotionEvent.AXIS_SCROLL
    })
    public @interface VelocityTrackableMotionEventAxis {}
    /**
     * Call {@link VelocityTracker#getXVelocity(int)}.
     * If running on a pre-{@link Build.VERSION_CODES#HONEYCOMB} device,
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
     * If running on a pre-{@link Build.VERSION_CODES#HONEYCOMB} device,
     * returns {@link VelocityTracker#getYVelocity()}.
     *
     * @deprecated Use {@link VelocityTracker#getYVelocity(int)} directly.
     */
    @Deprecated
    public static float getYVelocity(VelocityTracker tracker, int pointerId) {
        return tracker.getYVelocity(pointerId);
    }

    /**
     * Checks whether a given velocity-trackable {@link MotionEvent} axis is supported for velocity
     * tracking by this {@link VelocityTracker} instance (refer to
     * {@link #getAxisVelocity(VelocityTracker, int, int)} for a list of potentially
     * velocity-trackable axes).
     *
     * <p>Note that the value returned from this method will stay the same for a given instance, so
     * a single check for axis support is enough per a {@link VelocityTracker} instance.
     *
     * @param tracker The {@link VelocityTracker} for which to check axis support.
     * @param axis The axis to check for velocity support.
     * @return {@code true} if {@code axis} is supported for velocity tracking, or {@code false}
     *         otherwise.
     * @see #getAxisVelocity(VelocityTracker, int, int)
     * @see #getAxisVelocity(VelocityTracker, int)
     */
    public static boolean isAxisSupported(@NonNull VelocityTracker tracker,
            @VelocityTrackableMotionEventAxis int axis) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.isAxisSupported(tracker, axis);
        }
        return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y;
    }

    /**
     * Equivalent to calling {@link #getAxisVelocity(VelocityTracker, int, int)} for {@code axis}
     * and the active pointer.
     *
     * @param tracker The {@link VelocityTracker} from which to get axis velocity.
     * @param axis Which axis' velocity to return.
     * @return The previously computed velocity for {@code axis} for the active pointer if
     *         {@code axis} is supported for velocity tracking, or 0 if velocity tracking is not
     *         supported for the axis.
     * @see #isAxisSupported(VelocityTracker, int)
     * @see #getAxisVelocity(VelocityTracker, int, int)
     */
    public static float getAxisVelocity(@NonNull VelocityTracker tracker,
            @VelocityTrackableMotionEventAxis int axis) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.getAxisVelocity(tracker, axis);
        }
        if (axis == MotionEvent.AXIS_X) {
            return tracker.getXVelocity();
        }
        if (axis == MotionEvent.AXIS_Y) {
            return tracker.getYVelocity();
        }
        return  0;
    }

    /**
     * Retrieve the last computed velocity for a given motion axis. You must first call
     * {@link VelocityTracker#computeCurrentVelocity(int)} or
     * {@link VelocityTracker#computeCurrentVelocity(int, float)} before calling this function.
     *
     * <p>In addition to {@link MotionEvent#AXIS_X} and {@link MotionEvent#AXIS_Y} which have been
     * supported since the introduction of this class, the following axes can be candidates for this
     * method:
     * <ul>
     *   <li> {@link MotionEvent#AXIS_SCROLL}: supported starting
     *        {@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}
     * </ul>
     *
     * <p>Before accessing velocities of an axis using this method, check that your
     * {@link VelocityTracker} instance supports the axis by using
     * {@link #isAxisSupported(VelocityTracker, int)}.
     *
     * @param tracker The {@link VelocityTracker} from which to get axis velocity.
     * @param axis Which axis' velocity to return.
     * @param pointerId Which pointer's velocity to return.
     * @return The previously computed velocity for {@code axis} for pointer ID of {@code id} if
     *         {@code axis} is supported for velocity tracking, or 0 if velocity tracking is not
     *         supported for the axis.
     * @see #isAxisSupported(VelocityTracker, int)
     */
    public static float getAxisVelocity(
            @NonNull VelocityTracker tracker,
            @VelocityTrackableMotionEventAxis int axis,
            int pointerId) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.getAxisVelocity(tracker, axis, pointerId);
        }
        if (axis == MotionEvent.AXIS_X) {
            return tracker.getXVelocity(pointerId);
        }
        if (axis == MotionEvent.AXIS_Y) {
            return tracker.getYVelocity(pointerId);
        }
        return  0;

    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isAxisSupported(VelocityTracker velocityTracker, int axis) {
            return velocityTracker.isAxisSupported(axis);
        }

        @DoNotInline
        static float getAxisVelocity(VelocityTracker velocityTracker, int axis, int id) {
            return velocityTracker.getAxisVelocity(axis, id);
        }

        @DoNotInline
        static float getAxisVelocity(VelocityTracker velocityTracker, int axis) {
            return velocityTracker.getAxisVelocity(axis);
        }
    }

    private VelocityTrackerCompat() {}
}
