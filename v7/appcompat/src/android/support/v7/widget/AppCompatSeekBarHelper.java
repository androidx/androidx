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

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

class AppCompatSeekBarHelper extends AppCompatProgressBarHelper {

    private static final int[] TINT_ATTRS = {
            android.R.attr.thumb
    };

    private final SeekBar mView;

    AppCompatSeekBarHelper(SeekBar view, AppCompatDrawableManager drawableManager) {
        super(view, drawableManager);
        mView = view;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        super.loadFromAttributes(attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                TINT_ATTRS, defStyleAttr, 0);
        Drawable drawable = a.getDrawableIfKnown(0);
        if (drawable != null) {
            mView.setThumb(drawable);
        }
        a.recycle();
    }
}
