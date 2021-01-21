/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** A metadata class used for attaching additional properties to models. */
public class Metadata {
    /** An empty {@link Metadata} instance. */
    public static final Metadata EMPTY_METADATA = new Builder().build();

    @Keep
    @Nullable
    private final Place mPlace;

    /** Constructs a new builder of a {@link Metadata} instance. */
    // TODO(b/175827428): remove once host is changed to use new public ctor.
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link Builder} with the data from this {@link Metadata} instance.
     * @deprecated use constructor.
     */
    // TODO(b/177484889): remove once host is changed to use new public ctor.
    @Deprecated
    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    public Place getPlace() {
        return mPlace;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlace);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Metadata)) {
            return false;
        }
        Metadata otherMetadata = (Metadata) other;

        return Objects.equals(mPlace, otherMetadata.mPlace);
    }

    Metadata(Builder builder) {
        mPlace = builder.mPlace;
    }

    /** Default constructor for serialization. */
    private Metadata() {
        mPlace = null;
    }

    /** A builder for {@link Metadata}. */
    public static final class Builder {
        @Nullable
        Place mPlace;

        /**
         * Sets a {@link Place} used for showing {@link Distance} and {@link PlaceMarker}
         * information,
         * or {@code null} if no {@link Place} information is available.
         */
        @NonNull
        public Builder setPlace(@Nullable Place place) {
            this.mPlace = place;
            return this;
        }

        /**
         * Returns a {@link Metadata} instance defined by this builder.
         */
        @NonNull
        public Metadata build() {
            return new Metadata(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new {@link Builder} with the data from the given {@link Metadata} instance.
         *
         * @throws NullPointerException if {@code icon} is {@code null}.
         */
        public Builder(@NonNull Metadata metadata) {
            this.mPlace = requireNonNull(metadata).getPlace();
        }
    }
}
