/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import android.net.Uri

/**
 * An aggregated person record returned by a People Directory application.
 *
 * Each [id] appears at most once in a result. [matchedCriteria] reports every query input that
 * matched this person, so that a batched query, such as a lookup of two hundred phone numbers, can
 * be correlated back to its inputs:
 * ```
 * val person = results.firstOrNull { Criterion.Phone(number) in it.matchedCriteria }
 * ```
 */
public class Person(
    /** Unique compound identifier for this person record. */
    public val id: PersonId,
    /** The person's primary display name, or `null` if the directory did not supply one. */
    public val displayName: String?,
    /** URI pointing to the person's avatar photo, typically an HTTPS URL, or `null`. */
    public val photoUri: Uri?,
    /** Modes of communication this directory reports as available for this identity. */
    public val capabilities: Set<Capability>,
    /** Whether this person is an established contact or connection in the directory. */
    public val isKnown: Boolean,
    /**
     * The query inputs that matched this person, or an empty set if none were reported.
     *
     * Each value is echoed exactly as supplied in the [PersonQuery]. See [Criterion] for the full
     * contract.
     */
    public val matchedCriteria: Set<Criterion>,
) {
    /** Whether [Capability.VOICE] is present in [capabilities]. */
    public val isVoiceSupported: Boolean
        get() = Capability.VOICE in capabilities

    /** Whether [Capability.VIDEO] is present in [capabilities]. */
    public val isVideoSupported: Boolean
        get() = Capability.VIDEO in capabilities

    /** Whether [Capability.MESSAGE] is present in [capabilities]. */
    public val isMessageSupported: Boolean
        get() = Capability.MESSAGE in capabilities

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false
        return id == other.id &&
            displayName == other.displayName &&
            photoUri == other.photoUri &&
            capabilities == other.capabilities &&
            isKnown == other.isKnown &&
            matchedCriteria == other.matchedCriteria
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (photoUri?.hashCode() ?: 0)
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + isKnown.hashCode()
        result = 31 * result + matchedCriteria.hashCode()
        return result
    }

    override fun toString(): String =
        "Person(id=$id, displayName=$displayName, photoUri=$photoUri, " +
            "capabilities=$capabilities, isKnown=$isKnown, matchedCriteria=$matchedCriteria)"
}
