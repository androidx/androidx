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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.internal.Earth as RuntimeEarth
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimeArDevice
import androidx.xr.runtime.testing.FakeRuntimeAugmentedObject
import androidx.xr.runtime.testing.FakeRuntimeDepthMap
import androidx.xr.runtime.testing.FakeRuntimeEarth
import androidx.xr.runtime.testing.FakeRuntimeFace
import androidx.xr.runtime.testing.FakeRuntimeHand
import androidx.xr.runtime.testing.FakeRuntimePlane
import androidx.xr.runtime.testing.FakeRuntimeViewCamera
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XrResourcesManagerTest {

    private lateinit var underTest: XrResourcesManager
    private lateinit var session: Session

    private fun doBlocking(block: suspend CoroutineScope.() -> Unit) {
        runBlocking(block = block)
    }

    @Before
    fun setUp() {
        underTest = XrResourcesManager()
        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @After
    fun tearDown() {
        underTest.clear()
    }

    @Test
    fun initiateHands_setsAvailableHands() {
        val runtimeHand = FakeRuntimeHand()
        val runtimeHand2 = FakeRuntimeHand()

        underTest.initiateHands(runtimeHand, runtimeHand2)

        assertThat(underTest.leftHand!!.runtimeHand).isEqualTo(runtimeHand)
        assertThat(underTest.rightHand!!.runtimeHand).isEqualTo(runtimeHand2)
    }

    @Test
    fun initiateHands_setsWithNull() {
        underTest.initiateHands(leftRuntimeHand = null, rightRuntimeHand = null)

        assertThat(underTest.leftHand).isNull()
        assertThat(underTest.rightHand).isNull()
    }

    @Test
    fun initiateArDevice_setsArDeviceAndViewCameras() {
        val runtimeArDevice = FakeRuntimeArDevice()
        val runtimeViewCameras = listOf(FakeRuntimeViewCamera(), FakeRuntimeViewCamera())
        underTest.initiateArDeviceAndViewCameras(runtimeArDevice, runtimeViewCameras)

        assertThat(underTest.arDevice.runtimeArDevice).isEqualTo(runtimeArDevice)
        assertThat(underTest.viewCameras.size).isEqualTo(2)
        assertThat(underTest.viewCameras[0].state.value.pose).isEqualTo(runtimeViewCameras[0].pose)
        assertThat(underTest.viewCameras[1].state.value.pose).isEqualTo(runtimeViewCameras[1].pose)
    }

    @Test
    fun initiateFace_setsAvailableFace() {
        val runtimeFace = FakeRuntimeFace()

        underTest.initiateFace(runtimeFace)

        assertThat(underTest.userFace!!.runtimeFace).isEqualTo(runtimeFace)
    }

    @Test
    fun addUpdatable_addsUpdatable() = createTestSessionAndRunTest {
        val fakePerceptionManager = session.runtime.perceptionManager as FakePerceptionManager
        val anchor = Anchor(fakePerceptionManager.createAnchor(Pose()), underTest)
        check(underTest.updatables.isEmpty())

        underTest.addUpdatable(anchor)

        assertThat(underTest.updatables).containsExactly(anchor)
    }

    @Test
    fun removeUpdatable_removesUpdatable() = createTestSessionAndRunTest {
        val fakePerceptionManager = session.runtime.perceptionManager as FakePerceptionManager
        val anchor = Anchor(fakePerceptionManager.createAnchor(Pose()), underTest)
        underTest.addUpdatable(anchor)
        check(underTest.updatables.contains(anchor))
        check(underTest.updatables.size == 1)

        underTest.removeUpdatable(anchor)

        assertThat(underTest.updatables).isEmpty()
    }

    @Test
    fun clear_clearAllUpdatables() = createTestSessionAndRunTest {
        val fakePerceptionManager = session.runtime.perceptionManager as FakePerceptionManager
        val runtimeAnchor = fakePerceptionManager.createAnchor(Pose())
        val runtimeAnchor2 = fakePerceptionManager.createAnchor(Pose())
        val anchor = Anchor(runtimeAnchor, underTest)
        val anchor2 = Anchor(runtimeAnchor2, underTest)
        underTest.addUpdatable(anchor)
        underTest.addUpdatable(anchor2)
        check(underTest.updatables.isNotEmpty())

        underTest.clear()

        assertThat(underTest.updatables).isEmpty()
    }

    @Test
    fun syncTrackables_replacesExistingTrackables() {
        val runtimeTrackable1 = FakeRuntimePlane()
        val runtimeTrackable2 = FakeRuntimePlane()
        val runtimeTrackable3 = FakeRuntimePlane()
        underTest.syncTrackables(listOf(runtimeTrackable1, runtimeTrackable2))
        check(underTest.trackablesMap[runtimeTrackable1] != null)
        check(underTest.trackablesMap[runtimeTrackable2] != null)
        check(underTest.trackablesMap[runtimeTrackable3] == null)

        underTest.syncTrackables(listOf(runtimeTrackable2, runtimeTrackable3))

        assertThat(underTest.trackablesMap[runtimeTrackable1]).isNull()
        assertThat(underTest.trackablesMap[runtimeTrackable2]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeTrackable3]).isNotNull()
    }

    @Test
    fun syncTrackables_handlesAugmentedObjects() {
        val runtimeTrackable1 = FakeRuntimeAugmentedObject()
        val runtimeTrackable2 = FakeRuntimeAugmentedObject()
        val runtimeTrackable3 = FakeRuntimeAugmentedObject()

        underTest.syncTrackables(listOf(runtimeTrackable1, runtimeTrackable2))

        assertThat(underTest.trackablesMap[runtimeTrackable1]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeTrackable2]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeTrackable3]).isNull()
    }

    @Test
    fun clear_clearsAllTrackables() {
        underTest.syncTrackables(listOf(FakeRuntimePlane()))
        check(underTest.trackablesMap.isNotEmpty())

        underTest.clear()

        assertThat(underTest.trackablesMap).isEmpty()
    }

    @Test
    fun update_anchorDetached_andNotUpdated() = doBlocking {
        val runtimeAnchor = FakeRuntimePlane().createAnchor(Pose()) as FakeRuntimeAnchor
        check(runtimeAnchor.isAttached)
        val anchor = Anchor(runtimeAnchor, underTest)
        anchor.detach()
        check(underTest.anchorsToDetachQueue.contains(anchor))

        underTest.update()

        assertThat(underTest.anchorsToDetachQueue).isEmpty()
        assertThat(runtimeAnchor.isAttached).isFalse()
    }

    @Test
    fun update_earthUpdated() = doBlocking {
        val runtimeEarth = FakeRuntimeEarth()
        underTest.initiateEarth(runtimeEarth)
        underTest.update()
        check(underTest.earth.state.value == Earth.State.STOPPED)
        runtimeEarth.state = RuntimeEarth.State.RUNNING

        underTest.update()

        assertThat(underTest.earth.state.value).isEqualTo(Earth.State.RUNNING)
    }

    @Test
    fun update_updatesDepthMaps() = doBlocking {
        val runtimeDepthMap = FakeRuntimeDepthMap()
        underTest.initiateDepthMaps(listOf(runtimeDepthMap))
        underTest.update()
        check(underTest.depthMaps.size == 1)
        check(underTest.depthMaps[0].state.value.width == 0)
        val expectedWidth: Int = 100
        runtimeDepthMap.width = expectedWidth

        underTest.update()

        assertThat(underTest.depthMaps[0].state.value.width).isEqualTo(expectedWidth)
    }

    private fun createTestSessionAndRunTest(testBody: () -> Unit) {
        ActivityScenario.launch(ComponentActivity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, StandardTestDispatcher()) as SessionCreateSuccess)
                        .session
                underTest.lifecycleManager = session.runtime.lifecycleManager

                testBody()
            }
        }
    }
}
