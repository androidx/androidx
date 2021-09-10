/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.dynamicfeatures.createGraph
import androidx.navigation.get
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class DynamicFragmentNavigatorDestinationBuilderTest {

    @get:Rule
    public val rule: ActivityScenarioRule<TestActivity> = ActivityScenarioRule(
        TestActivity::class.java
    )
    private val fragmentManager get() = rule.withActivity { supportFragmentManager }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    public fun reified() {
        val navHostFragment = DynamicNavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment<TestFragment>(DESTINATION_ID)
        }
        val fragmentDestination = graph[DESTINATION_ID] as DynamicFragmentNavigator.Destination
        assertWithMessage("Fragment class should be set")
            .that(fragmentDestination.className)
            .isEqualTo(TestFragment::class.java.name)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    public fun moduleName() {
        val navHostFragment = DynamicNavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment(DESTINATION_ID, FRAGMENT_CLASS_NAME) {
                moduleName = MODULE_NAME
            }
        }
        val fragmentDestination = graph[DESTINATION_ID] as DynamicFragmentNavigator.Destination
        assertWithMessage("Fragment class should be set")
            .that(fragmentDestination.className)
            .isEqualTo(FRAGMENT_CLASS_NAME)
        assertWithMessage("Module name should be set")
            .that(fragmentDestination.moduleName)
            .isEqualTo(MODULE_NAME)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    public fun no_moduleName() {
        val navHostFragment = DynamicNavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment(DESTINATION_ID, FRAGMENT_CLASS_NAME) {}
        }
        val fragmentDestination = graph[DESTINATION_ID] as DynamicFragmentNavigator.Destination
        assertWithMessage("Fragment class should be set")
            .that(fragmentDestination.className)
            .isEqualTo(FRAGMENT_CLASS_NAME)
        assertWithMessage("Module name should be null")
            .that(fragmentDestination.moduleName)
            .isNull()
    }
}

private const val DESTINATION_ID = 1
private const val MODULE_NAME = "module"
private const val FRAGMENT_CLASS_NAME = "androidx.navigation.dynamicfeatures.fragment.TestFragment"

public class TestActivity : FragmentActivity()
private class TestFragment : Fragment()
