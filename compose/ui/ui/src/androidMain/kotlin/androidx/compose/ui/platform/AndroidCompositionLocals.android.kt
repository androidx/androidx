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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.view.Window
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.staticCompositionLocalWithComputedDefaultOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

@SuppressLint("NullAnnotationGroup", "BanInlineOptIn")
@OptIn(ExperimentalComposeUiApi::class)
private inline fun <T : Any> computedDefaultOf(
    name: String,
    crossinline compute: androidx.compose.runtime.CompositionLocalAccessorScope.() -> T,
): ProvidableCompositionLocal<T> =
    if (ComposeUiFlags.isMinimalistLocalsEnabled) {
        staticCompositionLocalWithComputedDefaultOf { compute() }
    } else {
        staticCompositionLocalOf { noLocalProvidedFor(name) }
    }

@SuppressLint("NullAnnotationGroup", "BanInlineOptIn")
@OptIn(ExperimentalComposeUiApi::class)
private inline fun <T : Any> computedNullableDefaultOf(
    crossinline compute: androidx.compose.runtime.CompositionLocalAccessorScope.() -> T?
): ProvidableCompositionLocal<T?> =
    if (ComposeUiFlags.isMinimalistLocalsEnabled) {
        staticCompositionLocalWithComputedDefaultOf { compute() }
    } else {
        staticCompositionLocalOf { null }
    }

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalComposeUiApi::class)
public val LocalConfiguration: ProvidableCompositionLocal<Configuration> =
    if (ComposeUiFlags.isMinimalistLocalsEnabled) {
        compositionLocalWithComputedDefaultOf { LocalContext.currentValue.resources.configuration }
    } else {
        compositionLocalOf { noLocalProvidedFor("LocalConfiguration") }
    }

/** Provides a [Context] that can be used by Android applications. */
public val LocalContext: ProvidableCompositionLocal<Context> =
    computedDefaultOf<Context>("LocalContext") {
        LocalAndroidComposeView.currentValue?.context ?: noLocalProvidedFor("LocalContext")
    }

/**
 * The Android [Resources]. This will be updated when [LocalConfiguration] changes, to ensure that
 * calls to APIs such as [Resources.getString] return updated values.
 */
public val LocalResources: ProvidableCompositionLocal<Resources> =
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
    computedDefaultOf("LocalImageVectorCache") {
        LocalAndroidComposeView.currentValue?.composeViewContext?.imageVectorCache
            ?: noLocalProvidedFor("LocalImageVectorCache")
    }

internal val LocalResourceIdCache =
    computedDefaultOf("LocalResourceIdCache") {
        LocalAndroidComposeView.currentValue?.composeViewContext?.resourceIdCache
            ?: noLocalProvidedFor("LocalResourceIdCache")
    }

@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
public actual val LocalLifecycleOwner: ProvidableCompositionLocal<androidx.lifecycle.LifecycleOwner>
    get() = androidx.lifecycle.compose.LocalLifecycleOwner

/** The CompositionLocal containing the current [SavedStateRegistryOwner]. */
@Deprecated(
    "Moved to savedstate-compose library in androidx.savedstate.compose package.",
    ReplaceWith("androidx.savedstate.compose.LocalSavedStateRegistryOwner"),
)
public val LocalSavedStateRegistryOwner:
    ProvidableCompositionLocal<androidx.savedstate.SavedStateRegistryOwner>
    get() = androidx.savedstate.compose.LocalSavedStateRegistryOwner

/** The CompositionLocal containing the current Compose [View]. */
public val LocalView: ProvidableCompositionLocal<View> =
    computedDefaultOf<View>("LocalView") {
        LocalAndroidComposeView.currentValue ?: noLocalProvidedFor("LocalView")
    }

/** The CompositionLocal containing the current [Window] if available. */
public val LocalWindow: ProvidableCompositionLocal<Window?> = computedNullableDefaultOf {
    LocalAndroidComposeView.currentValue?.window
}

/**
 * Recursively traverses up the [View] parent hierarchy to find a containing [DialogWindowProvider].
 *
 * This is used to locate the [Window] associated with a Compose [Dialog], which is hosted in a
 * separate window layer from the main Activity.
 *
 * @param view The starting [View] (typically the [AndroidComposeView] root of the composition).
 * @return The [Window] of the containing dialog if found, or `null` otherwise.
 */
internal fun findDialogWindow(view: View): Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) {
            return current.window
        }
        if (current is PopupLayout) {
            current = current.composeView
            continue
        }
        val parent = current.parent
        current = parent as? View
    }
    return null
}

/**
 * Recursively unwraps the [Context] chain of the given [View] to find the hosting [Activity].
 *
 * Views do not have a direct public API to retrieve their hosting [Window]. Instead, this helper
 * traverses and unwraps [ContextWrapper]s (e.g., theme or configuration wrappers) to find the
 * underlying [Activity] instance and access its [Window].
 *
 * @param view The [View] whose context chain should be searched.
 * @return The [Window] of the hosting [Activity] if found, or `null` otherwise.
 */
internal fun findActivityWindow(view: View): Window? {
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context.window
        }
        context = context.baseContext
    }
    return null
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
