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

import androidx.glance.wear.parcel.WearWidgetRequestParcel
import androidx.glance.wear.proto.WearWidgetRequestProto
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WearWidgetParamsTest {

    @Test
    fun fromParcel_matchesOriginalParams() {
        val originalParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        val parcel = originalParams.toParcel()
        val restoredParams = WearWidgetParams.fromParcel(parcel)

        assertThat(restoredParams).isEqualTo(originalParams)
    }

    @Test
    fun equals_sameInstance() {
        val params =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params).isEqualTo(params)
    }

    @Test
    fun equals_sameValues() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isEqualTo(params2)
    }

    @Test
    fun equals_differentInstanceId() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 456),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentContainerType() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentWidthDp() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 100f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentHeightDp() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 100f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentHorizontalPaddingDp() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentVerticalPaddingDp() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentCornerRadiusDp() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 0f,
            )

        assertThat(params1).isNotEqualTo(params2)
    }

    @Test
    fun equals_differentType() {
        val params =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params).isNotEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)
    }

    @Test
    fun equals_null() {
        val params =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params).isNotEqualTo(null)
    }

    @Test
    fun hashCode_sameForEqualObjects() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1.hashCode()).isEqualTo(params2.hashCode())
    }

    @Test
    fun hashCode_differentForDifferentValues() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 456),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

        assertThat(params1.hashCode()).isNotEqualTo(params2.hashCode())
    }

    @Test
    fun fromParcel_matchesOriginalParams_withCustomRendererVersion() {
        val originalParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
                rendererVersion = RendererVersion(2, 5, 3),
            )

        val parcel = originalParams.toParcel()
        val restoredParams = WearWidgetParams.fromParcel(parcel)

        assertThat(restoredParams).isEqualTo(originalParams)
        assertThat(restoredParams.rendererVersion.major).isEqualTo(2)
        assertThat(restoredParams.rendererVersion.minor).isEqualTo(5)
        assertThat(restoredParams.rendererVersion.revision).isEqualTo(3)
    }

    @Test
    fun fromParcel_usesDefaultRendererVersion_whenNotProvided() {
        val payloadWithoutVersion =
            WearWidgetRequestProto(
                    id = 123,
                    id_namespace = "ns",
                    container_type = ContainerInfo.CONTAINER_TYPE_SMALL,
                    width_dp = 200.5f,
                    height_dp = 300.25f,
                    horizontal_padding_dp = 9f,
                    vertical_padding_dp = 8f,
                    corner_radius_dp = 16f,
                )
                .encode()
        val parcel = WearWidgetRequestParcel().apply { payload = payloadWithoutVersion }

        val restoredParams = WearWidgetParams.fromParcel(parcel)

        assertThat(restoredParams.rendererVersion.major)
            .isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_MAJOR)
        assertThat(restoredParams.rendererVersion.minor)
            .isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_MINOR)
        assertThat(restoredParams.rendererVersion.revision)
            .isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_REVISION)
    }

    @Test
    fun fromParcel_usesProvidedDefaultRendererVersion_whenNotProvided() {
        val payloadWithoutVersion =
            WearWidgetRequestProto(
                    id = 123,
                    id_namespace = "ns",
                    container_type = ContainerInfo.CONTAINER_TYPE_SMALL,
                    width_dp = 200.5f,
                    height_dp = 300.25f,
                    horizontal_padding_dp = 9f,
                    vertical_padding_dp = 8f,
                    corner_radius_dp = 16f,
                )
                .encode()
        val parcel = WearWidgetRequestParcel().apply { payload = payloadWithoutVersion }
        val customDefault = RendererVersion(5, 12, 99)

        val restoredParams =
            WearWidgetParams.fromParcel(parcel, getDefaultRendererVersion = { customDefault })

        assertThat(restoredParams.rendererVersion.major).isEqualTo(5)
        assertThat(restoredParams.rendererVersion.minor).isEqualTo(12)
        assertThat(restoredParams.rendererVersion.revision).isEqualTo(99)
    }

    @Test
    fun equals_differentRendererVersion() {
        val params1 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
                rendererVersion = RendererVersion(1, 6, 0),
            )
        val params2 =
            WearWidgetParams(
                instanceId = WidgetInstanceId("ns", 123),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 200.5f,
                heightDp = 300.25f,
                horizontalPaddingDp = 9f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
                rendererVersion = RendererVersion(2, 0, 0),
            )

        assertThat(params1).isNotEqualTo(params2)
    }
}
