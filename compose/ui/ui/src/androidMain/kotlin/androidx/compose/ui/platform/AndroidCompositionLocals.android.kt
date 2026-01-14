/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.ResourceIdCache
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val LocalConfiguration =
    compositionLocalOf<Configuration> { noLocalProvidedFor("LocalConfiguration") }

/** Provides a [Context] that can be used by Android applications. */
val LocalContext = staticCompositionLocalOf<Context> { noLocalProvidedFor("LocalContext") }

/**
 * The Android [Resources]. This will be updated when [LocalConfiguration] changes, to ensure that
 * calls to APIs such as [Resources.getString] return updated values.
 */
val LocalResources =
    compositionLocalWithComputedDefaultOf<Resources> {
        // Read LocalConfiguration here to invalidate callers of LocalResources when the
        // configuration changes. This is preferable to explicitly providing the resources object
        // because the resources object can still have the same instance, even though the
        // configuration changed, which would mean that callers would not get invalidated. To
        // resolve that we would need to use neverEqualPolicy to force an invalidation even though
        // the Resources didn't change, but then that would cause invalidations every time the
        // providing Composable is recomposed, regardless of whether a configuration change happened
        // or not.
        LocalConfiguration.currentValue
        LocalContext.currentValue.resources
    }

internal val LocalImageVectorCache =
    staticCompositionLocalOf<ImageVectorCache> { noLocalProvidedFor("LocalImageVectorCache") }

internal val LocalResourceIdCache =
    staticCompositionLocalOf<ResourceIdCache> { noLocalProvidedFor("LocalResourceIdCache") }

@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
actual val LocalLifecycleOwner
    get() = LocalLifecycleOwner

/** The CompositionLocal containing the current [SavedStateRegistryOwner]. */
@Deprecated(
    "Moved to savedstate-compose library in androidx.savedstate.compose package.",
    ReplaceWith("androidx.savedstate.compose.LocalSavedStateRegistryOwner"),
)
val LocalSavedStateRegistryOwner
    get() = LocalSavedStateRegistryOwner

/** The CompositionLocal containing the current Compose [View]. */
val LocalView = staticCompositionLocalOf<View> { noLocalProvidedFor("LocalView") }

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
