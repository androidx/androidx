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

package androidx.xr.arcore.apps.whitebox.mobile.anchorsplaneshittest

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.MotionEvent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR
import androidx.window.layout.WindowMetricsCalculator
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.Plane
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
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers.PlaneRenderer
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.UnsupportedArCoreCompatApi
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Config
import androidx.xr.runtime.Log
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Activity to test the Anchor APIs. */
class AnchorsPlanesHitTestActivity :
    ComponentActivity(), DefaultLifecycleObserver, SampleRender.Companion.Renderer {
    companion object {
        private const val TAG = "AnchorsPlanesHitTestActivity"
        private const val MAX_HIT_TEST_RESULTS = 10
    }

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var renderer: SampleRender
    private lateinit var planeRenderer: PlaneRenderer
    private lateinit var virtualSceneFramebuffer: Framebuffer
    private lateinit var updatePlanesJob: Job
    private var screenWidth: Int = 1
    private var screenHeight: Int = 1
    private var image: EGLImageKHR? = null
    private val foundPlanes = mutableStateListOf<Plane>()
    private var foundHits = MutableStateFlow<List<HitResult>>(mutableStateListOf<HitResult>())
    private val anchors = mutableStateListOf<Anchor>()
    private val arCoreVerificationHelper: ArCoreVerificationHelper =
        ArCoreVerificationHelper(this, onArCoreVerified = { sessionHelper.tryCreateSession() })
    private val queuedTaps: ArrayBlockingQueue<MotionEvent> = ArrayBlockingQueue(16)

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
                Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL),
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

        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val currentBounds = windowMetrics.bounds

        screenWidth = currentBounds.width()
        screenHeight = currentBounds.height()
    }

    override fun onResume() {
        super<ComponentActivity>.onResume()
        if (::session.isInitialized) {
            val supervisorJob = SupervisorJob()
            val scope = CoroutineScope(supervisorJob + lifecycleScope.coroutineContext)
            updatePlanesJob = scope.launch { Plane.subscribe(session).collect { updatePlanes(it) } }
        }
        if (::surfaceView.isInitialized) {
            surfaceView.onResume()
            surfaceView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    queuedTaps.add(event)
                }
                true
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super<ComponentActivity>.onPause()
        if (::surfaceView.isInitialized) {
            surfaceView.onPause()
        }
        if (::updatePlanesJob.isInitialized) {
            updatePlanesJob.cancel()
        }
    }

    private fun updatePlanes(planes: Collection<Plane>) {
        foundPlanes.clear()
        foundPlanes.addAll(planes)
    }

    override fun onSurfaceCreated(render: SampleRender) {
        try {
            backgroundRenderer = BackgroundRenderer(render)
            planeRenderer = PlaneRenderer(render)
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
        try {
            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
        } catch (e: IOException) {
            Log.error(e) { "Failed to read a required asset file" }
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

            planeRenderer.drawPlanes(
                render,
                Plane.subscribe(session).value,
                checkNotNull(cameraState.displayOrientedPose) {
                    "cameraState.displayOrientedPose is null"
                },
                projectionMatrix.copy().data,
            )

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

            val drainedTaps = ArrayList<MotionEvent>()
            queuedTaps.drainTo(drainedTaps)
            for (tap in drainedTaps) {
                getHits(tap.x, tap.y)
            }

            for (hit in foundHits.value) {
                addAnchor(hit.hitPose)
            }
            foundHits.value = emptyList() // So we don't keep duplicating anchors

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
                    Button(onClick = { deleteAllAnchors() }, enabled = hasCameraTracking) {
                        Text(
                            text =
                                if (hasCameraTracking) "Clear anchors" else "Camera not tracking",
                            fontSize = 15.sp,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(color = Color.White).fillMaxWidth().padding(innerPadding)
            ) {
                planesAndHitsInfo()
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { _ -> surfaceView })
            }
        }
    }

    @Composable
    private fun planesAndHitsInfo() {
        Column(
            modifier = Modifier.background(color = Color.White).fillMaxWidth().fillMaxHeight(.15f)
        ) {
            for (hitResult in foundHits.value) {
                Text(
                    text =
                        "Hit Result: ${hitResult.trackable.hashCode()} - ${hitResult.hitPose.translation}",
                    fontSize = 14.sp,
                )
            }
            for (plane in foundPlanes) {
                Text(
                    text =
                        "Tracking Plane: ${plane.hashCode()} - ${plane.state.value.trackingState} - ${plane.type} - ${plane.state.value.label}",
                    fontSize = 14.sp,
                )
            }
        }
    }

    private fun addAnchor(anchorPose: Pose) {
        val anchorResult =
            try {
                Anchor.create(session, anchorPose)
            } catch (e: IllegalStateException) {
                Log.error(e) { "Failed to create anchor: ${e.message}" }
                return
            }
        if (anchorResult !is AnchorCreateSuccess) {
            Log.error { "Failed to create anchor: ${anchorResult::class.simpleName}" }
            return
        }
        anchors.add(anchorResult.anchor)
    }

    private fun deleteAllAnchors() {
        for (anchor in anchors) {
            anchor.detach()
        }
        anchors.clear()
    }

    @OptIn(UnsupportedArCoreCompatApi::class)
    private fun getHits(x: Float, y: Float) {
        if (lifecycle.currentStateFlow.value != Lifecycle.State.RESUMED) {
            return
        }
        val cameraState = session.state.value.cameraState
        if (cameraState == null || cameraState.displayOrientedPose == null) {
            return
        }
        val pose = cameraState.displayOrientedPose!!
        var projMatrix = FloatArray(16)
        var viewMatrix = FloatArray(16)
        projMatrix = cameraState.projectionMatrix!!.data
        viewMatrix = cameraState.viewMatrix!!.data

        val vpMatrix = FloatArray(16)
        val invVpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invVpMatrix, 0, vpMatrix, 0)

        val xNDC = (x / screenWidth) * 2 - 1
        val yNDC = 1 - (y / screenHeight) * 2
        val touchNDC = floatArrayOf(xNDC, yNDC, -1f, 1f)
        val worldPointNear = FloatArray(4)
        Matrix.multiplyMV(worldPointNear, 0, invVpMatrix, 0, touchNDC, 0)

        val nearX = worldPointNear[0] / worldPointNear[3]
        val nearY = worldPointNear[1] / worldPointNear[3]
        val nearZ = worldPointNear[2] / worldPointNear[3]

        val cameraPose = cameraState.displayOrientedPose
        val direction =
            Vector3(
                nearX - cameraPose!!.translation.x,
                nearY - cameraPose!!.translation.y,
                nearZ - cameraPose!!.translation.z,
            )

        val ray = Ray(cameraPose.translation, direction)
        val hitResults = hitTest(session, ray)
        if (hitResults.isNotEmpty()) {
            val newHits = mutableStateListOf<HitResult>()
            newHits.addAll(hitResults)
            while (newHits.size > MAX_HIT_TEST_RESULTS) {
                newHits.removeAt(0)
            }
            foundHits.value = newHits
        }
    }
}
