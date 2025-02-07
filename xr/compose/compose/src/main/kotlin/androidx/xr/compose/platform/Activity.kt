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

package androidx.xr.compose.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.scenecore.Session

private val activityToSpatialComposeScene = mutableMapOf<Activity, SpatialComposeScene>()

/**
 * Composes the provided composable [content] into this activity's subspace.
 *
 * This method only takes effect in Android XR. Calling it will cause the activity to register
 * itself as a subspace, allowing it to control its own panel (i.e. move and resize within its
 * subspace 3D bounds) and show additional spatial content around it.
 *
 * @param content A `@Composable` function declaring the spatial UI content.
 */
public fun ComponentActivity.setSubspaceContent(
    content: @Composable @SubspaceComposable () -> Unit
) {
    // Do nothing if we aren't running on an XR device.
    if (SpatialConfiguration.hasXrSpatialFeature(this)) {
        setSubspaceContent(session = defaultSession, content = content)
    }
}

/**
 * Composes the provided composable [content] into this activity's subspace.
 *
 * This method only takes effect in Android XR. Calling it will cause the activity to register
 * itself as a subspace, allowing it to control its own panel (i.e. move and resize within its
 * subspace 3D bounds) and show additional spatial content around it.
 *
 * @param session The JXR session to use for this subspace.
 * @param content A `@Composable` function declaring the spatial UI content.
 */
@VisibleForTesting
public fun ComponentActivity.setSubspaceContent(
    session: Session,
    content: @Composable @SubspaceComposable () -> Unit,
) {
    val spatialComposeScene = getOrCreateSpatialSceneForActivity(session)

    spatialComposeScene.setContent {
        DisposableEffect(session) {
            session.mainPanelEntity.setHidden(true)
            onDispose { session.mainPanelEntity.setHidden(false) }
        }

        // TODO(b/354009078) Why does rendering content in full space mode break presubmits.

        // We need to emulate the composition locals that setContent provides
        CompositionLocalProvider(
            LocalConfiguration provides resources.configuration,
            LocalDensity provides Density(this),
            LocalView provides window.decorView,
            content = content,
        )
    }
}

/**
 * Looks up an existing [SpatialComposeScene] for this activity. If it doesn't exist, adds it to the
 * map.
 *
 * @param session A custom session to be provided for testing purposes. If null, a suitable session
 *   for production use will be created.
 */
private fun ComponentActivity.getOrCreateSpatialSceneForActivity(
    session: Session = defaultSession
): SpatialComposeScene {
    return activityToSpatialComposeScene.computeIfAbsent(this) {
        SpatialComposeScene(this, session).also { subspace -> setUpSubspace(subspace) }
    }
}

/**
 * Sets up the newly created [spatialComposeScene] to listen to [this] lifecycle state changes and
 * to be cleaned up when [this] is destroyed.
 */
private fun ComponentActivity.setUpSubspace(spatialComposeScene: SpatialComposeScene) {
    lifecycle.addObserver(spatialComposeScene)
    lifecycle.addObserver(
        object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    activityToSpatialComposeScene.remove(this@setUpSubspace)
                }
            }
        }
    )
}

/** Get the default [Session] for this [ComponentActivity]. */
private val ComponentActivity.defaultSession
    get() = Session.create(this)

/** Utility extension function to fetch the current [Activity] based on the [Context] object. */
internal tailrec fun Context.getActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> error("Unexpected Context type when trying to resolve the context's Activity.")
    }
}
