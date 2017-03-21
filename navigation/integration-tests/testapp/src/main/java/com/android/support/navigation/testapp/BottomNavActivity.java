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

package com.android.support.navigation.testapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.navigation.app.nav.NavController;
import android.support.navigation.app.nav.NavDestination;
import android.support.navigation.app.nav.NavHostFragment;
import android.support.navigation.app.nav.Navigation;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * A simple activity demonstrating use of a NavHostFragment with a bottom navigation bar.
 */
public class BottomNavActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bottom_nav_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.my_nav_host_fragment);

        if (host != null) {
            NavController navController = host.getNavController();
            NavHelper.setupActionBar(navController, this);
            BottomNavigationView bottomNavView =
                    (BottomNavigationView) findViewById(R.id.bottom_nav_view);
            NavHelper.setupBottomNavigationView(navController, bottomNavView);
            navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
                @Override
                public void onNavigated(NavController controller, NavDestination destination) {
                    String dest = getResources().getResourceName(destination.getId());
                    Toast.makeText(BottomNavActivity.this, "Navigated to "
                            + dest,
                            Toast.LENGTH_SHORT).show();
                    Log.d("BottomNavActivity", "Navigated to " + dest, new Throwable());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        NavController navController = Navigation.findController(this, R.id.my_nav_host_fragment);
        NavHelper.addChildDestinationsToMenu(navController.getGraph(), menu,
                NavDestination.NAV_TYPE_SECONDARY);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return NavHelper.handleMenuItemSelected(
                Navigation.findController(this, R.id.my_nav_host_fragment), item)
                || super.onOptionsItemSelected(item);
    }
}
