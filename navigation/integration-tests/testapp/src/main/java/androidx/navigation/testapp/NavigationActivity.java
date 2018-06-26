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

package androidx.navigation.testapp;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

/**
 * A simple activity demonstrating use of a NavHostFragment with a navigation drawer.
 */
public class NavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);


        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.my_nav_host_fragment);

        if (host != null) {
            NavController navController = host.getNavController();
            Toolbar toolbar = findViewById(R.id.toolbar);
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            NavigationUI.setupWithNavController(toolbar, navController, drawerLayout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                NavigationUI.setupWithNavController(navigationView, navController);
            } else {
                // The NavigationView already has these same navigation items, so we only add
                // navigation items to the menu here if there isn't a NavigationView
                toolbar.inflateMenu(R.menu.menu_overflow);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return NavigationUI.onNavDestinationSelected(
                                item, Navigation.findNavController(
                                        NavigationActivity.this, R.id.my_nav_host_fragment));
                    }
                });
            }
            BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
            if (bottomNavView != null) {
                NavigationUI.setupWithNavController(bottomNavView, navController);
            }
            navController.addOnNavigatedListener(new NavController.OnNavigatedListener() {
                @Override
                public void onNavigated(@NonNull NavController controller,
                        @NonNull NavDestination destination) {
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
}
