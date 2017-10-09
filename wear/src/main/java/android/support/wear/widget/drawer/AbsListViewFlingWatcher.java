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
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingListener;
import android.support.wear.widget.drawer.FlingWatcherFactory.FlingWatcher;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import java.lang.ref.WeakReference;

/**
 * {@link FlingWatcher} implementation for {@link AbsListView AbsListViews}. Detects the end of
 * a Fling by waiting until the scroll state is no longer {@link
 * OnScrollListener#SCROLL_STATE_FLING}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
class AbsListViewFlingWatcher implements FlingWatcher, OnScrollListener {

    private final FlingListener mListener;
    private final WeakReference<AbsListView> mListView;

    AbsListViewFlingWatcher(FlingListener listener, AbsListView listView) {
        mListener = listener;
        mListView = new WeakReference<>(listView);
    }

    @Override
    public void watch() {
        AbsListView absListView = mListView.get();
        if (absListView != null) {
            absListView.setOnScrollListener(this);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState != OnScrollListener.SCROLL_STATE_FLING) {
            view.setOnScrollChangeListener(null);
            mListener.onFlingComplete(view);
        }
    }

    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
}
