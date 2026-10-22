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

package androidx.xr.arcore.integration.tests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.AugmentedImage
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntryMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [AugmentedImage] tracking and [AugmentedImageDatabase]
 * configuration.
 *
 * Target-detection tests use prebuilt asset `images/earth.jpg` (899x900 px). When physical size
 * estimation is unsupported, tests configure and expect a 0.20 m wide physical fixture. When
 * estimation is supported, tests configure width `0f` to estimate any positive physical width.
 */
// TODO(b/573219883): Once a mechanism for injecting synthetic augmented images or a printed test
// fixture is available, migrate target-detection assertions to use a deterministic fixture instead
// of timing out and skipping when no physical target is in the camera view.
@RunWith(AndroidJUnit4::class)
@LargeTest
class AugmentedImageTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions("android.permission.SCENE_UNDERSTANDING_COARSE")
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private val allocatedBitmaps = mutableListOf<Bitmap>()

    @After
    fun tearDown() {
        try {
            for (bitmap in allocatedBitmaps) {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } finally {
            allocatedBitmaps.clear()
        }
    }

    private fun assumeAugmentedImageSupported(): PerceptionManager {
        val perceptionManager = sessionRule.session.requirePerceptionManager()
        assumeTrue(
            "Skipping test: AugmentedImage tracking is not supported on this device",
            perceptionManager.imageDatabaseMaxLoadedImageCount > 0,
        )
        return perceptionManager
    }

    private fun assumePhysicalSizeEstimationSupported(perceptionManager: PerceptionManager) {
        assumeTrue(
            "Skipping test: Device does not support physical size estimation for AugmentedImage",
            perceptionManager.isPhysicalSizeEstimationSupported,
        )
    }

    private fun assumePhysicalSizeEstimationUnsupported(perceptionManager: PerceptionManager) {
        assumeFalse(
            "Skipping test: Device supports physical size estimation for AugmentedImage",
            perceptionManager.isPhysicalSizeEstimationSupported,
        )
    }

    private fun loadTargetBitmap(): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .assets
                .open(EARTH_IMAGE_ASSET)
                .use { inputStream ->
                    checkNotNull(
                        BitmapFactory.decodeStream(inputStream, /* outPadding= */ null, options)
                    ) {
                        "Failed to decode asset bitmap: $EARTH_IMAGE_ASSET"
                    }
                }
        allocatedBitmaps.add(bitmap)
        return bitmap
    }

    private fun createImageDatabaseConfig(
        bitmap: Bitmap,
        widthInMeters: Float = TARGET_WIDTH_METERS,
    ): Config {
        val database = AugmentedImageDatabase()
        database.addAugmentedImageDatabaseEntry(
            AugmentedImageDatabaseEntryMode.DYNAMIC,
            bitmap,
            widthInMeters,
        )
        return Config.Builder().setAugmentedImageDatabase(database).build()
    }

    private fun awaitTrackedImageStateOrSkip(): AugmentedImage.State {
        val imagesFlow = AugmentedImage.subscribe(sessionRule.session)
        val detectedState = waitUntilFoundOrNull {
            imagesFlow.value.firstNotNullOfOrNull { image ->
                image.state.value.takeIf { it.trackingState == TrackingState.TRACKING }
            }
        }
        assumeTrue(
            "Skipping target assertions: No physical image target in camera view",
            detectedState != null,
        )
        return checkNotNull(detectedState)
    }

    @Test
    @XrDeviceTest
    fun configureAndSubscribe_withImageDatabase_succeedsAndSubscribes() {
        assumeAugmentedImageSupported()
        val config = createImageDatabaseConfig(bitmap = loadTargetBitmap())

        val configResult = sessionRule.session.configure(config)
        waitForPerceptionStateUpdate(sessionRule.session)
        val subscribedImages = AugmentedImage.subscribe(sessionRule.session).value

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        val configuredEntry =
            sessionRule.session.config.augmentedImageDatabase?.entries.orEmpty().single()
        assertThat(configuredEntry.widthInMeters).isWithin(FLOAT_TOLERANCE).of(TARGET_WIDTH_METERS)
        assertThat(configuredEntry.mode).isEqualTo(AugmentedImageDatabaseEntryMode.DYNAMIC)
        assertThat(subscribedImages).containsNoDuplicates()
        for (image in subscribedImages) {
            val state = image.state.value
            assertThat(state.owner).isEqualTo(image)
            assertThat(state.index).isEqualTo(0)
        }
    }

    @Test
    @XrDeviceTest
    fun subscribe_whenTargetDetectedWithoutSizeEstimation_emitsConfiguredExtents() {
        val perceptionManager = assumeAugmentedImageSupported()
        assumePhysicalSizeEstimationUnsupported(perceptionManager)
        val targetBitmap = loadTargetBitmap()
        val expectedHeightMeters = TARGET_WIDTH_METERS * targetBitmap.height / targetBitmap.width
        configureSessionAndWaitForPerception(
            sessionRule.session,
            createImageDatabaseConfig(bitmap = targetBitmap),
        )

        val state = awaitTrackedImageStateOrSkip()

        assertThat(state.index).isEqualTo(0)
        assertFinitePose(state.centerPose)
        assertThat(state.extents.width).isWithin(EXTENTS_TOLERANCE_METERS).of(TARGET_WIDTH_METERS)
        assertThat(state.extents.height).isWithin(EXTENTS_TOLERANCE_METERS).of(expectedHeightMeters)
    }

    @Test
    @XrDeviceTest
    fun subscribe_whenTargetDetectedWithSizeEstimation_emitsPositiveExtents() {
        val perceptionManager = assumeAugmentedImageSupported()
        assumePhysicalSizeEstimationSupported(perceptionManager)
        configureSessionAndWaitForPerception(
            sessionRule.session,
            createImageDatabaseConfig(bitmap = loadTargetBitmap(), widthInMeters = 0f),
        )

        val state = awaitTrackedImageStateOrSkip()

        assertThat(state.index).isEqualTo(0)
        assertFinitePose(state.centerPose)
        assertThat(state.extents.width).isGreaterThan(0f)
        assertThat(state.extents.height).isGreaterThan(0f)
    }

    @Test
    @XrDeviceTest
    fun subscribe_withoutDatabaseConfigured_throwsIllegalStateException() {
        check(sessionRule.session.config.augmentedImageDatabase == null)

        val exception =
            assertFailsWith<IllegalStateException> {
                AugmentedImage.subscribe(sessionRule.session)
            }

        assertThat(exception).hasMessageThat().contains("augmentedImageDatabase")
    }

    @Test
    @XrDeviceTest
    fun configure_withEmptyDatabase_throwsIllegalArgumentException() {
        assumeAugmentedImageSupported()
        val config = Config.Builder().setAugmentedImageDatabase(AugmentedImageDatabase()).build()

        val exception =
            assertFailsWith<IllegalArgumentException> { sessionRule.session.configure(config) }

        assertThat(exception).hasMessageThat().contains("image database")
    }

    @Test
    @XrDeviceTest
    fun configure_withZeroWidthWhenSizeEstimationUnsupported_throwsIllegalArgumentException() {
        val perceptionManager = assumeAugmentedImageSupported()
        assumePhysicalSizeEstimationUnsupported(perceptionManager)
        val config = createImageDatabaseConfig(bitmap = loadTargetBitmap(), widthInMeters = 0f)

        val exception =
            assertFailsWith<IllegalArgumentException> { sessionRule.session.configure(config) }

        assertThat(exception).hasMessageThat().contains("widthInMeters")
    }

    @Test
    @XrDeviceTest
    fun configure_withNegativeWidthWhenSizeEstimationUnsupported_throwsIllegalArgumentException() {
        val perceptionManager = assumeAugmentedImageSupported()
        assumePhysicalSizeEstimationUnsupported(perceptionManager)
        val config = createImageDatabaseConfig(bitmap = loadTargetBitmap(), widthInMeters = -0.1f)

        val exception =
            assertFailsWith<IllegalArgumentException> { sessionRule.session.configure(config) }

        assertThat(exception).hasMessageThat().contains("widthInMeters")
    }

    @Test
    @XrDeviceTest
    fun subscribe_afterReconfiguringNullDatabase_throwsIllegalStateException() {
        assumeAugmentedImageSupported()
        configureSessionAndWaitForPerception(
            sessionRule.session,
            createImageDatabaseConfig(bitmap = loadTargetBitmap()),
        )
        // Subscribe while enabled so reconfiguring null runs with an active subscription.
        AugmentedImage.subscribe(sessionRule.session)
        configureSessionAndWaitForPerception(
            sessionRule.session,
            Config.Builder().setAugmentedImageDatabase(null).build(),
        )

        val exception =
            assertFailsWith<IllegalStateException> {
                AugmentedImage.subscribe(sessionRule.session)
            }

        assertThat(exception).hasMessageThat().contains("augmentedImageDatabase")
    }

    private companion object {
        private const val TARGET_WIDTH_METERS = 0.2f
        private const val EARTH_IMAGE_ASSET = "images/earth.jpg"
    }
}
