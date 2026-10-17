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
import androidx.people.Group
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonQuery
import androidx.people.internal.platform.requireInteractionTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Backend used when neither the platform People Provider nor the Google Play services backport
 * serves this device: reads are empty, and requests are declined.
 */
internal object UnavailablePeopleBackend : PeopleBackend {

    override val isAvailable: Boolean = false

    override suspend fun queryDirectories(): List<DirectoryInfo> = emptyList()

    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flowOf(emptyList())

    override suspend fun queryPersons(queries: List<PersonQuery>): List<Person> = emptyList()

    override fun observePersons(queries: List<PersonQuery>): Flow<List<Person>> =
        flowOf(emptyList())

    override suspend fun queryGroups(queries: List<GroupQuery>): List<Group> = emptyList()

    override fun observeGroups(queries: List<GroupQuery>): Flow<List<Group>> = flowOf(emptyList())

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? {
        // Validate anyway, so that an invalid target fails the same way on every device.
        interaction.targetQuery.requireInteractionTarget()
        return null
    }

    override suspend fun notifyChange(query: PersonQuery): Boolean = false

    override suspend fun notifyChange(query: GroupQuery): Boolean = false

    override suspend fun requestRefresh(): Boolean = false
}
