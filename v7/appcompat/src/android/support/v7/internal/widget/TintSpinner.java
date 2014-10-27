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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListPopupWindow;
import android.widget.Spinner;

import java.lang.reflect.Field;

/**
 * An tint aware {@link android.widget.Spinner}.
 *
 * @hide
 */
public class TintSpinner extends Spinner {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.popupBackground
    };

    public TintSpinner(Context context) {
        this(context, null);
    }

    public TintSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.spinnerStyle);
    }

    public TintSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS,
                defStyleAttr, 0);
        setBackgroundDrawable(a.getDrawable(0));

        if (a.hasValue(1)) {
            final Drawable background = a.getDrawable(1);
            if (Build.VERSION.SDK_INT >= 16) {
                setPopupBackgroundDrawable(background);
            } else if (Build.VERSION.SDK_INT >= 11) {
                setPopupBackgroundDrawableV11(this, background);
            }
        }

        a.recycle();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setPopupBackgroundDrawableV11(Spinner view, Drawable background) {
        try {
            Field popupField = Spinner.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);

            Object popup = popupField.get(view);

            if (popup instanceof ListPopupWindow) {
                ((ListPopupWindow) popup).setBackgroundDrawable(background);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
