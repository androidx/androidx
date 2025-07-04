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

import static androidx.core.util.Preconditions.checkArgument;

import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

/**
 * A wrapper class for GestureDetector allowing it interact with SelectionTracker
 * and its dependencies (like RecyclerView) on terms more amenable to SelectionTracker.
 */
final class GestureDetectorWrapper implements RecyclerView.OnItemTouchListener, Resettable {

    private final GestureDetector mDetector;
    private boolean mDisallowIntercept;

    GestureDetectorWrapper(@NonNull GestureDetector detector) {
        checkArgument(detector != null);

        mDetector = detector;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // Reset disallow when the event is down as advised in http://b/139141511#comment20.
        if (mDisallowIntercept && MotionEvents.isActionDown(e)) {
            mDisallowIntercept = false;
        }

        // While the idea of "intercepting" an event stream isn't consistent
        // with the world-view of GestureDetector, failure to return true here
        // resulted in a bug where a context menu shown on an item view was not
        // visible...despite returning reporting that the menu was shown.
        // See b/143494310 for further details.
        return !mDisallowIntercept && mDetector.onTouchEvent(e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (!disallowIntercept) {
            return;  // Ignore as advised in http://b/139141511#comment20
        }

        // Some types of views, such as HorizontalScrollView, may want
        // to take over the input stream. In this case they'll call this method
        // with disallowIntercept=true. mDisallowIntercept is reset on UP or CANCEL
        // events in onInterceptTouchEvent.
        mDisallowIntercept = disallowIntercept;


        // GestureDetector may have internal state (such as timers) that can
        // result in subsequent event handlers being called, even after
        // we receive a request to disallow intercept (e.g. LONG_PRESS).
        // For that reason we proactively reset GestureDetector.
        sendCancelEvent();
    }

    @Override
    public boolean isResetRequired() {
        // Always resettable as we don't know the specifics of GD's internal state.
        return true;
    }

    @Override
    public void reset() {
        mDisallowIntercept = false;
        sendCancelEvent();
    }

    private void sendCancelEvent() {
        // GestureDetector does not provide a public affordance for resetting
        // it's internal state, so we send it a synthetic ACTION_CANCEL event.
        mDetector.onTouchEvent(MotionEvents.createCancelEvent());
    }
}
