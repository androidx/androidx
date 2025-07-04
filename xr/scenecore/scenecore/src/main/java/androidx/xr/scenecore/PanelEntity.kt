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

package androidx.xr.scenecore

import android.content.Context
import android.view.View
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose

/**
 * PanelEntity contains an arbitrary 2D Android [View], within a spatialized XR scene.
 *
 * @param isMainPanelEntity True if this panel is the
 *   [MainPanelEntity](https://developer.android.com/develop/xr/jetpack-xr-sdk/work-with-entities),
 *   false otherwise.
 */
// TODO(ricknels): move isMainPanelEntity check to JxrPlatformAdapter and provide better kdocs
// for mainPanelEntity
@Suppress("HiddenSuperclass") // BaseEntity is an internal class
public open class PanelEntity
internal constructor(
    private val lifecycleManager: LifecycleManager,
    rtEntity: RtPanelEntity,
    entityManager: EntityManager,
    @get:JvmName("isMainPanelEntity") public val isMainPanelEntity: Boolean = false,
) : BaseEntity<RtPanelEntity>(rtEntity, entityManager) {

    /** The corner radius of the PanelEntity, in meters. */
    public var cornerRadius: Float
        get() = rtEntity.cornerRadius
        set(value) {
            rtEntity.cornerRadius = value
        }

    /**
     * The dimensions of this PanelEntity in local space, in units relative to the scale of this
     * Entity's parent.
     */
    public var size: FloatSize2d
        get() = rtEntity.size.toFloatSize2d()
        set(value) {
            rtEntity.size = value.toRtDimensions()
        }

    /**
     * The dimensions of this PanelEntity, in pixels, which is the resolution of the underlying
     * surface.
     *
     * This API doesn't do any scale compensation to the pixel dimensions.
     */
    public var sizeInPixels: IntSize2d
        get() = rtEntity.sizeInPixels.toIntSize2d()
        set(value) {
            rtEntity.sizeInPixels = value.toRtPixelDimensions()
        }

    /**
     * Gets the perceived resolution of this Entity in the [CameraView].
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidCameraView] in Home Space Mode.
     *
     * The Entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the Entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the Entity to the camera.
     *
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the Entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidCameraView] if the camera information required for
     *       the calculation is invalid or unavailable.
     *
     * @throws [IllegalStateException] if [Session.config.headTracking] is set to
     *   [Config.HeadTrackingMode.DISABLED].
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(): PerceivedResolutionResult {
        check(lifecycleManager.config.headTracking != Config.HeadTrackingMode.DISABLED) {
            "Config.HeadTrackingMode is set to Disabled."
        }
        return rtEntity.getPerceivedResolution().toPerceivedResolutionResult()
    }

    public companion object {
        internal fun create(
            lifecycleManager: LifecycleManager,
            context: Context,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            view: View,
            dimensions: FloatSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                adapter.createPanelEntity(
                    context,
                    pose,
                    view,
                    dimensions.toRtDimensions(),
                    name,
                    adapter.activitySpaceRootImpl,
                ),
                entityManager,
            )

        internal fun create(
            lifecycleManager: LifecycleManager,
            context: Context,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            view: View,
            pixelDimensions: IntSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                adapter.createPanelEntity(
                    context,
                    pose,
                    view,
                    pixelDimensions.toRtPixelDimensions(),
                    name,
                    adapter.activitySpaceRootImpl,
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
                session.runtime.lifecycleManager,
                session.activity,
                session.platformAdapter,
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
                session.runtime.lifecycleManager,
                session.activity,
                session.platformAdapter,
                session.scene.entityManager,
                view,
                pixelDimensions,
                name,
                pose,
            )

        /** Returns the PanelEntity backed by the main window for the Activity. */
        internal fun createMainPanelEntity(
            lifecycleManager: LifecycleManager,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
        ): PanelEntity =
            PanelEntity(
                lifecycleManager,
                adapter.mainPanelEntity,
                entityManager,
                isMainPanelEntity = true,
            )
    }
}
