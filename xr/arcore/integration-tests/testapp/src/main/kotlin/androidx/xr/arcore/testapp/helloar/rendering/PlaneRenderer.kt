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
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.arcore.testapp.helloar.rendering

import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.Plane
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PlaneRenderer(val session: Session, val coroutineScope: CoroutineScope) :
    DefaultLifecycleObserver {

    private val _planesModelsMap = ConcurrentHashMap<String, GltfModel?>()
    private val _renderedPlanes: MutableStateFlow<List<PlaneModel>> =
        MutableStateFlow(mutableListOf<PlaneModel>())
    internal val renderedPlanes: StateFlow<Collection<PlaneModel>> = _renderedPlanes.asStateFlow()

    private lateinit var updateJob: CompletableJob

    override fun onResume(owner: LifecycleOwner) {
        updateJob =
            SupervisorJob(
                coroutineScope.launch {
                    preloadModels()
                    Plane.subscribe(session).collect { updatePlaneModels(it) }
                }
            )
    }

    override fun onPause(owner: LifecycleOwner) {
        updateJob.complete()
        _renderedPlanes.value = emptyList<PlaneModel>()
    }

    private suspend fun preloadModels() {
        if (_planesModelsMap.isNotEmpty()) return

        val assetsToLoad =
            SUPPORTED_OBJECT_MODELS.values.toMutableSet().apply { add(DEFAULT_OBJECT_MODEL) }

        val assetToModel =
            assetsToLoad.associateWith { assetName ->
                GltfModel.create(session, Paths.get("models", assetName))
            }

        for ((category, assetName) in SUPPORTED_OBJECT_MODELS) {
            _planesModelsMap[category] = assetToModel[assetName]
        }

        _planesModelsMap["DEFAULT"] = assetToModel[DEFAULT_OBJECT_MODEL]
    }

    private suspend fun updatePlaneModels(planes: Collection<Plane>) {
        val planesToRender = _renderedPlanes.value.toMutableList()
        // Create renderers for new planes.
        for (plane in planes) {
            if (_renderedPlanes.value.none { it.id == plane.hashCode() }) {
                addPlaneModel(plane, planesToRender)
            }
        }
        // Stop rendering dropped planes.
        for (renderedPlane in _renderedPlanes.value) {
            if (planes.none { it.hashCode() == renderedPlane.id }) {
                removePlaneModel(renderedPlane, planesToRender)
            }
        }
        // Emit to notify collectors that collection has been updated.
        _renderedPlanes.value = planesToRender
    }

    private suspend fun addPlaneModel(plane: Plane, planesToRender: MutableList<PlaneModel>) {
        val label = plane.state.value.label.toString()
        val asset =
            if (SUPPORTED_OBJECT_MODELS.containsKey(label)) {
                SUPPORTED_OBJECT_MODELS[label]
            } else {
                DEFAULT_OBJECT_MODEL
            }
        val model =
            _planesModelsMap.getOrPut(label) {
                GltfModel.create(session, Paths.get("models", asset))
            }
        val modelEntity = GltfModelEntity.create(session, _planesModelsMap[label]!!)

        // The counter starts at max to trigger the resize on the first update loop since emulators
        // only
        // update their static planes once.
        var counter = PANEL_RESIZE_UPDATE_COUNT
        // Make the render job a child of the update job so it completes when the parent completes.
        val renderJob =
            coroutineScope.launch(updateJob) {
                plane.state.collect { state ->
                    if (state.trackingState == TrackingState.TRACKING) {
                        if (state.label == Plane.Label.UNKNOWN) {
                            modelEntity.setEnabled(false)
                        } else {
                            modelEntity.setEnabled(true)
                            modelEntity.setAlpha(.5f)
                            counter++
                            modelEntity.setPose(
                                session.scene.perceptionSpace
                                    .transformPoseTo(state.centerPose, session.scene.activitySpace)
                                    // Planes are X-Y while Panels are X-Z, so we need to rotate the
                                    // X-axis by -90
                                    // degrees to align them.
                                    .compose(PANEL_TO_PLANE_ROTATION)
                            )

                            if (counter > PANEL_RESIZE_UPDATE_COUNT) {
                                modelEntity.setScale(scaledExtents(state.extents))
                                counter = 0
                            }
                        }
                    } else if (state.trackingState == TrackingState.STOPPED) {
                        modelEntity.setEnabled(false)
                    }
                }
            }

        planesToRender.add(
            PlaneModel(plane.hashCode(), plane.type, plane.state, modelEntity!!, renderJob)
        )
    }

    private fun createPanelDebugViewUsingCompose(plane: Plane, activity: Activity): View {
        val view = TextView(activity.applicationContext)
        view.text = "Plane: ${plane.hashCode()}"
        view.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        view.setBackgroundColor(Color.WHITE)
        return view
    }

    private fun convertMetersToPixels(input: FloatSize2d): FloatSize2d = input * PX_PER_METER

    private fun removePlaneModel(planeModel: PlaneModel, planesToRender: MutableList<PlaneModel>) {
        planeModel.renderJob?.cancel()
        planeModel.modelEntity.dispose()
        planesToRender.remove(planeModel)
    }

    private fun scaledExtents(extents: FloatSize2d): Vector3 {
        return Vector3(
            extents.width * MODEL_SCALING_FACTOR,
            extents.height * MODEL_SCALING_FACTOR,
            MODEL_DEPTH,
        )
    }

    private companion object {
        private val PX_PER_METER = Resources.getSystem().displayMetrics.density * 1111.11f
        private val PANEL_TO_PLANE_ROTATION =
            Pose(Vector3(), Quaternion.fromEulerAngles(-90f, 0f, 0f))
        private const val PANEL_RESIZE_UPDATE_COUNT = 50
        private const val MODEL_SCALING_FACTOR = 1f / 1.7f / 2f
        private const val MODEL_DEPTH = .001f
        private val DEFAULT_OBJECT_MODEL = "BoundingBoxGreen.glb"
        private val SUPPORTED_OBJECT_MODELS =
            mapOf(
                "WALL" to "BoundingBoxGreen.glb",
                "FLOOR" to "BoundingBoxBlue.glb",
                "CEILING" to "BoundingBoxYellow.glb",
                "TABLE" to "BoundingBoxMagenta.glb",
            )
    }
}
