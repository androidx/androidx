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

/**
 * Shared SceneCore integration test helpers built on [XrSessionRule], [Scene], and SceneCore
 * entities.
 *
 * * [requestFullSpace] and [requestHomeSpace]: Switch space modes via [Scene.requestFullSpace] and
 *   [Scene.requestHomeSpace], waiting for [SpatialCapability.SPATIAL_UI] and [Scene.activitySpace]
 *   origin updates to settle.
 * * [waitForSpatialEnvironmentSettled]: Waits for pending [SpatialEnvironment] skybox and
 *   passthrough opacity changes to take effect.
 * * [finishActivitiesOtherThan], [MAIN_PANEL_NODE_NAME], [loadNodesBeforeTeardown], and
 *   [renderSolidColorFrame]: Consolidate embedded activity cleanup, compositor node lookup, glTF
 *   teardown stabilization, and surface canvas rendering across SceneCore test suites.
 *
 * **Layering boundary**: These SceneCore-specific helpers must live here instead of in
 * [XrSessionRule] so `:xr:testutils` depends only on `:xr:runtime:runtime` (for
 * [androidx.xr.runtime.Session]).
 */
import android.app.Activity
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Task-scoped compositor node name backing [Scene.mainPanelEntity]. */
internal const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

/** Timeout applied when waiting for a space mode transition to be observed. */
internal val SPACE_MODE_TIMEOUT = 5.seconds

/** Timeout applied when waiting for environment or passthrough callbacks. */
private val ENVIRONMENT_CALLBACK_TIMEOUT = 5.seconds

/** Tolerance for passthrough opacity comparisons (snapped within 0.01f by SceneCore). */
private const val PASSTHROUGH_OPACITY_TOLERANCE = 0.01f

/**
 * Requests Full Space Mode via [Scene.requestFullSpace] and blocks the instrumentation thread
 * serially until [SpatialCapability.SPATIAL_UI] is active and [Scene.activitySpace] has received
 * its initial origin update.
 */
internal fun XrSessionRule<*, *>.requestFullSpace() {
    requestSpaceMode(FullSpaceMode)
}

/**
 * Requests Home Space Mode via [Scene.requestHomeSpace] and blocks the instrumentation thread
 * serially until [SpatialCapability.SPATIAL_UI] is removed from [Scene.spatialCapabilities].
 */
internal fun XrSessionRule<*, *>.requestHomeSpace() {
    requestSpaceMode(HomeSpaceMode)
}

private fun XrSessionRule<*, *>.requestSpaceMode(spaceMode: SpaceMode) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "requestSpaceMode must be called from the instrumentation thread, not the main thread."
    }
    if (!spaceMode.isSpaceModeActive(session)) {
        spaceMode.requestSpaceMode(session)
    }
}

private sealed class SpaceMode(val name: String) {
    protected fun waitForSpaceModeActive(session: Session) {
        waitUntil(
            description = "${name} Space Mode to become active",
            timeout = SPACE_MODE_TIMEOUT,
        ) {
            isSpaceModeActive(session)
        }
    }

    abstract fun requestSpaceMode(session: Session)

    abstract fun isSpaceModeActive(session: Session): Boolean
}

private object HomeSpaceMode : SpaceMode("Home") {
    override fun requestSpaceMode(session: Session) {
        session.scene.requestHomeSpace()
        waitForSpaceModeActive(session)
    }

    override fun isSpaceModeActive(session: Session): Boolean {
        return !session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)
    }
}

private object FullSpaceMode : SpaceMode("Full") {
    override fun requestSpaceMode(session: Session) {
        val originSettled = CountDownLatch(1)
        val originListener = Runnable { originSettled.countDown() }
        try {
            session.scene.activitySpace.addOriginChangedListener(originListener)
            session.scene.requestFullSpace()
            waitForSpaceModeActive(session)
            originSettled.await(2, TimeUnit.SECONDS)
        } finally {
            session.scene.activitySpace.removeOriginChangedListener(originListener)
        }
    }

    override fun isSpaceModeActive(session: Session): Boolean {
        return session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)
    }
}

/**
 * Blocks the instrumentation thread serially until pending [SpatialEnvironment] and passthrough
 * opacity preferences have taken effect:
 * 1. If [SpatialCapability.APP_ENVIRONMENT] is present, waits until
 *    [SpatialEnvironment.isPreferredSpatialEnvironmentActive] matches whether a non-null
 *    [SpatialEnvironment.preferredSpatialEnvironment] is configured.
 * 2. If [SpatialCapability.PASSTHROUGH_CONTROL] is present and a concrete
 *    [SpatialEnvironment.preferredPassthroughOpacity] is set, waits until
 *    [SpatialEnvironment.currentPassthroughOpacity] reaches that target.
 *
 * Call this explicitly before [XrSessionRule.captureToBitmap] when testing async skybox or
 * passthrough changes.
 */
internal fun XrSessionRule<*, *>.waitForSpatialEnvironmentSettled() {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "waitForSpatialEnvironmentSettled must be called from the instrumentation thread, " +
            "not the main thread."
    }
    waitUntil(
        description = "environment and passthrough state for ${activity.packageName} to settle",
        timeout = ENVIRONMENT_CALLBACK_TIMEOUT,
    ) {
        isEnvironmentAndPassthroughSettled()
    }
}

private fun XrSessionRule<*, *>.isEnvironmentAndPassthroughSettled(): Boolean {
    val activeScene = session.scene
    val capabilities = activeScene.spatialCapabilities
    val env = activeScene.spatialEnvironment
    if (
        capabilities.contains(SpatialCapability.APP_ENVIRONMENT) &&
            env.isPreferredSpatialEnvironmentActive != (env.preferredSpatialEnvironment != null)
    ) {
        return false
    }
    val targetOpacity = env.preferredPassthroughOpacity
    return !capabilities.contains(SpatialCapability.PASSTHROUGH_CONTROL) ||
        targetOpacity == SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE ||
        abs(env.currentPassthroughOpacity - targetOpacity) <= PASSTHROUGH_OPACITY_TOLERANCE
}

/**
 * Finishes all non-destroyed process activities other than [host] and blocks until they reach
 * [Stage.DESTROYED].
 *
 * * Prevents [androidx.xr.scenecore.ActivityPanelEntity] child activities from outliving [host]
 *   during [XrSessionRule] teardown and crashing SystemUI (b/584286729, b/584288042).
 * * Must be called from the instrumentation thread.
 *
 * @param host host [Activity] kept alive while finishing embedded child activities
 * @param timeout maximum duration to wait for child activities to reach [Stage.DESTROYED]
 */
internal fun XrSessionRule<*, *>.finishActivitiesOtherThan(
    host: Activity = activity,
    timeout: Duration = SPACE_MODE_TIMEOUT,
) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "finishActivitiesOtherThan must be called from the instrumentation thread, " +
            "not the main thread."
    }
    val monitor = ActivityLifecycleMonitorRegistry.getInstance()
    val nonDestroyedStages = Stage.entries.filter { it != Stage.DESTROYED }
    fun aliveNonHostActivities(): List<Activity> =
        nonDestroyedStages
            .flatMap { stage -> monitor.getActivitiesInStage(stage) }
            .filter { candidate -> candidate !== host && !candidate.isDestroyed }

    val hadActivitiesToWaitFor = onMainThread {
        val candidates = aliveNonHostActivities()
        for (candidate in candidates) {
            if (!candidate.isFinishing) {
                candidate.finish()
            }
        }
        candidates.isNotEmpty()
    }
    if (hadActivitiesToWaitFor) {
        waitUntil(
            description =
                "embedded activities other than ${host::class.java.simpleName} to be destroyed",
            timeout = timeout,
        ) {
            onMainThread { aliveNonHostActivities().isEmpty() }
        }
    }
}

/**
 * Populates [GltfModelEntity.nodes] on the main thread while the renderer is still active.
 *
 * * Works around b/584771482, where `Session` destroys its rendering runtime before disposing
 *   entities and `GltfFeatureImpl.dispose()` otherwise initializes the lazy `nodes` list against a
 *   torn-down renderer (`IllegalArgumentException: Illegal Capacity: -1`).
 * * Must be called on the main thread, such as inside [XrSessionRule.onMainThread].
 */
@MainThread
internal fun GltfModelEntity.loadNodesBeforeTeardown() {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "loadNodesBeforeTeardown must be called on the main thread."
    }
    // TODO(b/584771482): Remove once entities are disposed before the rendering runtime is
    // destroyed.
    assertThat(nodes).isNotEmpty()
}

/**
 * Draws a single solid [color] frame into this [SurfaceEntity]'s backing [android.view.Surface].
 *
 * * Applies [dimensions] via [SurfaceEntity.setSurfacePixelDimensions] when non-null and acquires
 *   [SurfaceEntity.getSurface] on the main thread, then fills the hardware canvas with [color].
 * * Safe to call from either the instrumentation thread or the main thread.
 *
 * @param color ARGB color value drawn across the surface canvas
 * @param dimensions buffer dimensions in pixels applied before drawing, or `null` to keep current
 *   dimensions
 */
@OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
internal fun SurfaceEntity.renderSolidColorFrame(
    @ColorInt color: Int,
    dimensions: IntSize2d? = IntSize2d(256, 256),
) {
    val surface =
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (dimensions != null) {
                setSurfacePixelDimensions(dimensions)
            }
            getSurface()
        } else {
            runBlocking(Dispatchers.Main.immediate) {
                if (dimensions != null) {
                    setSurfacePixelDimensions(dimensions)
                }
                getSurface()
            }
        }
    assertWithMessage("Expected active Surface from SurfaceEntity.getSurface()")
        .that(surface.isValid)
        .isTrue()
    val canvas = surface.lockHardwareCanvas()
    try {
        canvas.drawColor(color)
    } finally {
        surface.unlockCanvasAndPost(canvas)
    }
}
