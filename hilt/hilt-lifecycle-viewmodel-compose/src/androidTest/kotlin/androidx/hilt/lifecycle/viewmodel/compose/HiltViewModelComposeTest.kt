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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
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
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltViewModelComposeTest {
    @get:Rule val testRule = HiltAndroidRule(this)

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())

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

    @Test
    fun rememberHiltViewModelFactory_compose() {
        lateinit var vmOne: MyViewModel
        lateinit var vmTwo: MyViewModel
        composeTestRule.setContent {
            val factory = rememberHiltViewModelFactory()
            vmOne = viewModel<MyViewModel>(factory = factory)
            vmTwo = viewModel<MyViewModel>(factory = factory)
        }
        composeTestRule.waitForIdle()

        assertThat(vmOne).isSameInstanceAs(vmTwo)
        assertThat(vmOne.handle).isNotNull()
        assertThat(vmOne.fooDep).isNotNull()
        assertThat(vmOne.handle).isSameInstanceAs(vmTwo.handle)
        assertThat(vmOne.fooDep).isSameInstanceAs(vmTwo.fooDep)
    }

    @Test // b/495230259
    fun hiltViewModel_withPlainViewModelStoreOwner_delegatesToHostFactory() {
        val testViewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalContext provides composeTestRule.activity,
                LocalViewModelStoreOwner provides testViewModelStoreOwner,
                LocalLifecycleOwner provides composeTestRule.activity,
                LocalSavedStateRegistryOwner provides composeTestRule.activity,
            ) {
                // Verifies that `hiltViewModel()` can successfully construct a ViewModel even when
                // provided with a plain `ViewModelStoreOwner` that lacks a default factory. It
                // ensures the internal logic correctly falls back to using the factory from the
                // surrounding @AndroidEntryPoint-annotated host (in this case,
                // composeTestRule.activity).
                assertThat(hiltViewModel<NoArgsViewModel>()).isNotNull()
            }
        }
    }

    @AndroidEntryPoint class TestActivity : ComponentActivity()

    class NoArgsViewModel : ViewModel()

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
