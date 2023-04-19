/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service.testing.internal

import androidx.appactions.builtintypes.experimental.types.Alarm
import androidx.appactions.interaction.capabilities.core.entity.EntityProvider
import androidx.appactions.interaction.capabilities.core.entity.EntityLookupRequest
import androidx.appactions.interaction.capabilities.core.entity.EntityLookupResponse
import androidx.appactions.interaction.capabilities.core.entity.EntityLookupCandidate
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters

class FakeAlarmEntityProvider(
    override val id: String,
    private val alarms: List<Alarm> = listOf()
) : EntityProvider<Alarm>(
    TypeConverters.ALARM_TYPE_SPEC
) {
    override suspend fun lookup(
        request: EntityLookupRequest<Alarm>
    ): EntityLookupResponse<Alarm> {
        return EntityLookupResponse.Builder<Alarm>().setCandidateList(
            alarms.map { EntityLookupCandidate.Builder<Alarm>().setCandidate(it).build() }
        ).setStatus(
            EntityLookupResponse.SUCCESS
        ).build()
    }
}