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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import java.util.UUID

/**
 * Describes the perception functionality that is required from a [PerceptionRuntime]
 * implementation.
 *
 * It is expected that these functions are only valid while the [PerceptionRuntime] is in a resumed
 * state.
 *
 * @property trackables the [Collection] of all known [Trackables][Trackable]
 * @property leftEye the left [Eye], or null if not available
 * @property rightEye the right [Eye], or null if not available
 * @property leftHand the left [Hand], or null if not available
 * @property rightHand the right [Hand], or null if not available
 * @property arDevice the [ArDevice] instance
 * @property leftRenderViewpoint the left [RenderViewpoint], or null if not available
 * @property rightRenderViewpoint the right [RenderViewpoint], or null if not available
 * @property monoRenderViewpoint the mono [RenderViewpoint], or null if not available
 * @property geospatial the [Geospatial] instance
 * @property leftDepth the left [Depth], or null if not available
 * @property rightDepth the right [Depth], or null if not available
 * @property monoDepth the mono [Depth], or null if not available
 * @property userFace the user's [Face], or null if not available
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PerceptionManager {
    /**
     * Defines a tracked location in the physical world.
     *
     * @param pose the [Pose] of the anchor
     * @return the created [Anchor]
     */
    public fun createAnchor(pose: Pose): Anchor

    /**
     * Performs a ray cast in the direction of the given [ray] in the latest camera view.
     *
     * @param ray the [Ray] to cast
     * @return a list of [HitResult] objects
     */
    public fun hitTest(ray: Ray): List<HitResult>

    /**
     * Retrieves all the [UUID] instances from [Anchor] objects that have been persisted.
     *
     * @return a list of [UUID]s
     */
    public fun getPersistedAnchorUuids(): List<UUID>

    /**
     * Loads an [Anchor] from local storage.
     *
     * @param uuid the [UUID] of the anchor to load
     * @return the loaded [Anchor]
     */
    public fun loadAnchor(uuid: UUID): Anchor

    /**
     * Deletes a persisted [Anchor] from local storage.
     *
     * @param uuid the [UUID] of the anchor to unpersist
     */
    public fun unpersistAnchor(uuid: UUID)

    /**
     * Sets the display rotation on supported devices.
     *
     * @param rotation the angle in degrees to rotate the display from its default orientation, with
     *   positive values representing clockwise rotations
     * @param width the new display width after rotation
     * @param height the new display width after rotation
     * @throws UnsupportedOperationException if the current runtime does not support display
     *   rotation
     */
    public fun setDisplayRotation(rotation: Int, width: Int, height: Int): Unit =
        throw UnsupportedOperationException()

    /**
     * Returns the maximum number of images that can be added to an
     * [androidx.xr.runtime.AugmentedImageDatabase]
     */
    public val imageDatabaseMaxLoadedImageCount: Int

    /** Returns if the device support physical size estimation. Used for image tracking. */
    public val isPhysicalSizeEstimationSupported: Boolean

    public val trackables: Collection<Trackable>
    public val leftEye: Eye?
    public val rightEye: Eye?
    public val leftHand: Hand?
    public val rightHand: Hand?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val arDevice: ArDevice
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val leftRenderViewpoint: RenderViewpoint?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val rightRenderViewpoint: RenderViewpoint?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val monoRenderViewpoint: RenderViewpoint?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val geospatial: Geospatial
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val leftDepth: Depth?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val rightDepth: Depth?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val monoDepth: Depth?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val userFace: Face?
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val conversationSceneSignal: ConversationState?
}
