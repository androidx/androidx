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

import androidx.fragment.app.Fragment
import androidx.navigation.contains
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestNavigatorDestinationBuilderTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test fun fragment() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment<BuilderTestFragment>(DESTINATION_ID)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("Fragment class should be set to BuilderTestFragment")
            .that((graph[DESTINATION_ID] as FragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestFragment::class.java.name)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test fun fragmentWithBody() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            fragment<BuilderTestFragment>(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("Fragment class should be set to BuilderTestFragment")
            .that((graph[DESTINATION_ID] as FragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestFragment::class.java.name)
        assertWithMessage("Fragment should have label set")
            .that(graph[DESTINATION_ID].label)
            .isEqualTo(LABEL)
    }

    @UiThreadTest
    @Test fun fragmentRoute() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ROUTE) {
            fragment<BuilderTestFragment>(DESTINATION_ROUTE)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("Fragment class should be set to BuilderTestFragment")
            .that((graph[DESTINATION_ROUTE] as FragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestFragment::class.java.name)
    }

    @UiThreadTest
    @Test fun fragmentWithBodyRoute() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
            .add(android.R.id.content, navHostFragment)
            .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ROUTE) {
            fragment<BuilderTestFragment>(DESTINATION_ROUTE) {
                label = LABEL
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("Fragment class should be set to BuilderTestFragment")
            .that((graph[DESTINATION_ROUTE] as FragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestFragment::class.java.name)
        assertWithMessage("Fragment should have label set")
            .that(graph[DESTINATION_ROUTE].label)
            .isEqualTo(LABEL)
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "destination"
private const val LABEL = "Test"
class BuilderTestFragment : Fragment()
