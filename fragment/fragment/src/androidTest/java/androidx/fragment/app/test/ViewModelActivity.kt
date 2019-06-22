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

package androidx.fragment.app.test

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.test.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

class ViewModelActivity : FragmentActivity() {

    lateinit var preOnCreateViewModelStore: ViewModelStore
    lateinit var postOnCreateViewModelStore: ViewModelStore
    lateinit var activityModel: TestViewModel
    lateinit var defaultActivityModel: TestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        preOnCreateViewModelStore = viewModelStore
        super.onCreate(savedInstanceState)
        postOnCreateViewModelStore = viewModelStore
        setContentView(R.layout.activity_view_model)
        if (savedInstanceState == null) {
            val backStackFragment = ViewModelFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, ViewModelFragment(), FRAGMENT_TAG_1)
                .add(ViewModelFragment(), FRAGMENT_TAG_2)
                .add(backStackFragment, FRAGMENT_TAG_BACK_STACK)
                .commit()
            supportFragmentManager.beginTransaction()
                .remove(backStackFragment)
                .addToBackStack(FRAGMENT_TAG_BACK_STACK)
                .commit()
        }

        val viewModelProvider = ViewModelProvider(this)
        activityModel = viewModelProvider.get(KEY_ACTIVITY_MODEL, TestViewModel::class.java)
        defaultActivityModel = viewModelProvider.get(TestViewModel::class.java)
    }

    class ViewModelFragment : Fragment() {
        lateinit var fragmentModel: TestViewModel
        lateinit var activityModel: TestViewModel
        lateinit var defaultActivityModel: TestViewModel
        lateinit var androidModel: TestAndroidViewModel
        lateinit var savedStateModel: TestSavedStateViewModel

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val viewModelProvider = ViewModelProvider(this)
            fragmentModel = viewModelProvider.get(
                KEY_FRAGMENT_MODEL,
                TestViewModel::class.java
            )
            val activityViewModelProvider = ViewModelProvider(requireActivity())
            activityModel = activityViewModelProvider.get(
                KEY_ACTIVITY_MODEL,
                TestViewModel::class.java
            )
            defaultActivityModel = activityViewModelProvider.get(TestViewModel::class.java)
            androidModel = viewModelProvider.get(TestAndroidViewModel::class.java)
            savedStateModel = viewModelProvider.get(TestSavedStateViewModel::class.java)
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

    companion object {
        const val KEY_ACTIVITY_MODEL = "activity-model"
        const val KEY_FRAGMENT_MODEL = "fragment-model"
        const val FRAGMENT_TAG_1 = "f1"
        const val FRAGMENT_TAG_2 = "f2"
        const val FRAGMENT_TAG_BACK_STACK = "back_stack"
    }
}