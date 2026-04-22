/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntryMode
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import com.google.common.truth.Truth
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.arcore.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrRuntimeTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var underTest: OpenXrRuntime

    @Test
    fun getPreferredBlendMode_returnsBlendMode() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        underTest.resume()
        // Result comes from `kBlendModes` defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(underTest.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.ADDITIVE)
    }

    @Test
    fun isSupported_geospatialSpatial_returnsTrue() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        underTest.resume()
        // Result comes from //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(underTest.isSupported(GeospatialMode.SPATIAL)).isTrue()
    }

    @Test
    fun initialize_initializesNativeOpenXrManager() = initOpenXrRuntimeAndRunTest {
        check(underTest.nativePointer == 0L)

        underTest.initialize()

        Truth.assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun initialize_afterDestroy_initializesNativeOpenXrManager() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        underTest.destroy()
        check(underTest.nativePointer == 0L)

        underTest.initialize()

        Truth.assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun configure_faceTrackingEnabled_addsFaceToUpdatables() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        check(underTest.config.faceTracking == FaceTrackingMode.DISABLED)
        check(underTest.perceptionManager.xrResources.updatables.isEmpty())

        // Configure twice because the first attempt will throw an exception during testing due to
        // calibration being read as false the first time the OpenXR stub is called.
        try {
            underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        } catch (e: FaceTrackingNotCalibratedException) {
            underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        }

        Truth.assertThat(underTest.perceptionManager.xrResources.updatables)
            .containsExactly(underTest.perceptionManager.xrResources.userFace)
    }

    @Test
    fun configure_faceTrackingDisabled_removesFaceFromUpdatables() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        try {
            underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        } catch (e: FaceTrackingNotCalibratedException) {
            underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        }
        check(
            underTest.perceptionManager.xrResources.updatables.contains(
                underTest.perceptionManager.xrResources.userFace
            )
        )

        underTest.configure(Config(faceTracking = FaceTrackingMode.DISABLED))

        Truth.assertThat(underTest.perceptionManager.xrResources.updatables)
            .doesNotContain(underTest.perceptionManager.xrResources.userFace)
    }

    @Test
    fun configure_faceTrackingEnabled_notCalibrated_throwsNotCalibratedException() =
        initOpenXrRuntimeAndRunTest {
            underTest.initialize()

            assertFailsWith<FaceTrackingNotCalibratedException> {
                underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
            }
        }

    @Test
    fun configure_deviceTrackingEnabled_addsDeviceToUpdatables() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        check(underTest.config.deviceTracking == DeviceTrackingMode.DISABLED)
        check(underTest.perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN))

        Truth.assertThat(underTest.perceptionManager.xrResources.updatables)
            .containsExactly(underTest.perceptionManager.xrResources.arDevice)
    }

    @Test
    fun configure_deviceTrackingDisabled_removesDeviceToUpdatables() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        underTest.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN))
        check(
            underTest.perceptionManager.xrResources.updatables.contains(
                underTest.perceptionManager.xrResources.arDevice
            )
        )

        underTest.configure(Config(deviceTracking = DeviceTrackingMode.DISABLED))

        Truth.assertThat(underTest.perceptionManager.xrResources.updatables)
            .doesNotContain(underTest.perceptionManager.xrResources.arDevice)
    }

    // TODO(b/392660855): Add a test for all APIs gated by a feature that needs to be configured.
    @Test
    fun configure_withSufficientPermissions_doesNotThrowException() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()

        underTest.configure(
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                deviceTracking = DeviceTrackingMode.DISABLED,
                depthEstimation = DepthEstimationMode.DISABLED,
                anchorPersistence = AnchorPersistenceMode.LOCAL,
            )
        )
    }

    @Test
    @Ignore("b/346615429 This test is currently broken")
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun configure_insufficientPermissions_throwsSecurityException() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()

        // The OpenXR stub returns `XR_ERROR_PERMISSION_INSUFFICIENT` when calling
        // `xrEnumerateDepthResolutionsANDROID` which is triggered by attempting to enable the
        // DepthEstimation feature.
        assertFailsWith<SecurityException> {
            underTest.configure(
                Config(
                    PlaneTrackingMode.DISABLED,
                    HandTrackingMode.DISABLED,
                    DeviceTrackingMode.DISABLED,
                    DepthEstimationMode.SMOOTH_AND_RAW,
                    AnchorPersistenceMode.DISABLED,
                )
            )
        }
    }

    @Test
    fun configure_beforeInitialize_throwsIllegalStateException() = initOpenXrRuntimeAndRunTest {
        // The OpenXR stub returns XR_ERROR_HANDLE_INVALID if native session hasn't been set via
        // OpenXrRuntime.initialize()
        assertFailsWith<IllegalStateException> {
            underTest.configure(
                Config(
                    PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    HandTrackingMode.DISABLED,
                    DeviceTrackingMode.DISABLED,
                    DepthEstimationMode.DISABLED,
                    AnchorPersistenceMode.DISABLED,
                )
            )
        }
    }

    @Test
    fun configure_smoothAndRawDepth_throwsUnsupportedOperationException() =
        initOpenXrRuntimeAndRunTest {
            underTest.initialize()

            assertFailsWith<UnsupportedOperationException> {
                underTest.configure(Config(depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW))
            }
        }

    @Test
    fun configure_updatesDepthEstimationForPerceptionManagerAndDepthMaps() =
        initOpenXrRuntimeAndRunTest {
            underTest.initialize()
            check(underTest.perceptionManager.depthEstimationMode == DepthEstimationMode.DISABLED)
            check(
                underTest.perceptionManager.xrResources.leftDepth.depthEstimationMode ==
                    DepthEstimationMode.DISABLED
            )
            check(
                underTest.perceptionManager.xrResources.rightDepth.depthEstimationMode ==
                    DepthEstimationMode.DISABLED
            )

            underTest.configure(Config(depthEstimation = DepthEstimationMode.RAW_ONLY))

            Truth.assertThat(underTest.perceptionManager.depthEstimationMode)
                .isEqualTo(DepthEstimationMode.RAW_ONLY)
            Truth.assertThat(underTest.perceptionManager.xrResources.leftDepth.depthEstimationMode)
                .isEqualTo(DepthEstimationMode.RAW_ONLY)
            Truth.assertThat(underTest.perceptionManager.xrResources.rightDepth.depthEstimationMode)
                .isEqualTo(DepthEstimationMode.RAW_ONLY)
        }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun resume_doesNotThrowIllegalStateException() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()

        underTest.resume()
    }

    @Test
    fun resume_afterDestroyAndInitialize_doesNotThrowIllegalStateException() =
        initOpenXrRuntimeAndRunTest {
            underTest.initialize()
            underTest.destroy()
            check(underTest.nativePointer == 0L)
            underTest.initialize()

            underTest.resume()
        }

    @Test
    fun update_planeTrackingDisabled_doesNotUpdateTrackables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.trackables.isEmpty())
            check(underTest.config.planeTracking == PlaneTrackingMode.DISABLED)

            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    fun update_planeTrackingEnabled_addsPlaneToUpdatables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.xrResources.updatables.isEmpty())
            underTest.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))

            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    fun update_objectTrackingDisabled_doesNotUpdateTrackables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.trackables.isEmpty())
            underTest.configure(Config())

            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    fun update_objectTrackingEnabled_addsObjectToUpdatables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.xrResources.updatables.isEmpty())

            underTest.configure(
                Config(augmentedObjectCategories = setOf(AugmentedObjectCategory.KEYBOARD))
            )
            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    fun update_imageTrackingDisabled_doesNotUpdateTrackables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.trackables.isEmpty())
            underTest.configure(Config())

            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    @Ignore("This test requires internal clock to be mocked")
    fun update_imageTracking_addsImageToUpdatables() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()
            check(underTest.perceptionManager.xrResources.updatables.isEmpty())

            val augmentedImageDatabase = AugmentedImageDatabase()
            augmentedImageDatabase.addAugmentedImageDatabaseEntry(
                AugmentedImageDatabaseEntryMode.DYNAMIC,
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )

            underTest.configure(Config(augmentedImageDatabase = augmentedImageDatabase))
            check(underTest.config.augmentedImageDatabase != null)
            check(underTest.config.augmentedImageDatabase?.entries?.isNotEmpty() == true)

            underTest.update()

            Truth.assertThat(underTest.perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun update_returnsTimeMarkFromTimeSource() = initOpenXrRuntimeAndRunTest {
        runTest {
            underTest.initialize()
            underTest.resume()

            // The OpenXR stub returns a different value for each call to [OpenXrTimeSource::read]
            // in increments of 1000ns when `xrConvertTimespecTimeToTimeKHR` is executed. The first
            // call returns 1000ns and is the value associated with [timeMark]. The second call
            // returns 2000ns and is the value associated with [AbstractLongTimeSource::zero],
            // which is calculated automatically with the first call to [OpenXrTimeSource::markNow].
            // Note that this is just an idiosyncrasy of the test stub and not how OpenXR works in
            // practice, where the second call would return an almost identical value to the first
            // call's value.
            val timeMark = underTest.update()

            // The third call happens with the call to [elapsedNow] and returns 3000ns. Thus, the
            // elapsed time is 3000ns (i.e. "now") -  1000ns (i.e. "the start time") = 2000ns.
            Truth.assertThat(timeMark.elapsedNow().inWholeNanoseconds).isEqualTo(2000L)
        }
    }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun pause_doesNotThrowIllegalStateException() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        underTest.resume()

        underTest.pause()
    }

    @Test
    fun pause_withoutResume_doesNotDestroyNativeOpenXrManager() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()

        underTest.pause()

        Truth.assertThat(underTest.nativePointer).isNotEqualTo(0L)
    }

    @Test
    fun destroy_destroysNativeOpenXrManager() = initOpenXrRuntimeAndRunTest {
        underTest.initialize()
        check(underTest.nativePointer != 0L)

        underTest.destroy()

        Truth.assertThat(underTest.nativePointer).isEqualTo(0L)
    }

    private fun initOpenXrRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val lifecycleManager = OpenXrManager(timeSource)
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            underTest = OpenXrRuntime(it, lifecycleManager, perceptionManager, timeSource)

            testBody()

            // Destroy the OpenXR runtime here in lieu of an @After method to ensure that the
            // calls to the OpenXR runtime are coming from the same thread.
            underTest.destroy()
        }
    }
}
