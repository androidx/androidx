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
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class DynamicNavHostFragmentTest {

    @get:Rule
    public val rule: ActivityScenarioRule<NavigationActivity> = ActivityScenarioRule(
        NavigationActivity::class.java
    )

    @Test
    public fun createSplitInstallManager() {
        lateinit var fragment: TestDynamicNavHostFragment
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                fragment = TestDynamicNavHostFragment()
                supportFragmentManager.beginTransaction()
                    .add(R.id.nav_host, fragment, null)
                    .setPrimaryNavigationFragment(fragment)
                    .commitNow()
            }
        }
        assertThat(fragment.createSplitInstallManager())
            .isEqualTo(fragment.createSplitInstallManager())
    }

    @UiThreadTest
    @Test
    public fun create_noArgs() {
        val fragment = DynamicNavHostFragment.create(R.id.nav_host)
        assertThat(fragment.arguments!!.size()).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    public fun create_withArgs() {
        val fragment = DynamicNavHostFragment.create(
            R.id.nav_host,
            Bundle().apply {
                putInt("Test", 1)
            }
        )
        assertThat(fragment.arguments!!.size()).isEqualTo(2)
    }
}

public class NavigationActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.dynamic_activity_layout)
        super.onCreate(savedInstanceState)
    }
}

public class TestDynamicNavHostFragment : DynamicNavHostFragment() {
    public override fun createSplitInstallManager(): SplitInstallManager =
        super.createSplitInstallManager()
}
