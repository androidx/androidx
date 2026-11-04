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

@file:JvmName("Persons")

package androidx.people

import android.net.Uri
import androidx.annotation.RestrictTo

/** Creates a [Person] initialized with [id] and configured by [block]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@JvmOverloads
public fun person(
    id: PersonId,
    block: PersonScope.() -> Unit = {},
): Person = PersonScope(id).apply(block).build()

/**
 * An aggregated person record returned by a People Directory application.
 *
 * Query results wrap each person in a [PersonMatch], which also reports the query criteria that
 * matched it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Person
internal constructor(
    /** Unique compound identifier for this person record. */
    public val id: PersonId,
    /** The person's primary display name, or `null` if the directory did not supply one. */
    public val displayName: String? = null,
    /** URI pointing to the person's avatar photo, typically an HTTPS URL, or `null`. */
    public val photoUri: Uri? = null,
    /** Modes of communication this directory reports as available for this identity. */
    public val capabilities: Set<Capability> = emptySet(),
    /** Whether this person is an established contact or connection in the directory. */
    public val isKnown: Boolean = false,
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

    /** Returns a copy of this [Person] with properties modified by [block]. */
    public fun copy(block: PersonScope.() -> Unit): Person = PersonScope(this).apply(block).build()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false
        return id == other.id &&
            displayName == other.displayName &&
            photoUri == other.photoUri &&
            capabilities == other.capabilities &&
            isKnown == other.isKnown
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (photoUri?.hashCode() ?: 0)
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + isKnown.hashCode()
        return result
    }

    override fun toString(): String =
        "Person(id=$id, displayName=$displayName, photoUri=$photoUri, " +
            "capabilities=$capabilities, isKnown=$isKnown)"
}

/**
 * Receiver scope for configuring a [Person].
 *
 * @param id Unique compound identifier for this person record.
 */
@PeopleQueryDsl
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersonScope
internal constructor(
    /** Unique compound identifier for this person record. */
    public val id: PersonId
) {
    /** The person's primary display name, or `null` if the directory did not supply one. */
    public var displayName: String? = null

    /** URI pointing to the person's avatar photo, typically an HTTPS URL, or `null`. */
    public var photoUri: Uri? = null

    /** Modes of communication this directory reports as available for this identity. */
    public var capabilities: Set<Capability> = emptySet()

    /** Whether this person is an established contact or connection in the directory. */
    public var isKnown: Boolean = false

    internal constructor(person: Person) : this(person.id) {
        this.displayName = person.displayName
        this.photoUri = person.photoUri
        this.capabilities = person.capabilities
        this.isKnown = person.isKnown
    }

    /** Adds [capability] to the communication modes available for this person. */
    public fun addCapability(capability: Capability) {
        capabilities = capabilities + capability
    }

    /** Adds [capabilities] to the communication modes available for this person. */
    public fun addCapabilities(vararg capabilities: Capability) {
        this.capabilities = this.capabilities + capabilities
    }

    /** Adds [capabilities] to the communication modes available for this person. */
    public fun addCapabilities(capabilities: Iterable<Capability>) {
        this.capabilities = this.capabilities + capabilities
    }

    internal fun build(): Person =
        Person(
            id = id,
            displayName = displayName,
            photoUri = photoUri,
            capabilities = capabilities,
            isKnown = isKnown,
        )
}
