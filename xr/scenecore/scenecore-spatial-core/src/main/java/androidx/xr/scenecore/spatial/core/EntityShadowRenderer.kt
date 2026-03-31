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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.xr.runtime.SpatialApiVersionHelper.spatialApiVersion
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import com.android.extensions.xr.XrExtensions
import java.util.concurrent.ScheduledExecutorService

/** Interface for rendering the border of an entity onto a perception plane. */
internal interface EntityShadowRenderer {

    fun enableShadow()

    fun updateShadow(openXrToProposedPanel: Pose, openXrToPlane: Pose, shadowDim: FloatSize2d)

    fun hideShadow()

    fun disableShadow()
}

internal class EntityShadowRendererImpl(
    private val activitySpaceImpl: ActivitySpaceImpl,
    private val perceptionSpaceScenePose: PerceptionSpaceScenePoseImpl,
    private val activity: Activity,
    private val xrExtensions: XrExtensions,
    private val entityRegistry: SceneNodeRegistry,
    private val executor: ScheduledExecutorService,
) : EntityShadowRenderer {
    private var panelEntity: PanelEntityImpl? = null
    private var isVisible = false

    private val marginInMeters: Float
        get() {
            val defaultPixelDensity =
                if (spatialApiVersion >= 2) {
                    xrExtensions.underlyingObject.config.defaultPixelsPerMeter()
                } else {
                    xrExtensions.config.defaultPixelsPerMeter(
                        Resources.getSystem().displayMetrics.density
                    )
                }
            return PANEL_BORDER_ADDED_MARGIN / defaultPixelDensity
        }

    override fun enableShadow() {
        if (panelEntity != null) return
        val entityShadowNode = xrExtensions.createNode()
        val view: View = EntityShadowView(activity)
        val dimensions = Dimensions(1f, 1f, 0f)

        panelEntity =
            PanelEntityImpl(
                activity,
                entityShadowNode,
                view,
                xrExtensions,
                entityRegistry,
                dimensions,
                "PanelRenderer",
                executor,
            )
        panelEntity?.parent = activitySpaceImpl
        panelEntity?.setHidden(true)
    }

    override fun updateShadow(
        openXrToProposedPanel: Pose,
        openXrToPlane: Pose,
        shadowDim: FloatSize2d,
    ) {
        val panelPoseInActivitySpace =
            calculateProjectedPanelPoseInActivitySpace(openXrToProposedPanel, openXrToPlane)

        panelEntity?.let { panel ->
            panel.size =
                Dimensions(shadowDim.width + marginInMeters, shadowDim.height + marginInMeters, 0f)
            panel.setPose(panelPoseInActivitySpace)
            panel.setHidden(false)
            isVisible = true
        }
    }

    override fun hideShadow() {
        if (!isVisible) {
            return
        }
        panelEntity?.setHidden(true)
        isVisible = false
    }

    override fun disableShadow() {
        panelEntity?.dispose()
        panelEntity = null
    }

    private fun calculateProjectedPanelPoseInActivitySpace(
        openXrToProposedPanel: Pose,
        openXrtoPlane: Pose,
    ): Pose {
        val planeToOpenXr = openXrtoPlane.inverse
        val planeToPanel = planeToOpenXr.compose(openXrToProposedPanel)
        val planeToProjectedPanel =
            Pose(
                Vector3(planeToPanel.translation.x, 0f, planeToPanel.translation.z),
                planeToOpenXr.rotation.times(
                    openXrToProposedPanel.getForwardVectorToUpRotation(openXrtoPlane)
                ),
            )
        val panelInOxr = openXrtoPlane.compose(planeToProjectedPanel)

        return perceptionSpaceScenePose.transformPoseTo(panelInOxr, activitySpaceImpl)
    }

    /** EntityShadowView is a view with a blue border to enable the shadow effect. */
    private class EntityShadowView(context: Context) : View(context) {
        override fun onDrawForeground(canvas: Canvas) {
            super.onDrawForeground(canvas)
            val border = Path()
            border.addRoundRect(
                HALF_STROKE_WIDTH,
                HALF_STROKE_WIDTH,
                canvas.width - HALF_STROKE_WIDTH,
                canvas.height - HALF_STROKE_WIDTH,
                CORNER_RADIUS,
                CORNER_RADIUS,
                Path.Direction.CW,
            )
            val paint = Paint()
            paint.setColor(-0xab9c61)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = STROKE_WIDTH
            canvas.drawPath(border, paint)
        }
    }

    companion object {
        private const val STROKE_WIDTH = 20f
        private const val HALF_STROKE_WIDTH = STROKE_WIDTH / 2
        private const val CORNER_RADIUS = 20f
        private const val PANEL_BORDER_ADDED_MARGIN = 50f
    }
}
