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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager.widget.ViewPager;

import com.example.android.supportv4.R;

import java.util.ArrayList;

public class ViewPagerActivity extends Activity {
    private ViewPager mPager;
    private PagerTitleStrip mTitles;
    private ColorPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_pager_layout);

        mAdapter = new ColorPagerAdapter();
        mAdapter.add("Red", Color.RED);
        mAdapter.add("Green", Color.GREEN);
        mAdapter.add("Blue", Color.BLUE);

        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mTitles = findViewById(R.id.titles);
    }

    private static class ColorPagerAdapter extends PagerAdapter {
        private ArrayList<Pair<String, Integer>> mEntries = new ArrayList<>();

        public void add(String title, int color) {
            mEntries.add(new Pair<>(title, color));
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View view = new View(container.getContext());
            view.setBackgroundColor(mEntries.get(position).second);

            // Unlike ListView adapters, the ViewPager adapter is responsible
            // for adding the view to the container.
            container.addView(view);

            return new ViewHolder(view, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // The adapter is also responsible for removing the view.
            container.removeView(((ViewHolder) object).view);
        }

        @Override
        public int getItemPosition(Object object) {
            return ((ViewHolder) object).position;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((ViewHolder) object).view == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mEntries.get(position).first;
        }

        private static class ViewHolder {
            final View view;
            final int position;

            public ViewHolder(View view, int position) {
                this.view = view;
                this.position = position;
            }
        }
    }
}
