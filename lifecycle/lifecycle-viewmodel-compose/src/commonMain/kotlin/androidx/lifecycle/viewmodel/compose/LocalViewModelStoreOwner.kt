/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalWithHostDefaultOf
import androidx.lifecycle.ViewModelStoreOwner

/** The CompositionLocal containing the current [ViewModelStoreOwner]. */
public object LocalViewModelStoreOwner {
    private val LocalViewModelStoreOwner =
        compositionLocalWithHostDefaultOf(ViewModelStoreOwnerHostDefaultKey)

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided.
     * On Android it will also try to get it via
     * [androidx.lifecycle.findViewTreeViewModelStoreOwner] on the current
     * [androidx.compose.ui.platform.LocalView].
     */
    public val current: ViewModelStoreOwner?
        @Composable get() = LocalViewModelStoreOwner.current

    /**
     * Associates a [LocalViewModelStoreOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(
        viewModelStoreOwner: ViewModelStoreOwner
    ): ProvidedValue<ViewModelStoreOwner?> {
        return LocalViewModelStoreOwner.provides(viewModelStoreOwner)
    }
}

/**
 * A [HostDefaultKey] used to retrieve the [ViewModelStoreOwner] provided by the current hosting
 * environment.
 *
 * This key allows the composition to access the host's [ViewModelStoreOwner] through a decoupled
 * mechanism, typically used by [compositionLocalWithHostDefaultOf].
 *
 * On platforms where a [ViewModelStoreOwner] is not present or supported, this may resolve to
 * `null`.
 *
 * @see HostDefaultKey
 * @see compositionLocalWithHostDefaultOf
 */
public expect val ViewModelStoreOwnerHostDefaultKey: HostDefaultKey<ViewModelStoreOwner?>
