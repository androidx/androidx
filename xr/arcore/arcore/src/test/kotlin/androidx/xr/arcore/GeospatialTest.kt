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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.AnchorNotAuthorizedException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AnchorUnsupportedLocationException
import androidx.xr.arcore.runtime.Geospatial as RuntimeGeospatial
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakeRuntimeGeospatial
import androidx.xr.runtime.Config
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeospatialTest {

    companion object {
        private val ROTATION: Quaternion = Quaternion.Identity
        private val TRANSLATION: Vector3 = Vector3(1f, 2f, 3f)
        private val POSE: Pose = Pose(TRANSLATION, ROTATION)
        private val GEOSPATIAL_POSE: GeospatialPose = GeospatialPose(1.0, 2.0, 3.0, ROTATION)
        private const val HORIZONTAL_ACCURACY: Double = 1.0
        private const val VERTICAL_ACCURACY: Double = 2.0
        private const val ORIENTATION_YAW_ACCURACY: Double = 3.0
        private const val LATITUDE: Double = 10.0
        private const val LONGITUDE: Double = 20.0
        private const val ALTITUDE: Double = 30.0
        private const val ALTITUDE_ABOVE_SURFACE: Double = 5.0
        private val EUS_QUATERNION: Quaternion = Quaternion.Identity
    }

    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var runtimeGeospatial: FakeRuntimeGeospatial
    private lateinit var session: Session

    private fun doBlocking(block: suspend CoroutineScope.() -> Unit) {
        runBlocking(block = block)
    }

    @Before
    fun setUp() {
        xrResourcesManager = XrResourcesManager()
        runtimeGeospatial = FakeRuntimeGeospatial(RuntimeGeospatial.State.NOT_RUNNING)
    }

    @Test
    fun getInstance_returnsGeospatial() {
        createTestSessionAndRunTest() { assertThat(Geospatial.getInstance(session)).isNotNull() }
    }

    private fun createTestSessionAndRunTest(
        coroutineDispatcher: CoroutineDispatcher = StandardTestDispatcher(),
        testBody: () -> Unit,
    ) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                session =
                    (Session.create(activity, coroutineDispatcher) as SessionCreateSuccess).session
                xrResourcesManager.lifecycleManager = session.perceptionRuntime.lifecycleManager
                session.configure(Config(geospatial = GeospatialMode.VPS_AND_GPS))

                testBody()
            }
        }
    }

    @Test
    fun state_defaultStateIsNotRunning() = runBlocking {
        val runtimeGeospatial = FakeRuntimeGeospatial()
        val underTest = Geospatial(runtimeGeospatial, xrResourcesManager)

        assertThat(underTest.state.value).isEqualTo(Geospatial.State.NOT_RUNNING)
    }

    @Test
    fun update_stateMatchesRuntimeGeospatial() = runBlocking {
        val runtimeGeospatial = FakeRuntimeGeospatial(RuntimeGeospatial.State.NOT_RUNNING)
        val underTest = Geospatial(runtimeGeospatial, xrResourcesManager)
        check(underTest.state.value == Geospatial.State.NOT_RUNNING)

        // Update to Running state.
        runtimeGeospatial.state = RuntimeGeospatial.State.RUNNING
        underTest.update()

        assertThat(underTest.state.value).isEqualTo(Geospatial.State.RUNNING)

        // Update to NotRunning state with error.
        runtimeGeospatial.state = RuntimeGeospatial.State.ERROR_INTERNAL
        underTest.update()

        assertThat(underTest.state.value).isEqualTo(Geospatial.State.ERROR_INTERNAL)
    }

    @Test
    fun createGeospatialPoseFromPose_success_returnsSuccessResult() = createTestSessionAndRunTest {
        val underTest = Geospatial.getInstance(session)
        getFakeRuntimeGeospatial().nextGeospatialPoseResult =
            RuntimeGeospatial.GeospatialPoseResult(
                GEOSPATIAL_POSE,
                HORIZONTAL_ACCURACY,
                VERTICAL_ACCURACY,
                ORIENTATION_YAW_ACCURACY,
            )

        val result = underTest.createGeospatialPoseFromPose(Pose(Vector3(), Quaternion()))
        val successResult = result as CreateGeospatialPoseFromPoseSuccess
        assertThat(successResult.pose).isEqualTo(GEOSPATIAL_POSE)
        assertThat(successResult.horizontalAccuracy).isEqualTo(HORIZONTAL_ACCURACY)
        assertThat(successResult.verticalAccuracy).isEqualTo(VERTICAL_ACCURACY)
        assertThat(successResult.orientationYawAccuracy).isEqualTo(ORIENTATION_YAW_ACCURACY)
    }

    @Test
    fun createGeospatialPoseFromPose_notTracking_returnsNotTrackingResult() =
        createTestSessionAndRunTest {
            val underTest = Geospatial.getInstance(session)
            getFakeRuntimeGeospatial().nextException = GeospatialPoseNotTrackingException()

            val result = underTest.createGeospatialPoseFromPose(Pose(Vector3(), Quaternion()))

            assertThat(result).isInstanceOf(CreateGeospatialPoseFromPoseNotTracking::class.java)
        }

    @Test
    fun createPoseFromGeospatialPose_success_returnsSuccessResult() = createTestSessionAndRunTest {
        val underTest = Geospatial.getInstance(session)
        getFakeRuntimeGeospatial().nextPose = POSE

        val result = underTest.createPoseFromGeospatialPose(GeospatialPose())
        val successResult = result as CreatePoseFromGeospatialPoseSuccess

        assertThat(successResult.pose).isEqualTo(POSE)
    }

    @Test
    fun createPoseFromGeospatialPose_notTracking_returnsNotTrackingResult() =
        createTestSessionAndRunTest {
            val underTest = Geospatial.getInstance(session)
            getFakeRuntimeGeospatial().nextException = GeospatialPoseNotTrackingException()

            val result = underTest.createPoseFromGeospatialPose(GeospatialPose())

            assertThat(result).isInstanceOf(CreatePoseFromGeospatialPoseNotTracking::class.java)
        }

    @Test
    fun createAnchor_success_returnsSuccessResultWithAnchor() = createTestSessionAndRunTest {
        val underTest = Geospatial(runtimeGeospatial, xrResourcesManager)
        val fakePerceptionManager = getFakePerceptionManager()
        val fakeAnchor = fakePerceptionManager.createAnchor(Pose.Identity)
        runtimeGeospatial.nextAnchor = fakeAnchor

        val result = underTest.createAnchor(LATITUDE, LONGITUDE, ALTITUDE, EUS_QUATERNION)

        assertThat(result).isInstanceOf(AnchorCreateSuccess::class.java)
        val successResult = result as AnchorCreateSuccess
        assertThat(successResult.anchor.runtimeAnchor).isEqualTo(fakeAnchor)
        assertThat((xrResourcesManager.updatables.firstOrNull() as Anchor).runtimeAnchor)
            .isEqualTo(fakeAnchor)
    }

    @Test
    fun createAnchor_resourceExhausted_returnsResourcesExhaustedResult() =
        createTestSessionAndRunTest {
            val underTest = Geospatial.getInstance(session)
            getFakeRuntimeGeospatial().nextException = AnchorResourcesExhaustedException()

            val result = underTest.createAnchor(LATITUDE, LONGITUDE, ALTITUDE, EUS_QUATERNION)

            assertThat(result).isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }

    @Test
    fun createAnchor_invalidLatitude_throwsIllegalArgumentException() =
        createTestSessionAndRunTest {
            val underTest = Geospatial.getInstance(session)
            getFakeRuntimeGeospatial().nextException = IllegalArgumentException()

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchor(90.0, LONGITUDE, ALTITUDE, EUS_QUATERNION)
            }
        }

    @Test
    fun createPoseFromGeospatialPose_withVpsDisabled_throwsIllegalStateException() {
        val newConfig = Config(geospatial = GeospatialMode.DISABLED)
        val fakeLifecycleManager = FakeLifecycleManager()
        fakeLifecycleManager.config = newConfig
        xrResourcesManager.lifecycleManager = fakeLifecycleManager
        val underTest = Geospatial(runtimeGeospatial, xrResourcesManager)
        val geospatialPose = GeospatialPose(1.0, 2.0, 3.0, Quaternion(0.1f, 0.2f, 0.3f, 0.4f))

        val exception =
            assertFailsWith<IllegalStateException> {
                underTest.createPoseFromGeospatialPose(geospatialPose)
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("To use this function, Config.GeospatialMode must be set to VPS_AND_GPS.")
    }

    @Test
    fun createAnchorOnSurface_success_returnsSuccessResultWithAnchor() =
        createTestSessionAndRunTest {
            doBlocking {
                val underTest = Geospatial(runtimeGeospatial, xrResourcesManager)
                val fakePerceptionManager = getFakePerceptionManager()
                val fakeAnchor = fakePerceptionManager.createAnchor(Pose.Identity)
                runtimeGeospatial.nextAnchor = fakeAnchor

                val result =
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Geospatial.Surface.TERRAIN,
                    )

                assertThat(result).isInstanceOf(AnchorCreateSuccess::class.java)
                val successResult = result as AnchorCreateSuccess
                assertThat(successResult.anchor.runtimeAnchor).isEqualTo(fakeAnchor)
                assertThat((xrResourcesManager.updatables.firstOrNull() as Anchor).runtimeAnchor)
                    .isEqualTo(fakeAnchor)
            }
        }

    @Test
    fun createAnchorOnSurface_resourceExhausted_returnsResourcesExhaustedResult() =
        createTestSessionAndRunTest {
            doBlocking {
                val underTest = Geospatial.getInstance(session)
                getFakeRuntimeGeospatial().nextException = AnchorResourcesExhaustedException()

                val result =
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Geospatial.Surface.TERRAIN,
                    )

                assertThat(result).isInstanceOf(AnchorCreateResourcesExhausted::class.java)
            }
        }

    @Test
    fun createAnchorOnSurface_notAuthorized_returnsNotAuthorizedResult() =
        createTestSessionAndRunTest {
            doBlocking {
                val underTest = Geospatial.getInstance(session)
                getFakeRuntimeGeospatial().nextException = AnchorNotAuthorizedException()

                val result =
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Geospatial.Surface.TERRAIN,
                    )

                assertThat(result).isInstanceOf(AnchorCreateNotAuthorized::class.java)
            }
        }

    @Test
    fun createAnchorOnSurface_unsupportedLocation_returnsUnsupportedLocationResult() =
        createTestSessionAndRunTest {
            doBlocking {
                val underTest = Geospatial.getInstance(session)
                getFakeRuntimeGeospatial().nextException = AnchorUnsupportedLocationException()

                val result =
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Geospatial.Surface.TERRAIN,
                    )

                assertThat(result).isInstanceOf(AnchorCreateUnsupportedLocation::class.java)
            }
        }

    @Test
    fun createAnchorOnSurface_invalidLatitude_throwsIllegalArgumentException() =
        createTestSessionAndRunTest {
            doBlocking {
                val underTest = Geospatial.getInstance(session)
                getFakeRuntimeGeospatial().nextException = IllegalArgumentException()

                assertFailsWith<IllegalArgumentException> {
                    underTest.createAnchorOnSurface(
                        90.0,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Geospatial.Surface.TERRAIN,
                    )
                }
            }
        }

    private fun getFakePerceptionManager(): FakePerceptionManager {
        return session.perceptionRuntime.perceptionManager as FakePerceptionManager
    }

    private fun getFakeRuntimeGeospatial(): FakeRuntimeGeospatial {
        return getFakePerceptionManager().geospatial as FakeRuntimeGeospatial
    }
}
