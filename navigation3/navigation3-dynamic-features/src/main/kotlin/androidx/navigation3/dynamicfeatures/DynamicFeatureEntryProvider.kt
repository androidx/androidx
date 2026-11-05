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
import androidx.navigation3.runtime.metadata
import com.google.android.play.core.splitinstall.SplitInstallSessionState

/**
 * Fallback entry provider owned by [DynamicFeatureNavEntryDecorator] that resolves temporary
 * loading entries for uninstalled dynamic modules and delegates to loaded module entries once
 * installed.
 */
internal class DynamicFeatureEntryProvider<T : Any>(
    private val installedModules: () -> Set<String>,
    private val defaultLoadingContent: @Composable (sessionState: SplitInstallSessionState) -> Unit,
) : (T) -> NavEntry<T> {
    internal var fallback: (unknownScreen: T) -> NavEntry<T> = {
        throw IllegalStateException("Unknown screen $it")
    }
    private val scope: EntryProviderScope<T> =
        EntryProviderScope(fallback = @Suppress("UnnecessaryLambdaCreation") { fallback(it) })
    private val delegate: (T) -> NavEntry<T> = scope.build()
    private val modules = mutableMapOf<T, DynamicFeatureModule>()

    internal fun register(
        initialKey: T,
        moduleName: String,
        entryProviderClassName: String,
        loadingContent: (@Composable (sessionState: SplitInstallSessionState) -> Unit)?,
    ) {
        modules.getOrPut(initialKey) {
            DynamicFeatureModule(
                moduleName = moduleName,
                entryProviderClassName = entryProviderClassName,
                loadingContent = loadingContent ?: defaultLoadingContent,
            )
        }
    }

    override fun invoke(key: T): NavEntry<T> {
        val module = modules[key]
        if (module != null) {
            if (!module.isInitialized && module.moduleName !in installedModules()) {
                return createLoadingEntry(key, module)
            }
            initialize(module)
        } else {
            // When resolving a sub-screen key directly (e.g. restored after process death without
            // its initialKey via deep linking), we don't know which module owns it, so initialize
            // any installed modules before delegating.
            for (candidate in modules.values) {
                if (!candidate.isInitialized && candidate.moduleName in installedModules()) {
                    initialize(candidate)
                }
            }
        }
        return delegate(key)
    }

    private fun createLoadingEntry(key: T, module: DynamicFeatureModule): NavEntry<T> =
        NavEntry(
            key = key,
            metadata = metadata { put(LoadingDynamicFeatureModuleMetadataKey, module) },
        ) {
            module.loadingContent(LocalInstallSessionState.current)
        }

    @Suppress("UNCHECKED_CAST")
    internal fun initialize(module: DynamicFeatureModule) {
        if (module.isInitialized) return
        val provider =
            try {
                Class.forName(module.entryProviderClassName).getConstructor().newInstance()
                    as DynamicFeatureModuleEntryProvider<T>
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Cannot instantiate the DynamicFeatureModuleEntryProvider with class name " +
                        module.entryProviderClassName,
                    e,
                )
            }
        with(provider) { scope.buildEntries() }
        module.isInitialized = true
    }
}
