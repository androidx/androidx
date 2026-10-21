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

package androidx.people.client

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonQuery
import androidx.people.client.internal.PeopleDirectoryClientImpl
import androidx.people.internal.PeopleBackendResolver
import kotlinx.coroutines.flow.Flow

/**
 * Reads persons and groups published by the People Directory applications installed on the device,
 * such as VoIP and messaging apps.
 *
 * Obtain an instance with [create]. Every read comes in two forms: a suspending `query*` function
 * for a one-shot result, and an `observe*` function returning a [Flow] that emits the current
 * result and re-emits whenever the underlying data changes.
 *
 * People Directory data is served from from a central database that is synced from the People
 * Directory applications. A `query*` call therefore returns whatever that has been synced, which
 * may be incomplete on first use; an `observe*` call returns the database result immediately and
 * emits again once a refresh finishes. Prefer `observe*` whenever the result is displayed in UI.
 *
 * Reads return an empty list both when nothing matches and when the People Directory is not
 * available on this device — check [isAvailable] to tell the two apart.
 *
 * Person and group reads return each [Person.id] or [Group.id] at most once, even when the record
 * matches several inputs of a batched query, and the order of the returned list is unspecified.
 * [Person.matchedCriteria] and [Group.matchedCriteria] report every input that matched, echoed
 * exactly as supplied, so results can be correlated back to their inputs:
 * ```
 * val people = client.queryPersons(personQuery(pkg) { addPhones(numbers) })
 * val byNumber = numbers.associateWith { n ->
 *     people.firstOrNull { Criterion.Phone(n) in it.matchedCriteria }
 * }
 * ```
 *
 * [createInteractionRequest] returns a [PendingIntent] that starts a voice call, video call, or
 * messaging session in the People Directory application that owns the record.
 *
 * This interface is not intended to be implemented outside the library; new members may be added in
 * future releases.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@JvmDefaultWithCompatibility
public interface PeopleDirectoryClient {

    /**
     * Returns whether the People Directory can be read on this device.
     *
     * When `false`, every read returns an empty result and [createInteractionRequest] returns
     * `null`.
     */
    public suspend fun isAvailable(): Boolean

    /** Observes the People Directory applications registered on the device. */
    public fun observeDirectories(): Flow<List<DirectoryInfo>>

    /** Returns the People Directory applications registered on the device. */
    public suspend fun queryDirectories(): List<DirectoryInfo>

    /**
     * Observes the people matching [queries].
     *
     * Emits the cached result immediately, then re-emits whenever the matching data changes,
     * including once a background refresh of stale or missing data completes. Consecutive identical
     * results are not re-emitted.
     *
     * Each [Person.id] appears at most once, with every input it matched in
     * [Person.matchedCriteria]. The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public fun observePersons(vararg queries: PersonQuery): Flow<List<Person>>

    /**
     * Observes the people matching [queries].
     *
     * Emits the cached result immediately, then re-emits whenever the matching data changes,
     * including once a background refresh of stale or missing data completes. Consecutive identical
     * results are not re-emitted.
     *
     * Each [Person.id] appears at most once, with every input it matched in
     * [Person.matchedCriteria]. The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public fun observePersons(queries: List<PersonQuery>): Flow<List<Person>> =
        observePersons(*queries.toTypedArray())

    /**
     * Returns the people matching [queries], or an empty list if none match.
     *
     * Each [Person.id] appears at most once, with every input it matched in
     * [Person.matchedCriteria]. The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public suspend fun queryPersons(vararg queries: PersonQuery): List<Person>

    /**
     * Returns the people matching [queries], or an empty list if none match.
     *
     * Each [Person.id] appears at most once, with every input it matched in
     * [Person.matchedCriteria]. The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public suspend fun queryPersons(queries: List<PersonQuery>): List<Person> =
        queryPersons(*queries.toTypedArray())

    /**
     * Observes the groups matching [queries].
     *
     * Emits the cached result immediately, then re-emits whenever the matching data changes,
     * including once a background refresh of stale or missing data completes. Consecutive identical
     * results are not re-emitted.
     *
     * Each [Group.id] appears at most once, with every input it matched in [Group.matchedCriteria].
     * The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public fun observeGroups(vararg queries: GroupQuery): Flow<List<Group>>

    /**
     * Observes the groups matching [queries].
     *
     * Emits the cached result immediately, then re-emits whenever the matching data changes,
     * including once a background refresh of stale or missing data completes. Consecutive identical
     * results are not re-emitted.
     *
     * Each [Group.id] appears at most once, with every input it matched in [Group.matchedCriteria].
     * The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public fun observeGroups(queries: List<GroupQuery>): Flow<List<Group>> =
        observeGroups(*queries.toTypedArray())

    /**
     * Returns the groups matching [queries], or an empty list if none match.
     *
     * Each [Group.id] appears at most once, with every input it matched in [Group.matchedCriteria].
     * The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public suspend fun queryGroups(vararg queries: GroupQuery): List<Group>

    /**
     * Returns the groups matching [queries], or an empty list if none match.
     *
     * Each [Group.id] appears at most once, with every input it matched in [Group.matchedCriteria].
     * The order of the list is unspecified.
     *
     * @param queries one query per People Directory application to read from, batched into a single
     *   lookup.
     */
    public suspend fun queryGroups(queries: List<GroupQuery>): List<Group> =
        queryGroups(*queries.toTypedArray())

    /**
     * Creates a [PendingIntent] that starts the interaction described by [interaction].
     *
     * [PeopleInteraction.targetQuery] must identify exactly one recipient in one People Directory
     * application, so it must contain exactly one criterion value: one phone number, one email
     * address, one person lookup key, or one group lookup key.
     *
     * ```
     * val request =
     *     client.createInteractionRequest(
     *         PeopleInteraction(person.id, Capability.VIDEO)
     *     )
     * ```
     *
     * @param interaction the interaction to initiate, identifying the recipient and communication
     *   mode.
     * @return a [PendingIntent] launching the interaction, or `null` if the People Directory
     *   application declines the request or the People Directory is unavailable.
     * @throws IllegalArgumentException if the target does not contain exactly one criterion value.
     */
    public suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent?

    public companion object {
        /**
         * Returns a [PeopleDirectoryClient].
         *
         * Cheap and main-safe: creating a client does no I/O. The first call in the process that
         * needs the People Directory determines which implementation serves this device, which may
         * take a round trip to Google Play services; later calls reuse that answer.
         *
         * @param context any [Context]; only the application context is retained.
         */
        @JvmStatic
        public fun create(context: Context): PeopleDirectoryClient {
            val applicationContext = context.applicationContext
            return PeopleDirectoryClientImpl { PeopleBackendResolver.resolve(applicationContext) }
        }

        /**
         * Returns whether the People Directory can be read on this device.
         *
         * Equivalent to `create(context).isAvailable()`.
         *
         * @param context any [Context]; only the application context is retained.
         */
        @JvmStatic
        public suspend fun isAvailable(context: Context): Boolean =
            PeopleBackendResolver.resolve(context).isAvailable
    }
}
