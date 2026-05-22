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

/**
 * A base [PreviewParameterProvider] for [WearWidgetParams] that provides standardized metadata for
 * Glance Wear widget previews in Android Studio.
 *
 * This class consolidates the logic for generating human-readable display names for preview
 * variants. It is intended to be subclassed by specific parameter providers that define the
 * sequence of configurations to preview.
 *
 * The implementation of [getDisplayName] ensures that each preview tab in the IDE clearly
 * identifies the widget type and its dimensions.
 *
 * @property values The sequence of [WearWidgetParams] configurations to be supplied to the preview.
 */
public abstract class BaseWidgetPreviewParams(override val values: Sequence<WearWidgetParams>) :
    PreviewParameterProvider<WearWidgetParams> {

    /**
     * Returns a descriptive name for the preview configuration at the specified [index].
     *
     * The returned string includes the widget's container type (Small or Large) and its specific
     * dimensions in DP, following the format: "{Type} Widget {Width}x{Height}dp". For example:
     * "Squircle Small 166x72dp".
     *
     * @param index The index of the parameter in the [values] sequence.
     * @return A human-readable label for the Android Studio preview tool.
     */
    override fun getDisplayName(index: Int): String {
        val param = values.elementAt(index)
        val type =
            when (param.containerType) {
                ContainerInfo.CONTAINER_TYPE_SMALL -> "Small"
                ContainerInfo.CONTAINER_TYPE_LARGE -> "Large"
                else -> "Unknown"
            }
        return "Squircle $type ${param.widthDp.toInt()}x${param.heightDp.toInt()}dp"
    }
}
