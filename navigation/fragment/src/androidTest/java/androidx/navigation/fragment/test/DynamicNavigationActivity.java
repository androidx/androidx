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

package androidx.navigation.fragment.test;

import android.os.Bundle;
import android.support.annotation.Nullable;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

/**
 * Test Navigation Activity that dynamically adds the {@link NavHostFragment}.
 *
 * <p>You must call {@link NavController#setGraph(int)}
 * to set the appropriate graph for your test.</p>
 */
public class DynamicNavigationActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dynamic_navigation_activity);

        if (savedInstanceState == null) {
            final NavHostFragment finalHost = new NavHostFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host, finalHost)
                    .setPrimaryNavigationFragment(finalHost)
                    .commit();
        }
    }
}
