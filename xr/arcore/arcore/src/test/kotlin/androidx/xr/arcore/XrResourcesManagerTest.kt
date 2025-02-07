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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimeHand
import androidx.xr.runtime.testing.FakeRuntimePlane
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XrResourcesManagerTest {

    private lateinit var underTest: XrResourcesManager

    @Before
    fun setUp() {
        underTest = XrResourcesManager()
        FakeRuntimeAnchor.anchorsCreated = 0
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
    fun addUpdatable_addsUpdatable() {
        val anchor = Anchor(FakeRuntimeAnchor(Pose()), underTest)
        check(underTest.updatables.isEmpty())

        underTest.addUpdatable(anchor)

        assertThat(underTest.updatables).containsExactly(anchor)
    }

    @Test
    fun removeUpdatable_removesUpdatable() {
        val anchor = Anchor(FakeRuntimeAnchor(Pose()), underTest)
        underTest.addUpdatable(anchor)
        check(underTest.updatables.contains(anchor))
        check(underTest.updatables.size == 1)

        underTest.removeUpdatable(anchor)

        assertThat(underTest.updatables).isEmpty()
    }

    @Test
    fun clear_clearAllUpdatables() {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val runtimeAnchor2 = FakeRuntimeAnchor(Pose())
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
    fun clear_clearsAllTrackables() {
        underTest.syncTrackables(listOf(FakeRuntimePlane()))
        check(underTest.trackablesMap.isNotEmpty())

        underTest.clear()

        assertThat(underTest.trackablesMap).isEmpty()
    }

    @Test
    fun update_anchorDetached_andNotUpdated() = runTest {
        val runtimeAnchor = FakeRuntimePlane().createAnchor(Pose()) as FakeRuntimeAnchor
        check(runtimeAnchor.isAttached)
        val anchor = Anchor(runtimeAnchor, underTest)
        anchor.detach()
        check(underTest.anchorsToDetachQueue.contains(anchor))

        underTest.update()

        assertThat(underTest.anchorsToDetachQueue).isEmpty()
        assertThat(runtimeAnchor.isAttached).isFalse()
    }
}
