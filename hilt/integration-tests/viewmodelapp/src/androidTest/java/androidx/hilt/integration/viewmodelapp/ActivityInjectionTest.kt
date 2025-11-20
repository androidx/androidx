/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.integration.viewmodelapp

import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
// TODO: Find out why random ClassNotFoundException is thrown in APIs lower than 21.
class ActivityInjectionTest {

    @get:Rule val rule = HiltAndroidRule(this)

    @Test
    fun verifyInjection() {
        ActivityScenario.launch(TestActivity::class.java).use {
            it.onActivity { activity ->
                assertThat(activity.myAndroidViewModel).isNotNull()
                assertThat(activity.myViewModel).isNotNull()
                assertThat(activity.myInjectedViewModel).isNotNull()
                assertThat(activity.myNestedInjectedViewModel).isNotNull()
                assertThat(activity.myAssistedInjectedViewModel).isNotNull()
                assertThat(activity.myAssistedInjectedViewModel.bar).isEqualTo(42)
            }
        }
    }

    @AndroidEntryPoint
    class TestActivity : FragmentActivity() {
        val myAndroidViewModel by viewModels<MyAndroidViewModel>()
        val myViewModel by viewModels<MyViewModel>()
        val myInjectedViewModel by viewModels<MyInjectedViewModel>()
        val myNestedInjectedViewModel by viewModels<TopClass.MyNestedInjectedViewModel>()
        val myAssistedInjectedViewModel by
            viewModels<MyAssistedInjectedViewModel>(
                extrasProducer = {
                    defaultViewModelCreationExtras.withCreationCallback<
                        MyAssistedInjectedViewModel.Factory
                    > { factory ->
                        factory.create(42)
                    }
                }
            )
    }
}
