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

package androidx.navigation3

import androidx.compose.runtime.Composable

/**
 * Interface that offers the ability to provide information to some Composable content that is
 * integrated with a [NavDisplay](reference/androidx/navigation/NavDisplay).
 *
 * Information can be provided to the entire back stack via [NavLocalProvider.ProvideToBackStack] or
 * to a single entry via [NavLocalProvider.ProvideToEntry].
 */
public interface NavLocalProvider {

    /**
     * Allows a [NavLocalProvider] to provide to the entire backstack.
     *
     * This function is called by the [NavWrapperManager] and should not be called directly.
     */
    @Composable
    public fun ProvideToBackStack(backStack: List<Any>, content: @Composable () -> Unit): Unit =
        content.invoke()

    /**
     * Allows a [NavLocalProvider] to provide information to a single entry.
     *
     * This function is called by the [NavDisplay](reference/androidx/navigation/NavDisplay) and
     * should not be called directly.
     */
    @Composable public fun <T : Any> ProvideToEntry(entry: NavEntry<T>)
}
