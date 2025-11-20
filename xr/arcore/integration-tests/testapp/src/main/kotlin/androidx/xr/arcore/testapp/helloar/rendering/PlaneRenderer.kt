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
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PlaneRenderer(val session: Session, val coroutineScope: CoroutineScope) :
    DefaultLifecycleObserver {

    private val _renderedPlanes: MutableStateFlow<List<PlaneModel>> =
        MutableStateFlow(mutableListOf<PlaneModel>())
    internal val renderedPlanes: StateFlow<Collection<PlaneModel>> = _renderedPlanes.asStateFlow()

    private lateinit var updateJob: CompletableJob

    override fun onResume(owner: LifecycleOwner) {
        updateJob =
            SupervisorJob(
                coroutineScope.launch { Plane.subscribe(session).collect { updatePlaneModels(it) } }
            )
    }

    override fun onPause(owner: LifecycleOwner) {
        updateJob.complete()
        _renderedPlanes.value = emptyList<PlaneModel>()
    }

    private fun updatePlaneModels(planes: Collection<Plane>) {
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

    private fun addPlaneModel(plane: Plane, planesToRender: MutableList<PlaneModel>) {
        val view = createPanelDebugViewUsingCompose(plane, session.activity)
        val entity = createPlanePanelEntity(plane, view)
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
                            entity.setEnabled(false)
                        } else {
                            entity.setEnabled(true)
                            counter++
                            entity.setPose(
                                session.scene.perceptionSpace
                                    .transformPoseTo(state.centerPose, session.scene.activitySpace)
                                    // Planes are X-Y while Panels are X-Z, so we need to rotate the
                                    // X-axis by -90
                                    // degrees to align them.
                                    .compose(PANEL_TO_PLANE_ROTATION)
                            )

                            updateViewText(view, plane, state)
                            if (counter > PANEL_RESIZE_UPDATE_COUNT) {
                                val panelExtentsInPixels = convertMetersToPixels(state.extents)
                                entity.sizeInPixels =
                                    IntSize2d(
                                        width = panelExtentsInPixels.width.toInt(),
                                        height = panelExtentsInPixels.height.toInt(),
                                    )
                                counter = 0
                            }
                        }
                    } else if (state.trackingState == TrackingState.STOPPED) {
                        entity.setEnabled(false)
                    }
                }
            }

        planesToRender.add(PlaneModel(plane.hashCode(), plane.type, plane.state, entity, renderJob))
    }

    private fun createPlanePanelEntity(plane: Plane, view: View): PanelEntity {
        return PanelEntity.create(
            session,
            view,
            IntSize2d(320, 320),
            plane.hashCode().toString(),
            plane.state.value.centerPose,
        )
    }

    private fun createPanelDebugViewUsingCompose(plane: Plane, activity: Activity): View {
        val view = TextView(activity.applicationContext)
        view.text = "Plane: ${plane.hashCode()}"
        view.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        view.setBackgroundColor(Color.WHITE)
        return view
    }

    private fun updateViewText(view: View, plane: Plane, state: Plane.State) {
        val textView = view as TextView
        textView.setBackgroundColor(convertPlaneLabelToColor(state.label))
        textView.text = "Plane: ${plane.hashCode()}"
    }

    private fun convertPlaneLabelToColor(label: Plane.Label): Int =
        when (label) {
            Plane.Label.WALL -> Color.GREEN
            Plane.Label.FLOOR -> Color.BLUE
            Plane.Label.CEILING -> Color.YELLOW
            Plane.Label.TABLE -> Color.MAGENTA
            // Planes with Unknown Label are currently not rendered.
            else -> Color.RED
        }

    private fun convertMetersToPixels(input: FloatSize2d): FloatSize2d = input * PX_PER_METER

    private fun removePlaneModel(planeModel: PlaneModel, planesToRender: MutableList<PlaneModel>) {
        planeModel.renderJob?.cancel()
        planeModel.entity.dispose()
        planesToRender.remove(planeModel)
    }

    private companion object {
        private val PX_PER_METER = Resources.getSystem().displayMetrics.density * 1111.11f
        private val PANEL_TO_PLANE_ROTATION =
            Pose(Vector3(), Quaternion.fromEulerAngles(-90f, 0f, 0f))
        private const val PANEL_RESIZE_UPDATE_COUNT = 50
    }
}
