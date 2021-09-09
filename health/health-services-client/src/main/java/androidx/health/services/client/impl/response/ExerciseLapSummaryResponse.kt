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

package androidx.health.services.client.impl.response

import android.os.Parcelable
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.ResponsesProto

/**
 * Response containing [ExerciseLapSummary] when it's updated.
 *
 * @hide
 */
public class ExerciseLapSummaryResponse(public val exerciseLapSummary: ExerciseLapSummary) :
    ProtoParcelable<ResponsesProto.ExerciseLapSummaryResponse>() {

    override val proto: ResponsesProto.ExerciseLapSummaryResponse by lazy {
        ResponsesProto.ExerciseLapSummaryResponse.newBuilder()
            .setLapSummary(exerciseLapSummary.proto)
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseLapSummaryResponse> = newCreator { bytes ->
            val proto = ResponsesProto.ExerciseLapSummaryResponse.parseFrom(bytes)
            ExerciseLapSummaryResponse(ExerciseLapSummary(proto.lapSummary))
        }
    }
}
