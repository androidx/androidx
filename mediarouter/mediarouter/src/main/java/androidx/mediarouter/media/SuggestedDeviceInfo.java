/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Allows applications to suggest routes to the user (for example, in the System UI Output
 * Switcher).
 *
 * <p>Suggested devices are used to transfer the current media session from one device to another.
 *
 * @see MediaRouter#setDeviceSuggestions
 * @see MediaRouter#clearDeviceSuggestions
 */
public final class SuggestedDeviceInfo {
    @NonNull private final String mDeviceDisplayName;

    @NonNull private final String mRouteId;

    private final @MediaRouter.RouteInfo.DeviceType int mType;

    @NonNull private final Bundle mExtras;

    private SuggestedDeviceInfo(Builder builder) {
        mDeviceDisplayName = builder.mDeviceDisplayName;
        mRouteId = builder.mRouteId;
        mType = builder.mType;
        mExtras = new Bundle(builder.mExtras);
    }

    /** Returns the id of the suggested device. */
    @NonNull
    public String getRouteId() {
        return mRouteId;
    }

    /** Returns the display name of the suggested device. */
    @NonNull
    public String getDeviceDisplayName() {
        return mDeviceDisplayName;
    }

    /** Returns the {@link MediaRouter.RouteInfo.DeviceType} of the suggested device. */
    public @MediaRouter.RouteInfo.DeviceType int getType() {
        return mType;
    }

    /**
     * Returns the {@link Bundle} of the suggested device set via {@link Builder#setExtras(Bundle)}.
     */
    @NonNull
    public Bundle getExtras() {
        return mExtras;
    }

    /** Builder for {@link SuggestedDeviceInfo}. */
    public static final class Builder {
        @NonNull private final String mDeviceDisplayName;

        @NonNull private final String mRouteId;

        private final @MediaRouter.RouteInfo.DeviceType int mType;

        private Bundle mExtras = Bundle.EMPTY;

        /**
         * Constructor.
         *
         * @param deviceDisplayName The {@link #getDeviceDisplayName() display name}.
         * @param routeId The {@link #getRouteId() route ID}.
         * @param type The {@link #getType() route type}.
         */
        public Builder(
                @NonNull String deviceDisplayName,
                @NonNull String routeId,
                @MediaRouter.RouteInfo.DeviceType int type) {
            if (TextUtils.isEmpty(deviceDisplayName)) {
                throw new IllegalArgumentException("Device display name cannot be empty");
            }
            mDeviceDisplayName = deviceDisplayName;

            if (TextUtils.isEmpty(routeId)) {
                throw new IllegalArgumentException("Route ID cannot be empty.");
            }
            mRouteId = routeId;
            mType = type;
        }

        /**
         * Creates a new SuggestedDeviceInfo. The device display name, route ID, and type must be
         * set. The extras cannot be null, but default to an empty {@link Bundle}.
         */
        @NonNull
        public SuggestedDeviceInfo build() {
            return new SuggestedDeviceInfo(this);
        }

        /**
         * Sets the {@link #getExtras() extras}.
         *
         * <p>The default value is an empty {@link Bundle}.
         *
         * <p>Do not mutate the given {@link Bundle} after passing it to this method. You can use
         * {@link Bundle#deepCopy()} to keep a mutable copy.
         *
         * @throws NullPointerException if the extras are null.
         */
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            mExtras = Objects.requireNonNull(extras, "Extras must not be null");
            return this;
        }
    }
}
