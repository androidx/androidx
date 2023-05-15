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
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.TimelineBuilders.Timeline;
import androidx.wear.protolayout.expression.proto.VersionProto.VersionInfo;
import androidx.wear.tiles.proto.TileProto;

/** Builders for the components of a tile that can be rendered by a tile renderer. */
public final class TileBuilders {
    private TileBuilders() {}

    /**
     * A holder for a tile. This specifies the resources to use for this delivery of the tile, and
     * the timeline for the tile.
     *
     * @since 1.0
     */
    public static final class Tile {
        private final TileProto.Tile mImpl;

        Tile(TileProto.Tile impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the resource version required for these tiles. This can be any developer-defined
         * string; it is only used to cache resources, and is passed in {@link
         * androidx.wear.tiles.RequestBuilders.ResourcesRequest} if the system does not have a copy
         * of the specified resource version.
         *
         * @since 1.0
         */
        @NonNull
        public String getResourcesVersion() {
            return mImpl.getResourcesVersion();
        }

        /**
         * Gets a {@link Timeline} containing the layouts for the tiles to show in the carousel,
         * along with their validity periods.
         *
         * @since 1.0
         */
        @Nullable
        public Timeline getTileTimeline() {
            if (mImpl.hasTileTimeline()) {
                return Timeline.fromProto(mImpl.getTileTimeline());
            } else {
                return null;
            }
        }

        /**
         * Gets how many milliseconds of elapsed time (**not** wall clock time) this tile can be
         * considered to be "fresh". The platform will attempt to refresh your tile at some point in
         * the future after this interval has lapsed. A value of 0 here signifies that
         * auto-refreshes should not be used (i.e. you will manually request updates via
         * TileService#getRequester).
         *
         * <p>This mechanism should not be used to update your tile more frequently than once a
         * minute, and the system may throttle your updates if you request updates faster than this
         * interval. This interval is also inexact; the system will generally update your tile if it
         * is on-screen, or about to be on-screen, although this is not guaranteed due to
         * system-level optimizations.
         *
         * @since 1.0
         */
        public long getFreshnessIntervalMillis() {
            return mImpl.getFreshnessIntervalMillis();
        }

        /**
         * Gets {@link androidx.wear.protolayout.StateBuilders.State} for this tile.
         *
         * @since 1.2
         */
        @Nullable
        public State getState() {
            if (mImpl.hasState()) {
                return State.fromProto(mImpl.getState());
            } else {
                return null;
            }
        }

        /**
         * Gets a {@link androidx.wear.tiles.TimelineBuilders.Timeline} containing the layouts for
         * the tiles to show in the carousel, along with their validity periods.
         *
         * @since 1.0
         * @deprecated Use {@link #getTileTimeline()} instead.
         */
        @Deprecated
        @Nullable
        @SuppressWarnings("deprecation") // for backward compatibility
        public androidx.wear.tiles.TimelineBuilders.Timeline getTimeline() {
            if (mImpl.hasTileTimeline()) {
                return androidx.wear.tiles.TimelineBuilders.Timeline.fromProto(
                        mImpl.getTileTimeline());
            } else {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Tile fromProto(@NonNull TileProto.Tile proto) {
            return new Tile(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TileProto.Tile toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "Tile{"
                    + "resourcesVersion="
                    + getResourcesVersion()
                    + ", tileTimeline="
                    + getTileTimeline()
                    + ", freshnessIntervalMillis="
                    + getFreshnessIntervalMillis()
                    + ", state="
                    + getState()
                    + "}";
        }

        /** Builder for {@link Tile} */
        public static final class Builder {
            private final TileProto.Tile.Builder mImpl = TileProto.Tile.newBuilder();

            public Builder() {}

            /**
             * Sets the resource version required for these tiles. This can be any developer-defined
             * string; it is only used to cache resources, and is passed in {@link
             * androidx.wear.tiles.RequestBuilders.ResourcesRequest} if the system does not have a
             * copy of the specified resource version.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setResourcesVersion(@NonNull String resourcesVersion) {
                mImpl.setResourcesVersion(resourcesVersion);
                return this;
            }

            /**
             * Sets a {@link Timeline} containing the layouts for the tiles to show in the carousel,
             * along with their validity periods.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setTileTimeline(@NonNull Timeline tileTimeline) {
                mImpl.setTileTimeline(tileTimeline.toProto());
                return this;
            }

            /**
             * Sets how many milliseconds of elapsed time (**not** wall clock time) this tile can be
             * considered to be "fresh". The platform will attempt to refresh your tile at some
             * point in the future after this interval has lapsed. A value of 0 here signifies that
             * auto-refreshes should not be used (i.e. you will manually request updates via
             * TileService#getRequester).
             *
             * <p>This mechanism should not be used to update your tile more frequently than once a
             * minute, and the system may throttle your updates if you request updates faster than
             * this interval. This interval is also inexact; the system will generally update your
             * tile if it is on-screen, or about to be on-screen, although this is not guaranteed
             * due to system-level optimizations.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setFreshnessIntervalMillis(long freshnessIntervalMillis) {
                mImpl.setFreshnessIntervalMillis(freshnessIntervalMillis);
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.StateBuilders.State} for this tile.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setState(@NonNull State state) {
                mImpl.setState(state.toProto());
                return this;
            }

            /**
             * Sets a {@link androidx.wear.tiles.TimelineBuilders.Timeline} containing the layouts
             * for the tiles to show in the carousel, along with their validity periods.
             *
             * @since 1.0
             * @deprecated Use {@link #setTileTimeline(Timeline)} instead.
             */
            @Deprecated
            @NonNull
            public Builder setTimeline(
                    @NonNull androidx.wear.tiles.TimelineBuilders.Timeline timeline) {
                mImpl.setTileTimeline(timeline.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Tile build() {
                return Tile.fromProto(mImpl.build());
            }
        }
    }

    /** Utility class with the current version of the Tile schema in use. */
    @RestrictTo(Scope.LIBRARY)
    public static final class Version {
        private Version() {}

        /** The current version of the Tiles schema in use. */
        public static final VersionInfo CURRENT =
                VersionInfo.newBuilder().setMajor(1).setMinor(200).build();
    }
}
