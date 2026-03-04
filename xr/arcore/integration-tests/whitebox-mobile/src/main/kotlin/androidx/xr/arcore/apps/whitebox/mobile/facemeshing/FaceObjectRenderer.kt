/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.apps.whitebox.mobile.facemeshing

import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import java.io.IOException

class FaceObjectRenderer(
    val render: SampleRender,
    meshObjAssetPath: String,
    textureAssetPath: String,
) {
    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private var modelMatrix = Matrix4.Zero
    private var modelViewMatrix = Matrix4.Zero
    private var modelViewProjectionMatrix = Matrix4.Zero

    private lateinit var objectShader: Shader
    private lateinit var objectMesh: Mesh

    init {
        try {
            val faceMeshReferenceTexture =
                Texture.createFromAsset(
                    render,
                    textureAssetPath,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB,
                )

            objectShader =
                Shader.createFromAssets(
                        render,
                        VERTEX_SHADER_ASSET_PATH,
                        FRAGMENT_SHADER_ASSET_PATH,
                        null,
                    )
                    .setTexture("u_Texture", faceMeshReferenceTexture)
                    .setCullFace(false)
                    .setDepthWrite(false)

            objectMesh = Mesh.createFromAsset(render, meshObjAssetPath)
        } catch (e: IOException) {
            XrLog.error(e) { "Failed to initialize FaceMesh assets" }
        }
    }

    fun draw(pose: Pose, viewMatrix: Matrix4, projectionMatrix: Matrix4, framebuffer: Framebuffer) {
        // render the face mesh
        modelMatrix = Matrix4.fromPose(pose)
        modelViewProjectionMatrix = projectionMatrix * viewMatrix * modelMatrix
        modelViewMatrix = viewMatrix * modelMatrix

        objectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix.data)
        render.draw(objectMesh, objectShader, framebuffer)
    }

    companion object {
        private val TAG: String = FaceObjectRenderer::class.java.name

        // Shader names
        private const val VERTEX_SHADER_ASSET_PATH = "shaders/ar_unlit_object.vert"
        private const val FRAGMENT_SHADER_ASSET_PATH = "shaders/ar_unlit_object.frag"
    }
}
