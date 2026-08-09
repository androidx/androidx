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

@file:Suppress("DEPRECATION")

package androidx.xr.compose.spatial

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.PlaneType
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.animation.follow.AnchorTarget
import androidx.xr.compose.subspace.animation.follow.ArDeviceTarget
import androidx.xr.compose.subspace.animation.follow.FollowBehavior
import androidx.xr.compose.subspace.animation.follow.FollowTarget
import androidx.xr.compose.subspace.animation.follow.TrackedDimensions
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.requiredSize
import androidx.xr.compose.subspace.layout.requiredSizeIn
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceSemanticsNodeInteraction
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertDepthIsNotEqualTo
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertHeightIsNotEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertWidthIsNotEqualTo
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.metersToDp
import androidx.xr.compose.unit.roundMetersToPx
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeRenderingRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class FollowingSubspaceV2Test {

    private val testDispatcher = StandardTestDispatcher()

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Before
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun setUp() {
        FollowBehavior.dispatcherOverride = testDispatcher
        androidx.xr.arcore.testing.FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @After
    fun tearDown() {
        FollowBehavior.dispatcherOverride = Dispatchers.Default
    }

    private object DefaultTestRecommendedBoxSize {
        const val WIDTH_METERS: Float = 1.73f
        const val HEIGHT_METERS: Float = 1.61f
        const val DEPTH_METERS: Float = 0.5f
    }

    private fun configureSessionWithDeviceTrackingMode(
        mode: DeviceTrackingMode = DeviceTrackingMode.SPATIAL
    ): Session {
        val result = runBlocking { Session.create(composeTestRule.activity, testDispatcher) }
        val session = assertIs<SessionCreateSuccess>(result).session
        session.configure(Config.Builder(session.config).setDeviceTracking(mode).build())

        return session
    }

    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    private fun translateDevice(
        fakeRuntime: FakePerceptionRuntime,
        offset: Vector3,
        durationMs: Long? = null,
    ) {
        val arDevice = fakeRuntime.perceptionManager.arDevice
        arDevice.devicePose = arDevice.devicePose.translate(translation = offset)
        testDispatcher.scheduler.advanceUntilIdle()
        fakeRuntime.allowOneMoreCallToUpdate()
        advanceTimeBy(fakeRuntime, durationMs)
    }

    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    private fun rotateDevice(
        fakeRuntime: FakePerceptionRuntime,
        offset: Quaternion,
        durationMs: Long? = null,
    ) {
        val fakePerceptionManager = fakeRuntime.perceptionManager
        fakePerceptionManager.arDevice.devicePose =
            fakePerceptionManager.arDevice.devicePose.rotate(rotation = offset)
        advanceTimeBy(fakeRuntime, durationMs)
    }

    private fun advanceTimeBy(fakeRuntime: FakePerceptionRuntime, durationMs: Long?) {
        testDispatcher.scheduler.advanceUntilIdle()
        fakeRuntime.allowOneMoreCallToUpdate()

        if (durationMs != null) {
            val frames = (durationMs / 16L).toInt() + 1
            for (i in 0..frames) {
                composeTestRule.mainClock.advanceTimeByFrame()
                testDispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    private fun assertExistenceAndGetNodeWorldPose(testTag: String): Pose {
        val node = composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode()
        return assertNotNull(node.semanticsEntity).getPose(relativeTo = Space.ACTIVITY)
    }

    /**
     * Asserts that the Entity associated with the current Subspace layout node is a descendant of
     * the [expectedAncestor] Entity.
     */
    private fun SubspaceSemanticsNodeInteraction.assertEntityIsDescendantOf(
        expectedAncestor: Entity
    ): SubspaceSemanticsNodeInteraction {
        val entity =
            fetchSemanticsNode().semanticsEntity
                ?: throw AssertionError("Did not find an associated entity for $this.")

        var current: Entity? = entity
        while (current != null) {
            if (current == expectedAncestor) {
                return this // Found the ancestor
            }
            current = current.parent
        }
        throw AssertionError(
            "Entity $entity of $this is not a descendant of the expected ancestor $expectedAncestor."
        )
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenNoDeviceTracking_DoNotRender() {
        composeTestRule.session =
            configureSessionWithDeviceTrackingMode(DeviceTrackingMode.DISABLED)

        assertFailsWith<IllegalStateException> {
            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            assertNotNull(LocalSession.current),
                            behavior = FollowBehavior.Static,
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }
        }
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenArDeviceTightUsedTogether_DoNotRender() {
        composeTestRule.session = configureSessionWithDeviceTrackingMode()

        composeTestRule.setContent {
            Subspace(
                follow =
                    FollowTarget.ArDevice(
                        assertNotNull(composeTestRule.session),
                        behavior = FollowBehavior.Tight,
                    ),
                modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
            ) {}
        }

        composeTestRule.onSubspaceNodeWithTag("FollowingSubspaceV2").assertDoesNotExist()
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenLoads_respectsDefaultOffset() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                        )
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("HeadPanel")) {}
                }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val headPanelPose = assertExistenceAndGetNodeWorldPose("HeadPanel")
            assertThat(headPanelPose).isEqualTo(ArDeviceTarget.DEFAULT_OFFSET)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_withFillMaxSizeModifierAndFraction_shouldRespectRecommendedContentBox() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)

            var density: Density? = null

            composeTestRule.setContent {
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft()),
                    modifier = SubspaceModifier.fillMaxSize(0.5f),
                ) {
                    SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {}
                }
            }
            testDispatcher.scheduler.advanceUntilIdle()
            val pixelDensity = session.scene.virtualPixelDensity
            val fullWidthPx =
                DefaultTestRecommendedBoxSize.WIDTH_METERS.roundMetersToPx(pixelDensity)
            val fullHeightPx =
                DefaultTestRecommendedBoxSize.HEIGHT_METERS.roundMetersToPx(pixelDensity)
            val fullDepthPx =
                DefaultTestRecommendedBoxSize.DEPTH_METERS.roundMetersToPx(pixelDensity)

            val expectedWidthPx = (fullWidthPx * 0.5f).toInt()
            val expectedHeightPx = (fullHeightPx * 0.5f).toInt()
            val expectedDepthPx = (fullDepthPx * 0.5f).toInt()

            composeTestRule
                .onSubspaceNodeWithTag("box")
                .assertWidthIsEqualTo(with(composeTestRule.density) { expectedWidthPx.toDp() })
                .assertHeightIsEqualTo(with(composeTestRule.density) { expectedHeightPx.toDp() })
                .assertDepthIsEqualTo(with(composeTestRule.density) { expectedDepthPx.toDp() })
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_withRequiredSizeModifier_overridesDefaultContentBox() {
        val session = configureSessionWithDeviceTrackingMode()
        val requiredSize = 50000.dp

        composeTestRule.setContent {
            // The user provides a requiredSize.
            Subspace(
                follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft()),
                modifier = SubspaceModifier.requiredSize(requiredSize),
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(requiredSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(requiredSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(requiredSize)
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_withRequiredSizeInModifier_overridesDefaultContentBox() {
        val session = configureSessionWithDeviceTrackingMode()
        val requiredMaxSize = 50000.dp

        composeTestRule.setContent {
            // The user provides a requiredSizeIn.
            // Since fillMaxSize is 1f, it fills the maximum size.
            Subspace(
                follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft()),
                modifier =
                    SubspaceModifier.requiredSizeIn(
                        maxWidth = requiredMaxSize,
                        maxHeight = requiredMaxSize,
                        maxDepth = requiredMaxSize,
                    ),
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(requiredMaxSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(requiredMaxSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(requiredMaxSize)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenScaleChanges_subspaceScaleUpdates() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)

            val fakeSceneRuntime =
                session.runtimes
                    .filterIsInstance<androidx.xr.scenecore.testing.FakeSceneRuntime>()
                    .first()

            composeTestRule.setContent {
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft()),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            var spaceNode =
                composeTestRule.onSubspaceNodeWithTag("FollowingSubspaceV2").fetchSemanticsNode()
            val initialSpaceRoot = spaceNode.semanticsEntity?.parent?.parent
            var expectedScale = spaceNode.semanticsEntity?.getScale(Space.ACTIVITY) ?: 1f
            assertNotNull(expectedScale)
            assertThat(initialSpaceRoot?.getScale(Space.ACTIVITY)).isEqualTo(expectedScale)

            expectedScale += 1.0f

            composeTestRule.runOnIdle {
                fakeSceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
                    recommendedPose = Pose.Identity,
                    recommendedScale = Vector3(expectedScale, expectedScale, expectedScale),
                )
            }

            composeTestRule.waitForIdle()

            spaceNode =
                composeTestRule.onSubspaceNodeWithTag("FollowingSubspaceV2").fetchSemanticsNode()
            val updatedSpaceRoot = spaceNode.semanticsEntity?.parent?.parent
            val spaceScale = updatedSpaceRoot?.getScale(Space.ACTIVITY)
            assertThat(spaceScale).isEqualTo(expectedScale)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenFollowTargetChanges_switchesTarget() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            var followTarget by
                mutableStateOf(
                    FollowTarget.ArDevice(
                        session,
                        behavior = FollowBehavior.Soft(durationMs = 1000),
                    )
                )

            composeTestRule.setContent {
                Subspace(
                    follow = followTarget,
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val arDeviceTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, arDeviceTranslation)

            var subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceCurrentPose.translation).isEqualTo(arDeviceTranslation)

            // Switch to an anchor target
            val anchorTranslation = Vector3(20.0f, 30.0f, 40.0f)
            val anchorResult = Anchor.create(session, Pose(anchorTranslation))
            val success = assertIs<AnchorCreateSuccess>(anchorResult)
            val anchorSpace = AnchorSpace.create(session, anchor = success.anchor)
            testDispatcher.scheduler.advanceUntilIdle()

            followTarget = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)
            subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceCurrentPose.translation).isEqualTo(anchorTranslation)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenFollowBehaviorChanges_actsLikeNewBehavior() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val durationMs = 1000L
            var followBehavior by
                mutableStateOf(FollowBehavior.Soft(durationMs = durationMs.toInt()))

            composeTestRule.setContent {
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = followBehavior),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val unitVector = Vector3(x = 1F, y = 1F, z = 1F)
            translateDevice(fakeRuntime, unitVector, durationMs)
            translateDevice(fakeRuntime, unitVector, durationMs)
            // With Soft behavior, subspace should have moved 2 unit vectors.
            var subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector * 2F)

            followBehavior = FollowBehavior.Static
            composeTestRule.waitForIdle()
            translateDevice(fakeRuntime, unitVector, durationMs)
            translateDevice(fakeRuntime, unitVector, durationMs)
            testDispatcher.scheduler.advanceUntilIdle()

            // With Static behavior, it should not move any more.
            subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector * 2F)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenStaticBehavior_OnlyMovesOnce() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Static,
                            dimensions =
                                TrackedDimensions(
                                    isTranslationXTracked = true,
                                    isTranslationYTracked = true,
                                    isTranslationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val unitVector = Vector3(x = 1F, y = 1F, z = 1F)
            translateDevice(fakeRuntime, unitVector)
            translateDevice(fakeRuntime, unitVector)

            val subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            // Device was moved 1 unit vector twice but is still just offset 1 unit vector.
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenFirstPoseReceived_NoAnimation() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val animationTime = 2000
            val subAnimationTime = 500L

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = animationTime),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val unitVector = Vector3(x = 1F, y = 1F, z = 1F)
            translateDevice(
                fakeRuntime = fakeRuntime,
                offset = unitVector,
                durationMs = subAnimationTime,
            )

            // The first device pose should cause the subspace to instantly spawn at that location.
            // The animation durationMs parameter only affects subsequent movements.
            var subspaceTranslation =
                assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation
            assertThat(subspaceTranslation).isEqualTo(unitVector)

            // Demonstrate how the next pose movement is not completed if adequate time is not
            // given.
            translateDevice(
                fakeRuntime = fakeRuntime,
                offset = unitVector,
                durationMs = subAnimationTime,
            )

            subspaceTranslation =
                assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation
            assertThat(subspaceTranslation.x).isIn(Range.open(1f, 2f))
            assertThat(subspaceTranslation.y).isIn(Range.open(1f, 2f))
            assertThat(subspaceTranslation.z).isIn(Range.open(1f, 2f))
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenNoDimensionsTracked_DoesNotMove() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isTranslationXTracked = false,
                                    isTranslationYTracked = false,
                                    isTranslationZTracked = false,
                                    isRotationXTracked = false,
                                    isRotationYTracked = false,
                                    isRotationZTracked = false,
                                ),
                        )
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("HeadPanel")) {}
                }
            }

            val headPanelInitialPose = assertExistenceAndGetNodeWorldPose("HeadPanel")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            assertThat(assertExistenceAndGetNodeWorldPose("HeadPanel"))
                .isEqualTo(headPanelInitialPose)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenDeviceTranslatesAndRotates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val durationMs = 1000L

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = durationMs.toInt()),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation, durationMs)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs)

            val subspaceWorldPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceWorldPose)
                .isEqualTo(fakeRuntime.perceptionManager.arDevice.devicePose)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    // TODO: b/508337756 Modify Soft Follow tests to move twice
    fun followingSubspaceV2_whenDeviceTranslates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isTranslationXTracked = true,
                                    isTranslationYTracked = true,
                                    isTranslationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val subspaceInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspaceCurrentPose.translation)
                .isEqualTo(fakeRuntime.perceptionManager.arDevice.devicePose.translation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyXTranslationTracked_OnlyXTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isTranslationXTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val subspaceInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val expectedTranslation = Vector3(fakeArDevice.devicePose.translation.x, 0F, 0F)

            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyYTranslationTracked_OnlyYTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isTranslationYTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val subspaceInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val expectedTranslation = Vector3(0F, fakeArDevice.devicePose.translation.y, 0F)

            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyZTranslationTracked_OnlyZTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isTranslationZTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val subspaceInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val expectedTranslation = Vector3(0F, 0F, fakeArDevice.devicePose.translation.z)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenDeviceRotates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice
            val durationMs = 1000L

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = durationMs.toInt()),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = true,
                                    isRotationYTracked = true,
                                    isRotationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val headPanelInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation, durationMs)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs)

            assertThat(assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation)
                .isEqualTo(fakeArDevice.devicePose.rotation)
            assertThat(assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation)
                .isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyXRotationTracked_OnlyXRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isRotationXTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val headPanelInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation =
                assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation
            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = deviceRotation.x, yaw = 0F, roll = 0F)

            assertThat(currentRotation.x).isWithin(1f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(0f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(0f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyYRotationTracked_OnlyYRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isRotationYTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val headPanelInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation =
                assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation
            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = 0F, yaw = deviceRotation.y, roll = 0F)

            assertThat(currentRotation.x).isWithin(0f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(0f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenOnlyZRotationTracked_OnlyZRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = TrackedDimensions(isRotationZTracked = true),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            val headPanelInitialPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation =
                assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation
            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = 0F, yaw = 0F, roll = deviceRotation.z)

            assertThat(currentRotation.x).isWithin(0f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(0f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndXTracked_tracksPitch() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = true,
                                    isRotationYTracked = false,
                                    isRotationZTracked = false,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            // Even though pitching up while turned 90 deg rotates around world Z, user-centric
            // Euler angle tracking correctly registers pitch while ignoring yaw and roll.
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 0F, roll = 0F)

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndXNotTracked_ignoresPitch() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = false,
                                    isRotationYTracked = true,
                                    isRotationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation =
                Quaternion.fromEulerAngles(
                    pitch = 0F,
                    yaw = fakeArDevice.devicePose.rotation.eulerAngles.y,
                    roll = fakeArDevice.devicePose.rotation.eulerAngles.z,
                )

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndYTracked_tracksYaw() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = false,
                                    isRotationYTracked = true,
                                    isRotationZTracked = false,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation =
                Quaternion.fromEulerAngles(
                    pitch = 0F,
                    yaw = fakeArDevice.devicePose.rotation.eulerAngles.y,
                    roll = 0F,
                )

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndYNotTracked_ignoresYaw() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = true,
                                    isRotationYTracked = false,
                                    isRotationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation =
                Quaternion.fromEulerAngles(
                    pitch = fakeArDevice.devicePose.rotation.eulerAngles.x,
                    yaw = 0F,
                    roll = fakeArDevice.devicePose.rotation.eulerAngles.z,
                )

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndZTracked_tracksRoll() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = false,
                                    isRotationYTracked = false,
                                    isRotationZTracked = true,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation =
                Quaternion.fromEulerAngles(
                    pitch = 0F,
                    yaw = 0F,
                    roll = fakeArDevice.devicePose.rotation.eulerAngles.z,
                )

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUserTurnsAndZNotTracked_ignoresRoll() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions =
                                TrackedDimensions(
                                    isRotationXTracked = true,
                                    isRotationYTracked = true,
                                    isRotationZTracked = false,
                                ),
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")

            // User turns left 90 degrees (yaw = 90), looks up 45 degrees (pitch = 45), and rolls
            // head 30 degrees (roll = 30).
            val offsetRotation = Quaternion.fromEulerAngles(pitch = 45F, yaw = 90F, roll = 30F)
            rotateDevice(fakeRuntime, offsetRotation, durationMs = 1000L)

            val currentRotation = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").rotation
            val expectedRotation =
                Quaternion.fromEulerAngles(
                    pitch = fakeArDevice.devicePose.rotation.eulerAngles.x,
                    yaw = fakeArDevice.devicePose.rotation.eulerAngles.y,
                    roll = 0F,
                )

            assertThat(currentRotation.x).isWithin(1e-5f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1e-5f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1e-5f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1e-5f).of(expectedRotation.w)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    fun followingSubspaceV2_whenTrackedDimensionsChange_MatchedDimensionsChange() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice
            var trackedDimensions by mutableStateOf(TrackedDimensions(isTranslationXTracked = true))

            composeTestRule.session = session
            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                            dimensions = trackedDimensions,
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            translateDevice(fakeRuntime, Vector3(x = 1F, y = 2F, z = 3F))

            var subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            var expectedTranslation = Vector3(fakeArDevice.devicePose.translation.x, 0F, 0F)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)

            // Switch x-only tracking to y-only tracking.
            trackedDimensions = TrackedDimensions(isTranslationYTracked = true)
            composeTestRule.waitForIdle()
            testDispatcher.scheduler.advanceUntilIdle()

            subspaceCurrentPose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            expectedTranslation += Vector3(0F, fakeArDevice.devicePose.translation.y, 0F)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenRemovedFromComposition_isDisposed() {
        composeTestRule.session = configureSessionWithDeviceTrackingMode()
        val session = assertNotNull(composeTestRule.session)
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft())
                ) {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {}
                }
            }
        }

        assertThat(composeTestRule.onSubspaceNodeWithTag("panel")).isNotNull()

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_withFillMaxSizeAndHigherDensity_respectsConstraints() {
        val higherDensity = 2f
        composeTestRule.session = configureSessionWithDeviceTrackingMode()
        val session = assertNotNull(composeTestRule.session)
        var density: Density? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(higherDensity)) {
                density = LocalDensity.current
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft())
                ) {
                    SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {}
                }
            }
        }

        assertNotNull(density)
        assertThat(density.density).isEqualTo(higherDensity)

        val expectedWidthPx =
            DefaultTestRecommendedBoxSize.WIDTH_METERS.roundMetersToPx(
                session.scene.virtualPixelDensity
            )
        val expectedHeightPx =
            DefaultTestRecommendedBoxSize.HEIGHT_METERS.roundMetersToPx(
                session.scene.virtualPixelDensity
            )
        val expectedDepthPx =
            DefaultTestRecommendedBoxSize.DEPTH_METERS.roundMetersToPx(
                session.scene.virtualPixelDensity
            )

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo((expectedWidthPx / higherDensity).dp)
            .assertHeightIsEqualTo((expectedHeightPx / higherDensity).dp)
            .assertDepthIsEqualTo((expectedDepthPx / higherDensity).dp)
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenUnbounded_withFillMaxSize_doesNotRespectConstraints() {
        composeTestRule.session = configureSessionWithDeviceTrackingMode()
        val session = assertNotNull(composeTestRule.session)

        composeTestRule.setContent {
            Subspace(
                follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft()),
                modifier =
                    SubspaceModifier.requiredSizeIn(
                        maxWidth = Dp.Infinity,
                        maxHeight = Dp.Infinity,
                        maxDepth = Dp.Infinity,
                    ),
            ) {
                SpatialBox(
                    SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(
                0.dp,
                0.dp,
                ArDeviceTarget.DEFAULT_OFFSET.translation.z.metersToDp(
                    composeTestRule.density,
                    session.scene.virtualPixelDensity,
                ),
            )
            .assertWidthIsNotEqualTo(
                with(composeTestRule.density) { VolumeConstraints().maxWidth.toDp() }
            )
            .assertHeightIsNotEqualTo(
                with(composeTestRule.density) { VolumeConstraints().maxHeight.toDp() }
            )
            .assertDepthIsNotEqualTo(
                with(composeTestRule.density) { VolumeConstraints().maxDepth.toDp() }
            )
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspaceV2_whenCreated_isParentedToAnchor() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        )
        val anchorSpace =
            AnchorSpace.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        composeTestRule.setContent {
            Subspace(follow = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertEntityIsDescendantOf(anchorSpace)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_withContent_positionsAtOrigin() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        )
        val anchorSpace =
            AnchorSpace.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        composeTestRule.setContent {
            Subspace(follow = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenNested_positionsRelativeToAnchor() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        )
        val anchorSpace =
            AnchorSpace.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        composeTestRule.setContent {
            Subspace(modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("subspacePanel")) {}
                Subspace(
                    follow = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)
                ) {
                    SpatialPanel(
                        SubspaceModifier.fillMaxSize().testTag("followingSubspaceV2Panel")
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("followingSubspaceV2Panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)

        composeTestRule
            .onSubspaceNodeWithTag("subspacePanel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(40.dp, 50.dp, 60.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenAnchoredToIdentity_positionsAtOrigin() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config.Builder().build())

        val anchorResult = Anchor.create(session, Pose.Identity)
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorSpace = AnchorSpace.create(session, anchor = success.anchor)

        composeTestRule.setContent {
            Subspace(follow = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenLocked_isPositionedCorrectly() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config.Builder().build())

        val anchorResult = Anchor.create(session, Pose(Vector3(20.0f, 30.0f, 40.0f)))
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorSpace = AnchorSpace.create(session, anchor = success.anchor)

        composeTestRule.setContent {
            Subspace(follow = FollowTarget.Anchor(anchorSpace, behavior = FollowBehavior.Tight)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
            }
        }

        val anchorWorldPose = anchorSpace.getPose(Space.ACTIVITY)
        val panelWorldPose = assertExistenceAndGetNodeWorldPose("Panel")
        assertThat(anchorWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))
        assertThat(panelWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))

        composeTestRule
            .onSubspaceNodeWithTag("Panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenAnchorChanges_repositions() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config.Builder().build())

        val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
        val anchorResult = Anchor.create(session, initialPose)
        val anchorSpace =
            AnchorSpace.create(session, anchor = assertIs<AnchorCreateSuccess>(anchorResult).anchor)

        val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
        val updatedAnchorResult = Anchor.create(session, updatedPose)
        val updatedAnchorSpace =
            AnchorSpace.create(
                session,
                anchor = assertIs<AnchorCreateSuccess>(updatedAnchorResult).anchor,
            )

        val currentAnchorState = mutableStateOf(anchorSpace)

        composeTestRule.setContent {
            Subspace(
                follow =
                    FollowTarget.Anchor(
                        assertNotNull(currentAnchorState.value),
                        behavior = FollowBehavior.Tight,
                    )
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
            }
        }

        assertThat(assertExistenceAndGetNodeWorldPose("Panel")).isEqualTo(initialPose)
        composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)

        currentAnchorState.value = updatedAnchorSpace
        composeTestRule.waitForIdle()

        assertThat(assertExistenceAndGetNodeWorldPose("Panel")).isEqualTo(updatedPose)

        composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenRecenterOccurs_reloadsSubspace() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val followingSubspaceTag = "FollowingSubspaceV2"

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.ArDevice(
                            session,
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                        ),
                    modifier = SubspaceModifier.testTag(followingSubspaceTag),
                ) {}
            }

            translateDevice(fakeRuntime, Vector3(x = 1F, y = 2F, z = 3F))

            // Verify the subspace has moved from the origin.
            assertThat(assertExistenceAndGetNodeWorldPose(followingSubspaceTag).translation)
                .isNotEqualTo(Pose.Identity.translation)

            // Trigger recenter
            val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
            val fakeActivitySpace = fakeSceneRuntime.activitySpace
            fakeActivitySpace.onOriginChanged()

            // After a recenter, the FollowingSubspaceV2 should consider its new position as origin.
            assertThat(assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2").translation)
                .isEqualTo(Pose.Identity.translation)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenTargetChanges_recenterUsesNewTarget() =
        runTest(context = testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTrackingMode()
            val session = assertNotNull(actual = composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()

            val anchorPose =
                Pose(
                    translation = Vector3(x = 10f, y = 20f, z = 30f),
                    rotation = Quaternion.Identity,
                )
            val anchor = (Anchor.create(session, anchorPose) as AnchorCreateSuccess).anchor
            val anchorTarget =
                FollowTarget.Anchor(
                    anchorSpace = assertNotNull(actual = AnchorSpace.create(session, anchor)),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                )

            // Start with the Anchor as the follow target
            var targetState by mutableStateOf(value = anchorTarget)

            composeTestRule.setContent {
                Subspace(
                    follow = targetState,
                    modifier = SubspaceModifier.testTag("FollowingSubspaceV2"),
                ) {}
            }

            translateDevice(fakeRuntime, offset = Vector3(x = 1f, y = 2f, z = 3f))

            // Swap the target to ArDevice.
            targetState =
                FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft(durationMs = 1000))
            composeTestRule.waitForIdle()
            testDispatcher.scheduler.advanceUntilIdle()

            var subspacePose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspacePose.translation).isEqualTo(Vector3(x = 1f, y = 2f, z = 3f))

            // Trigger recenter (Origin Change)
            val fakeActivitySpace = fakeSceneRuntime.activitySpace
            fakeActivitySpace.onOriginChanged()

            subspacePose = assertExistenceAndGetNodeWorldPose("FollowingSubspaceV2")
            assertThat(subspacePose.translation).isEqualTo(Pose.Identity.translation)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenDisposed_removesOriginChangedListener() {
        composeTestRule.session = configureSessionWithDeviceTrackingMode()
        val session = assertNotNull(composeTestRule.session)
        val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                Subspace(
                    follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft())
                ) {}
            }
        }

        composeTestRule.waitForIdle()
        assertThat(fakeSceneRuntime.activitySpace.onOriginChangedListener).isNotNull()

        showSubspace = false
        composeTestRule.waitForIdle()

        assertThat(fakeSceneRuntime.activitySpace.onOriginChangedListener).isNull()
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenDisposedAfterSessionDestroyed_doesNotCrash() {
        val originalSceneRuntime =
            FakeSceneRuntimeFactory().create(composeTestRule.activity).apply {
                deviceDpPerMeter = 2000f
            }
        val renderingRuntime = FakeRenderingRuntime(originalSceneRuntime)
        val perceptionRuntime =
            FakePerceptionRuntimeFactory().createRuntime(composeTestRule.activity).apply {
                initialize()
            }
        val customOwner =
            object : LifecycleOwner {
                override val lifecycle =
                    LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
            }

        val session =
            Session(
                context = composeTestRule.activity,
                runtimes = listOf(originalSceneRuntime, renderingRuntime, perceptionRuntime),
                coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate),
                lifecycleOwner = customOwner,
            )
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL))
        composeTestRule.session = session

        // Retrieve the internal observer and register it on customOwner.lifecycle.
        val observerField =
            Session::class.java.getDeclaredField("lifecycleObserver").apply { isAccessible = true }
        val lifecycleObserver = observerField.get(session) as LifecycleEventObserver
        customOwner.lifecycle.addObserver(lifecycleObserver)

        composeTestRule.setContent {
            Subspace(follow = FollowTarget.ArDevice(session, behavior = FollowBehavior.Soft())) {}
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            (customOwner.lifecycle).handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        composeTestRule.waitForIdle()

        assertThat(session.scene.activitySpace.isDisposed).isTrue()
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspaceV2_whenAnchorSpaceDisposed_doesNotCrash() =
        runTest(testDispatcher) {
            composeTestRule.session = composeTestRule.configureFakeSession()
            val session = assertNotNull(composeTestRule.session)
            session.configure(Config.Builder().build())

            val anchorResult = Anchor.create(session, Pose.Identity)
            val success = assertIs<AnchorCreateSuccess>(anchorResult)
            val anchorSpace = AnchorSpace.create(session, anchor = success.anchor)
            val anchorTarget = AnchorTarget(anchorSpace)

            val job = launch { anchorTarget.poseUpdates.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()

            // Close the session/scene, which disposes of all entities including anchorSpace
            session.scene.close()

            assertThat(anchorSpace.isDisposed).isTrue()

            // Canceling the collection coroutine triggers the awaitClose block in poseUpdates.
            // This verifies that unregistering the listener on a disposed AnchorSpace does not
            // crash.
            job.cancelAndJoin()
        }
}

@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class FollowingSubspaceV2TestWithArCoreTestRule {

    private val testDispatcher = StandardTestDispatcher()

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @get:Rule val arCoreTestRule = ArCoreTestRule()

    @Before
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun setUp() {
        FollowBehavior.dispatcherOverride = testDispatcher
        androidx.xr.arcore.testing.FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @After
    fun tearDown() {
        FollowBehavior.dispatcherOverride = Dispatchers.Default
    }

    private fun assertExistenceAndGetNodeWorldPose(testTag: String): Pose {
        val node = composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode()
        return assertNotNull(node.semanticsEntity).getPose(relativeTo = Space.ACTIVITY)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    @Suppress("DEPRECATION")
    fun followingSubspaceV2_whenAnchorPoseChanges_repositions() {
        runTest(testDispatcher) {
            val activity = composeTestRule.activity
            shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)
            val session =
                (Session.create(composeTestRule.activity, testDispatcher) as SessionCreateSuccess)
                    .session
            session.configure(
                Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
            )
            composeTestRule.session = session
            val initialPose =
                Pose(Vector3(10f, 20f, 30f), Quaternion.fromEulerAngles(10f, 20f, 30f))
            val testPlane =
                TestPlane(
                    androidx.xr.arcore.PlaneType.HORIZONTAL_UPWARD_FACING,
                    androidx.xr.arcore.PlaneLabel.FLOOR,
                )
            testPlane.centerPose = initialPose
            arCoreTestRule.addTrackables(testPlane)
            val anchorSpace =
                AnchorSpace.create(
                    session,
                    minimumPlaneExtents = FloatSize2d(),
                    planeOrientations = PlaneOrientation.ALL,
                    planeSemanticTypes = PlaneSemanticType.ALL,
                )
            advanceUntilIdle()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.Anchor(
                            assertNotNull(anchorSpace),
                            behavior = FollowBehavior.Tight,
                        )
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            assertPose(anchorSpace.getPose(Space.ACTIVITY), initialPose)
            assertPose(assertExistenceAndGetNodeWorldPose("Panel"), initialPose)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)

            val updatedPose =
                Pose(Vector3(40f, 50f, 60f), Quaternion.fromEulerAngles(40f, 50f, 60f))
            testPlane.centerPose = updatedPose
            advanceUntilIdle()

            assertPose(anchorSpace.getPose(Space.ACTIVITY), updatedPose)
            assertPose(assertExistenceAndGetNodeWorldPose("Panel"), updatedPose)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
        }
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    @Suppress("DEPRECATION")
    fun followingSubspaceV2_whenAnchorUsesSoftFollow_repositions() {
        runTest(testDispatcher) {
            val activity = composeTestRule.activity
            shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)
            val session =
                (Session.create(composeTestRule.activity, testDispatcher) as SessionCreateSuccess)
                    .session
            session.configure(
                Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
            )
            composeTestRule.session = session
            val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
            val testPlane = TestPlane(PlaneType.HORIZONTAL_UPWARD_FACING, PlaneLabel.FLOOR)
            testPlane.centerPose = initialPose
            arCoreTestRule.addTrackables(testPlane)
            val anchorSpace =
                AnchorSpace.create(
                    session,
                    minimumPlaneExtents = FloatSize2d(),
                    planeOrientations = PlaneOrientation.ALL,
                    planeSemanticTypes = PlaneSemanticType.ALL,
                )
            advanceUntilIdle()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.Anchor(
                            assertNotNull(anchorSpace),
                            behavior = FollowBehavior.Soft(durationMs = 1000),
                        )
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            // Update anchor's pose and verify the Panel is at the new location.
            val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
            testPlane.centerPose = updatedPose
            advanceUntilIdle()

            assertPose(anchorSpace.getPose(Space.ACTIVITY), updatedPose)
            assertPose(assertExistenceAndGetNodeWorldPose("Panel"), updatedPose)
        }
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    @Suppress("DEPRECATION")
    fun followingSubspaceV2_whenAnchorUsesStaticFollow_movesOnlyOnce() {
        runTest(testDispatcher) {
            val activity = composeTestRule.activity
            shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)
            val session =
                (Session.create(composeTestRule.activity, testDispatcher) as SessionCreateSuccess)
                    .session
            session.configure(
                Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
            )
            composeTestRule.session = session
            val initialPose =
                Pose(Vector3(10f, 20f, 30f), Quaternion.fromEulerAngles(10f, 20f, 30f))
            val testPlane =
                TestPlane(
                    androidx.xr.arcore.PlaneType.HORIZONTAL_UPWARD_FACING,
                    androidx.xr.arcore.PlaneLabel.FLOOR,
                )
            testPlane.centerPose = initialPose
            arCoreTestRule.addTrackables(testPlane)
            val anchorSpace =
                AnchorSpace.create(
                    session,
                    minimumPlaneExtents = FloatSize2d(),
                    planeOrientations = PlaneOrientation.ALL,
                    planeSemanticTypes = PlaneSemanticType.ALL,
                )
            advanceUntilIdle()

            composeTestRule.setContent {
                Subspace(
                    follow =
                        FollowTarget.Anchor(
                            assertNotNull(anchorSpace),
                            behavior = FollowBehavior.Static,
                        )
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            // Verify the panel is not at its destination immediately but after waiting, it is
            // there.
            assertThat(assertExistenceAndGetNodeWorldPose("Panel").translation)
                .isNotEqualTo(initialPose.translation)

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(assertExistenceAndGetNodeWorldPose("Panel").translation)
                .isEqualTo(initialPose.translation)

            // Verify the panel doesn't move if pose changes again.
            val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
            testPlane.centerPose = updatedPose

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(anchorSpace.getPose(Space.ACTIVITY)).isEqualTo(updatedPose)

            composeTestRule.waitForIdle()

            assertThat(assertExistenceAndGetNodeWorldPose("Panel").translation)
                .isNotEqualTo(updatedPose.translation)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
        }
    }
}
