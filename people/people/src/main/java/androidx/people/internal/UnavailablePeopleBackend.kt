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

package androidx.people.internal

import android.app.PendingIntent
import androidx.people.DirectoryInfo
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Backend used when neither the platform People Provider nor the Google Play services backport
 * serves this device: reads report every criterion as unhandled, and requests are declined.
 */
internal object UnavailablePeopleBackend : PeopleBackend {

    override val isAvailable: Boolean = false

    override suspend fun queryDirectories(): List<DirectoryInfo> = emptyList()

    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flowOf(emptyList())

    override suspend fun queryPersons(query: PersonQuery): PersonQueryResult =
        failedPersonQueryResultOf(query)

    override fun observePersons(query: PersonQuery): Flow<PersonQueryResult> =
        flowOf(failedPersonQueryResultOf(query))

    override suspend fun queryGroups(query: GroupQuery): GroupQueryResult =
        failedGroupQueryResultOf(query)

    override fun observeGroups(query: GroupQuery): Flow<GroupQueryResult> =
        flowOf(failedGroupQueryResultOf(query))

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? =
        null

    override suspend fun notifyChange(query: DirectoryPersonQuery): Boolean = false

    override suspend fun notifyChange(query: DirectoryGroupQuery): Boolean = false

    override suspend fun requestRefresh(): Boolean = false
}
