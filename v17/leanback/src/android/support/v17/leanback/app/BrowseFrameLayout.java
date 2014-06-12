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
package android.support.v17.leanback.app;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Top level implementation viewgroup for browse to manage transitions between
 * browse sub fragments.
 *
 */
class BrowseFrameLayout extends FrameLayout {

    public interface OnFocusSearchListener {
        public View onFocusSearch(View focused, int direction);
    }

    public interface OnChildFocusListener {
        public boolean onRequestFocusInDescendants(int direction,
                Rect previouslyFocusedRect);
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

    public void setOnFocusSearchListener(OnFocusSearchListener listener) {
        mListener = listener;
    }

    public void setOnChildFocusListener(OnChildFocusListener listener) {
        mOnChildFocusListener = listener;
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
