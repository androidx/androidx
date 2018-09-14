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

package androidx.car.moderator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A wrapping view that will monitor all touch events on its children views and prevent the user
 * from interacting if they have performed a preset number of interactions within a preset amount
 * of time.
 *
 * <p>When the user has performed the maximum number of interactions per the set unit of time, a
 * message explaining that they are no longer able to interact with the view is also displayed.
 */
public class SpeedBumpView extends FrameLayout {
    private final SpeedBumpController mSpeedBumpController;

    public SpeedBumpView(Context context) {
        super(context);
    }

    public SpeedBumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpeedBumpView(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
    }

    public SpeedBumpView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);
    }

    {
        mSpeedBumpController = new SpeedBumpController(this);
        addView(mSpeedBumpController.getLockoutMessageView());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSpeedBumpController.start();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSpeedBumpController.stop();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        // Always ensure that the lock out view has the highest Z-index so that it will show
        // above all other views.
        mSpeedBumpController.getLockoutMessageView().bringToFront();
    }


    // Overriding dispatchTouchEvent to intercept all touch events on child views.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mSpeedBumpController.onTouchEvent(ev) && super.dispatchTouchEvent(ev);
    }
}
