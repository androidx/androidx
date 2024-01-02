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
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

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
     * Mapping of platform velocity trackers to their respective fallback.
     *
     * <p>This mapping is used to provide a consistent add/clear/getVelocity experience for axes
     * that may not be supported at a given Android version. Clients can continue to call the
     * compat's add/clear/compute/getVelocity with the platform tracker instances, and this class
     * will assign a "fallback" tracker instance for each unique platform tracker instance to
     * consistently run these operations just as they would run on the platorm instances.
     *
     * <p>Since the compat APIs have been provided statically, we will use a singleton compat
     * instance to manage the mappings whenever we need a "fallback" handling for velocity.
     *
     * <p>High level flow for a compat velocity logic for a platform-unsupported axis "A" looks
     * as follows:
     *     [1]. add(platformTracker, event):
     *         [a] Create fallback tracker, and associate it with "platformTracker`.
     *         [b] Add `event` to the fallback tracker.
     *     [2]. computeCurrentVelocity(platformTracker, event):
     *         [a] If there is no associated fallback tracker for `platformTracker`, exit.
     *         [b] If there's a fallback, compute current velocity for the fallback.
     *     [3]. getAxisVelocity(platformTracker, axis):
     *         [a] If there is no associated fallback tracker for `platformTracker`, exit.
     *         [b] If there's a fallback, return the velocity from the fallback.
     *     [4]. clear/recycle(platformTracker)
     *         [a] Remove any association between `platformTracker` and a fallback tracker.
     *
     */
    private static Map<VelocityTracker, VelocityTrackerFallback> sFallbackTrackers =
            Collections.synchronizedMap(new WeakHashMap<>());

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
        return axis == MotionEvent.AXIS_SCROLL // Supported via VelocityTrackerFallback.
                || axis == MotionEvent.AXIS_X // Supported by platform at all API levels.
                || axis == MotionEvent.AXIS_Y; // Supported by platform at all API levels.
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

        // For X and Y axes, use the `get*Velocity` APIs that existed at all API levels.
        if (axis == MotionEvent.AXIS_X) {
            return tracker.getXVelocity();
        }
        if (axis == MotionEvent.AXIS_Y) {
            return tracker.getYVelocity();
        }

        // For any other axis before API 34, use the corresponding VelocityTrackerFallback, if any,
        // to determine the velocity.
        VelocityTrackerFallback fallback = getFallbackTrackerOrNull(tracker);
        if (fallback != null) {
            return fallback.getAxisVelocity(axis);
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
     *   <li> {@link MotionEvent#AXIS_SCROLL}: supported via the platform starting
     *        {@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}. Supported via a fallback logic at all
     *        platform levels for the active pointer only.
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

    /** Reset the velocity tracker back to its initial state. */
    public static void clear(@NonNull VelocityTracker tracker) {
        tracker.clear();
        removeFallbackForTracker(tracker);
    }

    /**
     * Return a {@link VelocityTracker} object back to be re-used by others.
     *
     * <p>Call this method for your {@link VelocityTracker} when you have finished tracking
     * velocity for the use-case you created this tracker for and decided that you no longer need
     * it. This allows it to be returned back to the pool of trackers to be re-used by others.
     *
     * <p>You must <b>not</b> touch the object after calling this function. That is, don't call any
     * methods on it, or pass it as an input to any of this class' compat APIs, as the instance
     * is no longer valid for velocity tracking.
     *
     * @see VelocityTracker#recycle()
     */
    public static void recycle(@NonNull VelocityTracker tracker) {
        tracker.recycle();
        removeFallbackForTracker(tracker);
    }

    /**
     * Compute the current velocity based on the points that have been
     * collected. Only call this when you actually want to retrieve velocity
     * information, as it is relatively expensive.  You can then retrieve
     * the velocity with {@link #getAxisVelocity(VelocityTracker, int)} ()}.
     *
     * @param tracker The {@link VelocityTracker} for which to compute velocity.
     * @param units The units you would like the velocity in.  A value of 1
     * provides units per millisecond, 1000 provides units per second, etc.
     * Note that the units referred to here are the same units with which motion is reported. For
     * axes X and Y, the units are pixels.
     * @param maxVelocity The maximum velocity that can be computed by this method.
     * This value must be declared in the same unit as the units parameter. This value
     * must be positive.
     */
    public static void computeCurrentVelocity(
            @NonNull VelocityTracker tracker, int units, float maxVelocity) {
        tracker.computeCurrentVelocity(units, maxVelocity);
        VelocityTrackerFallback fallback = getFallbackTrackerOrNull(tracker);
        if (fallback != null) {
            fallback.computeCurrentVelocity(units, maxVelocity);
        }
    }

    /**
     * Equivalent to invoking {@link #computeCurrentVelocity(VelocityTracker, int, float)} with a
     * maximum velocity of Float.MAX_VALUE.
     */
    public static void computeCurrentVelocity(@NonNull VelocityTracker tracker, int units) {
        VelocityTrackerCompat.computeCurrentVelocity(tracker, units, Float.MAX_VALUE);
    }

    /**
     * Add a user's movement to the tracker.
     *
     * <p>For pointer events, you should call this for the initial
     * {@link MotionEvent#ACTION_DOWN}, the following
     * {@link MotionEvent#ACTION_MOVE} events that you receive, and the final
     * {@link MotionEvent#ACTION_UP}.  You can, however, call this
     * for whichever events you desire.
     *
     * @param tracker The {@link VelocityTracker} to add the movement to.
     * @param event The MotionEvent you received and would like to track.
     */
    public static void addMovement(@NonNull VelocityTracker tracker, @NonNull MotionEvent event) {
        tracker.addMovement(event);
        if (Build.VERSION.SDK_INT >= 34) {
            // For API levels 34 and above, we currently do not support any compat logic.
            return;
        }

        if (event.getSource() == InputDevice.SOURCE_ROTARY_ENCODER) {
            // We support compat logic for AXIS_SCROLL.
            // Initialize the compat instance if needed.
            if (!sFallbackTrackers.containsKey(tracker)) {
                sFallbackTrackers.put(tracker, new VelocityTrackerFallback());
            }
            sFallbackTrackers.get(tracker).addMovement(event);
        }
    }

    private static void removeFallbackForTracker(VelocityTracker tracker) {
        sFallbackTrackers.remove(tracker);
    }

    @Nullable
    private static VelocityTrackerFallback getFallbackTrackerOrNull(VelocityTracker tracker) {
        return sFallbackTrackers.get(tracker);
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
