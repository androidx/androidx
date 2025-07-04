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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.DeviceParametersProto;
import androidx.wear.protolayout.proto.StateProto;
import androidx.wear.tiles.proto.RequestProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/** Builders for request messages used to fetch tiles and resources. */
public final class RequestBuilders {
    private RequestBuilders() {}

    /**
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} Service when the
     * renderer is requesting a new version of the tile.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TileRequest {
        private final RequestProto.TileRequest mImpl;

        TileRequest(RequestProto.TileRequest impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the {@link androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters}
         * object describing the device requesting the tile update. If not set, a default empty
         * instance is used.
         */
        public @NonNull DeviceParameters getDeviceConfiguration() {
            if (mImpl.hasDeviceConfiguration()) {
                return DeviceParameters.fromProto(mImpl.getDeviceConfiguration());
            } else {
                return DeviceParameters.fromProto(
                        DeviceParametersProto.DeviceParameters.getDefaultInstance());
            }
        }

        /**
         * Gets the {@link androidx.wear.protolayout.StateBuilders.State} that should be used when
         * building the tile.
         */
        public @NonNull State getCurrentState() {
            if (mImpl.hasCurrentState()) {
                return State.fromProto(mImpl.getCurrentState());
            } else {
                return State.fromProto(StateProto.State.getDefaultInstance());
            }
        }

        /**
         * Gets the instance ID of the tile being requested, allocated when the tile instance is
         * added to the carousel. This ID will remain the same for this tile instance as long it is
         * not removed from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /**
         * Gets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
         * describing the device requesting the tile update.
         *
         * @deprecated Use {@link #getDeviceConfiguration()} instead.
         */
        @Deprecated
        public androidx.wear.tiles.DeviceParametersBuilders.@Nullable DeviceParameters
                getDeviceParameters() {
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
         * @deprecated Use {@link #getCurrentState()} instead.
         */
        @Deprecated
        public androidx.wear.tiles.StateBuilders.@Nullable State getState() {
            if (mImpl.hasCurrentState()) {
                return androidx.wear.tiles.StateBuilders.State.fromProto(mImpl.getCurrentState());
            } else {
                return null;
            }
        }

        /**
         * Gets the {@link Instant} representing the last time the tile was visible.
         *
         * <p>If the tile has never been visible, or the last time it was visible is not known, this
         * will return {@link Instant#EPOCH}.
         *
         * <p>The returned value is not persistent across reboots or when the tile is removed from
         * the carousel and added again.
         */
        @RequiresSchemaVersion(major = 1, minor = 600)
        public @NonNull Instant getLastVisibleTime() {
            return Instant.ofEpochMilli(mImpl.getLastVisibleMillis());
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull TileRequest fromProto(RequestProto.@NonNull TileRequest proto) {
            return new TileRequest(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public RequestProto.@NonNull TileRequest toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "TileRequest{"
                    + "deviceConfiguration="
                    + getDeviceConfiguration()
                    + ", currentState="
                    + getCurrentState()
                    + ", tileId="
                    + getTileId()
                    + "}";
        }

        /** Builder for {@link TileRequest} */
        public static final class Builder {
            private final RequestProto.TileRequest.Builder mImpl =
                    RequestProto.TileRequest.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets the {@link androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters}
             * object describing the device requesting the tile update. If not set, a default empty
             * instance is used.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setDeviceConfiguration(
                    @NonNull DeviceParameters deviceConfiguration) {
                mImpl.setDeviceConfiguration(deviceConfiguration.toProto());
                return this;
            }

            /**
             * Sets the {@link androidx.wear.protolayout.StateBuilders.State} that should be used
             * when building the tile.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setCurrentState(@NonNull State currentState) {
                mImpl.setCurrentState(currentState.toProto());
                return this;
            }

            /** Sets the ID of the tile being requested. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

            /**
             * Sets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters}
             * describing the device requesting the tile update.
             *
             * @deprecated Use {@link setDeviceConfiguration(DeviceParameters)} instead.
             */
            @Deprecated
            public @NonNull Builder setDeviceParameters(
                    androidx.wear.tiles.DeviceParametersBuilders.@NonNull DeviceParameters
                            deviceParameters) {
                mImpl.setDeviceConfiguration(deviceParameters.toProto());
                return this;
            }

            /**
             * Sets the {@link androidx.wear.tiles.StateBuilders.State} that should be used when
             * building the tile.
             *
             * @deprecated Use {@link setCurrentState(State)} instead.
             */
            @Deprecated
            public @NonNull Builder setState(
                    androidx.wear.tiles.StateBuilders.@NonNull State state) {
                mImpl.setCurrentState(state.toProto());
                return this;
            }

            /**
             * Sets the {@link Instant} representing the last time the tile was visible.
             *
             * <p>If not set, defaults to {@link Instant#EPOCH}.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public @NonNull Builder setLastVisibleTime(@NonNull Instant instant) {
                mImpl.setLastVisibleMillis(instant.toEpochMilli());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull TileRequest build() {
                return TileRequest.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Parameters passed to a {@link androidx.wear.tiles.TileBuilders.Tile} Service when the
     * renderer is requesting a specific resource version.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ResourcesRequest {
        private final RequestProto.ResourcesRequest mImpl;

        ResourcesRequest(RequestProto.ResourcesRequest impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the version of the resources being fetched. This is the same as the requested
         * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
         */
        public @NonNull String getVersion() {
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
         */
        public @NonNull List<String> getResourceIds() {
            return mImpl.getResourceIdsList();
        }

        /**
         * Gets the {@link androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters}
         * object describing the device requesting the resources.
         */
        public @NonNull DeviceParameters getDeviceConfiguration() {
            if (mImpl.hasDeviceConfiguration()) {
                return DeviceParameters.fromProto(mImpl.getDeviceConfiguration());
            } else {
                return DeviceParameters.fromProto(
                        DeviceParametersProto.DeviceParameters.getDefaultInstance());
            }
        }

        /**
         * Gets the instance ID of the tile for which resources are being requested, allocated when
         * the tile instance is added to the carousel. This ID will remain the same for this tile
         * instance as long it is not removed from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /**
         * Gets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters} object
         * describing the device requesting the resources.
         *
         * @deprecated Use {@link #getDeviceConfiguration()} instead.
         */
        @Deprecated
        public androidx.wear.tiles.DeviceParametersBuilders.@Nullable DeviceParameters
                getDeviceParameters() {
            if (mImpl.hasDeviceConfiguration()) {
                return androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.fromProto(
                        mImpl.getDeviceConfiguration());
            } else {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ResourcesRequest fromProto(
                RequestProto.@NonNull ResourcesRequest proto) {
            return new ResourcesRequest(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public RequestProto.@NonNull ResourcesRequest toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ResourcesRequest{"
                    + "version="
                    + getVersion()
                    + ", resourceIds="
                    + getResourceIds()
                    + ", deviceConfiguration="
                    + getDeviceConfiguration()
                    + ", tileId="
                    + getTileId()
                    + "}";
        }

        /** Builder for {@link ResourcesRequest} */
        public static final class Builder {
            private final RequestProto.ResourcesRequest.Builder mImpl =
                    RequestProto.ResourcesRequest.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets the version of the resources being fetched. This is the same as the requested
             * resource version, passed in {@link androidx.wear.tiles.TileBuilders.Tile}.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVersion(@NonNull String version) {
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
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addResourceId(@NonNull String resourceId) {
                mImpl.addResourceIds(resourceId);
                return this;
            }

            /**
             * Sets the {@link androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters}
             * object describing the device requesting the resources.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setDeviceConfiguration(
                    @NonNull DeviceParameters deviceConfiguration) {
                mImpl.setDeviceConfiguration(deviceConfiguration.toProto());
                return this;
            }

            /** Sets the ID of the tile for which resources are being requested. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

            /**
             * Sets the {@link androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters}
             * describing the device requesting the resources.
             *
             * @deprecated Use {@link setDeviceConfiguration(DeviceParameters)} instead.
             */
            @Deprecated
            public @NonNull Builder setDeviceParameters(
                    androidx.wear.tiles.DeviceParametersBuilders.@NonNull DeviceParameters
                            deviceParameters) {
                mImpl.setDeviceConfiguration(deviceParameters.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ResourcesRequest build() {
                return ResourcesRequest.fromProto(mImpl.build());
            }
        }
    }
}
