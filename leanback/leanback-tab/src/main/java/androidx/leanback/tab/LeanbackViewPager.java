/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.leanback.tab;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * A viewpager with touch and key event handling disabled by default.
 *
 * <p>Key events handling is disabled by default as with the behaviour of viewpager the fragments
 * can change when DPAD keys are pressed and focus is on the content inside the {@link ViewPager}.
 * This is not desirable for a top navigation bar. The fragments should preferably change only
 * when the focused tab changes.
 */
public class LeanbackViewPager extends ViewPager {

    private boolean mTouchEnabled = false;
    private boolean mEnableKeyEvent = false;

    /**
     * Constructs LeanbackViewPager
     * @param context
     */
    public LeanbackViewPager(@NonNull Context context) {
        super(context);
    }

    /**
     * Constructs LeanbackViewPager
     * @param context
     * @param attrs
     */
    public LeanbackViewPager(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return mTouchEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        return mTouchEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        return mEnableKeyEvent && super.executeKeyEvent(event);
    }

    /**
     * Setter for enabling/disabling touch events
     * @param enableTouch
     */
    public void setTouchEnabled(boolean enableTouch) {
        mTouchEnabled = enableTouch;
    }

    /**
     * Setter for enabling/disabling key events
     * @param enableKeyEvent
     */
    public void setKeyEventsEnabled(boolean enableKeyEvent) {
        mEnableKeyEvent = enableKeyEvent;
    }
}
