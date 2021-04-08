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

import androidx.annotation.NonNull;

/**
 * The class for the AndroidX Advertising ID Provider that should provide the resettable ID and
 * LAT preference should implement this interface.
 *
 * See {@link AdvertisingIdProviderManager} for more details.
 *
 * <p>Note: The implementation of this interface must be completely thread-safe.
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
public interface AdvertisingIdProvider {
    /** Retrieves the Advertising ID. */
    @NonNull
    String getId();

    /** Retrieves whether the user has chosen to limit ad tracking (ads personalization). */
    boolean isLimitAdTrackingEnabled();
}
