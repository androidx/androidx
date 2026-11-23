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

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import java.util.Collections
import kotlin.math.roundToInt

/**
 * Describes standard Wear OS glanceable surfaces on which Glance Adaptive widgets can be placed.
 *
 * @property containerType Wear OS widget container type from [ContainerInfo] (such as
 *   [ContainerInfo.CONTAINER_TYPE_SMALL], [ContainerInfo.CONTAINER_TYPE_LARGE], or
 *   [ContainerInfo.CONTAINER_TYPE_TILE_COMPAT]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WearWidgetSurface
private constructor(
    tag: String,
    @param:ContainerInfo.ContainerType
    @get:ContainerInfo.ContainerType
    public val containerType: Int,
    dimensions: Dimensions = UNSPECIFIED_DIMENSIONS,
) : GlanceSurface(tag, dimensions) {

    /**
     * Returns this surface placed at [dimensions], for the selector and renderer of one placement.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun withDimensions(dimensions: Dimensions): WearWidgetSurface =
        WearWidgetSurface(tag, containerType, dimensions)

    public companion object {
        private val UNSPECIFIED_DIMENSIONS = Dimensions(0, 0)

        /** Small Wear OS widget container surface ([ContainerInfo.CONTAINER_TYPE_SMALL]). */
        @JvmField
        public val SMALL: WearWidgetSurface =
            WearWidgetSurface(
                tag = "wear_widget_small",
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )

        /** Large Wear OS widget container surface ([ContainerInfo.CONTAINER_TYPE_LARGE]). */
        @JvmField
        public val LARGE: WearWidgetSurface =
            WearWidgetSurface(
                tag = "wear_widget_large",
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            )

        /**
         * Tile-compatibility full-screen Wear OS widget container surface
         * ([ContainerInfo.CONTAINER_TYPE_TILE_COMPAT]).
         */
        @JvmField
        public val TILE_COMPAT: WearWidgetSurface =
            WearWidgetSurface(
                tag = "wear_widget_tile_compat",
                containerType = ContainerInfo.CONTAINER_TYPE_TILE_COMPAT,
            )

        internal val ALL: List<WearWidgetSurface> =
            Collections.unmodifiableList(listOf(SMALL, LARGE, TILE_COMPAT))

        /**
         * Resolves the [WearWidgetSurface] for the given [containerType] and optional [dimensions].
         */
        internal fun fromContainerType(
            @ContainerInfo.ContainerType containerType: Int,
            dimensions: Dimensions = UNSPECIFIED_DIMENSIONS,
        ): WearWidgetSurface {
            val surface =
                when (containerType) {
                    ContainerInfo.CONTAINER_TYPE_LARGE -> LARGE
                    ContainerInfo.CONTAINER_TYPE_TILE_COMPAT -> TILE_COMPAT
                    ContainerInfo.CONTAINER_TYPE_SMALL -> SMALL
                    else -> SMALL
                }
            return if (dimensions == UNSPECIFIED_DIMENSIONS) {
                surface
            } else {
                surface.withDimensions(dimensions)
            }
        }

        /**
         * Resolves the [WearWidgetSurface] for [containerType][WearWidgetParams.containerType]
         * placed at the container dimensions (including padding) from this [WearWidgetParams].
         */
        internal fun WearWidgetParams.toWearWidgetSurface(): WearWidgetSurface {
            val width = widthDp.roundToInt()
            val height = heightDp.roundToInt()
            val dimensions =
                if (width > 0 && height > 0) {
                    Dimensions(widthDp = width, heightDp = height)
                } else {
                    UNSPECIFIED_DIMENSIONS
                }
            return fromContainerType(containerType = containerType, dimensions = dimensions)
        }
    }
}
