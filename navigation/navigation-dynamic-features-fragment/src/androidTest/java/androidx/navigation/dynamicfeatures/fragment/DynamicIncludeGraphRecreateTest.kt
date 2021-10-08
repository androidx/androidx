/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures.fragment

import androidx.navigation.NavController
import androidx.navigation.dynamicfeatures.fragment.test.R
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DynamicIncludeGraphRecreateTest {

    @get:Rule
    public val rule: ActivityScenarioRule<NavigationActivity> = ActivityScenarioRule(
        NavigationActivity::class.java
    )

    @Test
    public fun recreateTest() {
        lateinit var fragment: TestDynamicNavHostFragment
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                fragment = TestDynamicNavHostFragment()
                supportFragmentManager.beginTransaction()
                    .add(R.id.nav_host, fragment, null)
                    .setPrimaryNavigationFragment(fragment)
                    .commitNow()
            }

            val navController = fragment.findNavController()
            withActivity {
                navController.setGraph(R.navigation.include_dynamic_nav_graph)
            }

            recreate()

            lateinit var restoredController: NavController

            withActivity {
                val restoredFragment = supportFragmentManager.findFragmentById(R.id.nav_host)
                restoredController = restoredFragment!!.findNavController()
                restoredController.setGraph(R.navigation.include_dynamic_nav_graph)
            }
        }
    }
}
