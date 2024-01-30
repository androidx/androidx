/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.solver.binderprovider

import androidx.room.ext.PagingTypeNames
import androidx.room.ext.RoomPagingRx2TypeNames
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors.MISSING_ROOM_PAGING_RXJAVA2_ARTIFACT
import androidx.room.solver.QueryResultBinderProvider

fun RxJava2PagingSourceQueryResultBinderProvider(
    context: Context
): QueryResultBinderProvider {
    val rxjava2PagingSource = RoomPagingRx2TypeNames.LIMIT_OFFSET_RX_PAGING_SOURCE

    return MultiTypedPagingSourceQueryResultBinderProvider(
        context = context,
        roomPagingClassName = rxjava2PagingSource,
        pagingSourceTypeName = PagingTypeNames.RX2_PAGING_SOURCE
    ).requireArtifact(
        context = context,
        requiredType = rxjava2PagingSource,
        missingArtifactErrorMsg = MISSING_ROOM_PAGING_RXJAVA2_ARTIFACT
    )
}
