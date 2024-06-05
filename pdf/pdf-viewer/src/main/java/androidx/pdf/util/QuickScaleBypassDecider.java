/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is a workaround for when Android mishandles fast scrolling as a QuickScale gesture.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class QuickScaleBypassDecider {

    /**
     * The duration between the first tap's up event and the second tap's down event for an
     * interaction to be considered a double-tap.
     */
    private static final int DOUBLE_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout();

    private static final Set<GestureTracker.Gesture> SCROLL_GESTURES =
            new HashSet<>(
                    Arrays.asList(
                            GestureTracker.Gesture.DRAG,
                            GestureTracker.Gesture.DRAG_X,
                            GestureTracker.Gesture.DRAG_Y,
                            GestureTracker.Gesture.FLING));

    @Nullable
    private GestureTracker.Gesture mLastGesture;

    /**
     * Returns whether to skip passing {@code event} to the {@code zoomDetector}.
     *
     * <p>Android sometimes misinterprets scroll gestures performed in quick succession for a
     * QuickScale (double-tap-and-drag to zoom) gesture. This is because {@link GestureDetector}'s
     * double tap detection logic compares the position of the first {@link MotionEvent#ACTION_DOWN}
     * event to the second {@link MotionEvent#ACTION_DOWN} event, but ignores where the first
     * gesture's {@link MotionEvent#ACTION_UP} event took place. In a drag/fling gesture, the Up
     * event happens far from the Down event, but if a second drag/fling has it's Down event near
     * the previous gesture's Down event (and occurs within {@link #DOUBLE_TAP_TIMEOUT_MS} of the
     * previous Up event), Android will detect a double tap, and if we pass this event to the
     * {@code zoomDetector}, it begins a QuickScale.
     *
     * <p>So we work around Android's poor double tap detection by capturing this case, (i.e. a
     * {@link MotionEvent#ACTION_DOWN} occurring within {@link #DOUBLE_TAP_TIMEOUT_MS} of a
     * drag/fling event) and skipping the {@code zoomDetector} in this case.
     */
    public boolean shouldSkipZoomDetector(@NonNull MotionEvent event,
            @NonNull GestureTracker.EventId lastEvent) {
        if (lastEvent == null || lastEvent.getEventAction() != MotionEvent.ACTION_UP) {
            return false;
        }
        if (!SCROLL_GESTURES.contains(mLastGesture)) {
            return false;
        }
        long deltaTime =
                lastEvent == null ? Integer.MAX_VALUE
                        : event.getEventTime() - lastEvent.getEventTimeMs();

        return deltaTime < DOUBLE_TAP_TIMEOUT_MS;
    }

    public void setLastGesture(@NonNull GestureTracker.Gesture gesture) {
        mLastGesture = gesture;
    }
}
