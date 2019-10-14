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

package androidx.ads.identifier.provider.testapp;

import androidx.ads.identifier.provider.AdvertisingIdProvider;
import androidx.annotation.NonNull;

/** An example Advertising ID Provider which always returns same ID. */
public class SampleAdvertisingIdProvider implements AdvertisingIdProvider {

    @NonNull
    @Override
    public String getId() {
        return "308f629d-c857-4026-8b62-7bdd71caaaaa";
    }

    @Override
    public boolean isLimitAdTrackingEnabled() {
        return false;
    }
}
