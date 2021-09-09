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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto

/**
 * A place holder class that represents the capabilities of the
 * [androidx.health.services.client.MeasureClient] on the device.
 */
@Suppress("ParcelCreator")
public class MeasureCapabilities(
    /**
     * Set of supported [DataType] s for measure capture on this device.
     *
     * Some data types are not available for measurement; this is typically used to measure health
     * data (e.g. HR).
     */
    public val supportedDataTypesMeasure: Set<DataType>,
) : ProtoParcelable<DataProto.MeasureCapabilities>() {

    internal constructor(
        proto: DataProto.MeasureCapabilities
    ) : this(proto.supportedDataTypesList.map { DataType(it) }.toSet())

    /** @hide */
    override val proto: DataProto.MeasureCapabilities by lazy {
        DataProto.MeasureCapabilities.newBuilder()
            .addAllSupportedDataTypes(supportedDataTypesMeasure.map { it.proto })
            .build()
    }

    override fun toString(): String =
        "MeasureCapabilities(supportedDataTypesMeasure=$supportedDataTypesMeasure)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<MeasureCapabilities> = newCreator { bytes ->
            val proto = DataProto.MeasureCapabilities.parseFrom(bytes)
            MeasureCapabilities(proto)
        }
    }
}
