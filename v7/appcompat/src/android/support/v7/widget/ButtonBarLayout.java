/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.content.res.ConfigurationHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/**
 * An extension of LinearLayout that automatically switches to vertical
 * orientation when it can't fit its child views horizontally.
 *
 * @hide
 */
public class ButtonBarLayout extends LinearLayout {
    // Whether to allow vertically stacked button bars. This is disabled for
    // configurations with a small (e.g. less than 320dp) screen height. -->
    private static final int ALLOW_STACKING_MIN_HEIGHT_DP = 320;

    /** Whether the current configuration allows stacking. */
    private boolean mAllowStacking;
    private int mLastWidthSize = -1;

    public ButtonBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        final boolean allowStackingDefault =
                ConfigurationHelper.getScreenHeightDp(getResources())
                        >= ALLOW_STACKING_MIN_HEIGHT_DP;
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ButtonBarLayout);
        mAllowStacking = ta.getBoolean(R.styleable.ButtonBarLayout_allowStacking,
                allowStackingDefault);
        ta.recycle();
    }

    public void setAllowStacking(boolean allowStacking) {
        if (mAllowStacking != allowStacking) {
            mAllowStacking = allowStacking;
            if (!mAllowStacking && getOrientation() == LinearLayout.VERTICAL) {
                setStacked(false);
            }
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (mAllowStacking) {
            if (widthSize > mLastWidthSize && isStacked()) {
                // We're being measured wider this time, try un-stacking.
                setStacked(false);
            }
            mLastWidthSize = widthSize;
        }
        boolean needsRemeasure = false;
        // If we're not stacked, make sure the measure spec is AT_MOST rather
        // than EXACTLY. This ensures that we'll still get TOO_SMALL so that we
        // know to stack the buttons.
        final int initialWidthMeasureSpec;
        if (!isStacked() && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            initialWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            // We'll need to remeasure again to fill excess space.
            needsRemeasure = true;
        } else {
            initialWidthMeasureSpec = widthMeasureSpec;
        }
        super.onMeasure(initialWidthMeasureSpec, heightMeasureSpec);
        if (mAllowStacking && !isStacked()) {
            final boolean stack;

            if (Build.VERSION.SDK_INT >= 11) {
                // On API v11+ we can use MEASURED_STATE_MASK and MEASURED_STATE_TOO_SMALL
                final int measuredWidth = ViewCompat.getMeasuredWidthAndState(this);
                final int measuredWidthState = measuredWidth & ViewCompat.MEASURED_STATE_MASK;
                stack = measuredWidthState == ViewCompat.MEASURED_STATE_TOO_SMALL;
            } else {
                // Before that we need to manually total up the children's preferred width.
                // This isn't perfect but works well enough for a workaround.
                int childWidthTotal = 0;
                for (int i = 0, count = getChildCount(); i < count; i++) {
                    childWidthTotal += getChildAt(i).getMeasuredWidth();
                }
                stack = (childWidthTotal + getPaddingLeft() + getPaddingRight()) > widthSize;
            }

            if (stack) {
                setStacked(true);
                // Measure again in the new orientation.
                needsRemeasure = true;
            }
        }
        if (needsRemeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void setStacked(boolean stacked) {
        setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        setGravity(stacked ? Gravity.RIGHT : Gravity.BOTTOM);
        final View spacer = findViewById(R.id.spacer);
        if (spacer != null) {
            spacer.setVisibility(stacked ? View.GONE : View.INVISIBLE);
        }
        // Reverse the child order. This is specific to the Material button
        // bar's layout XML and will probably not generalize.
        final int childCount = getChildCount();
        for (int i = childCount - 2; i >= 0; i--) {
            bringChildToFront(getChildAt(i));
        }
    }

    private boolean isStacked() {
        return getOrientation() == LinearLayout.VERTICAL;
    }
}
