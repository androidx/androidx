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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeRuntimeDepthMap
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DepthMapTest {

    @Test
    fun update_stateMatchesRuntimeDepthMap() = runBlocking {
        val runtimeDepthMap = FakeRuntimeDepthMap()
        val underTest = DepthMap(runtimeDepthMap)
        check(underTest.state.value.width == 0)
        check(underTest.state.value.height == 0)
        check(underTest.state.value.rawDepthMap == null)
        check(underTest.state.value.rawConfidenceMap == null)
        check(underTest.state.value.smoothDepthMap == null)
        check(underTest.state.value.smoothConfidenceMap == null)

        val expectedWidth = 2
        val expectedHeight = 2
        val expectedRawDepthMap: FloatBuffer =
            FloatBuffer.allocate(expectedWidth * expectedHeight)
                .put(floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f))
        val expectedRawConfidenceMap: ByteBuffer =
            ByteBuffer.allocate(expectedWidth * expectedHeight).put(byteArrayOf(2, 3, 4, 5))
        val expectedSmoothDepthMap: FloatBuffer =
            FloatBuffer.allocate(expectedWidth * expectedHeight)
                .put(floatArrayOf(10.0f, 11.0f, 12.0f, 13.0f))
        val expectedSmoothConfidenceMap: ByteBuffer =
            ByteBuffer.allocate(expectedWidth * expectedHeight).put(byteArrayOf(11, 12, 13, 14))
        runtimeDepthMap.width = expectedWidth
        runtimeDepthMap.height = expectedHeight
        runtimeDepthMap.rawDepthMap = expectedRawDepthMap
        runtimeDepthMap.rawConfidenceMap = expectedRawConfidenceMap
        runtimeDepthMap.smoothDepthMap = expectedSmoothDepthMap
        runtimeDepthMap.smoothConfidenceMap = expectedSmoothConfidenceMap
        underTest.update()

        assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
        assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
        assertThat(underTest.state.value.rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(underTest.state.value.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
        assertThat(underTest.state.value.smoothDepthMap).isEqualTo(expectedSmoothDepthMap)
        assertThat(underTest.state.value.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
    }
}
