/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier.provider;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * A {@link AdvertisingIdProviderInfo} represents the information about an Advertising ID Provider
 * installed on the device.
 *
 * <p>Used in cases when there are multiple Advertising ID Providers on the device. See
 * {@link AdvertisingIdProviderManager#getAdvertisingIdProviders} for more details.
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
@AutoValue
@AutoValue.CopyAnnotations
public abstract class AdvertisingIdProviderInfo {

    // Create a no-args constructor so it doesn't appear in current.txt
    AdvertisingIdProviderInfo() {
    }

    /** Retrieves the Advertising ID Provider package name. */
    @NonNull
    public abstract String getPackageName();

    /**
     * Retrieves the {@link Intent} to open the Advertising ID settings page for a given
     * Advertising ID Provider.
     *
     * <p>This page should allow the user to reset Advertising IDs and change Limit Advertising
     * Tracking preference.
     */
    @Nullable
    public abstract Intent getSettingsIntent();

    /**
     * Retrieves whether the provider has the highest priority among all the providers for the
     * developer library, meaning its provided ID will be used.
     */
    public abstract boolean isHighestPriority();

    /** Create a {@link Builder}. */
    static Builder builder() {
        return new AutoValue_AdvertisingIdProviderInfo.Builder().setHighestPriority(false);
    }

    /** The builder for {@link AdvertisingIdProviderInfo}. */
    @AutoValue.Builder
    abstract static class Builder {

        // Create a no-args constructor so it doesn't appear in current.txt
        Builder() {
        }

        abstract Builder setPackageName(String packageName);

        abstract Builder setSettingsIntent(Intent settingsIntent);

        abstract Builder setHighestPriority(boolean highestPriority);

        abstract AdvertisingIdProviderInfo build();
    }
}
