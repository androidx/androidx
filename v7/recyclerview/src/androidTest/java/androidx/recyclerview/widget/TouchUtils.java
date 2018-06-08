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

package androidx.recyclerview.widget;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * RV specific layout tests.
 */
public class TouchUtils {
    public static void tapView(Instrumentation inst, RecyclerView recyclerView,
            View v) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();
    }

    public static void touchAndCancelView(Instrumentation inst, View v) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_CANCEL,
                x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

    }

    public static void clickView(Instrumentation inst, View v) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void longClickView(Instrumentation inst, View v) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        try {
            Thread.sleep((long) (ViewConfiguration.getLongPressTimeout() * 1.5f));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();
    }

    public static void scrollView(int axis, int axisValue, int inputDevice, View v) {
        MotionEvent.PointerProperties[] pointerProperties = { new MotionEvent.PointerProperties() };
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.setAxisValue(axis, axisValue);
        MotionEvent.PointerCoords[] pointerCoords = { coords };
        MotionEvent e = MotionEvent.obtain(
                0, System.currentTimeMillis(), MotionEvent.ACTION_SCROLL,
                1, pointerProperties, pointerCoords, 0, 0, 1, 1, 0, 0, inputDevice, 0);
        v.onGenericMotionEvent(e);
        e.recycle();
    }

    public static void dragViewToTop(Instrumentation inst, View v) {
        dragViewToTop(inst, v, calculateStepsForDistance(v.getTop()));
    }

    public static void dragViewToTop(Instrumentation inst, View v, int stepCount) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float fromY = xy[1] + (viewHeight / 2.0f);
        float toY = 0;

        drag(inst, x, x, fromY, toY, stepCount);
    }

    private static void getStartLocation(View v, int gravity, int[] xy) {
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                break;
            case Gravity.CENTER_VERTICAL:
                xy[1] += viewHeight / 2;
                break;
            case Gravity.BOTTOM:
                xy[1] += viewHeight - 1;
                break;
            default:
                // Same as top -- do nothing
        }

        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                break;
            case Gravity.CENTER_HORIZONTAL:
                xy[0] += viewWidth / 2;
                break;
            case Gravity.RIGHT:
                xy[0] += viewWidth - 1;
                break;
            default:
                // Same as left -- do nothing
        }
    }

    public static int dragViewTo(Instrumentation inst, View v, int gravity, int toX,
            int toY) {
        int[] xy = new int[2];

        getStartLocation(v, gravity, xy);

        final int fromX = xy[0];
        final int fromY = xy[1];

        int deltaX = fromX - toX;
        int deltaY = fromY - toY;

        int distance = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        drag(inst, fromX, toX, fromY, toY, calculateStepsForDistance(distance));

        return distance;
    }

    public static int dragViewToX(Instrumentation inst, View v, int gravity, int toX) {
        int[] xy = new int[2];

        getStartLocation(v, gravity, xy);

        final int fromX = xy[0];
        final int fromY = xy[1];

        int deltaX = fromX - toX;

        drag(inst, fromX, toX, fromY, fromY, calculateStepsForDistance(deltaX));

        return deltaX;
    }

    public static int dragViewToY(Instrumentation inst, View v, int gravity, int toY) {
        int[] xy = new int[2];

        getStartLocation(v, gravity, xy);

        final int fromX = xy[0];
        final int fromY = xy[1];

        int deltaY = fromY - toY;

        drag(inst, fromX, fromX, fromY, toY, calculateStepsForDistance(deltaY));

        return deltaY;
    }


    public static void drag(Instrumentation inst, float fromX, float toX, float fromY,
            float toY, int stepCount) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        float y = fromY;
        float x = fromX;

        float yStep = (toY - fromY) / stepCount;
        float xStep = (toX - fromX) / stepCount;

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        for (int i = 0; i < stepCount; ++i) {
            y += yStep;
            x += xStep;
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
            inst.sendPointerSync(event);
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();
    }

    private static int calculateStepsForDistance(int distance) {
        return 1 + Math.abs(distance) / 10;
    }
}
