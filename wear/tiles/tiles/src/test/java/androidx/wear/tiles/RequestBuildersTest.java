/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.DeviceParametersBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.expression.StateEntryBuilders.StateEntryValue;
import androidx.wear.protolayout.expression.proto.FixedProto;
import androidx.wear.protolayout.expression.proto.StateEntryProto;
import androidx.wear.protolayout.proto.DeviceParametersProto;
import androidx.wear.protolayout.proto.StateProto;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.proto.RequestProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public final class RequestBuildersTest {
    @Test
    public void canBuildBasicTileRequest() {
        // Build the tile request using the RequestBuilders wrapper library.
        TileRequest tileRequest =
                new TileRequest.Builder()
                        .setCurrentState(
                                new State.Builder()
                                        .addIdToValueMapping(
                                                "entry_id", StateEntryValue.fromInt(13))
                                        .build())
                        .setDeviceConfiguration(
                                new DeviceParameters.Builder()
                                        .setDevicePlatform(
                                                DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.TileRequest protoTileRequest = buildBasicProtoTileRequest();

        assertThat(tileRequest.toProto()).isEqualTo(protoTileRequest);
    }

    @Test
    public void canBuildBasicTileRequest_compatibleDeviceConfiguration() {
        TileRequest tileRequest =
                new TileRequest.Builder()
                        .setDeviceParameters(
                                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                                .Builder()
                                        .setDevicePlatform(
                                                androidx.wear.tiles.DeviceParametersBuilders
                                                        .DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.TileRequest protoTileRequest = buildBasicProtoTileRequest();

        assertThat(tileRequest.toProto().getDeviceConfiguration())
                .isEqualTo(protoTileRequest.getDeviceConfiguration());
    }

    @Test
    public void canBuildBasicResourcesRequest() {
        // Build the tile request using the RequestBuilders wrapper library.
        ResourcesRequest resourcesRequest =
                new ResourcesRequest.Builder()
                        .addResourceId("resource_id_1")
                        .addResourceId("resource_id_2")
                        .setVersion("some_version")
                        .setDeviceConfiguration(
                                new DeviceParameters.Builder()
                                        .setDevicePlatform(
                                                DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.ResourcesRequest protoResourcesRequest = buildBasicProtoResourcesRequest();

        assertThat(resourcesRequest.toProto()).isEqualTo(protoResourcesRequest);
    }

    @Test
    public void canBuildBasicResourcesRequest_compatibleDeviceConfiguration() {
        ResourcesRequest resourcesRequest =
                new ResourcesRequest.Builder()
                        .addResourceId("resource_id_1")
                        .addResourceId("resource_id_2")
                        .setVersion("some_version")
                        .setDeviceParameters(
                                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                                .Builder()
                                        .setDevicePlatform(
                                                androidx.wear.tiles.DeviceParametersBuilders
                                                        .DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.ResourcesRequest protoResourcesRequest = buildBasicProtoResourcesRequest();

        assertThat(resourcesRequest.toProto()).isEqualTo(protoResourcesRequest);
    }

    private RequestProto.TileRequest buildBasicProtoTileRequest() {
        return RequestProto.TileRequest.newBuilder()
                .setCurrentState(
                        StateProto.State.newBuilder()
                                .putIdToValue(
                                        "entry_id",
                                        StateEntryProto.StateEntryValue.newBuilder()
                                                .setInt32Val(
                                                        FixedProto.FixedInt32.newBuilder()
                                                                .setValue(13))
                                                .build()))
                .setDeviceConfiguration(
                        DeviceParametersProto.DeviceParameters.newBuilder()
                                .setDevicePlatform(
                                        DeviceParametersProto.DevicePlatform
                                                .DEVICE_PLATFORM_WEAR_OS))
                .build();
    }

    private RequestProto.ResourcesRequest buildBasicProtoResourcesRequest() {
        return RequestProto.ResourcesRequest.newBuilder()
                .addResourceIds("resource_id_1")
                .addResourceIds("resource_id_2")
                .setVersion("some_version")
                .setDeviceConfiguration(
                        DeviceParametersProto.DeviceParameters.newBuilder()
                                .setDevicePlatform(
                                        DeviceParametersProto.DevicePlatform
                                                .DEVICE_PLATFORM_WEAR_OS))
                .build();
    }
}
