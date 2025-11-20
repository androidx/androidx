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

package androidx.xr.arcore.apps.whitebox.mobile.geospatial

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.Plane
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.maybeThrowGLException
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.BackgroundRenderer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.PlaneRenderer
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import java.io.IOException

/** Renders the Geospatial Activity scene. */
class GeospatialRenderer(private val session: Session, private val anchors: MutableList<Anchor>) :
    SampleRender.Companion.Renderer {

    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var planeRenderer: PlaneRenderer
    private var image: EGLImageKHR? = null
    private lateinit var virtualSceneFramebuffer: Framebuffer

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private var scaleMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)
    private var modelViewMatrix = FloatArray(16) // view x model
    private var modelViewProjectionMatrix = FloatArray(16) // projection x view x model
    private var rotationMatrix = FloatArray(16)
    private var rotationModelMatrix = FloatArray(16)

    // Virtual object (ARCore geospatial)
    private lateinit var virtualObjectMesh: Mesh
    private lateinit var geospatialAnchorVirtualObjectShader: Shader

    override fun onSurfaceCreated(render: SampleRender) {
        try {
            backgroundRenderer = BackgroundRenderer(render)
            planeRenderer = PlaneRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, width = 1, height = 1)

            // Virtual object to render geospatial anchors.
            val virtualObjectTexture =
                Texture.createFromAsset(
                    render,
                    SPATIAL_MARKER_TEXTURE_ASSET_PATH,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB,
                )

            virtualObjectMesh = Mesh.createFromAsset(render, GEOSPATIAL_MARKER_MESH_ASSET_PATH)
            geospatialAnchorVirtualObjectShader =
                Shader.createFromAssets(
                        render,
                        UNLIT_OBJECT_VERTEX_SHADER_ASSET_PATH,
                        UNLIT_OBJECT_FRAGMENT_SHADER_ASSET_PATH,
                        defines = null,
                    )
                    .setTexture("u_Texture", virtualObjectTexture)
        } catch (e: IOException) {
            Log.e(GeospatialActivity.ACTIVITY_NAME, "Failed to create background renderer", e)
            return
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        (session.runtimes.filterIsInstance<ArCoreRuntime>().first().perceptionManager)
            .setDisplayRotation(Surface.ROTATION_0, width, height)
        virtualSceneFramebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        try {
            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
        } catch (e: IOException) {
            Log.e(GeospatialActivity.ACTIVITY_NAME, "Failed to read a required asset file", e)
            return
        }

        val cameraState = session.state.value.cameraState
        if (cameraState != null && cameraState.transformCoordinates2D != null) {
            backgroundRenderer.updateDisplayGeometry(cameraState.transformCoordinates2D!!)
        }
        if (cameraState?.trackingState == TrackingState.TRACKING) {
            if (image != null) {
                EGLExt.eglDestroyImageKHR(EGL14.eglGetCurrentDisplay(), image!!)
            }
            image =
                EGLExt.eglCreateImageFromHardwareBuffer(
                    EGL14.eglGetCurrentDisplay(),
                    cameraState.hardwareBuffer!!,
                )
            maybeThrowGLException(
                "Failed to create image from hardware buffer",
                "eglCreateImageFromHardwareBuffer",
            )
        }
        if (image != null) {
            GLES30.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                backgroundRenderer.cameraColorTexture.textureId,
            )
            maybeThrowGLException("Failed to bind texture", "glBindTexture")
            EGLExt.glEGLImageTargetTexture2DOES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, image!!)
            maybeThrowGLException(
                "Failed to set image target texture",
                "glEGLImageTargetTexture2DOES",
            )
            backgroundRenderer.drawBackground(render)
        }

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        if (cameraState?.trackingState != TrackingState.TRACKING) {
            return
        }

        projectionMatrix =
            checkNotNull(cameraState.projectionMatrix) { "cameraState.projectionMatrix is null" }
                .copy()
                .data
        val viewMatrix =
            checkNotNull(cameraState.viewMatrix) { "cameraState.viewMatrix is null" }.copy().data
        planeRenderer.drawPlanes(
            render,
            Plane.subscribe(session).value,
            checkNotNull(cameraState.displayOrientedPose) {
                "cameraState.displayOrientedPose is null"
            },
            projectionMatrix,
        )

        for (anchor in anchors) {
            if (anchor.state.value.trackingState != TrackingState.TRACKING) {
                continue
            }
            // Create the matrix from the anchor's pose.
            val modelMatrix = Matrix4.fromPose(anchor.state.value.pose).data
            Matrix.setIdentityM(scaleMatrix, 0)
            val scale =
                anchorScale(
                    anchor.state.value.pose,
                    checkNotNull(cameraState.displayOrientedPose) {
                        "cameraState.displayOrientedPose is null"
                    },
                )
            scaleMatrix[0] = scale
            scaleMatrix[5] = scale
            scaleMatrix[10] = scale
            Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0)
            // Rotate the virtual object 180 degrees around the Y axis to make the object face the
            // GL
            // camera -Z axis, since camera Z axis faces toward users.
            Matrix.setRotateM(rotationMatrix, 0, 180.0f, 0.0f, 1.0f, 0.0f)
            Matrix.multiplyMM(rotationModelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
            // Calculate model/view/projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, rotationModelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            geospatialAnchorVirtualObjectShader.setMat4(
                UNIFORM_MODEL_VIEW_PROJECTION,
                modelViewProjectionMatrix,
            )
            render.draw(
                virtualObjectMesh,
                geospatialAnchorVirtualObjectShader,
                virtualSceneFramebuffer,
            )
        }
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    // Clean up EGL resources when the renderer is no longer needed.
    fun release() {
        if (image != null) {
            EGLExt.eglDestroyImageKHR(EGL14.eglGetCurrentDisplay(), image!!)
            image = null
        }
    }

    // Return the scale in range [1, 2] after mapping a distance between camera and anchor to [2,
    // 20].
    private fun anchorScale(anchorPose: Pose, cameraPose: Pose): Float {
        val distance = Pose.distance(anchorPose, cameraPose)
        val mapDistance = Math.min(Math.max(2.0f, distance), 20.0f)
        return ((mapDistance - 2) / (20 - 2) + 1)
    }

    companion object {
        const val Z_NEAR = 0.1f
        const val Z_FAR = 100.0f

        const val SPATIAL_MARKER_TEXTURE_ASSET_PATH = "textures/spatial_marker_baked.png"
        const val GEOSPATIAL_MARKER_MESH_ASSET_PATH = "models/geospatial_marker.obj"
        const val UNLIT_OBJECT_VERTEX_SHADER_ASSET_PATH = "shaders/ar_unlit_object.vert"
        const val UNLIT_OBJECT_FRAGMENT_SHADER_ASSET_PATH = "shaders/ar_unlit_object.frag"
        const val UNIFORM_MODEL_VIEW_PROJECTION = "u_ModelViewProjection"
    }
}
