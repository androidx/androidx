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

package androidx.xr.scenecore.testing

import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FakeAnchorPlacementTest {
    @Test
    fun constructor_setsPlaneTypeFilterAndPlaneSemanticFilter() {
        val planeTypeFilter = setOf(PlaneType.HORIZONTAL, PlaneType.VERTICAL)
        val planeSemanticFilter = setOf(PlaneSemantic.WALL, PlaneSemantic.FLOOR)

        val fakeAnchorPlacement = FakeAnchorPlacement(planeTypeFilter, planeSemanticFilter)

        assertThat(fakeAnchorPlacement.planeTypeFilter).isEqualTo(planeTypeFilter)
        assertThat(fakeAnchorPlacement.planeSemanticFilter).isEqualTo(planeSemanticFilter)
    }
}
