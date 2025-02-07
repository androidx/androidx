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

package androidx.xr.arcore.apps.whitebox.helloar.rendering

import android.app.Activity
import android.util.Log
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.hitTest
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.Session as JxrCoreSession
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** Class that keeps track of anchors rendered as GLTF models in a SceneCore session. */
internal class AnchorRenderer(
    val activity: Activity,
    val planeRenderer: PlaneRenderer,
    val session: Session,
    val renderSession: JxrCoreSession,
    val coroutineScope: CoroutineScope,
) {

    private lateinit var gltfAnchorModel: GltfModel

    private val renderedAnchors: MutableList<AnchorModel> = mutableListOf<AnchorModel>()

    private lateinit var updateJob: CompletableJob

    internal fun startRendering() {
        updateJob =
            SupervisorJob(
                coroutineScope.launch() {
                    gltfAnchorModel =
                        GltfModel.create(renderSession, "models/xyzArrows.glb").await()
                    planeRenderer.renderedPlanes.collect { attachInteractableComponents(it) }
                }
            )
    }

    internal fun stopRendering() {
        updateJob.complete()
        clearRenderedAnchors()
    }

    private fun clearRenderedAnchors() {
        for (anchor in renderedAnchors) {
            anchor.entity.dispose()
        }
        renderedAnchors.clear()
    }

    private fun attachInteractableComponents(planeModels: Collection<PlaneModel>) {
        for (planeModel in planeModels) {
            if (planeModel.entity.getComponents().isEmpty()) {
                planeModel.entity.addComponent(
                    InteractableComponent.create(renderSession, activity.mainExecutor) { event ->
                        if (event.action.equals(InputEvent.ACTION_DOWN)) {
                            val up =
                                renderSession.spatialUser.head?.getActivitySpacePose()?.up
                                    ?: Vector3.Up
                            val perceptionRayPose =
                                renderSession.activitySpace.transformPoseTo(
                                    Pose(
                                        event.origin,
                                        Quaternion.fromLookTowards(event.direction, up)
                                    ),
                                    renderSession.perceptionSpace,
                                )
                            val perceptionRay =
                                Ray(perceptionRayPose.translation, perceptionRayPose.forward)
                            hitTest(session, perceptionRay)
                                .firstOrNull {
                                    // TODO(b/372054517): Re-enable creating anchors on Unknown
                                    // planes once we can
                                    // support rendering them.
                                    (it.trackable as? Plane)?.state?.value?.label !=
                                        Plane.Label.Unknown
                                }
                                ?.let { hitResult ->
                                    try {
                                        when (
                                            val anchorResult =
                                                Anchor.create(session, hitResult.hitPose)
                                        ) {
                                            is AnchorCreateSuccess ->
                                                renderedAnchors.add(
                                                    createAnchorModel(anchorResult.anchor)
                                                )
                                            is AnchorCreateResourcesExhausted -> {
                                                Log.e(
                                                    activity::class.simpleName,
                                                    "Failed to create anchor: anchor resources exhausted.",
                                                )
                                            }
                                        }
                                    } catch (e: IllegalStateException) {
                                        Log.e(
                                            activity::class.simpleName,
                                            "Failed to create anchor: ${e.message}"
                                        )
                                    }
                                }
                        }
                    }
                )
            }
        }
    }

    private fun createAnchorModel(anchor: Anchor): AnchorModel {
        val entity = GltfModelEntity.create(renderSession, gltfAnchorModel, Pose())
        entity.setScale(.1f)
        val renderJob =
            coroutineScope.launch(updateJob) {
                anchor.state.collect { state ->
                    if (state.trackingState == TrackingState.Tracking) {
                        entity.setPose(
                            renderSession.perceptionSpace.transformPoseTo(
                                state.pose,
                                renderSession.activitySpace
                            )
                        )
                    } else if (state.trackingState == TrackingState.Stopped) {
                        entity.setHidden(true)
                    }
                }
            }
        return AnchorModel(anchor.hashCode(), anchor.state, entity, renderJob)
    }
}
