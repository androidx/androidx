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

import androidx.fragment.app.test.TestViewModel
import androidx.fragment.app.test.ViewModelActivity
import androidx.fragment.app.test.ViewModelActivity.ViewModelFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ViewModelTest {

    @Test(expected = IllegalStateException::class)
    @UiThreadTest
    fun testNotAttachedFragment() {
        // This is similar to calling getViewModelStore in Fragment's constructor
        Fragment().viewModelStore
    }

    @Test
    fun testSameActivityViewModels() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            val activityModel = withActivity { activityModel }
            val defaultActivityModel = withActivity { defaultActivityModel }
            assertThat(defaultActivityModel).isNotSameInstanceAs(activityModel)

            var fragment1 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_1) }
            var fragment2 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_2) }
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.activityModel).isSameInstanceAs(activityModel)
            assertThat(fragment2.activityModel).isSameInstanceAs(activityModel)

            assertThat(fragment1.defaultActivityModel).isSameInstanceAs(defaultActivityModel)
            assertThat(fragment2.defaultActivityModel).isSameInstanceAs(defaultActivityModel)

            recreate()

            assertThat(withActivity { activityModel }).isSameInstanceAs(activityModel)
            assertThat(withActivity { defaultActivityModel }).isSameInstanceAs(defaultActivityModel)

            fragment1 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_1) }
            fragment2 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_2) }

            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.activityModel).isSameInstanceAs(activityModel)
            assertThat(fragment2.activityModel).isSameInstanceAs(activityModel)

            assertThat(fragment1.defaultActivityModel).isSameInstanceAs(defaultActivityModel)
            assertThat(fragment2.defaultActivityModel).isSameInstanceAs(defaultActivityModel)
        }
    }

    @Test
    fun testSameFragmentViewModels() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            var fragment1 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_1) }
            var fragment2 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_2) }
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.fragmentModel).isNotSameInstanceAs(fragment2.fragmentModel)
            val fragment1Model = fragment1.fragmentModel
            val fragment2Model = fragment2.fragmentModel

            recreate()

            fragment1 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_1) }
            fragment2 = withActivity { getFragment(ViewModelActivity.FRAGMENT_TAG_2) }
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.fragmentModel).isSameInstanceAs(fragment1Model)
            assertThat(fragment2.fragmentModel).isSameInstanceAs(fragment2Model)
        }
    }

    @Test
    fun testFragmentOnClearedWhenFinished() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            val fragmentModel = withActivity {
                getFragment(ViewModelActivity.FRAGMENT_TAG_1).fragmentModel
            }
            val fragmentAndroidModel = withActivity {
                getFragment(ViewModelActivity.FRAGMENT_TAG_1).androidModel
            }
            val backStackFragmentModel = withActivity {
                getFragment(ViewModelActivity.FRAGMENT_TAG_BACK_STACK).fragmentModel
            }
            assertThat(fragmentModel.cleared).isFalse()
            assertThat(fragmentAndroidModel.cleared).isFalse()
            assertThat(backStackFragmentModel.cleared).isFalse()

            recreate()
            // recreate shouldn't clear the ViewModels
            assertThat(fragmentModel.cleared).isFalse()
            assertThat(fragmentAndroidModel.cleared).isFalse()
            assertThat(backStackFragmentModel.cleared).isFalse()

            moveToState(Lifecycle.State.DESTROYED)
            // But destroying the Activity should
            assertThat(fragmentModel.cleared).isTrue()
            assertThat(fragmentAndroidModel.cleared).isTrue()
            assertThat(backStackFragmentModel.cleared).isTrue()
        }
    }

    @Test
    fun testFragmentOnCleared() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            val fragment = withActivity {
                Fragment().also {
                    supportFragmentManager.beginTransaction().add(it, "temp").commitNow()
                }
            }
            val viewModelProvider = ViewModelProvider(fragment)
            val vm = viewModelProvider.get(TestViewModel::class.java)
            assertThat(vm.cleared).isFalse()
            onActivity { activity ->
                activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            }
            assertThat(vm.cleared).isTrue()
        }
    }
}

private fun FragmentActivity.getFragment(tag: String) =
    supportFragmentManager.findFragmentByTag(tag) as ViewModelFragment
