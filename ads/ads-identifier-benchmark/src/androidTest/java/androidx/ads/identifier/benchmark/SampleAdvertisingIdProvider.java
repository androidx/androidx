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

package androidx.ads.identifier.benchmark;

import androidx.annotation.NonNull;

/** An example Advertising ID Provider which always returns same ID. */
@SuppressWarnings("deprecation")
public class SampleAdvertisingIdProvider implements
        androidx.ads.identifier.provider.AdvertisingIdProvider {

    static final String DUMMY_AD_ID = "308f629d-c857-4026-8b62-7bdd71caaaaa";

    @NonNull
    @Override
    public String getId() {
        return DUMMY_AD_ID;
    }

    @Override
    public boolean isLimitAdTrackingEnabled() {
        return false;
    }
}
