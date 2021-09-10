/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.request

import android.os.Parcelable
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.RequestsProto

/**
 * Request for measure registration.
 *
 * @hide
 */
public class MeasureRegistrationRequest(
    public val packageName: String,
    public val dataType: DataType,
) : ProtoParcelable<RequestsProto.MeasureRegistrationRequest>() {

    override val proto: RequestsProto.MeasureRegistrationRequest by lazy {
        RequestsProto.MeasureRegistrationRequest.newBuilder()
            .setPackageName(packageName)
            .setDataType(dataType.proto)
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<MeasureRegistrationRequest> = newCreator { bytes ->
            val proto = RequestsProto.MeasureRegistrationRequest.parseFrom(bytes)
            MeasureRegistrationRequest(proto.packageName, DataType(proto.dataType))
        }
    }
}
