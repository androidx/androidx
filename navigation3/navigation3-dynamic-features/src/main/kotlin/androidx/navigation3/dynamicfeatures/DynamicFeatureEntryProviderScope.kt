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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

/**
 * Scope for constructing [NavEntry] instances and registering dynamic feature modules via
 * [dynamicFeatureModule] in [DynamicFeatureNavEntryDecorator.entryProvider].
 *
 * @param T the type of the [NavEntry] key
 */
public class DynamicFeatureEntryProviderScope<T : Any>
internal constructor(private val dynamicEntryProvider: DynamicFeatureEntryProvider<T>) :
    EntryProviderScope<T>(fallback = dynamicEntryProvider) {

    /**
     * Registers a dynamic feature module inside this [DynamicFeatureEntryProviderScope].
     *
     * Lazily resolves destinations defined by the module once installed. Before installation,
     * [initialKey] resolves to a temporary loading [NavEntry] intercepted by
     * [DynamicFeatureNavEntryDecorator].
     *
     * @param initialKey The initial destination key for this module, defined in the common app
     *   module.
     * @param moduleName The dynamic feature name.
     * @param entryProviderClassName Fully qualified class name of the
     *   [DynamicFeatureModuleEntryProvider] implementation.
     * @param loadingContent Optional module-specific content rendered while the module is not
     *   installed. If null, the decorator's `defaultLoadingContent` is used. Receives
     *   [SplitInstallSessionState] updates from [SplitInstallManager] and should handle states such
     *   as [SplitInstallSessionStatus.DOWNLOADING], [SplitInstallSessionStatus.FAILED], and
     *   [SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION] (see
     *   [SplitInstallManager.startConfirmationDialogForResult]).
     * @sample androidx.navigation3.dynamicfeatures.samples.DynamicFeatureNavDisplaySample
     * @sample androidx.navigation3.dynamicfeatures.samples.DynamicFeatureModuleEntryProviderSample
     */
    public fun <K : T> dynamicFeatureModule(
        initialKey: K,
        moduleName: String,
        entryProviderClassName: String,
        loadingContent: (@Composable (sessionState: SplitInstallSessionState) -> Unit)? = null,
    ) {
        dynamicEntryProvider.register(
            initialKey = initialKey,
            moduleName = moduleName,
            entryProviderClassName = entryProviderClassName,
            loadingContent = loadingContent,
        )
    }
}
