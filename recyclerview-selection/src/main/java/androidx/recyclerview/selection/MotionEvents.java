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
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Utility methods for working with {@link MotionEvent} instances.
 */
final class MotionEvents {

    private MotionEvents() {}

    static boolean isMouseEvent(MotionEvent e) {
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE;
    }

    static boolean isTouchEvent(MotionEvent e) {
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER;
    }

    static boolean isActionMove(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_MOVE;
    }

    static boolean isActionDown(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_DOWN;
    }

    static boolean isActionUp(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_UP;
    }

    static boolean isActionPointerUp(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_POINTER_UP;
    }

    @SuppressWarnings("unused")
    static boolean isActionPointerDown(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN;
    }

    static boolean isActionCancel(MotionEvent e) {
        return e.getActionMasked() == MotionEvent.ACTION_CANCEL;
    }

    static Point getOrigin(MotionEvent e) {
        return new Point((int) e.getX(), (int) e.getY());
    }

    static boolean isPrimaryButtonPressed(MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_PRIMARY);
    }

    static boolean isSecondaryButtonPressed(MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_SECONDARY);
    }

    static boolean isTertiaryButtonPressed(MotionEvent e) {
        return isButtonPressed(e, MotionEvent.BUTTON_TERTIARY);
    }

    // TODO: Replace with MotionEvent.isButtonPressed once targeting 21 or higher.
    private static boolean isButtonPressed(MotionEvent e, int button) {
        if (button == 0) {
            return false;
        }
        return (e.getButtonState() & button) == button;
    }

    static boolean isShiftKeyPressed(MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_SHIFT_ON);
    }

    static boolean isCtrlKeyPressed(MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_CTRL_ON);
    }

    static boolean isAltKeyPressed(MotionEvent e) {
        return hasBit(e.getMetaState(), KeyEvent.META_ALT_ON);
    }

    static boolean isTouchpadScroll(MotionEvent e) {
        // Touchpad inputs are treated as mouse inputs, and when scrolling, there are no buttons
        // returned.
        return isMouseEvent(e) && isActionMove(e) && e.getButtonState() == 0;
    }

    private static boolean hasBit(int metaState, int bit) {
        return (metaState & bit) != 0;
    }
}
