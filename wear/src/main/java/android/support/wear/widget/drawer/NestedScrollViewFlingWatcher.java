/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.wear.widget.drawer;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.NestedScrollView.OnScrollChangeListener;
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingListener;
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingWatcher;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * {@link FlingWatcher} implementation for {@link NestedScrollView NestedScrollViews}.
 * <p>
 * Because {@link NestedScrollView} does not provide a way to listen to the scroll state, there's no
 * callback which definitely indicates the fling has finished. So, we instead listen for scroll
 * events. If we reach the top or bottom of the view or if there are no events within {@link
 * #MAX_WAIT_TIME_MS}, we assume the fling has finished.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
class NestedScrollViewFlingWatcher implements FlingWatcher, OnScrollChangeListener {

    static final int MAX_WAIT_TIME_MS = 100;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final FlingListener mListener;
    private final WeakReference<NestedScrollView> mNestedScrollView;
    private final Runnable mNotifyListenerRunnable = new Runnable() {
        @Override
        public void run() {
            onEndOfFlingFound();
        }
    };

    NestedScrollViewFlingWatcher(FlingListener listener, NestedScrollView nestedScrollView) {
        mListener = listener;
        mNestedScrollView = new WeakReference<>(nestedScrollView);
    }

    private static boolean isViewAtTopOrBottom(View view) {
        return !view.canScrollVertically(-1 /* up */) || !view.canScrollVertically(1 /* down */);
    }

    @Override
    public void watch() {
        NestedScrollView nestedScrollView = mNestedScrollView.get();
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener(this);
            scheduleNext();
        }
    }

    @Override
    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX,
                               int oldScrollY) {
        if (isViewAtTopOrBottom(v)) {
            onEndOfFlingFound();
        } else {
            scheduleNext();
        }
    }

    private void onEndOfFlingFound() {
        mMainThreadHandler.removeCallbacks(mNotifyListenerRunnable);
        NestedScrollView nestedScrollView = mNestedScrollView.get();
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((OnScrollChangeListener) null);
            mListener.onFlingComplete(nestedScrollView);
        }
    }

    private void scheduleNext() {
        mMainThreadHandler.removeCallbacks(mNotifyListenerRunnable);
        mMainThreadHandler.postDelayed(mNotifyListenerRunnable, MAX_WAIT_TIME_MS);
    }
}
