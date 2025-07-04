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
 * OnItemTouchListener that claims all ACTION_UP events in streams that have otherwise gone
 * unclaimed after a LongPress has been detected by GestureDetector.
 * This addresses issue described in b/166836317, where child view
 * OnClickListeners were being called unexpectedly.
 */
class EventBackstop implements OnItemTouchListener, Resettable {

    private boolean mLongPressFired;

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // We only claim ACTION_UP events after a LongPress event. Were we to claim
        // all ACTION_UP events we'd deprive RecyclerView of the signal it needs to
        // initiate fling scrolling.
        if (MotionEvents.isActionUp(e) && mLongPressFired) {
            mLongPressFired = false;
            return true;
        }

        // Recover from disallow state.
        if (MotionEvents.isActionDown(e) && isResetRequired()) {
            reset();
        }
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // We should never receive any events, but were we to...we want to ignore them.
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        throw new UnsupportedOperationException("Wrap me in an InterceptFilter.");
    }

    @Override
    public boolean isResetRequired() {
        return  mLongPressFired;
    }

    @Override
    public void reset() {
        mLongPressFired = false;
    }

    void onLongPress() {
        mLongPressFired = true;
    }
}
