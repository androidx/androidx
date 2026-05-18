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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestQrCode
import androidx.xr.runtime.Config
import androidx.xr.runtime.QrCodeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class QrCodeTest {

    companion object {
        private const val QR_CODE_DATA: String = "data"
    }

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(qrCodeTracking = QrCodeTrackingMode.DYNAMIC))
    }

    @Test
    fun subscribe_collectReturnsQrCode() =
        runTest(testDispatcher) {
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)
            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
        }

    @Test
    fun subscribe_qrCodeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(qrCodeTracking = QrCodeTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { QrCode.subscribe(session) }
    }

    @Test
    fun update_trackingStateMatchesTestQrCodeVisibility() =
        runTest(testDispatcher) {
            activityController.resume()
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)
            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)

            testQrCode.isVisible = false
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun update_qrCodeTrackingDisabled_trackingStops() =
        runTest(testDispatcher) {
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)

            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }

            activityController.pause()
            advanceUntilIdle()
            session.configure(Config(qrCodeTracking = QrCodeTrackingMode.DISABLED))
            activityController.resume()
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.STOPPED)
        }

    @Test
    fun update_centerPoseMatchesTestQrCodeCenterPose() =
        runTest(testDispatcher) {
            activityController.resume()
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)
            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(Pose())

            val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
            testQrCode.centerPose = newPose
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(newPose)
        }

    @Test
    fun update_extentsMatchesTestQrCodeExtents() =
        runTest(testDispatcher) {
            activityController.resume()
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)
            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(FloatSize2d())

            val newExtents = FloatSize2d(3.0f, 4.0f)
            testQrCode.extents = newExtents
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(newExtents)
        }

    @Test
    fun update_dataMatchesTestQrCodeData() =
        runTest(testDispatcher) {
            activityController.resume()
            val testQrCode = TestQrCode(QR_CODE_DATA)
            arCoreTestRule.addTrackables(testQrCode)
            advanceUntilIdle()

            var underTest = emptyList<QrCode>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                QrCode.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.data).isEqualTo(QR_CODE_DATA)
        }
}
