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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity as RtEntity

/**
 * A SpaceEntity is a special Entity subtype whose position is independent of other SpaceEntity. It
 * is parentless and its position and scale are controlled by the system. Calling [setPose],
 * [setScale], or setting the [Entity.parent] property on a SpaceEntity will result in an
 * [UnsupportedOperationException].
 *
 * Examples of SpaceEntity are [ActivitySpace] and [AnchorSpace].
 */
public abstract class SpaceEntity
internal constructor(rtEntity: RtEntity, private val entityRegistry: EntityRegistry) :
    Entity(rtEntity, entityRegistry) {

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The `SpaceEntity` has no parent; retrieving the parent will return null.
     *
     * @throws UnsupportedOperationException when setting
     */
    override var parent: Entity? = null
        set(_) {
            throw UnsupportedOperationException("Cannot set 'parent' on a SpaceEntity.")
        }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The pose of the `SpaceEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param pose The new pose to set.
     * @param relativeTo The space in which the pose is defined. Defaults to [Space.PARENT].
     * @throws UnsupportedOperationException if called.
     */
    override fun setPose(pose: Pose, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'pose' on a SpaceEntity.")
    }

    /**
     * Returns the pose of the `SpaceEntity` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the pose relative to. Defaults to
     *   [Space.PARENT].
     * @return The current pose of the `SpaceEntity`.
     * @throws IllegalArgumentException if called with Space.PARENT since SpaceEntity has no
     *   parents.
     */
    override fun getPose(relativeTo: Space): Pose {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "Cannot get a pose relative to the parent of a SpaceEntity, it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653
            Space.REAL_WORLD -> super.getPose(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo.")
        }
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of an `SpaceEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined. Defaults to [Space.PARENT].
     * @throws UnsupportedOperationException if called.
     */
    override fun setScale(scale: Float, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'scale' on a SpaceEntity.")
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of the `SpaceEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined. Defaults to [Space.PARENT].
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun setScale(scale: Vector3, relativeTo: Space) {
        throw UnsupportedOperationException("Cannot set 'scale' on a SpaceEntity.")
    }

    /**
     * Returns the scale of the `SpaceEntity` along each axis, relative to the specified coordinate
     * space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `SpaceEntity` along each axis.
     * @throws IllegalArgumentException if called with Space.PARENT since SpaceEntity has no
     *   parents.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun getNonUniformScale(relativeTo: Space): Vector3 {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "A SpaceEntity is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653
            Space.REAL_WORLD -> super.getNonUniformScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo.")
        }
    }

    /**
     * Returns the scale of the `SpaceEntity` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `SpaceEntity`.
     * @throws IllegalArgumentException if called with Space.PARENT since SpaceEntity has no
     *   parents.
     */
    override fun getScale(relativeTo: Space): Float {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "A SpaceEntity is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
            Space.REAL_WORLD -> super.getScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo.")
        }
    }
}
