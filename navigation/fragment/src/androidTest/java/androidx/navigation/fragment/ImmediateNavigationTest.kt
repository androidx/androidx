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

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.navigation.fragment.test.R
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ImmediateNavigationTest {

    @get:Rule
    var activityRule = ActivityTestRule(ImmediateNavigationActivity::class.java, false, false)

    @Test
    fun testNavigateInOnResume() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.context,
                ImmediateNavigationActivity::class.java)

        val activity = activityRule.launchActivity(intent)
        instrumentation.waitForIdleSync()
        val navController = activity.navController
        navController.navigate(R.id.immediate_test)
        instrumentation.waitForIdleSync()
        assertEquals(R.id.deep_link_test, navController.currentDestination?.id ?: 0)
        navController.popBackStack()
        instrumentation.waitForIdleSync()
        assertEquals(R.id.immediate_test, navController.currentDestination?.id ?: 0)
    }
}

class ImmediateNavigationActivity : BaseNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.immediate_navigation_activity)
    }
}

class NavigateOnResumeFragment : Fragment() {

    private var shouldNavigateInOnResume = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FrameLayout(requireContext())
    }

    override fun onResume() {
        super.onResume()
        // Only automatically navigate in onResume the first time to avoid getting
        // stuck in a loop
        if (shouldNavigateInOnResume) {
            NavHostFragment.findNavController(this).navigate(R.id.deep_link_test)
            shouldNavigateInOnResume = false
        }
    }
}
