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
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
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

    override suspend fun queryPersons(query: PersonQuery): PersonQueryResult =
        backend().queryPersons(query)

    override fun observePersons(query: PersonQuery): Flow<PersonQueryResult> = flow {
        emitAll(backend().observePersons(query))
    }

    override suspend fun queryGroups(query: GroupQuery): GroupQueryResult =
        backend().queryGroups(query)

    override fun observeGroups(query: GroupQuery): Flow<GroupQueryResult> = flow {
        emitAll(backend().observeGroups(query))
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? =
        backend().createInteractionRequest(interaction)
}
