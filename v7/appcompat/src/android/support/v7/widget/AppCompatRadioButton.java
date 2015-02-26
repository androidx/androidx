/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v7.appcompat.R;
import android.support.v7.internal.widget.TintManager;
import android.support.v7.internal.widget.TintTypedArray;
import android.util.AttributeSet;
import android.widget.RadioButton;

/**
 * A tint aware {@link android.widget.RadioButton}.
 * <p>
 * This will automatically be used when you use {@link android.widget.RadioButton} in your
 * layouts. You should only need to manually use this class when writing custom views.
 */
public class AppCompatRadioButton extends RadioButton {

    private static final int[] TINT_ATTRS = {
            android.R.attr.button
    };

    private TintManager mTintManager;
    private Drawable mButtonDrawable;

    public AppCompatRadioButton(Context context) {
        this(context, null);
    }

    public AppCompatRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.radioButtonStyle);
    }

    public AppCompatRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (TintManager.SHOULD_BE_USED) {
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                    TINT_ATTRS, defStyleAttr, 0);
            setButtonDrawable(a.getDrawable(0));
            a.recycle();

            mTintManager = a.getTintManager();
        }
    }

    @Override
    public void setButtonDrawable(Drawable buttonDrawable) {
        super.setButtonDrawable(buttonDrawable);
        mButtonDrawable = buttonDrawable;
    }

    @Override
    public void setButtonDrawable(@DrawableRes int resid) {
        if (mTintManager != null) {
            setButtonDrawable(mTintManager.getDrawable(resid));
        } else {
            super.setButtonDrawable(resid);
        }
    }

    @Override
    public int getCompoundPaddingLeft() {
        int padding = super.getCompoundPaddingLeft();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Before JB-MR1 the button drawable wasn't taken into account for padding. We'll
            // workaround that here
            if (mButtonDrawable != null) {
                padding += mButtonDrawable.getIntrinsicWidth();
            }
        }
        return padding;
    }
}
