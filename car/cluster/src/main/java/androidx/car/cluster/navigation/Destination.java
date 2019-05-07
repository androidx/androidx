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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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
    @ParcelField(6)
    EnumWrapper<Traffic> mTraffic;
    @ParcelField(7)
    CharSequence mFormattedEta;

    /**
     * Traffic congestion level on the way to a destination, compared to ideal driving conditions.
     */
    public enum Traffic {
        /** Traffic information is not available */
        UNKNOWN,
        /** High amount of traffic */
        HIGH,
        /** Intermediate amount of traffic */
        MEDIUM,
        /** Traffic level close to free flow */
        LOW,
    }

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Destination() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Destination(@NonNull String title, @NonNull String address, @Nullable Distance distance,
            @Nullable Time eta, @Nullable LatLng latlng, @Nullable EnumWrapper<Traffic> traffic,
            @Nullable CharSequence formattedEta) {
        mTitle = title;
        mAddress = address;
        mDistance = distance;
        mEta = eta;
        mLatLng = latlng;
        mTraffic = traffic;
        mFormattedEta = formattedEta;
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
        private EnumWrapper<Traffic> mTraffic;
        private CharSequence mFormattedEta;

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
         * Provides an alternative to {@link #setFormattedEta(CharSequence)} and both may
         * optionally be set.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setEta(@Nullable ZonedDateTime eta) {
            mEta = eta != null ? new Time(eta) : null;
            return this;
        }

        /**
         * Sets the estimated time of arrival to this destination as a formatted CharSequence, or
         * empty if estimated time of arrival is unknown.
         *
         * Provides an alternative to {@link #setEta(ZonedDateTime)} and both may optionally be set.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setFormattedEta(@NonNull CharSequence formattedEta) {
            mFormattedEta = Preconditions.checkNotNull(formattedEta);
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
         * Sets the traffic congestion level to this destination, compared to ideal driving
         * conditions.
         *
         * @param traffic traffic level
         * @param fallbacks Variations of {@code traffic}, in case the consumer of this API doesn't
         *                  support the main one (used for backward compatibility).
         * @return this object for chaining
         */
        @NonNull
        public Builder setTraffic(@NonNull Traffic traffic, @NonNull Traffic... fallbacks) {
            mTraffic = EnumWrapper.of(traffic, fallbacks);
            return this;
        }

        /**
         * Returns a {@link Destination} built with the provided information.
         */
        @NonNull
        public Destination build() {
            return new Destination(
                    mTitle, mAddress, mDistance, mEta, mLatLng, mTraffic, mFormattedEta);
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
     * Returns the estimated time of arrival at this destination, or null if it was not provided or
     * is unknown.
     */
    @Nullable
    public ZonedDateTime getEta() {
        return mEta != null ? mEta.getTime() : null;
    }

    /**
     * Returns the estimated time of arrival at this destination as a formatted String, or empty if
     * it was not provided or is unknown.
     */
    @NonNull
    public CharSequence getFormattedEta() {
        return Common.nonNullOrEmpty(mFormattedEta);
    }

    /**
     * Returns the traffic congestion level to this destination, compared to to ideal driving
     * conditions.
     */
    @NonNull
    public Traffic getTraffic() {
        return EnumWrapper.getValue(mTraffic, Traffic.UNKNOWN);
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
                && Objects.equals(getEta(), that.getEta())
                && Objects.equals(getTraffic(), that.getTraffic())
                && Objects.equals(getFormattedEta(), that.getFormattedEta());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getAddress(), getDistance(), getLocation(), getEta(),
            getTraffic(), getFormattedEta());
    }

    @Override
    public String toString() {
        return String.format("{title: %s, address: %s, distance: %s, location: %s, eta: %s, "
                + "traffic: %s, formattedEta: %s}",
                mTitle, mAddress, mDistance, mLatLng, mEta, mTraffic, mFormattedEta);
    }
}
