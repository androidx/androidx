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

package androidx.recyclerview.selection;

import static androidx.recyclerview.selection.Shared.DEBUG;

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager resetting various states across the lib.
 *
 * E.g....
 * When selection is explicitly cleared, reset.
 * When cancel event is received, reset.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
final class ResetManager<K> {

    private static final String TAG = "ResetManager";

    private final List<Runnable> mListeners = new ArrayList<>();

    private final OnItemTouchListener mInputListener = new OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView unused,
                @NonNull MotionEvent e) {
            if (MotionEvents.isActionCancel(e)) {
                if (DEBUG) Log.d(TAG, "Received CANCEL event.");
                notifyResetListeners();
            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    };

    private final SelectionObserver<K> mSelectionObserver = new SelectionObserver<K>() {
        @Override
        protected void onSelectionCleared() {
            if (DEBUG) Log.d(TAG, "Received onSelectionCleared event.");
            notifyResetListeners();
        }

        @Override
        public void onSelectionRefresh() {
            if (DEBUG) Log.d(TAG, "Received onSelectionRefresh event.");
            notifyResetListeners();
        }

        @Override
        public void onSelectionRestored() {
            if (DEBUG) Log.d(TAG, "Received onSelectionRestored event.");
            notifyResetListeners();
        }
    };

    SelectionObserver<K> getSelectionObserver() {
        return mSelectionObserver;
    }

    OnItemTouchListener getInputListener() {
        return mInputListener;
    }

    /**
     * Registers a new listener.
     */
    void addResetListener(@NonNull Runnable listener) {
        mListeners.add(listener);
    }

    public void forceReset() {
        notifyResetListeners();
    }

    void notifyResetListeners() {
        for (Runnable listener : mListeners) {
            listener.run();
        }
    }
}
