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

package androidx.navigation.fragment.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.compose.test.R
import androidx.navigation.fragment.compose.test.TestActivity
import androidx.navigation.get
import androidx.navigation.plusAssign
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposableFragmentNavigatorTest {

    @get:Rule val testRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun inflateGraph() {
        val navController = NavController(testRule.activity)
        navController.navigatorProvider +=
            FragmentNavigator(
                testRule.activity,
                testRule.activity.supportFragmentManager,
                R.id.fragment_container
            )
        navController.navigatorProvider +=
            ComposableFragmentNavigator(navController.navigatorProvider)

        val navGraph = navController.navInflater.inflate(R.navigation.nav_simple)
        val startDestination = navGraph[R.id.start_fragment] as FragmentNavigator.Destination

        assertWithMessage("Inflated destination should point to ComposableFragment")
            .that(startDestination.className)
            .isEqualTo(ComposableFragment::class.java.name)
        assertWithMessage("Inflated Destination should point to FragmentNavigator")
            .that(startDestination.navigatorName)
            .isEqualTo("fragment")
    }
}

@Suppress("TestFunctionName")
@Composable
fun NavigatorContent() {
    Text("ComposableFragmentNavigator")
}
