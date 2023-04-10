/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window.layout

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.core.Bounds
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [HardwareFoldingFeature] class.  */
@SmallTest
@RunWith(AndroidJUnit4::class)
class HardwareFoldingFeatureTest {

    @Test
    fun testConstructorBoundsAndType() {
        val bounds = Bounds(0, 10, 30, 10)
        val type = HINGE
        val state = HALF_OPENED
        val feature = HardwareFoldingFeature(bounds, type, state)
        assertEquals(bounds.toRect(), feature.bounds)
        assertEquals(type, feature.type)
        assertEquals(state, feature.state)
    }
}
