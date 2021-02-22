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
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.IllegalArgumentException

@MediumTest
class FragmentViewModelLazyTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)

    @UiThreadTest
    @Test
    fun vmInitialization() {
        val fragment = TestVMFragment()
        activityRule.activity.supportFragmentManager.commitNow { add(fragment, "tag") }
        assertThat(fragment.vm).isNotNull()
        assertThat(fragment.factoryVM.prop).isEqualTo("fragment")
        assertThat(fragment.daggerPoorCopyVM.prop).isEqualTo("dagger")
        assertThat(fragment.activityVM).isEqualTo(activityRule.activity.vm)
        assertThat(fragment.activityVM2).isEqualTo(activityRule.activity.vm2)
    }

    class TestVMFragment : Fragment() {
        val vm: TestViewModel by viewModels()
        val factoryVM: TestFactorizedViewModel by viewModels { VMFactory("fragment") }
        lateinit var injectedFactory: ViewModelProvider.Factory
        val daggerPoorCopyVM: TestDaggerViewModel by viewModels { injectedFactory }
        val activityVM: TestActivityViewModel by activityViewModels()
        val activityVM2: TestActivityViewModel2 by viewModels({ requireActivity() })
        override fun onCreate(savedInstanceState: Bundle?) {
            injectedFactory = VMFactory("dagger")
            super.onCreate(savedInstanceState)
        }
    }

    class TestActivity : FragmentActivity() {
        val vm: TestActivityViewModel by viewModels()
        val vm2: TestActivityViewModel2 by viewModels()
    }

    class TestViewModel : ViewModel()
    class TestActivityViewModel : ViewModel()
    class TestActivityViewModel2 : ViewModel()
    class TestFactorizedViewModel(val prop: String) : ViewModel()
    class TestDaggerViewModel(val prop: String) : ViewModel()

    private class VMFactory(val prop: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                TestFactorizedViewModel::class.java -> TestFactorizedViewModel(prop)
                TestDaggerViewModel::class.java -> TestDaggerViewModel(prop)
                else -> throw IllegalArgumentException()
            } as T
        }
    }
}