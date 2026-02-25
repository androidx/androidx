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

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.runtime.Config
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@SuppressLint("BanThreadSleep")
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrGeospatialTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }

        const val XR_POLL_TIME_MS = 20L
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var openXrManager: OpenXrManager
    private lateinit var perceptionManager: OpenXrPerceptionManager
    private lateinit var underTest: OpenXrGeospatial
    private lateinit var timeSource: OpenXrTimeSource

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        perceptionManager = OpenXrPerceptionManager(timeSource)
        underTest = perceptionManager.geospatial
    }

    @Test
    fun createGeospatialPoseFromPose_returnsGeospatialPose() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            val result = underTest.createGeospatialPoseFromPose(Pose())

            // The values below come from `xrLocateGeospatialPoseFromPoseANDROIDX2` in
            // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
            assertThat(result.geospatialPose)
                .isEqualTo(
                    GeospatialPose(
                        latitude = 37.422,
                        longitude = -122.084,
                        altitude = 10.0,
                        eastUpSouthQuaternion = Quaternion(0f, 0f, 0f, 1f),
                    )
                )
            assertThat(result.horizontalAccuracy).isEqualTo(1.0)
            assertThat(result.verticalAccuracy).isEqualTo(2.0)
            assertThat(result.orientationYawAccuracy).isEqualTo(3.0)
        }
    }

    @Test
    fun createPoseFromGeospatialPose_returnsPose() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            val result = underTest.createPoseFromGeospatialPose(GeospatialPose())

            // The values below come from `xrLocateGeospatialPoseANDROIDX2` in
            // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
            assertThat(result)
                .isEqualTo(
                    Pose(
                        translation = Vector3(0.0f, 0.0f, 2.0f),
                        rotation = Quaternion(0.0f, 1.0f, 0.0f, 1.0f),
                    )
                )
        }
    }

    @Test
    fun createGeospatialAnchor_returnsAnchor() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            val anchor = underTest.createAnchor(0.0, 0.0, 0.0, Quaternion())
            assertThat(anchor).isInstanceOf(OpenXrAnchor::class.java)
        }
    }

    @Test
    fun createSurfaceAnchor_returnsAnchor() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            val anchor =
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion(),
                    Geospatial.Surface.TERRAIN,
                )
            assertThat(anchor).isInstanceOf(OpenXrAnchor::class.java)
        }
    }

    @Test
    fun createGeospatialAnchor_anchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            // Number of calls comes from 'kAnchorResourcesLimit' defined in
            // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
            repeat(5) { underTest.createAnchor(0.0, 0.0, 0.0, Quaternion()) }

            assertThrows(AnchorResourcesExhaustedException::class.java) {
                underTest.createAnchor(0.0, 0.0, 0.0, Quaternion())
            }
        }
    }

    @Test
    fun createAnchors_sharedAnchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        runTest {
            ensureGeospatialRunning()

            // Number of calls comes from 'kAnchorResourcesLimit' defined in
            // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
            repeat(2) { underTest.createAnchor(0.0, 0.0, 0.0, Quaternion()) }
            repeat(3) {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion(),
                    Geospatial.Surface.TERRAIN,
                )
            }

            assertThrows(AnchorResourcesExhaustedException::class.java) {
                underTest.createAnchor(0.0, 0.0, 0.0, Quaternion())
            }
            try {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion(),
                    Geospatial.Surface.TERRAIN,
                )
                fail("Expected AnchorResourcesExhaustedException was not thrown.")
            } catch (e: AnchorResourcesExhaustedException) {
                // This is expected.
            }
        }
    }

    private suspend fun ensureGeospatialRunning() {
        // Ensure the runtime handles async events and futures so that Geospatial is in the
        // running state.
        openXrManager.update()
        Thread.sleep(XR_POLL_TIME_MS)
        openXrManager.update()
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            openXrManager.create()
            openXrManager.resume()
            openXrManager.configure(Config(geospatial = GeospatialMode.VPS_AND_GPS))

            testBody()

            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
