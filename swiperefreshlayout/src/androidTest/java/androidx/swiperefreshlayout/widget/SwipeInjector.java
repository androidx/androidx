/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.swiperefreshlayout.widget;

import static android.os.SystemClock.uptimeMillis;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.action.CoordinatesProvider;

public class SwipeInjector {

    private static final int X = 0;
    private static final int Y = 1;

    private Instrumentation mInstrumentation;

    private long mStartTime = 0;
    private long mCurrentTime = 0;
    private float[] mFloats = new float[2];

    public SwipeInjector(Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
    }

    public void startDrag(CoordinatesProvider provider, View view) {
        float[] floats = provider.calculateCoordinates(view);
        startDrag(floats[X], floats[Y]);
    }

    public void startDrag(float x, float y) {
        mStartTime = uptimeMillis();
        mFloats[X] = x;
        mFloats[Y] = y;
        injectMotionEvent(obtainDownEvent(mStartTime, mFloats));
    }

    public void dragTo(CoordinatesProvider provider, View view) {
        float[] floats = provider.calculateCoordinates(view);
        dragTo(floats[X], floats[Y]);
    }

    public void dragTo(float x, float y) {
        mFloats[X] = x;
        mFloats[Y] = y;
        injectMotionEvent(obtainMoveEvent(mStartTime, uptimeMillis(), mFloats));
    }

    public void dragTo(CoordinatesProvider provider, View view, long duration) {
        float[] floats = provider.calculateCoordinates(view);
        dragTo(floats[X], floats[Y], duration);
    }

    public void dragTo(float x, float y, long duration) {
        float x0 = mFloats[X];
        float y0 = mFloats[Y];
        float dx = x - x0;
        float dy = y - y0;
        int steps = Math.max(1, Math.round(duration / 10f));
        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            mFloats[X] = x0 + dx * progress;
            mFloats[Y] = y0 + dy * progress;
            injectMotionEvent(obtainMoveEvent(mStartTime, mCurrentTime + 10L, mFloats));
        }
    }

    public void dragBy(float dx, float dy) {
        dragTo(mFloats[X] + dx, mFloats[Y] + dy);
    }

    public void dragBy(float dx, float dy, long duration) {
        dragTo(mFloats[X] + dx, mFloats[Y] + dy, duration);
    }

    public void finishDrag() {
        injectMotionEvent(obtainUpEvent(mStartTime, uptimeMillis(), mFloats));
    }

    private static MotionEvent obtainDownEvent(long time, float[] coord) {
        return MotionEvent.obtain(time, time,
                MotionEvent.ACTION_DOWN, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainMoveEvent(long startTime, long time, float[] coord) {
        return MotionEvent.obtain(startTime, time,
                MotionEvent.ACTION_MOVE, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainUpEvent(long startTime, long time, float[] coord) {
        return MotionEvent.obtain(startTime, time,
                MotionEvent.ACTION_UP, coord[X], coord[Y], 0);
    }

    private void injectMotionEvent(MotionEvent event) {
        try {
            long eventTime = event.getEventTime();
            long now = uptimeMillis();
            if (eventTime - now > 0) {
                SystemClock.sleep(eventTime - now);
            }
            mInstrumentation.sendPointerSync(event);
            mCurrentTime = eventTime;
        } finally {
            event.recycle();
        }
    }
}
