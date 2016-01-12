/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v17.internal.widget;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * {@link FrameLayout} subclass that provides an outline only when it has children, so that it does
 * not cast a shadow when empty.
 *
 * @hide
 */
public class OutlineOnlyWithChildrenFrameLayout extends FrameLayout {

    private ViewOutlineProvider mMagicalOutlineProvider;
    private ViewOutlineProvider mInnerOutlineProvider;

    public OutlineOnlyWithChildrenFrameLayout(Context context) {
        super(context);
    }

    public OutlineOnlyWithChildrenFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OutlineOnlyWithChildrenFrameLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OutlineOnlyWithChildrenFrameLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidateOutline();
    }

    @Override
    public void setOutlineProvider(ViewOutlineProvider provider) {
        mInnerOutlineProvider = provider;
        if (mMagicalOutlineProvider == null) {
            // Can't initialize this directly because this method is called from the superclass's
            // constructor.
            mMagicalOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (getChildCount() > 0) {
                        mInnerOutlineProvider.getOutline(view, outline);
                    } else {
                        ViewOutlineProvider.BACKGROUND.getOutline(view, outline);
                    }
                }
            };
        }
        super.setOutlineProvider(mMagicalOutlineProvider);
    }
}
