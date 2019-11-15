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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Class allowing GestureDetector to listen directly to RecyclerView touch events.
 */
final class GestureDetectorOnItemTouchListenerAdapter implements RecyclerView.OnItemTouchListener {

    private final GestureDetector mDetector;

    GestureDetectorOnItemTouchListenerAdapter(@NonNull GestureDetector detector) {
        checkArgument(detector != null);

        mDetector = detector;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // While the idea of "intercepting" an event stream isn't consistent
        // with the world-view of GestureDetector, failure to return true here
        // resulted in a bug where a context menu shown on an item view was not
        // visible...despite returning reporting that the menu was shown.
        // See b/143494310 for further details.
        return mDetector.onTouchEvent(e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}

