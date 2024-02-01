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
package com.example.androidx.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBar.Tab;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.androidx.R;

/**
 * This demo shows how various action bar display option flags can be combined and their effects.
 */
public class ActionBarDisplayOptions extends AppCompatActivity
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
        findViewById(R.id.cycle_custom_gravity).setOnClickListener(this);
        findViewById(R.id.toggle_visibility).setOnClickListener(this);

        // Configure several action bar elements that will be toggled by display options.
        mCustomView = getLayoutInflater().inflate(R.layout.action_bar_display_options_custom, null);
        mCustomViewLayoutParams = new ActionBar.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final ActionBar bar = getSupportActionBar();
        bar.setCustomView(mCustomView, mCustomViewLayoutParams);

        bar.setLogo(R.drawable.ic_media_play);
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
        int id = v.getId();
        if (id == R.id.toggle_home_as_up) {
            flags = ActionBar.DISPLAY_HOME_AS_UP;
        } else if (id == R.id.toggle_show_home) {
            flags = ActionBar.DISPLAY_SHOW_HOME;
        } else if (id == R.id.toggle_use_logo) {
            flags = ActionBar.DISPLAY_USE_LOGO;
        } else if (id == R.id.toggle_show_title) {
            flags = ActionBar.DISPLAY_SHOW_TITLE;
        } else if (id == R.id.toggle_show_custom) {
            flags = ActionBar.DISPLAY_SHOW_CUSTOM;
        } else if (id == R.id.cycle_custom_gravity) {
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
            lp.gravity = (lp.gravity & ~Gravity.HORIZONTAL_GRAVITY_MASK) | newGravity;
            bar.setCustomView(mCustomView, lp);
            return;
        } else if (id == R.id.toggle_visibility) {
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
