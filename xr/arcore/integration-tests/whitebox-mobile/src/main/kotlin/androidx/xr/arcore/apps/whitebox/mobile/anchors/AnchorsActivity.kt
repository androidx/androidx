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

package androidx.xr.arcore.apps.whitebox.mobile.anchors

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.apps.whitebox.mobile.common.ArCoreVerificationHelper
import androidx.xr.arcore.apps.whitebox.mobile.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.maybeThrowGLException
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.BackgroundRenderer
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import java.io.IOException

/** Activity to test the Anchor APIs. */
class AnchorsActivity :
    ComponentActivity(), DefaultLifecycleObserver, SampleRender.Companion.Renderer {
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var renderer: SampleRender
    private lateinit var virtualSceneFramebuffer: Framebuffer
    private var image: EGLImageKHR? = null
    private val anchors = mutableStateListOf<Anchor>()
    private val arCoreVerificationHelper: ArCoreVerificationHelper =
        ArCoreVerificationHelper(this, onArCoreVerified = { sessionHelper.tryCreateSession() })

    // Virtual object (ARCore pawn)
    private lateinit var virtualObjectMesh: Mesh
    private lateinit var virtualObjectShader: Shader
    private lateinit var virtualObjectAlbedoTexture: Texture

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    var modelMatrix = Matrix4.Zero
    var viewMatrix = Matrix4.Zero
    var projectionMatrix = Matrix4.Zero
    var modelViewMatrix = Matrix4.Zero // view x model
    var modelViewProjectionMatrix = Matrix4.Zero // projection x view x model

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        lifecycle.addObserver(arCoreVerificationHelper)
        sessionHelper =
            SessionLifecycleHelper(
                this,
                onSessionAvailable = { session ->
                    this.session = session
                    surfaceView = GLSurfaceView(this)
                    renderer = SampleRender(surfaceView, this, assets)
                    setContent { MainPanel() }
                },
                onSessionCreateActionRequired = { result ->
                    arCoreVerificationHelper.handleSessionCreateActionRequired(result)
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (::surfaceView.isInitialized) {
            surfaceView.onResume()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        if (::surfaceView.isInitialized) {
            surfaceView.onPause()
        }
    }

    override fun onSurfaceCreated(render: SampleRender) {
        try {
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, width = 1, height = 1)

            // Virtual object to render (ARCore pawn)
            virtualObjectAlbedoTexture =
                Texture.createFromAsset(
                    render,
                    "textures/pawn_albedo.png",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB,
                )

            virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj")

            virtualObjectShader =
                Shader.createFromAssets(
                        render,
                        "shaders/model_view.vert",
                        "shaders/model_view.frag",
                        defines = null,
                    )
                    .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
        } catch (e: IOException) {
            Log.v(ACTIVITY_NAME, "Failed to create background renderer", e)
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
            Log.e(ACTIVITY_NAME, "Failed to read a required asset file", e)
            return
        }

        if (!::backgroundRenderer.isInitialized) {
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

            // If not tracking, don't draw 3D objects.
            if (cameraState.trackingState == TrackingState.PAUSED) {
                return
            }

            // Get projection matrix.
            projectionMatrix = cameraState.projectionMatrix!!
            viewMatrix = cameraState.viewMatrix!!

            // Visualize anchors
            render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
            for (anchor in anchors) {
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                modelMatrix = Matrix4.fromPose(anchor.runtimeAnchor.pose)

                // Calculate model/view/projection matrices
                modelViewMatrix = viewMatrix * modelMatrix
                modelViewProjectionMatrix = projectionMatrix * modelViewMatrix

                // Update shader properties and draw
                virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix.data)
                render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
            }

            // Draw the virtual scene.
            backgroundRenderer.drawVirtualScene(
                render,
                virtualSceneFramebuffer,
                zNear = 0.01f,
                zFar = 100.0f,
            )
        }
    }

    @Composable
    private fun MainPanel() {
        val state by session.state.collectAsStateWithLifecycle()
        val cameraState = state.cameraState
        val hasCameraTracking = cameraState?.trackingState == TrackingState.TRACKING

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Button(
                        onClick = { addAnchor(cameraState?.cameraPose!!) },
                        enabled = hasCameraTracking,
                    ) {
                        Text(
                            text = if (hasCameraTracking) "Add anchor" else "Camera not tracking",
                            fontSize = 30.sp,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(innerPadding)
            ) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { _ -> surfaceView })
            }
        }
    }

    private fun addAnchor(anchorPose: Pose) {
        val anchorResult =
            try {
                Anchor.create(session, anchorPose)
            } catch (e: IllegalStateException) {
                Log.e(ACTIVITY_NAME, "Failed to create anchor: ${e.message}")
                return
            }
        if (anchorResult !is AnchorCreateSuccess) {
            Log.e(ACTIVITY_NAME, "Failed to create anchor: ${anchorResult::class.simpleName}")
            return
        }
        anchors.add(anchorResult.anchor)
    }

    private fun deleteAnchor(anchor: Anchor) {
        anchor.detach()
        anchors.remove(anchor)
    }

    companion object {
        const val ACTIVITY_NAME = "AnchorsActivity"
    }
}
