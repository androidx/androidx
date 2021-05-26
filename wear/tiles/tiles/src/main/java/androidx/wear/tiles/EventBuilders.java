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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.proto.EventProto;

/** Builders for messages used when events happen in the Tiles system. */
public final class EventBuilders {
    private EventBuilders() {}

    /** Event fired when a tile has been added to the carousel. */
    public static final class TileAddEvent {
        private final EventProto.TileAddEvent mImpl;

        private TileAddEvent(EventProto.TileAddEvent impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileAddEvent fromProto(@NonNull EventProto.TileAddEvent proto) {
            return new TileAddEvent(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileAddEvent toProto() {
            return mImpl;
        }

        /** Builder for {@link TileAddEvent} */
        public static final class Builder {
            private final EventProto.TileAddEvent.Builder mImpl =
                    EventProto.TileAddEvent.newBuilder();

            Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileAddEvent build() {
                return TileAddEvent.fromProto(mImpl.build());
            }
        }
    }

    /** Event fired when a tile has been removed from the carousel. */
    public static final class TileRemoveEvent {
        private final EventProto.TileRemoveEvent mImpl;

        private TileRemoveEvent(EventProto.TileRemoveEvent impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileRemoveEvent fromProto(@NonNull EventProto.TileRemoveEvent proto) {
            return new TileRemoveEvent(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileRemoveEvent toProto() {
            return mImpl;
        }

        /** Builder for {@link TileRemoveEvent} */
        public static final class Builder {
            private final EventProto.TileRemoveEvent.Builder mImpl =
                    EventProto.TileRemoveEvent.newBuilder();

            Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileRemoveEvent build() {
                return TileRemoveEvent.fromProto(mImpl.build());
            }
        }
    }

    /** Event fired when a tile is swiped to by the user (i.e. it's visible on screen). */
    public static final class TileEnterEvent {
        private final EventProto.TileEnterEvent mImpl;

        private TileEnterEvent(EventProto.TileEnterEvent impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileEnterEvent fromProto(@NonNull EventProto.TileEnterEvent proto) {
            return new TileEnterEvent(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileEnterEvent toProto() {
            return mImpl;
        }

        /** Builder for {@link TileEnterEvent} */
        public static final class Builder {
            private final EventProto.TileEnterEvent.Builder mImpl =
                    EventProto.TileEnterEvent.newBuilder();

            Builder() {}

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
     */
    public static final class TileLeaveEvent {
        private final EventProto.TileLeaveEvent mImpl;

        private TileLeaveEvent(EventProto.TileLeaveEvent impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileLeaveEvent fromProto(@NonNull EventProto.TileLeaveEvent proto) {
            return new TileLeaveEvent(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileLeaveEvent toProto() {
            return mImpl;
        }

        /** Builder for {@link TileLeaveEvent} */
        public static final class Builder {
            private final EventProto.TileLeaveEvent.Builder mImpl =
                    EventProto.TileLeaveEvent.newBuilder();

            Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileLeaveEvent build() {
                return TileLeaveEvent.fromProto(mImpl.build());
            }
        }
    }
}
