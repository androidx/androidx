/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.tiles.readers;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ResourcesRequestData;
import androidx.wear.tiles.StateBuilders.State;
import androidx.wear.tiles.TileRequestData;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.protobuf.ExtensionRegistryLite;
import androidx.wear.tiles.protobuf.InvalidProtocolBufferException;
import androidx.wear.tiles.readers.DeviceParametersReaders.DeviceParameters;

import java.util.List;

/** Request readers for androidx.wear.tiles' Parcelable classes. */
public class RequestReaders {
    private RequestReaders() {}

    /** Reader for Tile request parameters. */
    public static class TileRequest {
        private final RequestProto.TileRequest mProto;

        @SuppressWarnings("unused")
        private final int mTileId;

        private TileRequest(RequestProto.TileRequest proto, int tileId) {
            this.mProto = proto;
            this.mTileId = tileId;
        }

        /** Get the {@link State} that the tile should be built with. */
        @NonNull
        public State getState() {
            return State.fromProto(mProto.getState());
        }

        /** Get parameters describing the device requesting this tile. */
        @NonNull
        public DeviceParameters getDeviceParameters() {
            return new DeviceParameters(mProto.getDeviceParameters());
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileRequest fromParcelable(@NonNull TileRequestData parcelable, int tileId) {
            try {
                return new TileRequest(
                        RequestProto.TileRequest.parseFrom(
                                parcelable.getContents(), ExtensionRegistryLite.getEmptyRegistry()),
                        tileId);
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileRequestData did not contain a valid proto payload", ex);
            }
        }
    }

    /** Reader for resource request parameters. */
    public static class ResourcesRequest {
        private final RequestProto.ResourcesRequest mProto;

        @SuppressWarnings("unused")
        private final int mTileId;

        private ResourcesRequest(@NonNull RequestProto.ResourcesRequest proto, int tileId) {
            this.mProto = proto;
            this.mTileId = tileId;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static ResourcesRequest fromParcelable(
                @NonNull ResourcesRequestData parcelable, int tileId) {
            try {
                return new ResourcesRequest(
                        RequestProto.ResourcesRequest.parseFrom(
                                parcelable.getContents(), ExtensionRegistryLite.getEmptyRegistry()),
                        tileId);
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed ResourcesRequestData did not contain a valid proto payload", ex);
            }
        }

        /** Get the requested resource version. */
        @NonNull
        public String getVersion() {
            return mProto.getVersion();
        }

        /**
         * Get the requested resource IDs. May be empty, in which case all resources should be
         * returned.
         */
        @NonNull
        public List<String> getResourceIds() {
            return mProto.getResourceIdsList();
        }

        /** Get parameters describing the device requesting these resources. */
        @NonNull
        public DeviceParameters getDeviceParameters() {
            return new DeviceParameters(mProto.getDeviceParameters());
        }
    }
}
