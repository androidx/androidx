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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.lifecycle.LifecycleOwner

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
actual val LocalLifecycleOwner get() = androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The CompositionLocal that provides information about Screen Reader state associated with
 * the current scene.
 */
@InternalComposeUiApi
val LocalPlatformScreenReader = staticCompositionLocalOf<PlatformScreenReader> {
    error("CompositionLocal LocalPlatformScreenReader not present")
}

/**
 * The CompositionLocal that provides information about window insets associated with current
 * scene.
 */
@InternalComposeUiApi
val LocalPlatformWindowInsets = staticCompositionLocalOf<PlatformWindowInsets> {
    error("CompositionLocal LocalPlatformWindowInsets not present")
}

private val PlatformArchitectureComponentsOwner.values: Array<ProvidedValue<*>>
    get() {
        val providedValues = mutableListOf(
            androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner,
            LocalInternalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
            LocalCompatNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
        )
        viewModelStoreOwner?.let { providedValues.add(LocalInternalViewModelStoreOwner provides it) }
        return providedValues.toTypedArray()
    }

@Composable
internal fun ProvidePlatformCompositionLocals(
    vararg values: ProvidedValue<*>,
    platformContext: PlatformContext,
    content: @Composable () -> Unit,
) {
    // TODO: Provide LocalSavedStateRegistryOwner and LocalSaveableStateRegistry

    CompositionLocalProvider(
        *values,
        LocalPlatformScreenReader provides platformContext.screenReader,
        LocalPlatformWindowInsets provides platformContext.windowInsets,
        *platformContext.architectureComponentsOwner.values,
        content = content,
    )
}
