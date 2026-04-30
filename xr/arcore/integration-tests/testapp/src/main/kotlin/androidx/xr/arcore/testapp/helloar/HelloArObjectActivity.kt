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
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.arcore.testapp.helloar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.AugmentedObject
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.common.TrackablesList
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.transformingMovable
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HelloArObjectActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private val gltfModelMap: MutableMap<AugmentedObjectCategory, GltfModel> = ConcurrentHashMap()
    private val objectCategoryMap: MutableMap<AugmentedObject, AugmentedObjectCategory> =
        ConcurrentHashMap()
    private val objectEntitiesMap: MutableMap<AugmentedObject, GltfModelEntity> =
        ConcurrentHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    deviceTracking = DeviceTrackingMode.SPATIAL,
                    augmentedObjectCategories =
                        setOf(
                            AugmentedObjectCategory.KEYBOARD,
                            AugmentedObjectCategory.MOUSE,
                            AugmentedObjectCategory.LAPTOP,
                        ),
                ),
                onSessionAvailable = { session ->
                    this.session = session

                    setContent {
                        Subspace {
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.size(DpVolumeSize(640.dp, 480.dp, 0.dp))
                                        .transformingMovable(),
                                resizePolicy = ResizePolicy(),
                            ) {
                                HelloObjects(session)
                            }
                        }
                    }
                },
            )
        sessionHelper.tryCreateSession()
    }

    // Update the gltf model used (corresponds to the type of object, which might change during
    // tracking)
    private fun updateModelForObject(
        augmentedObject: AugmentedObject,
        state: AugmentedObject.State,
    ) {
        if (objectCategoryMap[augmentedObject] != state.category) {
            objectEntitiesMap[augmentedObject]?.parent = null
            objectEntitiesMap.remove(augmentedObject)
            gltfModelMap[state.category]?.let { gltfModel ->
                objectEntitiesMap[augmentedObject] =
                    GltfModelEntity.create(session, gltfModel, parent = session.scene.activitySpace)
            }
            objectCategoryMap[augmentedObject] = state.category
        }
    }

    @Composable
    fun ObjectBoundingBoxes() {
        LaunchedEffect(Unit) {
            // load GLTF assets
            gltfModelMap[AugmentedObjectCategory.LAPTOP] =
                GltfModel.create(session, Paths.get("models", "BoundingBoxYellow.glb"))
            gltfModelMap[AugmentedObjectCategory.MOUSE] =
                GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            gltfModelMap[AugmentedObjectCategory.KEYBOARD] =
                GltfModel.create(session, Paths.get("models", "BoundingBoxGreen.glb"))

            // subscribe to objects
            val objectJobs: MutableMap<AugmentedObject, Job> = ConcurrentHashMap()
            AugmentedObject.subscribe(session).collect { objects ->
                for (augmentedObject in objects) {
                    if (!objectJobs.contains(augmentedObject)) {
                        objectJobs[augmentedObject] =
                            lifecycleScope.launch {
                                augmentedObject.state.collect { state ->
                                    updateModelForObject(augmentedObject, state)
                                    objectEntitiesMap[augmentedObject]?.let { entity ->
                                        when (state.trackingState) {
                                            TrackingState.TRACKING -> {
                                                entity.setAlpha(TRACKED_ALPHA)
                                            }
                                            TrackingState.PAUSED -> {
                                                entity.setAlpha(PAUSED_ALPHA)
                                            }
                                            TrackingState.STOPPED -> {
                                                objectJobs[augmentedObject]!!.cancel()
                                                objectJobs.remove(augmentedObject)
                                                return@collect
                                            }
                                        }
                                        entity.setPose(
                                            session.scene.perceptionSpace.transformPoseTo(
                                                state.centerPose,
                                                session.scene.activitySpace,
                                            )
                                        )
                                        @SuppressLint("RestrictedApiAndroidX")
                                        entity.setScale(
                                            Vector3(
                                                state.extents.width,
                                                state.extents.height,
                                                state.extents.depth,
                                            )
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    @Composable
    fun HelloObjects(session: Session) {
        ObjectBoundingBoxes()
        var title = intent.getStringExtra("TITLE")
        if (title == null) title = ACTIVITY_NAME
        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = title,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            val state by session.state.collectAsStateWithLifecycle()
            Column(modifier = Modifier.padding(innerPadding).background(color = Color.White)) {
                Text(text = "CoreState: ${state.timeMark}")
                TrackablesList(
                    state.perceptionState!!
                        .trackableStates
                        .filterIsInstance<AugmentedObject.State>()
                        .map { it.owner }
                )
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "AugmentedObjectActivity"
        private const val TRACKED_ALPHA = 0.25f
        private const val PAUSED_ALPHA = 0.05f
    }
}
