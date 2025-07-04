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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.ActivityPose
import androidx.xr.runtime.internal.ActivitySpace
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.HitTestResult
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeActivitySpaceTest {
    lateinit var underTest: FakeActivitySpace

    @Before
    fun setUp() {
        underTest = FakeActivitySpace()
    }

    @Test
    fun getBounds_callsOnBoundsChangedToUpdateBounds_returnsCorrectBounds() {
        // The default bound size is (100, 100, 100)
        check(underTest.bounds.width == 100.0f)
        check(underTest.bounds.height == 100.0f)
        check(underTest.bounds.depth == 100.0f)

        val expectedDimensions = Dimensions(100.0f, 200.0f, 300.0f)
        underTest.onBoundsChanged(expectedDimensions)

        assertThat(underTest.bounds).isEqualTo(expectedDimensions)
    }

    @Test
    fun addOnBoundsChangedListener_listenerAdded() {
        val listener = TestOnBoundsChangedListener()
        underTest.addOnBoundsChangedListener(listener)

        assertThat(underTest.onBoundsChangedListeners.count()).isEqualTo(2)
    }

    @Test
    fun removeOnBoundsChangedListener_listenerRemoved() {
        val listener = TestOnBoundsChangedListener()
        underTest.addOnBoundsChangedListener(listener)
        underTest.removeOnBoundsChangedListener(listener)

        assertThat(underTest.onBoundsChangedListeners.count()).isEqualTo(1)
    }

    @Test
    fun addOnBoundsChangedListener_callsOnBoundsChangedToIterateListeners_listenerCalled() {
        val listener = TestOnBoundsChangedListener()
        underTest.addOnBoundsChangedListener(listener)
        val expectedDimensions = Dimensions(100.0f, 200.0f, 300.0f)
        underTest.onBoundsChangedListeners.forEach { it.onBoundsChanged(expectedDimensions) }

        assertThat(listener.wasCalled).isTrue()
        assertThat(listener.receivedDimensions).isEqualTo(expectedDimensions)
    }

    @Test
    fun removeOnBoundsChangedListener_callsOnBoundsChangedToIterateListeners_listenerNotCalled() {
        val listener = TestOnBoundsChangedListener()
        underTest.addOnBoundsChangedListener(listener)
        underTest.removeOnBoundsChangedListener(listener)
        underTest.onBoundsChangedListeners.forEach {
            it.onBoundsChanged(Dimensions(100.0f, 200.0f, 300.0f))
        }

        assertThat(listener.wasCalled).isFalse()
        assertThat(listener.receivedDimensions).isNull()
    }

    @Test
    fun hitTestRelativeToActivityPose_returnsHitTestResult() {
        val distance = 2.0f
        val hitPosition = Vector3(1.0f, 2.0f, 3.0f)
        val surfaceNormal = Vector3(4.0f, 5.0f, 6.0f)
        val extensionsHitTestResult = HitTestResult(hitPosition, surfaceNormal, 1, distance)
        val hitTestFilter = ActivityPose.HitTestFilter.SELF_SCENE

        underTest.activitySpaceHitTestResult = extensionsHitTestResult
        val hitTestResult =
            underTest
                .hitTestRelativeToActivityPose(Vector3.One, Vector3.One, hitTestFilter, underTest)
                .get()

        assertThat(hitTestResult.distance).isEqualTo(distance)
        assertThat(hitTestResult.hitPosition).isEqualTo(hitPosition)
        assertThat(hitTestResult.surfaceNormal).isEqualTo(surfaceNormal)
        assertThat(hitTestResult.surfaceType)
            .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
    }

    @Test
    fun getRecommendedContentBoxInFullSpace_returnsRecommendedContentBoxInFullSpace() {
        check(
            underTest.recommendedContentBoxInFullSpace ==
                BoundingBox(
                    min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                    max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
                )
        )
    }

    private class TestOnBoundsChangedListener : ActivitySpace.OnBoundsChangedListener {
        var wasCalled = false
            private set // Make the setter private to control modification

        var receivedDimensions: Dimensions? = null
            private set

        override fun onBoundsChanged(bounds: Dimensions) {
            wasCalled = true
            receivedDimensions = bounds
        }
    }
}
