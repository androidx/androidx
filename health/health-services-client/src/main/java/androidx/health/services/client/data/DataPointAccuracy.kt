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

package androidx.health.services.client.data

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.AccuracyCase.ACCURACY_NOT_SET
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.AccuracyCase.HR_ACCURACY
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.AccuracyCase.LOCATION_ACCURACY

/** Accuracy of a [DataPoint]. */
@Suppress("ParcelCreator", "ParcelNotFinal")
public abstract class DataPointAccuracy : ProtoParcelable<DataProto.DataPointAccuracy>() {

    internal companion object {
        internal fun fromProto(proto: DataProto.DataPointAccuracy): DataPointAccuracy =
            when (proto.accuracyCase) {
                HR_ACCURACY -> HrAccuracy(proto)
                LOCATION_ACCURACY -> LocationAccuracy(proto)
                null, ACCURACY_NOT_SET -> throw IllegalStateException("Accuracy not set on $proto")
            }
    }
}
