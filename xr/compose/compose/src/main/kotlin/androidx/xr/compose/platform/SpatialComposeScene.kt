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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Session

/**
 * A 3D scene represented via Compose elements and coordinated with SceneCore.
 *
 * This class manages the lifecycle and root element of the spatial scene. It also provides access
 * to the SceneCore session and environment.
 *
 * @param parentCompositionContext the optional composition context when this is a sub-composition
 * @param rootEntity the optional [CoreEntity] to associate with the root of this composition
 * @property ownerActivity the [ComponentActivity] that owns this scene.
 * @property jxrSession the [Session] used to interact with SceneCore.
 */
internal class SpatialComposeScene(
    /** Context of the activity that this scene is rooted on. */
    public val ownerActivity: ComponentActivity,
    @InternalSubspaceApi public val jxrSession: Session,
    parentCompositionContext: CompositionContext? = null,
    rootEntity: CoreEntity? = null,
) : DefaultLifecycleObserver, LifecycleOwner {
    init {
        SceneManager.onSceneCreated(this)
    }

    /** Root of the spatial scene graph of this [SpatialComposeScene]. */
    internal val rootElement: SpatialComposeElement =
        SpatialComposeElement(this, parentCompositionContext, rootEntity)

    public var rootVolumeConstraints: VolumeConstraints
        get() = rootElement.rootVolumeConstraints
        set(value) {
            rootElement.rootVolumeConstraints = value
        }

    public fun setContent(content: @Composable @SubspaceComposable () -> Unit) {
        rootElement.setContent(content)
    }

    public fun dispose() {
        rootElement.disposeComposition()
        SceneManager.onSceneDisposed(this)
    }

    override val lifecycle: Lifecycle
        get() = ownerActivity.lifecycle
}
