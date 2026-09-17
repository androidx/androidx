/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.dynamicfeatures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import com.google.android.play.core.splitinstall.SplitInstallSessionState

/**
 * Type-safe [NavMetadataKey] used to attach [DynamicFeatureModule] metadata to a temporary loading
 * [NavEntry].
 */
internal object LoadingDynamicFeatureModuleMetadataKey : NavMetadataKey<DynamicFeatureModule>

/**
 * Metadata representing a dynamic feature module.
 *
 * @param moduleName The dynamic feature name.
 * @param entryProviderClassName Fully qualified class name of the
 *   [DynamicFeatureModuleEntryProvider] implementation.
 * @param loadingContent Loading content rendered while the module is being installed.
 */
internal class DynamicFeatureModule(
    val moduleName: String,
    val entryProviderClassName: String,
    val loadingContent: @Composable (sessionState: SplitInstallSessionState) -> Unit,
) {
    /**
     * Flag indicating whether the module's [DynamicFeatureModuleEntryProvider] has been
     * initialized. It is stateful to trigger re-evaluating [NavEntry] instances in
     * [rememberDecoratedNavEntries] so the temporary loading entry is replaced with the module's
     * real entry once installation completes, even though the back stack stayed intact.
     */
    var isInitialized: Boolean by mutableStateOf(false)
}

/**
 * Interface that defines how to build [NavEntry] instances for a
 * [DynamicFeatureEntryProviderScope.dynamicFeatureModule].
 */
public interface DynamicFeatureModuleEntryProvider<K : Any> {

    /** Builds [NavEntry] instances for [K] within the receiver [EntryProviderScope]. */
    public fun EntryProviderScope<K>.buildEntries()
}
