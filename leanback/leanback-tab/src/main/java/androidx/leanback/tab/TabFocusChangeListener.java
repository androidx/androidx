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

import android.view.View;
import android.widget.LinearLayout;

import androidx.viewpager.widget.ViewPager;

class TabFocusChangeListener implements View.OnFocusChangeListener {

    LeanbackTabLayout mLeanbackTabLayout;
    ViewPager mViewPager;

    TabFocusChangeListener(LeanbackTabLayout leanbackTabLayout, ViewPager viewPager) {
        mLeanbackTabLayout = leanbackTabLayout;
        mViewPager = viewPager;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            LinearLayout tabStrip = (LinearLayout) mLeanbackTabLayout.getChildAt(0);
            for (int i = 0; i < tabStrip.getChildCount(); ++i) {
                if (v == tabStrip.getChildAt(i)) {
                    if (mViewPager != null) {
                        mViewPager.setCurrentItem(i, true);
                    }
                }
            }
        }
    }
}
