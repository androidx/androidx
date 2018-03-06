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

import android.view.MotionEvent;

/**
 * Helper for accessing features in {@link MotionEvent}.
 */
public final class MotionEventCompat {
    /**
     * Synonym for {@link MotionEvent#ACTION_MASK}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_MASK} directly.
     */
    @Deprecated
    public static final int ACTION_MASK = 0xff;

    /**
     * Synonym for {@link MotionEvent#ACTION_POINTER_DOWN}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_POINTER_DOWN} directly.
     */
    @Deprecated
    public static final int ACTION_POINTER_DOWN = 5;

    /**
     * Synonym for {@link MotionEvent#ACTION_POINTER_UP}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_POINTER_UP} directly.
     */
    @Deprecated
    public static final int ACTION_POINTER_UP = 6;

    /**
     * Synonym for {@link MotionEvent#ACTION_HOVER_MOVE}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_HOVER_MOVE} directly.
     */
    @Deprecated
    public static final int ACTION_HOVER_MOVE = 7;

    /**
     * Synonym for {@link MotionEvent#ACTION_SCROLL}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_SCROLL} directly.
     */
    @Deprecated
    public static final int ACTION_SCROLL = 8;

    /**
     * Synonym for {@link MotionEvent#ACTION_POINTER_INDEX_MASK}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_POINTER_INDEX_MASK} directly.
     */
    @Deprecated
    public static final int ACTION_POINTER_INDEX_MASK  = 0xff00;

    /**
     * Synonym for {@link MotionEvent#ACTION_POINTER_INDEX_SHIFT}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_POINTER_INDEX_SHIFT} directly.
     */
    @Deprecated
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    /**
     * Synonym for {@link MotionEvent#ACTION_HOVER_ENTER}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_HOVER_ENTER} directly.
     */
    @Deprecated
    public static final int ACTION_HOVER_ENTER = 9;

    /**
     * Synonym for {@link MotionEvent#ACTION_HOVER_EXIT}.
     *
     * @deprecated Use {@link MotionEvent#ACTION_HOVER_EXIT} directly.
     */
    @Deprecated
    public static final int ACTION_HOVER_EXIT = 10;

    /**
     * Synonym for {@link MotionEvent#AXIS_X}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_X} directly.
     */
    @Deprecated
    public static final int AXIS_X = 0;

    /**
     * Synonym for {@link MotionEvent#AXIS_Y}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_Y} directly.
     */
    @Deprecated
    public static final int AXIS_Y = 1;

    /**
     * Synonym for {@link MotionEvent#AXIS_PRESSURE}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_PRESSURE} directly.
     */
    @Deprecated
    public static final int AXIS_PRESSURE = 2;

    /**
     * Synonym for {@link MotionEvent#AXIS_SIZE}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_SIZE} directly.
     */
    @Deprecated
    public static final int AXIS_SIZE = 3;

    /**
     * Synonym for {@link MotionEvent#AXIS_TOUCH_MAJOR}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_TOUCH_MAJOR} directly.
     */
    @Deprecated
    public static final int AXIS_TOUCH_MAJOR = 4;

    /**
     * Synonym for {@link MotionEvent#AXIS_TOUCH_MINOR}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_TOUCH_MINOR} directly.
     */
    @Deprecated
    public static final int AXIS_TOUCH_MINOR = 5;

    /**
     * Synonym for {@link MotionEvent#AXIS_TOOL_MAJOR}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_TOOL_MAJOR} directly.
     */
    @Deprecated
    public static final int AXIS_TOOL_MAJOR = 6;

    /**
     * Synonym for {@link MotionEvent#AXIS_TOOL_MINOR}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_TOOL_MINOR} directly.
     */
    @Deprecated
    public static final int AXIS_TOOL_MINOR = 7;

    /**
     * Synonym for {@link MotionEvent#AXIS_ORIENTATION}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_ORIENTATION} directly.
     */
    @Deprecated
    public static final int AXIS_ORIENTATION = 8;

    /**
     * Synonym for {@link MotionEvent#AXIS_VSCROLL}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_VSCROLL} directly.
     */
    @Deprecated
    public static final int AXIS_VSCROLL = 9;

    /**
     * Synonym for {@link MotionEvent#AXIS_HSCROLL}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_HSCROLL} directly.
     */
    @Deprecated
    public static final int AXIS_HSCROLL = 10;

    /**
     * Synonym for {@link MotionEvent#AXIS_Z}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_Z} directly.
     */
    @Deprecated
    public static final int AXIS_Z = 11;

    /**
     * Synonym for {@link MotionEvent#AXIS_RX}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_RX} directly.
     */
    @Deprecated
    public static final int AXIS_RX = 12;

    /**
     * Synonym for {@link MotionEvent#AXIS_RY}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_RY} directly.
     */
    @Deprecated
    public static final int AXIS_RY = 13;

    /**
     * Synonym for {@link MotionEvent#AXIS_RZ}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_RZ} directly.
     */
    @Deprecated
    public static final int AXIS_RZ = 14;

    /**
     * Synonym for {@link MotionEvent#AXIS_HAT_X}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_HAT_X} directly.
     */
    @Deprecated
    public static final int AXIS_HAT_X = 15;

    /**
     * Synonym for {@link MotionEvent#AXIS_HAT_Y}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_HAT_Y} directly.
     */
    @Deprecated
    public static final int AXIS_HAT_Y = 16;

    /**
     * Synonym for {@link MotionEvent#AXIS_LTRIGGER}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_LTRIGGER} directly.
     */
    @Deprecated
    public static final int AXIS_LTRIGGER = 17;

    /**
     * Synonym for {@link MotionEvent#AXIS_RTRIGGER}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_RTRIGGER} directly.
     */
    @Deprecated
    public static final int AXIS_RTRIGGER = 18;

    /**
     * Synonym for {@link MotionEvent#AXIS_THROTTLE}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_THROTTLE} directly.
     */
    @Deprecated
    public static final int AXIS_THROTTLE = 19;

    /**
     * Synonym for {@link MotionEvent#AXIS_RUDDER}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_RUDDER} directly.
     */
    @Deprecated
    public static final int AXIS_RUDDER = 20;

    /**
     * Synonym for {@link MotionEvent#AXIS_WHEEL}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_WHEEL} directly.
     */
    @Deprecated
    public static final int AXIS_WHEEL = 21;

    /**
     * Synonym for {@link MotionEvent#AXIS_GAS}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GAS} directly.
     */
    @Deprecated
    public static final int AXIS_GAS = 22;

    /**
     * Synonym for {@link MotionEvent#AXIS_BRAKE}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_BRAKE} directly.
     */
    @Deprecated
    public static final int AXIS_BRAKE = 23;

    /**
     * Synonym for {@link MotionEvent#AXIS_DISTANCE}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_DISTANCE} directly.
     */
    @Deprecated
    public static final int AXIS_DISTANCE = 24;

    /**
     * Synonym for {@link MotionEvent#AXIS_TILT}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_TILT} directly.
     */
    @Deprecated
    public static final int AXIS_TILT = 25;

    /**
     * Synonym for {@link MotionEvent#AXIS_SCROLL}.
     */
    public static final int AXIS_SCROLL = 26;

    /**
     * Synonym for {@link MotionEvent#AXIS_RELATIVE_X}.
     */
    public static final int AXIS_RELATIVE_X = 27;

    /**
     * Synonym for {@link MotionEvent#AXIS_RELATIVE_Y}.
     */
    public static final int AXIS_RELATIVE_Y = 28;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_1}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_1} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_1 = 32;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_2}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_2} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_2 = 33;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_3}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_3} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_3 = 34;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_4}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_4} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_4 = 35;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_5}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_5} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_5 = 36;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_6}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_6} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_6 = 37;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_7}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_7} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_7 = 38;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_8}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_8} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_8 = 39;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_9}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_9} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_9 = 40;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_10}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_10} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_10 = 41;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_11}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_11} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_11 = 42;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_12}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_12} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_12 = 43;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_13}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_13} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_13 = 44;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_14}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_14} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_14 = 45;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_15}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_15} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_15 = 46;

    /**
     * Synonym for {@link MotionEvent#AXIS_GENERIC_16}.
     *
     * @deprecated Use {@link MotionEvent#AXIS_GENERIC_16} directly.
     */
    @Deprecated
    public static final int AXIS_GENERIC_16 = 47;

    /**
     * Synonym for {@link MotionEvent#BUTTON_PRIMARY}.
     *
     * @deprecated Use {@link MotionEvent#BUTTON_PRIMARY} directly.
     */
    @Deprecated
    public static final int BUTTON_PRIMARY = 1;

    /**
     * Call {@link MotionEvent#getAction}, returning only the {@link #ACTION_MASK}
     * portion.
     *
     * @deprecated Call {@link MotionEvent#getAction()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getActionMasked(MotionEvent event) {
        return event.getActionMasked();
    }

    /**
     * Call {@link MotionEvent#getAction}, returning only the pointer index
     * portion.
     *
     * @deprecated Call {@link MotionEvent#getActionIndex()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getActionIndex(MotionEvent event) {
        return event.getActionIndex();
    }

    /**
     * Call {@link MotionEvent#findPointerIndex(int)}.
     *
     * @deprecated Call {@link MotionEvent#findPointerIndex(int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int findPointerIndex(MotionEvent event, int pointerId) {
        return event.findPointerIndex(pointerId);
    }

    /**
     * Call {@link MotionEvent#getPointerId(int)}.
     *
     * @deprecated Call {@link MotionEvent#getPointerId(int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getPointerId(MotionEvent event, int pointerIndex) {
        return event.getPointerId(pointerIndex);
    }

    /**
     * Call {@link MotionEvent#getX(int)}.
     *
     * @deprecated Call {@link MotionEvent#getX()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static float getX(MotionEvent event, int pointerIndex) {
        return event.getX(pointerIndex);
    }

    /**
     * Call {@link MotionEvent#getY(int)}.
     *
     * @deprecated Call {@link MotionEvent#getY()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static float getY(MotionEvent event, int pointerIndex) {
        return event.getY(pointerIndex);
    }

    /**
     * The number of pointers of data contained in this event.  Always
     *
     * @deprecated Call {@link MotionEvent#getPointerCount()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getPointerCount(MotionEvent event) {
        return event.getPointerCount();
    }

    /**
     * Gets the source of the event.
     *
     * @return The event source or {@link InputDeviceCompat#SOURCE_UNKNOWN} if unknown.
     * @deprecated Call {@link MotionEvent#getSource()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getSource(MotionEvent event) {
        return event.getSource();
    }

    /**
     * Determines whether the event is from the given source.
     * @param source The input source to check against.
     * @return Whether the event is from the given source.
     */
    public static boolean isFromSource(MotionEvent event, int source) {
        return (event.getSource() & source) == source;
    }

    /**
     * Get axis value for the first pointer index (may be an
     * arbitrary pointer identifier).
     *
     * @param axis The axis identifier for the axis value to retrieve.
     *
     * @see #AXIS_X
     * @see #AXIS_Y
     *
     * @deprecated Call {@link MotionEvent#getAxisValue(int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static float getAxisValue(MotionEvent event, int axis) {
        return event.getAxisValue(axis);
    }

    /**
     * Returns the value of the requested axis for the given pointer <em>index</em>
     * (use {@link #getPointerId(MotionEvent, int)} to find the pointer identifier for this index).
     *
     * @param axis The axis identifier for the axis value to retrieve.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount(MotionEvent)}-1.
     * @return The value of the axis, or 0 if the axis is not available.
     *
     * @see #AXIS_X
     * @see #AXIS_Y
     *
     * @deprecated Call {@link MotionEvent#getAxisValue(int, int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static float getAxisValue(MotionEvent event, int axis, int pointerIndex) {
        return event.getAxisValue(axis, pointerIndex);
    }

    /**
     * @deprecated Call {@link MotionEvent#getButtonState()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static int getButtonState(MotionEvent event) {
        return event.getButtonState();
    }

    private MotionEventCompat() {}
}
