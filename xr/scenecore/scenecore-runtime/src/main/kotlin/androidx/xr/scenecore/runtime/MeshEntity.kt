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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/** Interface for a Mesh entity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MeshEntity : Entity {
    /**
     * Retrieves the axis-aligned bounding box (AABB) of the mesh in meters in the model's local
     * coordinate space.
     *
     * @return A [BoundingBox] object representing the mesh's bounding box.
     */
    public val meshBoundingBox: BoundingBox

    /** The current affordance state based on user interaction. */
    public var affordanceState: GeometryAffordanceState

    /** The recommended affordance scale based on user interaction. */
    public var recommendedAffordanceScale: Vector3

    /**
     * The recommended affordance rotation based on user interaction.
     *
     * This is the rotation the rendering runtime's rotation gesture recommends for the entity,
     * expressed in the runtime's world space (the platform reference space in which the entity's
     * subspace transform is reported), not in the entity's parent space. While no rotation gesture
     * is in progress it tracks the entity's current world rotation; it is only meaningful while the
     * [ReformAffordanceFlag.ROTATABLE] affordance is enabled.
     */
    public var recommendedAffordanceRotation: Quaternion

    /**
     * Sets a material for a mesh subset.
     *
     * @param material The new [MaterialResource] to apply to the mesh subset.
     * @param subsetIndex The zero-based index for the mesh subset.
     */
    public fun setMaterial(material: MaterialResource, subsetIndex: Int)

    /**
     * Sets the transforms for the bones in the skinned mesh.
     *
     * @param transforms A list of [Matrix4] objects representing the new bone transforms.
     */
    public fun setBoneTransforms(transforms: List<Matrix4>)

    /**
     * Enable/disable the reform affordances for [MeshEntity].
     *
     * @param enabled Whether the reform affordances should be enabled.
     * @param reformAffordanceFlag Flag of reform affordance to enable/disable.
     */
    public fun setReformAffordanceEnabled(
        enabled: Boolean,
        reformAffordanceFlag: ReformAffordanceFlag,
    )

    /**
     * Enable/disable the collider for the [MeshEntity].
     *
     * @param enabled Whether the collider should be enabled.
     */
    public fun setColliderEnabled(enabled: Boolean)
}

/** Provide the rendering implementation for [MeshEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MeshFeature : RenderingFeature {
    /**
     * Retrieves the axis-aligned bounding box (AABB) of the mesh in meters in the model's local
     * coordinate space.
     */
    public val meshBoundingBox: BoundingBox

    /**
     * Sets a material for a mesh subset.
     *
     * @param material The new [MaterialResource] to apply to the mesh subset.
     * @param subsetIndex The zero-based index for the mesh subset.
     */
    public fun setMaterial(material: MaterialResource, subsetIndex: Int)

    /**
     * Sets the transforms for the bones in the skinned mesh.
     *
     * @param transforms A list of [Matrix4] objects representing the new bone transforms.
     */
    public fun setBoneTransforms(transforms: List<Matrix4>)

    /**
     * Adds reform affordance to the passed [MeshEntity].
     *
     * @param entity The MeshEntity to attach the reform affordance to.
     * @param enabled Whether the affordance is enabled.
     * @param reformFlag The reform affordance flag to enable/disable.
     */
    public fun setReformAffordanceEnabled(
        entity: MeshEntity,
        enabled: Boolean,
        reformFlag: ReformAffordanceFlag,
    )

    /**
     * Enable/disable the collider for the [MeshEntity].
     *
     * @param enabled Whether the collider should be enabled.
     */
    public fun setColliderEnabled(enabled: Boolean)
}
