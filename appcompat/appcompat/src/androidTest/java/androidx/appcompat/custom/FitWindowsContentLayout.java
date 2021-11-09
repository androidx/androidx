/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.appcompat.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class FitWindowsContentLayout extends FrameLayout {

    private final Rect mInsets = new Rect();
    private boolean mFitSystemWindowsCalled = false;

    public FitWindowsContentLayout(Context context) {
        super(context);
    }

    public FitWindowsContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitWindowsContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        mFitSystemWindowsCalled = true;
        mInsets.set(insets);

        return super.fitSystemWindows(insets);
    }

    public boolean getFitsSystemWindowsCalled() {
        return mFitSystemWindowsCalled;
    }

    public Rect getFitSystemWindowsInsets() {
        return mInsets;
    }
}
