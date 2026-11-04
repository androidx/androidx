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

package androidx.xr.scenecore.integration.tests.e2e

import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.scene
import java.nio.file.Paths

/**
 * Loads a glTF model and a texture, then applies a textured PBR material override to one of the
 * model's nodes.
 */
class GltfMaterialTextureScenarioActivity : ScenarioActivity() {

    override suspend fun runScenario(session: Session) {
        val texture = Texture.create(session, Paths.get("textures", "pattern.png"))
        val material = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
        material.setBaseColorTexture(texture)
        material.setMetallicFactor(0.9f)
        material.setRoughnessFactor(0.2f)

        val model = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = model,
                pose = Pose(translation = Vector3(0f, 0.5f, -1.5f)),
                parent = session.scene.activitySpace,
            )

        val node =
            checkNotNull(entity.nodes.firstOrNull { it.name == "Dragon" }) {
                "Expected node 'Dragon' in Dragon_Evolved.gltf"
            }
        node.setMaterialOverride(material)
    }
}
