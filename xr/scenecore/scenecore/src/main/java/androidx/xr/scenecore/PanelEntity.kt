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

package androidx.xr.scenecore

import android.content.Context
import android.view.View
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * PanelEntity contains an arbitrary 2D Android [View], within a spatialized XR scene.
 *
 * @param isMainPanelEntity True if this panel is the
 *   [MainPanelEntity](https://developer.android.com/develop/xr/jetpack-xr-sdk/work-with-entities),
 *   false otherwise.
 */
// TODO(ricknels): move isMainPanelEntity check to SceneRuntime and provide better kdocs
// for mainPanelEntity
public open class PanelEntity
internal constructor(
    private val lifecycleManager: LifecycleManager,
    rtEntity: RtPanelEntity,
    entityManager: EntityManager,
    @get:JvmName("isMainPanelEntity") public val isMainPanelEntity: Boolean = false,
) : BaseEntity<RtPanelEntity>(rtEntity, entityManager) {

    /** The corner radius of the PanelEntity, in meters. */
    public var cornerRadius: Float
        get() {
            checkNotDisposed()
            return rtEntity!!.cornerRadius
        }
        set(value) {
            checkNotDisposed()
            rtEntity!!.cornerRadius = value
        }

    /**
     * The dimensions of this PanelEntity in local space, in units relative to the scale of this
     * Entity's parent.
     */
    public var size: FloatSize2d
        get() {
            checkNotDisposed()
            return rtEntity!!.size.toFloatSize2d()
        }
        set(value) {
            checkNotDisposed()
            rtEntity!!.size = value.toRtDimensions()
        }

    /**
     * The dimensions of this PanelEntity, in pixels, which is the resolution of the underlying
     * surface.
     *
     * This API doesn't do any scale compensation to the pixel dimensions.
     */
    public var sizeInPixels: IntSize2d
        get() {
            checkNotDisposed()
            return rtEntity!!.sizeInPixels.toIntSize2d()
        }
        set(value) {
            checkNotDisposed()
            rtEntity!!.sizeInPixels = value.toRtPixelDimensions()
        }

    /**
     * Gets the perceived resolution of this Entity in the [CameraView].
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidCameraView] in Home Space Mode. For applications requiring
     * perceived resolution in Home Space Mode, see [MainPanelEntity.getPerceivedResolution].
     *
     * This value represents the dimensions of the Entity on the camera view if its largest surface
     * was facing the camera without changing the distance of the Entity to the camera. This can be
     * used by clients to dynamically optimize the resolution of assets within the PanelEntity, for
     * example by using lower-resolution assets within panels that are further from the viewer. The
     * Entity's own rotation and the camera's viewing direction are disregarded.
     *
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the Entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidCameraView] if the camera information required for
     *       the calculation is invalid or unavailable.
     *
     * @throws [IllegalStateException] if [Session.config] is not set to
     *   [Config.DeviceTrackingMode.LAST_KNOWN].
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(): PerceivedResolutionResult {
        checkNotDisposed()
        check(lifecycleManager.config.deviceTracking == Config.DeviceTrackingMode.LAST_KNOWN) {
            "Config.DeviceTrackingMode is not set to LastKnown."
        }
        return rtEntity!!.getPerceivedResolution().toPerceivedResolutionResult()
    }

    /**
     * Gets the 3D pose of a 2D pixel coordinate within the Entity's local space.
     *
     * This method's inputs use a 2D pixel coordinate system where:
     * - The origin (0, 0) is at the **top-left** corner of the panel.
     * - The +X axis points towards the **right** edge of the panel content.
     * - The +Y axis points towards the **bottom** edge of the panel content.
     *
     * Input values are floats to allow for sub-pixel accuracy. Values outside the panel's pixel
     * dimensions (e.g., `x < 0` or `y > panelHeight`) are permitted and will result in a pose
     * positioned outside the panel's surface.
     *
     * Note that calling this method on [MainPanelEntity] during [android.app.Activity.onCreate] can
     * result in incorrect values.
     *
     * @param coordinates The pixel coordinate, relative to the top-left origin.
     * @return The 3D pose corresponding to the 2D pixel coordinate. The position will be coplanar
     *   to the panel's surface (z=0 in the panel's local space), and the orientation will be the
     *   identity rotation such that the Z axis points outwards from the panel.
     * @see ScenePose.transformPoseTo to transform the pose to a different coordinate space.
     */
    @ExperimentalPanelCoordinateApi
    public fun transformPixelCoordinatesToPose(coordinates: Vector2): Pose {
        checkNotDisposed()
        return rtEntity!!.transformPixelCoordinatesToPose(coordinates)
    }

    /**
     * Gets the 3D pose of a 2D normalized extent coordinate within the Entity's local space.
     *
     * This method's inputs use a 2D normalized coordinate system where:
     * - The origin (0.0, 0.0) is at the **center** of the panel.
     * - The +X axis points towards the **right** edge (mapped to 1.0) of the panel content.
     * - The +Y axis points towards the **top** edge (mapped to 1.0) of the panel content.
     *
     * Values outside the [-1.0, 1.0] range are permitted and will result in a pose positioned
     * outside the panel's surface.
     *
     * Note that calling this method on [MainPanelEntity] during [android.app.Activity.onCreate] can
     * result in incorrect values.
     *
     * @param coordinates The normalized coordinates, relative to the origin at the center of the
     *   panel.
     * @return The 3D pose corresponding to the 2D normalized coordinate. The position will be
     *   coplanar to the panel's surface (z=0 in the panel's local space), and the orientation will
     *   be the identity rotation such that the Z axis points outwards from the panel.
     * @see ScenePose.transformPoseTo to transform the pose to a different coordinate space.
     */
    @ExperimentalPanelCoordinateApi
    public fun transformNormalizedCoordinatesToPose(coordinates: Vector2): Pose {
        checkNotDisposed()
        return rtEntity!!.transformNormalizedCoordinatesToPose(coordinates)
    }

    public companion object {
        internal fun create(
            lifecycleManager: LifecycleManager,
            context: Context,
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
            view: View,
            dimensions: FloatSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                sceneRuntime.createPanelEntity(
                    context,
                    pose,
                    view,
                    dimensions.toRtDimensions(),
                    name,
                    sceneRuntime.activitySpace,
                ),
                entityManager,
            )

        internal fun create(
            lifecycleManager: LifecycleManager,
            context: Context,
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
            view: View,
            pixelDimensions: IntSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                sceneRuntime.createPanelEntity(
                    context,
                    pose,
                    view,
                    pixelDimensions.toRtPixelDimensions(),
                    name,
                    sceneRuntime.activitySpace,
                ),
                entityManager,
            )

        /**
         * Factory method for a spatialized PanelEntity.
         *
         * @param session XR [Session] in which to create the PanelEntity.
         * @param view [View] to embed in this panel entity.
         * @param dimensions Spatialized dimensions for the underlying surface for the given view,
         *   in meters.
         * @param name Name of this PanelEntity.
         * @param pose [Pose] of this entity relative to its parent, default value is Identity.
         * @return a PanelEntity instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            view: View,
            dimensions: FloatSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity.create(
                session.perceptionRuntime.lifecycleManager,
                session.activity,
                session.sceneRuntime,
                session.scene.entityManager,
                view,
                dimensions,
                name,
                pose,
            )

        /**
         * Factory method for a spatialized PanelEntity.
         *
         * @param session XR [Session] in which to create the PanelEntity.
         * @param view [View] to embed in this panel entity.
         * @param pixelDimensions Dimensions for the underlying surface for the given view, in
         *   pixels.
         * @param name Name of the panel.
         * @param pose [Pose] of this PanelEntity relative to its parent, default value is Identity.
         * @return a PanelEntity instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            view: View,
            pixelDimensions: IntSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity.create(
                session.perceptionRuntime.lifecycleManager,
                session.activity,
                session.sceneRuntime,
                session.scene.entityManager,
                view,
                pixelDimensions,
                name,
                pose,
            )

        /** Returns the PanelEntity backed by the main window for the Activity. */
        internal fun createMainPanelEntity(
            lifecycleManager: LifecycleManager,
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                sceneRuntime.mainPanelEntity,
                entityManager,
                isMainPanelEntity = true,
            )
    }
}
