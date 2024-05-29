/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.health.services.client.data.DebouncedGoal
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.RequestsProto

/** Request for adding or removing a [DebouncedGoal] for an exercise. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
data class DebouncedGoalRequest(val packageName: String, val debouncedGoal: DebouncedGoal<*>) :
    ProtoParcelable<RequestsProto.DebouncedGoalRequest>() {
    override val proto: RequestsProto.DebouncedGoalRequest
        get() =
            RequestsProto.DebouncedGoalRequest.newBuilder()
                .setPackageName(packageName)
                .setDebouncedGoal(debouncedGoal.proto)
                .build()

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DebouncedGoalRequest> = newCreator { bytes ->
            val proto = RequestsProto.DebouncedGoalRequest.parseFrom(bytes)
            DebouncedGoalRequest(proto.packageName, DebouncedGoal.fromProto(proto.debouncedGoal))
        }
    }
}
