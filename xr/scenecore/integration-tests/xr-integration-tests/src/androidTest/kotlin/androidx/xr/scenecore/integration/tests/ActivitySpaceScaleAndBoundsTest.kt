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

package androidx.xr.scenecore.integration.tests

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceptionSpace
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.captureSpatialScene
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device integration tests for [ActivitySpace] bounds, coordinate space scales (including CPM
 * verification), [ActivitySpace.recommendedContentBoxInFullSpace], and origin change listener
 * lifecycle across Home Space and Full Space transitions (b/581430543, b/583439191).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ActivitySpaceScaleAndBoundsTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun bounds_homeSpaceAndFullSpace_updatesBoundsAndListenerLifecycle() {
        sessionRule.requestHomeSpace()

        val activitySpace = sessionRule.session.scene.activitySpace
        val directEvents = CopyOnWriteArrayList<FloatSize3d>()
        val mainThreadEvents = CopyOnWriteArrayList<FloatSize3d>()
        val directListener = Consumer<FloatSize3d> { directEvents.add(it) }
        val defaultListener = Consumer<FloatSize3d> { mainThreadEvents.add(it) }

        activitySpace.addBoundsChangedListener(Executor { it.run() }, directListener)
        activitySpace.addBoundsChangedListener(defaultListener)

        waitUntil("ActivitySpace bounds are positive and finite in Home Space") {
            activitySpace.bounds.isPositiveFinite()
        }
        assertFinitePositiveHomeSpaceBounds(activitySpace.bounds)

        // Transition HSM -> FSM: bounds must expand to positive infinity and notify listeners.
        sessionRule.requestFullSpace()

        waitUntil("ActivitySpace bounds and listeners report positive infinity in Full Space") {
            activitySpace.bounds.isPositiveInfinity() &&
                directEvents.lastOrNull()?.isPositiveInfinity() == true &&
                mainThreadEvents.lastOrNull()?.isPositiveInfinity() == true
        }
        assertInfiniteFullSpaceBounds(activitySpace.bounds)

        // Transition FSM -> HSM: bounds must shrink back to finite Home Space window extents.
        sessionRule.requestHomeSpace()

        waitUntil("ActivitySpace bounds and listeners report finite extents in Home Space") {
            activitySpace.bounds.isPositiveFinite() &&
                directEvents.lastOrNull()?.isPositiveFinite() == true &&
                mainThreadEvents.lastOrNull()?.isPositiveFinite() == true
        }
        assertFinitePositiveHomeSpaceBounds(activitySpace.bounds)

        // Removing defaultListener stops further updates while directListener keeps receiving.
        activitySpace.removeBoundsChangedListener(defaultListener)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val mainCountAfterRemoval = mainThreadEvents.size
        val directCountBeforeSecondFsm = directEvents.size

        sessionRule.requestFullSpace()

        waitUntil("Active bounds listener observes Full Space after other listener removal") {
            activitySpace.bounds.isPositiveInfinity() &&
                directEvents.size > directCountBeforeSecondFsm &&
                directEvents.last().isPositiveInfinity()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(mainThreadEvents).hasSize(mainCountAfterRemoval)
    }

    @Test
    fun scaleAndTransform_acrossHomeSpaceAndFullSpace_preservesCoordinateMath() {
        sessionRule.requestHomeSpace()
        val settledHsmTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = false,
                timeout = SCENE_TIMEOUT,
            )

        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace
        val perceptionSpace = session.scene.perceptionSpace
        val parentLocalPose = Pose(Vector3(0.4f, -0.2f, -1.0f), Quaternion.Identity)
        val childLocalPose = Pose(Vector3(0.2f, 0.3f, -0.1f), Quaternion.Identity)

        val parentEntity = Entity.create(session, "scale-parent", parentLocalPose, activitySpace)
        parentEntity.setScale(1.5f, Space.PARENT)
        val childEntity = Entity.create(session, "scale-child", childLocalPose, parentEntity)
        childEntity.setScale(0.5f, Space.PARENT)

        val hsmInferredScale =
            assertSpaceScaleAndHierarchyMath(
                activitySpace,
                perceptionSpace,
                parentEntity,
                childEntity,
            )
        val hsmRealWorldScale = getActivitySpaceRealWorldScale(activitySpace)
        val hsmOriginInPerception = activitySpace.transformPoseTo(Pose.Identity, perceptionSpace)
        assertThat(hsmRealWorldScale).isWithin(FLOAT_TOLERANCE).of(hsmInferredScale)

        sessionRule.requestFullSpace()
        val settledFsmTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = true,
                timeout = SCENE_TIMEOUT,
            )

        val fsmInferredScale =
            assertSpaceScaleAndHierarchyMath(
                activitySpace,
                perceptionSpace,
                parentEntity,
                childEntity,
            )
        val fsmOriginInPerception = activitySpace.transformPoseTo(Pose.Identity, perceptionSpace)
        val fsmRealWorldScale = getActivitySpaceRealWorldScale(activitySpace)
        assertThat(fsmRealWorldScale).isWithin(FLOAT_TOLERANCE).of(1.0f)
        assertThat(fsmInferredScale).isWithin(FLOAT_TOLERANCE).of(1.0f)
        assertThat(Pose.distance(hsmOriginInPerception, fsmOriginInPerception))
            .isGreaterThan(ORIGIN_DELTA_MIN)
        val fsmSnapshotTask =
            checkNotNull(captureSpatialScene().findTaskByActivity(sessionRule.activity)) {
                "Expected Full Space task in captured CPM scene"
            }
        assertVector3(fsmSnapshotTask.scale, Vector3.One, CPM_TOLERANCE)
        assertVector3(settledFsmTask.scale, Vector3.One, CPM_TOLERANCE)

        sessionRule.requestHomeSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = false,
            timeout = SCENE_TIMEOUT,
        )
        val restoredHsmInferredScale =
            assertSpaceScaleAndHierarchyMath(
                activitySpace,
                perceptionSpace,
                parentEntity,
                childEntity,
            )
        val restoredHsmRealWorldScale = getActivitySpaceRealWorldScale(activitySpace)
        assertThat(restoredHsmRealWorldScale).isWithin(FLOAT_TOLERANCE).of(restoredHsmInferredScale)
        assertThat(settledHsmTask.isFullSpace).isFalse()
    }

    @Test
    fun recommendedContentBoxInFullSpace_hasPositiveExtents_andHostsPanelInsideBounds() {
        sessionRule.requestHomeSpace()
        val activitySpace = sessionRule.session.scene.activitySpace
        val initialBox = activitySpace.recommendedContentBoxInFullSpace
        assertValidRecommendedContentBox(initialBox)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        val fullSpaceBox = activitySpace.recommendedContentBoxInFullSpace
        assertValidRecommendedContentBox(fullSpaceBox)
        assertVector3(fullSpaceBox.min, initialBox.min, FLOAT_TOLERANCE)
        assertVector3(fullSpaceBox.max, initialBox.max, FLOAT_TOLERANCE)
        assertVector3(fullSpaceBox.center, initialBox.center, FLOAT_TOLERANCE)
        assertThat(fullSpaceBox.halfExtents.width)
            .isWithin(FLOAT_TOLERANCE)
            .of(initialBox.halfExtents.width)
        assertThat(fullSpaceBox.halfExtents.height)
            .isWithin(FLOAT_TOLERANCE)
            .of(initialBox.halfExtents.height)
        assertThat(fullSpaceBox.halfExtents.depth)
            .isWithin(FLOAT_TOLERANCE)
            .of(initialBox.halfExtents.depth)

        // Place a PanelEntity inside recommendedContentBoxInFullSpace using extents derived from
        // the box and verify its CPM leash sits inside the recommended bounds.
        val placedOffset =
            Vector3(
                fullSpaceBox.center.x,
                fullSpaceBox.center.y,
                fullSpaceBox.center.z.coerceIn(fullSpaceBox.min.z, fullSpaceBox.max.z),
            )
        val panelPixelSize =
            IntSize2d(
                width =
                    (min(0.8f, fullSpaceBox.halfExtents.width) * 500f).toInt().coerceAtLeast(240),
                height =
                    (min(0.6f, fullSpaceBox.halfExtents.height) * 500f).toInt().coerceAtLeast(180),
            )
        sessionRule.onMainThread {
            PanelEntity.create(
                session = sessionRule.session,
                view = TextView(activity).apply { text = "RecommendedContentBox" },
                pixelDimensions = panelPixelSize,
                name = CONTENT_BOX_PANEL_NAME,
                pose = Pose(placedOffset, Quaternion.Identity),
                parent = activitySpace,
            )
        }

        val snapshotTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                expectedEmbeddedPanelCount = 1,
                requireFullSpace = true,
                timeout = SCENE_TIMEOUT,
            )
        val panelNode =
            checkNotNull(
                snapshotTask.embeddedWindowLeashes.firstOrNull {
                    it.name == CONTENT_BOX_PANEL_NAME && it.isVisible
                }
            ) {
                "Expected visible $CONTENT_BOX_PANEL_NAME in embeddedWindowLeashes"
            }
        val activitySpaceNode =
            checkNotNull(snapshotTask.findNodeByName(ACTIVITY_SPACE_NODE_NAME)) {
                "Expected $ACTIVITY_SPACE_NODE_NAME in Full Space CPM scene"
            }
        val relativePosition = panelNode.position - activitySpaceNode.position
        assertThat(relativePosition.x)
            .isIn(
                com.google.common.collect.Range.closed(
                    fullSpaceBox.min.x - CPM_TOLERANCE,
                    fullSpaceBox.max.x + CPM_TOLERANCE,
                )
            )
        assertThat(relativePosition.y)
            .isIn(
                com.google.common.collect.Range.closed(
                    fullSpaceBox.min.y - CPM_TOLERANCE,
                    fullSpaceBox.max.y + CPM_TOLERANCE,
                )
            )
        assertThat(relativePosition.z)
            .isIn(
                com.google.common.collect.Range.closed(
                    fullSpaceBox.min.z - CPM_TOLERANCE,
                    fullSpaceBox.max.z + CPM_TOLERANCE,
                )
            )
        val extents = checkNotNull(panelNode.worldExtents) { "Expected CPM worldExtents on panel" }
        assertThat(extents.x).isGreaterThan(0f)
        assertThat(extents.y).isGreaterThan(0f)
        assertThat(extents.x).isAtMost(fullSpaceBox.halfExtents.width * 2f + CPM_TOLERANCE)
        assertThat(extents.y).isAtMost(fullSpaceBox.halfExtents.height * 2f + CPM_TOLERANCE)
    }

    @Test
    fun originChangedListener_invokedOnSpaceTransitions_andStopsAfterRemoval() {
        sessionRule.requestHomeSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = false,
            timeout = SCENE_TIMEOUT,
        )

        val scene = sessionRule.session.scene
        val activitySpace = scene.activitySpace
        val perceptionSpace = scene.perceptionSpace
        val hsmOriginInPerception = activitySpace.transformPoseTo(Pose.Identity, perceptionSpace)
        assertFinitePose(hsmOriginInPerception)
        assertPose(
            perceptionSpace.transformPoseTo(hsmOriginInPerception, activitySpace),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        val directLatch = CountDownLatch(1)
        val defaultLatch = CountDownLatch(1)
        val directCount = AtomicInteger(0)
        val defaultCount = AtomicInteger(0)
        val directListener = Runnable {
            directCount.incrementAndGet()
            directLatch.countDown()
        }
        val defaultListener = Runnable {
            defaultCount.incrementAndGet()
            defaultLatch.countDown()
        }

        activitySpace.addOriginChangedListener(Executor { it.run() }, directListener)
        activitySpace.addOriginChangedListener(defaultListener)

        // Transition HSM -> FSM: both listeners must receive origin-change notifications.
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        assertWithMessage(
                "Timed out waiting for direct executor origin change callback on HSM -> FSM"
            )
            .that(directLatch.await(ORIGIN_CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("Timed out waiting for default origin change callback on HSM -> FSM")
            .that(defaultLatch.await(ORIGIN_CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .isTrue()

        val fsmOriginInPerception = activitySpace.transformPoseTo(Pose.Identity, perceptionSpace)
        assertFinitePose(fsmOriginInPerception)
        assertPose(
            perceptionSpace.transformPoseTo(fsmOriginInPerception, activitySpace),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        assertThat(Pose.distance(hsmOriginInPerception, fsmOriginInPerception))
            .isGreaterThan(ORIGIN_DELTA_MIN)

        // Remove defaultListener while keeping directListener active across FSM -> HSM -> FSM.
        activitySpace.removeOriginChangedListener(defaultListener)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        sessionRule.requestHomeSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = false,
            timeout = SCENE_TIMEOUT,
        )
        val defaultCountBeforeSecondFsm = defaultCount.get()
        val directCountBeforeSecondFsm = directCount.get()

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )
        waitUntil(
            "Active origin listener observes second HSM -> FSM after defaultListener removal"
        ) {
            directCount.get() > directCountBeforeSecondFsm
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(defaultCount.get()).isEqualTo(defaultCountBeforeSecondFsm)

        // Remove the remaining listener and verify no callbacks arrive on FSM -> HSM -> FSM.
        activitySpace.removeOriginChangedListener(directListener)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val directCountAfterRemoval = directCount.get()
        sessionRule.requestHomeSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = false,
            timeout = SCENE_TIMEOUT,
        )
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(directCount.get()).isEqualTo(directCountAfterRemoval)
    }

    @Suppress("DEPRECATION", "RestrictedApiAndroidX")
    private fun getActivitySpaceRealWorldScale(activitySpace: ActivitySpace): Float =
        activitySpace.getScale(Space.REAL_WORLD)

    private fun assertSpaceScaleAndHierarchyMath(
        activitySpace: ActivitySpace,
        perceptionSpace: PerceptionSpace,
        parentEntity: Entity,
        childEntity: Entity,
    ): Float {
        val activityPoseInPerception = activitySpace.transformPoseTo(Pose.Identity, perceptionSpace)
        assertFinitePose(activityPoseInPerception)
        assertPose(
            perceptionSpace.transformPoseTo(activityPoseInPerception, activitySpace),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        assertThat(parentEntity.getScale(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(1.5f)
        assertThat(childEntity.getScale(Space.PARENT)).isWithin(FLOAT_TOLERANCE).of(0.5f)
        assertThat(childEntity.getScale(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.75f)

        val parentPoseInPerception = parentEntity.transformPoseTo(Pose.Identity, perceptionSpace)
        assertFinitePose(parentPoseInPerception)
        assertPose(
            perceptionSpace.transformPoseTo(parentPoseInPerception, parentEntity),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        val childPoseInPerception = childEntity.transformPoseTo(Pose.Identity, perceptionSpace)
        assertFinitePose(childPoseInPerception)
        assertPose(
            perceptionSpace.transformPoseTo(childPoseInPerception, childEntity),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        val parentInActivity = parentEntity.getPose(Space.ACTIVITY)
        assertPose(
            activitySpace.transformPoseTo(parentInActivity, perceptionSpace),
            parentPoseInPerception,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        // Parent (0.4, -0.2, -1.0) scaled 1.5x + child offset (0.2, 0.3, -0.1) => (0.7, 0.25,
        // -1.15).
        val expectedChildInActivity = Pose(Vector3(0.7f, 0.25f, -1.15f), Quaternion.Identity)
        val actualChildInActivity = childEntity.getPose(Space.ACTIVITY)
        assertPose(
            actualChildInActivity,
            expectedChildInActivity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        assertPose(
            activitySpace.transformPoseTo(actualChildInActivity, perceptionSpace),
            childPoseInPerception,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        val parentDeltaInActivity = parentInActivity.translation
        val childStepInActivity = actualChildInActivity.translation - parentDeltaInActivity
        val unrotatedParentDelta =
            activityPoseInPerception.rotation.inverse *
                (parentPoseInPerception.translation - activityPoseInPerception.translation)
        val unrotatedChildStep =
            activityPoseInPerception.rotation.inverse *
                (childPoseInPerception.translation - parentPoseInPerception.translation)
        val inferredScale =
            Vector3.distance(unrotatedParentDelta, Vector3.Zero) /
                Vector3.distance(parentDeltaInActivity, Vector3.Zero)
        assertThat(inferredScale.isFinite()).isTrue()
        assertThat(inferredScale).isGreaterThan(0f)
        assertVector3(unrotatedParentDelta, parentDeltaInActivity * inferredScale, FLOAT_TOLERANCE)
        assertVector3(unrotatedChildStep, childStepInActivity * inferredScale, FLOAT_TOLERANCE)

        assertPose(
            childEntity.transformPoseTo(Pose.Identity, parentEntity),
            childEntity.getPose(Space.PARENT),
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        assertPose(
            childEntity.transformPoseTo(Pose.Identity, activitySpace),
            actualChildInActivity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        assertPose(
            activitySpace.transformPoseTo(actualChildInActivity, childEntity),
            Pose.Identity,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )

        // Local offset (0.2, 0, 0) in 0.75x child space adds (0.15, 0, 0) in ActivitySpace.
        val localOffset = Pose(Vector3(0.2f, 0f, 0f), Quaternion.Identity)
        val offsetInActivity = childEntity.transformPoseTo(localOffset, activitySpace)
        assertPose(
            offsetInActivity,
            Pose(Vector3(0.85f, 0.25f, -1.15f), Quaternion.Identity),
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        assertPose(
            activitySpace.transformPoseTo(offsetInActivity, childEntity),
            localOffset,
            translationEpsilon = FLOAT_TOLERANCE,
            rotationEpsilon = FLOAT_TOLERANCE,
        )
        return inferredScale
    }

    private fun assertFinitePositiveHomeSpaceBounds(bounds: FloatSize3d) {
        assertThat(bounds.width.isFinite()).isTrue()
        assertThat(bounds.height.isFinite()).isTrue()
        assertThat(bounds.depth.isFinite()).isTrue()
        assertThat(bounds.width).isGreaterThan(0f)
        assertThat(bounds.height).isGreaterThan(0f)
        assertThat(bounds.depth).isAtLeast(0f)
    }

    private fun assertInfiniteFullSpaceBounds(bounds: FloatSize3d) {
        assertThat(bounds.width).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.height).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.depth).isEqualTo(Float.POSITIVE_INFINITY)
    }

    private fun assertValidRecommendedContentBox(box: BoundingBox) {
        assertThat(box.min.x.isFinite()).isTrue()
        assertThat(box.min.y.isFinite()).isTrue()
        assertThat(box.min.z.isFinite()).isTrue()
        assertThat(box.max.x.isFinite()).isTrue()
        assertThat(box.max.y.isFinite()).isTrue()
        assertThat(box.max.z.isFinite()).isTrue()
        assertThat(box.min.x).isLessThan(box.max.x)
        assertThat(box.min.y).isLessThan(box.max.y)
        assertThat(box.min.z).isLessThan(box.max.z)
        assertThat(box.halfExtents.width).isGreaterThan(0f)
        assertThat(box.halfExtents.height).isGreaterThan(0f)
        assertThat(box.halfExtents.depth).isGreaterThan(0f)
        assertVector3(box.center, (box.min + box.max) * 0.5f, FLOAT_TOLERANCE)
        val expectedHalfExtents = (box.max - box.min) * 0.5f
        assertThat(box.halfExtents.width).isWithin(FLOAT_TOLERANCE).of(expectedHalfExtents.x)
        assertThat(box.halfExtents.height).isWithin(FLOAT_TOLERANCE).of(expectedHalfExtents.y)
        assertThat(box.halfExtents.depth).isWithin(FLOAT_TOLERANCE).of(expectedHalfExtents.z)
    }

    private fun assertFinitePose(pose: Pose) {
        assertThat(pose.translation.x.isFinite()).isTrue()
        assertThat(pose.translation.y.isFinite()).isTrue()
        assertThat(pose.translation.z.isFinite()).isTrue()
        val rot = pose.rotation
        assertThat(rot.x.isFinite()).isTrue()
        assertThat(rot.y.isFinite()).isTrue()
        assertThat(rot.z.isFinite()).isTrue()
        assertThat(rot.w.isFinite()).isTrue()
        val normSquared = rot.x * rot.x + rot.y * rot.y + rot.z * rot.z + rot.w * rot.w
        assertThat(normSquared).isWithin(1e-3f).of(1.0f)
    }

    private fun FloatSize3d.isPositiveFinite(): Boolean =
        width.isFinite() &&
            height.isFinite() &&
            depth.isFinite() &&
            width > 0f &&
            height > 0f &&
            depth >= 0f

    private fun FloatSize3d.isPositiveInfinity(): Boolean =
        width == Float.POSITIVE_INFINITY &&
            height == Float.POSITIVE_INFINITY &&
            depth == Float.POSITIVE_INFINITY

    private companion object {
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val CONTENT_BOX_PANEL_NAME = "content-box-panel"
        const val ORIGIN_CALLBACK_TIMEOUT_SECONDS = 5L
        const val FLOAT_TOLERANCE = 1e-4f
        const val CPM_TOLERANCE = 5e-2f
        const val ORIGIN_DELTA_MIN = 1e-3f
        val SCENE_TIMEOUT = 5.seconds
    }
}
