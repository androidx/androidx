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

package androidx.leanback.tab.app;

import android.os.Bundle;


import androidx.fragment.app.FragmentActivity;
import androidx.leanback.tab.LeanbackViewPager;
import androidx.leanback.tab.test.R;

import com.google.android.material.tabs.TabLayout;

public class TabLayoutTestActivity extends FragmentActivity {

    private static final int NUMBER_OF_TAB = 5;

    public static int getTabCount() {
        return NUMBER_OF_TAB;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablayouttest);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        final LeanbackViewPager viewPager = (LeanbackViewPager) findViewById(R.id.view_pager);

        final PagerAdapter adapter =
                new PagerAdapter(getSupportFragmentManager(), NUMBER_OF_TAB);

        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
    }
}
