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


package com.example.android.supportv4.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.supportv4.R;

import java.lang.Override;
import java.lang.Runnable;

public class ViewPagerActivity extends FragmentActivity {
    private static int[] PAGE_COLORS = { 0xFF700000, 0xFF500020, 0xFF300030, 0xFF200050,
            0xFF000070};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_pager_sample);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return PAGE_COLORS.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return "Page " + position;
            }

            @Override
            public Fragment getItem(int position) {
                Fragment fragment = new DemoObjectFragment();
                Bundle args = new Bundle();
                args.putInt(DemoObjectFragment.ARG_INDEX, position);
                fragment.setArguments(args);
                return fragment;
            }
        });

        final CheckBox smoothScroll = (CheckBox) findViewById(R.id.view_pager_smooth_scroll);

        Button switchTabsButton = (Button) findViewById(R.id.view_pager_switch_tabs_button);
        switchTabsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(2, smoothScroll.isChecked());
                Toast.makeText(view.getContext(), "Current item = " + viewPager.getCurrentItem(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        Button doubleSwitchTabsButton =
                (Button) findViewById(R.id.view_pager_double_switch_tabs_button);
        doubleSwitchTabsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(0, smoothScroll.isChecked());
                viewPager.setCurrentItem(2, smoothScroll.isChecked());
                Toast.makeText(view.getContext(), "Current item = " + viewPager.getCurrentItem(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class DemoObjectFragment extends Fragment {
        public static final String ARG_INDEX = "index";

        @Override
        public View onCreateView(LayoutInflater inflater,
                ViewGroup container, Bundle savedInstanceState) {
            // The last two arguments ensure LayoutParams are inflated
            // properly.
            View rootView = inflater.inflate(R.layout.view_pager_tab, container, false);
            Bundle args = getArguments();
            int position = args.getInt(ARG_INDEX);
            rootView.setBackgroundColor(PAGE_COLORS[position]);
            ((TextView) rootView).setText(Integer.toString(position));
            return rootView;
        }
    }
}
