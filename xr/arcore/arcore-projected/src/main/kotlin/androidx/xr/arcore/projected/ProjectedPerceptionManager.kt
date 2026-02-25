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

package androidx.xr.arcore.projected

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.ArDevice
import androidx.xr.arcore.runtime.DepthMap
import androidx.xr.arcore.runtime.Eye
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.Hand
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.util.UUID

/**
 * Implementation of the perception capabilities of a runtime using Projected.
 *
 * @property timeSource the time source to use for the perception manager
 * @property trackables the collection of [Trackable] objects
 * @property leftEye the left [Eye], or null if not available
 * @property rightEye the right [Eye], or null if not available
 * @property leftHand the left [Hand], or null if not available
 * @property rightHand the right [Hand], or null if not available
 * @property geospatial the [Geospatial] instance
 * @property arDevice the [ArDevice] instance
 * @property leftRenderViewpoint the left [RenderViewpoint], or null if not available
 * @property rightRenderViewpoint the right [RenderViewpoint], or null if not available
 * @property monoRenderViewpoint the mono [RenderViewpoint], or null if not available
 * @property leftDepthMap the left [DepthMap], or null if not available
 * @property rightDepthMap the right [DepthMap], or null if not available
 * @property monoDepthMap the mono [DepthMap], or null if not available
 * @property userFace the user's [Face], or null if not available
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ProjectedPerceptionManager
internal constructor(private val timeSource: ProjectedTimeSource) : PerceptionManager {
    internal val xrResources = XrResources()

    /**
     * Creates an anchor in the scene.
     *
     * This method calls the [Session.createAnchor] method.
     *
     * @param pose the [Pose] of the anchor
     * @return the created [Anchor]
     */
    override fun createAnchor(pose: Pose): Anchor {
        throw NotImplementedError("Create anchor is currently not supported by Projected.")
    }

    /**
     * Performs a hit test against the scene.
     *
     * This method calls the [Frame.hitTest] method.
     *
     * @param ray the [Ray] to perform the hit test against
     * @return the list of [HitResult] objects
     */
    override fun hitTest(ray: Ray): List<HitResult> {
        throw NotImplementedError("Hit test is currently not supported by Projected.")
    }

    /**
     * Returns the UUIDs of all persisted anchors.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun getPersistedAnchorUuids(): List<UUID> {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    /**
     * Loads an anchor from the given UUID.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun loadAnchor(uuid: UUID): Anchor {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    /**
     * Unpersists an anchor with the given UUID.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun unpersistAnchor(uuid: UUID) {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    override val trackables: Collection<Trackable> = emptyList()

    override val leftEye: Eye? = null

    override val rightEye: Eye? = null

    override val leftHand: Hand? = null

    override val rightHand: Hand? = null

    override val geospatial: Geospatial
        get() = xrResources.geospatial

    override val arDevice: ArDevice
        get() = xrResources.arDevice

    override val leftRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(1f, 0f, 0f), Quaternion.Companion.Identity))

    override val rightRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(0f, 1f, 0f), Quaternion.Companion.Identity))

    override val monoRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(0f, 0f, 1f), Quaternion.Companion.Identity))

    override val leftDepthMap: DepthMap? = null

    override val rightDepthMap: DepthMap? = null

    override val monoDepthMap: DepthMap? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val userFace: Face? = null

    /**
     * Updates the perception manager.
     *
     * Sets the display geometry of the underlying [Session] if the display has changed. Grabs the
     * latest [Frame] from the underlying [Session], and if new, updates the internal state of the
     * perception manager.
     */
    internal fun update() {
        throw NotImplementedError("update is currently not supported by Projected.")
    }

    /**
     * Clears any internal state of the perception manager.
     *
     * Currently, this method only clears the [xrResources] instance.
     */
    internal fun clear() {
        throw NotImplementedError("clear is currently not supported by Projected.")
    }

    public fun setDisplayRotation(rotation: Int, width: Int, height: Int) {
        throw NotImplementedError("setDisplayRotation is currently not supported by Projected.")
    }
}
