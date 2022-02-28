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
package androidx.health.platform.client.request

import android.os.Parcelable
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.RequestProto

/** Internal parcelable for IPC calls. */
class DeleteDataRequest(
    val uids: List<RequestProto.DataTypeIdPair>,
    val clientIds: List<RequestProto.DataTypeIdPair>
) : ProtoParcelable<RequestProto.DeleteDataRequest>() {
    override val proto: RequestProto.DeleteDataRequest
        get() {
            val obj = this
            return RequestProto.DeleteDataRequest.newBuilder()
                .addAllUids(obj.uids)
                .addAllClientIds(obj.clientIds)
                .build()
        }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DeleteDataRequest> =
            ProtoParcelable.newCreator {
                val proto = RequestProto.DeleteDataRequest.parseFrom(it)
                fromProto(proto)
            }

        internal fun fromProto(
            proto: RequestProto.DeleteDataRequest,
        ): DeleteDataRequest {
            return DeleteDataRequest(proto.uidsList, proto.clientIdsList)
        }
    }
}
