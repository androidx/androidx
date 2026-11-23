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

package androidx.glance.adaptive.wear.selection

import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.wear.selection.WearWidgetSurface.Companion.toWearWidgetSurface
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearWidgetSurfaceTest {

    @Test
    fun wearGlanceSurface_constantsAndTags_resolveCorrectly() {
        assertThat(WearWidgetSurface.SMALL.tag).isEqualTo("wear_widget_small")
        assertThat(WearWidgetSurface.SMALL.dimensions).isEqualTo(Dimensions(0, 0))
        assertThat(WearWidgetSurface.SMALL.containerType)
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)

        assertThat(WearWidgetSurface.LARGE.tag).isEqualTo("wear_widget_large")
        assertThat(WearWidgetSurface.LARGE.dimensions).isEqualTo(Dimensions(0, 0))
        assertThat(WearWidgetSurface.LARGE.containerType)
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(WearWidgetSurface.TILE_COMPAT.tag).isEqualTo("wear_widget_tile_compat")
        assertThat(WearWidgetSurface.TILE_COMPAT.dimensions).isEqualTo(Dimensions(0, 0))
        assertThat(WearWidgetSurface.TILE_COMPAT.containerType)
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_TILE_COMPAT)

        assertThat(WearWidgetSurface.ALL)
            .containsExactly(
                WearWidgetSurface.SMALL,
                WearWidgetSurface.LARGE,
                WearWidgetSurface.TILE_COMPAT,
            )
            .inOrder()
    }

    @Test
    fun withDimensions_propagatesDimensionsAndPreservesSurfaceEquality() {
        val placed = WearWidgetSurface.LARGE.withDimensions(dimensions = Dimensions(180, 120))

        assertThat(placed).isEqualTo(WearWidgetSurface.LARGE)
        assertThat(placed.hashCode()).isEqualTo(WearWidgetSurface.LARGE.hashCode())
        assertThat(placed).isNotEqualTo(WearWidgetSurface.SMALL)
        assertThat(placed.dimensions).isEqualTo(Dimensions(180, 120))
        assertThat(placed.containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
    }

    @Test
    fun fromContainerType_resolvesSurfaceAndDimensions() {
        val large =
            WearWidgetSurface.fromContainerType(
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                dimensions = Dimensions(180, 120),
            )
        assertThat(large).isEqualTo(WearWidgetSurface.LARGE)
        assertThat(large.dimensions).isEqualTo(Dimensions(180, 120))
        assertThat(large.containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)

        val smallDefault = WearWidgetSurface.fromContainerType(ContainerInfo.CONTAINER_TYPE_SMALL)
        assertThat(smallDefault).isSameInstanceAs(WearWidgetSurface.SMALL)
        assertThat(smallDefault.dimensions).isEqualTo(Dimensions(0, 0))
    }

    @Test
    @Suppress("RestrictedApi")
    fun toWearWidgetSurface_resolvesSurfaceAndRoundsDimensionsIncludingPadding() {
        val largeParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("test", 1),
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 192.8f,
                heightDp = 148.2f,
                horizontalPaddingDp = 12f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 24f,
            )
        val placedLarge = largeParams.toWearWidgetSurface()
        assertThat(placedLarge).isEqualTo(WearWidgetSurface.LARGE)
        assertThat(placedLarge.dimensions).isEqualTo(Dimensions(193, 148))
        assertThat(placedLarge.containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)

        val tileCompatParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("test", 2),
                containerType = ContainerInfo.CONTAINER_TYPE_TILE_COMPAT,
                widthDp = 225f,
                heightDp = 225f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        val placedTileCompat = tileCompatParams.toWearWidgetSurface()
        assertThat(placedTileCompat).isEqualTo(WearWidgetSurface.TILE_COMPAT)
        assertThat(placedTileCompat.dimensions).isEqualTo(Dimensions(225, 225))
        assertThat(placedTileCompat.containerType)
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_TILE_COMPAT)
    }

    @Test
    @Suppress("RestrictedApi")
    fun toWearWidgetSurface_withNonPositiveDimensions_normalizesToUnspecifiedDimensions() {
        val zeroWidthParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("test", 3),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 0f,
                heightDp = 100f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        assertThat(zeroWidthParams.toWearWidgetSurface().dimensions).isEqualTo(Dimensions(0, 0))

        val negativeHeightParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("test", 4),
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 180f,
                heightDp = -10f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        assertThat(negativeHeightParams.toWearWidgetSurface().dimensions)
            .isEqualTo(Dimensions(0, 0))
    }
}
