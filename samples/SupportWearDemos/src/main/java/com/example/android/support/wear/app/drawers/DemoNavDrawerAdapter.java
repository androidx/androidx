/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.wear.app.drawers;

import android.graphics.drawable.Drawable;

import androidx.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

/** Simple and declarative {@link WearableNavigationDrawerAdapter}. */
public class DemoNavDrawerAdapter extends WearableNavigationDrawerAdapter {

    private final NavItem[] mNavItems;

    public DemoNavDrawerAdapter(NavItem[] navItems) {
        mNavItems = navItems;
    }

    @Override
    public CharSequence getItemText(int pos) {
        return mNavItems[pos].getTitle();
    }

    @Override
    public Drawable getItemDrawable(int pos) {
        return mNavItems[pos].getDrawable();
    }

    @Override
    public int getCount() {
        return mNavItems.length;
    }
}
