/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalSpatialAnnotationsApi::class)

package androidx.xr.arcore.testapp.helloar.rendering

import android.content.Context
import android.util.Log
import androidx.xr.arcore.SpatialAnnotation
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.testapp.helloar.ui.DotPlacement
import androidx.xr.arcore.testapp.helloar.ui.InteractionState
import androidx.xr.arcore.testapp.helloar.ui.QuadOverlayRenderer
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SpatialAnnotationRenderer(
    private val context: Context,
    private val onTrackingStopped: (String?) -> Unit = {},
) {

    @Volatile var isDotCenter: Boolean = false
    @Volatile var isDotTop: Boolean = false

    private val overlayRenderer = QuadOverlayRenderer()

    private val _renderedSpatialAnnotations: MutableStateFlow<List<SpatialAnnotation>> =
        MutableStateFlow(mutableListOf<SpatialAnnotation>())

    private var subscriptionJob: Job? = null
    private lateinit var session: Session

    val renderedSpatialAnnotations: StateFlow<Collection<SpatialAnnotation>> =
        _renderedSpatialAnnotations.asStateFlow()

    fun startRendering(session: Session, coroutineScope: CoroutineScope) {
        this.session = session
        subscriptionJob?.cancel()

        subscriptionJob =
            coroutineScope.launch(Dispatchers.Main) {
                val runningJobs = mutableMapOf<SpatialAnnotation, Job>()
                try {
                    SpatialAnnotation.subscribe(session).collect {
                        Log.d(TAG, "Received SpatialAnnotation update! Count: ${it.size}")
                        updateSpatialAnnotationModels(coroutineScope, it, runningJobs)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in subscribe flow: $e", e)
                }
            }
    }

    fun stopRendering() {
        subscriptionJob?.cancel()
        subscriptionJob = null
        _renderedSpatialAnnotations.value = emptyList()
    }

    private fun updateSpatialAnnotationModels(
        coroutineScope: CoroutineScope,
        spatialAnnotations: List<SpatialAnnotation>,
        runningJobs: MutableMap<SpatialAnnotation, Job>,
    ) {
        val spatialAnnotationsToRender = mutableListOf<SpatialAnnotation>()
        for (spatialAnnotation in spatialAnnotations) {
            if (!runningJobs.containsKey(spatialAnnotation)) {
                runningJobs[spatialAnnotation] = coroutineScope.launch {
                    updateAndRenderSpatialAnnotation(spatialAnnotation)
                }
            }
            spatialAnnotationsToRender.add(spatialAnnotation)
        }
        val iterator = runningJobs.entries.iterator()
        while (iterator.hasNext()) {
            val (spatialAnnotation, job) = iterator.next()
            if (!spatialAnnotationsToRender.contains(spatialAnnotation)) {
                job.cancel()
                iterator.remove()
            }
        }
        _renderedSpatialAnnotations.value = spatialAnnotationsToRender
    }

    // TODO(b/542273731): Break this function into smaller helper functions.
    @OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
    private suspend fun updateAndRenderSpatialAnnotation(spatialAnnotation: SpatialAnnotation) {
        var surfaceEntity: SurfaceEntity? = null
        var currentInteractionState = InteractionState.NORMAL
        var lastWidthMeters = 0f
        var lastHeightMeters = 0f
        var lastPixelW = 0
        var lastPixelH = 0
        var lastTrackingState = TrackingState.STOPPED
        var lastDotPlacement = DotPlacement.NONE

        try {
            spatialAnnotation.state.collect { state ->
                Log.d(
                    TAG,
                    "State updated: ${state.trackingState}, Quad: ${state.quad}",
                )
                when (state.trackingState) {
                    TrackingState.TRACKING -> {
                        val quad = state.quad
                        val w =
                            if (quad != null) {
                                val dxW = quad.upperRight.x - quad.upperLeft.x
                                val dyW = quad.upperRight.y - quad.upperLeft.y
                                hypot(dxW, dyW)
                            } else {
                                0f
                            }
                        val h =
                            if (quad != null) {
                                val dxH = quad.lowerLeft.x - quad.upperLeft.x
                                val dyH = quad.lowerLeft.y - quad.upperLeft.y
                                hypot(dxH, dyH)
                            } else {
                                0f
                            }
                        val coercedW = w.coerceIn(MIN_SIZE_METERS, MAX_SIZE_METERS)
                        val coercedH = h.coerceIn(MIN_SIZE_METERS, MAX_SIZE_METERS)

                        val pixelW =
                            (coercedW * BASE_PIXELS_PER_METER)
                                .roundToInt()
                                .coerceAtLeast(QuadOverlayRenderer.MIN_TRACKING_SIZE_PIXELS)
                        val pixelH =
                            (coercedH * BASE_PIXELS_PER_METER)
                                .roundToInt()
                                .coerceAtLeast(QuadOverlayRenderer.MIN_TRACKING_SIZE_PIXELS)

                        val annotationInPerception = state.pose
                        val panelInPerception =
                            annotationInPerception.compose(
                                Pose(rotation = ANNOTATION_FROM_PANEL_ROTATION)
                            )
                        val panelInActivity =
                            session.scene.perceptionSpace.transformPoseTo(
                                panelInPerception,
                                session.scene.activitySpace,
                            )

                        val currentSurfaceEntity: SurfaceEntity
                        if (surfaceEntity == null) {
                            currentSurfaceEntity =
                                SurfaceEntity.create(
                                        session = session,
                                        shape =
                                            SurfaceEntity.Shape.Quad(
                                                FloatSize2d(coercedW, coercedH)
                                            ),
                                        pose = panelInActivity,
                                        parent = session.scene.activitySpace,
                                    )
                                    .apply { setSurfacePixelDimensions(IntSize2d(pixelW, pixelH)) }

                            val interactable =
                                InteractableComponent.create(session) { inputEvent ->
                                    when (inputEvent.action) {
                                        InputEvent.Action.UP -> {
                                            onTrackingStopped(
                                                "Tracking stopped. Ready to track new object."
                                            )
                                        }
                                        InputEvent.Action.HOVER_ENTER -> {
                                            if (
                                                currentInteractionState != InteractionState.HOVERED
                                            ) {
                                                currentInteractionState = InteractionState.HOVERED
                                                renderOverlay(
                                                    surfaceEntity,
                                                    lastPixelW,
                                                    lastPixelH,
                                                    currentInteractionState,
                                                    lastTrackingState,
                                                    lastDotPlacement,
                                                )
                                            }
                                        }
                                        InputEvent.Action.HOVER_EXIT -> {
                                            if (
                                                currentInteractionState != InteractionState.NORMAL
                                            ) {
                                                currentInteractionState = InteractionState.NORMAL
                                                renderOverlay(
                                                    surfaceEntity,
                                                    lastPixelW,
                                                    lastPixelH,
                                                    currentInteractionState,
                                                    lastTrackingState,
                                                    lastDotPlacement,
                                                )
                                            }
                                        }
                                    }
                                }
                            currentSurfaceEntity.addComponent(interactable)
                            surfaceEntity = currentSurfaceEntity
                            lastWidthMeters = coercedW
                            lastHeightMeters = coercedH
                        } else {
                            currentSurfaceEntity = surfaceEntity!!
                            currentSurfaceEntity.setPose(panelInActivity)
                            val sizeChanged =
                                abs(lastWidthMeters - coercedW) > DIMENSION_EPSILON_METERS ||
                                    abs(lastHeightMeters - coercedH) > DIMENSION_EPSILON_METERS
                            if (sizeChanged) {
                                lastWidthMeters = coercedW
                                lastHeightMeters = coercedH
                                currentSurfaceEntity.shape =
                                    SurfaceEntity.Shape.Quad(FloatSize2d(coercedW, coercedH))
                                currentSurfaceEntity.setSurfacePixelDimensions(
                                    IntSize2d(pixelW, pixelH)
                                )
                            }
                        }

                        val dotPlacement =
                            when {
                                isDotCenter && isDotTop -> DotPlacement.BOTH
                                isDotCenter -> DotPlacement.CENTER
                                isDotTop -> DotPlacement.TOP
                                else -> DotPlacement.NONE
                            }

                        lastPixelW = pixelW
                        lastPixelH = pixelH
                        lastTrackingState = state.trackingState
                        lastDotPlacement = dotPlacement

                        renderOverlay(
                            currentSurfaceEntity,
                            pixelW,
                            pixelH,
                            currentInteractionState,
                            state.trackingState,
                            dotPlacement,
                        )
                    }
                    TrackingState.PAUSED,
                    TrackingState.STOPPED -> {
                        lastTrackingState = state.trackingState
                        lastDotPlacement = DotPlacement.NONE
                        val pixelW =
                            (lastWidthMeters * BASE_PIXELS_PER_METER)
                                .roundToInt()
                                .coerceAtLeast(QuadOverlayRenderer.MIN_TRACKING_SIZE_PIXELS)
                        val pixelH =
                            (lastHeightMeters * BASE_PIXELS_PER_METER)
                                .roundToInt()
                                .coerceAtLeast(QuadOverlayRenderer.MIN_TRACKING_SIZE_PIXELS)
                        lastPixelW = pixelW
                        lastPixelH = pixelH
                        renderOverlay(
                            surfaceEntity,
                            pixelW,
                            pixelH,
                            currentInteractionState,
                            state.trackingState,
                            DotPlacement.NONE,
                        )
                    }
                }
            }
        } finally {
            surfaceEntity?.removeAllComponents()
            surfaceEntity?.parent = null
        }
    }

    private fun renderOverlay(
        entity: SurfaceEntity?,
        width: Int,
        height: Int,
        interactionState: InteractionState,
        trackingState: TrackingState,
        dotPlacement: DotPlacement,
    ) {
        if (entity == null || width <= 0 || height <= 0) return
        val surface = entity.getSurface()
        overlayRenderer.drawQuadOverlay(
            surface = surface,
            width = width,
            height = height,
            interactionState = interactionState,
            trackingState = trackingState,
            distance = 1.0f,
            dotPlacement = dotPlacement,
        )
    }

    private companion object {
        private const val TAG = "SpatialAnnotationRenderer"
        private const val BASE_PIXELS_PER_METER = 1000f
        private const val MIN_SIZE_METERS = 0.01f
        private const val MAX_SIZE_METERS = 10.0f
        private const val DIMENSION_EPSILON_METERS = 0.005f

        // Rotates from SceneCore Panel local frame (+Y up) into OpenXR Annotation local frame (+Y
        // down).
        private val ANNOTATION_FROM_PANEL_ROTATION = Quaternion(1f, 0f, 0f, 0f)
    }
}
