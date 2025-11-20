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

package androidx.health.platform.client.exerciseroute

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.DataProto

/** Internal parcelable wrapper over proto object. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ExerciseRoute(override val proto: DataProto.DataPoint.SubTypeDataList) :
    ProtoParcelable<DataProto.DataPoint.SubTypeDataList>() {

    // ExerciseRoute is passed as an Intent extra, where shared memory isn't supported. See
    // b/442348082
    override fun shouldStoreInPlace() = true

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExerciseRoute> = newCreator {
            val proto = DataProto.DataPoint.SubTypeDataList.parseFrom(it)
            ExerciseRoute(proto)
        }
    }
}
