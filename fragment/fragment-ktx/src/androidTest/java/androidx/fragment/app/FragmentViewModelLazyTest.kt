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
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

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
        assertThat(fragment.savedStateViewModel.defaultValue).isEqualTo("value")
        assertThat(fragment.activityVMCE.defaultValue).isEqualTo("value2")
        assertThat(fragment.savedStateViewModelCE.defaultValue).isEqualTo("value3")
    }

    class TestVMFragment : Fragment() {
        val vm: TestViewModel by viewModels()
        val factoryVM: TestFactorizedViewModel by viewModels { VMFactory("fragment") }
        lateinit var injectedFactory: ViewModelProvider.Factory
        val daggerPoorCopyVM: TestDaggerViewModel by viewModels { injectedFactory }
        val activityVM: TestActivityViewModel by activityViewModels()
        val activityVM2: TestActivityViewModel2 by viewModels({ requireActivity() })
        val savedStateViewModel: TestSavedStateViewModel by viewModels({ requireActivity() })
        val activityVMCE: TestActivityViewModelCE by activityViewModels(
            extrasProducer = {
                MutableCreationExtras().apply {
                    set(SAVED_STATE_REGISTRY_OWNER_KEY, requireActivity())
                    set(VIEW_MODEL_STORE_OWNER_KEY, requireActivity())
                    set(DEFAULT_ARGS_KEY, bundleOf("test" to "value2"))
                }
            }
        )
        val savedStateViewModelCE: TestSavedStateViewModelCE by viewModels(
            ownerProducer = { requireActivity() },
            extrasProducer = {
                MutableCreationExtras().apply {
                    set(SAVED_STATE_REGISTRY_OWNER_KEY, requireActivity())
                    set(VIEW_MODEL_STORE_OWNER_KEY, requireActivity())
                    set(DEFAULT_ARGS_KEY, bundleOf("test" to "value3"))
                }
            }
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            injectedFactory = VMFactory("dagger")
            super.onCreate(savedInstanceState)
        }
    }

    class TestActivity : FragmentActivity() {
        val vm: TestActivityViewModel by viewModels()
        val vm2: TestActivityViewModel2 by viewModels()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableSavedStateHandles()
        }

        override val defaultViewModelProviderFactory = SavedStateViewModelFactory()

        override val defaultViewModelCreationExtras: CreationExtras
            get() {
                val extras = MutableCreationExtras()
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = application
                extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                extras[DEFAULT_ARGS_KEY] = bundleOf("test" to "value")
                return extras
            }
    }

    class TestViewModel : ViewModel()
    class TestActivityViewModel : ViewModel()
    class TestActivityViewModel2 : ViewModel()
    class TestFactorizedViewModel(val prop: String) : ViewModel()
    class TestDaggerViewModel(val prop: String) : ViewModel()
    class TestSavedStateViewModel(handle: SavedStateHandle) : ViewModel() {
        val defaultValue = handle.get<String>("test")
    }
    class TestActivityViewModelCE(handle: SavedStateHandle) : ViewModel() {
        val defaultValue = handle.get<String>("test")
    }
    class TestSavedStateViewModelCE(handle: SavedStateHandle) : ViewModel() {
        val defaultValue = handle.get<String>("test")
    }

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
