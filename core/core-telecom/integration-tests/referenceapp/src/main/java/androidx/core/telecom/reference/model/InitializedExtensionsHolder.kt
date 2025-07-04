/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.reference.model

import androidx.core.telecom.extensions.*
import androidx.core.telecom.reference.ParticipantsExtensionManager
import androidx.core.telecom.util.ExperimentalAppActions

@OptIn(ExperimentalAppActions::class)
data class InitializedExtensionsHolder(
    val localCallSilence: LocalCallSilenceExtension? = null,
    val callIcon: CallIconExtension? = null,
    val iconData: IconData? = null,
    val participants: ParticipantExtension? = null,
    val raiseHand: RaiseHandState? = null,
    val participantsManager: ParticipantsExtensionManager? = null,
)
