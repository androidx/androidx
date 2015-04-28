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
import android.support.v7.appcompat.R;
import android.support.v7.internal.text.AllCapsTransformationMethod;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A {@link android.widget.TextView} which supports compatible features on older version of the
 * platform.
 * <p>
 * This will automatically be used when you use {@link android.widget.TextView} in your
 * layouts. You should only need to manually use this class when writing custom views.
 */
public class AppCompatTextView extends TextView {

    public AppCompatTextView(Context context) {
        this(context, null);
    }

    public AppCompatTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppCompatTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // First read the TextAppearance style id
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppCompatTextView,
                defStyle, 0);
        final int ap = a.getResourceId(R.styleable.AppCompatTextView_android_textAppearance, -1);
        a.recycle();

        // Now check TextAppearance's textAllCaps value
        if (ap != -1) {
            TypedArray appearance = context.obtainStyledAttributes(ap, R.styleable.TextAppearance);
            if (appearance.hasValue(R.styleable.TextAppearance_textAllCaps)) {
                setAllCaps(appearance.getBoolean(R.styleable.TextAppearance_textAllCaps, false));
            }
            appearance.recycle();
        }

        // Now read the style's value
        a = context.obtainStyledAttributes(attrs, R.styleable.AppCompatTextView, defStyle, 0);
        if (a.hasValue(R.styleable.AppCompatTextView_textAllCaps)) {
            setAllCaps(a.getBoolean(R.styleable.AppCompatTextView_textAllCaps, false));
        }
        a.recycle();
    }

    public void setAllCaps(boolean allCaps) {
        setTransformationMethod(allCaps ? new AllCapsTransformationMethod(getContext()) : null);
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);

        TypedArray appearance = context.obtainStyledAttributes(resId, R.styleable.TextAppearance);
        if (appearance.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            setAllCaps(appearance.getBoolean(R.styleable.TextAppearance_textAllCaps, false));
        }
        appearance.recycle();
    }
}
