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

import static androidx.core.util.Preconditions.checkArgument;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * GestureRouter is responsible for routing gestures detected by a GestureDetector
 * to registered handlers. The primary function is to divide events by tool-type
 * allowing handlers to cleanly implement tool-type specific policies.
 *
 * @param <T> listener type. Must extend OnGestureListener & OnDoubleTapListener.
 */
final class GestureRouter<T extends OnGestureListener & OnDoubleTapListener>
        implements OnGestureListener, OnDoubleTapListener {

    private final ToolHandlerRegistry<T> mDelegates;

    GestureRouter(@NonNull T defaultDelegate) {
        checkArgument(defaultDelegate != null);
        mDelegates = new ToolHandlerRegistry<>(defaultDelegate);
    }

    @SuppressWarnings("unchecked")
    GestureRouter() {
        this((T) new SimpleOnGestureListener());
    }

    /**
     * @param toolType
     * @param delegate the delegate, or null to unregister.
     */
    public void register(int toolType, @Nullable T delegate) {
        mDelegates.set(toolType, delegate);
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        return mDelegates.get(e).onSingleTapConfirmed(e);
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        return mDelegates.get(e).onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
        return mDelegates.get(e).onDoubleTapEvent(e);
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return mDelegates.get(e).onDown(e);
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {
        mDelegates.get(e).onShowPress(e);
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return mDelegates.get(e).onSingleTapUp(e);
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
            float distanceX, float distanceY) {
        return mDelegates.get(e2).onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        mDelegates.get(e).onLongPress(e);
    }

    @Override
    public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
            float velocityX, float velocityY) {
        return mDelegates.get(e2).onFling(e1, e2, velocityX, velocityY);
    }
}
