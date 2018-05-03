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

import android.view.MenuItem;

import com.example.android.support.design.R;
import com.google.android.material.navigation.NavigationView;

/**
 * This demonstrates basic usage of NavigationView
 */
public class NavigationViewWithoutDrawer extends NavigationViewUsageBase {

    @Override
    protected int getLayout() {
        return R.layout.design_navigation_without_drawer;
    }

    @Override
    protected NavigationView.OnNavigationItemSelectedListener getNavigationItemSelectedListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return handleNavigationItemSelected(item);
            }
        };
    }

}
