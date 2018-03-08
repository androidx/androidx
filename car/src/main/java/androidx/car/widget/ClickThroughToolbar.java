/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.widget;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.car.R;

/**
 * A toolbar that optionally supports allowing clicks on it to pass through to any underlying views.
 *
 * <p>By default, the {@link Toolbar} eats all touches on it. This view will override
 * {@link #onTouchEvent(MotionEvent)} and return {@code false} if configured to allow pass through.
 */
public class ClickThroughToolbar extends Toolbar {
    private boolean mAllowClickPassThrough;

    public ClickThroughToolbar(Context context) {
        super(context);
    }

    public ClickThroughToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0 /* defStyleAttrs */);
    }

    public ClickThroughToolbar(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        initAttributes(context, attrs, defStyleAttrs);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttrs) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ClickThroughToolbar, defStyleAttrs, 0 /* defStyleRes */);

        mAllowClickPassThrough = a.getBoolean(R.styleable.ClickThroughToolbar_clickThrough, false);

        a.recycle();
    }

    /**
     * Whether or not clicks on this toolbar will pass through to any views that are underneath
     * it. By default, this value is {@code false}.
     *
     * @param allowPassThrough {@code true} if clicks will pass through to an underlying view;
     *                         {@code false} otherwise.
     */
    public void setClickPassThrough(boolean allowPassThrough) {
        mAllowClickPassThrough = allowPassThrough;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mAllowClickPassThrough) {
            return false;
        }

        return super.onTouchEvent(ev);
    }
}
