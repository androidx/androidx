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

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityViewModelTest {

    @Test(expected = IllegalStateException::class)
    @UiThreadTest
    fun testNotAttachedActivity() {
        // This is similar to calling getViewModelStore in Activity's constructor
        ComponentActivity().viewModelStore
    }

    @Test
    fun testSameViewModelStorePrePostOnCreate() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            assertWithMessage(
                "Pre-onCreate() ViewModelStore should equal the post-onCreate() ViewModelStore")
                .that(withActivity { preOnCreateViewModelStore })
                .isSameInstanceAs(withActivity { postOnCreateViewModelStore })
        }
    }

    @Test
    fun testSameActivityViewModels() {
        with(ActivityScenario.launch(ViewModelActivity::class.java)) {
            val activityModel = withActivity { activityModel }
            val defaultActivityModel = withActivity { defaultActivityModel }
            assertThat(defaultActivityModel).isNotSameInstanceAs(activityModel)

            recreate()

            assertThat(withActivity { activityModel })
                .isSameInstanceAs(activityModel)
            assertThat(withActivity { defaultActivityModel })
                .isSameInstanceAs(defaultActivityModel)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testActivityOnCleared() {
        lateinit var activityModel: TestViewModel
        lateinit var defaultActivityModel: TestViewModel
        lateinit var androidModel: TestAndroidViewModel
        ActivityScenario.launch(ViewModelActivity::class.java).use { scenario ->
            activityModel = scenario.withActivity { this.activityModel }
            defaultActivityModel = scenario.withActivity { this.defaultActivityModel }
            androidModel = scenario.withActivity { this.androidModel }
        }
        assertThat(activityModel.cleared).isTrue()
        assertThat(defaultActivityModel.cleared).isTrue()
        assertThat(androidModel.cleared).isTrue()
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
    lateinit var androidModel: TestAndroidViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        preOnCreateViewModelStore = viewModelStore
        super.onCreate(savedInstanceState)
        postOnCreateViewModelStore = viewModelStore

        val viewModelProvider = ViewModelProvider(this)
        activityModel = viewModelProvider.get(KEY_ACTIVITY_MODEL, TestViewModel::class.java)
        defaultActivityModel = viewModelProvider.get(TestViewModel::class.java)
        androidModel = viewModelProvider.get(TestAndroidViewModel::class.java)
    }
}

class TestViewModel : ViewModel() {
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}

class TestAndroidViewModel(application: Application) : AndroidViewModel(application) {
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}
