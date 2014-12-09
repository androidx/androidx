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
import android.widget.MultiAutoCompleteTextView;

/**
 * An tint aware {@link android.widget.MultiAutoCompleteTextView}.
 * <p>
 * This will automatically be used when you use {@link android.widget.MultiAutoCompleteTextView}
 * in your layouts. You should only need to manually use this class when writing custom views.
 */
public class TintMultiAutoCompleteTextView extends MultiAutoCompleteTextView {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.popupBackground
    };

    private TintManager mTintManager;

    public TintMultiAutoCompleteTextView(Context context) {
        this(context, null);
    }

    public TintMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public TintMultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (TintManager.SHOULD_BE_USED) {
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                    TINT_ATTRS, defStyleAttr, 0);
            if (a.hasValue(0)) {
                setBackgroundDrawable(a.getDrawable(0));
            }
            if (a.hasValue(1)) {
                setDropDownBackgroundDrawable(a.getDrawable(1));
            }
            a.recycle();

            mTintManager = a.getTintManager();
        }
    }

    @Override
    public void setDropDownBackgroundResource(int id) {
        if (mTintManager != null) {
            setDropDownBackgroundDrawable(mTintManager.getDrawable(id));
        } else {
            super.setDropDownBackgroundResource(id);
        }
    }

}
