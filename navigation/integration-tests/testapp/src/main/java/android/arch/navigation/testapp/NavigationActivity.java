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

package android.arch.navigation.testapp;

import android.arch.navigation.NavController;
import android.arch.navigation.NavDestination;
import android.arch.navigation.NavHostFragment;
import android.arch.navigation.Navigation;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * A simple activity demonstrating use of a NavHostFragment with a navigation drawer.
 */
public class NavigationActivity extends AppCompatActivity {
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.my_nav_host_fragment);

        if (host != null) {
            NavController navController = host.getNavController();
            mDrawerLayout = findViewById(R.id.drawer_layout);
            NavHelper.setupActionBar(navController, this, mDrawerLayout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            NavHelper.setupNavigationView(navController, navigationView);
            BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
            NavHelper.setupBottomNavigationView(navController, bottomNavView);
            navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
                @Override
                public void onNavigated(NavController controller, NavDestination destination) {
                    String dest;
                    try {
                        dest = getResources().getResourceName(destination.getId());
                    } catch (Resources.NotFoundException e) {
                        dest = Integer.toString(destination.getId());
                    }
                    Toast.makeText(NavigationActivity.this, "Navigated to "
                            + dest,
                            Toast.LENGTH_SHORT).show();
                    Log.d("NavigationActivity", "Navigated to " + dest);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean retValue = super.onCreateOptionsMenu(menu);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // The NavigationView already has these same navigation items, so we only add
        // navigation items to the menu here if there isn't a NavigationView
        if (navigationView == null) {
            getMenuInflater().inflate(R.menu.menu_overflow, menu);
            return true;
        }
        return retValue;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return NavHelper.handleMenuItemSelected(
                Navigation.findNavController(this, R.id.my_nav_host_fragment), item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavHelper.handleNavigateUp(
                Navigation.findNavController(this, R.id.my_nav_host_fragment),
                mDrawerLayout);
    }
}
