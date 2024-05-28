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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.fragment.app.commitNow
import androidx.navigation.fragment.compose.ComposableFragment.Companion.ComposableFragment
import androidx.navigation.fragment.compose.test.R
import androidx.navigation.fragment.compose.test.TestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposableFragmentTest {

    @get:Rule val testRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun showContent() {
        val composableFragment =
            ComposableFragment(
                "androidx.navigation.fragment.compose.ComposableFragmentTestKt\$Content"
            )
        val fragmentManager = testRule.activity.supportFragmentManager
        testRule.runOnUiThread {
            fragmentManager.commitNow { add(R.id.fragment_container, composableFragment) }
        }

        testRule.waitForIdle()

        testRule.onNodeWithText("ComposableFragment").assertIsDisplayed()
    }

    @Test
    fun showContentWithArgs() {
        val composableFragment =
            ComposableFragment(
                    "androidx.navigation.fragment.compose.ComposableFragmentTestKt\$ContentWithArgs"
                )
                .apply { requireArguments().putString("test", "argument") }
        val fragmentManager = testRule.activity.supportFragmentManager
        testRule.runOnUiThread {
            fragmentManager.commitNow { add(R.id.fragment_container, composableFragment) }
        }

        testRule.waitForIdle()

        testRule.onNodeWithText("ComposableFragment: argument").assertIsDisplayed()
    }
}

@Suppress("TestFunctionName")
@Composable
fun Content() {
    Text("ComposableFragment")
}

@Suppress("TestFunctionName")
@Composable
fun ContentWithArgs() {
    val args = LocalFragment.current.requireArguments()
    val testArgument = args.getString("test")
    Text("ComposableFragment: $testArgument")
}
