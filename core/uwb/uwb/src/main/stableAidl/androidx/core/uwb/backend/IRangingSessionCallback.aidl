
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

import androidx.core.uwb.backend.RangingPosition;
import androidx.core.uwb.backend.UwbDevice;

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingSessionCallback */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface IRangingSessionCallback {
    /** Reasons for suspend */
    const int UNKNOWN = 0;
    const int WRONG_PARAMETERS = 1;
    const int FAILED_TO_START = 2;
    const int STOPPED_BY_PEER = 3;
    const int STOP_RANGING_CALLED = 4;
    const int MAX_RANGING_ROUND_RETRY_REACHED = 5;

    void onRangingInitialized(in UwbDevice device);
    void onRangingResult(in UwbDevice device, in RangingPosition position);
    void onRangingSuspended(in UwbDevice device, int reason);
}
