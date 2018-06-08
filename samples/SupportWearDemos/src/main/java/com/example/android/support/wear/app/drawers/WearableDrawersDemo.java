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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.wear.widget.drawer.WearableActionDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.example.android.support.wear.R;

/** Main {@link Activity} for demoing the Wearable Drawers. */
public class WearableDrawersDemo extends Activity {
    private static final String TAG = "WearableDrawersDemo";

    private WearableNavigationDrawerView mNavDrawer;
    private WearableActionDrawerView mActionDrawer;
    private NavItem[] mNavItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wearable_drawers_demo);
        mNavItems = new NavItem[] {
                new NavItem(
                        ScrollViewFragment.class,
                        "ScrollView",
                        getDrawable(android.R.drawable.star_big_off)),
                new NavItem(
                        FrameLayoutFragment.class,
                        "FrameLayout",
                        getDrawable(android.R.drawable.star_big_on)),
        };

        onNavItemSelected(0);

        mNavDrawer = findViewById(R.id.nav_drawer);
        mNavDrawer.setAdapter(new DemoNavDrawerAdapter(mNavItems));
        mNavDrawer.addOnItemSelectedListener(this::onNavItemSelected);
        mNavDrawer.getController().peekDrawer();

        mActionDrawer = findViewById(R.id.action_drawer);
        mActionDrawer.setOnMenuItemClickListener(this::onActionClicked);
        mActionDrawer.getController().peekDrawer();
    }

    private boolean onActionClicked(MenuItem menuItem) {
        Toast.makeText(this, menuItem.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
        mActionDrawer.getController().peekDrawer();
        return true;
    }

    private void onNavItemSelected(int pos) {
        Fragment fragment;
        try {
            fragment = mNavItems[pos].getFragment().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "Failed to instantiate fragment", e);
            return;
        }

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
