/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.StateBuilders.State;
import androidx.wear.tiles.proto.RequestProto;

import java.util.List;

/** Builders for request messages used to fetch tiles and resources. */
public final class RequestBuilders {
    private RequestBuilders() {}

    /**
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} provider when the
     * renderer is requesting a new version of the tile.
     */
    public static final class TileRequest {
        private final RequestProto.TileRequest mImpl;

        private TileRequest(RequestProto.TileRequest impl) {
            this.mImpl = impl;
        }

        /** Gets parameters describing the device requesting the tile update. */
        @Nullable
        public DeviceParameters getDeviceParameters() {
            if (mImpl.hasDeviceParameters()) {
                return DeviceParameters.fromProto(mImpl.getDeviceParameters());
            } else {
                return null;
            }
        }

        /** Gets the state that should be used when building the tile. */
        @Nullable
        public State getState() {
            if (mImpl.hasState()) {
                return State.fromProto(mImpl.getState());
            } else {
                return null;
            }
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileRequest fromProto(@NonNull RequestProto.TileRequest proto) {
            return new TileRequest(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public RequestProto.TileRequest toProto() {
            return mImpl;
        }

        /** Builder for {@link TileRequest} */
        public static final class Builder {
            private final RequestProto.TileRequest.Builder mImpl =
                    RequestProto.TileRequest.newBuilder();

            Builder() {}

            /** Sets parameters describing the device requesting the tile update. */
            @NonNull
            public Builder setDeviceParameters(@NonNull DeviceParameters deviceParameters) {
                mImpl.setDeviceParameters(deviceParameters.toProto());
                return this;
            }

            /** Sets parameters describing the device requesting the tile update. */
            @NonNull
            public Builder setDeviceParameters(
                    @NonNull DeviceParameters.Builder deviceParametersBuilder) {
                mImpl.setDeviceParameters(deviceParametersBuilder.build().toProto());
                return this;
            }

            /** Sets the state that should be used when building the tile. */
            @NonNull
            public Builder setState(@NonNull State state) {
                mImpl.setState(state.toProto());
                return this;
            }

            /** Sets the state that should be used when building the tile. */
            @NonNull
            public Builder setState(@NonNull State.Builder stateBuilder) {
                mImpl.setState(stateBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileRequest build() {
                return TileRequest.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} provider when the
     * renderer is requesting a specific resource version.
     */
    public static final class ResourcesRequest {
        private final RequestProto.ResourcesRequest mImpl;

        private ResourcesRequest(RequestProto.ResourcesRequest impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the version of the resources being fetched. This is the same as the requested
         * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
         */
        @NonNull
        public String getVersion() {
            return mImpl.getVersion();
        }

        /**
         * Gets requested resource IDs. This specifies which tile resources should be returned in
         * the {@link androidx.wear.tiles.ResourceBuilders.Resources} response. If not specified,
         * all resources for the given version must be provided in the response.
         *
         * <p>Note that resource IDs here correspond to tile resources (i.e. keys that would be used
         * in {@link androidx.wear.tiles.ResourceBuilders.Resources}.idToImage), not Android
         * resource names or similar.
         */
        @NonNull
        public List<String> getResourceIds() {
            return mImpl.getResourceIdsList();
        }

        /**
         * Gets parameters describing the device requesting the resources. Intended for testing
         * purposes only.
         */
        @Nullable
        public DeviceParameters getDeviceParameters() {
            if (mImpl.hasDeviceParameters()) {
                return DeviceParameters.fromProto(mImpl.getDeviceParameters());
            } else {
                return null;
            }
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ResourcesRequest fromProto(@NonNull RequestProto.ResourcesRequest proto) {
            return new ResourcesRequest(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public RequestProto.ResourcesRequest toProto() {
            return mImpl;
        }

        /** Builder for {@link ResourcesRequest} */
        public static final class Builder {
            private final RequestProto.ResourcesRequest.Builder mImpl =
                    RequestProto.ResourcesRequest.newBuilder();

            Builder() {}

            /**
             * Sets the version of the resources being fetched. This is the same as the requested
             * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
             */
            @NonNull
            public Builder setVersion(@NonNull String version) {
                mImpl.setVersion(version);
                return this;
            }

            /**
             * Adds one item to requested resource IDs. This specifies which tile resources should
             * be returned in the {@link androidx.wear.tiles.ResourceBuilders.Resources} response.
             * If not specified, all resources for the given version must be provided in the
             * response.
             *
             * <p>Note that resource IDs here correspond to tile resources (i.e. keys that would be
             * used in {@link androidx.wear.tiles.ResourceBuilders.Resources}.idToImage), not
             * Android resource names or similar.
             */
            @NonNull
            public Builder addResourceId(@NonNull String resourceId) {
                mImpl.addResourceIds(resourceId);
                return this;
            }

            /** Sets parameters describing the device requesting the resources. */
            @NonNull
            public Builder setDeviceParameters(@NonNull DeviceParameters deviceParameters) {
                mImpl.setDeviceParameters(deviceParameters.toProto());
                return this;
            }

            /** Sets parameters describing the device requesting the resources. */
            @NonNull
            public Builder setDeviceParameters(
                    @NonNull DeviceParameters.Builder deviceParametersBuilder) {
                mImpl.setDeviceParameters(deviceParametersBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ResourcesRequest build() {
                return ResourcesRequest.fromProto(mImpl.build());
            }
        }
    }
}
