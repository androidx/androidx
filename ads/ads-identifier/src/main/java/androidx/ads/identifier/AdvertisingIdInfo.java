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

package androidx.ads.identifier;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * Advertising ID Information.
 * Includes both the Advertising ID and the limit ad tracking setting.
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
@AutoValue
@AutoValue.CopyAnnotations
public abstract class AdvertisingIdInfo {

    // Create a no-args constructor so it doesn't appear in current.txt
    AdvertisingIdInfo() {
    }

    /** Retrieves the Advertising ID. */
    @NonNull
    public abstract String getId();

    /** Retrieves the Advertising ID provider package name. */
    @NonNull
    public abstract String getProviderPackageName();

    /** Retrieves whether the user has set Limit Advertising Tracking. */
    public abstract boolean isLimitAdTrackingEnabled();

    /** Create a {@link Builder}. */
    static Builder builder() {
        return new AutoValue_AdvertisingIdInfo.Builder();
    }

    /** The builder for {@link AdvertisingIdInfo}. */
    @AutoValue.Builder
    abstract static class Builder {

        // Create a no-args constructor so it doesn't appear in current.txt
        Builder() {
        }

        abstract Builder setId(String id);

        abstract Builder setProviderPackageName(String providerPackageName);

        abstract Builder setLimitAdTrackingEnabled(boolean limitAdTrackingEnabled);

        abstract AdvertisingIdInfo build();
    }
}
