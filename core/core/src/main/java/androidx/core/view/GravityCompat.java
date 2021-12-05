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

import static android.os.Build.VERSION.SDK_INT;

import android.graphics.Rect;
import android.view.Gravity;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Compatibility shim for accessing newer functionality from {@link Gravity}.
 */
public final class GravityCompat {
    /** Raw bit controlling whether the layout direction is relative or not (START/END instead of
     * absolute LEFT/RIGHT).
     */
    public static final int RELATIVE_LAYOUT_DIRECTION = 0x00800000;

    /** Push object to x-axis position at the start of its container, not changing its size. */
    public static final int START = RELATIVE_LAYOUT_DIRECTION | Gravity.LEFT;

    /** Push object to x-axis position at the end of its container, not changing its size. */
    public static final int END = RELATIVE_LAYOUT_DIRECTION | Gravity.RIGHT;

    /**
     * Binary mask for the horizontal gravity and script specific direction bit.
     */
    public static final int RELATIVE_HORIZONTAL_GRAVITY_MASK = START | END;

    /**
     * Apply a gravity constant to an object and take care if layout direction is RTL or not.
     *
     * @param gravity The desired placement of the object, as defined by the
     *                constants in this class.
     * @param w The horizontal size of the object.
     * @param h The vertical size of the object.
     * @param container The frame of the containing space, in which the object
     *                  will be placed.  Should be large enough to contain the
     *                  width and height of the object.
     * @param outRect Receives the computed frame of the object in its
     *                container.
     * @param layoutDirection The layout direction.
     *
     * @see ViewCompat#LAYOUT_DIRECTION_LTR
     * @see ViewCompat#LAYOUT_DIRECTION_RTL
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
            @NonNull Rect outRect, int layoutDirection) {
        if (SDK_INT >= 17) {
            Api17Impl.apply(gravity, w, h, container, outRect, layoutDirection);
        } else {
            Gravity.apply(gravity, w, h, container, outRect);
        }
    }

    /**
     * Apply a gravity constant to an object.
     *
     * @param gravity The desired placement of the object, as defined by the
     *                constants in this class.
     * @param w The horizontal size of the object.
     * @param h The vertical size of the object.
     * @param container The frame of the containing space, in which the object
     *                  will be placed.  Should be large enough to contain the
     *                  width and height of the object.
     * @param xAdj Offset to apply to the X axis.  If gravity is LEFT this
     *             pushes it to the right; if gravity is RIGHT it pushes it to
     *             the left; if gravity is CENTER_HORIZONTAL it pushes it to the
     *             right or left; otherwise it is ignored.
     * @param yAdj Offset to apply to the Y axis.  If gravity is TOP this pushes
     *             it down; if gravity is BOTTOM it pushes it up; if gravity is
     *             CENTER_VERTICAL it pushes it down or up; otherwise it is
     *             ignored.
     * @param outRect Receives the computed frame of the object in its
     *                container.
     * @param layoutDirection The layout direction.
     *
     * @see ViewCompat#LAYOUT_DIRECTION_LTR
     * @see ViewCompat#LAYOUT_DIRECTION_RTL
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
            int xAdj, int yAdj, @NonNull Rect outRect, int layoutDirection) {
        if (SDK_INT >= 17) {
            Api17Impl.apply(gravity, w, h, container, xAdj, yAdj, outRect, layoutDirection);
        } else {
            Gravity.apply(gravity, w, h, container, xAdj, yAdj, outRect);
        }
    }

    /**
     * Apply additional gravity behavior based on the overall "display" that an
     * object exists in.  This can be used after
     * {@link Gravity#apply(int, int, int, Rect, int, int, Rect)} to place the object
     * within a visible display.  By default this moves or clips the object
     * to be visible in the display; the gravity flags
     * {@link Gravity#DISPLAY_CLIP_HORIZONTAL} and
     * {@link Gravity#DISPLAY_CLIP_VERTICAL} can be used to change this behavior.
     *
     * @param gravity Gravity constants to modify the placement within the
     * display.
     * @param display The rectangle of the display in which the object is
     * being placed.
     * @param inoutObj Supplies the current object position; returns with it
     * modified if needed to fit in the display.
     * @param layoutDirection The layout direction.
     *
     * @see ViewCompat#LAYOUT_DIRECTION_LTR
     * @see ViewCompat#LAYOUT_DIRECTION_RTL
     */
    public static void applyDisplay(int gravity, @NonNull Rect display, @NonNull Rect inoutObj,
            int layoutDirection) {
        if (SDK_INT >= 17) {
            Api17Impl.applyDisplay(gravity, display, inoutObj, layoutDirection);
        } else {
            Gravity.applyDisplay(gravity, display, inoutObj);
        }
    }

    /**
     * <p>Convert script specific gravity to absolute horizontal value.</p>
     *
     * if horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
     * if horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
     *
     *
     * @param gravity The gravity to convert to absolute (horizontal) values.
     * @param layoutDirection The layout direction.
     * @return gravity converted to absolute (horizontal) values.
     */
    public static int getAbsoluteGravity(int gravity, int layoutDirection) {
        if (SDK_INT >= 17) {
            return Gravity.getAbsoluteGravity(gravity, layoutDirection);
        } else {
            // Just strip off the relative bit to get LEFT/RIGHT.
            return gravity & ~RELATIVE_LAYOUT_DIRECTION;
        }
    }

    private GravityCompat() {
    }

    @RequiresApi(17)
    static class Api17Impl {
        private Api17Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void apply(int gravity, int w, int h, Rect container, Rect outRect,
                int layoutDirection) {
            Gravity.apply(gravity, w, h, container, outRect, layoutDirection);
        }

        @DoNotInline
        static void apply(int gravity, int w, int h, Rect container, int xAdj, int yAdj,
                Rect outRect, int layoutDirection) {
            Gravity.apply(gravity, w, h, container, xAdj, yAdj, outRect, layoutDirection);
        }

        @DoNotInline
        static void applyDisplay(int gravity, Rect display, Rect inoutObj, int layoutDirection) {
            Gravity.applyDisplay(gravity, display, inoutObj, layoutDirection);
        }
    }
}
