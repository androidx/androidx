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

package androidx.activity

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.IllegalArgumentException

@MediumTest
class ActivityViewModelLazyTest {
    @Suppress("DEPRECATION")
    @get:Rule val activityRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    @UiThreadTest
    @Test fun vmInitialization() {
        val activity = activityRule.activity
        assertThat(activity.vm).isNotNull()
        assertThat(activity.factoryVM.prop).isEqualTo("activity")
        assertThat(activity.daggerPoorCopyVM.prop).isEqualTo("dagger")
        assertThat(activity.savedStateViewModel.defaultValue).isEqualTo("value")
    }

    class TestActivity : ComponentActivity() {
        val vm: TestViewModel by viewModels()
        val factoryVM: TestFactorizedViewModel by viewModels { VMFactory("activity") }
        lateinit var injectedFactory: ViewModelProvider.Factory
        val daggerPoorCopyVM: TestDaggerViewModel by viewModels { injectedFactory }
        val savedStateViewModel: TestSavedStateViewModel by viewModels(
            extrasProducer = { defaultViewModelCreationExtras }
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            injectedFactory = VMFactory("dagger")
            super.onCreate(savedInstanceState)
        }

        override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
            return SavedStateViewModelFactory()
        }

        override fun getDefaultViewModelCreationExtras(): CreationExtras {
            val extras = MutableCreationExtras(super.getDefaultViewModelCreationExtras())
            extras[DEFAULT_ARGS_KEY] = bundleOf("test" to "value")
            return extras
        }
    }

    class TestViewModel : ViewModel()
    class TestFactorizedViewModel(val prop: String) : ViewModel()
    class TestDaggerViewModel(val prop: String) : ViewModel()
    class TestSavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel() {
        val defaultValue = savedStateHandle.get<String>("test")
    }

    private class VMFactory(val prop: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass == TestFactorizedViewModel::class.java -> TestFactorizedViewModel(prop)
                modelClass == TestDaggerViewModel::class.java -> TestDaggerViewModel(prop)
                else -> throw IllegalArgumentException()
            } as T
        }
    }
}
