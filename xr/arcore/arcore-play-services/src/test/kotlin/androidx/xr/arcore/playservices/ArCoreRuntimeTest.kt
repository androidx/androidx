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

package androidx.xr.arcore.playservices

import android.app.Activity
import androidx.kruth.assertThat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Config.GeospatialMode
import com.google.ar.core.Session
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreRuntimeTest {
    private lateinit var underTest: ArCoreRuntime
    private lateinit var arCoreManager: ArCoreManager
    private lateinit var mockArCoreApk: ArCoreApk
    private lateinit var mockSession: Session

    private val timeSource = ArCoreTimeSource()

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    @Test
    fun isSupported_depthSmoothOnly_whenTrueIn1x_returnsTrue() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.AUTOMATIC)).thenReturn(true)

        assertThat(underTest.isSupported(DepthEstimationMode.SMOOTH_ONLY)).isTrue()
    }

    @Test
    fun isSupported_depthSmoothOnly_whenFalseIn1x_returnsFalse() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.AUTOMATIC)).thenReturn(false)

        assertThat(underTest.isSupported(DepthEstimationMode.SMOOTH_ONLY)).isFalse()
    }

    @Test
    fun isSupported_depthSmoothAndRaw_whenTrueIn1x_returnsTrue() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.AUTOMATIC)).thenReturn(true)

        assertThat(underTest.isSupported(DepthEstimationMode.SMOOTH_AND_RAW)).isTrue()
    }

    @Test
    fun isSupported_depthSmoothAndRaw_whenFalseIn1x_returnsFalse() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.AUTOMATIC)).thenReturn(false)

        assertThat(underTest.isSupported(DepthEstimationMode.SMOOTH_AND_RAW)).isFalse()
    }

    @Test
    fun isSupported_depthRawOnly_whenTrueIn1x_returnsTrue() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.RAW_DEPTH_ONLY)).thenReturn(true)

        assertThat(underTest.isSupported(DepthEstimationMode.RAW_ONLY)).isTrue()
    }

    @Test
    fun isSupported_depthRawOnly_whenFalseIn1x_returnsFalse() = initRuntimeAndRunTest {
        whenever(mockSession.isDepthModeSupported(DepthMode.RAW_DEPTH_ONLY)).thenReturn(false)

        assertThat(underTest.isSupported(DepthEstimationMode.RAW_ONLY)).isFalse()
    }

    @Test
    fun isSupported_geospatialVpsAndGps_whenFalseIn1x_returnsFalse() = initRuntimeAndRunTest {
        whenever(mockSession.isGeospatialModeSupported(GeospatialMode.ENABLED)).thenReturn(false)

        assertThat(underTest.isSupported(androidx.xr.runtime.GeospatialMode.VPS_AND_GPS)).isFalse()
    }

    @Test
    fun isSupported_geospatialVpsAndGps_whenTrueIn1x_returnsTrue() = initRuntimeAndRunTest {
        whenever(mockSession.isGeospatialModeSupported(GeospatialMode.ENABLED)).thenReturn(true)

        assertThat(underTest.isSupported(androidx.xr.runtime.GeospatialMode.VPS_AND_GPS)).isTrue()
    }

    @Test
    fun isSupported_inSupportedList_returnsTrue() = initRuntimeAndRunTest {
        for (mode in ArCoreRuntime.SUPPORTED_CONFIG_MODES) {
            assertThat(underTest.isSupported(mode)).isTrue()
        }
    }

    @Test
    fun isSupported_notInSupportedList_returnsFalse() = initRuntimeAndRunTest {
        assertThat(underTest.isSupported(FakeConfigMode.UNSUPPORTED_BY_ARCORE)).isFalse()
    }

    private fun initRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val perceptionManager = ArCorePerceptionManager(timeSource)
            mockArCoreApk = mock<ArCoreApk>()
            mockSession = mock<Session>()
            arCoreManager = ArCoreManager(it, perceptionManager, timeSource, mockArCoreApk)
            arCoreManager._session = mockSession
            underTest = ArCoreRuntime(arCoreManager, perceptionManager)

            testBody()
        }
    }

    private class FakeConfigMode private constructor() : Config.ConfigMode() {
        companion object {
            @JvmField val UNSUPPORTED_BY_ARCORE: FakeConfigMode = FakeConfigMode()
        }
    }
}
