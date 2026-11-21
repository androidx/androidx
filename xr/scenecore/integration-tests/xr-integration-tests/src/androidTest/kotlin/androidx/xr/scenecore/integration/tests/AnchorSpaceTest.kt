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
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorCreateTrackingUnavailable
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.captureSpatialScene
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device integration tests for [AnchorSpace], covering [PlaneTrackingMode.DISABLED] validation,
 * semantic anchor lifecycle and child parenting while unanchored, and ARCore [Anchor] binding with
 * child [PanelEntity] rendering in CPM (b/581430543).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class AnchorSpaceTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    @get:Rule(order = 2) val timeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @Test
    fun anchorSpace_semanticCreate_unobstructedLifecycleAndParenting() {
        val session = sessionRule.session
        val initialConfig = session.config
        val disabledConfigResult =
            session.configure(
                Config.Builder(initialConfig).setPlaneTracking(PlaneTrackingMode.DISABLED).build()
            )
        assertThat(disabledConfigResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThrows(IllegalStateException::class.java) {
            AnchorSpace.create(
                session = session,
                minimumPlaneExtents = FloatSize2d(0.2f, 0.2f),
                planeOrientations = PlaneOrientation.ALL,
                planeSemanticTypes = PlaneSemanticType.ALL,
            )
        }

        val planeTrackingConfigResult =
            session.configure(
                Config.Builder(initialConfig)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )
        assertThat(planeTrackingConfigResult).isInstanceOf(SessionConfigureSuccess::class.java)

        // Large plane extents keep the search UNANCHORED on an unobstructed desk rig without
        // depending on physical room geometry.
        val anchorSpace =
            AnchorSpace.create(
                session = session,
                minimumPlaneExtents = FloatSize2d(1000f, 1000f),
                planeOrientations = PlaneOrientation.ALL,
                planeSemanticTypes = PlaneSemanticType.ALL,
                timeout = Duration.ofSeconds(5),
            )
        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
        assertThat(anchorSpace.anchor).isNull()

        val observedStates = CopyOnWriteArrayList<AnchorSpace.State>()
        val executorListener = Consumer<AnchorSpace.State> { observedStates.add(it) }
        val defaultListener = Consumer<AnchorSpace.State> { observedStates.add(it) }
        anchorSpace.addStateChangedListener(Executor { it.run() }, executorListener)
        anchorSpace.addStateChangedListener(defaultListener)

        waitUntil("AnchorSpace state listener immediately emits UNANCHORED") {
            observedStates.contains(AnchorSpace.State.UNANCHORED)
        }

        anchorSpace.removeStateChangedListener(defaultListener)
        anchorSpace.removeStateChangedListener(executorListener)

        val unanchoredPanel = createChildPanel(name = UNANCHORED_PANEL_NAME)
        unanchoredPanel.parent = anchorSpace
        assertThat(anchorSpace.children).containsExactly(unanchoredPanel)

        // While AnchorSpace is UNANCHORED, its CPM node stays unattached from ActivitySpace so the
        // child panel is not surfaced as a visible embedded window leash.
        waitUntil("unanchored AnchorSpace child panel absent from visible CPM leashes") {
            val task = captureSpatialScene().findTaskByActivity(sessionRule.activity)
            task != null &&
                task.embeddedWindowLeashes.none {
                    it.name.contains(UNANCHORED_PANEL_NAME) && it.isVisible
                }
        }

        unanchoredPanel.parent = null
    }

    // TODO(b/584934775): Remove @Ignore once fixed.
    @Ignore("b/584934775: AnchorSpace semantic timeout hangs when plane set is static")
    @Test
    fun anchorSpace_semanticCreate_shortTimeout_transitionsToTimedOut() {
        val session = sessionRule.session
        val planeTrackingConfigResult =
            session.configure(
                Config.Builder(session.config)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )
        assertThat(planeTrackingConfigResult).isInstanceOf(SessionConfigureSuccess::class.java)

        val timedOutStates = CopyOnWriteArrayList<AnchorSpace.State>()
        val timedOutListener = Consumer<AnchorSpace.State> { timedOutStates.add(it) }

        val anchorSpace =
            AnchorSpace.create(
                    session = session,
                    minimumPlaneExtents = FloatSize2d(1000f, 1000f),
                    planeOrientations = PlaneOrientation.ALL,
                    planeSemanticTypes = PlaneSemanticType.ALL,
                    timeout = Duration.ofMillis(100),
                )
                .also { it.addStateChangedListener(Executor { r -> r.run() }, timedOutListener) }

        waitUntil("Semantic AnchorSpace transitions to TIMED_OUT", timeout = 10.seconds) {
            anchorSpace.state == AnchorSpace.State.TIMED_OUT &&
                timedOutStates.contains(AnchorSpace.State.TIMED_OUT)
        }
        assertThat(anchorSpace.anchor).isNull()
    }

    @Test
    fun anchorSpace_viaArAnchor_bindsAnchorManagesListenersAndParentPanel() {
        val session = sessionRule.session
        val configResult =
            session.configure(
                Config.Builder(session.config)
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )
        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)

        waitUntil("Perception state becomes available", timeout = 5.seconds) {
            session.state.value.perceptionState != null
        }

        val anchorRef = AtomicReference<Anchor?>(null)
        val anchorPose = Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity)
        waitUntil("Anchor.create succeeds with spatial tracking", timeout = 5.seconds) {
            when (val result = Anchor.create(session = session, pose = anchorPose)) {
                is AnchorCreateSuccess -> {
                    anchorRef.set(result.anchor)
                    true
                }
                is AnchorCreateTrackingUnavailable -> false
                is AnchorCreateResourcesExhausted ->
                    throw AssertionError("Anchor creation exhausted runtime resources")
                else -> throw AssertionError("Unexpected Anchor.create result: $result")
            }
        }

        val anchor = checkNotNull(anchorRef.get())
        val observedStates = CopyOnWriteArrayList<AnchorSpace.State>()
        val stateListener = Consumer<AnchorSpace.State> { observedStates.add(it) }
        val anchorSpace =
            AnchorSpace.create(session, anchor).also {
                assertThat(it.anchor).isEqualTo(anchor)
                assertThat(it.state).isEqualTo(AnchorSpace.State.ANCHORED)
                it.addStateChangedListener(Executor { r -> r.run() }, stateListener)
            }

        waitUntil("AnchorSpace state listener immediately emits ANCHORED") {
            observedStates.contains(AnchorSpace.State.ANCHORED)
        }
        anchorSpace.removeStateChangedListener(stateListener)

        val activityPose = anchorSpace.getPose(Space.ACTIVITY)
        assertThat(activityPose.translation.x.isFinite()).isTrue()
        assertThat(activityPose.translation.y.isFinite()).isTrue()
        assertThat(activityPose.translation.z.isFinite()).isTrue()
        val scale = anchorSpace.getScale(Space.ACTIVITY)
        assertThat(scale.isFinite()).isTrue()
        assertThat(scale).isGreaterThan(0f)

        val anchorPanel = createChildPanel(name = ANCHORED_PANEL_NAME)
        anchorPanel.parent = anchorSpace
        assertThat(anchorSpace.children).containsExactly(anchorPanel)

        waitUntil("anchored child panel visible in CPM") {
            val task = captureSpatialScene().findTaskByActivity(sessionRule.activity)
            task != null &&
                task.embeddedWindowLeashes.any {
                    it.name.contains(ANCHORED_PANEL_NAME) && it.isVisible && it.worldExtents != null
                }
        }
        assertPose(
            anchorPanel.getPose(Space.ACTIVITY),
            anchorSpace.getPose(Space.ACTIVITY),
            translationEpsilon = POSE_TOLERANCE,
            rotationEpsilon = POSE_TOLERANCE,
        )

        anchorPanel.parent = null
        anchor.detach()

        waitUntil("detached AnchorSpace child panel removed from visible CPM leashes") {
            val task = captureSpatialScene().findTaskByActivity(sessionRule.activity)
            task != null &&
                task.embeddedWindowLeashes.none {
                    it.name.contains(ANCHORED_PANEL_NAME) && it.isVisible
                }
        }
    }

    @Test
    fun anchorSpace_setChildRealWorldScale_preservesScaleAcrossOriginUpdates() {
        val session = sessionRule.session
        val configResult =
            session.configure(
                Config.Builder(session.config)
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )
        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)

        waitUntil("Perception state becomes available", timeout = 5.seconds) {
            session.state.value.perceptionState != null
        }

        val gltfModel = sessionRule.onMainThread {
            GltfModel.create(session, Paths.get("models", GIZMO_MODEL_ASSET))
        }

        val originListeners = mutableListOf<Pair<AnchorSpace, Runnable>>()
        val createdGizmos = mutableListOf<GltfModelEntity>()
        val createdAnchors = mutableListOf<Anchor>()

        try {
            // Induce non-unit task scale via keyEntity continuity across Full -> Home -> Full.
            session.scene.keyEntity = session.scene.mainPanelEntity
            session.scene.mainPanelEntity.setScale(CONTINUITY_TASK_SCALE)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = true,
                timeout = 5.seconds,
            )

            val transitionAnchor =
                createTrackingAnchor(session, Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity))
            createdAnchors.add(transitionAnchor)

            sessionRule.requestHomeSpace()
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = false,
                timeout = 5.seconds,
            )

            val observedAnchorWorldScales = CopyOnWriteArrayList<Float>()
            val transitionScaledOnce = AtomicBoolean(false)
            val transitionCallbackFired = AtomicBoolean(false)

            // Trigger non-blocking Full Space transition and immediately create AnchorSpace +
            // child GltfModelEntity during the transition window before origin updates settle.
            session.scene.requestFullSpace()
            val transitionSpace = AnchorSpace.create(session, transitionAnchor)
            val transitionGizmo = sessionRule.onMainThread {
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = Pose.Identity,
                    parent = transitionSpace,
                )
            }
            createdGizmos.add(transitionGizmo)
            setRealWorldScale(transitionGizmo, TARGET_GIZMO_SCALE)
            val transitionListener = Runnable {
                observedAnchorWorldScales.add(getRealWorldScale(transitionSpace))
                if (transitionScaledOnce.compareAndSet(false, true)) {
                    setRealWorldScale(transitionGizmo, TARGET_GIZMO_SCALE)
                }
                transitionCallbackFired.set(true)
            }
            transitionSpace.addOriginChangedListener(Executor { it.run() }, transitionListener)
            originListeners.add(transitionSpace to transitionListener)

            waitUntil("Full Space Mode active", timeout = 5.seconds) {
                session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)
            }
            val settledFsmTask =
                waitForSettledSpatialTask(
                    activity = sessionRule.activity,
                    requireFullSpace = true,
                    timeout = 5.seconds,
                )
            val spatialTransformScale =
                checkNotNull(settledFsmTask.spatialTransform) {
                        "Expected space-transform-spatial node in settled Full Space CPM scene"
                    }
                    .scale
            assertWithMessage(
                    "Requires non-unit Full Space task scale from keyEntity continuity to " +
                        "exercise ActivitySpace scale compensation " +
                        "(spatialTransform.scale=$spatialTransformScale)"
                )
                .that(abs(spatialTransformScale.x - 1.0f))
                .isGreaterThan(MIN_NON_UNIT_SCALE_DELTA)

            // Also create 2 settled-FSM anchors with immediate setScale(0.1f, Space.REAL_WORLD).
            val settledTriples =
                listOf(
                        "settledLeft" to Pose(Vector3(-0.25f, 0f, -1.5f), Quaternion.Identity),
                        "settledRight" to Pose(Vector3(0.25f, 0f, -1.5f), Quaternion.Identity),
                    )
                    .map { (label, pose) ->
                        val anchor = createTrackingAnchor(session, pose)
                        createdAnchors.add(anchor)
                        val space = AnchorSpace.create(session, anchor)
                        val gizmo = sessionRule.onMainThread {
                            GltfModelEntity.create(
                                session = session,
                                model = gltfModel,
                                pose = Pose.Identity,
                                parent = space,
                            )
                        }
                        createdGizmos.add(gizmo)
                        setRealWorldScale(gizmo, TARGET_GIZMO_SCALE)
                        val scaledOnce = AtomicBoolean(false)
                        val callbackFired = AtomicBoolean(false)
                        val listener = Runnable {
                            observedAnchorWorldScales.add(getRealWorldScale(space))
                            if (scaledOnce.compareAndSet(false, true)) {
                                setRealWorldScale(gizmo, TARGET_GIZMO_SCALE)
                            }
                            callbackFired.set(true)
                        }
                        space.addOriginChangedListener(Executor { it.run() }, listener)
                        originListeners.add(space to listener)
                        QuadTuple(label, anchor, space, gizmo, callbackFired)
                    }

            waitUntil(
                "AnchorSpace origin callbacks fire and anchors reach TRACKING",
                timeout = 5.seconds,
            ) {
                transitionCallbackFired.get() &&
                    transitionAnchor.state.value.trackingState == TrackingState.TRACKING &&
                    settledTriples.all {
                        it.callbackFired.get() &&
                            it.anchor.state.value.trackingState == TrackingState.TRACKING
                    }
            }

            val activityWorldScale = getRealWorldScale(session.scene.activitySpace)
            assertThat(activityWorldScale.isFinite()).isTrue()
            assertThat(activityWorldScale).isGreaterThan(0f)

            assertThat(observedAnchorWorldScales).isNotEmpty()
            for (observedScale in observedAnchorWorldScales) {
                assertWithMessage(
                        "AnchorSpace.getScale(Space.REAL_WORLD) on origin callback " +
                            "(observed=$observedAnchorWorldScales)"
                    )
                    .that(observedScale)
                    .isWithin(SCALE_TOLERANCE)
                    .of(1.0f)
            }

            val allTriples =
                listOf(Triple("transitionAnchor", transitionSpace, transitionGizmo)) +
                    settledTriples.map { Triple(it.label, it.space, it.gizmo) }
            val expectedAnchorActivityScale = 1.0f / activityWorldScale
            val expectedGizmoActivityScale = TARGET_GIZMO_SCALE / activityWorldScale

            for ((label, space, gizmo) in allTriples) {
                assertWithMessage("$label gizmo getScale(Space.PARENT)")
                    .that(gizmo.getScale(Space.PARENT))
                    .isWithin(SCALE_TOLERANCE)
                    .of(TARGET_GIZMO_SCALE)
                assertWithMessage("$label gizmo getScale(Space.REAL_WORLD)")
                    .that(getRealWorldScale(gizmo))
                    .isWithin(SCALE_TOLERANCE)
                    .of(TARGET_GIZMO_SCALE)
                assertWithMessage("$label gizmo getScale(Space.ACTIVITY)")
                    .that(gizmo.getScale(Space.ACTIVITY))
                    .isWithin(SCALE_TOLERANCE)
                    .of(expectedGizmoActivityScale)
                assertWithMessage("$label space getScale(Space.REAL_WORLD)")
                    .that(getRealWorldScale(space))
                    .isWithin(SCALE_TOLERANCE)
                    .of(1.0f)
                assertWithMessage("$label space getScale(Space.ACTIVITY)")
                    .that(space.getScale(Space.ACTIVITY))
                    .isWithin(SCALE_TOLERANCE)
                    .of(expectedAnchorActivityScale)
            }
        } finally {
            for ((space, listener) in originListeners) {
                space.removeOriginChangedListener(listener)
            }
            for (gizmo in createdGizmos) {
                gizmo.parent = null
            }
            for (anchor in createdAnchors) {
                anchor.detach()
            }
            sessionRule.onMainThread { gltfModel.close() }
        }
    }

    private fun createTrackingAnchor(session: Session, pose: Pose): Anchor {
        val anchorRef = AtomicReference<Anchor?>(null)
        waitUntil("Anchor.create succeeds with spatial tracking", timeout = 5.seconds) {
            when (val result = Anchor.create(session = session, pose = pose)) {
                is AnchorCreateSuccess -> {
                    anchorRef.set(result.anchor)
                    true
                }
                is AnchorCreateTrackingUnavailable -> false
                is AnchorCreateResourcesExhausted ->
                    throw AssertionError("Anchor creation exhausted runtime resources")
                else -> throw AssertionError("Unexpected Anchor.create result: $result")
            }
        }
        return checkNotNull(anchorRef.get())
    }

    private data class QuadTuple(
        val label: String,
        val anchor: Anchor,
        val space: AnchorSpace,
        val gizmo: GltfModelEntity,
        val callbackFired: AtomicBoolean,
    )

    @Suppress("DEPRECATION")
    private fun setRealWorldScale(entity: Entity, scale: Float) {
        entity.setScale(scale, Space.REAL_WORLD)
    }

    @Suppress("DEPRECATION")
    private fun getRealWorldScale(entity: Entity): Float = entity.getScale(Space.REAL_WORLD)

    private fun createChildPanel(name: String): PanelEntity = sessionRule.onMainThread {
        val view = TextView(activity).apply { text = name }
        PanelEntity.create(
            session = session,
            view = view,
            dimensions = FloatSize2d(0.6f, 0.4f),
            name = name,
            pose = Pose.Identity,
        )
    }

    private companion object {
        const val UNANCHORED_PANEL_NAME = "unanchored_anchor_panel"
        const val ANCHORED_PANEL_NAME = "anchored_child_panel"
        const val GIZMO_MODEL_ASSET = "xyzArrows.glb"
        const val CONTINUITY_TASK_SCALE = 1.75f
        const val TARGET_GIZMO_SCALE = 0.1f
        const val MIN_NON_UNIT_SCALE_DELTA = 0.05f
        const val POSE_TOLERANCE = 1e-3f
        const val SCALE_TOLERANCE = 1e-3f
    }
}
