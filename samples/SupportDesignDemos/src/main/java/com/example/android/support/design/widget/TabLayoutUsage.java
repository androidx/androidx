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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.TextViewCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.android.support.design.Cheeses;
import com.example.android.support.design.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Random;

/**
 * This demonstrates idiomatic usage of TabLayout with a ViewPager
 */
public class TabLayoutUsage extends AppCompatActivity {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private CheesePagerAdapter mPagerAdapter;

    private final Random mRandom = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_tabs_viewpager);

        // Retrieve the Toolbar from our content view, and set it as the action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTabLayout = findViewById(R.id.tabs);
        mViewPager = findViewById(R.id.tabs_viewpager);

        mPagerAdapter = new CheesePagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);

        mTabLayout.setupWithViewPager(mViewPager);

        setupRadioGroup();
    }

    public void addTab(View view) {
        String cheese = Cheeses.sCheeseStrings[mRandom.nextInt(Cheeses.sCheeseStrings.length)];
        mPagerAdapter.addTab(cheese);
    }

    public void selectFirstTab(View view) {
        if (mTabLayout.getTabCount() > 0) {
            mViewPager.setCurrentItem(0);
        }
    }

    public void removeTab(View view) {
        mPagerAdapter.removeTab();
    }

    private void setupRadioGroup() {
        // Setup the initially checked item
        switch (mTabLayout.getTabMode()) {
            case TabLayout.MODE_SCROLLABLE:
                ((RadioButton) findViewById(R.id.rb_tab_scrollable)).setChecked(true);
                break;
            case TabLayout.MODE_FIXED:
                ((RadioButton) findViewById(R.id.rb_tab_fixed)).setChecked(true);
                break;
        }

        RadioGroup rg = findViewById(R.id.radiogroup_tab_mode);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.rb_tab_fixed:
                        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
                        break;
                    case R.id.rb_tab_scrollable:
                        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
                        break;
                }
            }
        });

        // Setup the initially checked item
        switch (mTabLayout.getTabGravity()) {
            case TabLayout.GRAVITY_CENTER:
                ((RadioButton) findViewById(R.id.rb_tab_g_center)).setChecked(true);
                break;
            case TabLayout.GRAVITY_FILL:
                ((RadioButton) findViewById(R.id.rb_tab_g_fill)).setChecked(true);
                break;
        }

        rg = findViewById(R.id.radiogroup_tab_gravity);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.rb_tab_g_center:
                        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
                        break;
                    case R.id.rb_tab_g_fill:
                        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                        break;
                }
            }
        });
    }

    private static class CheesePagerAdapter extends PagerAdapter {
        private final ArrayList<CharSequence> mCheeses = new ArrayList<>();

        public void addTab(String title) {
            mCheeses.add(title);
            notifyDataSetChanged();
        }

        public void removeTab() {
            if (!mCheeses.isEmpty()) {
                mCheeses.remove(mCheeses.size() - 1);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return mCheeses.size();
        }

        @Override
        public int getItemPosition(Object object) {
            final Item item = (Item) object;
            final int index = mCheeses.indexOf(item.cheese);
            return index >= 0 ? index : POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final TextView tv = new TextView(container.getContext());
            tv.setText(getPageTitle(position));
            tv.setGravity(Gravity.CENTER);
            TextViewCompat.setTextAppearance(tv, R.style.TextAppearance_AppCompat_Title);
            container.addView(tv, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            Item item = new Item();
            item.cheese = mCheeses.get(position);
            item.view = tv;
            return item;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            final Item item = (Item) object;
            return item.view == view;
        }

        @Override
         public CharSequence getPageTitle(int position) {
            return mCheeses.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            final Item item = (Item) object;
            container.removeView(item.view);
        }

        private static class Item {
            TextView view;
            CharSequence cheese;
        }
    }

}
