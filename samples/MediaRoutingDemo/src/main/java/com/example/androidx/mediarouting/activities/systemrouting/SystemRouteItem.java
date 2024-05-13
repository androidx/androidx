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

package com.example.androidx.mediarouting.activities.systemrouting;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** Holds information about a system route. */
public final class SystemRouteItem implements SystemRoutesAdapterItem {

    @NonNull public final String mId;

    @NonNull public final String mName;

    @Nullable public final String mAddress;

    @Nullable public final String mDescription;

    @Nullable public final String mSuitabilityStatus;

    @Nullable public final Boolean mTransferInitiatedBySelf;

    @Nullable public final String mTransferReason;

    private SystemRouteItem(@NonNull Builder builder) {
        mId = Objects.requireNonNull(builder.mId);
        mName = Objects.requireNonNull(builder.mName);
        mAddress = builder.mAddress;
        mDescription = builder.mDescription;
        mSuitabilityStatus = builder.mSuitabilityStatus;
        mTransferInitiatedBySelf = builder.mTransferInitiatedBySelf;
        mTransferReason = builder.mTransferReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemRouteItem that = (SystemRouteItem) o;
        return mId.equals(that.mId)
                && mName.equals(that.mName)
                && Objects.equals(mAddress, that.mAddress)
                && Objects.equals(mDescription, that.mDescription)
                && Objects.equals(mSuitabilityStatus, that.mSuitabilityStatus)
                && Objects.equals(mTransferInitiatedBySelf, that.mTransferInitiatedBySelf)
                && Objects.equals(mTransferReason, that.mTransferReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mId,
                mName,
                mAddress,
                mDescription,
                mSuitabilityStatus,
                mTransferInitiatedBySelf,
                mTransferReason);
    }

    /**
     * Helps to construct {@link SystemRouteItem}.
     */
    public static final class Builder {

        @NonNull private final String mId;
        @NonNull private String mName;
        @Nullable private String mAddress;
        @Nullable private String mDescription;
        @Nullable private String mSuitabilityStatus;
        @Nullable private Boolean mTransferInitiatedBySelf;
        @Nullable private String mTransferReason;

        public Builder(@NonNull String id) {
            mId = id;
        }

        /**
         * Sets a route name.
         */
        @NonNull
        public Builder setName(@NonNull String name) {
            mName = name;
            return this;
        }

        /**
         * Sets an address for the route.
         */
        @NonNull
        public Builder setAddress(@NonNull String address) {
            if (!TextUtils.isEmpty(address)) {
                mAddress = address;
            }
            return this;
        }

        /**
         * Sets a description for the route.
         */
        @NonNull
        public Builder setDescription(@NonNull String description) {
            if (!TextUtils.isEmpty(description)) {
                mDescription = description;
            }
            return this;
        }

        /**
         * Sets a human-readable string describing the transfer suitability of the route, or null if
         * not applicable.
         */
        @NonNull
        public Builder setSuitabilityStatus(@Nullable String suitabilityStatus) {
            mSuitabilityStatus = suitabilityStatus;
            return this;
        }

        /**
         * Sets whether the corresponding route's selection is the result of an action of this app,
         * or null if not applicable.
         */
        @NonNull
        public Builder setTransferInitiatedBySelf(@Nullable Boolean transferInitiatedBySelf) {
            mTransferInitiatedBySelf = transferInitiatedBySelf;
            return this;
        }

        /**
         * Sets a human-readable string describing the transfer reason, or null if not applicable.
         */
        @NonNull
        public Builder setTransferReason(@Nullable String transferReason) {
            mTransferReason = transferReason;
            return this;
        }

        /**
         * Builds {@link SystemRouteItem}.
         */
        @NonNull
        public SystemRouteItem build() {
            return new SystemRouteItem(this);
        }
    }
}
