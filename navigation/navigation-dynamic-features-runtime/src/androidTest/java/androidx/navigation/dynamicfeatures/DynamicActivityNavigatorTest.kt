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

package androidx.navigation.dynamicfeatures

import android.app.Activity
import android.content.Intent
import androidx.navigation.NavigatorProvider
import androidx.navigation.NoOpNavigator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as mockWhen

/* ktlint-enable unused-imports */

@SmallTest
@RunWith(AndroidJUnit4::class)
public class DynamicActivityNavigatorTest {

    private lateinit var navigator: DynamicActivityNavigator
    private lateinit var installManager: DynamicInstallManager
    private lateinit var splitInstallManager: SplitInstallManager
    private lateinit var provider: NavigatorProvider
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var dynamicDestination: DynamicActivityNavigator.Destination

    @Suppress("DEPRECATION")
    @get:Rule
    public val activityRule: ActivityScenarioRule<NavigationActivity> =
        ActivityScenarioRule(NavigationActivity::class.java)

    @Before
    public fun setup() {
        splitInstallManager = mock(SplitInstallManager::class.java)
        activityRule.withActivity {
            installManager = DynamicInstallManager(
                this,
                splitInstallManager
            )
            navigator = DynamicActivityNavigator(this, installManager)
            dynamicDestination = navigator.createDestination()
            dynamicDestination.setIntent(
                Intent(this, DestinationActivity::class.java)
            )
        }
        provider = NavigatorProvider()
        noOpNavigator = NoOpNavigator()
        provider.addNavigator(noOpNavigator)
    }

    @Test
    public fun navigate_DynamicActivityDestination() {
        navigator.navigate(dynamicDestination, null, null, null)
    }

    @Test(expected = IllegalStateException::class)
    public fun navigate_DynamicActivityDestination_NoDynamicNavGraph() {
        lateinit var activity: NavigationActivity
        activityRule.scenario.onActivity {
            activity = it
        }
        @Suppress("UNUSED_VARIABLE")
        val destination = DynamicActivityNavigator.Destination(NavigatorProvider())
        val navDestination = mock(DynamicActivityNavigator.Destination::class.java).apply {
            mockWhen(moduleName).thenReturn("module")
            setIntent(Intent(activity, DestinationActivity::class.java))
        }
        navigator.navigate(navDestination, null, null, null)
    }

    @Test
    public fun createDestination() {
        assertThat(navigator.createDestination()).isNotNull()
    }
}

public class NavigationActivity : Activity()

public class DestinationActivity : Activity()
