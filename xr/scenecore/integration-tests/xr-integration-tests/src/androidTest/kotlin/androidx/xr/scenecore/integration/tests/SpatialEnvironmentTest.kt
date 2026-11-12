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
import android.os.SystemClock
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
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.toPixelArray
import androidx.xr.testutils.toSoftwareBitmap
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device integration tests for the SceneCore [SpatialEnvironment] APIs: app environment preference
 * (image-based lighting and glTF geometry), passthrough opacity, and Full Space / Home Space
 * transitions.
 *
 * Environment state is asserted only through SceneCore APIs
 * ([SpatialEnvironment.preferredSpatialEnvironment],
 * [SpatialEnvironment.isPreferredSpatialEnvironmentActive] and its change listener). Skybox
 * rendering is checked by sampling the colors of a screenshot region.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SpatialEnvironmentTest {

    @get:Rule val sessionRule = createXrSessionRule()

    // TODO(b/583873060): Verify that the geometry is rendered while the preference is set and
    //  disappears after it is cleared.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_setAndClear_updatesPreferenceAndActiveState() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        val env = scene.spatialEnvironment

        val preference = sessionRule.onMainThread {
            val blueIbl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))

            SpatialEnvironmentPreference(blueIbl, geometry).also {
                env.preferredSpatialEnvironment = it
            }
        }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.preferredSpatialEnvironment).isEqualTo(preference)
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

        // A null preference reverts to the system default environment.
        sessionRule.onMainThread { env.preferredSpatialEnvironment = null }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.preferredSpatialEnvironment).isNull()
        assertThat(env.isPreferredSpatialEnvironmentActive).isFalse()
    }

    // TODO(b/583873060): Verify that swapping or unsetting the IBL keeps the rendered geometry,
    //  and that swapping or unsetting the geometry replaces or removes it.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_swapIblOrGeometry_keepsEnvironmentActive() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        val env = scene.spatialEnvironment

        val (blueIbl, blueBox, greenBox) =
            sessionRule.onMainThread {
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
                val greenBox =
                    GltfModel.create(session, Paths.get("models", "BoundingBoxGreen.glb"))

                env.preferredSpatialEnvironment = SpatialEnvironmentPreference(greyIbl, blueBox)
                Triple(blueIbl, blueBox, greenBox)
            }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

        // Record every active state change reported while the preference is swapped.
        val reportedActiveStates = CopyOnWriteArrayList<Boolean>()
        val listener = Consumer<Boolean> { reportedActiveStates.add(it) }
        env.addSpatialEnvironmentChangedListener(listener)

        // Swap the IBL, unset the IBL, swap the geometry, then unset the geometry. Each
        // preference still has an IBL or geometry, so the app environment stays active.
        for (preference in
            listOf(
                SpatialEnvironmentPreference(blueIbl, blueBox),
                SpatialEnvironmentPreference(null, blueBox),
                SpatialEnvironmentPreference(blueIbl, greenBox),
                SpatialEnvironmentPreference(blueIbl, null),
            )) {
            sessionRule.onMainThread { env.preferredSpatialEnvironment = preference }
            sessionRule.waitForSpatialEnvironmentSettled()
            assertThat(env.preferredSpatialEnvironment).isEqualTo(preference)
            assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
        }

        // waitForSpatialEnvironmentSettled() above is the arrival anchor for the last update.
        // Observe for a short settle window to confirm no late inactive notification is delivered.
        SystemClock.sleep(NO_CHANGE_SETTLE_WINDOW.inWholeMilliseconds)
        assertThat(reportedActiveStates).doesNotContain(false)
    }

    @Test
    @XrDeviceTest
    fun appEnvironment_blackVoidPreference_isDistinctFromSystemDefault() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        val env = scene.spatialEnvironment

        // A preference whose fields are all null is a black void app environment, which is
        // distinct from a null preference (system default environment).
        val blackVoidPref = SpatialEnvironmentPreference(null, null)
        sessionRule.onMainThread { env.preferredSpatialEnvironment = blackVoidPref }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.preferredSpatialEnvironment).isEqualTo(blackVoidPref)
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

        sessionRule.onMainThread { env.preferredSpatialEnvironment = null }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.preferredSpatialEnvironment).isNull()
        assertThat(env.isPreferredSpatialEnvironmentActive).isFalse()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun appEnvironment_setBlueSkybox_turnsScreenBlue() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        val env = scene.spatialEnvironment

        scene.mainPanelEntity.setEnabled(false)
        env.preferredPassthroughOpacity = 0.0f

        val blueIbl = sessionRule.onMainThread {
            val ibl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            // Start from a black void so that the baseline does not depend on the system
            // environment, which may itself contain blue.
            env.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, null)
            ibl
        }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
        pollScreenshot("sky hemisphere to not be blue", ::skyBlueFraction) {
            it <= MAX_NOT_BLUE_FRACTION
        }

        sessionRule.onMainThread {
            env.preferredSpatialEnvironment = SpatialEnvironmentPreference(blueIbl, null)
        }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
        pollScreenshot("sky hemisphere to turn blue", ::skyBlueFraction) {
            it >= MIN_BLUE_FRACTION
        }
    }

    // TODO(b/583873060): Verify that the entity is rendered as the environment geometry while the
    //  preference is set.
    // TODO(b/568632541): Confirm whether clearing the preference should render the entity in the
    //  app task again, and verify it once the expected behavior is settled.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    @SuppressLint("RestrictedApiAndroidX")
    fun geometryEntity_setAndClear_updatesPreferenceAndActiveState() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        val env = scene.spatialEnvironment

        val preference = sessionRule.onMainThread {
            val model = GltfModel.create(session, Paths.get("models", "BoundingBoxYellow.glb"))
            val entity =
                GltfModelEntity.create(session, model, parent = session.scene.activitySpace)

            SpatialEnvironmentPreference(null, null, geometryEntity = entity).also {
                env.preferredSpatialEnvironment = it
            }
        }
        sessionRule.waitForSpatialEnvironmentSettled()
        // SpatialEnvironmentPreference.equals() also compares the geometry entity.
        assertThat(env.preferredSpatialEnvironment).isEqualTo(preference)
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()

        sessionRule.onMainThread { env.preferredSpatialEnvironment = null }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.preferredSpatialEnvironment).isNull()
        assertThat(env.isPreferredSpatialEnvironmentActive).isFalse()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun passthroughOpacity_inFullSpaceMode_updatesCurrentOpacityAndNotifiesListeners() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.PASSTHROUGH_CONTROL)
        val env = scene.spatialEnvironment

        // Semi-transparent passthrough is disabled when there is no preferred spatial environment
        // and no active system passthrough, so activate an app environment before requesting a
        // semi-transparent opacity.
        sessionRule.onMainThread {
            val ibl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            env.preferredSpatialEnvironment = SpatialEnvironmentPreference(ibl, geometry)
        }
        sessionRule.waitForSpatialEnvironmentSettled()

        val removedListenerOpacities = CopyOnWriteArrayList<Float>()
        val removedListenerNotified = CountDownLatch(1)
        val removedListener =
            Consumer<Float> { opacity ->
                removedListenerOpacities.add(opacity)
                removedListenerNotified.countDown()
            }

        val halfOpacityReceived = CountDownLatch(1)
        val receivedHalfOpacity = AtomicReference<Float?>(null)
        val halfOpacityListener =
            Consumer<Float> { opacity ->
                if (abs(opacity - 0.5f) <= OPACITY_TOLERANCE) {
                    receivedHalfOpacity.set(opacity)
                    halfOpacityReceived.countDown()
                }
            }

        env.addPassthroughOpacityChangedListener(removedListener)
        env.addPassthroughOpacityChangedListener(halfOpacityListener)
        env.preferredPassthroughOpacity = 0.5f
        sessionRule.waitForSpatialEnvironmentSettled()

        assertThat(
                halfOpacityReceived.await(
                    CALLBACK_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isTrue()
        assertThat(receivedHalfOpacity.get()).isWithin(OPACITY_TOLERANCE).of(0.5f)
        assertThat(env.currentPassthroughOpacity).isWithin(OPACITY_TOLERANCE).of(0.5f)
        assertThat(
                removedListenerNotified.await(
                    CALLBACK_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isTrue()

        // Removing a listener prevents further notifications. The callback for 1.0f acts as the
        // arrival anchor; the settle window then lets any late notification run before the check.
        env.removePassthroughOpacityChangedListener(halfOpacityListener)
        env.removePassthroughOpacityChangedListener(removedListener)

        val fullOpacityReceived = CountDownLatch(1)
        val fullOpacityListener =
            Consumer<Float> { opacity ->
                if (abs(opacity - 1.0f) <= OPACITY_TOLERANCE) {
                    fullOpacityReceived.countDown()
                }
            }
        env.addPassthroughOpacityChangedListener(fullOpacityListener)
        env.preferredPassthroughOpacity = 1.0f
        waitUntil(
            description = "currentPassthroughOpacity to reach 1.0f",
            timeout = CALLBACK_TIMEOUT,
        ) {
            abs(env.currentPassthroughOpacity - 1.0f) <= OPACITY_TOLERANCE
        }
        assertThat(
                fullOpacityReceived.await(
                    CALLBACK_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isTrue()

        SystemClock.sleep(NO_CHANGE_SETTLE_WINDOW.inWholeMilliseconds)
        assertThat(removedListenerOpacities.none { abs(it - 1.0f) <= OPACITY_TOLERANCE }).isTrue()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun spaceModeToggle_betweenFullSpaceAndHomeSpace_updatesCapabilitiesAndEnvironmentActiveState() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        val env = scene.spatialEnvironment

        val targetPref = sessionRule.onMainThread {
            val blueIbl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val geometry = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            SpatialEnvironmentPreference(blueIbl, geometry).also {
                env.preferredSpatialEnvironment = it
            }
        }
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
        assertThat(scene.activitySpace.bounds.width).isEqualTo(Float.POSITIVE_INFINITY)

        sessionRule.requestHomeSpace()
        assertThat(scene.spatialCapabilities).doesNotContain(SpatialCapability.APP_ENVIRONMENT)
        assertThat(scene.activitySpace.bounds.width).isLessThan(Float.POSITIVE_INFINITY)
        // The environment visibility update may arrive in a separate spatial state update from
        // the capability change, so wait for it explicitly.
        waitUntil(
            description = "preferred spatial environment to become inactive in Home Space Mode",
            timeout = CALLBACK_TIMEOUT,
        ) {
            !env.isPreferredSpatialEnvironmentActive
        }
        // The preference itself remains retained across HSM transitions.
        assertThat(env.preferredSpatialEnvironment).isEqualTo(targetPref)

        sessionRule.requestFullSpace()
        assertThat(scene.spatialCapabilities).contains(SpatialCapability.APP_ENVIRONMENT)
        assertThat(scene.activitySpace.bounds.width).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(env.preferredSpatialEnvironment).isEqualTo(targetPref)
        // The retained preference is re-applied once back in Full Space Mode.
        sessionRule.waitForSpatialEnvironmentSettled()
        assertThat(env.isPreferredSpatialEnvironmentActive).isTrue()
    }

    /**
     * Repeatedly takes screenshots until [until] holds for the value returned by [select], using
     * [waitUntil]. Taking a screenshot blocks, so this must be called from the instrumentation
     * thread, where [select] and [until] are also evaluated. The [Bitmap] passed to [select] is
     * recycled after each call.
     *
     * @throws AssertionError if [until] does not hold within [SCREENSHOT_TIMEOUT], including when
     *   no screenshot could be taken.
     */
    private fun <T> pollScreenshot(
        description: String,
        select: (Bitmap) -> T,
        until: (T) -> Boolean,
    ): T {
        var screenshotCount = 0
        var lastValue: T? = null
        var matched = false
        try {
            waitUntil(description, SCREENSHOT_TIMEOUT, SCREENSHOT_POLL_INTERVAL) {
                val screenshot =
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                        ?: return@waitUntil false
                screenshotCount++
                val value =
                    try {
                        select(screenshot)
                    } finally {
                        screenshot.recycle()
                    }
                lastValue = value
                until(value).also { matched = it }
            }
        } catch (e: AssertionError) {
            throw AssertionError(
                if (screenshotCount == 0) {
                    "${e.message} takeScreenshot() kept returning null."
                } else {
                    "${e.message} Took $screenshotCount screenshots, last value: $lastValue"
                },
                e,
            )
        }
        check(matched)
        @Suppress("UNCHECKED_CAST")
        return lastValue as T
    }

    /**
     * Returns the fraction of blue pixels in the sky hemisphere of [screenshot]. Both the top and
     * bottom halves are evaluated and the larger fraction is returned so the check is independent
     * of whether a physical headset is resting right-side up or upside down. The screenshot is
     * downscaled to [SAMPLE_WIDTH] pixels wide, keeping its aspect ratio, so that only a few pixels
     * need to be classified.
     */
    private fun skyBlueFraction(screenshot: Bitmap): Float {
        val source = toSoftwareBitmap(screenshot)
        try {
            val sampleHeight = (SAMPLE_WIDTH * source.height / source.width).coerceAtLeast(2)
            val sample = Bitmap.createScaledBitmap(source, SAMPLE_WIDTH, sampleHeight, true)
            val regionWidth = sample.width
            val halfHeight = (sample.height / 2).coerceAtLeast(1)
            val splitIndex = halfHeight * regionWidth
            val pixels =
                try {
                    toPixelArray(sample)
                } finally {
                    if (sample !== source) {
                        sample.recycle()
                    }
                }
            val hsv = FloatArray(3)
            fun blueFraction(startIndex: Int, endExclusive: Int): Float {
                var blueCount = 0
                for (i in startIndex until endExclusive) {
                    Color.colorToHSV(pixels[i], hsv)
                    if (
                        hsv[0] in BLUE_HUE_RANGE &&
                            hsv[1] >= MIN_BLUE_SATURATION &&
                            hsv[2] >= MIN_BLUE_BRIGHTNESS
                    ) {
                        blueCount++
                    }
                }
                return blueCount.toFloat() / (endExclusive - startIndex)
            }

            return maxOf(blueFraction(0, splitIndex), blueFraction(splitIndex, pixels.size))
        } finally {
            if (source !== screenshot) {
                source.recycle()
            }
        }
    }

    private companion object {
        val CALLBACK_TIMEOUT = 5.seconds
        const val OPACITY_TOLERANCE = 1e-4f

        /**
         * How long to keep observing after an arrival anchor before asserting that no further
         * notification was delivered.
         */
        val NO_CHANGE_SETTLE_WINDOW = 300.milliseconds

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
    }
}
