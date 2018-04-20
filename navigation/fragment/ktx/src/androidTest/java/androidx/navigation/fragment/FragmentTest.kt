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

import android.support.test.annotation.UiThreadTest
import android.support.test.filters.SmallTest
import android.support.test.rule.ActivityTestRule
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import androidx.navigation.fragment.ktx.test.R
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@SmallTest
class ActivityTest {
    @get:Rule val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager
    private val contentFragment get() = fragmentManager.findFragmentById(android.R.id.content)

    @UiThreadTest
    @Test fun findNavController() {
        val navHostFragment = NavHostFragment.create(R.navigation.test_graph)
        fragmentManager.beginTransaction()
                .add(android.R.id.content, navHostFragment)
                .commitNow()

        val foundNavController = contentFragment.findNavController()
        assertTrue("Fragment should have NavController set",
                foundNavController == navHostFragment.navController)
    }

    @UiThreadTest
    @Test fun findNavControllerNull() {
        fragmentManager.beginTransaction()
                .add(android.R.id.content, TestFragment())
                .commitNow()
        try {
            contentFragment.findNavController()
            fail("findNavController should throw IllegalStateException if a NavController " +
                    "was not set")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }
}

class TestActivity : FragmentActivity()
class TestFragment : Fragment()
