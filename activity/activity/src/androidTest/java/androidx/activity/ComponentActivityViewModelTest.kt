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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityViewModelTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test(expected = IllegalStateException::class)
    @UiThreadTest
    fun testNotAttachedActivity() {
        // This is similar to calling getViewModelStore in Activity's constructor
        ComponentActivity().viewModelStore
    }

    @Test
    fun testSameViewModelStorePrePostOnCreate() {
       withUse(ActivityScenario.launch(ViewModelActivity::class.java)) {
            val originalStore = withActivity { preOnCreateViewModelStore }
            assertWithMessage(
                "Pre-onCreate() ViewModelStore should equal the post-onCreate() ViewModelStore"
            )
                .that(originalStore)
                .isSameInstanceAs(withActivity { postOnCreateViewModelStore })

            recreate()

            assertThat(withActivity { preOnCreateViewModelStore })
                .isSameInstanceAs(originalStore)
            assertThat(withActivity { postOnCreateViewModelStore })
                .isSameInstanceAs(originalStore)
        }
    }

    @Test
    fun testSameActivityViewModels() {
       withUse(ActivityScenario.launch(ViewModelActivity::class.java)) {
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
        lateinit var savedStateModel: TestSavedStateViewModel
        ActivityScenario.launch(ViewModelActivity::class.java).use { scenario ->
            activityModel = scenario.withActivity { this.activityModel }
            defaultActivityModel = scenario.withActivity { this.defaultActivityModel }
            androidModel = scenario.withActivity { this.androidModel }
            savedStateModel = scenario.withActivity { this.savedStateModel }
        }
        assertThat(activityModel.cleared).isTrue()
        assertThat(defaultActivityModel.cleared).isTrue()
        assertThat(androidModel.cleared).isTrue()
        assertThat(savedStateModel.cleared).isTrue()
    }

    @Test
    fun testViewModelsAfterOnResume() {
        val scenario = ActivityScenario.launch(ResumeViewModelActivity::class.java)
        with(scenario) {
            val vm = withActivity { viewModel }
            recreate()
            assertThat(withActivity { viewModel }).isSameInstanceAs(vm)
        }
    }

    @Test
    fun testCreateViewModelViaExtras() {
        val scenario = ActivityScenario.launch(ViewModelActivity::class.java)
        with(scenario) {
            val creationViewModel = withActivity {
                ViewModelProvider(
                    viewModelStore,
                    defaultViewModelProviderFactory,
                    defaultViewModelCreationExtras)["test", TestViewModel::class.java]
            }
            recreate()
            assertThat(withActivity {
                ViewModelProvider(this)["test", TestViewModel::class.java]
            }).isSameInstanceAs(creationViewModel)
        }
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
    lateinit var savedStateModel: TestSavedStateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        preOnCreateViewModelStore = viewModelStore
        super.onCreate(savedInstanceState)
        postOnCreateViewModelStore = viewModelStore

        val viewModelProvider = ViewModelProvider(this)
        activityModel = viewModelProvider.get(KEY_ACTIVITY_MODEL, TestViewModel::class.java)
        defaultActivityModel = viewModelProvider.get(TestViewModel::class.java)
        androidModel = viewModelProvider.get(TestAndroidViewModel::class.java)
        savedStateModel = viewModelProvider.get(TestSavedStateViewModel::class.java)
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

@Suppress("unused")
class TestSavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel() {
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}

class ResumeViewModelActivity : ComponentActivity() {
    lateinit var viewModel: TestViewModel

    override fun onResume() {
        super.onResume()
        viewModel = ViewModelProvider(this).get(TestViewModel::class.java)
    }
}
