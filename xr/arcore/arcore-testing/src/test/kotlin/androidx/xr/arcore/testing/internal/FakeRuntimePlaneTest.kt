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

package androidx.xr.arcore.testing.internal

import androidx.kruth.assertThat
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimePlaneTest {

    private lateinit var underTest: FakeRuntimePlane

    @Test
    fun createAnchor_addsAndReturnsAnchor() {
        underTest = FakeRuntimePlane()

        val anchor = underTest.createAnchor(Pose())

        assertThat(underTest.anchors).containsExactly(anchor)
    }

    @Test
    fun createAnchor_composesAnchorPose() {
        underTest = FakeRuntimePlane()
        val planePose = Pose(Vector3.Forward, Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        val anchorPose = Pose(Vector3.Right, Quaternion(4.0f, 3.0f, 2.0f, 1.0f))
        underTest.centerPose = planePose

        val anchor = underTest.createAnchor(anchorPose)

        assertThat(anchor.pose).isEqualTo(planePose.compose(anchorPose))
    }

    @Test
    fun detachAnchor_removesAnchor() {
        underTest = FakeRuntimePlane()
        val anchor = underTest.createAnchor(Pose())
        check(underTest.anchors.contains(anchor))

        underTest.detachAnchor(anchor)

        assertThat(underTest.anchors).doesNotContain(anchor)
    }
}
