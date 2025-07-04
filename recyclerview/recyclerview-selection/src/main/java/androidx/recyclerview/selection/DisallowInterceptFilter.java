/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import org.jspecify.annotations.NonNull;

/**
 * Wrapper class that regulates delivery of MotionEvents to delegate listeners, uniformly
 * honoring requests to onRequestDisallowInterceptTouchEvent.
 * Wrap this class around other OnItemTouchListeners to bestow them with
 * proper support for onRequestDisallowInterceptTouchEvent.
 */
// TODO: Replace in-situ "disallow" implementation in EventRouter, ResetManager,
//  BandSelectionHelper, GestureSelectionHelper by wrapping w/ this class.
class DisallowInterceptFilter implements
        OnItemTouchListener, Resettable {

    private final OnItemTouchListener mDelegate;
    private boolean mDisallowIntercept;

    DisallowInterceptFilter(@NonNull OnItemTouchListener delegate) {
        mDelegate = delegate;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // Reset disallow when the event is down as advised in http://b/139141511#comment20.
        if (mDisallowIntercept && MotionEvents.isActionDown(e)) {
            mDisallowIntercept = false;
        }
        return !mDisallowIntercept && mDelegate.onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        mDelegate.onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = true;
    }

    @Override
    public boolean isResetRequired() {
        return mDisallowIntercept;
    }

    @Override
    public void reset() {
        mDisallowIntercept = false;
    }
}
