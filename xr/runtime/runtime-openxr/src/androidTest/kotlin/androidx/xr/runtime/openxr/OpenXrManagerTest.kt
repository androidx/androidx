/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.internal.PermissionNotGrantedException
import com.google.common.truth.Truth.assertThat
import kotlin.check
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrManagerTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var underTest: OpenXrManager
    private lateinit var perceptionManager: OpenXrPerceptionManager
    private lateinit var timeSource: OpenXrTimeSource

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        perceptionManager = OpenXrPerceptionManager(timeSource)
    }

    @Test
    fun create_initializesNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        check(underTest.nativePointer == 0L)

        underTest.create()

        assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun create_afterStop_initializesNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.stop()
        check(underTest.nativePointer == 0L)

        underTest.create()

        assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun configure_faceTrackingEnabled_addsFaceToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.config.faceTracking == Config.FaceTrackingMode.DISABLED)
        check(perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(faceTracking = Config.FaceTrackingMode.USER))

        assertThat(perceptionManager.xrResources.updatables)
            .containsExactly(perceptionManager.xrResources.userFace)
    }

    @Test
    fun configure_faceTrackingDisabled_removesFaceFromUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.configure(Config(faceTracking = Config.FaceTrackingMode.USER))
        check(
            perceptionManager.xrResources.updatables.contains(
                perceptionManager.xrResources.userFace
            )
        )

        underTest.configure(Config(faceTracking = Config.FaceTrackingMode.DISABLED))

        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.userFace)
    }

    @Ignore("b/427434474 test behavior is unpredictable until stub is fixed")
    @Test
    fun configure_faceTrackingEnabled_notCalibrated_throwsNotCalibratedException() =
        initOpenXrManagerAndRunTest(isFaceTrackingCalibrated = false) {
            underTest.create()

            assertThrows(FaceTrackingNotCalibratedException::class.java) {
                underTest.configure(Config(faceTracking = Config.FaceTrackingMode.USER))
            }
        }

    @Test
    fun configure_deviceTrackingEnabled_addsDeviceToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.config.deviceTracking == Config.DeviceTrackingMode.DISABLED)
        check(perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN))

        assertThat(perceptionManager.xrResources.updatables)
            .containsExactly(perceptionManager.xrResources.arDevice)
    }

    @Test
    fun configure_deviceTrackingDisabled_removesDeviceToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN))
        check(
            perceptionManager.xrResources.updatables.contains(
                perceptionManager.xrResources.arDevice
            )
        )

        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.arDevice)
    }

    // TODO(b/392660855): Add a test for all APIs gated by a feature that needs to be configured.
    @Test
    fun configure_withSufficientPermissions_doesNotThrowException() = initOpenXrManagerAndRunTest {
        underTest.create()

        underTest.configure(
            Config(
                planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                headTracking = Config.HeadTrackingMode.DISABLED,
                depthEstimation = Config.DepthEstimationMode.DISABLED,
                anchorPersistence = Config.AnchorPersistenceMode.LOCAL,
            )
        )
    }

    @Test
    @Ignore("b/346615429 This test is currently broken")
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun configure_insufficientPermissions_throwsPermissionNotGrantedException() =
        initOpenXrManagerAndRunTest {
            underTest.create()

            // The OpenXR stub returns `XR_ERROR_PERMISSION_INSUFFICIENT` when calling
            // `xrEnumerateDepthResolutionsANDROID` which is triggered by attempting to enable the
            // DepthEstimation feature.
            assertThrows(PermissionNotGrantedException::class.java) {
                underTest.configure(
                    Config(
                        Config.PlaneTrackingMode.DISABLED,
                        Config.HandTrackingMode.DISABLED,
                        Config.HeadTrackingMode.DISABLED,
                        Config.DepthEstimationMode.SMOOTH_AND_RAW,
                        Config.AnchorPersistenceMode.DISABLED,
                    )
                )
            }
        }

    @Test
    fun configure_withoutCreate_throwsIllegalStateException() = initOpenXrManagerAndRunTest {
        // The OpenXR stub returns `XR_ERROR_HANDLE_INVALID` if the `xrSession` has not been
        // initialized by `OpenXrManager.create()`.
        assertThrows(IllegalStateException::class.java) {
            underTest.configure(
                Config(
                    Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    Config.HandTrackingMode.DISABLED,
                    Config.HeadTrackingMode.DISABLED,
                    Config.DepthEstimationMode.DISABLED,
                    Config.AnchorPersistenceMode.DISABLED,
                )
            )
        }
    }

    @Test
    fun configure_smoothAndRawDepth_throwsConfigurationNotSupportedException() =
        initOpenXrManagerAndRunTest {
            underTest.create()

            assertThrows(ConfigurationNotSupportedException::class.java) {
                underTest.configure(
                    Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_AND_RAW)
                )
            }
        }

    @Test
    fun configure_updatesDepthEstimationForPerceptionManagerAndDepthMaps() =
        initOpenXrManagerAndRunTest {
            underTest.create()
            check(perceptionManager.depthEstimationMode == Config.DepthEstimationMode.DISABLED)
            check(
                perceptionManager.xrResources.leftDepthMap.depthEstimationMode ==
                    Config.DepthEstimationMode.DISABLED
            )
            check(
                perceptionManager.xrResources.rightDepthMap.depthEstimationMode ==
                    Config.DepthEstimationMode.DISABLED
            )

            underTest.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))

            assertThat(perceptionManager.depthEstimationMode)
                .isEqualTo(Config.DepthEstimationMode.RAW_ONLY)
            assertThat(perceptionManager.xrResources.leftDepthMap.depthEstimationMode)
                .isEqualTo(Config.DepthEstimationMode.RAW_ONLY)
            assertThat(perceptionManager.xrResources.rightDepthMap.depthEstimationMode)
                .isEqualTo(Config.DepthEstimationMode.RAW_ONLY)
        }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun resume_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.create()

        underTest.resume()
    }

    @Test
    fun resume_afterStopAndCreate_doesNotThrowIllegalStateException() =
        initOpenXrManagerAndRunTest {
            underTest.create()
            underTest.stop()
            check(underTest.nativePointer == 0L)
            underTest.create()

            underTest.resume()
        }

    @Test
    fun update_planeTrackingDisabled_doesNotUpdateTrackables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.trackables.isEmpty())
            check(underTest.config.planeTracking == Config.PlaneTrackingMode.DISABLED)

            underTest.update()

            assertThat(perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    fun update_planeTrackingEnabled_addsPlaneToUpdatables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.xrResources.updatables.isEmpty())
            underTest.configure(
                Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )

            underTest.update()

            assertThat(perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    fun update_objectTrackingDisabled_doesNotUpdateTrackables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.trackables.isEmpty())
            underTest.configure(Config())

            underTest.update()

            assertThat(perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    fun update_objectTrackingEnabled_addsObjectToUpdatables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.xrResources.updatables.isEmpty())

            underTest.configure(
                Config(augmentedObjectCategories = listOf(AugmentedObjectCategory.KEYBOARD))
            )
            underTest.update()

            assertThat(perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun update_returnsTimeMarkFromTimeSource() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
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
            assertThat(timeMark.elapsedNow().inWholeNanoseconds).isEqualTo(2000L)
        }
    }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun pause_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.resume()

        underTest.pause()
    }

    @Test
    fun pause_withoutResume_doesNotDestroyNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        underTest.create()

        underTest.pause()

        assertThat(underTest.nativePointer).isNotEqualTo(0L)
    }

    @Test
    fun stop_destroysNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.nativePointer != 0L)

        underTest.stop()

        assertThat(underTest.nativePointer).isEqualTo(0L)
    }

    private fun initOpenXrManagerAndRunTest(
        isFaceTrackingCalibrated: Boolean = true,
        testBody: () -> Unit,
    ) {
        activityRule.scenario.onActivity {
            underTest =
                OpenXrManager(
                    it,
                    perceptionManager,
                    timeSource,
                    faceTrackingCalibrated = isFaceTrackingCalibrated,
                )

            testBody()

            // Stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            underTest.stop()
        }
    }
}
