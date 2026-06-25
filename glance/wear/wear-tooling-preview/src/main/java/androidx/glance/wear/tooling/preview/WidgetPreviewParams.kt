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

package androidx.glance.wear.tooling.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId

private object WidgetPreviewConstants {
    const val WIDGETS_NAMESPACE = "tiles"
    const val SQUIRCLE_PREFIX = "Squircle"
    const val ROUND_PREFIX = "Round"
    const val CORNER_RADIUS_ROUND_DP = 999f
}

/**
 * A [PreviewParameterProvider] that provides standard configurations based on the default squircle
 * specifications from the wear widget host.
 *
 * These are default configurations with squircle shape provided by the Wear Widget Host, which may
 * be overridden by specific OEM specifications. The suite covers small and large screen variants
 * for both Small and Large widget types.
 */
public class SquircleAllWidgetPreviewParams :
    BaseWidgetPreviewParams(SQUIRCLE_ALL_PARAMS, WidgetPreviewConstants.SQUIRCLE_PREFIX)

/**
 * Provides Small-type widget configurations from the default squircle specification for both small
 * and large screens.
 */
public class SquircleSmallWidgetPreviewParams :
    BaseWidgetPreviewParams(SQUIRCLE_SMALL_PARAMS, WidgetPreviewConstants.SQUIRCLE_PREFIX)

/**
 * Provides Large-type widget configurations from the default squircle specification for both small
 * and large screens.
 */
public class SquircleLargeWidgetPreviewParams :
    BaseWidgetPreviewParams(SQUIRCLE_LARGE_PARAMS, WidgetPreviewConstants.SQUIRCLE_PREFIX)

/**
 * A [PreviewParameterProvider] that provides standard configurations based on the custom fully
 * rounded specification. This covers small and large screen variants for both Small and Large
 * widget types.
 */
public class RoundAllWidgetPreviewParams :
    BaseWidgetPreviewParams(ROUND_ALL_PARAMS, WidgetPreviewConstants.ROUND_PREFIX)

/**
 * Provides Small-type widget configurations from the default round specification for both small and
 * large screens.
 */
public class RoundSmallWidgetPreviewParams :
    BaseWidgetPreviewParams(ROUND_SMALL_PARAMS, WidgetPreviewConstants.ROUND_PREFIX)

/**
 * Provides Large-type widget configurations from the default round specification for both small and
 * large screens.
 */
public class RoundLargeWidgetPreviewParams :
    BaseWidgetPreviewParams(ROUND_LARGE_PARAMS, WidgetPreviewConstants.ROUND_PREFIX)

private val SQUIRCLE_SMALL_PARAMS =
    sequenceOf(
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 204dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 1),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 166f,
            heightDp = 60f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 240dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 2),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 200f,
            heightDp = 60f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
    )

private val SQUIRCLE_LARGE_PARAMS =
    sequenceOf(
        /**
         * A [WearWidgetParams] calculated with the default spec of large widget defined in renderer
         * on a 204dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 3),
            containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            widthDp = 166f,
            heightDp = 96f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
        /**
         * A [WearWidgetParams] calculated with the default spec of large widget defined in renderer
         * on a 240dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 4),
            containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            widthDp = 200f,
            heightDp = 108f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
    )

private val SQUIRCLE_ALL_PARAMS = SQUIRCLE_SMALL_PARAMS + SQUIRCLE_LARGE_PARAMS

private val ROUND_SMALL_PARAMS =
    sequenceOf(
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 216dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 5),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 182f,
            heightDp = 54f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 13f,
            cornerRadiusDp = WidgetPreviewConstants.CORNER_RADIUS_ROUND_DP,
        ),
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 240dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 6),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 200f,
            heightDp = 60f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 15f,
            cornerRadiusDp = WidgetPreviewConstants.CORNER_RADIUS_ROUND_DP,
        ),
    )

private val ROUND_LARGE_PARAMS =
    sequenceOf(
        /**
         * A [WearWidgetParams] calculated with the default spec of large widget defined in renderer
         * on a 216dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 7),
            containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            widthDp = 150f,
            heightDp = 120f,
            verticalPaddingDp = 16f,
            horizontalPaddingDp = 29f,
            cornerRadiusDp = WidgetPreviewConstants.CORNER_RADIUS_ROUND_DP,
        ),
        /**
         * A [WearWidgetParams] calculated with the default spec of large widget defined in renderer
         * on a 240dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WidgetPreviewConstants.WIDGETS_NAMESPACE, 8),
            containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            widthDp = 160f,
            heightDp = 136f,
            verticalPaddingDp = 16f,
            horizontalPaddingDp = 35f,
            cornerRadiusDp = WidgetPreviewConstants.CORNER_RADIUS_ROUND_DP,
        ),
    )

private val ROUND_ALL_PARAMS = ROUND_SMALL_PARAMS + ROUND_LARGE_PARAMS
