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

package android.support.v7.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * An tint aware {@link android.widget.Button}.
 * <p>
 * This will automatically be used when you use {@link android.widget.Button} in your layouts. You
 * should only need to manually use this class when writing custom views.
 */
public class TintButton extends Button {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.textAppearance
    };

    private TintManager mTintManager;

    public TintButton(Context context) {
        this(context, null);
    }

    public TintButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public TintButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (TintManager.SHOULD_BE_USED) {
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                    TINT_ATTRS, defStyleAttr, 0);
            if (a.hasValue(0)) {
                setBackgroundDrawable(a.getDrawable(0));
            }

            // Keep the TintManager in case we need it later
            mTintManager = a.getTintManager();
        }
    }

    @Override
    public void setBackgroundResource(int resid) {
        if (mTintManager != null) {
            setBackgroundDrawable(mTintManager.getDrawable(resid));
        } else {
            super.setBackgroundResource(resid);
        }
    }
}
