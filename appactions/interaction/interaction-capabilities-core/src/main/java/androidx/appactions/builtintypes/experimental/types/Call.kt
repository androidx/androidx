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

package androidx.appactions.builtintypes.experimental.types

import androidx.appactions.builtintypes.experimental.properties.CallFormat
import androidx.appactions.builtintypes.experimental.properties.Name
import androidx.appactions.builtintypes.experimental.properties.Participant

interface Call : Thing {
    val callFormat: CallFormat?
    val participantList: List<Participant>
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = CallBuilderImpl()
    }

    object CanonicalValue {
        class CallFormat private constructor(textValue: String) :
            androidx.appactions.builtintypes.experimental.properties.CallFormat
                .CanonicalValue(textValue) {
            companion object {
                @JvmField
                val Audio = CallFormat("Audio")

                @JvmField
                val Video = CallFormat("Video")
            }
        }
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        fun setCallFormat(callFormat: CallFormat?): Self
        fun setCallFormat(canonicalValue: CanonicalValue.CallFormat): Self
        fun setCallFormat(text: String): Self
        fun addParticipant(person: Person): Self
        fun addParticipant(participant: Participant): Self
        fun addAllParticipant(value: Iterable<Participant>): Self

        override fun build(): Call
    }
}

private class CallBuilderImpl : Call.Builder<CallBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null
    private var callFormat: CallFormat? = null
    private var participantList = mutableListOf<Participant>()

    override fun build() = CallImpl(identifier, name, callFormat, participantList.toList())

    override fun setCallFormat(callFormat: CallFormat?): CallBuilderImpl =
        apply { this.callFormat = callFormat }

    override fun setCallFormat(canonicalValue: Call.CanonicalValue.CallFormat): CallBuilderImpl =
        apply {
            this.callFormat = CallFormat(canonicalValue)
        }

    override fun setCallFormat(text: String): CallBuilderImpl = apply {
        this.callFormat = CallFormat(text)
    }

    override fun addParticipant(person: Person): CallBuilderImpl = apply {
        participantList.add(Participant(person))
    }

    override fun addParticipant(participant: Participant): CallBuilderImpl = apply {
        participantList.add(participant)
    }

    override fun addAllParticipant(value: Iterable<Participant>): CallBuilderImpl = apply {
        participantList.addAll(value)
    }

    override fun setIdentifier(text: String?): CallBuilderImpl = apply { identifier = text }

    override fun setName(text: String): CallBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): CallBuilderImpl = apply { this.name = name }

    override fun clearName(): CallBuilderImpl = apply { name = null }
}

private class CallImpl(
    override val identifier: String?,
    override val name: Name?,
    override val callFormat: CallFormat?,
    override val participantList: List<Participant>
) : Call {
    override fun toBuilder(): Call.Builder<*> =
        CallBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setCallFormat(callFormat)
            .addAllParticipant(participantList)
}