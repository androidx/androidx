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

package androidx.xr.scenecore.spatial.core

import android.content.Context
import androidx.xr.runtime.math.Matrix4
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MeshFeature
import com.android.extensions.xr.XrExtensions
import java.util.concurrent.ScheduledExecutorService

internal class MeshEntityImpl(
    context: Context,
    private val meshFeature: MeshFeature,
    parentEntity: Entity?,
    extensions: XrExtensions,
    sceneNodeRegistry: SceneNodeRegistry,
    executor: ScheduledExecutorService,
) : BaseRenderingEntity(context, meshFeature, extensions, sceneNodeRegistry, executor), MeshEntity {
    init {
        parent = parentEntity
    }

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        meshFeature.setMaterial(material, subsetIndex)
    }

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        meshFeature.setBoneTransforms(transforms)
    }
}
