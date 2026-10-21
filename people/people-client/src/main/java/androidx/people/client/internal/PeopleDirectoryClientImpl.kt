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

package androidx.people.client.internal

import android.app.PendingIntent
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonQuery
import androidx.people.client.PeopleDirectoryClient
import androidx.people.internal.PeopleBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * The [PeopleDirectoryClient] returned by [PeopleDirectoryClient.create].
 *
 * Holds no state of its own: every call goes to the backend that serves this process, which the
 * first call in the process resolves.
 *
 * @param backend returns the backend serving this process.
 */
internal class PeopleDirectoryClientImpl(private val backend: suspend () -> PeopleBackend) :
    PeopleDirectoryClient {

    override suspend fun isAvailable(): Boolean = backend().isAvailable

    override suspend fun queryDirectories(): List<DirectoryInfo> = backend().queryDirectories()

    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flow {
        emitAll(backend().observeDirectories())
    }

    override suspend fun queryPersons(vararg queries: PersonQuery): List<Person> =
        backend().queryPersons(queries.toList())

    override fun observePersons(vararg queries: PersonQuery): Flow<List<Person>> {
        val queryList = queries.toList()
        return flow { emitAll(backend().observePersons(queryList)) }
    }

    override suspend fun queryGroups(vararg queries: GroupQuery): List<Group> =
        backend().queryGroups(queries.toList())

    override fun observeGroups(vararg queries: GroupQuery): Flow<List<Group>> {
        val queryList = queries.toList()
        return flow { emitAll(backend().observeGroups(queryList)) }
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? =
        backend().createInteractionRequest(interaction)
}
