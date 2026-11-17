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
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorCreateTrackingUnavailable
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.captureSpatialScene
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
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
            POSE_TOLERANCE,
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
        const val POSE_TOLERANCE = 1e-3f
    }
}
