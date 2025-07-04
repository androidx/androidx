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

import androidx.xr.runtime.internal.SpatialPointerIcon
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSpatialPointerComponentTest {
    lateinit var underTest: FakeSpatialPointerComponent

    @Before
    fun setUp() {
        underTest = FakeSpatialPointerComponent()
    }

    @Test
    fun setSpatialPointerIcon_getSpatialPointerIconReturnsSetIcon() {
        // Default value.
        check(underTest.getSpatialPointerIcon() == SpatialPointerIcon.TYPE_NONE)

        underTest.setSpatialPointerIcon(SpatialPointerIcon.TYPE_CIRCLE)
        assertThat(underTest.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_CIRCLE)

        underTest.setSpatialPointerIcon(SpatialPointerIcon.TYPE_DEFAULT)
        assertThat(underTest.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT)
    }
}
