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

package androidx.xr.arcore.playservices

import androidx.xr.runtime.internal.Plane
import com.google.ar.core.Plane.Type as ARCore1xPlaneType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PlaneExtensionsTest {

    @Test
    fun fromARCorePlaneType_horizontalDownwardFacing_returnsHorizontalDownwardFacing() {
        assertTypeConversion(
            ARCore1xPlaneType.HORIZONTAL_DOWNWARD_FACING,
            Plane.Type.HORIZONTAL_DOWNWARD_FACING,
        )
    }

    @Test
    fun fromARCorePlaneType_horizontalUpwardFacing_returnsHorizontalUpwardFacing() {
        assertTypeConversion(
            ARCore1xPlaneType.HORIZONTAL_UPWARD_FACING,
            Plane.Type.HORIZONTAL_UPWARD_FACING,
        )
    }

    @Test
    fun fromARCorePlaneType_vertical_returnsVertical() {
        assertTypeConversion(ARCore1xPlaneType.VERTICAL, Plane.Type.VERTICAL)
    }

    private fun assertTypeConversion(arCoreType: ARCore1xPlaneType, planeType: Plane.Type) {
        assertThat(Plane.Type.fromArCoreType(arCoreType)).isEqualTo(planeType)
    }
}
