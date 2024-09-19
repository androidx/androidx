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

import java.util.List;
import androidx.core.telecom.extensions.Capability;
import androidx.core.telecom.extensions.IParticipantStateListener;
import androidx.core.telecom.extensions.ICallDetailsListener;
import androidx.core.telecom.extensions.ILocalSilenceStateListener;

// ICS Client -> VOIP app
@JavaPassthrough(annotation="@androidx.core.telecom.util.ExperimentalAppActions")
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ICapabilityExchangeListener {
    // V1 - Signal to VOIP containing participant extension support provided by ICS and provides
    // VOIP side with a listener to communicate participant state changes to.
    void onCreateParticipantExtension(in int version, in int[] actions, in IParticipantStateListener l) = 0;
    // V1 - no actions set for call details yet, but we want to be forwards compatible.
    void onCreateCallDetailsExtension(in int version, in int[] actions, in ICallDetailsListener l, in String packageName) = 1;
    // V1 - Remove extensions and release resources related to this InCallService connection
    void onRemoveExtensions() = 2;
    // V1 - no actions, only the ability to toggle the isLocallySilenced value
    void onCreateLocalCallSilenceExtension(in int version, in int[] actions,  in ILocalSilenceStateListener l) = 3;
}