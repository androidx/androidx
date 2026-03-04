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

import androidx.xr.arcore.Face
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.IndexBuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.VertexBuffer
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Matrix4
import java.io.IOException

class FaceMeshRenderer(val render: SampleRender, textureAssetPath: String) {
    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private var modelMatrix = Matrix4.Zero
    private var modelViewMatrix = Matrix4.Zero
    private var modelViewProjectionMatrix = Matrix4.Zero

    private lateinit var faceMeshShader: Shader
    private lateinit var renderedFaceMesh: Mesh

    init {
        try {
            val faceMeshReferenceTexture =
                Texture.createFromAsset(
                    render,
                    textureAssetPath,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB,
                )

            faceMeshShader =
                Shader.createFromAssets(
                        render,
                        MESH_OBJECT_VERTEX_SHADER_ASSET_PATH,
                        MESH_OBJECT_FRAGMENT_SHADER_ASSET_PATH,
                        null,
                    )
                    .setTexture("u_Texture", faceMeshReferenceTexture)
                    .setCullFace(false)
                    .setDepthWrite(false)
        } catch (e: IOException) {
            XrLog.error(e) { "Failed to initialize FaceMesh assets" }
        }
    }

    fun draw(
        faceMesh: Face,
        viewMatrix: Matrix4,
        projectionMatrix: Matrix4,
        framebuffer: Framebuffer,
    ) {
        val faceState = faceMesh.state.value
        renderedFaceMesh =
            Mesh(
                render,
                Mesh.PrimitiveMode.TRIANGLES,
                IndexBuffer(render, faceState.mesh!!.triangleIndices),
                arrayOf(
                    VertexBuffer(3, faceState.mesh!!.vertices),
                    VertexBuffer(3, faceState.mesh!!.normals),
                    VertexBuffer(2, faceState.mesh!!.textureCoordinates),
                ),
            )

        // render the face mesh
        modelMatrix = Matrix4.fromPose(faceState.centerPose!!)
        modelViewProjectionMatrix = projectionMatrix * viewMatrix * modelMatrix
        modelViewMatrix = viewMatrix * modelMatrix

        faceMeshShader.setMat4("u_ModelView", modelViewMatrix.data)
        faceMeshShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix.data)
        render.draw(renderedFaceMesh, faceMeshShader, framebuffer)
    }

    companion object {
        private val TAG = FaceMeshRenderer::class.java.name

        // Shader names
        private const val MESH_OBJECT_VERTEX_SHADER_ASSET_PATH = "shaders/object.vert"
        private const val MESH_OBJECT_FRAGMENT_SHADER_ASSET_PATH = "shaders/object.frag"
    }
}
