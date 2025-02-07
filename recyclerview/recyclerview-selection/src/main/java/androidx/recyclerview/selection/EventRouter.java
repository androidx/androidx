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

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import org.jspecify.annotations.NonNull;

/**
 * A class responsible for routing MotionEvents to tool-type specific handlers.
 * Individual tool-type specific handlers are added after the class is constructed.
 *
 * <p>
 * EventRouter takes its name from
 * {@link RecyclerView#addOnItemTouchListener(OnItemTouchListener)}. Despite "Touch"
 * being in the name, it receives MotionEvents for all types of tools.
 */
final class EventRouter implements OnItemTouchListener, Resettable {

    private final ToolSourceHandlerRegistry<OnItemTouchListener> mDelegates;
    private boolean mDisallowIntercept;

    EventRouter() {
        mDelegates = new ToolSourceHandlerRegistry<>(new StubOnItemTouchListener());
    }

    /**
     * @param key      Either a TOOL_TYPE or a combination of TOOL_TYPE and SOURCE
     * @param delegate An {@link OnItemTouchListener} to receive events of {@code ToolSourceKey}.
     */
    void set(@NonNull ToolSourceKey key, @NonNull OnItemTouchListener delegate) {
        checkArgument(delegate != null);

        mDelegates.set(key, delegate);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // Reset disallow when the event is down as advised in http://b/139141511#comment20.
        if (mDisallowIntercept && MotionEvents.isActionDown(e)) {
            mDisallowIntercept = false;
        }
        return !mDisallowIntercept && mDelegates.get(e).onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        if (!mDisallowIntercept) {
            mDelegates.get(e).onTouchEvent(rv, e);
        }
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
