/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Final or intermediate stop in a navigation route.
 */
@VersionedParcelize
public final class Destination implements VersionedParcelable {
    @ParcelField(1)
    String mTitle;
    @ParcelField(2)
    String mAddress;
    @ParcelField(3)
    Distance mDistance;
    @ParcelField(4)
    Time mEta;
    @ParcelField(5)
    LatLng mLatLng;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Destination() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Destination(@NonNull String title, @NonNull String address, @Nullable Distance distance,
            @Nullable Time eta, @Nullable LatLng latlng) {
        mTitle = title;
        mAddress = address;
        mDistance = distance;
        mEta = eta;
        mLatLng = latlng;
    }

    /**
     * Builder for creating a {@link Destination}
     */
    public static final class Builder {
        private String mTitle;
        private String mAddress;
        private Distance mDistance;
        private Time mEta;
        private LatLng mLatLng;

        /**
         * Sets the destination title (formatted for the current user's locale), or empty if there
         * is no title associated with this destination.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setTitle(@NonNull String title) {
            mTitle = Preconditions.checkNotNull(title);
            return this;
        }

        /**
         * Sets the destination address (formatted for the current user's locale), or empty if there
         * is no address associated with this destination.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setAddress(@NonNull String address) {
            mAddress = Preconditions.checkNotNull(address);
            return this;
        }

        /**
         * Sets the distance from the current position to this destination, or null if distance is
         * unknown.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setDistance(@Nullable Distance distance) {
            mDistance = distance;
            return this;
        }

        /**
         * Sets the estimated time of arrival to this destination, or null if estimated time of
         * arrival is unknown.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setEta(@Nullable ZonedDateTime eta) {
            mEta = eta != null ? new Time(eta) : null;
            return this;
        }

        /**
         * Sets the geo-location of this destination, or null if location is unknown.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setLocation(@Nullable LatLng latlng) {
            mLatLng = latlng;
            return this;
        }

        /**
         * Returns a {@link Destination} built with the provided information.
         */
        @NonNull
        public Destination build() {
            return new Destination(mTitle, mAddress, mDistance, mEta, mLatLng);
        }
    }

    /**
     * Returns the name of the destination (formatted for the current user's locale), or empty if
     * destination name is unknown.
     */
    @NonNull
    public String getTitle() {
        return Common.nonNullOrEmpty(mTitle);
    }

    /**
     * Returns the destination address (formatted for the current user's locale), or empty if there
     * is no address associated with this destination.
     */
    @NonNull
    public String getAddress() {
        return Common.nonNullOrEmpty(mAddress);
    }

    /**
     * Returns the distance from the current position to this destination, or null if distance was
     * not provided or is unknown.
     */
    @Nullable
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Returns the estimated time of arrival to this destination, or null if it was not provided or
     * is unknown.
     */
    @Nullable
    public ZonedDateTime getEta() {
        return mEta != null ? mEta.getTime() : null;
    }

    /**
     * Returns the geo-location of this destination, or null if it was not provided or is unknown.
     */
    @Nullable
    public LatLng getLocation() {
        return mLatLng;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Destination that = (Destination) o;
        return Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getAddress(), that.getAddress())
                && Objects.equals(getDistance(), that.getDistance())
                && Objects.equals(getLocation(), that.getLocation())
                && Objects.equals(getEta(), that.getEta());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getAddress(), getDistance(), getLocation(), getEta());
    }

    @Override
    public String toString() {
        return String.format("{title: %s, address: %s, distance: %s, location: %s, eta: %s}",
                mTitle, mAddress, mDistance, mLatLng, mEta);
    }
}
