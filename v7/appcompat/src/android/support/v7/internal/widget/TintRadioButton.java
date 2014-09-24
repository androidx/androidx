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
import android.widget.CheckBox;
import android.widget.RadioButton;

/**
 * An tint aware {@link android.widget.RadioButton}.
 *
 * @hide
 */
public class TintRadioButton extends RadioButton {

    private static final int[] TINT_ATTRS = {
            android.R.attr.button
    };

    private final TintManager mTintManager;

    public TintRadioButton(Context context) {
        this(context, null);
    }

    public TintRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.radioButtonStyle);
    }

    public TintRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS,
                defStyleAttr, 0);
        setButtonDrawable(a.getDrawable(0));
        a.recycle();

        mTintManager = a.getTintManager();
    }

    @Override
    public void setButtonDrawable(int resid) {
        setButtonDrawable(mTintManager.getDrawable(resid));
    }
}
