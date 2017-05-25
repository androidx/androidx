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

package android.support.navigation.app.nav.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.navigation.app.nav.NavController;
import android.support.navigation.app.nav.Navigation;
import android.support.v4.app.FragmentActivity;

import com.android.support.navigation.test.R;

/**
 * Simple Navigation Activity.
 *
 * <p>You must call {@link android.support.navigation.app.nav.NavController#setGraph(int)}
 * to set the appropriate graph for your test.</p>
 */
public class NavigationActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);
    }

    public NavController getNavController() {
        return Navigation.findController(this, R.id.nav_host);
    }
}
