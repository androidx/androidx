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

package androidx.glance.wear.core

import androidx.annotation.Dimension
import androidx.annotation.RestrictTo
import androidx.glance.wear.parcel.WearWidgetRequestParcel
import androidx.glance.wear.proto.WearWidgetRequestProto

/**
 * The parameters used for providing data for a Wear Widget.
 *
 * @property instanceId The instance id of the widget for this request. The id is created by the
 *   system and is provided when [GlanceWearWidget.onActivated] is called.
 * @property containerType The container type being requested. See
 *   [androidx.glance.wear.ContainerInfo].
 * @property widthDp The width in dp of the content for this widget.
 * @property heightDp The height in dp of the content for this widget.
 */
public class WearWidgetParams
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    public val instanceId: WidgetInstanceId,
    @param:ContainerInfo.ContainerType
    @get:ContainerInfo.ContainerType
    public val containerType: Int,
    @param:Dimension(unit = Dimension.Companion.DP)
    @get:Dimension(unit = Dimension.Companion.DP)
    public val widthDp: Float,
    @param:Dimension(unit = Dimension.Companion.DP)
    @get:Dimension(unit = Dimension.Companion.DP)
    public val heightDp: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @param:Dimension(unit = Dimension.Companion.DP)
    @get:Dimension(unit = Dimension.Companion.DP)
    public val horizontalPaddingDp: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @param:Dimension(unit = Dimension.Companion.DP)
    @get:Dimension(unit = Dimension.Companion.DP)
    public val verticalPaddingDp: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @param:Dimension(unit = Dimension.Companion.DP)
    @get:Dimension(unit = Dimension.Companion.DP)
    public val cornerRadiusDp: Float,
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun withContainerType(containerType: Int = this.containerType): WearWidgetParams {
        return WearWidgetParams(
            instanceId = instanceId,
            containerType = containerType,
            widthDp = widthDp,
            heightDp = heightDp,
            horizontalPaddingDp = horizontalPaddingDp,
            verticalPaddingDp = verticalPaddingDp,
            cornerRadiusDp = cornerRadiusDp,
        )
    }

    /** Converts this object to [androidx.glance.wear.parcel.WearWidgetRequestParcel]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toParcel(): WearWidgetRequestParcel {
        val requestProto =
            WearWidgetRequestProto(
                id = instanceId.id,
                id_namespace = instanceId.namespace,
                container_type = containerType,
                width_dp = widthDp,
                height_dp = heightDp,
                horizontal_padding_dp = horizontalPaddingDp,
                vertical_padding_dp = verticalPaddingDp,
                corner_radius_dp = cornerRadiusDp,
            )
        return WearWidgetRequestParcel().apply { payload = requestProto.encode() }
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromParcel(parcel: WearWidgetRequestParcel): WearWidgetParams {
            val requestProto = WearWidgetRequestProto.Companion.ADAPTER.decode(parcel.payload)
            return WearWidgetParams(
                instanceId =
                    WidgetInstanceId(namespace = requestProto.id_namespace, requestProto.id),
                containerType = requestProto.container_type,
                widthDp = requestProto.width_dp,
                heightDp = requestProto.height_dp,
                horizontalPaddingDp = requestProto.horizontal_padding_dp,
                verticalPaddingDp = requestProto.vertical_padding_dp,
                cornerRadiusDp = requestProto.corner_radius_dp,
            )
        }
    }
}
