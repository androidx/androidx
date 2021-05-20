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

package androidx.wear.tiles.readers;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.TileAddEventData;
import androidx.wear.tiles.TileEnterEventData;
import androidx.wear.tiles.TileLeaveEventData;
import androidx.wear.tiles.TileRemoveEventData;
import androidx.wear.tiles.proto.EventProto;
import androidx.wear.tiles.protobuf.ExtensionRegistryLite;
import androidx.wear.tiles.protobuf.InvalidProtocolBufferException;

/** Event readers for androidx.wear.tiles' Parcelable classes. */
public class EventReaders {
    private EventReaders() {}

    /** Reader for Tile add event parameters. */
    public static class TileAddEvent {
        @SuppressWarnings("unused")
        private final EventProto.TileAddEvent mProto;

        private TileAddEvent(@NonNull EventProto.TileAddEvent proto) {
            this.mProto = proto;
        }

        /**
         * Create an instance of this reader from a given {@link TileAddEventData} instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileAddEvent fromParcelable(@NonNull TileAddEventData parcelable) {
            try {
                return new TileAddEvent(
                        EventProto.TileAddEvent.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileAddEventData did not contain a valid proto payload", ex);
            }
        }
    }

    /** Reader for Tile remove event parameters. */
    public static class TileRemoveEvent {
        @SuppressWarnings("unused")
        private final EventProto.TileRemoveEvent mProto;

        private TileRemoveEvent(@NonNull EventProto.TileRemoveEvent proto) {
            this.mProto = proto;
        }

        /**
         * Create an instance of this reader from a given {@link TileRemoveEventData} instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileRemoveEvent fromParcelable(@NonNull TileRemoveEventData parcelable) {
            try {
                return new TileRemoveEvent(
                        EventProto.TileRemoveEvent.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileRemoveEventData did not contain a valid proto payload", ex);
            }
        }
    }

    /** Reader for Tile enter event parameters. */
    public static class TileEnterEvent {
        @SuppressWarnings("unused")
        private final EventProto.TileEnterEvent mProto;

        private TileEnterEvent(@NonNull EventProto.TileEnterEvent proto) {
            this.mProto = proto;
        }

        /**
         * Create an instance of this reader from a given {@link TileEnterEventData} instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileEnterEvent fromParcelable(@NonNull TileEnterEventData parcelable) {
            try {
                return new TileEnterEvent(
                        EventProto.TileEnterEvent.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileEnterEventData did not contain a valid proto payload", ex);
            }
        }
    }

    /** Reader for a Tile leave event parameters. */
    public static class TileLeaveEvent {
        @SuppressWarnings("unused")
        private final EventProto.TileLeaveEvent mProto;

        private TileLeaveEvent(@NonNull EventProto.TileLeaveEvent proto) {
            this.mProto = proto;
        }

        /**
         * Create an instance of this reader from a given {@link TileLeaveEventData} instance.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public static TileLeaveEvent fromParcelable(@NonNull TileLeaveEventData parcelable) {
            try {
                return new TileLeaveEvent(
                        EventProto.TileLeaveEvent.parseFrom(
                                parcelable.getContents(),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (InvalidProtocolBufferException ex) {
                throw new IllegalArgumentException(
                        "Passed TileLeaveEventData did not contain a valid proto payload", ex);
            }
        }
    }
}
