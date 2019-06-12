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

package androidx.lifecycle.viewmodel.savedstate

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateVMFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SavedStateFactoryTest {

    @get:Rule
    var activityRule = ActivityTestRule(MyActivity::class.java)

    @Test
    fun testCreateAndroidVM() {
        val savedStateVMFactory = SavedStateVMFactory(activityRule.activity)
        val vm = ViewModelProvider(ViewModelStore(), savedStateVMFactory)
        assertThat(vm.get(MyAndroidViewModel::class.java).handle).isNotNull()
        assertThat(vm.get(MyViewModel::class.java).handle).isNotNull()
    }

    internal class MyAndroidViewModel(app: Application, val handle: SavedStateHandle) :
        AndroidViewModel(app)

    internal class MyViewModel(val handle: SavedStateHandle) : ViewModel()

    class MyActivity : FragmentActivity()
}
