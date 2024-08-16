/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.core.telecom.extensions

import androidx.core.telecom.util.ExperimentalAppActions

@ExperimentalAppActions
internal fun ParticipantParcelable.toParticipant(): Participant {
    return Participant(id, name)
}

/**
 * A representation of aspects of a participant in a Call.
 *
 * @param id A unique identifier shared with remote surfaces that represents this participant. This
 *   value MUST be unique and stable for the life of the call, meaning that this ID should not
 *   change or be reused for the lifetime of the call.
 * @param name The name of the Participant, which remote surfaces will display to users.
 */
@ExperimentalAppActions
public class Participant(
    public val id: String,
    public val name: CharSequence,
) {

    internal fun toParticipantParcelable(): ParticipantParcelable {
        return ParticipantParcelable().also { parcelable ->
            parcelable.id = id
            parcelable.name = name
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Participant

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Participant[$id]: name=$name"
    }
}
