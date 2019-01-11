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
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityViewModelTest {

    companion object {
        private const val TIMEOUT = 2 // secs
    }

    @get:Rule
    var activityRule = ActivityTestRule(ViewModelActivity::class.java)

    @Test(expected = IllegalStateException::class)
    @UiThreadTest
    fun testNotAttachedActivity() {
        // This is similar to calling getViewModelStore in Activity's constructor
        ComponentActivity().viewModelStore
    }

    @Test
    fun testSameViewModelStorePrePostOnCreate() {
        val activity = activityRule.activity
        assertWithMessage(
            "Pre-onCreate() ViewModelStore should equal the post-onCreate() ViewModelStore")
            .that(activity.preOnCreateViewModelStore)
            .isSameAs(activity.postOnCreateViewModelStore)
    }

    @Test
    fun testSameActivityViewModels() {
        val activityModel = arrayOfNulls<TestViewModel>(1)
        val defaultActivityModel = arrayOfNulls<TestViewModel>(1)
        val viewModelActivity = activityRule.activity
        activityRule.runOnUiThread {
            activityModel[0] = viewModelActivity.activityModel
            defaultActivityModel[0] = viewModelActivity.defaultActivityModel
            assertThat(defaultActivityModel[0]).isNotSameAs(activityModel[0])
        }
        val recreatedActivity = recreateActivity(activityRule)
        activityRule.runOnUiThread {
            assertThat(recreatedActivity.activityModel).isSameAs(activityModel[0])
            assertThat(recreatedActivity.defaultActivityModel).isSameAs(defaultActivityModel[0])
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testActivityOnCleared() {
        val activity = activityRule.activity
        val latch = CountDownLatch(1)
        val observer = GenericLifecycleObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                activity.window.decorView.post {
                    try {
                        assertThat(activity.activityModel.cleared).isTrue()
                        assertThat(activity.defaultActivityModel.cleared).isTrue()
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
}

class ViewModelActivity : ComponentActivity() {

    companion object {
        const val KEY_ACTIVITY_MODEL = "activity-model"
    }

    lateinit var preOnCreateViewModelStore: ViewModelStore
    lateinit var postOnCreateViewModelStore: ViewModelStore
    lateinit var activityModel: TestViewModel
    lateinit var defaultActivityModel: TestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        preOnCreateViewModelStore = viewModelStore
        super.onCreate(savedInstanceState)
        postOnCreateViewModelStore = viewModelStore

        val viewModelProvider = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )
        activityModel = viewModelProvider.get(KEY_ACTIVITY_MODEL, TestViewModel::class.java)
        defaultActivityModel = viewModelProvider.get(TestViewModel::class.java)
    }
}

class TestViewModel : ViewModel() {
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}