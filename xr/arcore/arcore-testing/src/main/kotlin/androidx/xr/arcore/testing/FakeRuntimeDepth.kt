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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Depth as RuntimeDepth
import java.nio.ByteBuffer
import java.nio.FloatBuffer

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [Depth][RuntimeDepth] for testing purposes. This should not be used to
 * unit test `Depth` APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun left_rawOnly_updatesRawDepthMap() {
 *     session.configure(Config(depthEstimation = DepthEstimationMode.RAW_ONLY))
 *
 *     runTest(testDispatcher) {
 *         val expectedWidth = 2
 *         val expectedHeight = 2
 *         arCoreTestRule.leftDepthMaps.width = expectedWidth
 *         arCoreTestRule.leftDepthMaps.height = expectedHeight
 *         advanceUntilIdle()
 *         val underTest = DepthMap.left(session)!!
 *         assertThat(underTest.state.value.width).isEqualTo(expectedWidth)
 *         assertThat(underTest.state.value.height).isEqualTo(expectedHeight)
 *     }
 * }
 * ```
 *
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimeDepth(
    override var width: Int = 0,
    override var height: Int = 0,
    override var rawDepthMap: FloatBuffer? = null,
    override var rawConfidenceMap: ByteBuffer? = null,
    override var smoothDepthMap: FloatBuffer? = null,
    override var smoothConfidenceMap: ByteBuffer? = null,
) : RuntimeDepth {}
