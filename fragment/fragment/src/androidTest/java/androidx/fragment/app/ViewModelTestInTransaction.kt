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

package androidx.fragment.app

import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.app.test.TestViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewModelTestInTransaction {

    @get:Rule
    var activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    @UiThreadTest
    fun testViewModelInTransactionActivity() {
        val activity = activityRule.activity
        val fragment = TestFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "tag").commitNow()
        val viewModelProvider = ViewModelProvider(activity)
        val viewModel = viewModelProvider.get(TestViewModel::class.java)
        assertThat(viewModel).isSameInstanceAs(fragment.viewModel)
    }

    @Test
    @UiThreadTest
    fun testViewModelInTransactionFragment() {
        val activity = activityRule.activity
        val parent = ParentFragment()
        activity.supportFragmentManager.beginTransaction().add(parent, "parent").commitNow()
        assertThat(parent.executed).isTrue()
    }

    class ParentFragment : Fragment() {

        var executed: Boolean = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val fragment = TestFragment()
            childFragmentManager.beginTransaction().add(fragment, "tag").commitNow()
            val viewModelProvider = ViewModelProvider(this)
            val viewModel = viewModelProvider.get(TestViewModel::class.java)
            assertThat(viewModel).isSameInstanceAs(fragment.viewModel)
            executed = true
        }
    }

    class TestFragment : Fragment() {

        lateinit var viewModel: TestViewModel

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val parentFragment = parentFragment
            val provider = ViewModelProvider(parentFragment ?: requireActivity())
            viewModel = provider.get(TestViewModel::class.java)
            assertThat(viewModel).isNotNull()
        }
    }
}
