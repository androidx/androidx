/*
 * Copyright 2018 The Android Open Source Project
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
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import org.jspecify.annotations.Nullable;

/**
 * OnItemTouchListener that detects and delegates drag events to a drag listener, else sends event
 * to fallback {@link OnItemTouchListener}.
 *
 * <p>See {@link OnDragInitiatedListener} for details on implementing drag and drop.
 */
final class PointerDragEventInterceptor implements OnItemTouchListener {

    private final ItemDetailsLookup<?> mEventDetailsLookup;
    private final OnDragInitiatedListener mDragListener;
    private OnItemTouchListener mDelegate;
    private float mDownX;
    private float mDownY;
    private boolean mDownInItemDragRegion;

    PointerDragEventInterceptor(
            ItemDetailsLookup<?> eventDetailsLookup,
            OnDragInitiatedListener dragListener,
            @Nullable OnItemTouchListener delegate) {

        checkArgument(eventDetailsLookup != null);
        checkArgument(dragListener != null);

        mEventDetailsLookup = eventDetailsLookup;
        mDragListener = dragListener;

        if (delegate != null) {
            mDelegate = delegate;
        } else {
            mDelegate = new StubOnItemTouchListener();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (MotionEvents.isPrimaryMouseButtonPressed(e)) {
            float x = e.getX();
            float y = e.getY();
            if (MotionEvents.isActionDown(e)) {
                mDownX = x;
                mDownY = y;
                mDownInItemDragRegion = mEventDetailsLookup.inItemDragRegion(e);
            } else if (mDownInItemDragRegion && MotionEvents.isActionMove(e)) {
                int touchSlop = ViewConfiguration.get(rv.getContext()).getScaledTouchSlop();
                float dx = x - mDownX;
                float dy = y - mDownY;
                float distanceSquared = (dx * dx) + (dy * dy);
                if (distanceSquared > (touchSlop * touchSlop)) {
                    return mDragListener.onDragInitiated(e);
                }
            }
        }
        return mDelegate.onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        mDelegate.onTouchEvent(rv, e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDelegate.onRequestDisallowInterceptTouchEvent(disallowIntercept);
    }
}
