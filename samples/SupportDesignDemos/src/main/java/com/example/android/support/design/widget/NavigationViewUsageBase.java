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
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.support.design.R;
import com.google.android.material.navigation.NavigationView;

public abstract class NavigationViewUsageBase extends AppCompatActivity {

    private TextView mTextMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());

        mTextMessage = findViewById(R.id.message);

        // Menu
        NavigationView navigation = findViewById(R.id.navigation);
        navigation.setNavigationItemSelectedListener(getNavigationItemSelectedListener());
        navigation.inflateHeaderView(R.layout.design_navigation_header);
    }

    @LayoutRes
    protected abstract int getLayout();

    protected abstract NavigationView.OnNavigationItemSelectedListener getNavigationItemSelectedListener();

    protected boolean handleNavigationItemSelected(MenuItem item) {
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
            case R.id.navigation_sub_item_3:
                showToast(R.string.navigation_sub_item_3);
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

}
