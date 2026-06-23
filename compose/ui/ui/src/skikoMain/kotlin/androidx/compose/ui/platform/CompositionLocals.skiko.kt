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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LocalHostDefaultProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
    level = DeprecationLevel.HIDDEN
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

// TODO: Remove as part of https://youtrack.jetbrains.com/issue/CMP-9379
/**
 * The CompositionLocal that provides information about window insets associated with current
 * scene.
 */
@InternalComposeUiApi
val LocalPlatformWindowInsets = staticCompositionLocalOf<PlatformWindowInsets> {
    error("CompositionLocal LocalPlatformWindowInsets not present")
}

/**
 * The CompositionLocal providing prefetch scheduler associated with the current scene.
 */
@InternalComposeUiApi
val LocalPlatformPrefetchScheduler = staticCompositionLocalOf<PlatformPrefetchScheduler> {
    error("CompositionLocal LocalPlatformPrefetchScheduler not present")
}

@OptIn(InternalComposeApi::class)
@Composable
internal fun ProvidePlatformCompositionLocals(
    vararg values: ProvidedValue<*>,
    platformContext: PlatformContext,
    content: @Composable () -> Unit,
) {
    val saveableStateRegistry = remember(platformContext) {
        DisposableSaveableStateRegistry(
            id = "ComposeContainer",
            savedStateRegistryOwner = platformContext.architectureComponentsOwner.savedStateRegistryOwner
        )
    }
    DisposableEffect(platformContext) {
        //save a reference to dispose of a right object
        val registry = saveableStateRegistry
        onDispose { registry.dispose() }
    }

    val hostDefaultProvider = remember(platformContext) {
        HostDefaultProviderImpl(platformContext)
    }

    CompositionLocalProvider(
        *values,
        LocalPlatformScreenReader provides platformContext.screenReader,
        LocalPlatformWindowInsets provides platformContext.windowInsets,
        LocalPlatformPrefetchScheduler provides platformContext.prefetchScheduler,
        androidx.lifecycle.compose.LocalLifecycleOwner provides platformContext.architectureComponentsOwner.lifecycleOwner,
        LocalSavedStateRegistryOwner provides platformContext.architectureComponentsOwner.savedStateRegistryOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalHostDefaultProvider provides hostDefaultProvider,
        content = content,
    )
}
