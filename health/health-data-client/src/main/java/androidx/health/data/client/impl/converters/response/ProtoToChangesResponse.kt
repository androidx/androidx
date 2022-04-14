/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.impl.converters.response

import androidx.health.data.client.changes.Change
import androidx.health.data.client.changes.DeletionChange
import androidx.health.data.client.changes.UpsertionChange
import androidx.health.data.client.impl.converters.records.toRecord
import androidx.health.data.client.response.ChangesResponse
import androidx.health.platform.client.proto.ChangeProto
import androidx.health.platform.client.proto.ResponseProto

/** Converts proto response to public API object. */
fun toChangesResponse(proto: ResponseProto.GetChangesResponse): ChangesResponse {
    return ChangesResponse(
        changes = extractChanges(proto.changesList),
        nextChangesToken = proto.nextChangesToken,
        hasMore = proto.hasMore,
        changesTokenExpired = proto.changesTokenExpired
    )
}

private fun extractChanges(changes: List<ChangeProto.DataChange>): List<Change> {
    return changes.mapNotNull {
        when {
            it.hasDeleteUid() -> DeletionChange(it.deleteUid)
            it.hasUpsertDataPoint() -> UpsertionChange(toRecord(it.upsertDataPoint))
            else -> null
        }
    }
}
