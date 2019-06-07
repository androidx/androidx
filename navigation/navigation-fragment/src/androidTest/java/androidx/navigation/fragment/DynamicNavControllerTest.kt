/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.fragment

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DynamicNavControllerTest : BaseNavControllerTest<DynamicNavigationActivity>(
        DynamicNavigationActivity::class.java)

/**
 * Test Navigation Activity that dynamically adds the [NavHostFragment].
 *
 * You must call [NavController.setGraph] to set the appropriate graph for your test.
 */
class DynamicNavigationActivity : BaseNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dynamic_navigation_activity)

        if (savedInstanceState == null) {
            val finalHost = NavHostFragment()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, finalHost)
                    .setPrimaryNavigationFragment(finalHost)
                    .commit()
        }
    }
}
