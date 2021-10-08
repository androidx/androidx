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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.test.R
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ImmediateNavigationTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = ActivityScenarioRule(
        ImmediateNavigationActivity::class.java
    )

    @Test
    fun testNavigateInOnResume() {
        val activity = activityRule.withActivity { this }
        val navController = activity.navController
        val countDownLatch = CountDownLatch(3)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.immediate_test -> {
                    // This should be called twice - once when initially navigating
                    // through immediate_test and once when popping the back stack
                    countDownLatch.countDown()
                }
                R.id.deep_link_test -> {
                    countDownLatch.countDown()
                    // Now pop the back stack to go back to immediate_test
                    // which should be on the back stack
                    navController.popBackStack()
                }
            }
        }
        activityRule.withActivity {
            navController.navigate(R.id.immediate_test)
        }
        countDownLatch.await(1, TimeUnit.SECONDS)
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
    ): View {
        return FrameLayout(requireContext())
    }

    override fun onResume() {
        super.onResume()
        // Only automatically navigate in onResume the first time to avoid getting
        // stuck in a loop
        if (shouldNavigateInOnResume) {
            shouldNavigateInOnResume = false
            NavHostFragment.findNavController(this).navigate(R.id.deep_link_test)
        }
    }
}
