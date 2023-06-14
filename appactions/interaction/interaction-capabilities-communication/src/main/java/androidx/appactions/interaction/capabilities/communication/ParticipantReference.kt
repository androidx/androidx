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
package androidx.appactions.interaction.capabilities.communication

import androidx.appactions.builtintypes.experimental.properties.Participant
import androidx.appactions.interaction.capabilities.core.SearchAction
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec

class ParticipantReference private constructor(
    val asParticipant: Participant?,
    val asSearchAction: SearchAction<Participant>?,
) {
    constructor(participant: Participant) : this(participant, null)

    // TODO(b/268071906) add ParticipantFilter type to SearchAction
    constructor(participantFilter: SearchAction<Participant>) : this(null, participantFilter)

    companion object {
        private val TYPE_SPEC = UnionTypeSpec.Builder<ParticipantReference>()
            .bindMemberType(
                memberGetter = ParticipantReference::asParticipant,
                ctor = { ParticipantReference(it) },
                typeSpec = TypeConverters.PARTICIPANT_TYPE_SPEC,
            )
            .bindMemberType(
                memberGetter = ParticipantReference::asSearchAction,
                ctor = { ParticipantReference(it) },
                typeSpec = TypeConverters.createSearchActionTypeSpec(
                    TypeConverters.PARTICIPANT_TYPE_SPEC,
                ),
            )
            .build()

        internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
    }
}
