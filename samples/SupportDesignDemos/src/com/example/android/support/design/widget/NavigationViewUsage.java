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

import com.example.android.support.design.R;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This demonstrates basic usage of NavigationView
 */
public class NavigationViewUsage extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;

    private TextView mTextMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_navigation);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mTextMessage = (TextView) findViewById(R.id.message);

        // Set the color of status bar
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimaryDark, value, true);
        mDrawerLayout.setStatusBarBackgroundColor(value.data);

        // Retrieve the Toolbar from our content view, and set it as the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Toggle icon
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Menu
        NavigationView navigation = (NavigationView) findViewById(R.id.navigation);
        navigation.setNavigationItemSelectedListener(mNavigationItemSelectedListener);
        navigation.inflateHeaderView(R.layout.design_navigation_header);
    }

    private NavigationView.OnNavigationItemSelectedListener mNavigationItemSelectedListener
            = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            if (handleNavigationItemSelected(item)) {
                mDrawerLayout.closeDrawers();
                return true;
            }
            return false;
        }
    };

    private boolean handleNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_item_1:
                mTextMessage.setText("1");
                return true;
            case R.id.navigation_item_2:
                mTextMessage.setText("2");
                return true;
            case R.id.navigation_item_3:
                mTextMessage.setText("3");
                return true;
            case R.id.navigation_sub_item_1:
                showToast(R.string.navigation_sub_item_1);
                return true;
            case R.id.navigation_sub_item_2:
                showToast(R.string.navigation_sub_item_2);
                return true;
            case R.id.navigation_with_icon:
                showToast(R.string.navigation_item_with_icon);
                return true;
            case R.id.navigation_without_icon:
                showToast(R.string.navigation_item_without_icon);
                return true;
            default:
                return false;
        }
    }

    private void showToast(int res) {
        Toast.makeText(this, getString(R.string.navigation_message, getString(res)),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
