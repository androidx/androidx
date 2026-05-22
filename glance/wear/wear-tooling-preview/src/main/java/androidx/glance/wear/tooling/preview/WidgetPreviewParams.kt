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

private const val WIDGETS_NAMESPACE = "tiles"

/**
 * A [PreviewParameterProvider] that provides standard configurations based on the default squircle
 * specifications from the wear widget host.
 *
 * These are default configurations with squircle shape provided by the Wear Widget Host, which may
 * be overridden by specific OEM specifications. The suite covers small and large screen variants
 * for both Small and Large widget types.
 */
public class SquircleAllWidgetPreviewParams : BaseWidgetPreviewParams(SQUIRCLE_ALL_PARAMS)

/**
 * Provides Small-type widget configurations from the default squircle specification for both small
 * and large screens.
 */
public class SquircleSmallWidgetPreviewParams : BaseWidgetPreviewParams(SQUIRCLE_SMALL_PARAMS)

/**
 * Provides Large-type widget configurations from the default squircle specification for both small
 * and large screens.
 */
public class SquircleLargeWidgetPreviewParams : BaseWidgetPreviewParams(SQUIRCLE_LARGE_PARAMS)

private val SQUIRCLE_SMALL_PARAMS =
    sequenceOf(
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 204dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WIDGETS_NAMESPACE, 1),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 166f,
            heightDp = 72f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
        /**
         * A [WearWidgetParams] calculated with the default spec of small widget defined in renderer
         * on a 240dp screen.
         */
        WearWidgetParams(
            instanceId = WidgetInstanceId(WIDGETS_NAMESPACE, 2),
            containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 199f,
            heightDp = 72f,
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
            instanceId = WidgetInstanceId(WIDGETS_NAMESPACE, 3),
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
            instanceId = WidgetInstanceId(WIDGETS_NAMESPACE, 4),
            containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
            widthDp = 199f,
            heightDp = 112f,
            verticalPaddingDp = 8f,
            horizontalPaddingDp = 8f,
            cornerRadiusDp = 26f,
        ),
    )

private val SQUIRCLE_ALL_PARAMS = SQUIRCLE_SMALL_PARAMS + SQUIRCLE_LARGE_PARAMS
