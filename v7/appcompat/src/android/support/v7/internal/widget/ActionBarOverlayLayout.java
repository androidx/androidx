/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.app.ActionBar;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Special layout for the containing of an overlay action bar (and its content) to correctly handle
 * fitting system windows when the content has request that its layout ignore them.
 *
 * @hide
 */
public class ActionBarOverlayLayout extends FrameLayout {

    private int mActionBarHeight;
    private ActionBar mActionBar;
    private View mContent;
    private View mActionBarTop;
    private ActionBarContainer mContainerView;
    private ActionBarView mActionView;
    private View mActionBarBottom;
    private final Rect mZeroRect = new Rect(0, 0, 0, 0);

    static final int[] mActionBarSizeAttr = new int[]{
            R.attr.actionBarSize
    };

    public ActionBarOverlayLayout(Context context) {
        super(context);
        init(context);
    }

    public ActionBarOverlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(mActionBarSizeAttr);
        mActionBarHeight = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
    }

    public void setActionBar(ActionBar impl) {
        mActionBar = impl;
    }

    private boolean applyInsets(View view, Rect insets, boolean left, boolean top,
            boolean bottom, boolean right) {
        boolean changed = false;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (left && lp.leftMargin != insets.left) {
            changed = true;
            lp.leftMargin = insets.left;
        }
        if (top && lp.topMargin != insets.top) {
            changed = true;
            lp.topMargin = insets.top;
        }
        if (right && lp.rightMargin != insets.right) {
            changed = true;
            lp.rightMargin = insets.right;
        }
        if (bottom && lp.bottomMargin != insets.bottom) {
            changed = true;
            lp.bottomMargin = insets.bottom;
        }
        return changed;
    }

    void pullChildren() {
        if (mContent == null) {
            mContent = findViewById(R.id.action_bar_activity_content);
            if (mContent == null) {
                mContent = findViewById(android.R.id.content);
            }
            mActionBarTop = findViewById(R.id.top_action_bar);
            mContainerView = (ActionBarContainer) findViewById(R.id.action_bar_container);
            mActionView = (ActionBarView) findViewById(R.id.action_bar);
            mActionBarBottom = findViewById(R.id.split_action_bar);
        }
    }
}
