/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testapp.lifecycle

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateNotAuthorized
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateResult
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorCreateTrackingUnavailable
import androidx.xr.arcore.AnchorLoadInvalidUuid
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.composables.BasicLayout
import androidx.xr.compose.testapp.common.composables.TestResult
import androidx.xr.compose.testapp.common.composables.TestResultsDisplay
import androidx.xr.compose.testapp.common.composables.addTestResult
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Paths
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Testing if the session lifecycle fires with the activity lifecycle by creating
 * an anchor when the activity is RESUMED
 */

class RuntimeSessionActivity : BaseLifecycleTestActivity() {
    private val activityName = "RuntimeSessionActivity"
    private var currentSession: Session? by mutableStateOf(null)
    private var xyzModel: GltfModel? by mutableStateOf(null)
    private var latestCreatedAnchor: Anchor? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result: SessionCreateResult = Session.Companion.create(this)
        currentSession =
            if (result is SessionCreateSuccess) {
                result.session
            } else {
                Log.e(
                    TAG,
                    "[$activityName] Failed to create Session: ${result.javaClass.simpleName}",
                )
                null
            }

        // Load 3D models once the session is created
        currentSession?.let { session -> lifecycleScope.launch { load3DModels(session) } }

        setContent { RuntimeSessionContent() }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear activity state variables
        currentSession = null
        xyzModel = null
        latestCreatedAnchor = null
    }

    @Composable
    private fun RuntimeSessionContent() {
        val session = currentSession
        val anchorTrackingState =
            remember(latestCreatedAnchor) {
                latestCreatedAnchor?.state?.value?.trackingState?.toString() ?: "No Anchor"
            }

        val testResults = remember { mutableStateListOf<TestResult>() }

        LaunchedEffect(Unit) {
            delay(3000)
            if (session == null) {
                addTestResult(testResults, TAG, "Session is null", false)
            } else if (xyzModel == null) {
                addTestResult(testResults, TAG, "Model is not loaded", false)
            } else if ((lifecycle.currentState == Lifecycle.State.RESUMED)) {
                Log.i(TAG, "[$activityName] Creating anchor...")
                val randomPose = Pose(getRandomTranslation(), getRandomRotation())
                latestCreatedAnchor = createAnchor(session, randomPose, xyzModel!!)
                addTestResult(
                    testResults,
                    TAG,
                    "Anchor successfully created on activity RESUMED",
                    true,
                )
            } else {
                addTestResult(testResults, TAG, "The activity is not RESUMED", false)
            }

            if (runAutomated) {
                delay(3000)
                finish()
            }
        }

        val customTextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 30.sp)

        BasicLayout(getString(R.string.lifecycle_runtime_session_test)) {
            CompositionLocalProvider(LocalTextStyle provides customTextStyle) {
                Text(text = "Lifecycle state: ${lifecycle.currentStateAsState().value}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Anchor State: $anchorTrackingState")
                Spacer(modifier = Modifier.height(16.dp))
            }
            TestResultsDisplay(testResults)
        }
    }

    private suspend fun load3DModels(session: Session?) {
        if (session == null) {
            Log.e(TAG, "[$activityName] Cannot load models: Session is null.")
            return
        }
        try {
            xyzModel = GltfModel.create(session, Paths.get("models/xyzArrows.glb"))
            Log.i(TAG, "[$activityName] 3D model 'xyzArrows.glb' loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "[$activityName] Failed to load 3D model: ${e.message}", e)
            Toast.makeText(this, "Failed to load 3D model!", Toast.LENGTH_LONG).show()
        }
    }

    private fun createAnchor(session: Session, pose: Pose, model: GltfModel): Anchor? {
        val result: AnchorCreateResult = Anchor.create(session, pose)
        when (result) {
            is AnchorCreateSuccess -> {
                Log.i(TAG, "[$activityName] [PASS] ANCHOR_SPAWN: success: ${result.anchor}")
                val anchor = result.anchor
                val anchorEntity = AnchorEntity.create(session, anchor)
                val gltfEntity = GltfModelEntity.create(session, model, Pose.Identity)
                gltfEntity.setScale(0.5f)
                anchorEntity.addChild(gltfEntity)
                Log.i(TAG, "[$activityName] [PASS] Visual entity attached to anchor.")
                return anchor
            }
            is AnchorCreateResourcesExhausted -> {
                Log.e(TAG, "[$activityName] ANCHOR_SPAWN: failed: AnchorCreateResourcesExhausted")
                Toast.makeText(this, "ARCore resources exhausted!", Toast.LENGTH_SHORT).show()
            }
            is AnchorLoadInvalidUuid -> {
                Log.e(
                    TAG,
                    "[$activityName] ANCHOR_SPAWN: failed: AnchorLoadInvalidUuid (should not happen for new creation)",
                )
                Toast.makeText(this, "Invalid Anchor UUID!", Toast.LENGTH_SHORT).show()
            }
            is AnchorCreateTrackingUnavailable -> {
                Log.e(
                    TAG,
                    "[$activityName] ANCHOR_SPAWN: failed: AnchorCreateTrackingUnavailable (tracking system is unavailable)",
                )
                Toast.makeText(this, "ARCore tracking unavailable!", Toast.LENGTH_SHORT).show()
            }
            is AnchorCreateNotAuthorized -> {
                Log.e(
                    TAG,
                    "[$activityName] ANCHOR_SPAWN: failed: AnchorCreateNotAuthorized (app not authorized)",
                )
                Toast.makeText(this, "App not authorized for ARCore!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e(
                    TAG,
                    "[$activityName] ANCHOR_SPAWN: failed: Unexpected AnchorCreateResult: ${result.javaClass.simpleName}",
                )
                Toast.makeText(this, "Failed to create anchor: Unknown error!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return null
    }

    private fun getRandomTranslation(): Vector3 {
        val x = Random.Default.nextDouble(-0.5, 0.5)
        val y = Random.Default.nextDouble(-0.5, 0.5)
        val z = Random.Default.nextDouble(-1.5, -0.5)
        return Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    }

    private fun getRandomRotation(): Quaternion {
        val x = Random.Default.nextDouble(0.0, 360.0)
        val y = Random.Default.nextDouble(0.0, 360.0)
        val z = Random.Default.nextDouble(0.0, 360.0)
        return Quaternion.fromEulerAngles(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }
}
