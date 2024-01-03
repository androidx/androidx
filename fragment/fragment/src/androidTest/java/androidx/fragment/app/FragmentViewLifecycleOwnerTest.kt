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

package androidx.fragment.app

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.app.test.TestViewModel
import androidx.fragment.app.test.ViewModelActivity
import androidx.fragment.test.R
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FragmentViewLifecycleOwnerTest {

    /**
     * Test representing a Non-Hilt case, in which the default factory is not overwritten at the
     * Fragment level.
     */
    @Test
    fun defaultFactoryNotOverwritten() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit()
            executePendingTransactions()

            val defaultFactory1 = (
                fragment.viewLifecycleOwner as HasDefaultViewModelProviderFactory
                ).defaultViewModelProviderFactory
            val defaultFactory2 = (
                fragment.viewLifecycleOwner as HasDefaultViewModelProviderFactory
                ).defaultViewModelProviderFactory

            // Assure that multiple call return the same default factory
            assertThat(defaultFactory1).isSameInstanceAs(defaultFactory2)
            assertThat(defaultFactory1).isNotSameInstanceAs(
                fragment.defaultViewModelProviderFactory
            )
        }
    }

    /**
     * Test representing a Hilt case, in which the default factory is overwritten at the
     * Fragment level.
     */
    @Test
    fun defaultFactoryOverwritten() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment = FragmentWithFactoryOverride()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit()
            executePendingTransactions()

            val defaultFactory = (
                fragment.viewLifecycleOwner as HasDefaultViewModelProviderFactory
                ).defaultViewModelProviderFactory

            assertThat(defaultFactory).isInstanceOf(FakeViewModelProviderFactory::class.java)
        }
    }

    @Test
    fun testCreateViewModelViaExtras() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "fragment")
                .commit()
            executePendingTransactions()

            val viewLifecycleOwner = (fragment.viewLifecycleOwner as FragmentViewLifecycleOwner)

            val creationViewModel = ViewModelProvider(
                viewLifecycleOwner.viewModelStore,
                viewLifecycleOwner.defaultViewModelProviderFactory,
                viewLifecycleOwner.defaultViewModelCreationExtras
            )["test", TestViewModel::class.java]

            recreate()

            val recreatedViewLifecycleOwner = withActivity {
                supportFragmentManager.findFragmentByTag("fragment")?.viewLifecycleOwner
                    as FragmentViewLifecycleOwner
            }

            assertThat(
                ViewModelProvider(recreatedViewLifecycleOwner)["test", TestViewModel::class.java]
            ).isSameInstanceAs(creationViewModel)
        }
    }

    @Test
    fun testCreateViewModelViaExtrasSavedState() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "fragment")
                .commit()
            executePendingTransactions()

            val viewLifecycleOwner = (fragment.viewLifecycleOwner as FragmentViewLifecycleOwner)

            val creationViewModel = ViewModelProvider(
                viewLifecycleOwner.viewModelStore,
                viewLifecycleOwner.defaultViewModelProviderFactory,
                viewLifecycleOwner.defaultViewModelCreationExtras
            )["test", ViewModelActivity.TestSavedStateViewModel::class.java]

            creationViewModel.savedStateHandle["key"] = "value"

            recreate()

            val recreatedViewLifecycleOwner = withActivity {
                supportFragmentManager.findFragmentByTag("fragment")?.viewLifecycleOwner
                    as FragmentViewLifecycleOwner
            }

            val recreateViewModel = ViewModelProvider(recreatedViewLifecycleOwner)[
                "test", ViewModelActivity.TestSavedStateViewModel::class.java
            ]

            assertThat(recreateViewModel).isSameInstanceAs(creationViewModel)

            val value: String? = recreateViewModel.savedStateHandle["key"]
            assertThat(value).isEqualTo("value")
        }
    }

    class FakeViewModelProviderFactory : ViewModelProvider.Factory {
        private var createCalled: Boolean = false
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == TestViewModel::class.java)
            createCalled = true
            @Suppress("UNCHECKED_CAST")
            return TestViewModel() as T
        }
    }

    public class FragmentWithFactoryOverride : StrictViewFragment() {
        public override val defaultViewModelProviderFactory = FakeViewModelProviderFactory()
    }
}
