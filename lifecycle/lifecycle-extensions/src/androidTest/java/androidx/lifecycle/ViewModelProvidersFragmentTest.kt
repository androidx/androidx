/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewModelProvidersFragmentTest {
    @Test
    fun testViewModelProvidersActivity() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            val activityModel = withActivity {
                ViewModelProviders.of(this).get(TestViewModel::class.java)
            }
            assertThat(activityModel).isNotNull()
        }
    }

    @Test
    fun testViewModelProvidersFragment() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            val fragmentModel = withActivity {
                val fragment = Fragment()
                supportFragmentManager.beginTransaction()
                    .add(fragment, "tag")
                    .commitNow()
                ViewModelProviders.of(fragment).get(TestViewModel::class.java)
            }
            assertThat(fragmentModel).isNotNull()
        }
    }

    @Test
    fun testViewModelProvidersWithCustomFactoryActivity() {
        val factory = CountingFactory()
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            val activityModel = withActivity {
                ViewModelProviders.of(this, factory).get(TestViewModel::class.java)
            }
            assertThat(activityModel).isNotNull()
            assertThat(factory.count).isEqualTo(1)
        }
    }

    @Test
    fun testViewModelProvidersWithCustomFactoryFragment() {
        val factory = CountingFactory()
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            val fragmentModel = withActivity {
                val fragment = Fragment()
                supportFragmentManager.beginTransaction()
                    .add(fragment, "tag")
                    .commitNow()
                ViewModelProviders.of(fragment, factory).get(TestViewModel::class.java)
            }
            assertThat(fragmentModel).isNotNull()
            assertThat(factory.count).isEqualTo(1)
        }
    }

    class CountingFactory : ViewModelProvider.NewInstanceFactory() {
        var count = 0

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            count++
            return super.create(modelClass)
        }
    }

    class TestViewModel : ViewModel()
}
