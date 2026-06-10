/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import android.os.Build;

import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.serializers.DurationAsLongSerializer;
import androidx.appsearch.serializers.InstantAsLongSerializer;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * AppSearch document representing an {@link Event} entity.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
@Document(name = "builtin:Event")
public class Event extends Thing {

    @Document.LongProperty(serializer = InstantAsLongSerializer.class)
    private @NonNull Instant mStartDate;

    @Document.LongProperty(serializer = InstantAsLongSerializer.class)
    private @Nullable Instant mEndDate;

    @Document.LongProperty(serializer = DurationAsLongSerializer.class)
    private @Nullable Duration mDuration;

    @Document.StringProperty
    private @Nullable String mLocation;

    @Document.DocumentProperty
    private @Nullable ImageObject mLogo;

    /**
     * Constructor for {@link Event}.
     *
     * @param builder The builder to construct the {@link Event} from.
     */
    @ExperimentalAppSearchApi
    public Event(@NonNull BuilderBase<?> builder) {
        super(builder);
        mStartDate = builder.mStartDate;
        mEndDate = builder.mEndDate;
        mDuration = builder.mDuration;
        mLocation = builder.mLocation;
        mLogo = builder.mLogo;
    }

    /**
     * Returns the start date of the event as a {@link Instant}.
     */
    @NonNull
    public Instant getStartDate() {
        return mStartDate;
    }

    /**
     * Returns the end date of the event as a {@link Instant}, if set.
     */
    @Nullable
    public Instant getEndDate() {
        return mEndDate;
    }

    /**
     * Returns the duration of the event as a {@link Duration}, if set.
     */
    @Nullable
    public Duration getDuration() {
        return mDuration;
    }

    /**
     * Returns the location of the event, if set.
     *
     * The location can be a venue name, a street address, or a string
     * representation of a latitude/longitude pair.
     *
     * For example, "Some Place" or "123 Main Street, Anytown, CA" or
     * "37.7749,-122.4194".
     */
    @Nullable
    public String getLocation() {
        return mLocation;
    }

    /**
     * Returns the logo of the event, if set.
     */
    public @Nullable ImageObject getLogo() {
        return mLogo;
    }

    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {

        /**
         * Constructor for {@link Event.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         * @param startDate The start date of the event.
         */
        public Builder(@NonNull String namespace, @NonNull String id,
            @NonNull Instant startDate) {
            super(namespace, id, startDate);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull Event event) {
            super(event);
        }
    }

    /**
     * Builder for {@link Event}.
     */
    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private Instant mStartDate;
        private Instant mEndDate;
        private Duration mDuration;
        private String mLocation;
        private ImageObject mLogo;

        /**
         * Constructor for {@link Event.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         * @param startDate The start date of the event.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id,
            @NonNull Instant startDate) {
            super(namespace, id);
            this.mStartDate = Preconditions.checkNotNull(startDate);
        }

        /**
         * Constructor for {@link Event.BuilderBase} with all the existing values.
         *
         * @param event The existing {@link Event} to copy values from.
         */
        public BuilderBase(@NonNull Event event) {
            super(event);
            mStartDate = event.getStartDate();
            mEndDate = event.getEndDate();
            mDuration = event.getDuration();
            mLocation = event.getLocation();
            mLogo = event.getLogo();
        }

        /**
         * Sets the end date of the event as a {@link Instant}.
         *
         * @param endDate The end date of the event.
         */
        public @NonNull T setEndDate(@Nullable Instant endDate) {
            mEndDate = endDate;
            return (T) this;
        }

        /**
         * Sets the duration of the event as a {@link Duration}.
         *
         * @param duration The duration of the event.
         */
        public @NonNull T setDuration(@Nullable Duration duration) {
            mDuration = duration;
            return (T) this;
        }

        /**
         * Sets the location of the event.
         *
         * A freeform string describing the Event location. This could be a
         * venue name, a string representation of a street address,
         * or a string representation of a latitude/longitude pair.
         *
         * For example, "Some Place" or "123 Main Street, Anytown, CA" or
         * "37.7749,-122.4194".
         */
        public @NonNull T setLocation(@Nullable String location) {
            mLocation = location;
            return (T) this;
        }

        /**
         * Sets the logo of the event.
         */
        public @NonNull T setLogo(@Nullable ImageObject logo) {
            mLogo = logo;
            return (T) this;
        }

        @Override
        public @NonNull Event build() {
            return new Event(this);
        }
    }
}