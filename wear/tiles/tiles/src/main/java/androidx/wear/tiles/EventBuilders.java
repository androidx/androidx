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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.tiles.proto.EventProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;

/** Builders for messages used when events happen in the Tiles system. */
public final class EventBuilders {
    private EventBuilders() {}

    /** Event fired when a tile has been added to the carousel. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TileAddEvent {
        private final EventProto.TileAddEvent mImpl;

        TileAddEvent(EventProto.TileAddEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the instance ID of the tile, allocated when the tile instance is added to the
         * carousel. This ID will remain the same for this tile instance as long it is not removed
         * from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileAddEvent fromProto(@NonNull EventProto.TileAddEvent proto) {
            return new TileAddEvent(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileAddEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileAddEvent{" + "tileId=" + getTileId() + "}";
        }

        /** Builder for {@link TileAddEvent} */
        public static final class Builder {
            private final EventProto.TileAddEvent.Builder mImpl =
                    EventProto.TileAddEvent.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets instance ID of the added tile, allocated when the tile instance was added to the
             * carousel. This ID will remain the same for this tile instance as long it is not
             * removed from the carousel.
             */
            @NonNull
            public Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileAddEvent build() {
                return TileAddEvent.fromProto(mImpl.build());
            }
        }
    }

    /** Event fired when a tile has been removed from the carousel. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TileRemoveEvent {
        private final EventProto.TileRemoveEvent mImpl;

        TileRemoveEvent(EventProto.TileRemoveEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the instance ID of the tile, allocated when the tile instance is added to the
         * carousel. This ID will remain the same for this tile instance as long it is not removed
         * from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileRemoveEvent fromProto(@NonNull EventProto.TileRemoveEvent proto) {
            return new TileRemoveEvent(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileRemoveEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileRemoveEvent{" + "tileId=" + getTileId() + "}";
        }

        /** Builder for {@link TileRemoveEvent} */
        public static final class Builder {
            private final EventProto.TileRemoveEvent.Builder mImpl =
                    EventProto.TileRemoveEvent.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets instance ID of the removed tile, allocated when the tile instance was added to
             * the carousel.
             */
            @NonNull
            public Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileRemoveEvent build() {
                return TileRemoveEvent.fromProto(mImpl.build());
            }
        }
    }

    /** Event fired when a tile is swiped to by the user (i.e. it's visible on screen). */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TileEnterEvent {
        private final EventProto.TileEnterEvent mImpl;

        TileEnterEvent(EventProto.TileEnterEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the instance ID of the tile, allocated when the tile instance is added to the
         * carousel. This ID will remain the same for this tile instance as long it is not removed
         * from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileEnterEvent fromProto(@NonNull EventProto.TileEnterEvent proto) {
            return new TileEnterEvent(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileEnterEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileEnterEvent{" + "tileId=" + getTileId() + "}";
        }

        /** Builder for {@link TileEnterEvent} */
        public static final class Builder {
            private final EventProto.TileEnterEvent.Builder mImpl =
                    EventProto.TileEnterEvent.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets instance ID of the tile, allocated when the tile instance is added to the
             * carousel. This ID will remain the same for this tile instance as long it is not
             * removed from the carousel.
             */
            @NonNull
            public Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TileLeaveEvent {
        private final EventProto.TileLeaveEvent mImpl;

        TileLeaveEvent(EventProto.TileLeaveEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the instance ID of the tile, allocated when the tile instance is added to the
         * carousel. This ID will remain the same for this tile instance as long it is not removed
         * from the carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileLeaveEvent fromProto(@NonNull EventProto.TileLeaveEvent proto) {
            return new TileLeaveEvent(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileLeaveEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileLeaveEvent{" + "tileId=" + getTileId() + "}";
        }

        /** Builder for {@link TileLeaveEvent} */
        public static final class Builder {
            private final EventProto.TileLeaveEvent.Builder mImpl =
                    EventProto.TileLeaveEvent.newBuilder();

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public Builder() {}

            /**
             * Sets instance ID of the tile, allocated when the tile instance is added to the
             * carousel. This ID will remain the same for this tile instance as long it is not
             * removed from the carousel.
             */
            @NonNull
            public Builder setTileId(int tileId) {
                mImpl.setTileId(tileId);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileLeaveEvent build() {
                return TileLeaveEvent.fromProto(mImpl.build());
            }
        }
    }

    /** Event reported when a user interacts with a tile (e.g. entering or leaving a tile). */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public static final class TileInteractionEvent {
        private final EventProto.TileInteractionEvent mImpl;

        TileInteractionEvent(EventProto.TileInteractionEvent impl) {
            this.mImpl = impl;
        }

        /**
         * Gets instance ID of the tile, allocated when the tile instance is added to the carousel.
         * This ID will remain the same for this tile instance as long it is not removed from the
         * carousel.
         */
        public int getTileId() {
            return mImpl.getTileId();
        }

        /** {@link TileInteractionEvent} type. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef({ENTER, LEAVE, UNKNOWN})
        @Retention(RetentionPolicy.SOURCE)
        public @interface EventType {}

        /** Unknown type */
        @EventType public static final int UNKNOWN = 0;

        /** User entered the tile. */
        @EventType public static final int ENTER = 1;

        /** User left the tile. */
        @EventType public static final int LEAVE = 2;

        /** Gets the type of the {@link TileInteractionEvent}. */
        @EventType
        public int getEventType() {
            if (mImpl.hasEnter()) {
                return ENTER;
            } else if (mImpl.hasLeave()) {
                return LEAVE;
            }
            return UNKNOWN;
        }

        /**
         * Gets the timestamp of when the interaction was reported. Defaults to {@link
         * Instant#now()} (Created at the time of {@link
         * TileInteractionEvent.Builder#Builder(int,int)} constructor call) if not provided.
         */
        @NonNull
        public Instant getTimestamp() {
            return Instant.ofEpochMilli(mImpl.getTimestampEpochMillis());
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TileInteractionEvent fromProto(
                @NonNull EventProto.TileInteractionEvent proto) {
            return new TileInteractionEvent(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public EventProto.TileInteractionEvent toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "TileInteractionEvent{"
                    + "tileId="
                    + getTileId()
                    + ", timestamp="
                    + getTimestamp()
                    + ", eventType="
                    + getEventType()
                    + "}";
        }

        /** Builder for {@link TileInteractionEvent} */
        public static final class Builder {
            @NonNull
            private final EventProto.TileInteractionEvent.Builder mImpl =
                    EventProto.TileInteractionEvent.newBuilder();

            /** Interface so this Builder can retrieve the current time. */
            @VisibleForTesting
            interface Clock {
                /** Get the current wall-clock time in millis. */
                long getCurrentTimeMillis();
            }

            /**
             * A builder for {@link TileInteractionEvent}.
             *
             * @param tileId the instance ID of the tile, allocated when the tile instance is added
             *     to the carousel. This ID will remain the same for this tile instance as long it
             *     is not removed from the carousel.
             * @param eventType the type of the {@link TileInteractionEvent}.
             * @throws IllegalArgumentException when the provided {@code eventType} is equal to
             *     {@link EventType#UNKNOWN} or not defined by {@link EventType}.
             */
            public Builder(int tileId, @EventType int eventType) {
                this(() -> Instant.now().toEpochMilli(), tileId, eventType);
            }

            /**
             * A builder for {@link TileInteractionEvent}.
             *
             * @param clock the clock providing current timestamp.
             * @param tileId the instance ID of the tile, allocated when the tile instance is added
             *     to the carousel. This ID will remain the same for this tile instance as long it
             *     is not removed from the carousel.
             * @param eventType the type of the {@link TileInteractionEvent}.
             * @throws IllegalArgumentException when the provided {@code eventType} is equal to
             *     {@link EventType#UNKNOWN} or not defined by {@link EventType}.
             */
            @VisibleForTesting
            Builder(@NonNull Clock clock, int tileId, @EventType int eventType) {
                mImpl.setTileId(tileId);
                mImpl.setTimestampEpochMillis(clock.getCurrentTimeMillis());
                switch (eventType) {
                    case ENTER:
                        mImpl.setEnter(EventProto.TileEnter.newBuilder().build());
                        break;
                    case LEAVE:
                        mImpl.setLeave(EventProto.TileLeave.newBuilder().build());
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Event type not supported: " + eventType);
                }
            }

            /**
             * Sets the timestamp of when the interaction was reported. Defaults to {@link
             * Instant#now()} (Created at the time of {@link Builder#Builder(int,int)} constructor
             * call) if not provided.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull
            public Builder setTimestamp(@NonNull Instant instant) {
                mImpl.setTimestampEpochMillis(instant.toEpochMilli());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TileInteractionEvent build() {
                return TileInteractionEvent.fromProto(mImpl.build());
            }
        }
    }
}
