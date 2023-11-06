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

package androidx.health.services.client.impl.request

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.ExerciseTypeConfig
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.RequestsProto

/**
 * Request for updating exercise type configuration in an [ExerciseTypeConfig].
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class UpdateExerciseTypeConfigRequest(
    val packageName: String,
    val exerciseTypeConfig: ExerciseTypeConfig,
) : ProtoParcelable<RequestsProto.UpdateExerciseTypeConfigRequest>() {
    override val proto: RequestsProto.UpdateExerciseTypeConfigRequest =
        RequestsProto.UpdateExerciseTypeConfigRequest.newBuilder()
            .setPackageName(packageName)
            .setConfig(exerciseTypeConfig.toProto())
            .build()

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<UpdateExerciseTypeConfigRequest> = newCreator { bytes ->
            val proto = RequestsProto.UpdateExerciseTypeConfigRequest.parseFrom(bytes)
            UpdateExerciseTypeConfigRequest(
                proto.packageName,
                ExerciseTypeConfig.fromProto(proto.config)
            )
        }
    }
}
