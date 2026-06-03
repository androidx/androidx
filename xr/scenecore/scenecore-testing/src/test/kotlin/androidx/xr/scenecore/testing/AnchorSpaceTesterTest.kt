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

package androidx.xr.scenecore.testing

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorSpace
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnchorSpaceTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var anchor: Anchor

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session

        val anchorPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion.Identity)
        anchor = (Anchor.create(session, anchorPose) as AnchorCreateSuccess).anchor
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val tester1 = AnchorSpaceTester.create(anchorSpace)
        val tester2 = AnchorSpaceTester.create(anchorSpace)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun state_getsAndSetsStateCorrectlyAndTriggersListener() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val tester = testRule.createTester<AnchorSpaceTester>(anchorSpace)
        var capturedState: AnchorSpace.State? = null
        anchorSpace.addStateChangedListener { capturedState = it }

        tester.state = AnchorSpace.State.ANCHORED
        ShadowLooper.idleMainLooper()

        assertThat(tester.state).isEqualTo(AnchorSpace.State.ANCHORED)
        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.ANCHORED)
        assertThat(capturedState).isEqualTo(AnchorSpace.State.ANCHORED)

        tester.state = AnchorSpace.State.UNANCHORED
        ShadowLooper.idleMainLooper()

        assertThat(tester.state).isEqualTo(AnchorSpace.State.UNANCHORED)
        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
        assertThat(capturedState).isEqualTo(AnchorSpace.State.UNANCHORED)
    }

    @Test
    fun onOriginChanged_triggersListener() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val tester = testRule.createTester<AnchorSpaceTester>(anchorSpace)
        var listenerCalled = false
        anchorSpace.addOriginChangedListener { listenerCalled = true }

        tester.triggerOnOriginChanged()
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalled).isTrue()
    }
}
