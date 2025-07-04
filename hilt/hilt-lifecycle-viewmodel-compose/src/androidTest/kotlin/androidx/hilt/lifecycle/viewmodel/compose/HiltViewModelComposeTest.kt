/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.hilt.lifecycle.viewmodel.compose

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltViewModelComposeTest {
    @get:Rule val testRule = HiltAndroidRule(this)

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

    // TODO(kuanyingchou) Remove this after https://github.com/google/dagger/issues/3601 is
    //  resolved.
    @Inject @ApplicationContext lateinit var context: Context

    @Test
    fun hiltViewModel_compose() {
        lateinit var vmOne: MyViewModel
        lateinit var vmTwo: MyViewModel
        composeTestRule.setContent {
            vmOne = hiltViewModel<MyViewModel>()
            vmTwo = hiltViewModel<MyViewModel>()
        }
        composeTestRule.waitForIdle()

        assertThat(vmOne).isSameInstanceAs(vmTwo)
        assertThat(vmOne.handle).isSameInstanceAs(vmTwo.handle)
        assertThat(vmOne.fooDep).isSameInstanceAs(vmTwo.fooDep)
    }

    @Test
    fun hiltViewModel_assisted_compose() {
        lateinit var vm: MyAssistedViewModel
        composeTestRule.setContent {
            vm = hiltViewModel<MyAssistedViewModel, MyAssistedViewModel.Factory>() { it.create(42) }
        }
        composeTestRule.waitForIdle()

        assertThat(vm).isNotNull()
        assertThat(vm.handle).isNotNull()
        assertThat(vm.fooDep).isNotNull()
        assertThat(vm.arg).isEqualTo(42)
    }

    @AndroidEntryPoint class TestActivity : ComponentActivity()

    @HiltViewModel
    class MyViewModel @Inject constructor(val handle: SavedStateHandle, val fooDep: Foo) :
        ViewModel()

    @HiltViewModel(assistedFactory = MyAssistedViewModel.Factory::class)
    class MyAssistedViewModel
    @AssistedInject
    constructor(val handle: SavedStateHandle, val fooDep: Foo, @Assisted val arg: Int) :
        ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(arg: Int): MyAssistedViewModel
        }
    }

    class Foo @Inject constructor()
}
