/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle.viewmodel.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RememberViewModelStoreProviderTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rememberViewModelStoreProvider_whenParentProvided_createsLinkedProvider() {
        var capturedProvider: ViewModelStoreProvider? = null

        rule.setContent {
            // Linking a provider to a parent (like the Activity) allows the
            // provider to participate in a larger, managed lifecycle.
            val activityOwner = LocalViewModelStoreOwner.current!!
            capturedProvider = rememberViewModelStoreProvider(parent = activityOwner)
        }
        rule.waitForIdle()

        assertThat(capturedProvider).isNotNull()
    }

    @Test
    fun rememberViewModelStoreProvider_whenComposableRecomposed_returnsSameInstance() {
        var provider1: ViewModelStoreProvider? = null
        var provider2: ViewModelStoreProvider? = null
        var triggerRecompose by mutableStateOf(0)

        rule.setContent {
            // We use SideEffect to capture the instance across recompositions.
            // The provider must be stable; if it recreated on every recompose,
            // we would lose all stored ViewModels and their state.
            SideEffect { triggerRecompose.toString() }

            val p = rememberViewModelStoreProvider()
            if (triggerRecompose == 0) provider1 = p else provider2 = p
        }
        rule.waitForIdle()

        triggerRecompose++
        rule.waitForIdle()

        assertThat(provider2).isSameInstanceAs(provider1)
    }

    @Test
    fun rememberViewModelStoreProvider_whenParentNull_createsRootProvider() {
        var capturedProvider: ViewModelStoreProvider? = null

        rule.setContent {
            // Passing null creates a standalone root. This is useful for features
            // that need to manage their own lifecycle entirely independent of
            // the hosting Activity or Fragment.
            capturedProvider = rememberViewModelStoreProvider(parent = null)
        }
        rule.waitForIdle()

        assertThat(capturedProvider).isNotNull()
        val store = capturedProvider?.getOrCreate("test_key")
        assertThat(store).isNotNull()
    }

    @Test
    fun rememberViewModelStoreProvider_whenLeavingComposition_disposesProvider() {
        var provider: ViewModelStoreProvider? = null
        var showComposable by mutableStateOf(true)

        rule.setContent {
            if (showComposable) {
                provider = rememberViewModelStoreProvider()
            }
        }
        rule.waitForIdle()
        val originalProvider = provider!!

        // When a composable is removed from the tree, its 'remembered' state
        // is discarded. This ensures that memory is reclaimed once the
        // feature is no longer active in the UI.
        showComposable = false
        rule.waitForIdle()

        showComposable = true
        rule.waitForIdle()

        assertThat(provider).isNotSameInstanceAs(originalProvider)
    }
}
