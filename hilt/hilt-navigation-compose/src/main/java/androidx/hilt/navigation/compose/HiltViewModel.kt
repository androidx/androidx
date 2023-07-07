/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.hilt.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] or creates a new one scoped to the current navigation graph present on
 * the {@link NavController} back stack.
 *
 * If no navigation graph is currently present then the current scope will be used, usually, a
 * fragment or an activity.
 *
 * @sample androidx.hilt.navigation.compose.samples.NavComposable
 * @sample androidx.hilt.navigation.compose.samples.NestedNavComposable
 */
@Composable
inline fun <reified VM : ViewModel> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null
): VM {
    val factory = createHiltViewModelFactory(viewModelStoreOwner)
    return viewModel(viewModelStoreOwner, key, factory = factory)
}

@Composable
@PublishedApi
internal fun createHiltViewModelFactory(
    viewModelStoreOwner: ViewModelStoreOwner
): ViewModelProvider.Factory? = if (viewModelStoreOwner is HasDefaultViewModelProviderFactory) {
    HiltViewModelFactory(
        context = LocalContext.current,
        delegateFactory = viewModelStoreOwner.defaultViewModelProviderFactory
    )
} else {
    // Use the default factory provided by the ViewModelStoreOwner
    // and assume it is an @AndroidEntryPoint annotated fragment or activity
    null
}
