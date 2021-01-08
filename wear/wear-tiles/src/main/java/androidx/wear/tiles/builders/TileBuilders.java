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

package androidx.wear.tiles.builders;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.builders.TimelineBuilders.Timeline;
import androidx.wear.tiles.proto.TileProto;

/** Builders for the components of a tile that can be rendered by a tile renderer. */
public final class TileBuilders {
    private TileBuilders() {}

    /**
     * A holder for a tile. This specifies the resources to use for this delivery of the tile, and
     * the timeline for the tile.
     */
    public static final class Tile {
        private final TileProto.Tile mImpl;

        Tile(TileProto.Tile impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public TileProto.Tile toProto() {
            return mImpl;
        }

        /** Builder for {@link Tile} */
        public static final class Builder {
            private final TileProto.Tile.Builder mImpl = TileProto.Tile.newBuilder();

            Builder() {}

            /** Sets the resource version required for these tiles. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setResourcesVersion(@NonNull String resourcesVersion) {
                mImpl.setResourcesVersion(resourcesVersion);
                return this;
            }

            /** Sets the tiles to show in the carousel, along with their validity periods. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTimeline(@NonNull Timeline timeline) {
                mImpl.setTimeline(timeline.toProto());
                return this;
            }

            /** Sets the tiles to show in the carousel, along with their validity periods. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTimeline(@NonNull Timeline.Builder timelineBuilder) {
                mImpl.setTimeline(timelineBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Tile build() {
                return new Tile(mImpl.build());
            }
        }
    }
}
