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

package com.example.android.support.percent;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SupportPercentDemos extends Activity {
    private static final String LAYOUT_RESOURCE_ID = "layout_resource_id";

    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.support_percent_demos);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new Adapter(getFragmentManager()));
    }

    public static class Adapter extends FragmentStatePagerAdapter {
        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            PercentFragment fragment = new PercentFragment();
            Bundle args = new Bundle();
            switch (position) {
                case 0:
                    args.putInt(LAYOUT_RESOURCE_ID, R.layout.demo_1);
                    break;
                case 1:
                    args.putInt(LAYOUT_RESOURCE_ID, R.layout.demo_2);
                default:
                    break;
            }
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public static class PercentFragment extends Fragment {
        public PercentFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Bundle args = getArguments();
            int layout = args.getInt(LAYOUT_RESOURCE_ID);
            return inflater.inflate(layout, container, false);
        }
    }
}
