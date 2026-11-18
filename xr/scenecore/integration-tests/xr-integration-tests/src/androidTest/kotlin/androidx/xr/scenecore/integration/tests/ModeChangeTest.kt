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

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.SpatialWindow
import androidx.xr.scenecore.createBundleForFullSpaceLaunch
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createAndroidXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.toPixelArray
import androidx.xr.testutils.toSoftwareBitmap
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

// TODO(b/584749603): Automate the CUJ steps that launch Settings with
// createBundleForFullSpaceLaunchWithEnvironmentInherited, which needs the privileged
// INHERIT_FSM_ENV permission.
/**
 * End-to-end tests for the SceneCore Home Space Mode / Full Space Mode CUJ
 * (`FsmHsmTransitionActivity` in the SceneCore testapp).
 *
 * Like the testapp, [FullSpaceStartActivity] declares `XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED`,
 * so every test starts in Full Space Mode. The tests cover:
 * - Starting in Full Space Mode, and switching to Home Space Mode and back with
 *   [Scene.requestHomeSpace] and [Scene.requestFullSpace], including the system caption bar (the
 *   System UI toolbar) that SysUI only shows on the main panel in Home Space Mode.
 * - Resizing and moving the main panel in Full Space Mode through the system affordances of a
 *   [ResizableComponent] and a system [MovableComponent].
 * - Resizing the main panel to portrait and landscape with [PanelEntity.sizeInPixels] in Full Space
 *   Mode, and with [SpatialWindow.setPreferredAspectRatio] in Home Space Mode.
 * - A blue skybox app environment that disappears in Home Space Mode, and while Settings, launched
 *   with [createBundleForFullSpaceLaunch], owns Full Space Mode.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ModeChangeTest {

    @get:Rule(order = 0) val sessionRule = createAndroidXrSessionRule<FullSpaceStartActivity>()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    private val scene: Scene
        get() = sessionRule.session.scene

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)
    fun fullSpaceStartMode_startsActivityInFullSpace() {
        // CUJ: The app starts in Full Space Mode.
        assertThat(scene.spatialCapabilities)
            .containsAtLeast(SpatialCapability.SPATIAL_UI, SpatialCapability.APP_ENVIRONMENT)
        val bounds = scene.activitySpace.bounds
        assertWithMessage("activity space bounds $bounds").that(bounds.isUnbounded()).isTrue()
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) // Reads the system scene graph.
    fun requestHomeSpaceThenFullSpace_switchesModesAndSystemCaptionBar() {
        val reportedBounds = CopyOnWriteArrayList<FloatSize3d>()
        val boundsListener = Consumer<FloatSize3d> { reportedBounds.add(it) }
        val activitySpace = scene.activitySpace
        activitySpace.addBoundsChangedListener(boundsListener)
        try {
            // CUJ: Click "Request HSM". The app enters Home Space Mode, and SysUI shows its
            // toolbar.
            sessionRule.requestHomeSpace()
            assertThat(scene.spatialCapabilities)
                .containsNoneOf(SpatialCapability.SPATIAL_UI, SpatialCapability.APP_ENVIRONMENT)
            waitUntil("bounded activity space bounds to be reported", SPACE_MODE_TIMEOUT) {
                reportedBounds.lastOrNull()?.isUnbounded() == false
            }
            awaitSystemCaptionBarVisible(visible = true)

            // CUJ: Click "Request FSM". The app enters Full Space Mode, and the toolbar disappears.
            sessionRule.requestFullSpace()
            assertThat(scene.spatialCapabilities)
                .containsAtLeast(SpatialCapability.SPATIAL_UI, SpatialCapability.APP_ENVIRONMENT)
            waitUntil("unbounded activity space bounds to be reported", SPACE_MODE_TIMEOUT) {
                reportedBounds.lastOrNull()?.isUnbounded() == true
            }
            awaitSystemCaptionBarVisible(visible = false)
        } finally {
            activitySpace.removeBoundsChangedListener(boundsListener)
        }
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) // Injects spatial input.
    fun resizableComponent_onMainPanel_resizesMainPanelWithSystemAffordance() {
        val mainPanel = scene.mainPanelEntity
        val resizeEnded = CountDownLatch(1)

        // CUJ: Toggle "Resizable in FSM". Like the testapp, apply the size that ResizableComponent
        // proposes at the end of the resize.
        val resizableComponent =
            ResizableComponent.create(sessionRule.session) { event ->
                if (event.resizeState == ResizeEvent.ResizeState.END) {
                    mainPanel.size = event.newSize.to2d()
                    resizeEnded.countDown()
                }
            }
        assertThat(mainPanel.addComponent(resizableComponent)).isTrue()

        // CUJ: Resize the main panel with the affordance at its corner.
        val mainPanelNode = awaitMainPanelNode { it.isResizable }
        val initialSize = mainPanel.sizeInPixels
        mainPanelNode.performSpatialInteraction {
            resizeNodeBy(RESIZE_DELTA, alignment = SpatialAlignment.BottomRight)
        }

        assertWithMessage("resize end event")
            .that(resizeEnded.await(INTERACTION_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .isTrue()
        waitUntil("main panel to grow beyond $initialSize", INTERACTION_TIMEOUT) {
            val size = mainPanel.sizeInPixels
            size.width > initialSize.width && size.height > initialSize.height
        }
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) // Injects spatial input.
    fun systemMovableComponent_onMainPanel_movesMainPanelWithSystemAffordance() {
        val mainPanel = scene.mainPanelEntity
        val moveEnded = CountDownLatch(1)

        // CUJ: Toggle "Movable in FSM". The system moves the main panel itself.
        val movableComponent =
            MovableComponent.createSystemMovable(sessionRule.session, scaleInZ = false)
        movableComponent.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    moveEnded.countDown()
                }
            }
        )
        assertThat(mainPanel.addComponent(movableComponent)).isTrue()

        // CUJ: Move the main panel with the affordance at its edge.
        val mainPanelNode = awaitMainPanelNode { it.isMovable }
        val initialPose = mainPanel.getPose()
        mainPanelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        assertWithMessage("move end event")
            .that(moveEnded.await(INTERACTION_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .isTrue()
        waitUntil("main panel to move", INTERACTION_TIMEOUT) {
            (mainPanel.getPose().translation - initialPose.translation).length >
                MIN_MOVE_DISTANCE_METERS
        }
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) // Renders an ImageBasedLightingAsset.
    fun blueSkybox_inFullSpace_turnsSkyBlueAndDisappearsInHomeSpace() {
        // CUJ: Click "Load Skybox". The sky turns blue.
        loadBlueSkyboxAndAwaitBlueSky()

        // CUJ: Click "Request HSM". The app enters Home Space Mode and the blue skybox disappears.
        sessionRule.requestHomeSpace()
        waitUntil("app environment to become inactive", ENVIRONMENT_TIMEOUT) {
            !scene.spatialEnvironment.isPreferredSpatialEnvironmentActive
        }
        awaitSkyBlueFraction("blue skybox to disappear") { it < MIN_BLUE_FRACTION }
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) // Renders IBL; reads the scene graph.
    fun createBundleForFullSpaceLaunch_launchesSettingsInFullSpaceOverAppEnvironment() {
        // CUJ: Click "Load Skybox".
        loadBlueSkyboxAndAwaitBlueSky()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // CUJ: Click "Launch Settings App in a new Task in FSM".
        // Main thread: Activity.startActivity cancels pending input on, and starts exit
        // transitions for, the Activity's views.
        try {
            sessionRule.onMainThread {
                val intent =
                    Intent()
                        .setComponent(ComponentName(SETTINGS_PACKAGE, SETTINGS_ACTIVITY))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val options = ActivityOptions.makeBasic().toBundle()
                activity.startActivity(intent, createBundleForFullSpaceLaunch(session, options))
            }

            // Settings takes over Full Space Mode, which stops this activity. Launching Settings
            // into
            // Home Space Mode instead would keep this activity visible next to it.
            awaitActivityState(Lifecycle.State.CREATED)
            awaitSettingsWindowWithoutSystemCaptionBar()
            // The caption bar must not appear later either, so check again after a settle window.
            SystemClock.sleep(NO_OP_SETTLE_WINDOW.inWholeMilliseconds)
            awaitSettingsWindowWithoutSystemCaptionBar()
            // The blue skybox of this app disappears behind Settings.
            awaitSkyBlueFraction("blue skybox to disappear") { it < MIN_BLUE_FRACTION }

            // CUJ: Navigate back to the test app. It returns in Full Space Mode with its blue
            // skybox.
            device.pressBack()
            awaitActivityState(Lifecycle.State.RESUMED)
            assertThat(scene.spatialCapabilities).contains(SpatialCapability.SPATIAL_UI)
            awaitSkyBlueFraction("blue skybox to reappear") { it >= MIN_BLUE_FRACTION }
        } finally {
            device.executeShellCommand("am force-stop $SETTINGS_PACKAGE")
        }
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)
    fun mainPanelSizeInPixels_inFullSpace_resizesMainPanelToPortraitAndLandscape() {
        val mainPanel = scene.mainPanelEntity

        // CUJ: Click "Resize in FSM (Portrait)".
        // Main thread: the sizeInPixels setter relayouts the panel's views.
        sessionRule.onMainThread { mainPanel.sizeInPixels = FSM_PORTRAIT_SIZE }
        awaitMainPanelSize(FSM_PORTRAIT_SIZE)

        // CUJ: Click "Resize in FSM (Landscape)".
        sessionRule.onMainThread { mainPanel.sizeInPixels = FSM_LANDSCAPE_SIZE }
        awaitMainPanelSize(FSM_LANDSCAPE_SIZE)
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)
    fun setPreferredAspectRatio_inHomeSpace_resizesMainPanelToPortraitAndLandscape() {
        // CUJ: Click "Request HSM".
        sessionRule.requestHomeSpace()
        val session = sessionRule.session
        val activity = sessionRule.activity

        // TODO(b/584749431): Also check how the resize changes the main panel area, once
        // setPreferredAspectRatio and its KDoc agree.
        // CUJ: Click the "Portrait" radio button.
        SpatialWindow.setPreferredAspectRatio(session, activity, HSM_PORTRAIT_ASPECT_RATIO)
        awaitMainPanelAspectRatio(HSM_PORTRAIT_ASPECT_RATIO)

        // CUJ: Click the "Landscape" radio button.
        SpatialWindow.setPreferredAspectRatio(session, activity, HSM_LANDSCAPE_ASPECT_RATIO)
        awaitMainPanelAspectRatio(HSM_LANDSCAPE_ASPECT_RATIO)
    }

    /** Blocks until this activity reaches the lifecycle [state]. */
    private fun awaitActivityState(state: Lifecycle.State) {
        waitUntil("activity to reach $state", ACTIVITY_TIMEOUT) {
            sessionRule.onMainThread { activity.lifecycle.currentState == state }
        }
    }

    /**
     * Blocks until the system caption bar of this activity's task is [visible].
     *
     * SysUI draws the caption bar (the System UI toolbar of Home Space Mode windows), and no public
     * API exposes it, so this reads the system scene graph.
     */
    private fun awaitSystemCaptionBarVisible(visible: Boolean) {
        val activity = sessionRule.activity
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForSpatialScene(
            description = "system caption bar to be ${if (visible) "shown" else "hidden"}",
            select = { it.findTaskByActivity(activity)?.hasVisibleCaptionBar() },
            until = { it == visible },
        )
    }

    /**
     * Blocks until Settings shows its window without a system caption bar, which only decorates
     * Home Space Mode windows. No public API exposes the space mode of another task, so this reads
     * the system scene graph.
     */
    private fun awaitSettingsWindowWithoutSystemCaptionBar() {
        waitForSpatialScene(
            description = "Settings window without a system caption bar",
            select = { scene ->
                scene.findTasksByPackageName(SETTINGS_PACKAGE).firstOrNull { it.isActive }
            },
            until = { it?.taskWindowLeash?.isVisible == true && !it.hasVisibleCaptionBar() },
        )
    }

    /**
     * Blocks until the system scene graph shows the main panel ready for an injected gesture: it
     * passes [isReady] (the capability the gesture needs) and has known extents.
     */
    private fun awaitMainPanelNode(isReady: (SnapshotXrSpace) -> Boolean): SnapshotXrSpace {
        val activity = sessionRule.activity
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(
            waitForSpatialScene(
                description = "main panel ready for interaction",
                select = { scene ->
                    scene.findTaskByActivity(activity)?.let { task ->
                        task.findNode { it.name == MAIN_PANEL_NODE_NAME } ?: task.taskWindowLeash
                    }
                },
                until = { it != null && isReady(it) && it.worldExtents != null },
            )
        )
    }

    /**
     * Blocks until the main panel reports [expected] pixels and the content of its window is laid
     * out at that size. The main panel reads its size from the WindowManager, which applies a new
     * size asynchronously.
     */
    private fun awaitMainPanelSize(expected: IntSize2d) {
        waitUntil("main panel and its window to be resized to $expected", WINDOW_RESIZE_TIMEOUT) {
            scene.mainPanelEntity.sizeInPixels == expected &&
                sessionRule.onMainThread {
                    val decorView = activity.window.decorView
                    decorView.width == expected.width && decorView.height == expected.height
                }
        }
    }

    /** Blocks until the main panel, and the window content it hosts, have the [expected] ratio. */
    private fun awaitMainPanelAspectRatio(expected: Float) {
        waitUntil("main panel aspect ratio to be $expected", WINDOW_RESIZE_TIMEOUT) {
            abs(scene.mainPanelEntity.sizeInPixels.aspectRatio() - expected) <=
                ASPECT_RATIO_TOLERANCE
        }
        awaitMainPanelSize(scene.mainPanelEntity.sizeInPixels)
    }

    /**
     * Loads `BlueSkybox.zip` as the app environment and blocks until the sky turns blue.
     *
     * The sky is first checked against a black void, so that the baseline does not depend on the
     * system environment. Like the testapp, which loads the skybox when it starts, this also
     * creates the asset well before using it.
     */
    private fun loadBlueSkyboxAndAwaitBlueSky() {
        // Create the asset before the black void baseline: a skybox set right after createFromZip()
        // returns can stay black while isPreferredSpatialEnvironmentActive is true (b/584750674).
        val skybox =
            // Main thread: ImageBasedLightingAsset is Impress-backed.
            sessionRule.onMainThread {
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            }
        setPreferredEnvironmentAndWait(SpatialEnvironmentPreference(null, null))
        awaitSkyBlueFraction("sky to not be blue") { it <= MAX_NOT_BLUE_FRACTION }

        setPreferredEnvironmentAndWait(SpatialEnvironmentPreference(skybox, null))
        awaitSkyBlueFraction("sky to turn blue") { it >= MIN_BLUE_FRACTION }
    }

    /**
     * Sets [preference] as the app environment, with passthrough off so that it is not covered, and
     * blocks until the system reports the environment and passthrough state as settled.
     */
    private fun setPreferredEnvironmentAndWait(preference: SpatialEnvironmentPreference) {
        val environment = scene.spatialEnvironment
        environment.preferredPassthroughOpacity = 0f
        // Main thread: the preferredSpatialEnvironment setter throws on other threads.
        sessionRule.onMainThread { environment.preferredSpatialEnvironment = preference }
        sessionRule.waitForSpatialEnvironmentSettled()
    }

    /**
     * Takes screenshots until [until] holds for the fraction of blue sky they show, as computed by
     * [skyBlueFraction]. Blocks, so never call it inside `onMainThread`.
     */
    private fun awaitSkyBlueFraction(description: String, until: (Float) -> Boolean) {
        var lastFraction: Float? = null
        try {
            waitUntil(description, SCREENSHOT_TIMEOUT, SCREENSHOT_POLL_INTERVAL) {
                val screenshot =
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                        ?: return@waitUntil false
                val fraction =
                    try {
                        skyBlueFraction(screenshot)
                    } finally {
                        screenshot.recycle()
                    }
                lastFraction = fraction
                until(fraction)
            }
        } catch (e: AssertionError) {
            throw AssertionError("${e.message} Last blue fraction: $lastFraction", e)
        }
    }

    /**
     * Returns the fraction of saturated blue pixels in the bluer half (top or bottom) of
     * [screenshot], so that the check does not depend on whether the headset renders upside down.
     */
    private fun skyBlueFraction(screenshot: Bitmap): Float {
        val source = toSoftwareBitmap(screenshot)
        try {
            val sampleHeight = (SAMPLE_WIDTH * source.height / source.width).coerceAtLeast(2)
            val sample = Bitmap.createScaledBitmap(source, SAMPLE_WIDTH, sampleHeight, true)
            val pixels =
                try {
                    toPixelArray(sample)
                } finally {
                    if (sample !== source) sample.recycle()
                }
            val half = SAMPLE_WIDTH * (sampleHeight / 2)
            val hsv = FloatArray(3)
            fun fraction(range: IntRange) =
                range
                    .count { i ->
                        Color.colorToHSV(pixels[i], hsv)
                        hsv[0] in BLUE_HUE_RANGE &&
                            hsv[1] >= MIN_BLUE_SATURATION &&
                            hsv[2] >= MIN_BLUE_BRIGHTNESS
                    }
                    .toFloat() / range.count()
            return maxOf(fraction(0 until half), fraction(half until pixels.size))
        } finally {
            if (source !== screenshot) source.recycle()
        }
    }

    private fun FloatSize3d.isUnbounded(): Boolean =
        width == Float.POSITIVE_INFINITY &&
            height == Float.POSITIVE_INFINITY &&
            depth == Float.POSITIVE_INFINITY

    private fun IntSize2d.aspectRatio(): Float = width.toFloat() / height

    /** Whether SysUI shows a caption bar surface for this task. */
    private fun SnapshotXrSpace.hasVisibleCaptionBar(): Boolean =
        findNode { it.isCaptionBar && it.isAndroid2dSurface && it.isVisible } != null

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val SETTINGS_ACTIVITY = "com.android.settings.Settings"

        /** Name of the task-scoped node that backs the main panel in the system scene graph. */
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

        val ENVIRONMENT_TIMEOUT = 5.seconds
        val ACTIVITY_TIMEOUT = 5.seconds
        val WINDOW_RESIZE_TIMEOUT = 5.seconds
        val INTERACTION_TIMEOUT = 5.seconds

        /** Settle window of the check that no caption bar appears on the Settings window. */
        val NO_OP_SETTLE_WINDOW = 500.milliseconds

        /** Main panel sizes of the testapp's "Resize in FSM" buttons. */
        val FSM_PORTRAIT_SIZE = IntSize2d(1200, 1600)
        val FSM_LANDSCAPE_SIZE = IntSize2d(1600, 1200)

        /** Aspect ratios (width / height) of the testapp's Home Space Mode radio buttons. */
        const val HSM_PORTRAIT_ASPECT_RATIO = 0.7f
        const val HSM_LANDSCAPE_ASPECT_RATIO = 1.4f
        /** Tolerance for the rounding of the main panel size to whole pixels. */
        const val ASPECT_RATIO_TOLERANCE = 0.01f

        /** Drag of the bottom-right resize affordance, in the main panel's local space. */
        val RESIZE_DELTA = Vector3(0.2f, -0.2f, 0f)

        /** Drag of the move affordance, in the main panel's local space. */
        val MOVE_DELTA = Vector3(0.25f, 0.16f, 0f)

        /**
         * Distance the main panel must travel for [MOVE_DELTA]. It is well below the length of
         * [MOVE_DELTA] (~0.3 m), because the distance in the activity space also depends on the
         * scale of the panel while it is dragged.
         */
        const val MIN_MOVE_DISTANCE_METERS = 0.1f

        /** Bound for the sky to change color, which takes well under a second on Aura. */
        val SCREENSHOT_TIMEOUT = 10.seconds
        val SCREENSHOT_POLL_INTERVAL = 200.milliseconds

        /** Width that screenshots are downscaled to before their colors are sampled. */
        const val SAMPLE_WIDTH = 32

        /**
         * A pixel is blue when its hue is in [BLUE_HUE_RANGE] and it is saturated and bright enough
         * to rule out white, grey, and black.
         */
        val BLUE_HUE_RANGE = 190f..250f
        const val MIN_BLUE_SATURATION = 0.25f
        const val MIN_BLUE_BRIGHTNESS = 0.25f

        /**
         * Fraction of blue pixels for the sky to count as blue. Narrow-FOV headsets such as Aura
         * show at least ~0.3 with the blue skybox behind the main panel.
         */
        const val MIN_BLUE_FRACTION = 0.25f

        /**
         * Fraction of blue pixels for the black void baseline to count as not blue. Checks against
         * the system environment, which may be passthrough of the real world, only require the sky
         * to drop below [MIN_BLUE_FRACTION].
         */
        const val MAX_NOT_BLUE_FRACTION = 0.1f
    }
}
