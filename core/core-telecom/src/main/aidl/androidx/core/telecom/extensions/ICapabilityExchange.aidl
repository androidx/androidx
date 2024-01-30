/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.core.telecom.extensions;

import androidx.core.telecom.extensions.Capability;
import androidx.core.telecom.extensions.ICapabilityExchangeListener;
import androidx.core.telecom.extensions.IParticipantActions;
import androidx.core.telecom.extensions.IParticipantStateListener;
import androidx.core.telecom.extensions.ICallDetailsListener;

@JavaPassthrough(annotation="@androidx.core.telecom.util.ExperimentalAppActions")
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ICapabilityExchange {
    const int VERSION = 1;
    // Provide the capabilities of the service and request that capabilities of the remote are
    // calculated passing along a listener to send update back to the host side.
    void beginExchange(in List<Capability> capabilities, in ICapabilityExchangeListener l) = 0;
}