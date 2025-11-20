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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers

import android.opengl.GLES30
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * This class both renders the AR camera background and composes the scene foreground. The camera
 * background can be rendered as either camera image data or camera depth data. The virtual scene
 * can be composited with or without depth occlusion.
 */
public class BackgroundRenderer(render: SampleRender) {

    private val mesh: Mesh

    private val cameraTexCoordsVertexBuffer: VertexBuffer

    private var backgroundShader: Shader? = null

    private var occlusionShader: Shader? = null

    public val cameraDepthTexture: Texture

    public val cameraColorTexture: Texture

    private lateinit var depthColorPaletteTexture: Texture

    private var useDepthVisualization: Boolean = false

    private var useOcclusion: Boolean = false

    private var aspectRatio: Float = 0.0f

    /**
     * Allocates and initializes OpenGL resources needed by the background renderer. Must be called
     * during a [SampleRender.Renderer] callback, typically in
     * [SampleRender.Renderer.onSurfaceCreated].
     */
    init {
        cameraColorTexture =
            Texture(
                render,
                Texture.Target.TEXTURE_EXTERNAL_OES,
                Texture.WrapMode.CLAMP_TO_EDGE,
                useMipmaps = false,
            )
        cameraDepthTexture =
            Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                useMipmaps = false,
            )

        // Create a [Mesh] with three vertex buffers: one for the screen coordinates (normalized
        // device
        // coordinates), one for the camera texture coordinates (to be populated with proper data
        // later
        // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
        val screenCoordsVertexBuffer: VertexBuffer =
            VertexBuffer(numberOfEntriesPerVertex = 2, NDC_QUAD_COORDS_BUFFER)
        cameraTexCoordsVertexBuffer = VertexBuffer(numberOfEntriesPerVertex = 2, entries = null)
        val virtualSceneTexCoordsVertexBuffer: VertexBuffer =
            VertexBuffer(numberOfEntriesPerVertex = 2, VIRTUAL_SCENE_TEX_COORDS_BUFFER)
        val vertexBuffers: Array<VertexBuffer> =
            arrayOf(
                screenCoordsVertexBuffer,
                cameraTexCoordsVertexBuffer,
                virtualSceneTexCoordsVertexBuffer,
            )
        mesh = Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, indexBuffer = null, vertexBuffers)
    }

    private val cameraTexCoords: FloatBuffer =
        ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer()

    /**
     * Sets whether the background camera image should be replaced with a depth visualization
     * instead. This reloads the corresponding shader code, and must be called on the GL thread.
     */
    public fun setUseDepthVisualization(render: SampleRender, useDepthVisualization: Boolean) {
        if (backgroundShader != null) {
            if (this.useDepthVisualization == useDepthVisualization) {
                return
            }
            backgroundShader!!.close()
            backgroundShader = null
            this.useDepthVisualization = useDepthVisualization
        }
        if (useDepthVisualization) {

            depthColorPaletteTexture =
                Texture.createFromAsset(
                    render,
                    "textures/depth_color_palette.png",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.LINEAR,
                )
            backgroundShader =
                Shader.createFromAssets(
                        render,
                        "shaders/background_show_depth_color_visualization.vert",
                        "shaders/background_show_depth_color_visualization.frag",
                        defines = null,
                    )
                    .setTexture("u_CameraDepthTexture", cameraDepthTexture)
                    .setTexture("u_ColorMap", depthColorPaletteTexture)
                    .setDepthTest(false)
                    .setDepthWrite(false)
        } else {
            backgroundShader =
                Shader.createFromAssets(
                        render,
                        "shaders/background_show_camera.vert",
                        "shaders/background_show_camera.frag",
                        defines = null,
                    )
                    .setTexture("u_CameraColorTexture", cameraColorTexture)
                    .setDepthTest(false)
                    .setDepthWrite(false)
        }
    }

    public fun setUseOcclusion(render: SampleRender, useOcclusion: Boolean) {
        if (occlusionShader != null) {
            if (this.useOcclusion == useOcclusion) {
                return
            }
            occlusionShader!!.close()
            occlusionShader = null
            this.useOcclusion = useOcclusion
        }
        val defines = mutableMapOf<String, String>()
        defines.put("USE_OCCLUSION", if (useOcclusion) "1" else "0")
        occlusionShader =
            Shader.createFromAssets(
                    render,
                    "shaders/occlusion.vert",
                    "shaders/occlusion.frag",
                    defines,
                )
                .setDepthTest(false)
                .setDepthWrite(false)
                .setBlend(Shader.BlendFactor.SRC_ALPHA, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA)
        if (useOcclusion) {
            occlusionShader!!
                .setTexture("u_CameraDepthTexture", cameraDepthTexture)
                .setFloat("u_DepthAspectRatio", aspectRatio)
        }
    }

    /**
     * Updates the display geometry. This must be called every frame before calling either of
     * BackgroundRenderer's draw methods.
     *
     * @param transformFunc How to process the display's [FloatBuffer]
     */
    public fun updateDisplayGeometry(transformFunc: (FloatBuffer) -> FloatBuffer) {
        // If display rotation changed (also includes view size change), we need to re-query the UV
        // coordinates for the screen rect, as they may have changed as well.
        cameraTexCoordsVertexBuffer.set(transformFunc(NDC_QUAD_COORDS_BUFFER))
    }

    /** Update depth texture with Float Buffer contents. */
    public fun updateCameraDepthTexture(width: Int, height: Int, floatBuffer: FloatBuffer) {
        // SampleRender abstraction leaks here
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraDepthTexture.textureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_R32F,
            width,
            height,
            0,
            GLES30.GL_RED,
            GLES30.GL_FLOAT,
            floatBuffer,
        )
        if (useOcclusion) {
            val aspectRatio: Float = 1.0f * width / height
            occlusionShader!!.setFloat("u_DepthAspectRatio", aspectRatio)
        }
    }

    /**
     * Draws the AR background image. The image will be drawn such that virtual content rendered
     * with the matrices provided by [CameraState.viewMatrix] and [CameraState.projectionMatrix]
     * will accurately follow static physical objects.
     */
    public fun drawBackground(render: SampleRender) {
        backgroundShader?.let { render.draw(mesh, it) }
    }

    /**
     * Draws the virtual scene. Any objects rendered in the given [Framebuffer] will be drawn given
     * the previously specified [OcclusionMode].
     *
     * Virtual content should be rendered using the matrices provided by [CameraState.viewMatrix]
     * and [CameraState.projectionMatrix].
     */
    public fun drawVirtualScene(
        render: SampleRender,
        virtualSceneFramebuffer: Framebuffer,
        zNear: Float,
        zFar: Float,
    ) {
        if (occlusionShader != null) {
            occlusionShader!!.setTexture(
                "u_VirtualSceneColorTexture",
                virtualSceneFramebuffer.colorTexture,
            )
            if (useOcclusion) {
                occlusionShader!!
                    .setTexture("u_VirtualSceneDepthTexture", virtualSceneFramebuffer.depthTexture)
                    .setFloat("u_ZNear", zNear)
                    .setFloat("u_ZFar", zFar)
            }
            render.draw(mesh, occlusionShader!!)
        }
    }

    companion object {
        private const val TAG: String = "BackgroundRenderer"

        // components_per_vertex * number_of_vertices * float_size
        private const val COORDS_BUFFER_SIZE: Int = 2 * 4 * 4

        private val NDC_QUAD_COORDS_BUFFER: FloatBuffer =
            ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        private val VIRTUAL_SCENE_TEX_COORDS_BUFFER: FloatBuffer =
            ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        init {
            NDC_QUAD_COORDS_BUFFER.put(
                floatArrayOf(/*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f)
            )
            VIRTUAL_SCENE_TEX_COORDS_BUFFER.put(
                floatArrayOf(/*0:*/ 0f, 0f, /*1:*/ 1f, 0f, /*2:*/ 0f, 1f, /*3:*/ 1f, 1f)
            )
        }
    }
}
