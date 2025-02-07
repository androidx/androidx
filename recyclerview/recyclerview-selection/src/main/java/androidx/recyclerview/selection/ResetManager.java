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

import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class managing resetting of library state in response to specific
 * events like clearing of selection and MotionEvent.ACTION_CANCEL
 * events.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
final class ResetManager<K> {

    private static final String TAG = "ResetManager";

    private final List<Resettable> mResetHandlers = new ArrayList<>();

    private final OnItemTouchListener mInputListener = new OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView unused,
                @NonNull MotionEvent e) {
            if (MotionEvents.isActionCancel(e)) {
                if (DEBUG) Log.d(TAG, "Received CANCEL event.");
                callResetHandlers();
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

    // Resettable interface has a #requiresReset method because DefaultSelectionTracker
    // (owner of the state we observe with our SelectionObserver) is, itself,
    // a Resettable. Such an arrangement introduces the real possibility of infinite recursion.
    // When we call reset on DefaultSelectionTracker it'll eventually call back to
    // notify us of the change via onSelectionCleared. We avoid recursion by
    // checking #requiresReset before calling reset again.
    private final SelectionObserver<K> mSelectionObserver = new SelectionObserver<K>() {
        @Override
        protected void onSelectionCleared() {
            if (DEBUG) Log.d(TAG, "Received onSelectionCleared event.");
            callResetHandlers();
        }
    };

    SelectionObserver<K> getSelectionObserver() {
        return mSelectionObserver;
    }

    OnItemTouchListener getInputListener() {
        return mInputListener;
    }

    /**
     * Registers a new Resettable.
     */
    void addResetHandler(@NonNull Resettable handler) {
        mResetHandlers.add(handler);
    }

    void callResetHandlers() {
        for (Resettable handler : mResetHandlers) {
            if (handler.isResetRequired()) {
                handler.reset();
            }
        }
    }
}
