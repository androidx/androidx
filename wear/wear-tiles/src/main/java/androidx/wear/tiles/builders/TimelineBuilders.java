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
import androidx.wear.tiles.builders.LayoutElementBuilders.Layout;
import androidx.wear.tiles.proto.TimelineProto;

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

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TimeInterval fromProto(@NonNull TimelineProto.TimeInterval proto) {
            return new TimeInterval(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.TimeInterval toProto() {
            return mImpl;
        }

        /** Builder for {@link TimeInterval} */
        public static final class Builder {
            private final TimelineProto.TimeInterval.Builder mImpl =
                    TimelineProto.TimeInterval.newBuilder();

            Builder() {}

            /** Sets starting point of the time interval, in milliseconds since the Unix epoch. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStartMillis(long startMillis) {
                mImpl.setStartMillis(startMillis);
                return this;
            }

            /** Sets end point of the time interval, in milliseconds since the Unix epoch. */
            @SuppressLint("MissingGetterMatchingBuilder")
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

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static TimelineEntry fromProto(@NonNull TimelineProto.TimelineEntry proto) {
            return new TimelineEntry(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.TimelineEntry toProto() {
            return mImpl;
        }

        /** Builder for {@link TimelineEntry} */
        public static final class Builder {
            private final TimelineProto.TimelineEntry.Builder mImpl =
                    TimelineProto.TimelineEntry.newBuilder();

            Builder() {}

            /** Sets the validity period for this timeline entry. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValidity(@NonNull TimeInterval validity) {
                mImpl.setValidity(validity.toProto());
                return this;
            }

            /** Sets the validity period for this timeline entry. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValidity(@NonNull TimeInterval.Builder validityBuilder) {
                mImpl.setValidity(validityBuilder.build().toProto());
                return this;
            }

            /** Sets the contents of this timeline entry. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLayout(@NonNull Layout layout) {
                mImpl.setLayout(layout.toProto());
                return this;
            }

            /** Sets the contents of this timeline entry. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLayout(@NonNull Layout.Builder layoutBuilder) {
                mImpl.setLayout(layoutBuilder.build().toProto());
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

        Timeline(TimelineProto.Timeline impl) {
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
        public static Timeline fromProto(@NonNull TimelineProto.Timeline proto) {
            return new Timeline(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TimelineProto.Timeline toProto() {
            return mImpl;
        }

        /** Builder for {@link Timeline} */
        public static final class Builder {
            private final TimelineProto.Timeline.Builder mImpl =
                    TimelineProto.Timeline.newBuilder();

            Builder() {}

            /** Adds one item to the entries in a timeline. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addTimelineEntry(@NonNull TimelineEntry timelineEntry) {
                mImpl.addTimelineEntries(timelineEntry.toProto());
                return this;
            }

            /** Adds one item to the entries in a timeline. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addTimelineEntry(@NonNull TimelineEntry.Builder timelineEntryBuilder) {
                mImpl.addTimelineEntries(timelineEntryBuilder.build().toProto());
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
