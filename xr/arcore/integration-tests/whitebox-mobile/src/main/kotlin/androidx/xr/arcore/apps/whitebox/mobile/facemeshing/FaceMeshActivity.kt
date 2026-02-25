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

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR
import androidx.xr.arcore.Face
import androidx.xr.arcore.FaceMeshRegion
import androidx.xr.arcore.apps.whitebox.mobile.common.ArCoreVerificationHelper
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.maybeThrowGLException
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.BackgroundRenderer
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.CameraFacingDirection
import androidx.xr.runtime.Config
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Matrix4
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FaceMeshActivity : ComponentActivity(), SampleRender.Companion.Renderer {
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var updateJob: CompletableJob

    // OpenGL Rendering.
    private var image: EGLImageKHR? = null
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var framebuffer: Framebuffer
    private lateinit var renderContext: SampleRender
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var faceMeshRenderer: FaceMeshRenderer
    private lateinit var faceEffectsRenderer: FaceMeshRenderer
    private lateinit var noseTipRenderer: FaceObjectRenderer
    private lateinit var foreheadLeftRenderer: FaceObjectRenderer
    private lateinit var foreheadRightRenderer: FaceObjectRenderer

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private var viewMatrix = Matrix4.Zero
    private var projectionMatrix = Matrix4.Zero

    private val foundFaces = mutableStateListOf<Face>()
    private val arCoreVerificationHelper: ArCoreVerificationHelper =
        ArCoreVerificationHelper(this, onArCoreVerified = { sessionHelper.tryCreateSession() })

    private var renderStyle: RenderStyle = RenderStyle.MASK

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        lifecycle.addObserver(arCoreVerificationHelper)
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    faceTracking = FaceTrackingMode.MESHES,
                    cameraFacingDirection = CameraFacingDirection.USER,
                ),
                onSessionAvailable = { newSession ->
                    session = newSession
                    setContent { MainPanel() }
                },
                onSessionCreateActionRequired = { result ->
                    arCoreVerificationHelper.handleSessionCreateActionRequired(result)
                },
            )

        surfaceView = GLSurfaceView(this)
        renderContext = SampleRender(surfaceView, this, assets)

        sessionHelper.tryCreateSession()
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            updateJob =
                SupervisorJob(
                    lifecycleScope.launch {
                        Face.subscribe(session).collect {
                            foundFaces.clear()
                            foundFaces.addAll(it)
                        }
                    }
                )
        }
        if (::surfaceView.isInitialized) {
            surfaceView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::surfaceView.isInitialized) {
            surfaceView.onPause()
        }
        if (::updateJob.isInitialized) {
            updateJob.complete()
        }
    }

    @Composable
    private fun MainPanel() {
        var trackedFaceCount by remember { mutableIntStateOf(0) }
        trackedFaceCount = foundFaces.size

        Box(
            modifier =
                Modifier.fillMaxSize().pointerInput(renderStyle) {
                    detectTapGestures {
                        renderStyle =
                            when (renderStyle) {
                                RenderStyle.MASK -> RenderStyle.REGION_OBJECTS
                                RenderStyle.REGION_OBJECTS -> RenderStyle.MASK
                            }
                    }
                }
        ) {
            BackgroundImage(surfaceView)

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tap to change\nFound faces: $trackedFaceCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
            }
        }
    }

    @Composable
    fun BackgroundImage(view: GLSurfaceView) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { _ -> view })
    }

    override fun onSurfaceCreated(render: SampleRender) {
        framebuffer = Framebuffer(render, width = 1, height = 1)
        backgroundRenderer = BackgroundRenderer(renderContext)
        faceMeshRenderer = FaceMeshRenderer(renderContext, "textures/reference_face_texture.png")
        faceEffectsRenderer = FaceMeshRenderer(renderContext, "textures/freckles.png")
        noseTipRenderer =
            FaceObjectRenderer(renderContext, "models/nose.obj", "textures/nose_fur.png")
        foreheadLeftRenderer =
            FaceObjectRenderer(renderContext, "models/forehead_left.obj", "textures/ear_fur.png")
        foreheadRightRenderer =
            FaceObjectRenderer(renderContext, "models/forehead_right.obj", "textures/ear_fur.png")
        backgroundRenderer.setUseDepthVisualization(render, false)
        backgroundRenderer.setUseOcclusion(render, false)
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        (session.runtimes.first() as ArCoreRuntime)
            .perceptionManager
            .setDisplayRotation(Surface.ROTATION_0, width, height)
        framebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        val cameraState = session.state.value.cameraState
        val perceptionManager = (session.runtimes.first() as ArCoreRuntime).perceptionManager

        if (cameraState?.hardwareBuffer == null) {
            return
        }

        if (cameraState.transformCoordinates2D != null) {
            backgroundRenderer.updateDisplayGeometry(cameraState.transformCoordinates2D!!)
        }

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
        maybeThrowGLException("Failed to set image target texture", "glEGLImageTargetTexture2DOES")
        backgroundRenderer.drawBackground(render)

        // Get projection matrix.
        projectionMatrix = cameraState.projectionMatrix!!
        viewMatrix = cameraState.viewMatrix!!

        render.clear(framebuffer, 0f, 0f, 0f, 0f)

        // Visualize the first face detected by runtime. detailed mesh data won't be available for
        // any other detected faces if they exist.
        if (foundFaces.isNotEmpty()) {
            val faceState = foundFaces.first().state.value
            if (renderStyle == RenderStyle.MASK) {
                faceMeshRenderer.draw(foundFaces.first(), viewMatrix, projectionMatrix, framebuffer)
            } else {
                faceEffectsRenderer.draw(
                    foundFaces.first(),
                    viewMatrix,
                    projectionMatrix,
                    framebuffer,
                )
                noseTipRenderer.draw(
                    faceState.regionPoses!![FaceMeshRegion.NOSE_TIP]!!,
                    viewMatrix,
                    projectionMatrix,
                    framebuffer,
                )
                foreheadLeftRenderer.draw(
                    faceState.regionPoses!![FaceMeshRegion.FOREHEAD_LEFT]!!,
                    viewMatrix,
                    projectionMatrix,
                    framebuffer,
                )
                foreheadRightRenderer.draw(
                    faceState.regionPoses!![FaceMeshRegion.FOREHEAD_RIGHT]!!,
                    viewMatrix,
                    projectionMatrix,
                    framebuffer,
                )
            }
        }

        // Draw the virtual scene.
        backgroundRenderer.drawVirtualScene(render, framebuffer, 0.01f, 100.0f)
    }

    enum class RenderStyle {
        MASK,
        REGION_OBJECTS,
    }

    companion object {
        val ACTIVITY_NAME: String = FaceMeshActivity::class.java.name
    }
}
