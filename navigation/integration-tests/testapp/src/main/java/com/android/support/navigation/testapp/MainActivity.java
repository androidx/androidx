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
import android.support.navigation.app.nav.NavController;
import android.support.navigation.app.nav.NavDestination;
import android.support.navigation.app.nav.NavHostFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * A simple activity demonstrating use of a NavHostFragment.
 */
public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.my_nav_host_fragment);


        if (host != null) {
            host.getNavController().addOnNavigatedListener(new NavController.OnNavigatedListener() {
                @Override
                public void onNavigated(NavController controller, NavDestination destination) {
                    String dest = getResources().getResourceName(destination.getId());
                    Toast.makeText(MainActivity.this, "Navigated to "
                            + dest,
                            Toast.LENGTH_SHORT).show();
                    Log.d("adamp", "Navigated to " + dest, new Throwable());
                }
            });
        }
    }
}
