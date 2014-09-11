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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.widget.PopupWindow;

/**
 * @hide
 */
public class AppCompatPopupWindow extends PopupWindow {

    private final boolean mOverlapAnchor;

    public AppCompatPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.PopupWindow, defStyleAttr, 0);
        mOverlapAnchor = a.getBoolean(R.styleable.PopupWindow_overlapAnchor, false);
        // We re-set this for tinting purposes
        setBackgroundDrawable(a.getDrawable(R.styleable.PopupWindow_android_popupBackground));
        a.recycle();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        if (Build.VERSION.SDK_INT < 21 && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        if (Build.VERSION.SDK_INT < 21 && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff, gravity);
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        if (Build.VERSION.SDK_INT < 21 && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.update(anchor, xoff, yoff, width, height);
    }
}
