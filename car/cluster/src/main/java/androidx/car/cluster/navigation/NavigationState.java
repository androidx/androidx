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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Navigation state data to be displayed on the instrument cluster of a car. This is composed by:
 * <ul>
 * <li>a list of destinations.
 * <li>the immediate step or steps in order to drive towards those destinations.
 * </ul>
 * This information can converted it to/from a {@link Parcelable} by using {@link #toParcelable()}
 * and {@link #fromParcelable(Parcelable)}, in order to be used in IPC (see {@link Parcel}).
 */
@VersionedParcelize
public final class NavigationState implements VersionedParcelable {
    @ParcelField(1)
    List<Step> mSteps;
    @ParcelField(2)
    List<Destination> mDestinations;
    @ParcelField(3)
    Segment mCurrentSegment;

    /**
     * Used by {@link VersionedParcelable}

     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    NavigationState() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    NavigationState(@NonNull List<Step> steps, @NonNull List<Destination> destinations,
            @Nullable Segment currentSegment) {
        mSteps = Collections.unmodifiableList(new ArrayList<>(steps));
        mDestinations = Collections.unmodifiableList(new ArrayList<>(destinations));
        mCurrentSegment = currentSegment;
    }

    /**
     * Builder for creating a {@link NavigationState}
     */
    public static final class Builder {
        List<Step> mSteps = new ArrayList<>();
        List<Destination> mDestinations = new ArrayList<>();
        Segment mCurrentSegment;

        /**
         * Add a navigation step. Steps should be provided in order of execution. It is up to the
         * producer to decide how many steps in advance will be provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addStep(@NonNull Step step) {
            mSteps.add(Preconditions.checkNotNull(step));
            return this;
        }

        /**
         * Add a destination or intermediate stop in the navigation. Destinations should be provided
         * from nearest to furthest.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addDestination(@NonNull Destination destination) {
            mDestinations.add(Preconditions.checkNotNull(destination));
            return this;
        }

        /**
         * Sets the current segment being driven, or null if the segment being driven is unknown.
         */
        @NonNull
        public Builder setCurrentSegment(@Nullable Segment segment) {
            mCurrentSegment = segment;
            return this;
        }

        /**
         * Returns a {@link NavigationState} built with the provided information.
         */
        @NonNull
        public NavigationState build() {
            return new NavigationState(mSteps, mDestinations, mCurrentSegment);
        }
    }

    /**
     * Returns an unmodifiable list of navigation steps, in order of execution. It is up to the
     * producer to decide how many steps in advance will be provided.
     */
    @NonNull
    public List<Step> getSteps() {
        return Common.nonNullOrEmpty(mSteps);
    }

    /**
     * Returns an unmodifiable list of destinations and intermediate stops in the navigation, sorted
     * from nearest to furthest.
     */
    @NonNull
    public List<Destination> getDestinations() {
        return Common.nonNullOrEmpty(mDestinations);
    }

    /**
     * Returns the current segment being driven, or null if the segment being driven is unknown.
     */
    @Nullable
    public Segment getCurrentSegment() {
        return mCurrentSegment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NavigationState that = (NavigationState) o;
        return Objects.equals(getSteps(), that.getSteps())
                && Objects.equals(getDestinations(), that.getDestinations())
                && Objects.equals(getCurrentSegment(), that.getCurrentSegment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSteps(), getDestinations(), getCurrentSegment());
    }

    @Override
    public String toString() {
        return String.format("{steps: %s, destinations: %s, segment: %s}", mSteps, mDestinations,
                mCurrentSegment);
    }

    /**
     * Returns this {@link NavigationState} as a {@link Parcelable}
     */
    @NonNull
    public Parcelable toParcelable() {
        return ParcelUtils.toParcelable(this);
    }

    /**
     * Creates a {@link NavigationState} based on data stored in the given {@link Parcelable}
     */
    public static NavigationState fromParcelable(@Nullable Parcelable parcelable) {
        return parcelable != null ? ParcelUtils.fromParcelable(parcelable) : new NavigationState();
    }
}
