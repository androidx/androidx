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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_ARG = "test"
private const val TEST_ARG_VALUE = "value"

@MediumTest
@RunWith(AndroidJUnit4::class)
class StartDestinationArgsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(StartDestinationArgsActivity::class.java)

    @Test
    fun testNavigateInOnResume() {
        val startArgs = activityRule.activity.startArgs
        assertThat(startArgs).isNotNull()
        assertThat(startArgs?.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }
}

class StartDestinationArgsActivity : FragmentActivity() {

    internal var startArgs: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_destination_args_activity)

        if (savedInstanceState == null) {
            val args = Bundle().apply {
                putString(TEST_ARG, TEST_ARG_VALUE)
            }
            val navHostFragment = NavHostFragment.create(
                R.navigation.nav_fragment_start_args,
                args
            )
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host, navHostFragment)
                .setPrimaryNavigationFragment(navHostFragment)
                .commit()
        }
    }
}

class StartDestinationArgsFragment : Fragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as StartDestinationArgsActivity).startArgs = arguments
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(requireContext())
    }
}
