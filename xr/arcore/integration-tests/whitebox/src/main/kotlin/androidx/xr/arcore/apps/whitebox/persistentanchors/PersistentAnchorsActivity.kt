/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.apps.whitebox.persistentanchors

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.apps.whitebox.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.common.SessionLifecycleHelper
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Session as JxrCoreSession
import java.util.UUID
import kotlin.collections.List
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Activity to test the Persistent Anchor APIs. */
class PersistentAnchorsActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var jxrCoreSession: JxrCoreSession
    private lateinit var movableEntity: Entity
    private val movableEntityOffset = Pose(Vector3(0f, 0f, -2.0f))
    private val uuids = MutableStateFlow<List<UUID>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionHelper =
            SessionLifecycleHelper(
                onCreateCallback = {
                    session = it
                    jxrCoreSession = JxrCoreSession.create(this)
                    createTargetPanel()
                    setContent { MainPanel() }
                },
                onResumeCallback = { onResumeCallback() },
            )
        lifecycle.addObserver(sessionHelper)
    }

    private fun createTargetPanel() {
        val composeView = ComposeView(this)
        composeView.setContent { TargetPanel() }
        configureComposeView(composeView, this)
        movableEntity =
            PanelEntity.create(
                jxrCoreSession,
                composeView,
                Dimensions(640f, 640f),
                Dimensions(1f, 1f, 1f),
                "movableEntity",
                movableEntityOffset,
            )
        movableEntity.setParent(jxrCoreSession.activitySpace)
    }

    private fun onResumeCallback() {
        lifecycleScope.launch {
            // First load will fail, so we launch a second load after a delay which should succeed.
            uuids.emit(Anchor.getPersistedAnchorUuids(session))
            delay(2.seconds)
            uuids.emit(Anchor.getPersistedAnchorUuids(session))
        }
        lifecycleScope.launch { session.state.collect { updatePlaneEntity() } }
    }

    private fun updatePlaneEntity() {
        jxrCoreSession.spatialUser.head?.let {
            movableEntity.setPose(
                it.transformPoseTo(movableEntityOffset, jxrCoreSession.activitySpace)
            )
        }
    }

    private fun configureComposeView(composeView: ComposeView, activity: Activity) {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        composeView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        composeView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    @Composable
    private fun MainPanel() {
        val uuidsState = uuids.collectAsStateWithLifecycle()

        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
        ) {
            BackToMainActivityButton()
            Text(modifier = Modifier.padding(top = 20.dp), text = "Persisted anchors:")
            for (uuid in uuidsState.value) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                    Text(text = "UUID: $uuid", fontSize = 24.sp)
                    Button(onClick = { loadAnchor(uuid) }) { Text("Load anchor") }
                    Button(onClick = { unpersistAnchor(uuid) }) { Text("Unpersist anchor") }
                }
            }
        }
    }

    @Composable
    private fun TargetPanel() {
        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Button(onClick = { addAnchor() }) { Text(text = "Add anchor", fontSize = 38.sp) }
        }
    }

    private fun addAnchor() {
        val anchorPose =
            jxrCoreSession.activitySpace.transformPoseTo(
                movableEntity.getPose(),
                jxrCoreSession.perceptionSpace,
            )
        try {
            when (val anchorResult = Anchor.create(session, anchorPose)) {
                is AnchorCreateSuccess -> createAnchorPanel(anchorResult.anchor)
                is AnchorCreateResourcesExhausted -> {
                    Log.e(ACTIVITY_NAME, "Failed to create anchor: anchor resources exhausted.")
                }
            }
        } catch (e: IllegalStateException) {
            Log.e(ACTIVITY_NAME, "Failed to create anchor: ${e.message}")
        }
    }

    private fun createAnchorPanel(anchor: Anchor) {
        val composeView = ComposeView(this)
        configureComposeView(composeView, this)
        val anchorEntity = AnchorEntity.create(jxrCoreSession, anchor)
        val panelEntity =
            PanelEntity.create(
                jxrCoreSession,
                composeView,
                Dimensions(640f, 640f),
                Dimensions(1f, 1f, 1f),
                "anchorEntity ${anchor.hashCode()}",
                Pose(),
            )
        panelEntity.setParent(anchorEntity)
        composeView.setContent { AnchorPanel(anchor, panelEntity) }
    }

    @Composable
    private fun AnchorPanel(anchor: Anchor, entity: Entity) {
        val anchorState = anchor.state.collectAsStateWithLifecycle()
        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Tracking State: ${anchorState.value.trackingState}",
                fontSize = 32.sp,
            )
            Button(modifier = Modifier.padding(top = 10.dp), onClick = { persistAnchor(anchor) }) {
                Text(text = "Persist anchor", fontSize = 32.sp)
            }
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = { deleteEntity(anchor, entity) }
            ) {
                Text(text = "Delete anchor", fontSize = 32.sp)
            }
        }
    }

    private fun persistAnchor(anchor: Anchor) {
        lifecycleScope.launch {
            try {
                anchor.persist()
                uuids.emit(Anchor.getPersistedAnchorUuids(session))
            } catch (e: RuntimeException) {
                Log.e("ARCore", "Error persisting anchor: ${e.message}")
            }
        }
    }

    private fun deleteEntity(anchor: Anchor, entity: Entity) {
        entity.dispose()
        anchor.detach()
    }

    private fun unpersistAnchor(uuid: UUID) {
        Anchor.unpersist(session, uuid)
        lifecycleScope.launch { uuids.emit(uuids.value - uuid) }
    }

    private fun loadAnchor(uuid: UUID) {
        try {
            when (val anchorResult = Anchor.load(session, uuid)) {
                is AnchorCreateSuccess -> {
                    lifecycleScope.launch {
                        // We need to wait until the anchor is tracked before querying its pose.
                        delay(1.seconds)
                        createAnchorPanel(anchorResult.anchor)
                    }
                }
                is AnchorCreateResourcesExhausted -> {
                    Log.e(ACTIVITY_NAME, "Failed to create anchor: anchor resources exhausted.")
                }
            }
        } catch (e: IllegalStateException) {
            Log.e(ACTIVITY_NAME, "Failed to create anchor: ${e.message}")
        }
    }

    companion object {
        const val ACTIVITY_NAME = "PersistentAnchorsActivity"
    }
}
