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

package androidx.viewpager2.widget.setup;

import static android.view.View.OVER_SCROLL_NEVER;

import android.os.Build;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class TestSetup {
    private final ViewPager2 mViewPager;

    public TestSetup(ViewPager2 viewPager) {
        mViewPager = viewPager;
    }

    /** Test issues workarounds */
    public void applyWorkarounds() {
        // Disabling edge animations on API < 16. Espresso discourages animations altogether, but
        // keeping them for now where they work - as closer to the real environment.
        if (Build.VERSION.SDK_INT < 16) {
            getRecyclerView().setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }

    private RecyclerView getRecyclerView() {
        return (RecyclerView) mViewPager.getChildAt(0);
    }
}
