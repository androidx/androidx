/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModel
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(Parameterized::class)
public class ViewModelInFragmentTest(private val viewModelClass: Class<out ViewModel>) {

    public companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun initParameters(): Array<Class<out ViewModel>> = viewModelClasses
    }

    @Suppress("DEPRECATION")
    @get:Rule
    public val activityTestRule: androidx.test.rule.ActivityTestRule<FragmentActivity> =
        androidx.test.rule.ActivityTestRule(FragmentActivity::class.java)
    private lateinit var activity: FragmentActivity

    @Before
    public fun setup() {
        activity = activityTestRule.activity
    }

    @FlakyTest(bugId = 190608770)
    @Test
    public fun viewModelCreatedInFragment() {
        val fragment = TestFragment(viewModelClass)

        activityTestRule.runOnUiThread {
            val view = FragmentContainerView(activity)
            view.id = 100
            activity.setContentView(view)
            activity.supportFragmentManager.beginTransaction()
                .replace(100, fragment)
                .commit()
        }

        assertTrue(fragment.latch.await(1, TimeUnit.SECONDS))
    }
}

public class TestFragment(private val viewModelClass: Class<out ViewModel>) : Fragment() {

    public val latch: CountDownLatch = CountDownLatch(1)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView = ComposeView(requireContext()).apply {
        setContent {
            viewModel(viewModelClass)
            latch.countDown()
        }
    }
}
