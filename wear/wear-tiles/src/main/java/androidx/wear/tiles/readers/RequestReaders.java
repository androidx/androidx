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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ResourcesRequestData;
import androidx.wear.tiles.TileRequestData;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.readers.DeviceParametersReaders.DeviceParameters;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;

/** Request readers for androidx.wear.tiles' Parcelable classes. */
public class RequestReaders {
    private RequestReaders() {}

    /** Reader for a {@link TileRequestData} instance. */
    public static class TileRequest {
        private final RequestProto.TileRequest mProto;

        private TileRequest(RequestProto.TileRequest proto) {
            this.mProto = proto;
        }

        /** Get the Clickable ID which triggered this Tile request. */
        @NonNull
        public String getClickableId() {
            return mProto.getClickableId();
        }

        /** Get parameters describing the device requesting this tile. */
        @Nullable
        public DeviceParameters getDeviceParameters() {
            if (!mProto.hasDeviceParameters()) {
                return null;
            }

            return new DeviceParameters(mProto.getDeviceParameters());
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileRequest fromParcelable(@NonNull TileRequestData parcelable) {
            try {
                return new TileRequest(
                        RequestProto.TileRequest.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileRequestData did not contain a valid proto payload", ex);
            }
        }
    }

    /** Reader for a {@link ResourcesRequestData} instance. */
    public static class ResourcesRequest {
        private final RequestProto.ResourcesRequest mProto;

        private ResourcesRequest(@NonNull RequestProto.ResourcesRequest proto) {
            this.mProto = proto;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static ResourcesRequest fromParcelable(@NonNull ResourcesRequestData parcelable) {
            try {
                return new ResourcesRequest(
                        RequestProto.ResourcesRequest.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed ResourcesRequestData did not contain a valid proto payload", ex);
            }
        }

        /** Get the resource version requested by this {@link ResourcesRequestData}. */
        @NonNull
        public String getVersion() {
            return mProto.getVersion();
        }

        /**
         * Get the resource IDs requested by this {@link ResourcesRequestData}. May be empty, in
         * which case all resources should be returned.
         */
        @NonNull
        public List<String> getResourceIds() {
            return mProto.getResourceIdsList();
        }
    }
}
