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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.appcompat.R;
import android.support.v7.internal.text.AllCapsTransformationMethod;
import android.support.v7.internal.widget.ThemeUtils;
import android.util.AttributeSet;
import android.widget.TextView;

class AppCompatTextHelper {

    private static final int[] VIEW_ATTRS = {android.R.attr.textAppearance};
    private static final int[] TEXT_APPEARANCE_ATTRS = {R.attr.textAllCaps};

    private final TextView mView;

    AppCompatTextHelper(TextView view) {
        mView = view;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        Context context = mView.getContext();

        // First read the TextAppearance style id
        TypedArray a = context.obtainStyledAttributes(attrs, VIEW_ATTRS, defStyleAttr, 0);
        final int ap = a.getResourceId(0, -1);
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
        a = context.obtainStyledAttributes(attrs, TEXT_APPEARANCE_ATTRS, defStyleAttr, 0);
        if (a.hasValue(0)) {
            setAllCaps(a.getBoolean(0, false));
        }
        a.recycle();

        final ColorStateList textColors = mView.getTextColors();
        if (textColors != null && !textColors.isStateful()) {
            // If we have a ColorStateList which isn't stateful, create one which includes
            // a disabled state

            final int disabledTextColor;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Pre-Lollipop, we will use textColorSecondary with android:disabledAlpha
                // applied
                disabledTextColor = ThemeUtils.getDisabledThemeAttrColor(context,
                        android.R.attr.textColorSecondary);
            } else {
                // With certain styles on Lollipop, there is a StateListAnimator which sets
                // an alpha on the whole view, so we don't need to apply disabledAlpha to
                // textColorSecondary
                disabledTextColor = ThemeUtils.getThemeAttrColor(context,
                        android.R.attr.textColorSecondary);
            }

            mView.setTextColor(ThemeUtils.createDisabledStateList(
                    textColors.getDefaultColor(), disabledTextColor));
        }
    }

    void onSetTextAppearance(Context context, int resId) {
        TypedArray appearance = context.obtainStyledAttributes(resId, TEXT_APPEARANCE_ATTRS);
        if (appearance.hasValue(0)) {
            setAllCaps(appearance.getBoolean(0, false));
        }
        appearance.recycle();
    }

    void setAllCaps(boolean allCaps) {
        mView.setTransformationMethod(allCaps
                ? new AllCapsTransformationMethod(mView.getContext())
                : null);
    }
}
