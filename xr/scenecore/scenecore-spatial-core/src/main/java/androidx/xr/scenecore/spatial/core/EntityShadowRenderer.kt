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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node

/** Interface for rendering the border of an entity onto a perception plane. */
internal interface EntityShadowRenderer {
    fun updateShadow(openXrToProposedPanel: Pose, openXrToPlane: Pose, shadowDim: FloatSize2d)

    fun hidePlane()

    fun destroy()
}

// TODO: b/413661481 - Remove this suppression prior to JXR stable release.
// For SurfaceControlViewHost and getDisplay
@SuppressLint("NewApi")
internal class EntityShadowRendererImpl(
    private val activitySpaceImpl: ActivitySpaceImpl,
    private val perceptionSpaceScenePose: PerceptionSpaceScenePoseImpl,
    private val activity: Activity,
    private val xrExtensions: XrExtensions,
) : EntityShadowRenderer {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var entityShadowNode: Node? = null
    private var surfaceControlViewHost: SurfaceControlViewHost? = null
    private var isVisible = false

    override fun updateShadow(
        openXrToProposedPanel: Pose,
        openXrToPlane: Pose,
        shadowDim: FloatSize2d,
    ) {
        // If there is no panel shadow node, create it.
        if (entityShadowNode == null) {
            createEntityShadow(openXrToProposedPanel, openXrToPlane, shadowDim)
            return
        }

        val panelPoseInActivitySpace =
            calculateProjectedPanelPoseInActivitySpace(openXrToProposedPanel, openXrToPlane)
        xrExtensions.createNodeTransaction().use { transaction ->
            if (!isVisible) {
                transaction.setVisibility(entityShadowNode, true)
                isVisible = true
            }
            transaction
                .setPosition(
                    entityShadowNode,
                    panelPoseInActivitySpace.translation.x,
                    panelPoseInActivitySpace.translation.y,
                    panelPoseInActivitySpace.translation.z,
                )
                .setOrientation(
                    entityShadowNode,
                    panelPoseInActivitySpace.rotation.x,
                    panelPoseInActivitySpace.rotation.y,
                    panelPoseInActivitySpace.rotation.z,
                    panelPoseInActivitySpace.rotation.w,
                )
                .apply()
        }
    }

    override fun hidePlane() {
        if (!isVisible || entityShadowNode == null) {
            return
        }
        xrExtensions.createNodeTransaction().use { transaction ->
            transaction.setVisibility(entityShadowNode, false).apply()
        }
        isVisible = false
    }

    override fun destroy() {
        synchronized(this) {
            handler.removeCallbacksAndMessages(null)
            surfaceControlViewHost?.let { handler.post { it.release() } }
            entityShadowNode?.let {
                xrExtensions.createNodeTransaction().use { transaction ->
                    transaction.setParent(it, null).apply()
                }
            }
            surfaceControlViewHost = null
            entityShadowNode = null
        }
    }

    private fun createEntityShadow(
        openXrToProposedPanel: Pose,
        openXrToPlane: Pose,
        shadowDim: FloatSize2d,
    ) {
        val shadowWidth = shadowDim.width + PANEL_BORDER_ADDED_MARGIN
        val shadowDepth = shadowDim.height + PANEL_BORDER_ADDED_MARGIN
        val view: View = EntityShadowView(activity)

        val panelPoseInActivitySpace =
            calculateProjectedPanelPoseInActivitySpace(openXrToProposedPanel, openXrToPlane)

        entityShadowNode = xrExtensions.createNode()

        // TODO(b/484421916): Use PanelEntityImpl instead as it does the same thing as below
        // The surfaceControlViewHost needs to be created on the main thread.
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        handler.post {
            synchronized(this) {
                if (entityShadowNode == null) return@post
                activity.display?.let {
                    surfaceControlViewHost = SurfaceControlViewHost(activity, it, Binder())
                    surfaceControlViewHost?.setView(view, shadowWidth.toInt(), shadowDepth.toInt())
                    surfaceControlViewHost?.surfacePackage?.let { surfacePackage ->
                        xrExtensions.createNodeTransaction().use { transaction ->
                            transaction
                                .setName(entityShadowNode, "PanelRenderer")
                                .setSurfacePackage(entityShadowNode, surfacePackage)
                                .setWindowBounds(
                                    surfacePackage,
                                    shadowWidth.toInt(),
                                    shadowDepth.toInt(),
                                )
                                .setVisibility(entityShadowNode, true)
                                .setPosition(
                                    entityShadowNode,
                                    panelPoseInActivitySpace.translation.x,
                                    panelPoseInActivitySpace.translation.y,
                                    panelPoseInActivitySpace.translation.z,
                                )
                                .setOrientation(
                                    entityShadowNode,
                                    panelPoseInActivitySpace.rotation.x,
                                    panelPoseInActivitySpace.rotation.y,
                                    panelPoseInActivitySpace.rotation.z,
                                    panelPoseInActivitySpace.rotation.w,
                                )
                                .setParent(entityShadowNode, activitySpaceImpl.getNode())
                                .apply()
                        }
                        surfacePackage.release()
                    }
                }
            }
        }
        isVisible = true
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
