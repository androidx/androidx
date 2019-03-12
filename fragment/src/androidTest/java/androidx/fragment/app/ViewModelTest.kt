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

import android.app.Instrumentation
import androidx.fragment.app.test.TestViewModel
import androidx.fragment.app.test.ViewModelActivity
import androidx.fragment.app.test.ViewModelActivity.ViewModelFragment
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewModelTest {

    @get:Rule
    var activityRule = ActivityTestRule(ViewModelActivity::class.java)

    @Test(expected = IllegalStateException::class)
    @UiThreadTest
    fun testNotAttachedFragment() {
        // This is similar to calling getViewModelStore in Fragment's constructor
        Fragment().viewModelStore
    }

    @Test
    fun testSameActivityViewModels() {
        var viewModelActivity = activityRule.activity
        val activityModel = viewModelActivity.activityModel
        val defaultActivityModel = viewModelActivity.defaultActivityModel
        activityRule.runOnUiThread {
            assertThat(defaultActivityModel).isNotSameAs(activityModel)

            val fragment1 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_1)
            val fragment2 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_2)
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.activityModel).isSameAs(activityModel)
            assertThat(fragment2.activityModel).isSameAs(activityModel)

            assertThat(fragment1.defaultActivityModel).isSameAs(defaultActivityModel)
            assertThat(fragment2.defaultActivityModel).isSameAs(defaultActivityModel)
        }
        viewModelActivity = recreateActivity()
        activityRule.runOnUiThread {
            assertThat(viewModelActivity.activityModel).isSameAs(activityModel)
            assertThat(viewModelActivity.defaultActivityModel).isSameAs(defaultActivityModel)

            val fragment1 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_1)
            val fragment2 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_2)
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.activityModel).isSameAs(activityModel)
            assertThat(fragment2.activityModel).isSameAs(activityModel)

            assertThat(fragment1.defaultActivityModel).isSameAs(defaultActivityModel)
            assertThat(fragment2.defaultActivityModel).isSameAs(defaultActivityModel)
        }
    }

    @Test
    fun testSameFragmentViewModels() {
        var viewModelActivity = activityRule.activity
        lateinit var fragment1Model: TestViewModel
        lateinit var fragment2Model: TestViewModel
        activityRule.runOnUiThread {
            val fragment1 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_1)
            val fragment2 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_2)
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.fragmentModel).isNotSameAs(fragment2.fragmentModel)
            fragment1Model = fragment1.fragmentModel
            fragment2Model = fragment2.fragmentModel
        }
        viewModelActivity = recreateActivity()
        activityRule.runOnUiThread {
            val fragment1 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_1)
            val fragment2 = getFragment(viewModelActivity, ViewModelActivity.FRAGMENT_TAG_2)
            assertThat(fragment1).isNotNull()
            assertThat(fragment2).isNotNull()

            assertThat(fragment1.fragmentModel).isSameAs(fragment1Model)
            assertThat(fragment2.fragmentModel).isSameAs(fragment2Model)
        }
    }

    @Test
    fun testFragmentOnClearedWhenFinished() {
        val activity = activityRule.activity
        val fragment = getFragment(activity, ViewModelActivity.FRAGMENT_TAG_1)
        val latch = CountDownLatch(1)
        val observer = object : LifecycleObserver {
            @OnLifecycleEvent(ON_DESTROY)
            fun onDestroy() {
                activity.window.decorView.post {
                    try {
                        assertThat(fragment.fragmentModel.cleared).isTrue()
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        activityRule.runOnUiThread { activity.lifecycle.addObserver(observer) }
        activity.finish()
        assertThat(latch.await(TIMEOUT.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testFragmentOnCleared() {
        val activity = activityRule.activity
        val latch = CountDownLatch(1)
        val observer = object : LifecycleObserver {
            @OnLifecycleEvent(ON_RESUME)
            fun onResume() {
                try {
                    val manager = activity.supportFragmentManager
                    val fragment = Fragment()
                    manager.beginTransaction().add(fragment, "temp").commitNow()
                    val viewModelProvider = ViewModelProvider(
                        fragment,
                        ViewModelProvider.NewInstanceFactory()
                    )
                    val vm = viewModelProvider.get(TestViewModel::class.java)
                    assertThat(vm.cleared).isFalse()
                    manager.beginTransaction().remove(fragment).commitNow()
                    assertThat(vm.cleared).isTrue()
                } finally {
                    latch.countDown()
                }
            }
        }

        activityRule.runOnUiThread { activity.lifecycle.addObserver(observer) }
        assertThat(latch.await(TIMEOUT.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    private fun getFragment(activity: FragmentActivity, tag: String) =
        activity.supportFragmentManager.findFragmentByTag(tag) as ViewModelFragment

    private fun recreateActivity(): ViewModelActivity {
        val monitor = Instrumentation.ActivityMonitor(
            ViewModelActivity::class.java.canonicalName, null, false
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.addMonitor(monitor)
        val previous = activityRule.activity
        activityRule.runOnUiThread { previous.recreate() }
        var result: ViewModelActivity

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate

        synchronized(monitor) {
            do {
                // the documentation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destroyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = monitor.waitForActivityWithTimeout(4000) as ViewModelActivity
            } while (result === previous)
        }
        return result
    }

    companion object {
        private const val TIMEOUT = 2 // secs
    }
}
