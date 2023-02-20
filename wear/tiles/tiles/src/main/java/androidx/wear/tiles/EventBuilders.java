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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.proto.EventProto;

/** Builders for messages used when events happen in the Tiles system. */
public final class EventBuilders {
    private EventBuilders() {}

    /**
     * Event fired when a tile has been added to the carousel.
     *
     * @since 1.0
     */
    public static final class TileAddEvent {
        private final EventProto.TileAddEvent mImpl;

        TileAddEvent(EventProto.TileAddEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Creates a new wrapper instance from the proto.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileAddEvent fromProto(@NonNull EventProto.TileAddEvent proto) {
            return new TileAddEvent(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileAddEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileAddEvent{" + "}";
        }

        /** Builder for {@link TileAddEvent} */
        public static final class Builder {
            private final EventProto.TileAddEvent.Builder mImpl =
                    EventProto.TileAddEvent.newBuilder();

            public Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileAddEvent build() {
                return TileAddEvent.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Event fired when a tile has been removed from the carousel.
     *
     * @since 1.0
     */
    public static final class TileRemoveEvent {
        private final EventProto.TileRemoveEvent mImpl;

        TileRemoveEvent(EventProto.TileRemoveEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Creates a new wrapper instance from the proto.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileRemoveEvent fromProto(@NonNull EventProto.TileRemoveEvent proto) {
            return new TileRemoveEvent(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileRemoveEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileRemoveEvent{" + "}";
        }

        /** Builder for {@link TileRemoveEvent} */
        public static final class Builder {
            private final EventProto.TileRemoveEvent.Builder mImpl =
                    EventProto.TileRemoveEvent.newBuilder();

            public Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileRemoveEvent build() {
                return TileRemoveEvent.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Event fired when a tile is swiped to by the user (i.e. it's visible on screen).
     *
     * @since 1.0
     */
    public static final class TileEnterEvent {
        private final EventProto.TileEnterEvent mImpl;

        TileEnterEvent(EventProto.TileEnterEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Creates a new wrapper instance from the proto.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileEnterEvent fromProto(@NonNull EventProto.TileEnterEvent proto) {
            return new TileEnterEvent(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileEnterEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileEnterEvent{" + "}";
        }

        /** Builder for {@link TileEnterEvent} */
        public static final class Builder {
            private final EventProto.TileEnterEvent.Builder mImpl =
                    EventProto.TileEnterEvent.newBuilder();

            public Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileEnterEvent build() {
                return TileEnterEvent.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Event fired when a tile is swiped away from by the user (i.e. it's no longer visible on
     * screen).
     *
     * @since 1.0
     */
    public static final class TileLeaveEvent {
        private final EventProto.TileLeaveEvent mImpl;

        TileLeaveEvent(EventProto.TileLeaveEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Creates a new wrapper instance from the proto.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileLeaveEvent fromProto(@NonNull EventProto.TileLeaveEvent proto) {
            return new TileLeaveEvent(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileLeaveEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileLeaveEvent{" + "}";
        }

        /** Builder for {@link TileLeaveEvent} */
        public static final class Builder {
            private final EventProto.TileLeaveEvent.Builder mImpl =
                    EventProto.TileLeaveEvent.newBuilder();

            public Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileLeaveEvent build() {
                return TileLeaveEvent.fromProto(mImpl.build());
            }
        }
    }
}
