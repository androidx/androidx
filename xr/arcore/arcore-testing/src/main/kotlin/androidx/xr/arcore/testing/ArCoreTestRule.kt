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
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.internal.FakeRuntimeAnchor
import androidx.xr.arcore.testing.internal.FakeRuntimeConversationState
import androidx.xr.arcore.testing.internal.FakeRuntimeDepth
import androidx.xr.arcore.testing.internal.FakeRuntimeEye
import androidx.xr.arcore.testing.internal.FakeRuntimeHand
import androidx.xr.arcore.testing.internal.FakeRuntimeRenderViewpoint
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.ExperimentalSceneSignalApi
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.math.Pose
import java.util.UUID
import org.junit.rules.ExternalResource

/**
 * A JUnit Rule for creating a test environment for ARCore for Jetpack XR applications. This rule
 * allows you to write unit tests where you alter the state of the perception system.
 */
public class ArCoreTestRule : ExternalResource() {

    private val _persistedAnchorPoses: MutableMap<UUID, Pose> = mutableMapOf()
    private val _planes: MutableList<TestPlane> = mutableListOf()
    private val _objects: MutableList<TestAugmentedObject> = mutableListOf()
    private val _images: MutableList<TestAugmentedImage> = mutableListOf()
    private val _faceMeshes: MutableList<TestFace> = mutableListOf()

    internal lateinit var runtime: FakePerceptionRuntime
        private set

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
    public val augmentableObjects: List<TestAugmentedObject>
        get() = _objects.toList()

    /**
     * A list of all [TestAugmentedImage] objects in the environment. Tracking must be configured
     * via [androidx.xr.runtime.Session.configure] in order for an added object to be ingested by
     * the runtime.
     */
    public val augmentableImages: List<TestAugmentedImage>
        get() = _images.toList()

    /**
     * A list of all [TestFace] objects in the environment, excluding the user's. Tracking must be
     * configured via [androidx.xr.runtime.Session.configure] in order for an added face to be
     * ingested by the runtime.
     */
    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val faces: List<TestFace>
        get() = _faceMeshes.toList()

    /** A Map of [UUID] to `Anchor` [Poses][Pose] stored outside the session. */
    public val persistedAnchorPoses: Map<UUID, Pose>
        get() = _persistedAnchorPoses.toMap()

    /**
     * The object representing the user's device in the environment.
     * [androidx.xr.runtime.DeviceTrackingMode.LAST_KNOWN] must be configured for it to be ingested
     * by the runtime.
     */
    public val device: TestArDevice = TestArDevice(this)

    /**
     * The object representing the user's face in the environment. [Config.faceTracking] must be set
     * to [androidx.xr.runtime.FaceTrackingMode.MESHES] or
     * [androidx.xr.runtime.FaceTrackingMode.BLEND_SHAPES] for it to be integrated by the runtime.
     * [TestTrackable.isVisible] must be set to true for the face's pose to update in the API.
     */
    public val face: TestFace = TestFace(this)

    /**
     * The object representing the user's left hand in the environment.
     * [androidx.xr.runtime.HandTrackingMode.BOTH] must be configured for it to be ingested by the
     * runtime. [TestHand.isVisible] must be set to true for the hand's pose to update in the API.
     */
    public val leftHand: TestHand by lazy {
        TestHand(this, runtime.perceptionManager.leftHand as FakeRuntimeHand)
    }

    /**
     * The object representing the user's right hand in the environment.
     * [androidx.xr.runtime.HandTrackingMode.BOTH] must be configured for it to be ingested by the
     * runtime. [TestHand.isVisible] must be set to true for the hand's pose to update in the API.
     */
    public val rightHand: TestHand by lazy {
        TestHand(this, runtime.perceptionManager.rightHand as FakeRuntimeHand)
    }

    /**
     * The object representing the user's left eye in the environment.
     * [androidx.xr.runtime.EyeTrackingMode.COARSE_TRACKING] or
     * [androidx.xr.runtime.EyeTrackingMode.FINE_TRACKING] must be configured for it to be ingested
     * by the runtime. [TestEye.isOpen] must be set to true for the eye's pose to update in the API.
     */
    public val leftEye: TestEye by lazy {
        TestEye(this, runtime.perceptionManager.leftEye as FakeRuntimeEye)
    }

    /**
     * The object representing the user's right eye in the environment.
     * [androidx.xr.runtime.EyeTrackingMode.COARSE_TRACKING] or
     * [androidx.xr.runtime.EyeTrackingMode.FINE_TRACKING] must be configured for it to be ingested
     * by the runtime. [TestEye.isOpen] must be set to true for the eye's pose to update in the API.
     */
    public val rightEye: TestEye by lazy {
        TestEye(this, runtime.perceptionManager.rightEye as FakeRuntimeEye)
    }

    /** A test representation of the device's [androidx.xr.arcore.Geospatial] status. */
    public val geospatial: TestGeospatial = TestGeospatial(this)

    /** A test representation of the device's left [androidx.xr.arcore.RenderViewpoint]. */
    public val leftRenderViewpoint: TestRenderViewpoint by lazy {
        TestRenderViewpoint(
            this,
            runtime.perceptionManager.leftRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's right [androidx.xr.arcore.RenderViewpoint]. */
    public val rightRenderViewpoint: TestRenderViewpoint by lazy {
        TestRenderViewpoint(
            this,
            runtime.perceptionManager.rightRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's mono [androidx.xr.arcore.RenderViewpoint]. */
    public val monoRenderViewpoint: TestRenderViewpoint by lazy {
        TestRenderViewpoint(
            this,
            runtime.perceptionManager.monoRenderViewpoint as FakeRuntimeRenderViewpoint,
        )
    }

    /** A test representation of the device's left [androidx.xr.arcore.Depth] data. */
    public val leftDepth: TestDepth by lazy {
        TestDepth(this, runtime.perceptionManager.leftDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's right [androidx.xr.arcore.Depth] data. */
    public val rightDepth: TestDepth by lazy {
        TestDepth(this, runtime.perceptionManager.rightDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's mono [androidx.xr.arcore.Depth] data. */
    public val monoDepth: TestDepth by lazy {
        TestDepth(this, runtime.perceptionManager.monoDepth as FakeRuntimeDepth)
    }

    /** A test representation of the device's Conversation Scene Signal. */
    @ExperimentalSceneSignalApi
    @PreviewSpatialApi
    public val conversationSceneSignal: TestConversationSceneSignal by lazy {
        TestConversationSceneSignal(
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
    public fun addTrackables(vararg trackables: TestTrackable) {
        trackables.forEach {
            it.arCoreTestRule = this
            when (it) {
                is TestPlane -> {
                    _planes.add(it)
                    if (it.isConfigured()) {
                        runtime.perceptionManager.trackables.add(it.fakeRuntimeTrackable)
                    }
                }
                is TestAugmentedObject -> {
                    _objects.add(it)
                    if (it.isConfigured()) {
                        runtime.perceptionManager.trackables.add(it.fakeRuntimeTrackable)
                    }
                }
                is TestFace -> {
                    _faceMeshes.add(it)
                    if (it.isConfiguredForMeshing()) {
                        runtime.perceptionManager.trackables.add(it.fakeRuntimeTrackable)
                    }
                }
                is TestAugmentedImage -> {
                    _images.add(it)
                    if (it.isConfigured()) {
                        runtime.perceptionManager.trackables.add(it.fakeRuntimeTrackable)
                    }
                }
            }
            // TODO: b/497925970 ensure trackables added before they get configured are
            // retroactively added to the PerceptionManager
            updateFakeRuntimeTrackable(it)
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

    @Suppress("DEPRECATION")
    private fun updateFakeRuntimeTrackable(testTrackable: TestTrackable) {
        when (testTrackable) {
            is TestPlane -> {
                if (!testTrackable.isConfigured()) {
                    return
                }
                testTrackable.fakeRuntimeTrackable.apply {
                    centerPose = testTrackable.centerPose
                    type = testTrackable.type.toRuntimeType()
                    label = testTrackable.label.toRuntimeType()
                    extents = testTrackable.extents
                    vertices = testTrackable.vertices
                    subsumedBy = testTrackable.subsumedBy?.fakeRuntimeTrackable
                    trackingState =
                        if (testTrackable.isVisible) {
                            TrackingState.TRACKING
                        } else {
                            TrackingState.PAUSED
                        }
                }
            }
            is TestAugmentedObject -> {
                if (!testTrackable.isConfigured()) {
                    return
                }
                testTrackable.fakeRuntimeTrackable.apply {
                    centerPose = testTrackable.centerPose
                    extents = testTrackable.extents
                    category = testTrackable.category
                    trackingState =
                        if (testTrackable.isVisible) {
                            TrackingState.TRACKING
                        } else {
                            TrackingState.PAUSED
                        }
                }
            }
            is TestFace -> {
                if (!testTrackable.isConfiguredForMeshing()) {
                    return
                }
                testTrackable.fakeRuntimeTrackable.apply {
                    centerPose = testTrackable.centerPose
                    mesh = testTrackable.mesh
                    noseTipPose = testTrackable.noseTipPose
                    foreheadLeftPose = testTrackable.foreheadLeftPose
                    foreheadRightPose = testTrackable.foreheadRightPose
                    trackingState =
                        if (testTrackable.isVisible) {
                            TrackingState.TRACKING
                        } else {
                            TrackingState.PAUSED
                        }
                }
            }
            is TestAugmentedImage -> {
                if (!testTrackable.isConfigured()) {
                    return
                }
                testTrackable.fakeRuntimeTrackable.apply {
                    centerPose = testTrackable.centerPose
                    extents = testTrackable.extents
                    index = testTrackable.index
                    trackingState =
                        if (testTrackable.isVisible) {
                            TrackingState.TRACKING
                        } else {
                            TrackingState.PAUSED
                        }
                }
            }
        }
    }

    internal fun registerWithRuntime(fakePerceptionRuntime: FakePerceptionRuntime) {
        runtime = fakePerceptionRuntime
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
}
