/*
 * Copyright 2021-2022 The Android Open Source Project
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
import androidx.wear.protolayout.proto.TimelineProto;
import androidx.wear.tiles.LayoutElementBuilders.Layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builders for a timeline with entries representing content that should be displayed within given
 * time intervals.
 */
public final class TimelineBuilders {
    private TimelineBuilders() {}

    /**
     * A time interval, typically used to describe the validity period of a {@link TimelineEntry}.
     */
    public static final class TimeInterval {
        private final TimelineProto.TimeInterval mImpl;

        private TimeInterval(TimelineProto.TimeInterval impl) {
            this.mImpl = impl;
        }

        /**
         * Gets starting point of the time interval, in milliseconds since the Unix epoch. Intended
         * for testing purposes only.
         */
        public long getStartMillis() {
            return mImpl.getStartMillis();
        }

        /**
         * Gets end point of the time interval, in milliseconds since the Unix epoch. Intended for
         * testing purposes only.
         */
        public long getEndMillis() {
            return mImpl.getEndMillis();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TimeInterval fromProto(@NonNull TimelineProto.TimeInterval proto) {
            return new TimeInterval(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.TimeInterval toProto() {
            return mImpl;
        }

        /** Builder for {@link TimeInterval} */
        public static final class Builder {
            private final TimelineProto.TimeInterval.Builder mImpl =
                    TimelineProto.TimeInterval.newBuilder();

            public Builder() {}

            /** Sets starting point of the time interval, in milliseconds since the Unix epoch. */
            @NonNull
            public Builder setStartMillis(long startMillis) {
                mImpl.setStartMillis(startMillis);
                return this;
            }

            /** Sets end point of the time interval, in milliseconds since the Unix epoch. */
            @NonNull
            public Builder setEndMillis(long endMillis) {
                mImpl.setEndMillis(endMillis);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TimeInterval build() {
                return TimeInterval.fromProto(mImpl.build());
            }
        }
    }

    /** One piece of renderable content along with the time that it is valid for. */
    public static final class TimelineEntry {
        private final TimelineProto.TimelineEntry mImpl;

        private TimelineEntry(TimelineProto.TimelineEntry impl) {
            this.mImpl = impl;
        }

        /** Gets the validity period for this timeline entry. Intended for testing purposes only. */
        @Nullable
        public TimeInterval getValidity() {
            if (mImpl.hasValidity()) {
                return TimeInterval.fromProto(mImpl.getValidity());
            } else {
                return null;
            }
        }

        /** Gets the contents of this timeline entry. Intended for testing purposes only. */
        @Nullable
        public Layout getLayout() {
            if (mImpl.hasLayout()) {
                return Layout.fromProto(mImpl.getLayout());
            } else {
                return null;
            }
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TimelineEntry fromProto(@NonNull TimelineProto.TimelineEntry proto) {
            return new TimelineEntry(proto);
        }

        /** Returns the {@link TimelineEntry} object containing the given layout element. */
        @NonNull
        public static TimelineEntry fromLayoutElement(
                @NonNull LayoutElementBuilders.LayoutElement layoutElement) {
            return new Builder().setLayout(Layout.fromLayoutElement(layoutElement)).build();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.TimelineEntry toProto() {
            return mImpl;
        }

        /** Builder for {@link TimelineEntry} */
        public static final class Builder {
            private final TimelineProto.TimelineEntry.Builder mImpl =
                    TimelineProto.TimelineEntry.newBuilder();

            public Builder() {}

            /** Sets the validity period for this timeline entry. */
            @NonNull
            public Builder setValidity(@NonNull TimeInterval validity) {
                mImpl.setValidity(validity.toProto());
                return this;
            }

            /** Sets the contents of this timeline entry. */
            @NonNull
            public Builder setLayout(@NonNull Layout layout) {
                mImpl.setLayout(layout.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TimelineEntry build() {
                return TimelineEntry.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A collection of {@link TimelineEntry} items.
     *
     * <p>{@link TimelineEntry} items can be used to update a layout on-screen at known times,
     * without having to explicitly update a layout. This allows for cases where, say, a calendar
     * can be used to show the next event, and automatically switch to showing the next event when
     * one has passed.
     *
     * <p>The active {@link TimelineEntry} is switched, at most, once a minute. In the case where
     * the validity periods of {@link TimelineEntry} items overlap, the item with the shortest*
     * validity period will be shown. This allows a layout provider to show a "default" layout, and
     * override it at set points without having to explicitly insert the default layout between the
     * "override" layout.
     */
    public static final class Timeline {
        private final TimelineProto.Timeline mImpl;

        private Timeline(TimelineProto.Timeline impl) {
            this.mImpl = impl;
        }

        /** Gets the entries in a timeline. Intended for testing purposes only. */
        @NonNull
        public List<TimelineEntry> getTimelineEntries() {
            List<TimelineEntry> list = new ArrayList<>();
            for (TimelineProto.TimelineEntry item : mImpl.getTimelineEntriesList()) {
                list.add(TimelineEntry.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Timeline fromProto(@NonNull TimelineProto.Timeline proto) {
            return new Timeline(proto);
        }

        /** Returns the {@link Timeline} object containing the given layout element. */
        @NonNull
        public static Timeline fromLayoutElement(
                @NonNull LayoutElementBuilders.LayoutElement layoutElement) {
            return new Builder()
                    .addTimelineEntry(TimelineEntry.fromLayoutElement(layoutElement))
                    .build();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.Timeline toProto() {
            return mImpl;
        }

        /** Builder for {@link Timeline} */
        public static final class Builder {
            private final TimelineProto.Timeline.Builder mImpl =
                    TimelineProto.Timeline.newBuilder();

            public Builder() {}

            /** Adds one item to the entries in a timeline. */
            @NonNull
            public Builder addTimelineEntry(@NonNull TimelineEntry timelineEntry) {
                mImpl.addTimelineEntries(timelineEntry.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Timeline build() {
                return Timeline.fromProto(mImpl.build());
            }
        }
    }
}
