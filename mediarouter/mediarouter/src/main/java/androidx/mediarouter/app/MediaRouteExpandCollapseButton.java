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

package androidx.mediarouter.app;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.mediarouter.R;

/**
 * Chevron/Caret button to expand/collapse group volume list with animation.
 */
class MediaRouteExpandCollapseButton extends AppCompatImageButton {
    private final AnimatedVectorDrawable mExpandAnimatedDrawable;
    private final AnimatedVectorDrawable mCollapseAnimatedDrawable;
    private final String mExpandGroupDescription;
    private final String mCollapseGroupDescription;
    private boolean mIsGroupExpanded;
    private OnClickListener mListener;

    public MediaRouteExpandCollapseButton(Context context) {
        this(context, null);
    }

    public MediaRouteExpandCollapseButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaRouteExpandCollapseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mExpandAnimatedDrawable = (AnimatedVectorDrawable) context.getDrawable(
                R.drawable.mr_group_expand);
        mCollapseAnimatedDrawable = (AnimatedVectorDrawable) context.getDrawable(
                R.drawable.mr_group_collapse);

        ColorFilter filter = new PorterDuffColorFilter(
                MediaRouterThemeHelper.getControllerColor(context, defStyleAttr),
                PorterDuff.Mode.SRC_IN);
        mExpandAnimatedDrawable.setColorFilter(filter);
        mCollapseAnimatedDrawable.setColorFilter(filter);

        mExpandGroupDescription = context.getString(R.string.mr_controller_expand_group);
        mCollapseGroupDescription = context.getString(R.string.mr_controller_collapse_group);

        setImageDrawable(mExpandAnimatedDrawable);
        setContentDescription(mExpandGroupDescription);

        super.setOnClickListener(view -> {
            mIsGroupExpanded = !mIsGroupExpanded;
            if (mIsGroupExpanded) {
                setImageDrawable(mExpandAnimatedDrawable);
                mExpandAnimatedDrawable.start();
                setContentDescription(mCollapseGroupDescription);
            } else {
                setImageDrawable(mCollapseAnimatedDrawable);
                mCollapseAnimatedDrawable.start();
                setContentDescription(mExpandGroupDescription);
            }
            if (mListener != null) {
                mListener.onClick(view);
            }
        });
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }
}
