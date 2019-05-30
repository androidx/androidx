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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.IllegalArgumentException

@MediumTest
class ActivityViewModelLazyTest {
    @get:Rule val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)

    @UiThreadTest
    @Test fun vmInitialization() {
        val activity = activityRule.activity
        assertThat(activity.vm).isNotNull()
        assertThat(activity.factoryVM.prop).isEqualTo("activity")
        assertThat(activity.daggerPoorCopyVM.prop).isEqualTo("dagger")
    }

    class TestActivity : ComponentActivity() {
        val vm: TestViewModel by viewModels()
        val factoryVM: TestFactorizedViewModel by viewModels { VMFactory("activity") }
        lateinit var injectedFactory: ViewModelProvider.Factory
        val daggerPoorCopyVM: TestDaggerViewModel by viewModels { injectedFactory }

        override fun onCreate(savedInstanceState: Bundle?) {
            injectedFactory = VMFactory("dagger")
            super.onCreate(savedInstanceState)
        }
    }

    class TestViewModel : ViewModel()
    class TestFactorizedViewModel(val prop: String) : ViewModel()
    class TestDaggerViewModel(val prop: String) : ViewModel()

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
