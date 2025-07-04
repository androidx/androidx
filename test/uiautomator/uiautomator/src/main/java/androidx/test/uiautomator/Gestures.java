/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.ViewConfiguration;

/** Factory methods for constructing {@link PointerGesture}s. */
class Gestures {

    // Duration of a long press (with multiplier to ensure detection).
    private static final long LONG_PRESS_DURATION_MS =
            (long) (ViewConfiguration.getLongPressTimeout() * 1.5f);

    // Constants used by pinch gestures
    private static final int INNER = 0;
    private static final int OUTER = 1;

    private final int mDisplayId;
    private final int mWindowId;

    /**
     * Construct a {@link Gestures} factory for building gestures that target the given display and
     * window IDs.
     *
     * @param displayId The ID of the display the gesture is on.
     * @param windowId The ID of the window the gesture is on.
     */
    Gestures(int displayId, int windowId) {
        mDisplayId = displayId;
        mWindowId = windowId;
    }

    /**
     * Returns a {@link PointerGesture} representing a click at the given {@code point}.
     *
     * @param point The point to click.
     * @return The {@link PointerGesture} representing this click.
     */
    public PointerGesture click(Point point) {
        // A basic click is a touch down and touch up over the same point with no delay.
        return click(point, 0);
    }

    /**
     * Returns a {@link PointerGesture} representing a click at the given {@code point} that lasts
     * for {@code duration} milliseconds.
     *
     * @param point The point to click.
     * @param duration The duration of the click in milliseconds.
     * @return The {@link PointerGesture} representing this click.
     */
    public PointerGesture click(Point point, long duration) {
        // A click is a touch down and touch up over the same point with an optional delay inbetween
        return new PointerGesture(point, mDisplayId, mWindowId).pause(duration);
    }

    /**
     * Returns a {@link PointerGesture} representing a long click at the given {@code point}.
     *
     * @param point The point to click.
     * @return The {@link PointerGesture} representing this long click.
     */
    public PointerGesture longClick(Point point) {
        // A long click is a click with a duration that exceeds a certain threshold.
        return click(point, LONG_PRESS_DURATION_MS);
    }

    /**
     * Returns a {@link PointerGesture} representing a swipe.
     *
     * @param start The touch down point for the swipe.
     * @param end The touch up point for the swipe.
     * @param speed The speed at which to move in pixels per second.
     * @return The {@link PointerGesture} representing this swipe.
     */
    public PointerGesture swipe(Point start, Point end, int speed) {
        // A swipe is a click that moves before releasing the pointer.
        return new PointerGesture(start, mDisplayId, mWindowId).move(end, speed);
    }

    /**
     * Returns a {@link PointerGesture} representing a horizontal or vertical swipe over an area.
     *
     * @param area The area to swipe over.
     * @param direction The direction in which to swipe.
     * @param percent The size of the swipe as a percentage of the total area.
     * @param speed The speed at which to move in pixels per second.
     * @return The {@link PointerGesture} representing this swipe.
     */
    public PointerGesture swipeRect(Rect area, Direction direction, float percent, int speed) {
        Point start, end;
        // TODO: Reverse horizontal direction if locale is RTL
        switch (direction) {
            case LEFT:
                start = new Point(area.right, area.centerY());
                end   = new Point(area.right - (int)(area.width() * percent), area.centerY());
                break;
            case RIGHT:
                start = new Point(area.left, area.centerY());
                end   = new Point(area.left + (int)(area.width() * percent), area.centerY());
                break;
            case UP:
                start = new Point(area.centerX(), area.bottom);
                end   = new Point(area.centerX(), area.bottom - (int)(area.height() * percent));
                break;
            case DOWN:
                start = new Point(area.centerX(), area.top);
                end   = new Point(area.centerX(), area.top + (int)(area.height() * percent));
                break;
            default:
                throw new RuntimeException();
        }

        return swipe(start, end, speed);
    }

    /**
     * Returns a {@link PointerGesture} representing a click and drag.
     *
     * @param start The touch down point for the swipe.
     * @param end The touch up point for the swipe.
     * @param speed The speed at which to move in pixels per second.
     * @return The {@link PointerGesture} representing this swipe.
     */
    public PointerGesture drag(Point start, Point end, int speed) {
        // A drag is a swipe that starts with a long click.
        return longClick(start).move(end, speed);
    }

    /**
     * Returns an array of {@link PointerGesture}s representing a pinch close.
     *
     * @param area The area to pinch over.
     * @param percent The size of the pinch as a percentage of the total area.
     * @param speed The speed at which to move in pixels per second.
     * @return An array containing the two PointerGestures representing this pinch.
     */
    public PointerGesture[] pinchClose(Rect area, float percent, int speed) {
        Point[] bottomLeft = new Point[2];
        Point[] topRight = new Point[2];
        calcPinchCoordinates(area, percent, bottomLeft, topRight);

        // A pinch close is a multi-point gesture composed of two swipes moving from the outer
        // coordinates to the inner ones.
        return new PointerGesture[] {
            swipe(bottomLeft[OUTER], bottomLeft[INNER], speed).pause(250),
            swipe(topRight[OUTER], topRight[INNER], speed).pause(250)
        };
    }

    /**
     * Returns an array of {@link PointerGesture}s representing a pinch close.
     *
     * @param area The area to pinch over.
     * @param percent The size of the pinch as a percentage of the total area.
     * @param speed The speed at which to move in pixels per second.
     * @return An array containing the two PointerGestures representing this pinch.
     */
    public PointerGesture[] pinchOpen(Rect area, float percent, int speed) {
        Point[] bottomLeft = new Point[2];
        Point[] topRight = new Point[2];
        calcPinchCoordinates(area, percent, bottomLeft, topRight);

        // A pinch open is a multi-point gesture composed of two swipes moving from the inner
        // coordinates to the outer ones.
        return new PointerGesture[] {
            swipe(bottomLeft[INNER], bottomLeft[OUTER], speed),
            swipe(topRight[INNER], topRight[OUTER], speed)
        };
    }

    /** Calculates the inner and outer coordinates used in a pinch gesture. */
    private static void calcPinchCoordinates(Rect area, float percent,
            Point[] bottomLeft, Point[] topRight) {

        int offsetX = (int) (area.width() / 2 * percent);
        int offsetY = (int) (area.height() / 2 * percent);

        // Outer set of pinch coordinates
        bottomLeft[OUTER] = new Point(area.left, area.bottom);
        topRight[OUTER]   = new Point(area.right, area.top);

        // Inner set of pinch coordinates
        bottomLeft[INNER] = new Point(bottomLeft[OUTER]);
        bottomLeft[INNER].offset(offsetX, -offsetY);
        topRight[INNER] = new Point(topRight[OUTER]);
        topRight[INNER].offset(-offsetX, offsetY);
    }
}
