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

import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingListener;
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingWatcher;

import java.lang.ref.WeakReference;

/**
 * {@link FlingWatcher} implementation for {@link RecyclerView RecyclerViews}. Detects the end of
 * a Fling by waiting until the scroll state becomes idle.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
class RecyclerViewFlingWatcher extends OnScrollListener implements FlingWatcher {

    private final FlingListener mListener;
    private final WeakReference<RecyclerView> mRecyclerView;

    RecyclerViewFlingWatcher(FlingListener listener, RecyclerView view) {
        mListener = listener;
        mRecyclerView = new WeakReference<>(view);
    }

    @Override
    public void watch() {
        RecyclerView recyclerView = mRecyclerView.get();
        if (recyclerView != null) {
            recyclerView.addOnScrollListener(this);
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            mListener.onFlingComplete(recyclerView);
            recyclerView.removeOnScrollListener(this);
        }
    }
}
