/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.example.android.supportv7.app;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.example.android.supportv7.R;

/**
 * This demo shows how various action bar display option flags can be combined and their effects.
 */
public class ActionBarDisplayOptions extends ActionBarActivity
        implements View.OnClickListener, ActionBar.TabListener {
    private View mCustomView;
    private ActionBar.LayoutParams mCustomViewLayoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_bar_display_options);

        findViewById(R.id.toggle_home_as_up).setOnClickListener(this);
        findViewById(R.id.toggle_show_home).setOnClickListener(this);
        findViewById(R.id.toggle_use_logo).setOnClickListener(this);
        findViewById(R.id.toggle_show_title).setOnClickListener(this);
        findViewById(R.id.toggle_show_custom).setOnClickListener(this);
        findViewById(R.id.toggle_navigation).setOnClickListener(this);
        findViewById(R.id.cycle_custom_gravity).setOnClickListener(this);
        findViewById(R.id.toggle_visibility).setOnClickListener(this);

        // Configure several action bar elements that will be toggled by display options.
        mCustomView = getLayoutInflater().inflate(R.layout.action_bar_display_options_custom, null);
        mCustomViewLayoutParams = new ActionBar.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final ActionBar bar = getSupportActionBar();
        bar.setCustomView(mCustomView, mCustomViewLayoutParams);
        bar.addTab(bar.newTab().setText("Tab 1").setTabListener(this));
        bar.addTab(bar.newTab().setText("Tab 2").setTabListener(this));
        bar.addTab(bar.newTab().setText("Tab 3").setTabListener(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.display_options_actions, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        final ActionBar bar = getSupportActionBar();
        int flags = 0;
        switch (v.getId()) {
            case R.id.toggle_home_as_up:
                flags = ActionBar.DISPLAY_HOME_AS_UP;
                break;
            case R.id.toggle_show_home:
                flags = ActionBar.DISPLAY_SHOW_HOME;
                break;
            case R.id.toggle_use_logo:
                flags = ActionBar.DISPLAY_USE_LOGO;
                break;
            case R.id.toggle_show_title:
                flags = ActionBar.DISPLAY_SHOW_TITLE;
                break;
            case R.id.toggle_show_custom:
                flags = ActionBar.DISPLAY_SHOW_CUSTOM;
                break;
            case R.id.toggle_navigation:
                bar.setNavigationMode(
                        bar.getNavigationMode() == ActionBar.NAVIGATION_MODE_STANDARD
                                ? ActionBar.NAVIGATION_MODE_TABS
                                : ActionBar.NAVIGATION_MODE_STANDARD);
                return;
            case R.id.cycle_custom_gravity: {
                ActionBar.LayoutParams lp = mCustomViewLayoutParams;
                int newGravity = 0;
                switch (lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.LEFT:
                        newGravity = Gravity.CENTER_HORIZONTAL;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        newGravity = Gravity.RIGHT;
                        break;
                    case Gravity.RIGHT:
                        newGravity = Gravity.LEFT;
                        break;
                }
                lp.gravity = lp.gravity & ~Gravity.HORIZONTAL_GRAVITY_MASK | newGravity;
                bar.setCustomView(mCustomView, lp);
                return;
            }
            case R.id.toggle_visibility:
                if (bar.isShowing()) {
                    bar.hide();
                } else {
                    bar.show();
                }
                return;
        }

        int change = bar.getDisplayOptions() ^ flags;
        bar.setDisplayOptions(change, flags);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }
}
