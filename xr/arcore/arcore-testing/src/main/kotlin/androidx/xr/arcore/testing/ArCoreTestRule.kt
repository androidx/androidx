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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.internal.FakeRuntimeAnchor
import androidx.xr.arcore.testing.internal.FakeRuntimeConversationState
import androidx.xr.arcore.testing.internal.FakeRuntimeDepth
import androidx.xr.arcore.testing.internal.FakeRuntimeEye
import androidx.xr.arcore.testing.internal.FakeRuntimeFace
import androidx.xr.arcore.testing.internal.FakeRuntimeHand
import androidx.xr.arcore.testing.internal.FakeRuntimeRenderViewpoint
import androidx.xr.arcore.testing.internal.PendingTrackablesProvider
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.math.Pose
import java.util.UUID
import org.junit.rules.ExternalResource

/**
 * A JUnit `TestRule` that provides a way to simulate ARCore for Jetpack XR's perception of the real
 * world, enabling you to write reliable and clean tests for ARCore for Jetpack XR applications
 * without needing a physical device or emulator. You can control various aspects of the XR
 * environment, such as the presence of planes, augmented objects, images, QR codes, the user's face
 * or hands, etc.
 *
 * Writing effective ARCore for Jetpack XR tests involves the
 * [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
 * library, namely `TestScopes` and `TestDispatchers`. Unit tests should typically execute within a
 * [runTest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html)
 * block and pass a
 * [StandardTestDispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/-standard-test-dispatcher.html)
 * to the appropriate [androidx.xr.runtime.Session].
 *
 * Any changes to the simulated environment require coroutines to be executed to enable ARCore for
 * Jetpack XR to consume the new information. To achieve this, follow any set of changes to your
 * `ArCoreTestRule` trackables and tester objects with a call to
 * [advanceUntilIdle()](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/advance-until-idle.html).
 * This will execute the `TestDispatcher`'s queued coroutines until they complete or suspend, at
 * which point the ARCore for Jetpack XR runtime will have consumed the data based on the state of
 * the `ArCoreTestRule`. Further, any new coroutine that the test launches, whose results later test
 * steps depend on (e.g. `Plane.subscribe`), should also be followed by `advanceUntilIdle()`. This
 * will delay the test until the coroutine has completed or yields.
 *
 * Tests using `ArCoreTestRule` typically follow these phases:
 * 1. **Environment Setup**: Configure the XR environment by setting properties on the
 *    `ArCoreTestRule`. Follow any set of changes to the `ArCoreTestRule` values with a call to
 *    `advanceUntilIdle()`.
 * 2. **Action/Collection**: Perform actions that would trigger changes to ARCore for Jetpack XR's
 *    perception system, such as launching coroutines (e.g., subscribing to `Trackable` objects) or
 *    modifying the [androidx.xr.runtime.Session] configuration. For these actions, follow
 *    immediately with a call to `advanceUntilIdle()` to ensure the changes are processed. If the
 *    action is simply to read the state of static objects (like a hand or the device pose),
 *    `advanceUntilIdle()` is not required.
 * 3. **Assertion**: Check the state of ARCore for Jetpack XR objects (e.g., `Plane`,
 *    `AugmentedImage`, `ArDevice`) to verify that the data was consumed and read correctly.
 *
 * @sample androidx.xr.arcore.samples.arCoreTestSamples
 */
public class ArCoreTestRule : ExternalResource(), PendingTrackablesProvider {

    private val _persistedAnchorPoses: MutableMap<UUID, Pose> = mutableMapOf()
    private val _planes: MutableList<TestPlane> = mutableListOf()
    private val _objects: MutableList<TestAugmentedObject> = mutableListOf()
    private val _images: MutableList<TestAugmentedImage> = mutableListOf()
    private val _qrCodes: MutableList<TestQrCode> = mutableListOf()
    private val _faceMeshes: MutableList<TestFace> = mutableListOf()
    @OptIn(ExperimentalSpatialAnnotationsApi::class)
    private val _spatialAnnotations: MutableList<TestSpatialAnnotation> = mutableListOf()

    internal lateinit var runtime: FakePerceptionRuntime
        private set

    internal val pendingTrackables: MutableSet<TestTrackable> = mutableSetOf()

    /**
     * The maximum number of [androidx.xr.arcore.Anchor] objects that can be loaded at once in the
     * runtime. Defaults to 6.
     */
    public var anchorResourceLimit: Int = 6
        set(value) {
            field = value
            FakeRuntimeAnchor.anchorResourceLimit = value
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    /**
     * A list of all [TestPlane] objects in the environment. Tracking must be configured via
     * [androidx.xr.runtime.Session.configure] in order for an added plane to be ingested by the
     * runtime.
     */
    public val planes: List<TestPlane>
        get() = _planes.toList()

    /**
     * A list of all [TestAugmentedObject] objects in the environment. Tracking must be configured
     * via [androidx.xr.runtime.Session.configure] in order for an added object to be ingested by
     * the runtime.
     */
    public val augmentedObjects: List<TestAugmentedObject>
        get() = _objects.toList()

    /**
     * A list of all [TestAugmentedImage] objects in the environment. Tracking must be configured
     * via [androidx.xr.runtime.Session.configure] in order for an added object to be ingested by
     * the runtime.
     */
    public val augmentedImages: List<TestAugmentedImage>
        get() = _images.toList()

    /**
     * A list of all [TestQrCode] objects in the environment. Tracking must be configured via
     * [androidx.xr.runtime.Session.configure] in order for an added object to be ingested by the
     * runtime.
     */
    public val qrCodes: List<TestQrCode>
        get() = _qrCodes.toList()

    /**
     * A list of all [TestFace] objects in the environment, excluding the user's. Tracking must be
     * configured via [androidx.xr.runtime.Session.configure] in order for an added face to be
     * ingested by the runtime.
     */
    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val faces: List<TestFace>
        get() = _faceMeshes.toList()

    /**
     * A list of all [TestSpatialAnnotation] objects in the environment. Tracking must be configured
     * via [androidx.xr.runtime.Session.configure] in order for an added object to be ingested by
     * the runtime.
     */
    @ExperimentalSpatialAnnotationsApi
    public fun getSpatialAnnotations(): List<TestSpatialAnnotation> = _spatialAnnotations.toList()

    /** A Map of [UUID] to `Anchor` [Poses][Pose] stored outside the session. */
    public val persistedAnchorPoses: Map<UUID, Pose>
        get() = _persistedAnchorPoses.toMap()

    /**
     * The object representing the user's device in the environment.
     * [androidx.xr.runtime.DeviceTrackingMode.LAST_KNOWN] must be configured for it to be ingested
     * by the runtime.
     */
    public val deviceTester: ArDeviceTester = ArDeviceTester(this)

    /**
     * The object representing the user's face. [Config.faceTracking] must be set to
     * [androidx.xr.runtime.FaceTrackingMode.BLEND_SHAPES] for it to be integrated by the runtime.
     * [FaceTester.isValid] must be set to true for the face's blend shape and confidence values to
     * update in the API.
     */
    public val faceTester: FaceTester by lazy {
        FaceTester(this, runtime.perceptionManager.userFace as FakeRuntimeFace)
    }

    /**
     * The object representing the user's left hand in the environment.
     * [androidx.xr.runtime.HandTrackingMode.BOTH] must be configured for it to be ingested by the
     * runtime. [HandTester.isVisible] must be set to true for the hand's pose to update in the API.
     */
    public val leftHandTester: HandTester by lazy {
        HandTester(this, runtime.perceptionManager.leftHand as FakeRuntimeHand)
    }

    /**
     * The object representing the user's right hand in the environment.
     * [androidx.xr.runtime.HandTrackingMode.BOTH] must be configured for it to be ingested by the
     * runtime. [HandTester.isVisible] must be set to true for the hand's pose to update in the API.
     */
    public val rightHandTester: HandTester by lazy {
        HandTester(this, runtime.perceptionManager.rightHand as FakeRuntimeHand)
    }

    /**
     * The object representing the user's left eye in the environment.
     * [androidx.xr.runtime.EyeTrackingMode.COARSE_TRACKING] or
     * [androidx.xr.runtime.EyeTrackingMode.FINE_TRACKING] must be configured for it to be ingested
     * by the runtime. [EyeTester.isOpen] must be set to true for the eye's pose to update in the
     * API.
     */
    public val leftEyeTester: EyeTester by lazy {
        EyeTester(this, runtime.perceptionManager.leftEye as FakeRuntimeEye)
    }

    /**
     * The object representing the user's right eye in the environment.
     * [androidx.xr.runtime.EyeTrackingMode.COARSE_TRACKING] or
     * [androidx.xr.runtime.EyeTrackingMode.FINE_TRACKING] must be configured for it to be ingested
     * by the runtime. [EyeTester.isOpen] must be set to true for the eye's pose to update in the
     * API.
     */
    public val rightEyeTester: EyeTester by lazy {
        EyeTester(this, runtime.perceptionManager.rightEye as FakeRuntimeEye)
    }

    /** A test representation of the device's [androidx.xr.arcore.Geospatial] status. */
    public val geospatialTester: GeospatialTester = GeospatialTester(this)

    /** A test representation of the device's left [androidx.xr.arcore.RenderViewpoint]. */
    public val leftRenderViewpointTester: RenderViewpointTester by lazy {
        RenderViewpointTester(
            this,
            runtime.perceptionManager.leftRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's right [androidx.xr.arcore.RenderViewpoint]. */
    public val rightRenderViewpointTester: RenderViewpointTester by lazy {
        RenderViewpointTester(
            this,
            runtime.perceptionManager.rightRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's mono [androidx.xr.arcore.RenderViewpoint]. */
    public val monoRenderViewpointTester: RenderViewpointTester by lazy {
        RenderViewpointTester(
            this,
            runtime.perceptionManager.monoRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's left [androidx.xr.arcore.Depth] data. */
    public val leftDepthTester: DepthTester by lazy {
        DepthTester(this, runtime.perceptionManager.leftDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's right [androidx.xr.arcore.Depth] data. */
    public val rightDepthTester: DepthTester by lazy {
        DepthTester(this, runtime.perceptionManager.rightDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's mono [androidx.xr.arcore.Depth] data. */
    public val monoDepthTester: DepthTester by lazy {
        DepthTester(this, runtime.perceptionManager.monoDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's Conversation Scene Signal. */
    @get:android.annotation.SuppressLint("ExperimentalPropertyAnnotation")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val conversationSceneSignal: ConversationSceneSignalTester by lazy {
        ConversationSceneSignalTester(
            this,
            runtime.perceptionManager.conversationSceneSignal as FakeRuntimeConversationState,
        )
    }

    /**
     * Adds the given [TestTrackable] objects and registers them with this ArCoreTestRule.
     *
     * Objects that are added are not removed during the lifetime of the test. Instead, their
     * [TrackingState] will be updated based on their [TestTrackable.isVisible] property and the
     * [androidx.xr.runtime.Session] configuration.
     *
     * @param trackables [TestTrackable] objects to add
     */
    @OptIn(ExperimentalSpatialAnnotationsApi::class)
    public fun addTrackables(vararg trackables: TestTrackable) {
        trackables.forEach {
            if (it.isAddedToTestRule) return@forEach
            it.arCoreTestRule = this
            when (it) {
                is TestPlane -> {
                    _planes.add(it)
                }
                is TestAugmentedObject -> {
                    _objects.add(it)
                }
                is TestFace -> {
                    _faceMeshes.add(it)
                }
                is TestAugmentedImage -> {
                    _images.add(it)
                }
                is TestQrCode -> {
                    _qrCodes.add(it)
                }
                is TestSpatialAnnotation -> {
                    _spatialAnnotations.add(it)
                }
            }
            pendingTrackables.add(it)
        }
        FakePerceptionRuntime.allowOneMoreCallToUpdate()
    }

    /**
     * Supply the [Pose] of an [androidx.xr.arcore.Anchor] to be persisted outside the
     * [androidx.xr.runtime.Session]. Persisted Anchors can be loaded by their [UUID].
     *
     * @param pose the [Pose] at which the test will persist an Anchor
     * @return the [UUID] of the newly persisted Anchor
     */
    public fun persistAnchor(pose: Pose): UUID {
        val uuid = UUID.randomUUID()
        _persistedAnchorPoses[uuid] = pose
        if (runtime.config.anchorPersistence == AnchorPersistenceMode.LOCAL) {
            runtime.perceptionManager.persistedAnchorUUIDs[uuid] = pose
        }
        FakePerceptionRuntime.allowOneMoreCallToUpdate()
        return uuid
    }

    /** Clears the map of [UUID] instances to [Pose] instances. */
    public fun clearPersistedAnchors() {
        _persistedAnchorPoses.clear()
        runtime.perceptionManager.persistedAnchorUUIDs.clear()
        FakePerceptionRuntime.allowOneMoreCallToUpdate()
    }

    internal fun registerWithRuntime(fakePerceptionRuntime: FakePerceptionRuntime) {
        runtime = fakePerceptionRuntime
        runtime.addPendingTrackableProvider(this)
        for ((uuid, pose) in persistedAnchorPoses) {
            if (runtime.config.anchorPersistence == AnchorPersistenceMode.LOCAL) {
                runtime.perceptionManager.persistedAnchorUUIDs[uuid] = pose
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun before() {
        FakePerceptionRuntimeFactory.arCoreTestRule = this
        // TODO b/448689133: Remove this when no longer necessary
        androidx.xr.arcore.testing.FakePerceptionRuntimeFactory.createNewFakeRuntime = true
        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @Suppress("DEPRECATION")
    override fun after() {
        FakePerceptionRuntimeFactory.arCoreTestRule = null
        // TODO b/448689133: Remove this when no longer necessary
        androidx.xr.arcore.testing.FakePerceptionRuntimeFactory.createNewFakeRuntime = false
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getPendingTrackables(): Set<Trackable> {
        val configuredPendingTrackables: MutableSet<Trackable> = mutableSetOf()
        val pending = pendingTrackables.iterator()
        while (pending.hasNext()) {
            val testTrackable = pending.next()
            if (testTrackable.isTrackableConfigured()) {
                configuredPendingTrackables.add(testTrackable.fakeRuntimeTrackable)
                pending.remove()
            }
        }
        return configuredPendingTrackables
    }
}
