/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.xr.arcore.apps.whitebox.depthmaps

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.xr.arcore.PerceptionState
import androidx.xr.arcore.apps.whitebox.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.depthmaps.rendering.DepthMapRenderer
import androidx.xr.arcore.apps.whitebox.depthmaps.rendering.DepthTextureHandler
import androidx.xr.arcore.perceptionState
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sample that demonstrates usage of depth map data provided by JXR ARCore API. */
class DepthMapActivity : ComponentActivity(), GLSurfaceView.Renderer {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper

    private lateinit var surfaceView: GLSurfaceView
    private val depthTexture: DepthTextureHandler = DepthTextureHandler()
    private val depthMapRenderer: DepthMapRenderer = DepthMapRenderer()
    private var selectedDepthMode by mutableStateOf(DepthMode.RAW)
    private var selectedView by mutableStateOf(ViewSelection.LEFT)
    private val rawConfig =
        Config(
            depthEstimation = Config.DepthEstimationMode.RAW_ONLY,
            headTracking = Config.HeadTrackingMode.LAST_KNOWN,
        )
    private val smoothConfig =
        Config(
            depthEstimation = Config.DepthEstimationMode.SMOOTH_ONLY,
            headTracking = Config.HeadTrackingMode.LAST_KNOWN,
        )
    private var configurationMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        // Create session and renderers.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                rawConfig,
                onSessionAvailable = { session ->
                    this.session = session
                    // Set up renderer.
                    surfaceView = GLSurfaceView(this)
                    surfaceView.setPreserveEGLContextOnPause(true)
                    surfaceView.setEGLContextClientVersion(2)
                    surfaceView.setEGLConfigChooser(
                        8,
                        8,
                        8,
                        8,
                        16,
                        0,
                    ) // Alpha used for plane blending.
                    surfaceView.setRenderer(this)
                    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
                    surfaceView.setWillNotDraw(false)
                    setContent { DepthMapPanel(surfaceView) }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onResume() {
        super<ComponentActivity>.onResume()
        if (::surfaceView.isInitialized) {
            surfaceView.onResume()
        }
    }

    override fun onPause() {
        super<ComponentActivity>.onPause()
        if (::surfaceView.isInitialized) {
            surfaceView.onPause()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an exception.
        try {
            depthTexture.createOnGlThread()
            depthMapRenderer.createDepthGradientTexture(/* context= */ this)
            depthMapRenderer.createDepthShaders(/* context= */ this, depthTexture.depthTextureId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Get the latest depth from the session
        runBlocking {
            configurationMutex.withLock {
                val perceptionState: PerceptionState? = session.state.value.perceptionState
                if (perceptionState != null) {
                    val depthMap =
                        when (selectedView) {
                            ViewSelection.LEFT ->
                                session.state.value.perceptionState!!.depthMaps[LEFT_VIEW]
                            ViewSelection.RIGHT ->
                                session.state.value.perceptionState!!.depthMaps[RIGHT_VIEW]
                        }
                    depthTexture.updateDepthTexture(depthMap.state.value, selectedDepthMode)
                    depthMapRenderer.drawDepth()
                }
            }
        }
    }

    @Composable
    fun DepthMapPanel(view: View) {
        Subspace {
            SpatialPanel(modifier = SubspaceModifier.movable()) {
                AndroidView(
                    modifier = Modifier.width(1200.dp).height(1200.dp),
                    factory = { _ -> surfaceView },
                )
                Orbiter(
                    position = ContentEdge.Top,
                    offset = 8.dp,
                    shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
                    offsetType = OrbiterOffsetType.InnerEdge,
                ) {
                    Row(modifier = Modifier) {
                        BackToMainActivityButton()
                        val depthDescription: String =
                            when (selectedDepthMode) {
                                DepthMode.RAW ->
                                    when (selectedView) {
                                        ViewSelection.LEFT -> "Left Smooth"
                                        ViewSelection.RIGHT -> "Right Smooth"
                                    }
                                DepthMode.SMOOTH ->
                                    when (selectedView) {
                                        ViewSelection.LEFT -> "Left Raw"
                                        ViewSelection.RIGHT -> "Right Raw"
                                    }
                            }
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "Rendering ${depthDescription} Depth",
                            color = Color.White,
                            fontSize = 50.sp,
                        )
                        Button(
                            modifier = Modifier.padding(8.dp),
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
                        ) {
                            Text("Toggle Depth Type")
                        }
                        Button(
                            modifier = Modifier.padding(8.dp),
                            onClick = {
                                selectedView =
                                    when (selectedView) {
                                        ViewSelection.LEFT -> ViewSelection.RIGHT
                                        ViewSelection.RIGHT -> ViewSelection.LEFT
                                    }
                            },
                        ) {
                            Text("Toggle View")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG: String = "DepthMapActivity"
        private const val LEFT_VIEW: Int = 0
        private const val RIGHT_VIEW: Int = 1
    }
}

enum class DepthMode {
    RAW,
    SMOOTH,
}

enum class ViewSelection {
    LEFT,
    RIGHT,
}
