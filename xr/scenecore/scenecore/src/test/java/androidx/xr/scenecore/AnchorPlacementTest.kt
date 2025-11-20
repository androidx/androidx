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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorPlacementTest {

    @Test
    fun sameAnchorPlacement_isEqual() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.VERTICAL),
                anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.TABLE),
            )

        assertThat(anchorPlacement).isEqualTo(anchorPlacement)
    }

    @Test
    fun equivalentAnchorPlacement_isEqual() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.VERTICAL),
                anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.TABLE),
            )
        val anchorPlacement2 =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.VERTICAL),
                anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.TABLE),
            )

        assertThat(anchorPlacement).isEqualTo(anchorPlacement2)
    }

    @Test
    fun differentAnchorPlacement_isNotEqual() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.VERTICAL),
                anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.TABLE),
            )
        val anchorPlacement2 =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.HORIZONTAL),
                anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.TABLE),
            )

        assertThat(anchorPlacement).isNotEqualTo(anchorPlacement2)
    }

    @Test
    fun anchorPlacementPlaneOrientations_matchesValuePassed() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations =
                    setOf(PlaneOrientation.VERTICAL, PlaneOrientation.HORIZONTAL)
            )

        assertThat(anchorPlacement.anchorablePlaneOrientations)
            .containsExactly(PlaneOrientation.VERTICAL, PlaneOrientation.HORIZONTAL)
    }

    @Test
    fun anchorPlacementPlaneSemanticTypes_matchesValuePassed() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneSemanticTypes =
                    setOf(
                        PlaneSemanticType.CEILING,
                        PlaneSemanticType.FLOOR,
                        PlaneSemanticType.WALL,
                    )
            )

        assertThat(anchorPlacement.anchorablePlaneSemanticTypes)
            .containsExactly(
                PlaneSemanticType.CEILING,
                PlaneSemanticType.FLOOR,
                PlaneSemanticType.WALL,
            )
    }

    @Test
    fun anchorPlacementAddingToSemanticTypesAfterCreation_doesNotUpdateAnchorPlacement() {
        val anchorSemanticSet: MutableSet<Int> =
            mutableSetOf(PlaneSemanticType.CEILING, PlaneSemanticType.FLOOR)
        val anchorPlacement =
            AnchorPlacement.createForPlanes(anchorablePlaneSemanticTypes = anchorSemanticSet)

        // Add an extra semantic type and verify that it is not added in the AnchorPlacement.
        anchorSemanticSet.add(PlaneSemanticType.WALL)

        assertThat(anchorPlacement.anchorablePlaneSemanticTypes)
            .containsExactly(PlaneSemanticType.CEILING, PlaneSemanticType.FLOOR)
    }

    @Test
    fun anchorPlacementAddingToOrientationsAfterCreation_doesNotUpdateAnchorPlacement() {
        val anchorOrientationSet: MutableSet<Int> = mutableSetOf(PlaneOrientation.VERTICAL)
        val anchorPlacement =
            AnchorPlacement.createForPlanes(anchorablePlaneOrientations = anchorOrientationSet)

        // Add an extra orientation and verify that it is not added in the AnchorPlacement.
        anchorOrientationSet.add(PlaneOrientation.HORIZONTAL)

        assertThat(anchorPlacement.anchorablePlaneOrientations)
            .containsExactly(PlaneOrientation.VERTICAL)
    }

    @Test
    fun anchorPlacementToString_returnsCorrectString() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = setOf(PlaneOrientation.VERTICAL),
                anchorablePlaneSemanticTypes =
                    setOf(PlaneSemanticType.CEILING, PlaneSemanticType.TABLE),
            )

        val anchorPlacementString = anchorPlacement.toString()

        assertThat(anchorPlacementString)
            .isEqualTo(
                "AnchorPlacement(anchorablePlaneOrientations=[1], anchorablePlaneSemanticTypes=[2, 3])"
            )
    }

    @Test
    fun emptyAnchorPlacementToString_returnsCorrectString() {
        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                anchorablePlaneOrientations = emptySet(),
                anchorablePlaneSemanticTypes = emptySet(),
            )

        val anchorPlacementString = anchorPlacement.toString()

        assertThat(anchorPlacementString)
            .isEqualTo(
                "AnchorPlacement(anchorablePlaneOrientations=[], anchorablePlaneSemanticTypes=[])"
            )
    }
}
