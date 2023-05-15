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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An abstract model that holds information about routes from different sources.
 *
 * Can represent media routers' routes, bluetooth routes, or audio routes.
 */
public final class SystemRouteItem {

    public static final int ROUTE_SOURCE_MEDIA_ROUTER = 0;
    public static final int ROUTE_SOURCE_MEDIA_ROUTER2 = 1;
    public static final int ROUTE_SOURCE_ANDROIDX_ROUTER = 2;
    public static final int ROUTE_SOURCE_BLUETOOTH_MANAGER = 3;
    public static final int ROUTE_SOURCE_AUDIO_MANAGER = 4;

    @IntDef({
            ROUTE_SOURCE_MEDIA_ROUTER,
            ROUTE_SOURCE_MEDIA_ROUTER2,
            ROUTE_SOURCE_ANDROIDX_ROUTER,
            ROUTE_SOURCE_BLUETOOTH_MANAGER,
            ROUTE_SOURCE_AUDIO_MANAGER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @NonNull
    private final String mId;

    @Type
    private final int mType;

    @NonNull
    private final String mName;

    @Nullable
    private final String mAddress;

    @Nullable
    private final String mDescription;

    private SystemRouteItem(@NonNull Builder builder) {
        Objects.requireNonNull(builder.mId);
        Objects.requireNonNull(builder.mName);

        mId = builder.mId;
        mName = builder.mName;
        mType = builder.mType;

        mAddress = builder.mAddress;
        mDescription = builder.mDescription;
    }

    /**
     * Returns a unique identifier of a route.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns a route source.
     *
     * see {@link SystemRouteItem.Type}
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns a human-readable name of the route.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns address if the route is a Bluetooth route and {@code null} otherwise.
     */
    @Nullable
    public String getAddress() {
        return mAddress;
    }

    /**
     * Returns a route description or {@code null} if empty.
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemRouteItem that = (SystemRouteItem) o;
        return mType == that.mType && mId.equals(that.mId) && mName.equals(that.mName)
                && Objects.equals(mAddress, that.mAddress) && Objects.equals(
                mDescription, that.mDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mType, mName, mAddress, mDescription);
    }

    /**
     * Helps to construct {@link SystemRouteItem}.
     */
    public static final class Builder {

        @NonNull
        private final String mId;
        @Type
        private final int mType;

        @NonNull
        private String mName;

        @Nullable
        private String mAddress;

        @Nullable
        private String mDescription;

        public Builder(@NonNull String id, @Type int type) {
            mId = id;
            mType = type;
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
         * Builds {@link SystemRouteItem}.
         */
        @NonNull
        public SystemRouteItem build() {
            return new SystemRouteItem(this);
        }
    }
}
