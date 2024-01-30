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

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A {@link PointerGesture} represents the actions of a single pointer when performing a gesture.
 */
class PointerGesture {
    // The list of actions that make up this gesture.
    private final Deque<PointerAction> mActions = new ArrayDeque<>();
    private final long mDelay;
    private final int mDisplayId;
    private long mDuration;

    /**
     * Constructs a PointerGesture which touches down at the given start point on the given display.
     */
    public PointerGesture(Point startPoint, int displayId) {
        this(startPoint, 0, displayId);
    }

    /**
     * Constructs a PointerGesture which touches down at the given start point on the give display
     * after a given delay.  Used in multi-point gestures when the pointers do not all touch down at
     * the same time.
     */
    public PointerGesture(Point startPoint, long initialDelay, int displayId) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("initialDelay cannot be negative");
        }
        mActions.addFirst(new PointerPauseAction(startPoint, 0));
        mDelay = initialDelay;
        mDisplayId = displayId;
    }

    public int displayId() {
        return mDisplayId;
    }

    /** Adds an action which pauses for the specified amount of {@code time} in milliseconds. */
    public PointerGesture pause(long time) {
        if (time < 0) {
            throw new IllegalArgumentException("time cannot be negative");
        }
        mActions.addLast(new PointerPauseAction(mActions.peekLast().end, time));
        mDuration += mActions.peekLast().duration;
        return this;
    }

    /** Adds an action that moves the pointer to {@code dest} at {@code speed} pixels per second. */
    public PointerGesture move(Point dest, int speed) {
        mActions.addLast(new PointerLinearMoveAction(mActions.peekLast().end, dest, speed));
        mDuration += mActions.peekLast().duration;
        return this;
    }

    /** Returns the start point of this gesture. */
    public Point start() {
        return mActions.peekFirst().start;
    }

    /** Returns the end point of this gesture. */
    public Point end() {
        return mActions.peekLast().end;
    }

    /** Returns the duration of this gesture. */
    public long duration() {
        return mDuration;
    }

    /** Returns the amount of delay before this gesture starts. */
    public long delay() {
        return mDelay;
    }

    /** Returns the pointer location at {@code time} milliseconds into this gesture. */
    public Point pointAt(long time) {
        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
        time -= mDelay;
        for (PointerAction action : mActions) {
            if (time < action.duration) {
                return action.interpolate((float)time / action.duration);
            }
            time -= action.duration;
        }
        return mActions.peekLast().end;
    }

    @NonNull
    @Override
    public String toString() {
        return mActions.toString();
    }

    /** A {@link PointerAction} represents part of a {@link PointerGesture}. */
    private static abstract class PointerAction {
        final Point start;
        final Point end;
        final long duration;

        public PointerAction(Point startPoint, Point endPoint, long time) {
            start = startPoint;
            end = endPoint;
            duration = time;
        }

        public abstract Point interpolate(float fraction);
    }

    /** A {@link PointerPauseAction} holds the pointer steady for the given amount of time. */
    private static class PointerPauseAction extends PointerAction {

        public PointerPauseAction(Point startPoint, long time) {
            super(startPoint, startPoint, time);
        }

        @Override
        public Point interpolate(float fraction) {
            return new Point(start);
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Pause(point=%s, duration=%dms)", start, duration);
        }
    }

    /** Action that moves the pointer between two points at a constant speed. */
    private static class PointerLinearMoveAction extends PointerAction {

        public PointerLinearMoveAction(Point startPoint, Point endPoint, int speed) {
            super(startPoint, endPoint, (long)(1000 * calcDistance(startPoint, endPoint) / speed));
        }

        @Override
        public Point interpolate(float fraction) {
            Point ret = new Point(start);
            ret.offset((int)(fraction * (end.x - start.x)), (int)(fraction * (end.y - start.y)));
            return ret;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Move(start=%s, end=%s, duration=%dms)", start, end, duration);
        }

        private static double calcDistance(final Point a, final Point b) {
            return Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
        }
    }
}
