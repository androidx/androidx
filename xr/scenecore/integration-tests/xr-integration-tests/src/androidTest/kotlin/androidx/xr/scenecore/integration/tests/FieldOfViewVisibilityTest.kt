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

import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialScene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.XrSessionScope
import androidx.xr.testutils.captureSpatialScene
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * End-to-end integration tests for [Scene.addSpatialVisibilityChangedListener] and
 * [Scene.removeSpatialVisibilityChangedListener]:
 * - Moving panel entities into, partially across, and outside the user's field of view.
 * - Hiding and showing contributing panels and parent entities via [Entity.setEnabled].
 * - Verifying non-zero [Entity.setAlpha] preserves visibility bounds while `alpha = 0.0f` excludes
 *   the entity from visibility bounds.
 * - Verifying [SpatialEnvironment] skybox and room geometry do not affect scene content FOV bounds.
 * - Verifying [SurfaceEntity] creation, shape changes (`Quad`, `Sphere`, `Hemisphere`), pose
 *   updates, and detachment update scene content FOV bounds.
 * - Verifying [GltfModelEntity] creation, hiding/showing, and detachment update scene content FOV
 *   bounds.
 * - Verifying Home Space Mode transitions fire [SpatialVisibility] for the main window and removing
 *   a listener stops subsequent callbacks.
 *
 * Simulated 6DoF head-rotation tests live in [FieldOfViewVisibilityHeadTrackingTest].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class FieldOfViewVisibilityTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    private val recorder = SpatialVisibilityRecorder()
    private lateinit var anchors: FovAnchors

    @Before
    fun setUp() {
        val scene = sessionRule.session.scene
        scene.addSpatialVisibilityChangedListener(recorder)
        sessionRule.requestFullSpace()
        anchors = sessionRule.computeFovAnchors()
        // Hide the main panel so only each test's own entities contribute to visibility bounds,
        // and start from OUTSIDE so subsequent transitions always trigger callbacks.
        scene.mainPanelEntity.setEnabled(false)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
    }

    @After
    fun tearDown() {
        sessionRule.session.scene.removeSpatialVisibilityChangedListener(recorder)
    }

    @Test
    fun fieldOfView_movePanelsInFrontOfAndBehindUser_reportsWithinPartiallyAndOutside() {
        val (panelA, panelB) =
            sessionRule.onMainThread {
                createFovTestPanel("fovPanelA", anchors.behindUser) to
                    createFovTestPanel("fovPanelB", anchors.behindUser)
            }

        panelA.setPose(anchors.inFront)
        panelB.setPose(anchors.besideInFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // One panel in front of the user and one behind straddles the field of view.
        panelB.setPose(anchors.behindUser)
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        panelA.setPose(anchors.behindUser)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)

        panelA.setPose(anchors.inFront)
        panelB.setPose(anchors.besideInFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Test
    fun fieldOfView_setEnabledOnContributingPanelAndParent_updatesVisibilityBounds() {
        val parentBehind =
            Entity.create(
                session = sessionRule.session,
                name = "fovParentBehind",
                pose = anchors.behindUser,
                parent = sessionRule.session.scene.activitySpace,
            )
        val (panelInFront, childPanelBehind) =
            sessionRule.onMainThread {
                createFovTestPanel("fovPanelInFront", anchors.behindUser) to
                    createFovTestPanel("fovChildPanelBehind", Pose.Identity, parent = parentBehind)
            }

        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Hiding the behind-user panel shrinks the scene bounds to the in-front panel.
        childPanelBehind.setEnabled(false)
        assertThat(childPanelBehind.isEnabled(includeParents = false)).isFalse()
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Re-enabling the panel re-expands the scene bounds.
        childPanelBehind.setEnabled(true)
        assertThat(childPanelBehind.isEnabled(includeParents = false)).isTrue()
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Disabling the parent entity also excludes the child panel from FOV bounds.
        parentBehind.setEnabled(false)
        assertThat(childPanelBehind.isEnabled(includeParents = true)).isFalse()
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        parentBehind.setEnabled(true)
        assertThat(childPanelBehind.isEnabled(includeParents = true)).isTrue()
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Hiding all remaining entities leaves no visible content in the field of view.
        panelInFront.setEnabled(false)
        parentBehind.setEnabled(false)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
    }

    @Test
    fun fieldOfView_nonZeroAlpha_preservesVisibilityBounds() {
        val (panelInFront, panelBehind) =
            sessionRule.onMainThread {
                createFovTestPanel("fovAlphaFront", anchors.behindUser) to
                    createFovTestPanel("fovAlphaBehind", anchors.behindUser)
            }

        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Set non-zero alpha (0.5f) on the behind-user panel, then force the platform to
        // re-evaluate the scene's FOV bounds by toggling panelInFront off and back on.
        val markBeforeHalfAlpha = recorder.mark()
        panelBehind.setAlpha(0.5f)
        assertThat(panelBehind.getAlpha(Space.ACTIVITY)).isWithin(1e-3f).of(0.5f)
        panelInFront.setEnabled(false)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
        assertThat(recorder.eventsSince(markBeforeHalfAlpha))
            .doesNotContain(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        panelInFront.setEnabled(true)
        // Reaching PARTIALLY_WITHIN_FIELD_OF_VIEW (rather than WITHIN_FIELD_OF_VIEW) proves that
        // panelBehind with alpha = 0.5f still contributes to the scene's FOV bounding box.
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        panelBehind.setAlpha(1.0f)
        assertThat(panelBehind.getAlpha(Space.ACTIVITY)).isWithin(1e-3f).of(1.0f)
        panelBehind.setPose(anchors.besideInFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Ignore("b/584328354: Entity.setAlpha(0.0f) does not yet exclude entities from FOV bounds.")
    @Test
    fun fieldOfView_alphaZero_excludesPanelFromVisibilityBounds() {
        val (panelInFront, panelBehind) =
            sessionRule.onMainThread {
                createFovTestPanel("fovAlphaZeroFront", anchors.behindUser) to
                    createFovTestPanel("fovAlphaZeroBehind", anchors.behindUser)
            }

        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Setting alpha to 0.0f excludes the behind-user panel just like hiding it.
        panelBehind.setAlpha(0.0f)
        assertThat(panelBehind.getAlpha(Space.ACTIVITY)).isWithin(1e-3f).of(0.0f)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Restoring non-zero alpha re-includes the panel in the FOV bounding box.
        panelBehind.setAlpha(1.0f)
        assertThat(panelBehind.getAlpha(Space.ACTIVITY)).isWithin(1e-3f).of(1.0f)
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)
    }

    // Method-level @XrDeviceTest so that XrDeviceFilter applies @RequiresSpatialApi.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun fieldOfView_spatialEnvironmentSkyboxAndGeometry_doNotAffectVisibilityBounds() {
        val env = sessionRule.session.scene.spatialEnvironment
        val (panelInFront, preference) =
            sessionRule.onMainThread {
                val blueSkybox =
                    ImageBasedLightingAsset.createFromZip(
                        session,
                        Paths.get("skyboxes", "BlueSkybox.zip"),
                    )
                val groundGeometry =
                    GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))
                createFovTestPanel("fovEnvPanel", anchors.behindUser) to
                    SpatialEnvironmentPreference(blueSkybox, groundGeometry)
            }

        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Apply a skybox and room-scale environment geometry. Once active, hiding panelInFront
        // while the custom environment remains active must transition to OUTSIDE_FIELD_OF_VIEW
        // (proving neither the skybox nor GroundGeometry.glb contributes to the scene's FOV
        // bounding box).
        val systemGeometryNodeIds =
            findEnvironmentGeometryNodes(captureSpatialScene()).map { it.id }.toSet()
        val markBeforeSetEnv = recorder.mark()
        sessionRule.onMainThread { env.preferredSpatialEnvironment = preference }
        sessionRule.waitForSpatialEnvironmentSettled()
        // Environment geometry attaches asynchronously after the environment becomes active; wait
        // for it to be in the scene graph before asserting it doesn't affect visibility bounds.
        awaitEnvironmentGeometryAttached(systemGeometryNodeIds)
        assertThat(recorder.eventsSince(markBeforeSetEnv))
            .containsNoneOf(
                SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW,
                SpatialVisibility.OUTSIDE_FIELD_OF_VIEW,
            )

        panelInFront.setEnabled(false)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)

        panelInFront.setEnabled(true)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Reverting to the system default environment must also preserve WITHIN_FIELD_OF_VIEW.
        val markBeforeRevertEnv = recorder.mark()
        sessionRule.onMainThread { env.preferredSpatialEnvironment = null }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(recorder.eventsSince(markBeforeRevertEnv))
            .containsNoneOf(
                SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW,
                SpatialVisibility.OUTSIDE_FIELD_OF_VIEW,
            )

        panelInFront.setPose(anchors.behindUser)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
    }

    @Test
    fun fieldOfView_surfaceEntityShapesAndPose_contributeToVisibilityBounds() {
        val panelInFront = sessionRule.onMainThread {
            createFovTestPanel("fovSurfaceAnchorPanel", anchors.behindUser)
        }
        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        val quadShape = SurfaceEntity.Shape.Quad(FloatSize2d(PANEL_SIZE_M, PANEL_SIZE_M))
        val surfaceEntity = sessionRule.onMainThread {
            SurfaceEntity.create(
                    session = session,
                    pose = anchors.behindUser,
                    shape = quadShape,
                    stereoMode = SurfaceEntity.StereoMode.MONO,
                    parent = session.scene.activitySpace,
                )
                .also { entity ->
                    val surface = entity.getSurface()
                    val canvas = surface.lockHardwareCanvas()
                    canvas.drawColor(Color.MAGENTA)
                    surface.unlockCanvasAndPost(canvas)
                }
        }

        // Creating the Quad SurfaceEntity behind the user expands the scene bounds outside the
        // field of view while panelInFront remains in view.
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        surfaceEntity.setPose(anchors.besideInFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // A large 360° sphere centered in front of the user wraps behind the user's head, so the
        // surface is only partially within the field of view.
        sessionRule.onMainThread {
            surfaceEntity.shape = SurfaceEntity.Shape.Sphere(SURROUNDING_SURFACE_RADIUS_M)
        }
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        sessionRule.onMainThread { surfaceEntity.shape = quadShape }
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // A large 180° hemisphere also extends beyond the user's field of view.
        sessionRule.onMainThread {
            surfaceEntity.shape = SurfaceEntity.Shape.Hemisphere(SURROUNDING_SURFACE_RADIUS_M)
        }
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Detaching the SurfaceEntity shrinks the bounds back to the in-front panel.
        surfaceEntity.parent = null
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    // Method-level @XrDeviceTest so that XrDeviceFilter applies @RequiresSpatialApi.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun fieldOfView_gltfModelEntityCreateHideShowAndRemove_updatesVisibilityBounds() {
        val panelInFront = sessionRule.onMainThread {
            createFovTestPanel("fovGltfAnchorPanel", anchors.behindUser)
        }
        panelInFront.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        val gltfEntity = sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                pose = anchors.behindUser,
                parent = session.scene.activitySpace,
            )
        }

        // Creating the glTF entity behind the user expands the scene bounds to PARTIALLY.
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Hiding the glTF entity shrinks the bounds back to the in-front panel.
        gltfEntity.setEnabled(false)
        assertThat(gltfEntity.isEnabled(includeParents = false)).isFalse()
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Showing the glTF entity re-expands the bounds.
        gltfEntity.setEnabled(true)
        assertThat(gltfEntity.isEnabled(includeParents = false)).isTrue()
        recorder.awaitState(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        // Detaching the glTF entity shrinks the bounds back to WITHIN_FIELD_OF_VIEW.
        gltfEntity.parent = null
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Test
    fun fieldOfView_homeSpaceModeTransitionAndRemoveListener_firesForMainWindowAndStopsAfterRemoval() {
        val panel = sessionRule.onMainThread {
            createFovTestPanel("fovHsmPanel", anchors.behindUser)
        }

        // In Home Space Mode, the system renders the 2D main window in front of the user even when
        // mainPanelEntity was disabled in Full Space Mode. Depending on device FOV and window
        // placement, this may be WITHIN or PARTIALLY_WITHIN.
        sessionRule.requestHomeSpace()
        recorder.awaitAnyState(
            SpatialVisibility.WITHIN_FIELD_OF_VIEW,
            SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW,
        )

        // Returning to Full Space Mode with mainPanelEntity still disabled and panel behind the
        // user transitions back to OUTSIDE_FIELD_OF_VIEW.
        sessionRule.requestFullSpace()
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)

        // Unregistering the listener must stop delivery of subsequent WITHIN_FIELD_OF_VIEW events.
        val scene = sessionRule.session.scene
        val activeRecorder = SpatialVisibilityRecorder()
        scene.removeSpatialVisibilityChangedListener(recorder)
        scene.addSpatialVisibilityChangedListener(activeRecorder)
        try {
            val markAfterRemoval = recorder.mark()
            panel.setPose(anchors.inFront)
            activeRecorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
            assertThat(recorder.eventsSince(markAfterRemoval))
                .doesNotContain(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
        } finally {
            scene.removeSpatialVisibilityChangedListener(activeRecorder)
        }
    }

    private fun findEnvironmentGeometryNodes(scene: SpatialScene): List<SpatialNode> =
        scene.rootNodes
            .firstNotNullOfOrNull { root -> root.findNode { it.name == ENVIRONMENT_ROOT_NODE } }
            ?.findDescendants { it.name == ENVIRONMENT_GEOMETRY_NODE_NAME }
            .orEmpty()

    /**
     * Blocks until a visible [ENVIRONMENT_GEOMETRY_NODE_NAME] not in [previousGeometryNodeIds] is
     * attached under [ENVIRONMENT_ROOT_NODE].
     */
    private fun awaitEnvironmentGeometryAttached(previousGeometryNodeIds: Set<Int>) {
        waitForSpatialScene(
            description = "$ENVIRONMENT_GEOMETRY_NODE_NAME to be attached and visible",
            timeout = ENVIRONMENT_GEOMETRY_TIMEOUT,
            select = { scene ->
                findEnvironmentGeometryNodes(scene).filter { it.id !in previousGeometryNodeIds }
            },
            until = { nodes -> nodes.any { it.isVisible } },
        )
    }

    private companion object {
        const val ENVIRONMENT_ROOT_NODE = "Environment Root"
        const val ENVIRONMENT_GEOMETRY_NODE_NAME = "EnvironmentGeometryNode"
        val ENVIRONMENT_GEOMETRY_TIMEOUT = 10.seconds
    }
}

internal data class FovAnchors(val inFront: Pose, val besideInFront: Pose, val behindUser: Pose)

/**
 * Waits for the main thread to apply the recommended Full Space Mode pose to
 * [Scene.mainPanelEntity] and returns head-relative [FovAnchors] derived from that pose.
 */
internal fun XrSessionRule<*, *>.computeFovAnchors(): FovAnchors {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    val inFront = session.scene.mainPanelEntity.getPose(Space.ACTIVITY)
    return FovAnchors(
        inFront = inFront,
        besideInFront = inFront.compose(Pose(Vector3(PANEL_GAP_M, 0f, 0f))),
        behindUser = inFront.compose(Pose(Vector3(0f, 0f, BEHIND_USER_OFFSET_M))),
    )
}

/**
 * Creates a white [PanelEntity] under [parent]. It creates a [View], so call it inside
 * [XrSessionRule.onMainThread].
 */
internal fun XrSessionScope<*>.createFovTestPanel(
    name: String,
    pose: Pose,
    dimensions: FloatSize2d = FloatSize2d(PANEL_SIZE_M, PANEL_SIZE_M),
    parent: Entity = session.scene.activitySpace,
): PanelEntity =
    PanelEntity.create(
        session = session,
        view = View(activity).apply { setBackgroundColor(Color.WHITE) },
        dimensions = dimensions,
        name = name,
        pose = pose,
        parent = parent,
    )

/**
 * Records, in order, every [SpatialVisibility] delivered to a scene listener.
 *
 * The listener runs on the main thread and publishes each event with a single atomic
 * [MutableStateFlow] update, so the instrumentation thread can read the events directly.
 */
internal class SpatialVisibilityRecorder : Consumer<SpatialVisibility> {
    private val events = MutableStateFlow<List<SpatialVisibility>>(emptyList())

    override fun accept(visibility: SpatialVisibility) {
        events.update { it + visibility }
    }

    /** Returns the current event count for use with [eventsSince] and [stateBeforeLatest]. */
    fun mark(): Int = events.value.size

    /** Returns all events recorded since [mark] was taken. */
    fun eventsSince(mark: Int): List<SpatialVisibility> = events.value.drop(mark)

    /**
     * Returns the state reported immediately before the latest state, considering only the state
     * that was current when [mark] was taken and the events recorded after it, or `null` if the
     * latest state has not changed since [mark].
     */
    fun stateBeforeLatest(mark: Int): SpatialVisibility? {
        val states = events.value.drop((mark - 1).coerceAtLeast(0))
        val latest = states.lastOrNull() ?: return null
        return states.dropLastWhile { it == latest }.lastOrNull()
    }

    /** Blocks on the instrumentation thread until the most recent event is [expected]. */
    fun awaitState(expected: SpatialVisibility) = awaitAnyState(expected)

    /** Blocks on the instrumentation thread until the most recent event is one of [expected]. */
    fun awaitAnyState(vararg expected: SpatialVisibility) {
        val targets = expected.toList()
        try {
            waitUntil(description = "SpatialVisibility in $targets", timeout = VISIBILITY_TIMEOUT) {
                events.value.lastOrNull() in targets
            }
        } catch (e: AssertionError) {
            // waitUntil builds its message before polling starts, so report the events that
            // actually arrived by the time the wait timed out.
            throw AssertionError("${e.message} Received events: ${events.value}", e)
        }
    }
}

internal val VISIBILITY_TIMEOUT = 5.seconds
/** Small enough to fit wholly inside the field of view at the main panel distance. */
internal const val PANEL_SIZE_M = 0.1f
internal const val PANEL_GAP_M = 0.15f
/** Offset along the main panel's +Z axis so the entity is placed behind the user. */
internal const val BEHIND_USER_OFFSET_M = 10f
/** Radius large enough to wrap behind the user when centered on the in-front anchor. */
internal const val SURROUNDING_SURFACE_RADIUS_M = 5.0f
