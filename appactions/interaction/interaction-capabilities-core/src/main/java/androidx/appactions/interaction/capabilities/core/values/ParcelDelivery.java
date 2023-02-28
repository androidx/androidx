/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;
import java.util.Optional;

/** The delivery of a parcel. */
@AutoValue
public abstract class ParcelDelivery extends Thing {

    /** Create a new ParcelDelivery.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_ParcelDelivery.Builder();
    }

    /** Returns the delivery address. */
    @NonNull
    public abstract Optional<String> getDeliveryAddress();

    /** Returns the method used for delivery or shipping. */
    @NonNull
    public abstract Optional<String> getDeliveryMethod();

    /** Returns the earliest date the package may arrive. */
    @NonNull
    public abstract Optional<ZonedDateTime> getExpectedArrivalFrom();

    /** Returns the latest date the package may arrive. */
    @NonNull
    public abstract Optional<ZonedDateTime> getExpectedArrivalUntil();

    /** Returns the tracking number. */
    @NonNull
    public abstract Optional<String> getTrackingNumber();

    /** Returns the tracking URL. */
    @NonNull
    public abstract Optional<String> getTrackingUrl();

    /** Builder class for building ParcelDelivery. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<ParcelDelivery> {

        /** Sets the delivery address. */
        @NonNull
        public abstract Builder setDeliveryAddress(@NonNull String deliveryAddress);

        /** Sets the delivery method. */
        @NonNull
        public abstract Builder setDeliveryMethod(@NonNull String deliveryMethod);

        /** Sets the earliest date the package may arrive. */
        @NonNull
        public abstract Builder setExpectedArrivalFrom(@NonNull ZonedDateTime expectedArrivalFrom);

        /** Sets the latest date the package may arrive. */
        @NonNull
        public abstract Builder setExpectedArrivalUntil(
                @NonNull ZonedDateTime expectedArrivalUntil);

        /** Sets the tracking number. */
        @NonNull
        public abstract Builder setTrackingNumber(@NonNull String trackingNumber);

        /** Sets the tracking URL. */
        @NonNull
        public abstract Builder setTrackingUrl(@NonNull String trackingUrl);
    }
}
