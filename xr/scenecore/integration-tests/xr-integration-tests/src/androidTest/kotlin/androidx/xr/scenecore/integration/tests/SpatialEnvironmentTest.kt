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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialScene
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device integration tests for the SceneCore [SpatialEnvironment] APIs: app environment preference
 * (image-based lighting and glTF geometry), passthrough opacity, and Full Space / Home Space
 * transitions.
 *
 * Environment geometry attachment is verified through the SpaceFlinger scene graph (`dumpsys
 * spf_cpm`, parsed by [SpatialSceneHelper]): the attached geometry appears as an
 * [ENVIRONMENT_GEOMETRY_NODE] node under the [ENVIRONMENT_ROOT_NODE] node. The system environment
 * may use the same node name, so tests compare node ids rather than names.
 *
 * Skybox rendering is checked by sampling the colors of a screenshot region.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SpatialEnvironmentTest {

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_setAndClear_attachesGeometryAndUpdatesActiveState() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val env = session.scene.spatialEnvironment
            assertThat(session.scene.spatialCapabilities)
                .contains(SpatialCapability.APP_ENVIRONMENT)

            val blueIbl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))

            try {
                val systemGeometryNode = environmentGeometryNode(captureSpatialScene())

                env.setPreferredEnvironmentAndSettle(
                    SpatialEnvironmentPreference(blueIbl, geometry)
                )
                env.awaitPreferredEnvironmentActive(expected = true)
                val appGeometryNode =
                    awaitEnvironmentGeometryNode("app geometry to be attached") {
                        it != null && it.id != systemGeometryNode?.id
                    }

                // A null preference reverts to the system default environment.
                env.setPreferredEnvironmentAndSettle(null)
                env.awaitPreferredEnvironmentActive(expected = false)
                awaitEnvironmentGeometryNode("app geometry to be detached") {
                    it?.id != appGeometryNode?.id
                }
            } finally {
                env.setPreferredEnvironmentAndSettle(null)
                blueIbl.close()
                geometry.close()
            }
        }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_swapIblOrGeometry_keepsEnvironmentActive() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        val env = session.scene.spatialEnvironment
        assertThat(session.scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)

        val greyIbl =
            ImageBasedLightingAsset.createFromZip(
                session,
                Paths.get("skyboxes", "GreySkybox.zip"),
            )
        val blueIbl =
            ImageBasedLightingAsset.createFromZip(
                session,
                Paths.get("skyboxes", "BlueSkybox.zip"),
            )
        val blueBox = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
        val greenBox = GltfModel.create(session, Paths.get("models", "BoundingBoxGreen.glb"))

        try {
            val systemGeometryNode = environmentGeometryNode(captureSpatialScene())
            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(greyIbl, blueBox))
            env.awaitPreferredEnvironmentActive(expected = true)
            val blueBoxNode =
                awaitEnvironmentGeometryNode("blue box geometry to be attached") {
                    it != null && it.id != systemGeometryNode?.id
                }

            // Swapping or unsetting the IBL keeps the attached geometry. This is a no-op for the
            // scene graph, so the geometry node must stay the same over a settle window that
            // starts after the main thread has processed the new preference.
            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(blueIbl, blueBox))
            assertEnvironmentGeometryNodeUnchanged("geometry kept after swapping IBL", blueBoxNode)
            assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(null, blueBox))
            assertEnvironmentGeometryNodeUnchanged("geometry kept after unsetting IBL", blueBoxNode)
            assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

            // Swapping the geometry attaches a new geometry node.
            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(blueIbl, greenBox))
            assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
            val greenBoxNode =
                awaitEnvironmentGeometryNode("green box geometry to be attached") {
                    it != null && it.id != blueBoxNode?.id && it.id != systemGeometryNode?.id
                }

            // Unsetting the geometry detaches it while the IBL-only environment stays active.
            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(blueIbl, null))
            assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
            awaitEnvironmentGeometryNode("green box geometry to be detached") {
                it?.id != greenBoxNode?.id
            }
        } finally {
            env.setPreferredEnvironmentAndSettle(null)
            greyIbl.close()
            blueIbl.close()
            blueBox.close()
            greenBox.close()
        }
    }

    @Test
    @XrDeviceTest
    fun appEnvironment_blackVoidPreference_isDistinctFromSystemDefault() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val env = session.scene.spatialEnvironment
            assertThat(session.scene.spatialCapabilities)
                .contains(SpatialCapability.APP_ENVIRONMENT)

            try {
                // A preference whose fields are all null is a black void app environment, which
                // is distinct from a null preference (system default environment).
                val blackVoidPref = SpatialEnvironmentPreference(null, null)
                env.setPreferredEnvironmentAndSettle(blackVoidPref)
                assertThat(env.preferredSpatialEnvironment).isEqualTo(blackVoidPref)
                env.awaitPreferredEnvironmentActive(expected = true)

                env.setPreferredEnvironmentAndSettle(null)
                assertThat(env.preferredSpatialEnvironment).isNull()
                env.awaitPreferredEnvironmentActive(expected = false)
            } finally {
                env.setPreferredEnvironmentAndSettle(null)
            }
        }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_setBlueSkybox_turnsScreenBlue() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        val env = session.scene.spatialEnvironment
        assertThat(session.scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)

        val blueIbl =
            ImageBasedLightingAsset.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))

        try {
            session.scene.mainPanelEntity.setEnabled(false)
            env.preferredPassthroughOpacity = 0.0f

            // Start from a black void so that the baseline does not depend on the system
            // environment, which may itself contain blue.
            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(null, null))
            env.awaitPreferredEnvironmentActive(expected = true)
            pollScreenshot("sky hemisphere to not be blue", ::skyBlueFraction) {
                it <= MAX_NOT_BLUE_FRACTION
            }

            env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(blueIbl, null))
            pollScreenshot("sky hemisphere to turn blue", ::skyBlueFraction) {
                it >= MIN_BLUE_FRACTION
            }
        } finally {
            session.scene.mainPanelEntity.setEnabled(true)
            env.preferredPassthroughOpacity = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
            env.setPreferredEnvironmentAndSettle(null)
            blueIbl.close()
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    @SuppressLint("RestrictedApiAndroidX")
    fun geometryEntity_setAndClear_movesEntitySubspaceUnderEnvironmentRoot() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val env = session.scene.spatialEnvironment
            assertThat(session.scene.spatialCapabilities)
                .contains(SpatialCapability.APP_ENVIRONMENT)

            val model = GltfModel.create(session, Paths.get("models", "BoundingBoxYellow.glb"))
            val entity =
                GltfModelEntity.create(session, model, parent = session.scene.activitySpace)

            try {
                // The entity renders through its own subspace, which starts in the app task.
                val entitySubspace =
                    checkNotNull(
                        SpatialSceneHelper.awaitSpatialScene(
                            description = "entity subspace to appear in the app task",
                            timeout = SCENE_TIMEOUT,
                            pollInterval = SCENE_POLL_INTERVAL,
                            select = { scene ->
                                scene
                                    .findTaskByPackageName(activity.packageName)
                                    ?.subspaceNodes
                                    ?.firstOrNull()
                            },
                            until = { it != null },
                        )
                    )

                // The environment reuses the entity's subspace node as its geometry node.
                env.setPreferredEnvironmentAndSettle(
                    SpatialEnvironmentPreference(null, null, geometryEntity = entity)
                )
                env.awaitPreferredEnvironmentActive(expected = true)
                awaitEnvironmentGeometryNode("entity subspace to be attached") {
                    it?.id == entitySubspace.id
                }

                // TODO(b/568632541): Confirm whether clearing the preference should reparent the
                // entity subspace back to the entity node in the app task. Currently it only
                // detaches the environment root, so we only verify it leaves the environment root.
                env.setPreferredEnvironmentAndSettle(null)
                env.awaitPreferredEnvironmentActive(expected = false)
                awaitEnvironmentGeometryNode("entity subspace to be detached") {
                    it?.id != entitySubspace.id
                }
            } finally {
                env.setPreferredEnvironmentAndSettle(null)
                entity.parent = null
                model.close()
            }
        }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun passthroughOpacity_inFullSpaceMode_updatesCurrentOpacityAndNotifiesListeners() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val env = session.scene.spatialEnvironment
            assertThat(session.scene.spatialCapabilities)
                .contains(SpatialCapability.PASSTHROUGH_CONTROL)

            val ibl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            val removedListenerOpacities = CopyOnWriteArrayList<Float>()
            val removedListenerNotified = CompletableDeferred<Unit>()
            val removedListener =
                Consumer<Float> { opacity ->
                    removedListenerOpacities.add(opacity)
                    removedListenerNotified.complete(Unit)
                }

            try {
                // Semi-transparent passthrough is disabled when there is no preferred spatial
                // environment and no active system passthrough, so activate an app environment
                // before requesting a semi-transparent opacity.
                env.setPreferredEnvironmentAndSettle(SpatialEnvironmentPreference(ibl, geometry))
                env.awaitPreferredEnvironmentActive(expected = true)

                env.addPassthroughOpacityChangedListener(removedListener)

                val receivedOpacity = env.setPassthroughOpacityAndAwaitCallback(0.5f)
                assertThat(receivedOpacity).isWithin(OPACITY_TOLERANCE).of(0.5f)
                assertThat(env.currentPassthroughOpacity).isWithin(OPACITY_TOLERANCE).of(0.5f)
                withTimeout(CALLBACK_TIMEOUT) { removedListenerNotified.await() }

                // Removing a listener prevents further notifications. The callback awaited by
                // setPassthroughOpacityAndAwaitCallback acts as the control, and yielding lets any
                // other notification dispatched for the same change run before the check.
                env.removePassthroughOpacityChangedListener(removedListener)
                env.setPassthroughOpacityAndAwaitCallback(1.0f)
                yield()
                assertThat(removedListenerOpacities.none { abs(it - 1.0f) <= OPACITY_TOLERANCE })
                    .isTrue()
            } finally {
                env.removePassthroughOpacityChangedListener(removedListener)
                env.preferredPassthroughOpacity =
                    SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
                env.setPreferredEnvironmentAndSettle(null)
                ibl.close()
                geometry.close()
            }
        }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun spaceModeToggle_betweenFullSpaceAndHomeSpace_updatesCapabilitiesAndEnvironmentActiveState() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val env = session.scene.spatialEnvironment

            val blueIbl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            val targetPref = SpatialEnvironmentPreference(blueIbl, geometry)

            try {
                env.setPreferredEnvironmentAndSettle(targetPref)
                env.awaitPreferredEnvironmentActive(expected = true)
                assertThat(session.scene.activitySpace.bounds.width)
                    .isEqualTo(Float.POSITIVE_INFINITY)

                session.scene.requestHomeSpaceAndAwait()
                assertThat(session.scene.spatialCapabilities)
                    .doesNotContain(SpatialCapability.APP_ENVIRONMENT)
                assertThat(session.scene.activitySpace.bounds.width)
                    .isLessThan(Float.POSITIVE_INFINITY)
                // The environment visibility update may arrive in a separate spatial state update
                // from the capability change, so wait for it explicitly.
                env.awaitPreferredEnvironmentActive(expected = false)
                // The preference itself remains retained across HSM transitions
                assertThat(env.preferredSpatialEnvironment).isEqualTo(targetPref)

                session.scene.requestFullSpaceAndAwait()
                assertThat(session.scene.spatialCapabilities)
                    .contains(SpatialCapability.APP_ENVIRONMENT)
                assertThat(session.scene.activitySpace.bounds.width)
                    .isEqualTo(Float.POSITIVE_INFINITY)
                assertThat(env.preferredSpatialEnvironment).isEqualTo(targetPref)
                // The retained preference is re-applied once back in Full Space Mode
                env.awaitPreferredEnvironmentActive(expected = true)
            } finally {
                // Clear the preference before releasing the assets it still references.
                env.setPreferredEnvironmentAndSettle(null)
                blueIbl.close()
                geometry.close()
            }
        }

    /**
     * Sets [preference] and yields the main thread once, so that the geometry attachment coroutine
     * launched by the rendering runtime runs before the test continues or cleans up.
     */
    private suspend fun SpatialEnvironment.setPreferredEnvironmentAndSettle(
        preference: SpatialEnvironmentPreference?
    ) {
        preferredSpatialEnvironment = preference
        yield()
    }

    /**
     * Suspends until [SpatialEnvironment.isPreferredSpatialEnvironmentActive] equals [expected],
     * either immediately or via [SpatialEnvironment.addSpatialEnvironmentChangedListener].
     */
    private suspend fun SpatialEnvironment.awaitPreferredEnvironmentActive(expected: Boolean) {
        val reached = CompletableDeferred<Unit>()
        val listener =
            Consumer<Boolean> { isActive ->
                if (isActive == expected) reached.complete(Unit)
            }
        addSpatialEnvironmentChangedListener(listener)
        try {
            if (isPreferredSpatialEnvironmentActive == expected) return
            withTimeout(CALLBACK_TIMEOUT) { reached.await() }
        } finally {
            removeSpatialEnvironmentChangedListener(listener)
        }
    }

    /**
     * Sets [SpatialEnvironment.preferredPassthroughOpacity] to [target] and suspends until a
     * passthrough opacity listener reports a value matching [target].
     *
     * @return the opacity reported by the listener.
     */
    private suspend fun SpatialEnvironment.setPassthroughOpacityAndAwaitCallback(
        target: Float
    ): Float {
        val received = CompletableDeferred<Float>()
        val listener =
            Consumer<Float> { opacity ->
                if (abs(opacity - target) <= OPACITY_TOLERANCE) received.complete(opacity)
            }
        addPassthroughOpacityChangedListener(listener)
        try {
            preferredPassthroughOpacity = target
            return withTimeout(CALLBACK_TIMEOUT) { received.await() }
        } finally {
            removePassthroughOpacityChangedListener(listener)
        }
    }

    /** Captures the scene graph off the main thread, since the capture blocks while polling. */
    private suspend fun captureSpatialScene(): SpatialScene =
        withContext(Dispatchers.IO) { SpatialSceneHelper.captureSpatialScene() }

    /** Returns the geometry node currently attached under the environment root, if any. */
    private fun environmentGeometryNode(scene: SpatialScene): SpatialNode? =
        scene.rootNodes
            .firstNotNullOfOrNull { root -> root.findNode { it.name == ENVIRONMENT_ROOT_NODE } }
            ?.findDescendant { it.name == ENVIRONMENT_GEOMETRY_NODE }

    private suspend fun awaitEnvironmentGeometryNode(
        description: String,
        until: (SpatialNode?) -> Boolean,
    ): SpatialNode? =
        SpatialSceneHelper.awaitSpatialScene(
            description = description,
            timeout = SCENE_TIMEOUT,
            pollInterval = SCENE_POLL_INTERVAL,
            select = ::environmentGeometryNode,
            until = until,
        )

    /**
     * Asserts that the environment geometry node keeps the id of [expected] throughout a settle
     * window. There is no scene graph change to anchor on, so the window starts after the main
     * Looper has drained, which submits any work queued by the preceding preference update.
     */
    private suspend fun assertEnvironmentGeometryNodeUnchanged(
        description: String,
        expected: SpatialNode?,
    ) {
        withContext(Dispatchers.IO) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        assertSpatialSceneInvariant(
            description,
            select = ::environmentGeometryNode,
            anchor = { true },
            invariant = { it?.id == expected?.id },
        )
    }

    /**
     * Polls until [anchor] holds, then asserts that [invariant] holds on every snapshot for at
     * least [SETTLE_POLLS] captures spanning [SETTLE_WINDOW].
     *
     * @throws AssertionError if [anchor] does not hold within [SCENE_TIMEOUT] or [invariant] is
     *   violated during the settle window.
     */
    private suspend fun <T> assertSpatialSceneInvariant(
        description: String,
        select: (SpatialScene) -> T,
        anchor: (T) -> Boolean,
        invariant: (T) -> Boolean,
    ) {
        var value =
            SpatialSceneHelper.awaitSpatialScene(
                description = "anchor for $description",
                timeout = SCENE_TIMEOUT,
                pollInterval = SCENE_POLL_INTERVAL,
                select = select,
                until = anchor,
            )
        val settleStart = TimeSource.Monotonic.markNow()
        var polls = 0
        while (true) {
            if (!invariant(value)) throw AssertionError("Violated $description, value: $value")
            if (++polls >= SETTLE_POLLS && settleStart.elapsedNow() >= SETTLE_WINDOW) return
            delay(SCENE_POLL_INTERVAL)
            value = select(captureSpatialScene())
        }
    }

    /**
     * Repeatedly takes screenshots until [until] holds for the value returned by [select]. Taking a
     * screenshot blocks, so the loop runs on [Dispatchers.IO], and [select] and [until] are also
     * evaluated off the main thread. The [Bitmap] passed to [select] is recycled after each call.
     *
     * @throws AssertionError if [until] does not hold within [SCREENSHOT_TIMEOUT], including when
     *   no screenshot could be taken.
     */
    private suspend fun <T> pollScreenshot(
        description: String,
        select: (Bitmap) -> T,
        until: (T) -> Boolean,
    ): T =
        withContext(Dispatchers.IO) {
            val start = TimeSource.Monotonic.markNow()
            var screenshotCount = 0
            var lastValue: T? = null
            while (true) {
                val screenshot =
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                if (screenshot != null) {
                    screenshotCount++
                    val value =
                        try {
                            select(screenshot)
                        } finally {
                            screenshot.recycle()
                        }
                    if (until(value)) return@withContext value
                    lastValue = value
                }
                if (start.elapsedNow() >= SCREENSHOT_TIMEOUT) {
                    break
                }
                delay(SCREENSHOT_POLL_INTERVAL)
            }
            throw AssertionError(
                if (screenshotCount == 0) {
                    "Timed out waiting for $description: takeScreenshot() kept returning null"
                } else {
                    "Timed out waiting for $description after $screenshotCount screenshots, " +
                        "last value: $lastValue"
                }
            )
        }

    /**
     * Returns the fraction of blue pixels in the sky hemisphere of [screenshot]. Both the top and
     * bottom halves are evaluated and the larger fraction is returned so the check is independent
     * of whether a physical headset is resting right-side up or upside down. The screenshot is
     * downscaled to [SAMPLE_WIDTH] pixels wide, keeping its aspect ratio, so that only a few pixels
     * need to be classified.
     */
    private fun skyBlueFraction(screenshot: Bitmap): Float {
        // Pixel access is not supported on hardware bitmaps.
        val source =
            if (screenshot.config == Bitmap.Config.HARDWARE) {
                checkNotNull(screenshot.copy(Bitmap.Config.ARGB_8888, false))
            } else {
                screenshot
            }
        try {
            val sampleHeight = (SAMPLE_WIDTH * source.height / source.width).coerceAtLeast(2)
            val sample = Bitmap.createScaledBitmap(source, SAMPLE_WIDTH, sampleHeight, true)
            val regionWidth = sample.width
            val halfHeight = (sample.height / 2).coerceAtLeast(1)
            val topPixels = IntArray(regionWidth * halfHeight)
            val bottomPixels = IntArray(regionWidth * (sample.height - halfHeight))
            try {
                sample.getPixels(topPixels, 0, regionWidth, 0, 0, regionWidth, halfHeight)
                sample.getPixels(
                    bottomPixels,
                    0,
                    regionWidth,
                    0,
                    halfHeight,
                    regionWidth,
                    sample.height - halfHeight,
                )
            } finally {
                if (sample !== source) {
                    sample.recycle()
                }
            }
            val hsv = FloatArray(3)
            fun IntArray.blueFraction(): Float =
                count { pixel ->
                    Color.colorToHSV(pixel, hsv)
                    hsv[0] in BLUE_HUE_RANGE &&
                        hsv[1] >= MIN_BLUE_SATURATION &&
                        hsv[2] >= MIN_BLUE_BRIGHTNESS
                }
                    .toFloat() / size

            return maxOf(topPixels.blueFraction(), bottomPixels.blueFraction())
        } finally {
            if (source !== screenshot) {
                source.recycle()
            }
        }
    }

    private companion object {
        val CALLBACK_TIMEOUT = 5.seconds
        const val OPACITY_TOLERANCE = 1e-4f

        val SCENE_TIMEOUT = 5.seconds
        val SCENE_POLL_INTERVAL = 100.milliseconds
        val SETTLE_WINDOW = 300.milliseconds
        const val SETTLE_POLLS = 3

        val SCREENSHOT_TIMEOUT = 8.seconds
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
         * Fractions of blue pixels for a screenshot hemisphere to count as blue or as not blue. Set
         * to 0.25f so narrow-FOV headsets (e.g. Aura with ±18° vertical FOV, where the horizon
         * cloud band of BlueSkybox.zip occupies ~70% of the sky hemisphere at identity head pose)
         * pass while keeping a wide margin above [MAX_NOT_BLUE_FRACTION].
         */
        const val MIN_BLUE_FRACTION = 0.25f
        const val MAX_NOT_BLUE_FRACTION = 0.1f

        /** Scene graph node that parents the active environment. */
        const val ENVIRONMENT_ROOT_NODE = "Environment Root"

        /** Name given to the environment geometry subspace by the rendering runtime. */
        const val ENVIRONMENT_GEOMETRY_NODE = "EnvironmentGeometryNode"
    }
}
