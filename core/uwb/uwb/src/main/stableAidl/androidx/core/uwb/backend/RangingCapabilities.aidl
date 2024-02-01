
/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.core.uwb.backend;

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingCapabilities */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
parcelable RangingCapabilities {
    boolean supportsDistance;
    boolean supportsAzimuthalAngle;
    boolean supportsElevationAngle;
    int minRangingInterval;
    int[] supportedChannels;
    int[] supportedNtfConfigs;
    int[] supportedConfigIds;
    @nullable int[] supportedSlotDurations;
    @nullable int[] supportedRangingUpdateRates;
    boolean supportsRangingIntervalReconfigure;
    boolean hasBackgroundRangingSupport;
}
