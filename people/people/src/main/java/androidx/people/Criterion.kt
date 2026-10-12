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

/**
 * A single query input that a result record matched, reported through [Person.matchedCriteria] and
 * [Group.matchedCriteria].
 *
 * A batched query, for example a lookup of two hundred phone numbers, returns one record per
 * matched person or group. Each record reports every input that matched it, so callers can
 * correlate results back to their inputs:
 * ```
 * val person = results.firstOrNull { Criterion.Phone(number) in it.matchedCriteria }
 * ```
 *
 * **Echo contract.** A reported criterion carries the value exactly as the caller supplied it in
 * the query, not a normalized or reformatted form. For People Directory applications: report only
 * values that appear in the incoming query; others are dropped before they reach the client.
 *
 * **Non-exhaustive.** New criterion types may be added in future releases. Callers that branch on
 * the type must include an `else` branch. Each new type documents the minimum platform version that
 * supports it; on older platforms it is never reported.
 */
public abstract class Criterion private constructor() {

    /**
     * Matches by phone number.
     *
     * Applies to [Person] results.
     */
    public class Phone(
        /** The phone number, exactly as supplied in [PersonQuery.phones]. */
        public val number: String
    ) : Criterion() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Phone && number == other.number)

        override fun hashCode(): Int = number.hashCode()

        override fun toString(): String = "Criterion.Phone(number=$number)"
    }

    /**
     * Matches by email address.
     *
     * Applies to [Person] results.
     */
    public class Email(
        /** The email address, exactly as supplied in [PersonQuery.emails]. */
        public val address: String
    ) : Criterion() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Email && address == other.address)

        override fun hashCode(): Int = address.hashCode()

        override fun toString(): String = "Criterion.Email(address=$address)"
    }

    /**
     * Matches by the person record's own lookup key.
     *
     * Applies to [Person] results, where [key] is a [PersonId.lookupKey] supplied in
     * [PersonQuery.lookupKeys].
     */
    public class PersonLookupKey(
        /** The person lookup key, exactly as supplied in [PersonQuery.lookupKeys]. */
        public val key: String
    ) : Criterion() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is PersonLookupKey && key == other.key)

        override fun hashCode(): Int = key.hashCode()

        override fun toString(): String = "Criterion.PersonLookupKey(key=$key)"
    }

    /**
     * Matches by a group lookup key.
     *
     * Applies to [Group] results, where [key] is a [GroupId.lookupKey] supplied in
     * [GroupQuery.lookupKeys], and to [Person] results matched by group membership, where [key] is
     * supplied in [PersonQuery.groupLookupKeys].
     */
    public class GroupLookupKey(
        /** The group's lookup key, exactly as supplied in the query. */
        public val key: String
    ) : Criterion() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is GroupLookupKey && key == other.key)

        override fun hashCode(): Int = key.hashCode()

        override fun toString(): String = "Criterion.GroupLookupKey(key=$key)"
    }
}
