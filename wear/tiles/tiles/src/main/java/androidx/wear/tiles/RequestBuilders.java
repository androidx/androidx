/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.proto.DeviceParametersProto;
import androidx.wear.protolayout.proto.StateProto;
import androidx.wear.tiles.proto.RequestProto;

import java.util.List;

/** Builders for request messages used to fetch tiles and resources. */
public final class RequestBuilders {
    private RequestBuilders() {}

    /**
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} Service when the
     * renderer is requesting a new version of the tile.
     *
     * @since 1.0
     */
    public static final class TileRequest {
        private final RequestProto.TileRequest mImpl;

        TileRequest(RequestProto.TileRequest impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the {@link DeviceParameters} describing the device requesting the tile update. If it
         * was not set, a default empty instance is returned.
         *
         * @since 1.0
         */
        @NonNull
        public DeviceParameters getDeviceConfiguration() {
            if (mImpl.hasDeviceConfiguration()) {
                return DeviceParameters.fromProto(mImpl.getDeviceConfiguration());
            } else {
                return DeviceParameters.fromProto(
                        DeviceParametersProto.DeviceParameters.getDefaultInstance());
            }
        }

        /**
         * Gets the {@link State} that should be used when building the tile.
         *
         * @since 1.0
         */
        @NonNull
        public State getCurrentState() {
            if (mImpl.hasCurrentState()) {
                return State.fromProto(mImpl.getCurrentState());
            } else {
                return State.fromProto(StateProto.State.getDefaultInstance());
            }
        }

        /**
         * Gets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
         * describing the device requesting the tile update.
         *
         * @since 1.0
         * @deprecated Use {@link #getDeviceConfiguration()} instead.
         */
        @Deprecated
        @Nullable
        @SuppressWarnings("deprecation") // for backward compatibility
        public androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters getDeviceParameters() {
            if (mImpl.hasDeviceConfiguration()) {
                return androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.fromProto(
                        mImpl.getDeviceConfiguration());
            } else {
                return null;
            }
        }

        /**
         * Gets the {@link androidx.wear.tiles.StateBuilders.State} that should be used when
         * building the tile.
         *
         * @since 1.0
         * @deprecated Use {@link #getCurrentState()} instead.
         */
        @Deprecated
        @Nullable
        @SuppressWarnings("deprecation") // for backward compatibility
        public androidx.wear.tiles.StateBuilders.State getState() {
            if (mImpl.hasCurrentState()) {
                return androidx.wear.tiles.StateBuilders.State.fromProto(mImpl.getCurrentState());
            } else {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileRequest fromProto(@NonNull RequestProto.TileRequest proto) {
            return new TileRequest(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public RequestProto.TileRequest toProto() {
            return mImpl;
        }

        /** Builder for {@link TileRequest} */
        public static final class Builder {
            private final RequestProto.TileRequest.Builder mImpl =
                    RequestProto.TileRequest.newBuilder();

            public Builder() {}

            /**
             * Sets a {@link DeviceParameters} object describing the device requesting the tile
             * update. If not set, a default empty instance is used.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setDeviceConfiguration(@NonNull DeviceParameters deviceConfiguration) {
                mImpl.setDeviceConfiguration(deviceConfiguration.toProto());
                return this;
            }

            /**
             * Sets the {@link State} that should be used when building the tile.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setCurrentState(@NonNull State currentState) {
                mImpl.setCurrentState(currentState.toProto());
                return this;
            }

            /**
             * Sets a {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
             * describing the device requesting the tile update.
             *
             * @since 1.0
             * @deprecated Use {@link setDeviceConfiguration(DeviceParameters)} instead.
             */
            @Deprecated
            @NonNull
            public Builder setDeviceParameters(
                    @NonNull
                            androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                    deviceParameters) {
                mImpl.setDeviceConfiguration(deviceParameters.toProto());
                return this;
            }

            /**
             * Sets the {@link androidx.wear.tiles.StateBuilders.State} that should be used when
             * building the tile.
             *
             * @since 1.0
             * @deprecated Use {@link setCurrentState(State)} instead.
             */
            @Deprecated
            @NonNull
            public Builder setState(@NonNull androidx.wear.tiles.StateBuilders.State state) {
                mImpl.setCurrentState(state.toProto());
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
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} Service when the
     * renderer is requesting a specific resource version.
     *
     * @since 1.0
     */
    public static final class ResourcesRequest {
        private final RequestProto.ResourcesRequest mImpl;

        ResourcesRequest(RequestProto.ResourcesRequest impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the version of the resources being fetched. This is the same as the requested
         * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
         *
         * @since 1.0
         */
        @NonNull
        public String getVersion() {
            return mImpl.getVersion();
        }

        /**
         * Gets requested resource IDs. This specifies which tile resources should be returned in
         * the {@link androidx.wear.protolayout.ResourceBuilders.Resources} response. If not
         * specified, all resources for the given version must be provided in the response.
         *
         * <p>Note that resource IDs here correspond to tile resources (i.e. keys that would be used
         * in {@link androidx.wear.protolayout.ResourceBuilders.Resources}.idToImage), not Android
         * resource names or similar.
         *
         * @since 1.0
         */
        @NonNull
        public List<String> getResourceIds() {
            return mImpl.getResourceIdsList();
        }

        /**
         * Gets the {@link DeviceParameters} object describing the device requesting the resources.
         * If it was not set, a default empty instance is returned.
         *
         * @since 1.0
         */
        @NonNull
        public DeviceParameters getDeviceConfiguration() {
            if (mImpl.hasDeviceConfiguration()) {
                return DeviceParameters.fromProto(mImpl.getDeviceConfiguration());
            } else {
                return DeviceParameters.fromProto(
                        DeviceParametersProto.DeviceParameters.getDefaultInstance());
            }
        }

        /**
         * Gets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
         * describing the device requesting the resources.
         *
         * @since 1.0
         * @deprecated Use {@link #getDeviceConfiguration()} instead.
         */
        @Deprecated
        @Nullable
        @SuppressWarnings("deprecation") // for backward compatibility
        public androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters getDeviceParameters() {
            if (mImpl.hasDeviceConfiguration()) {
                return androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.fromProto(
                        mImpl.getDeviceConfiguration());
            } else {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ResourcesRequest fromProto(@NonNull RequestProto.ResourcesRequest proto) {
            return new ResourcesRequest(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public RequestProto.ResourcesRequest toProto() {
            return mImpl;
        }

        /** Builder for {@link ResourcesRequest} */
        public static final class Builder {
            private final RequestProto.ResourcesRequest.Builder mImpl =
                    RequestProto.ResourcesRequest.newBuilder();

            public Builder() {}

            /**
             * Sets the version of the resources being fetched. This is the same as the requested
             * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setVersion(@NonNull String version) {
                mImpl.setVersion(version);
                return this;
            }

            /**
             * Adds one item to requested resource IDs. This specifies which tile resources should
             * be returned in the {@link androidx.wear.protolayout.ResourceBuilders.Resources}
             * response. If not specified, all resources for the given version must be provided in
             * the response.
             *
             * <p>Note that resource IDs here correspond to tile resources (i.e. keys that would be
             * used in {@link androidx.wear.protolayout.ResourceBuilders.Resources}.idToImage), not
             * Android resource names or similar.
             *
             * @since 1.0
             */
            @NonNull
            public Builder addResourceId(@NonNull String resourceId) {
                mImpl.addResourceIds(resourceId);
                return this;
            }

            /**
             * Sets a {@link DeviceParameters} object describing the device requesting the
             * resources. If not set, a default empty instance is used.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setDeviceConfiguration(@NonNull DeviceParameters deviceConfiguration) {
                mImpl.setDeviceConfiguration(deviceConfiguration.toProto());
                return this;
            }

            /**
             * Sets a {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
             * describing the device requesting the resources.
             *
             * @since 1.0
             * @deprecated Use {@link setDeviceConfiguration(DeviceParameters)} instead.
             */
            @Deprecated
            @NonNull
            @SuppressWarnings("deprecation") // for backward compatibility
            public Builder setDeviceParameters(
                    @NonNull
                            androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                    deviceParameters) {
                mImpl.setDeviceConfiguration(deviceParameters.toProto());
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
