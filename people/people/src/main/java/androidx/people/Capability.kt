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

import androidx.annotation.RestrictTo

/**
 * A mode of communication with a person or group.
 *
 * [Capability] serves two symmetric roles:
 * - As a **reachability signal** on a result, via [Person.capabilities], describing which modes of
 *   communication a People Directory reports as available for that identity.
 * - As a **request parameter** when creating an interaction request, selecting which mode of
 *   communication to initiate.
 *
 * A client can therefore check reachability and launch an interaction using the same value:
 * ```
 * if (Capability.VIDEO in person.capabilities) {
 *     val request = client.createInteractionRequest(peopleInteraction(person.id, Capability.VIDEO))
 * }
 * ```
 *
 * This is the type-safe representation of the reachability bitmask flags defined by the underlying
 * wire contract.
 *
 * New capabilities may be added in future releases. Clients must ignore values they do not
 * recognize, and code that branches on a [Capability] must include an `else` branch.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Capability private constructor(private val name: String) {

    override fun toString(): String = name

    public companion object {
        /** Voice calling, over VoIP or cellular. */
        @JvmField public val VOICE: Capability = Capability("VOICE")

        /** Video calling. */
        @JvmField public val VIDEO: Capability = Capability("VIDEO")

        /** Text or chat messaging. */
        @JvmField public val MESSAGE: Capability = Capability("MESSAGE")
    }
}
