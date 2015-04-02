/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A ViewGroup for managing focus behavior between overlapping views.
 */
public class BrowseFrameLayout extends FrameLayout {

    /**
     * Interface for selecting a focused view in a BrowseFrameLayout when the system focus finder
     * couldn't find a view to focus.
     */
    public interface OnFocusSearchListener {
        /**
         * Returns the view where focus should be requested given the current focused view and
         * the direction of focus search.
         */
        public View onFocusSearch(View focused, int direction);
    }

    /**
     * Interface for managing child focus in a BrowseFrameLayout.
     */
    public interface OnChildFocusListener {
        /**
         * See {@link android.view.ViewGroup#onRequestFocusInDescendants(
         * int, android.graphics.Rect)}.
         */
        public boolean onRequestFocusInDescendants(int direction,
                Rect previouslyFocusedRect);
        /**
         * See {@link android.view.ViewGroup#requestChildFocus(
         * android.view.View, android.view.View)}.
         */
        public void onRequestChildFocus(View child, View focused);
    }

    public BrowseFrameLayout(Context context) {
        this(context, null, 0);
    }

    public BrowseFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrowseFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private OnFocusSearchListener mListener;
    private OnChildFocusListener mOnChildFocusListener;

    /**
     * Sets a {@link OnFocusSearchListener}.
     */
    public void setOnFocusSearchListener(OnFocusSearchListener listener) {
        mListener = listener;
    }

    /**
     * Returns the {@link OnFocusSearchListener}.
     */
    public OnFocusSearchListener getOnFocusSearchListener() {
        return mListener;
    }

    /**
     * Sets a {@link OnChildFocusListener}.
     */
    public void setOnChildFocusListener(OnChildFocusListener listener) {
        mOnChildFocusListener = listener;
    }

    /**
     * Returns the {@link OnChildFocusListener}.
     */
    public OnChildFocusListener getOnChildFocusListener() {
        return mOnChildFocusListener;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
        if (mOnChildFocusListener != null) {
            return mOnChildFocusListener.onRequestFocusInDescendants(direction,
                    previouslyFocusedRect);
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (mListener != null) {
            View view = mListener.onFocusSearch(focused, direction);
            if (view != null) {
                return view;
            }
        }
        return super.focusSearch(focused, direction);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (mOnChildFocusListener != null) {
            mOnChildFocusListener.onRequestChildFocus(child, focused);
        }
    }
}
