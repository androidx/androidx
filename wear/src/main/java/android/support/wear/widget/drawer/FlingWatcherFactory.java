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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ScrollView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Creates a {@link FlingWatcher} based on the type of {@link View}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
class FlingWatcherFactory {

    /**
     * Listener that is notified when a fling completes and the view has settled. Polling may be
     * used to determine when the fling has completed, so there may be up to a 100ms delay.
     */
    interface FlingListener {
        void onFlingComplete(View view);
    }

    /**
     * Watches a given {@code view} to detect the end of a fling. Will notify a {@link
     * FlingListener} when the end is found.
     */
    interface FlingWatcher {
        void watch();
    }

    private final FlingListener mListener;
    private final Map<View, FlingWatcher> mWatchers = new WeakHashMap<>();

    FlingWatcherFactory(FlingListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("FlingListener was null");
        }

        mListener = listener;
    }

    /**
     * Returns a {@link FlingWatcher} for the particular type of {@link View}.
     */
    @Nullable
    FlingWatcher getFor(View view) {
        FlingWatcher watcher = mWatchers.get(view);
        if (watcher == null) {
            watcher = createFor(view);
            if (watcher != null) {
                mWatchers.put(view, watcher);
            }
        }

        return watcher;
    }

    /**
     * Creates a {@link FlingWatcher} for the particular type of {@link View}.
     */
    @Nullable
    private FlingWatcher createFor(View view) {
        if (view == null) {
            throw new IllegalArgumentException("View was null");
        }

        if (view instanceof RecyclerView) {
            return new RecyclerViewFlingWatcher(mListener, (RecyclerView) view);
        } else if (view instanceof AbsListView) {
            return new AbsListViewFlingWatcher(mListener, (AbsListView) view);
        } else if (view instanceof ScrollView) {
            return new ScrollViewFlingWatcher(mListener, (ScrollView) view);
        } else if (view instanceof NestedScrollView) {
            return new NestedScrollViewFlingWatcher(mListener, (NestedScrollView) view);
        } else {
            return null;
        }
    }
}
