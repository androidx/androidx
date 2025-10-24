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

import android.content.Context
import android.view.View
import androidx.annotation.RestrictTo
import androidx.xr.compose.R
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo

/**
 * Manager for all [SpatialComposeScene]s that are created in a given context.
 *
 * Enables finding all semantic roots in a spatial scene graph. This is useful for testing libraries
 * as well as developer tooling to help semantically identify parts of the compose tree. It is not
 * intended to be used in individual apps. Scenes are scoped to the current View hierarchy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object SceneManager {
    internal fun onSceneCreated(scene: SpatialComposeScene) {
        scene.context.decorView?.registeredScenes?.takeIf { scene !in it }?.add(scene)
    }

    internal fun onSceneDisposed(scene: SpatialComposeScene) {
        scene.context.decorView?.registeredScenes?.remove(scene)
    }

    /**
     * Returns all root subspace semantics nodes of all registered scenes in the given context.
     *
     * @param context The context to search for registered scenes.
     */
    public fun getAllRootSubspaceSemanticsNodes(context: Context): List<SubspaceSemanticsInfo> =
        context.decorView?.registeredScenes?.map {
            it.rootElement.compositionOwner.root.measurableLayout
        } ?: emptyList()

    /**
     * Returns the number of registered scene for the given context.
     *
     * @param context The context to search for registered scenes.
     */
    public fun getSceneCount(context: Context): Int = context.decorView?.registeredScenes?.size ?: 0

    private val Context.decorView: View?
        get() = getActivity()?.window?.decorView

    private val View.registeredScenes: MutableList<SpatialComposeScene>
        get() {
            @Suppress("UNCHECKED_CAST")
            return (getTag(R.id.compose_xr_registered_scenes)
                ?: mutableListOf<SpatialComposeScene>().also {
                    setTag(R.id.compose_xr_registered_scenes, it)
                })
                as MutableList<SpatialComposeScene>
        }
}
