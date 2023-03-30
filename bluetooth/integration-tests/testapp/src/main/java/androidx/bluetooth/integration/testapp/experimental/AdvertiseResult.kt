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

package androidx.bluetooth.integration.testapp.experimental

enum class AdvertiseResult {
    ADVERTISE_STARTED,
    ADVERTISE_FAILED_ALREADY_STARTED,
    ADVERTISE_FAILED_DATA_TOO_LARGE,
    ADVERTISE_FAILED_FEATURE_UNSUPPORTED,
    ADVERTISE_FAILED_INTERNAL_ERROR,
    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
}
