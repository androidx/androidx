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
    fun withContainerType_matchesOriginalParamsExceptModified() {
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

        val modifiedParams =
            originalParams.withContainerType(containerType = ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(modifiedParams).isNotEqualTo(originalParams)
        assertThat(modifiedParams.containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
        assertThat(modifiedParams.instanceId).isEqualTo(originalParams.instanceId)
        assertThat(modifiedParams.widthDp).isEqualTo(originalParams.widthDp)
        assertThat(modifiedParams.heightDp).isEqualTo(originalParams.heightDp)
        assertThat(modifiedParams.horizontalPaddingDp).isEqualTo(originalParams.horizontalPaddingDp)
        assertThat(modifiedParams.verticalPaddingDp).isEqualTo(originalParams.verticalPaddingDp)
        assertThat(modifiedParams.cornerRadiusDp).isEqualTo(originalParams.cornerRadiusDp)
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
}
