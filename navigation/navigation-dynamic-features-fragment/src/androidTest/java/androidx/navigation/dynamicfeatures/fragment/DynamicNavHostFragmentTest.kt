/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.dynamicfeatures.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.withActivity
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DynamicNavHostFragmentTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(NavigationActivity::class.java)

    @Test
    fun createSplitInstallManager() {
        val fragment = TestDynamicNavHostFragment()
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(R.id.nav_host, fragment, null)
                    .setPrimaryNavigationFragment(fragment)
                    .commitNow()
            }
        }
        assertNotEquals(fragment.createSplitInstallManager(), fragment.createSplitInstallManager())
    }
}

class NavigationActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.dynamic_activity_layout)
        super.onCreate(savedInstanceState)
    }
}

class TestDynamicNavHostFragment : DynamicNavHostFragment() {
    public override fun createSplitInstallManager() = super.createSplitInstallManager()
}
