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
import androidx.wear.tiles.TimelineBuilders.Timeline;
import androidx.wear.tiles.proto.TileProto;
import androidx.wear.tiles.proto.VersionProto.VersionInfo;

/** Builders for the components of a tile that can be rendered by a tile renderer. */
public final class TileBuilders {
    private TileBuilders() {}

    /**
     * A holder for a tile. This specifies the resources to use for this delivery of the tile, and
     * the timeline for the tile.
     */
    public static final class Tile {
        private final TileProto.Tile mImpl;

        private Tile(TileProto.Tile impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the resource version required for these tiles. This can be any developer-defined
         * string; it is only used to cache resources, and is passed in {@link
         * androidx.wear.tiles.RequestBuilders.ResourcesRequest} if the system does not have a copy
         * of the specified resource version. Intended for testing purposes only.
         */
        @NonNull
        public String getResourcesVersion() {
            return mImpl.getResourcesVersion();
        }

        /**
         * Gets the tiles to show in the carousel, along with their validity periods. Intended for
         * testing purposes only.
         */
        @Nullable
        public Timeline getTimeline() {
            if (mImpl.hasTimeline()) {
                return Timeline.fromProto(mImpl.getTimeline());
            } else {
                return null;
            }
        }

        /**
         * Gets how many milliseconds of elapsed time (**not** wall clock time) this tile can be
         * considered to be "fresh". The platform will attempt to refresh your tile at some point in
         * the future after this interval has lapsed. A value of 0 here signifies that
         * auto-refreshes should not be used (i.e. you will manually request updates via
         * TileProviderService#getRequester).
         *
         * <p>This mechanism should not be used to update your tile more frequently than once a
         * minute, and the system may throttle your updates if you request updates faster than this
         * interval. This interval is also inexact; the system will generally update your tile if it
         * is on-screen, or about to be on-screen, although this is not guaranteed due to
         * system-level optimizations. Intended for testing purposes only.
         */
        public long getFreshnessIntervalMillis() {
            return mImpl.getFreshnessIntervalMillis();
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Tile fromProto(@NonNull TileProto.Tile proto) {
            return new Tile(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TileProto.Tile toProto() {
            return mImpl;
        }

        /** Builder for {@link Tile} */
        public static final class Builder {
            private final TileProto.Tile.Builder mImpl = TileProto.Tile.newBuilder();

            Builder() {}

            /**
             * Sets the resource version required for these tiles. This can be any developer-defined
             * string; it is only used to cache resources, and is passed in {@link
             * androidx.wear.tiles.RequestBuilders.ResourcesRequest} if the system does not have a
             * copy of the specified resource version.
             */
            @NonNull
            public Builder setResourcesVersion(@NonNull String resourcesVersion) {
                mImpl.setResourcesVersion(resourcesVersion);
                return this;
            }

            /** Sets the tiles to show in the carousel, along with their validity periods. */
            @NonNull
            public Builder setTimeline(@NonNull Timeline timeline) {
                mImpl.setTimeline(timeline.toProto());
                return this;
            }

            /** Sets the tiles to show in the carousel, along with their validity periods. */
            @NonNull
            public Builder setTimeline(@NonNull Timeline.Builder timelineBuilder) {
                mImpl.setTimeline(timelineBuilder.build().toProto());
                return this;
            }

            /**
             * Sets how many milliseconds of elapsed time (**not** wall clock time) this tile can be
             * considered to be "fresh". The platform will attempt to refresh your tile at some
             * point in the future after this interval has lapsed. A value of 0 here signifies that
             * auto-refreshes should not be used (i.e. you will manually request updates via
             * TileProviderService#getRequester).
             *
             * <p>This mechanism should not be used to update your tile more frequently than once a
             * minute, and the system may throttle your updates if you request updates faster than
             * this interval. This interval is also inexact; the system will generally update your
             * tile if it is on-screen, or about to be on-screen, although this is not guaranteed
             * due to system-level optimizations.
             */
            @NonNull
            public Builder setFreshnessIntervalMillis(long freshnessIntervalMillis) {
                mImpl.setFreshnessIntervalMillis(freshnessIntervalMillis);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Tile build() {
                return Tile.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Utility class with the current version of the Tile schema in use.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final class Version {
        private Version() {}

        /** The current version of the Tiles schema in use. */
        public static final VersionInfo CURRENT =
                VersionInfo.newBuilder().setMajor(0).setMinor(1).build();
    }
}
