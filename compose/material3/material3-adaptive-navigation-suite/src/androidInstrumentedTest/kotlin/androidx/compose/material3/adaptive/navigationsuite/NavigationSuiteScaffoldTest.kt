/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertIsNotEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigationSuiteScaffoldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun navigationSuiteScaffoldTest_fillMaxSize_withNavBar_succeeds() {
        rule.setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleNavigationSuiteScaffoldLayout(NavigationSuiteType.NavigationBar)
            }
        }

        // Assert that Modifier.fillMaxSize didn't propagate to the nav bar (its height should not
        // fill the screen).
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .height.assertIsNotEqualTo(rule.onRoot().getUnclippedBoundsInRoot().height)
        // Nav bar width is always the same as screen width.
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .width.assertIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }

    @Test
    fun navigationSuiteScaffoldTest_fillMaxSize_withNavRail_succeeds() {
        rule.setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleNavigationSuiteScaffoldLayout(NavigationSuiteType.NavigationRail)
            }
        }

        // Nav rail height is always the same as screen height.
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .height.assertIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().height)
        // Assert that Modifier.fillMaxSize didn't propagate to the nav rail (its width should not
        // fill the screen).
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .width.assertIsNotEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }

    @Test
    fun navigationSuiteScaffoldTest_fillMaxSize_withNavDrawer_succeeds() {
        rule.setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleNavigationSuiteScaffoldLayout(NavigationSuiteType.NavigationDrawer)
            }
        }

        // Nav drawer height is always the same as screen height.
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .height.assertIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().height)
        // Assert that Modifier.fillMaxSize didn't propagate to the nav drawer (its width should not
        // fill the screen).
        rule.onNodeWithTag(NavigationSuiteTag).getUnclippedBoundsInRoot()
            .width.assertIsNotEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }
}

@Composable
private fun SampleNavigationSuiteScaffoldLayout(
    layoutType: NavigationSuiteType
) {
    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            NavigationSuite(
                modifier = Modifier.testTag(NavigationSuiteTag),
                layoutType = layoutType
            ) { }
        }
    )
}

private const val NavigationSuiteTag = "NavigationSuite"
