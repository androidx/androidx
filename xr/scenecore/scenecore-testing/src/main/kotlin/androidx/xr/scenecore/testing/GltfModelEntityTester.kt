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

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelNode
import androidx.xr.scenecore.runtime.GltfAnimationFeature as RtGltfAnimation
import androidx.xr.scenecore.runtime.GltfModelNodeFeature as RtGltfModelNode
import androidx.xr.scenecore.testing.internal.FakeGltfAnimationFeature as InternalFakeGltfAnimationFeature
import androidx.xr.scenecore.testing.internal.FakeGltfEntity as InternalFakeGltfEntity
import androidx.xr.scenecore.testing.internal.FakeGltfModelNodeFeature as InternalFakeGltfModelNodeFeature

/**
 * [GltfModelEntityTester] is a testing accessor used to inspect the internal state of a
 * [GltfModelEntity] within a test environment.
 *
 * It utilizes reflection to bridge the [GltfModelEntity] with the fake runtime, allowing tests to
 * verify nodes, animations, and bounding box data that are otherwise encapsulated or tied to the
 * underlying runtime implementation.
 */
public class GltfModelEntityTester
internal constructor(
    private val rtEntity: InternalFakeGltfEntity,
    internal val gltfModelEntity: GltfModelEntity,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [GltfModelEntity].
         *
         * This function provides a [GltfModelEntityTester] instance, which can be used to inspect
         * and manipulate its underlying data in the test environment.
         *
         * @param gltfModelEntity The entity for which to retrieve the test data accessor.
         * @return A [GltfModelEntityTester] instance for the given entity, or `null` if its
         *   corresponding runtime data could not be located.
         */
        internal fun create(gltfModelEntity: GltfModelEntity): GltfModelEntityTester {
            return GltfModelEntityTester(
                @Suppress("DEPRECATION") (gltfModelEntity.rtEntity as FakeGltfEntity).fakeInternal
                    as InternalFakeGltfEntity,
                gltfModelEntity,
            )
        }
    }

    /**
     * Creates and adds a [TestGltfModelNode] to the entity using the properties of the given
     * [GltfModelEntity].
     *
     * This simulates the attachment of a new node to the underlying runtime entity.
     *
     * @param node The test node feature containing the properties for the new [TestGltfModelNode].
     */
    public fun addNode(node: TestGltfModelNode) {
        val rtGltfModelNode =
            InternalFakeGltfModelNodeFeature(node.name).apply {
                localPose = node.localPose
                localScale = node.localScale
                modelPose = node.modelPose
                modelScale = node.modelScale
            }
        node.rtGltfModelNode = rtGltfModelNode
        rtEntity.addNode(rtGltfModelNode as RtGltfModelNode)
    }

    /**
     * Creates and adds a [TestGltfAnimation] to the entity using the properties of the given
     * [GltfModelEntity].
     *
     * This simulates the addition of a new animation to the underlying runtime entity.
     *
     * @param animation The test animation feature containing the properties for the new
     *   [TestGltfAnimation].
     */
    public fun addAnimation(animation: TestGltfAnimation) {
        val rtGltfAnimation =
            InternalFakeGltfAnimationFeature(
                animation.animationName,
                animation.animationIndex,
                animation.animationDuration,
            )
        animation.rtGltfAnimation = rtGltfAnimation
        rtEntity.addAnimation(rtGltfAnimation as RtGltfAnimation)
    }

    /**
     * Configures the axis-aligned bounding box (AABB) of the glTF model in meters, relative to the
     * model's local coordinate space.
     */
    public var gltfModelBoundingBox: BoundingBox
        get() = rtEntity.gltfModelBoundingBox
        set(value) {
            rtEntity.setGltfModelBoundingBox(value)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GltfModelEntityTester

        if (rtEntity != other.rtEntity) return false
        if (gltfModelEntity != other.gltfModelEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtEntity.hashCode()
        result = 31 * result + gltfModelEntity.hashCode()
        return result
    }
}

/**
 * A test-only data class that mirrors the properties of a [GltfModelNode].
 *
 * This is used to define the characteristics of a node to be added to a [GltfModelEntityTester] for
 * testing purposes.
 *
 * @property name The name of the node.
 * @property localPose The local pose (position and rotation) of the node relative to its immediate
 *   parent.
 * @property localScale The local scale of the node relative to its immediate parent.
 * @property modelPose The pose (position and rotation) of the node relative to the model's root
 *   node.
 * @property modelScale The scale of the node relative to the model's root node.
 */
public class TestGltfModelNode
internal constructor(
    public val name: String? = "test_node",
    public val localPose: Pose = Pose.Identity,
    public val localScale: Vector3 = Vector3.One,
    public val modelPose: Pose = Pose.Identity,
    public val modelScale: Vector3 = Vector3.One,
) {
    internal var rtGltfModelNode: RtGltfModelNode? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestGltfModelNode

        if (name != other.name) return false
        if (localPose != other.localPose) return false
        if (localScale != other.localScale) return false
        if (modelPose != other.modelPose) return false
        if (modelScale != other.modelScale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + localPose.hashCode()
        result = 31 * result + localScale.hashCode()
        result = 31 * result + modelPose.hashCode()
        result = 31 * result + modelScale.hashCode()
        return result
    }

    /** Builder for [TestGltfModelNode]. */
    public class Builder {
        private var name: String? = "test_node"
        private var localPose: Pose = Pose.Identity
        private var localScale: Vector3 = Vector3.One
        private var modelPose: Pose = Pose.Identity
        private var modelScale: Vector3 = Vector3.One

        public fun setName(name: String?): Builder = apply { this.name = name }

        public fun setLocalPose(localPose: Pose): Builder = apply { this.localPose = localPose }

        public fun setLocalScale(localScale: Vector3): Builder = apply {
            this.localScale = localScale
        }

        public fun setModelPose(modelPose: Pose): Builder = apply { this.modelPose = modelPose }

        public fun setModelScale(modelScale: Vector3): Builder = apply {
            this.modelScale = modelScale
        }

        public fun build(): TestGltfModelNode {
            return TestGltfModelNode(
                name = name,
                localPose = localPose,
                localScale = localScale,
                modelPose = modelPose,
                modelScale = modelScale,
            )
        }
    }
}

/**
 * A test-only data class that mirrors the properties of a [GltfAnimation].
 *
 * This is used to define the characteristics of an animation to be added to a
 * [GltfModelEntityTester] for testing purposes.
 *
 * @property animationName The optional name of the animation.
 * @property animationIndex The index of the animation in the glTF model file.
 * @property animationDuration The duration of the animation in seconds.
 */
public class TestGltfAnimation
internal constructor(
    public val animationName: String? = "animation_name",
    public val animationIndex: Int = 0,
    public val animationDuration: Float = 1.0f,
) {
    internal var rtGltfAnimation: RtGltfAnimation? = null

    /**
     * Checks whether the animation is currently configured to loop.
     *
     * This reflects the underlying runtime state typically modified by [GltfAnimation.start].
     *
     * @return `true` if the animation is set to loop, `false` otherwise.
     */
    @get:JvmName("shouldLoop")
    public val shouldLoop: Boolean
        get() {
            val rtAnim =
                checkNotNull(rtGltfAnimation as? InternalFakeGltfAnimationFeature) {
                    "Animation must be added to a GltfModelEntityTester before accessing its runtime state."
                }
            return rtAnim.isLooping
        }

    /**
     * Retrieves the current playback speed of the animation.
     *
     * This reflects the underlying runtime state typically modified by [GltfAnimation.start] or
     * [GltfAnimation.setSpeed].
     *
     * @return The playback speed multiplier of the animation.
     */
    public val speed: Float
        get() {
            val rtAnim =
                checkNotNull(rtGltfAnimation as? InternalFakeGltfAnimationFeature) {
                    "Animation must be added to a GltfModelEntityTester before accessing its runtime state."
                }
            return rtAnim.speed
        }

    /**
     * Retrieves the seek start time (in seconds) for the animation.
     *
     * This reflects the underlying runtime state typically configured when starting the animation
     * via [GltfAnimation.start].
     *
     * @return The seek start time of the animation in seconds.
     */
    public val seekStartTime: Float
        get() {
            val rtAnim =
                checkNotNull(rtGltfAnimation as? InternalFakeGltfAnimationFeature) {
                    "Animation must be added to a GltfModelEntityTester before accessing its runtime state."
                }
            return rtAnim.seekStartTimeSeconds
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestGltfAnimation

        if (animationIndex != other.animationIndex) return false
        if (animationDuration != other.animationDuration) return false
        if (animationName != other.animationName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = animationIndex
        result = 31 * result + animationDuration.hashCode()
        result = 31 * result + (animationName?.hashCode() ?: 0)
        return result
    }

    /** Builder for [TestGltfAnimation]. */
    public class Builder {
        private var animationName: String? = "animation_name"
        private var animationIndex: Int = 0
        private var animationDuration: Float = 1.0f

        public fun setAnimationName(animationName: String?): Builder = apply {
            this.animationName = animationName
        }

        public fun setAnimationIndex(animationIndex: Int): Builder = apply {
            this.animationIndex = animationIndex
        }

        public fun setAnimationDuration(animationDuration: Float): Builder = apply {
            this.animationDuration = animationDuration
        }

        public fun build(): TestGltfAnimation {
            return TestGltfAnimation(
                animationName = animationName,
                animationIndex = animationIndex,
                animationDuration = animationDuration,
            )
        }
    }
}
