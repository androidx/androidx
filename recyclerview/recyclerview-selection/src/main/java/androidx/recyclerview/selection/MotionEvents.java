/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import android.graphics.Point;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.jspecify.annotations.NonNull;

/**
 * Utility methods for working with {@link MotionEvent} instances.
 */
final class MotionEvents {

    private MotionEvents() {
    }

    static boolean isTouchpadEvent(@NonNull MotionEvent e) {
        // ChromeOS ARC devices with touchpads emit their events with
        // {@link MotionEvent#TOOL_TYPE_MOUSE}, so this is specifically capturing non-ARC devices
        // with touchpads (e.g. attachable keyboards with touchpads on Android tablets).
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                && e.getSource() == InputDevice.SOURCE_MOUSE;
    }

    static boolean isMouseEvent(@NonNull MotionEvent e) {
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE;
    }

    static boolean isFingerEvent(@NonNull MotionEvent e) {
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER;
    }

    static boolean isActionDown(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_DOWN;
    }

    static boolean isActionMove(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_MOVE;
    }

    static boolean isActionUp(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_UP;
    }

    static boolean isActionPointerUp(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_POINTER_UP;
    }

    @SuppressWarnings("unused")
    static boolean isActionPointerDown(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN;
    }

    static boolean isActionCancel(@NonNull MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_CANCEL;
    }

    static Point getOrigin(@NonNull MotionEvent e) {
        return new Point((int) e.getX(), (int) e.getY());
    }

    static boolean isPrimaryMouseButtonPressed(@NonNull MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_PRIMARY);
    }

    // alsoInterpretNoButtonsPressedAsPrimary works around imperfect test code. It should be true
    // only if the caller is certain that e is a mouse (not touch) MotionEvent.
    static boolean isPrimaryMouseButtonPressed(
            @NonNull MotionEvent e,
            boolean alsoInterpretNoButtonsPressedAsPrimary) {
        return isButtonPressed(e, MotionEvent.BUTTON_PRIMARY)
            // In real life, for ACTION_DOWN, touch MotionEvent objects can have getButtonState()
            // return zero but mouse MotionEvent objects should see non-zero. However, some test
            // code creates synthetic MotionEvents whose getToolType() returns TOOL_TYPE_MOUSE but
            // the test code forgets to also set which mouse button caused the ACTION_DOWN. For
            // tests, getButtonState() can unintuitively return zero (despite ACTION_DOWN) for
            // mouse. If alsoInterpretNoButtonsPressedAsPrimary then we also interpret these events
            // as implicitly being a "primary mouse button" click.
            || (alsoInterpretNoButtonsPressedAsPrimary && (e.getButtonState() == 0));
    }

    static boolean isSecondaryMouseButtonPressed(@NonNull MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_SECONDARY);
    }

    static boolean isTertiaryMouseButtonPressed(@NonNull MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_TERTIARY);
    }

    // NOTE: Can replace this with MotionEvent.isButtonPressed once targeting 21 or higher.
    private static boolean isButtonPressed(MotionEvent e, int button) {
        if (button == 0) {
            return false;
        }
        return (e.getButtonState() & button) == button;
    }

    static boolean isShiftKeyPressed(@NonNull MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_SHIFT_ON);
    }

    static boolean isCtrlKeyPressed(@NonNull MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_CTRL_ON);
    }

    static boolean isAltKeyPressed(@NonNull MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_ALT_ON);
    }

    static boolean isTouchpadScroll(@NonNull MotionEvent e) {
        // Touchpad inputs are treated as mouse inputs, and when scrolling, there are no buttons
        // returned.
        return (isTouchpadEvent(e) || isMouseEvent(e)) && isActionMove(e)
                && e.getButtonState() == 0;
    }

    private static boolean hasBit(int metaState, int bit) {
        return (metaState & bit) != 0;
    }

    static MotionEvent createCancelEvent() {
        return MotionEvent.obtain(
                0,     // down time
                1,     // event time
                MotionEvent.ACTION_CANCEL,
                0,  // x
                0,  // y
                0  // metaState
        );
    }
}
