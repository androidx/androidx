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

package androidx.xr.arcore.apps.whitebox.mobile.depthmaps

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.xr.arcore.DepthMap
import androidx.xr.arcore.apps.whitebox.mobile.common.ArCoreVerificationHelper
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Framebuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.maybeThrowGLException
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.BackgroundRenderer
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.Log
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.FloatBuffer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DepthMode {
    RAW,
    SMOOTH,
}

class DepthMapsActivity :
    ComponentActivity(), DefaultLifecycleObserver, SampleRender.Companion.Renderer {
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var arSession: com.google.ar.core.Session
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var renderer: SampleRender
    private lateinit var virtualSceneFramebuffer: Framebuffer
    private var image: EGLImageKHR? = null

    private val arCoreVerificationHelper: ArCoreVerificationHelper =
        ArCoreVerificationHelper(this, onArCoreVerified = { sessionHelper.tryCreateSession() })

    var viewMatrix = Matrix4.Zero
    var projectionMatrix = Matrix4.Zero

    private var selectedDepthMode by mutableStateOf(DepthMode.RAW)

    private val rawConfig = Config(depthEstimation = DepthEstimationMode.RAW_ONLY)
    private val smoothConfig = Config(depthEstimation = DepthEstimationMode.SMOOTH_ONLY)
    private var configurationMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        lifecycle.addObserver(arCoreVerificationHelper)
        sessionHelper =
            SessionLifecycleHelper(
                this,
                rawConfig,
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
        } catch (e: IOException) {
            Log.error(e) { "Failed to create background renderer" }
            return
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        (session.runtimes.filterIsInstance<ArCoreRuntime>().first().perceptionManager)
            .setDisplayRotation(Surface.ROTATION_0, width, height)
        virtualSceneFramebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        if (!::backgroundRenderer.isInitialized) {
            return
        }
        var depthImageNotAvailable = true
        runBlocking {
            configurationMutex.withLock {
                var floatBuffer: FloatBuffer? = null
                var currentDepthMap: DepthMap? = null
                try {
                    currentDepthMap = DepthMap.mono(session)
                    if (currentDepthMap != null) {
                        floatBuffer =
                            if (selectedDepthMode == DepthMode.RAW)
                                currentDepthMap!!.state.value.rawDepthMap
                            else currentDepthMap!!.state.value.smoothDepthMap
                    }
                    backgroundRenderer.apply {
                        val useDepthVisualization = floatBuffer != null
                        setUseDepthVisualization(render, useDepthVisualization)
                        setUseOcclusion(render, false)

                        if (useDepthVisualization) {
                            updateCameraDepthTexture(
                                currentDepthMap!!.state.value.width,
                                currentDepthMap!!.state.value.height,
                                floatBuffer,
                            )
                        }
                    }
                    depthImageNotAvailable = false
                } catch (e: IOException) {
                    Log.error(e) { "Failed to read a required asset file" }
                } catch (e: NotYetAvailableException) {
                    Log.error(e) {
                        "Depth image is not yet available, unable to retrieve depth map buffers."
                    }
                } catch (e: DeadlineExceededException) {
                    Log.error(e) {
                        "Depth image DeadlineExceededException, unable to retrieve depth map buffers."
                    }
                }
            }
        }

        if (depthImageNotAvailable) {
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
                    Button(
                        onClick = {
                            runBlocking {
                                configurationMutex.withLock {
                                    when (selectedDepthMode) {
                                        DepthMode.RAW -> {
                                            session.configure(smoothConfig)
                                            selectedDepthMode = DepthMode.SMOOTH
                                        }

                                        DepthMode.SMOOTH -> {
                                            session.configure(rawConfig)
                                            selectedDepthMode = DepthMode.RAW
                                        }
                                    }
                                }
                            }
                        },
                        enabled = hasCameraTracking,
                    ) {
                        Text(
                            text =
                                if (hasCameraTracking)
                                    (if (selectedDepthMode == DepthMode.RAW) "Smooth Toggle"
                                    else "Raw Toggle")
                                else "Camera not tracking",
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
                // Depth Visualizations
                Row(modifier = Modifier.fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { _ -> surfaceView },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "DepthMapsActivity"
    }
}
