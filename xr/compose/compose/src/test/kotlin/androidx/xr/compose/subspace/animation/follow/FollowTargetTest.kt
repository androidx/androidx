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

package androidx.xr.compose.subspace.animation.follow

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FollowTargetTest {

    @get:Rule val arCoreTestRule = ArCoreTestRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var session: Session

    @Before
    fun setUp(): Unit = runBlocking {
        testDispatcher = StandardTestDispatcher()
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = activityController.get()
        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)
        activityController.create().start().resume()

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session
        session.configure(
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        )
    }

    private fun createAnchor(): Anchor {
        val result = Anchor.create(session, Pose.Identity)
        return (result as AnchorCreateSuccess).anchor
    }

    @Test
    fun viewTarget_equals_sameInstance_returnsTrue() {
        val target = FollowTarget.view(FollowMode.soft())

        assertThat(target).isEqualTo(target)
    }

    @Test
    fun viewTarget_equals_sameMode_returnsTrue() {
        val target1 = FollowTarget.view(FollowMode.soft())
        val target2 = FollowTarget.view(FollowMode.soft())

        assertThat(target1).isEqualTo(target2)
    }

    @Test
    fun viewTarget_equals_differentMode_returnsFalse() {
        val target1 = FollowTarget.view(FollowMode.soft())
        val target2 = FollowTarget.view(FollowMode.tight())

        assertThat(target1).isNotEqualTo(target2)
    }

    @Test
    fun viewTarget_equals_nullOrDifferentType_returnsFalse() {
        val target = FollowTarget.view(FollowMode.soft())

        assertFalse(target.equals(null))
        assertFalse(target.equals("Dummy String"))
    }

    @Test
    fun viewTarget_hashCode_sameMode_matches() {
        val target1 = FollowTarget.view(FollowMode.soft())
        val target2 = FollowTarget.view(FollowMode.soft())

        assertThat(target1.hashCode()).isEqualTo(target2.hashCode())
    }

    @Test
    fun viewTarget_hashCode_differentMode_differs() {
        val target1 = FollowTarget.view(FollowMode.soft())
        val target2 = FollowTarget.view(FollowMode.tight())

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
    }

    @Test
    fun anchorTarget_equals_sameInstance_returnsTrue() =
        runTest(testDispatcher) {
            val anchor = createAnchor()
            val target = FollowTarget.anchor(anchor, FollowMode.tight())

            assertThat(target).isEqualTo(target)
        }

    @Test
    fun anchorTarget_equals_sameAnchorAndMode_returnsTrue() =
        runTest(testDispatcher) {
            val anchor = createAnchor()
            val target1 = FollowTarget.anchor(anchor, FollowMode.tight())
            val target2 = FollowTarget.anchor(anchor, FollowMode.tight())

            assertThat(target1).isEqualTo(target2)
        }

    @Test
    fun anchorTarget_equals_differentAnchor_returnsFalse() =
        runTest(testDispatcher) {
            val anchor1 = createAnchor()
            val anchor2 = createAnchor()
            val target1 = FollowTarget.anchor(anchor1, FollowMode.tight())
            val target2 = FollowTarget.anchor(anchor2, FollowMode.tight())

            assertThat(target1).isNotEqualTo(target2)
        }

    @Test
    fun anchorTarget_equals_differentMode_returnsFalse() =
        runTest(testDispatcher) {
            val anchor = createAnchor()
            val target1 = FollowTarget.anchor(anchor, FollowMode.tight())
            val target2 = FollowTarget.anchor(anchor, FollowMode.soft())

            assertThat(target1).isNotEqualTo(target2)
        }

    @Test
    fun anchorTarget_equals_nullOrDifferentType_returnsFalse() =
        runTest(testDispatcher) {
            val anchor = createAnchor()
            val target = FollowTarget.anchor(anchor, FollowMode.tight())

            assertFalse(target.equals(null))
            assertFalse(target.equals("Dummy String"))
        }

    @Test
    fun anchorTarget_hashCode_sameAnchorAndMode_matches() =
        runTest(testDispatcher) {
            val anchor = createAnchor()
            val target1 = FollowTarget.anchor(anchor, FollowMode.tight())
            val target2 = FollowTarget.anchor(anchor, FollowMode.tight())

            assertThat(target1.hashCode()).isEqualTo(target2.hashCode())
        }

    @Test
    fun anchorTarget_hashCode_differentAnchorOrMode_differs() =
        runTest(testDispatcher) {
            val anchor1 = createAnchor()
            val anchor2 = createAnchor()
            val target1 = FollowTarget.anchor(anchor1, FollowMode.tight())
            val target2 = FollowTarget.anchor(anchor2, FollowMode.tight())
            val target3 = FollowTarget.anchor(anchor1, FollowMode.soft())

            assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
            assertThat(target1.hashCode()).isNotEqualTo(target3.hashCode())
        }
}
