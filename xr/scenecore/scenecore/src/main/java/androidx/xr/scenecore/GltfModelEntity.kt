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

import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.GltfEntity as RtGltfEntity
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * GltfModelEntity is a concrete implementation of Entity that hosts a glTF model.
 *
 * Note: The size property of this Entity is always reported as {0, 0, 0}, regardless of the actual
 * size of the model.
 */
public class GltfModelEntity
private constructor(rtGltfEntity: RtGltfEntity, entityRegistry: EntityRegistry) :
    Entity(rtGltfEntity, entityRegistry) {

    internal val rtGltfEntity: RtGltfEntity
        get() = rtEntity as RtGltfEntity

    private val _nodes: List<GltfModelNode> by lazy {
        // The unique identifier of a node is their index so we first get the
        // count of the nodes in the model from the native side.
        val features = rtGltfEntity.nodes
        val list = ArrayList<GltfModelNode>(features.size)

        for (i in features.indices) {
            // For each node index in the model, query its name from the native side
            // and create a [GltfModelNode]. A node may have no name (`null`).
            val feature = features[i]
            list.add(GltfModelNode(this, feature, i, feature.name))
        }
        list.toList()
    }

    /**
     * A list of all [GltfModelNode]s defined in the [GltfModelEntity]. The list is lazily
     * initialized on the first access.
     *
     * The returned list corresponds to the flattened array of nodes defined in the source glTF
     * file. The order of elements in this list is guaranteed to match the order of nodes in the
     * glTF file's `nodes` array.
     */
    public val nodes: List<GltfModelNode>
        @MainThread
        get() {
            checkNotDisposed()
            return _nodes
        }

    @delegate:RequiresApi(Build.VERSION_CODES.O)
    private val _animations: List<GltfAnimation> by lazy {
        // The unique identifier of an animation is their index so we first get the
        // count of the nodes in the model from the native side.
        val features = rtGltfEntity.animations
        val list = ArrayList<GltfAnimation>(features.size)

        for (i in features.indices) {
            // For each animation index in the model, query its name from the native side
            // and create a [GltfAnimation]. An animation may have no name ("").
            val feature = features[i]
            list.add(
                GltfAnimation(
                    rtGltfEntity = rtGltfEntity,
                    rtGltfAnimation = feature,
                    index = feature.animationIndex,
                    name = feature.animationName,
                    // The animation duration is in seconds [Float]. We convert it to the [Duration]
                    // datatype.
                    duration =
                        java.time.Duration.ofMillis(
                            (feature.animationDuration * TimeUnit.SECONDS.toMillis(1)).toLong()
                        ),
                )
            )
        }
        Collections.unmodifiableList(list)
    }

    /**
     * A list of all [GltfAnimation]s defined in the [GltfModelEntity]. The list is lazily
     * initialized on the first access.
     *
     * The returned list corresponds to the array of animations defined in the source glTF file. The
     * order of elements in this list is guaranteed to match the order of animations in the glTF
     * file's `animations` array.
     */
    @get:RequiresApi(Build.VERSION_CODES.O)
    public val animations: List<GltfAnimation>
        @MainThread
        get() {
            checkNotDisposed()
            return _animations
        }

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model in meters in the
     * model's local coordinate space.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    internal val gltfModelBoundingBox: BoundingBox
        @MainThread get() = rtGltfEntity.gltfModelBoundingBox

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model in meters in the
     * model's local coordinate space.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    // TODO - b/501059605: Make the property public and remove this getter.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @ExperimentalGltfComposeMethod
    public fun getGltfModelBoundingBox(): BoundingBox = gltfModelBoundingBox

    public companion object {
        /**
         * Factory method for GltfModelEntity.
         *
         * @param sceneRuntime SceneRuntime.
         * @param renderingRuntime RenderingRuntime.
         * @param model [GltfModel] which this entity will display.
         * @param pose Pose for this [GltfModelEntity], relative to its parent.
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         */
        @Suppress("RestrictedApiAndroidX")
        internal fun create(
            sceneRuntime: SceneRuntime,
            renderingRuntime: RenderingRuntime,
            entityRegistry: EntityRegistry,
            model: GltfModel,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityRegistry.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): GltfModelEntity =
            GltfModelEntity(
                    renderingRuntime.createGltfEntity(pose, model.model, parent?.rtEntity),
                    entityRegistry,
                )
                .also { it.parent = parent }

        /**
         * Public factory function for a [GltfModelEntity].
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session [Session] to create the [GltfModel] in.
         * @param model The [GltfModel] this [Entity] is referencing.
         * @param pose The initial [Pose] of the [Entity]. The default value is [Pose.Identity].
         * @param parent Parent entity. Defaults to `null`. If `null`, the entity is created but not
         *   attached to the scene graph and will be invisible. When a parent entity (e.g.,
         *   [ActivitySpace] or any other [Entity] already present in the scene) is assigned later,
         *   the entity will remain invisible until you explicitly enable it by calling
         *   [Entity.setEnabled] (enabled=true). This allows for [Entity] pre-configuration before
         *   making it visible.
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            model: GltfModel,
            pose: Pose = Pose.Identity,
            parent: Entity? = null,
        ): GltfModelEntity =
            create(
                session.sceneRuntime,
                session.renderingRuntime,
                session.scene.entityRegistry,
                model,
                pose,
                parent,
            )
    }
}

// Annotation for Gltf-specific restricted LIBRARY_GROUP_PREFIX APIs that have not been finalized.
// The annotation itself is also restricted, to match the methods being annotated.
@RequiresOptIn(
    "This API is experimental and used exclusively by XR Compose. It is not supported for general use. (b/501059605)"
)
@Retention(AnnotationRetention.BINARY)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public annotation class ExperimentalGltfComposeMethod
