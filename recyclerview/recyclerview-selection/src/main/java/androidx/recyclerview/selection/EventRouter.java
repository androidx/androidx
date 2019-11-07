/*
 * Copyright 2017 The Android Open Source Project
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

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

/**
 * A class responsible for routing MotionEvents to tool-type specific handlers.
 * Individual tool-type specific handlers are added after the class is constructed.
 *
 * <p>
 * EventRouter takes its name from
 * {@link RecyclerView#addOnItemTouchListener(OnItemTouchListener)}. Despite "Touch"
 * being in the name, it receives MotionEvents for all types of tools.
 */
final class EventRouter implements OnItemTouchListener {

    private final ToolHandlerRegistry<OnItemTouchListener> mDelegates;

    EventRouter() {
        mDelegates = new ToolHandlerRegistry<>(new DummyOnItemTouchListener());
    }

    /**
     * @param toolType See MotionEvent for details on available types.
     * @param delegate An {@link OnItemTouchListener} to receive events
     *     of {@code toolType}.
     */
    void set(int toolType, @NonNull OnItemTouchListener delegate) {
        checkArgument(delegate != null);

        mDelegates.set(toolType, delegate);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        return mDelegates.get(e).onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        mDelegates.get(e).onTouchEvent(rv, e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // TODO(b/139141511): Handle onRequestDisallowInterceptTouchEvent.
    }
}
