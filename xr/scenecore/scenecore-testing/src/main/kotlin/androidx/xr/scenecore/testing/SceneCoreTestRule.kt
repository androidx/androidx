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

import android.media.AudioTrack
import android.media.MediaPlayer
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.BoundsComponent
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MainPanelEntity
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceptionSpace
import androidx.xr.scenecore.PointerCaptureComponent
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SpatialAudioTrack
import androidx.xr.scenecore.SpatialAudioTrackBuilder
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialWindow
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.testing.internal.FakePerceptionSpaceScenePose
import androidx.xr.scenecore.testing.internal.FakeRenderingRuntime
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime
import org.junit.rules.ExternalResource

/**
 * A JUnit Rule for establishing a test environment for SceneCore applications.
 *
 * This rule provides fake implementations and test accessors for SceneCore components, allowing you
 * to configure and verify the internal runtime states of SceneCore's public API objects like
 * [Entity] and [Component].
 *
 * To enable the test rule to manage the lifecycle and state of SceneCore objects, you must register
 * the created instance with the rule after its creation using the [createTester] method provided by
 * this rule. These methods return test-specific accessors that allow inspection and manipulation of
 * the underlying fake instance.
 *
 * Example usage:
 * ```kotlin
 * @Rule @JvmField val testRule = SceneCoreTestRule()
 *
 * // Inside a test method:
 * // Create a SceneCore Entity
 * val entity = PanelEntity.create(...)
 * // Register it with the rule and get the test accessor
 * val entityTester = testRule.createTester<PanelEntityTester>(entity)
 * // Use entityTester to interact with the entity's state
 *
 * // Create a SceneCore Component
 * val component = MovableComponent.create(...)
 * // Register it and get the test accessor
 * val componentTester = testRule.createTester<MovableComponentTester>(component)
 * // Use componentTester to interact with the component's state
 * ```
 */
public class SceneCoreTestRule : ExternalResource() {

    private inline fun <T> requireRuntimesReady(block: () -> T): T {
        check(FakeSceneRuntime.instance != null && FakeRenderingRuntime.instance != null) {
            "SceneCore testing runtimes are not ready. Did you create a Session?"
        }
        return block()
    }

    // TODO: b/512223711 - Add testers for specific entities/components in follow-up PRs.
    @PublishedApi
    internal fun resolveTesterInternal(entity: Entity): Any? {
        return if (entity::class == Entity::class) {
            EntityTester.create(entity)
        } else {
            when (entity) {
                is ActivityPanelEntity -> ActivityPanelEntityTester.create(entity)
                is AnchorEntity -> AnchorEntityTester.create(entity)
                is GltfModelEntity -> GltfModelEntityTester.create(entity)
                is MeshEntity -> MeshEntityTester.create(entity)
                is PanelEntity -> PanelEntityTester.create(entity)
                is SurfaceEntity -> SurfaceEntityTester.create(entity)
                else -> null
            }
        }
    }

    // TODO: b/512223711 - Add testers for specific entities/components in follow-up PRs.
    @PublishedApi
    internal fun resolveTesterInternal(component: Component): Any? {
        return when (component) {
            is BoundsComponent -> BoundsComponentTester.create(component)
            is InteractableComponent -> InteractableComponentTester.create(component)
            is MovableComponent -> MovableComponentTester.create(component)
            is PointerCaptureComponent -> PointerCaptureComponentTester.create(component)
            is PositionalAudioComponent -> PositionalAudioComponentTester.create(component)
            is ResizableComponent -> ResizableComponentTester.create(component)
            else -> null
        }
    }

    /**
     * Creates a specific test data accessor for the given [Entity].
     *
     * In the test environment, entities have corresponding underlying fake data. This function
     * provides a convenient, type-safe way to access the specific tester associated with an
     * entity's subclass, allowing for verification or manipulation in tests.
     *
     * Example usage:
     * ```
     * val entityTester = rule.createTester<PanelEntityTester>(myPanelEntity)
     * ```
     *
     * @param T The expected type of the entity tester (e.g., `PanelEntityTester`).
     * @param entity The [Entity] instance for which to retrieve the test data accessor.
     * @return A tester instance of type [T].
     * @throws IllegalArgumentException If the underlying tester created for the entity does not
     *   match the expected type [T].
     */
    public inline fun <reified T : Any> createTester(entity: Entity): T {
        val tester =
            resolveTesterInternal(entity)
                ?: throw IllegalArgumentException(
                    "Unsupported entity type: ${entity::class.simpleName}"
                )

        return tester as? T
            ?: throw IllegalArgumentException(
                "Expected tester of type ${T::class.simpleName}, but actual entity created a ${tester::class.simpleName}"
            )
    }

    /**
     * Creates a specific test data accessor for the given [Component].
     *
     * In the test environment, components have corresponding underlying fake data. This function
     * provides a convenient, type-safe way to access the specific tester associated with a
     * component's subclass, allowing for verification or manipulation in tests.
     *
     * Example usage:
     * ```
     * val componentTester = rule.createTester<BoundsComponentTester>(myBoundsComponent)
     * ```
     *
     * @param T The expected type of the component tester (e.g., `BoundsComponentTester`).
     * @param component The [Component] instance for which to retrieve the test data accessor.
     * @return A tester instance of type [T].
     * @throws IllegalArgumentException If the underlying tester created for the component does not
     *   match the expected type [T], or if no tester is found.
     */
    public inline fun <reified T : Any> createTester(component: Component): T {
        val tester =
            resolveTesterInternal(component)
                ?: throw IllegalArgumentException(
                    "Unsupported component type: ${component::class.simpleName}"
                )

        return tester as? T
            ?: throw IllegalArgumentException(
                "Expected tester of type ${T::class.simpleName}, but actual component created a ${tester::class.simpleName}"
            )
    }

    /**
     * Retrieves a test data accessor for the given [GltfModel].
     *
     * In the test environment, each model created via [GltfModel.create] has corresponding
     * underlying fake data. This function provides access to that fake data, allowing for
     * verification or manipulation in tests.
     *
     * @param gltfModel The [GltfModel] instance for which to retrieve test data.
     * @return A [GltfModelTester] instance used to inspect and manipulate the test data.
     */
    public fun createTester(gltfModel: GltfModel): GltfModelTester {
        return GltfModelTester.create(gltfModel)
    }

    /**
     * Creates the test data accessor for the given [MediaPlayer].
     *
     * This class provides a mechanism for tests to inspect and verify spatial audio attributes
     * associated with a [MediaPlayer] that are otherwise encapsulated within the SceneCore runtime.
     *
     * @param mediaPlayer The [MediaPlayer] audio attributes are associated with.
     * @return A [SpatialMediaPlayerTester] instance used to inspect and manipulate the test data.
     */
    public fun createTester(mediaPlayer: MediaPlayer): SpatialMediaPlayerTester {
        val rtInstance = requireRuntimesReady {
            requireNotNull(FakeSceneRuntime.instance).mediaPlayerExtensionsWrapper
        }

        return SpatialMediaPlayerTester(rtInstance, mediaPlayer)
    }

    /**
     * Creates a test data accessor for the given [ImageBasedLightingAsset].
     *
     * In the test environment, each asset created via [ImageBasedLightingAsset.createFromZip] has
     * corresponding underlying fake data. This function provides access to that fake data, allowing
     * for verification or manipulation in tests.
     *
     * @param asset The [ImageBasedLightingAsset] instance for which to retrieve test data.
     * @return A [ImageBasedLightingAssetTester] instance used to inspect and manipulate the test
     *   data.
     */
    public fun createTester(asset: ImageBasedLightingAsset): ImageBasedLightingAssetTester {
        return ImageBasedLightingAssetTester.create(asset)
    }

    /**
     * Retrieves a test data accessor for the given [SoundEffectPool].
     *
     * In the test environment, each [SoundEffectPool] created via [SoundEffectPool.create] has
     * corresponding underlying fake data. This function provides access to that fake data, allowing
     * for verification or manipulation in tests.
     *
     * @param soundEffectPool The [SoundEffectPool] instance for which to retrieve test data.
     * @return A [SoundEffectPoolTester] instance used to inspect and manipulate the test data.
     */
    public fun createTester(soundEffectPool: SoundEffectPool): SoundEffectPoolTester =
        SoundEffectPoolTester.create(soundEffectPool)

    /**
     * Creates a test data accessor for the given [Texture].
     *
     * In the test environment, each texture created via [Texture.create] has corresponding
     * underlying fake data. This function provides access to that fake data, allowing for
     * verification or manipulation in tests.
     *
     * @param texture The [Texture] instance for which to retrieve test data.
     * @return A [TextureTester] instance used to inspect and manipulate the test data.
     */
    public fun createTester(texture: Texture): TextureTester = TextureTester.create(texture)

    private var _sceneTester: SceneTester? = null

    /**
     * Provides access to a controller for simulating runtime spatial states of the [Scene].
     *
     * Use this to test how your application responds to changes in various spatial-related runtime
     * states, such as visibility, capability and so on.
     */
    public val sceneTester: SceneTester
        get() {
            if (_sceneTester != null) {
                return _sceneTester!!
            }

            _sceneTester = requireRuntimesReady {
                SceneTester(requireNotNull(FakeSceneRuntime.instance))
            }

            return _sceneTester!!
        }

    private var _activitySpaceTester: ActivitySpaceTester? = null

    /**
     * Provides the instance of [ActivitySpaceTester] which is a test data accessor of the
     * [ActivitySpace].
     *
     * The [ActivitySpace] is an [Entity] used to track the system-managed pose and boundary of the
     * volume associated with a spatialized Activity. The Application cannot directly control this
     * volume, but the system might update it in response to the User moving it or entering or
     * exiting Full Space Mode.
     */
    public val activitySpaceTester: ActivitySpaceTester
        get() {
            if (_activitySpaceTester != null) {
                return _activitySpaceTester!!
            }

            _activitySpaceTester = requireRuntimesReady {
                ActivitySpaceTester(requireNotNull(FakeSceneRuntime.instance).activitySpace)
            }

            return _activitySpaceTester!!
        }

    private var _perceptionSpaceTester: PerceptionSpaceTester? = null

    /**
     * Provides the instance of [PerceptionSpaceTester] which is a test data accessor of the
     * [PerceptionSpace].
     *
     * The [PerceptionSpace] represents the origin of the space in which ARCore for Jetpack XR
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public val perceptionSpaceTester: PerceptionSpaceTester
        get() {
            if (_perceptionSpaceTester != null) {
                return _perceptionSpaceTester!!
            }

            _perceptionSpaceTester = requireRuntimesReady {
                PerceptionSpaceTester(
                    requireNotNull(FakeSceneRuntime.instance).perceptionSpaceActivityPose
                        as FakePerceptionSpaceScenePose
                )
            }

            return _perceptionSpaceTester!!
        }

    private var _mainPanelEntityTester: MainPanelEntityTester? = null

    /**
     * Provides the test-only accessor for [MainPanelEntity] that enables direct manipulation and
     * inspection of its internal state.
     */
    public val mainPanelEntityTester: MainPanelEntityTester
        get() {
            if (_mainPanelEntityTester != null) {
                return _mainPanelEntityTester!!
            }

            _mainPanelEntityTester = requireRuntimesReady {
                MainPanelEntityTester.create(requireNotNull(FakeSceneRuntime.instance))
            }

            return _mainPanelEntityTester!!
        }

    private var _spatialAudioTrackTester: SpatialAudioTrackTester? = null

    /**
     * Provides the test data accessor for the [SpatialAudioTrack].
     *
     * This class provides a mechanism for tests to inspect and verify spatial audio attributes
     * associated with an [AudioTrack] that are otherwise encapsulated within the SceneCore runtime.
     */
    public val spatialAudioTrackTester: SpatialAudioTrackTester
        get() {
            if (_spatialAudioTrackTester != null) {
                return _spatialAudioTrackTester!!
            }

            _spatialAudioTrackTester = requireRuntimesReady {
                val rtInstance =
                    requireNotNull(FakeSceneRuntime.instance).audioTrackExtensionsWrapper
                SpatialAudioTrackTester(rtInstance)
            }

            return _spatialAudioTrackTester!!
        }

    private var _spatialAudioTrackBuilderTester: SpatialAudioTrackBuilderTester? = null

    /**
     * Provides the test data accessor for the [SpatialAudioTrackBuilder].
     *
     * This class provides a mechanism for tests to inspect and verify spatial audio attributes
     * associated with an [AudioTrack.Builder] that are otherwise encapsulated within the SceneCore
     * runtime.
     */
    public val spatialAudioTrackBuilderTester: SpatialAudioTrackBuilderTester
        get() {
            if (_spatialAudioTrackBuilderTester != null) {
                return _spatialAudioTrackBuilderTester!!
            }

            _spatialAudioTrackBuilderTester = requireRuntimesReady {
                val rtInstance =
                    requireNotNull(FakeSceneRuntime.instance).audioTrackExtensionsWrapper
                SpatialAudioTrackBuilderTester(rtInstance)
            }

            return _spatialAudioTrackBuilderTester!!
        }

    /** The test data accessor for the [SpatialEnvironment]. */
    public val spatialEnvironmentTester: SpatialEnvironmentTester
        get() = requireRuntimesReady { SpatialEnvironmentTester.instance }

    /** Provides the [SpatialWindowTester] test data accessor for the [SpatialWindow]. */
    public val spatialWindowTester: SpatialWindowTester
        get() = requireRuntimesReady { SpatialWindowTester.instance }

    private var _spatialSoundPoolTester: SpatialSoundPoolTester? = null

    /**
     * This provides a [SpatialSoundPoolTester] instance for accessing the SpatialSoundPool's
     * underlying fake test data.
     */
    public val spatialSoundPoolTester: SpatialSoundPoolTester
        get() {
            if (_spatialSoundPoolTester != null) {
                return _spatialSoundPoolTester!!
            }

            _spatialSoundPoolTester = requireRuntimesReady {
                SpatialSoundPoolTester(requireNotNull(FakeSceneRuntime.instance))
            }

            return _spatialSoundPoolTester!!
        }

    @Suppress("GenericException")
    @Throws(Throwable::class)
    override fun before() {
        _sceneTester = null
        _activitySpaceTester = null
        _perceptionSpaceTester = null
        _spatialAudioTrackTester = null
        _spatialAudioTrackBuilderTester = null
        _spatialSoundPoolTester = null
        _mainPanelEntityTester = null
    }

    override fun after() {
        _sceneTester = null
        _activitySpaceTester = null
        _perceptionSpaceTester = null
        _spatialAudioTrackTester = null
        _spatialAudioTrackBuilderTester = null
        _spatialSoundPoolTester = null
        _mainPanelEntityTester = null
    }
}
