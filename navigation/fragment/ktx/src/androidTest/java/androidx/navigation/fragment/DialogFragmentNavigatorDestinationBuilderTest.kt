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

package androidx.navigation.fragment

import androidx.fragment.app.DialogFragment
import androidx.navigation.contains
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DialogFragmentNavigatorDestinationBuilderTest {
    @get:Rule
    val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager

    @UiThreadTest
    @Test fun fragment() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
                .add(android.R.id.content, navHostFragment)
                .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            dialog<BuilderTestDialogFragment>(DESTINATION_ID)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("DialogFragment class should be set to BuilderTestDialogFragment")
            .that((graph[DESTINATION_ID] as DialogFragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestDialogFragment::class.java.name)
    }

    @UiThreadTest
    @Test fun fragmentWithBody() {
        val navHostFragment = NavHostFragment()
        fragmentManager.beginTransaction()
                .add(android.R.id.content, navHostFragment)
                .commitNow()
        val graph = navHostFragment.createGraph(startDestination = DESTINATION_ID) {
            dialog<BuilderTestDialogFragment>(DESTINATION_ID) {
                label = LABEL
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("DialogFragment class should be set to BuilderTestDialogFragment")
            .that((graph[DESTINATION_ID] as DialogFragmentNavigator.Destination).className)
            .isEqualTo(BuilderTestDialogFragment::class.java.name)
        assertWithMessage("DialogFragment should have label set")
            .that(graph[DESTINATION_ID].label)
            .isEqualTo(LABEL)
    }
}

private const val DESTINATION_ID = 1
private const val LABEL = "Test"
class BuilderTestDialogFragment : DialogFragment()
