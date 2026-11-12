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
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val HEAD_TRACKING_PERMISSION = "android.permission.HEAD_TRACKING"

internal const val MEDIUM_DISTANCE_M = 1.5f
internal const val SURFACE_SPHERE_RADIUS_M = 1.0f

/** Relative tolerance (4%) absorbing integer pixel rounding across device display densities. */
internal const val RATIO_TOLERANCE = 0.04f

private const val FOV_SPAN_EPSILON = 1e-3f

internal val CALLBACK_TIMEOUT = 5.seconds

internal val INITIAL_PANEL_SIZE_M = FloatSize2d(1.0f, 0.75f)
internal val SURFACE_QUAD_EXTENTS_M = FloatSize2d(1.0f, 1.0f)

internal data class EntityDistanceAndResolution(
    val distanceMeters: Float,
    val resolution: IntSize2d,
)

internal fun configureSpatialDeviceTracking(session: Session) {
    val result =
        session.configure(
            Config.Builder(session.config).setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
        )
    assertThat(result).isInstanceOf(SessionConfigureSuccess::class.java)
}

/** Returns the left-eye [RenderViewpoint], or the mono one on devices without stereo rendering. */
internal fun stereoOrMonoRenderViewpoint(session: Session): RenderViewpoint =
    try {
        RenderViewpoint.left(session)
    } catch (_: IllegalStateException) {
        RenderViewpoint.mono(session)
    }

internal fun XrSessionRule<*, *>.configureSpatialTrackingAndAwaitRenderViewpoint():
    RenderViewpoint {
    configureSpatialDeviceTracking(session)
    val renderViewpoint = stereoOrMonoRenderViewpoint(session)
    waitUntil(
        description = "non-zero FieldOfView on RenderViewpoint",
        timeout = CALLBACK_TIMEOUT,
    ) {
        renderViewpoint.state.value.hasNonZeroFieldOfView()
    }
    return renderViewpoint
}

private fun RenderViewpoint.State.hasNonZeroFieldOfView(): Boolean {
    val horizontalSpan = abs(tan(fieldOfView.angleLeft)) + abs(tan(fieldOfView.angleRight))
    val verticalSpan = abs(tan(fieldOfView.angleDown)) + abs(tan(fieldOfView.angleUp))
    return horizontalSpan > FOV_SPAN_EPSILON && verticalSpan > FOV_SPAN_EPSILON
}

internal fun cameraPoseInActivitySpace(session: Session, renderViewpoint: RenderViewpoint): Pose =
    session.scene.perceptionSpace
        .getScenePoseFromPerceptionPose(renderViewpoint.state.value.pose)
        .poseInActivitySpace

internal fun poseInFrontOfCamera(
    session: Session,
    renderViewpoint: RenderViewpoint,
    distanceMeters: Float,
): Pose =
    cameraPoseInActivitySpace(session, renderViewpoint)
        .compose(Pose(Vector3(0f, 0f, -distanceMeters)))

internal fun distanceToCamera(
    session: Session,
    renderViewpoint: RenderViewpoint,
    entity: Entity,
): Float =
    Vector3.distance(
        cameraPoseInActivitySpace(session, renderViewpoint).translation,
        entity.getPose(Space.ACTIVITY).translation,
    )

internal fun PerceivedResolutionResult.assertSuccess(): IntSize2d {
    assertThat(this).isInstanceOf(PerceivedResolutionResult.Success::class.java)
    val size = (this as PerceivedResolutionResult.Success).perceivedResolution
    assertThat(size.width).isGreaterThan(0)
    assertThat(size.height).isGreaterThan(0)
    return size
}

/**
 * Asserts that both [IntSize2d.width] and [IntSize2d.height] of [actual] scale relative to [base]
 * by [expectedRatio] within [toleranceFraction].
 */
internal fun assertScaledBy(
    actual: IntSize2d,
    base: IntSize2d,
    expectedRatio: Float,
    toleranceFraction: Float = RATIO_TOLERANCE,
) {
    assertScaledByAxes(
        actual = actual,
        base = base,
        expectedWidthRatio = expectedRatio,
        expectedHeightRatio = expectedRatio,
        toleranceFraction = toleranceFraction,
    )
}

/**
 * Asserts that [actual] scales relative to [base] by [expectedWidthRatio] horizontally and
 * [expectedHeightRatio] vertically within [toleranceFraction].
 */
internal fun assertScaledByAxes(
    actual: IntSize2d,
    base: IntSize2d,
    expectedWidthRatio: Float,
    expectedHeightRatio: Float,
    toleranceFraction: Float = RATIO_TOLERANCE,
) {
    assertWithMessage("width ratio ($actual vs $base)")
        .that(actual.width.toFloat() / base.width)
        .isWithin(expectedWidthRatio * toleranceFraction)
        .of(expectedWidthRatio)
    assertWithMessage("height ratio ($actual vs $base)")
        .that(actual.height.toFloat() / base.height)
        .isWithin(expectedHeightRatio * toleranceFraction)
        .of(expectedHeightRatio)
}

/**
 * Creates a white [PanelEntity] parented to the activity space. Creating the [View] and calling
 * [PanelEntity.create] both need the main thread.
 */
internal fun XrSessionRule<*, *>.createWhitePanelEntity(
    name: String,
    pose: Pose,
    dimensions: FloatSize2d = INITIAL_PANEL_SIZE_M,
): PanelEntity = onMainThread {
    PanelEntity.create(
        session = session,
        view = View(activity).apply { setBackgroundColor(Color.WHITE) },
        dimensions = dimensions,
        name = name,
        pose = pose,
        parent = session.scene.activitySpace,
    )
}

/**
 * Creates a Quad [SurfaceEntity] [MEDIUM_DISTANCE_M] in front of the camera. [SurfaceEntity.create]
 * is `@MainThread`.
 */
internal fun XrSessionRule<*, *>.createQuadSurfaceEntity(
    renderViewpoint: RenderViewpoint
): SurfaceEntity {
    val pose = poseInFrontOfCamera(session, renderViewpoint, MEDIUM_DISTANCE_M)
    return onMainThread {
        SurfaceEntity.create(
            session = session,
            pose = pose,
            shape = SurfaceEntity.Shape.Quad(SURFACE_QUAD_EXTENTS_M),
            stereoMode = SurfaceEntity.StereoMode.MONO,
            parent = session.scene.activitySpace,
        )
    }
}

/**
 * Records every [IntSize2d] delivered to `MainPanelEntity`'s perceived-resolution listener. The
 * listener runs on the main thread; [CopyOnWriteArrayList.add] is its last action, so the
 * instrumentation thread can read [eventsSince] safely.
 */
internal class PerceivedResolutionRecorder : Consumer<IntSize2d> {
    private val events = CopyOnWriteArrayList<IntSize2d>()
    private val waiters = CopyOnWriteArrayList<CountDownLatch>()

    override fun accept(resolution: IntSize2d) {
        events.add(resolution)
        waiters.forEach { it.countDown() }
    }

    fun mark(): Int = events.size

    fun eventsSince(mark: Int): List<IntSize2d> = events.drop(mark)

    fun awaitEventSince(mark: Int, timeout: Duration): Boolean {
        if (events.size > mark) {
            return true
        }
        val latch = CountDownLatch(1)
        waiters.add(latch)
        try {
            if (events.size > mark) {
                return true
            }
            return latch.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } finally {
            waiters.remove(latch)
        }
    }

    fun awaitResolution(
        description: String,
        timeout: Duration = CALLBACK_TIMEOUT,
        sinceMark: Int = 0,
        predicate: (IntSize2d) -> Boolean,
    ): IntSize2d {
        var matched: IntSize2d? = null
        try {
            waitUntil(description = description, timeout = timeout) {
                val candidate = eventsSince(sinceMark).lastOrNull() ?: return@waitUntil false
                if (predicate(candidate)) {
                    matched = candidate
                    true
                } else {
                    false
                }
            }
        } catch (e: AssertionError) {
            throw AssertionError("${e.message} (received events: $events)", e)
        }
        return checkNotNull(matched)
    }
}
