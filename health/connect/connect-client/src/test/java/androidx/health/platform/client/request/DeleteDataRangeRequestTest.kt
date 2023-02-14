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

import android.os.Parcel
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.RequestProto
import androidx.health.platform.client.proto.TimeProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

// Checks ipc serialization/deserialization
@RunWith(AndroidJUnit4::class)
class DeleteDataRangeRequestTest {

    @Test
    fun writeToParcel_emptyTimeSpec() {
        val deleteDataRangeRequest =
            DeleteDataRangeRequest(
                RequestProto.DeleteDataRangeRequest.newBuilder()
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                    .build()
            )

        val parcel: Parcel = Parcel.obtain()
        parcel.writeParcelable(deleteDataRangeRequest, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val out: DeleteDataRangeRequest? =
            parcel.readParcelable(DeleteDataRangeRequest::class.java.classLoader)
        assertThat(out?.proto).isEqualTo(deleteDataRangeRequest.proto)
    }

    @Test
    fun writeToParcel_physicalTimeSpec() {
        val deleteDataRangeRequest =
            DeleteDataRangeRequest(
                RequestProto.DeleteDataRangeRequest.newBuilder()
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                    .setTimeSpec(
                        TimeProto.TimeSpec.newBuilder()
                            .setStartTimeEpochMs(1234L)
                            .setEndTimeEpochMs(1235L)
                            .build()
                    )
                    .build()
            )

        val parcel: Parcel = Parcel.obtain()
        parcel.writeParcelable(deleteDataRangeRequest, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val out: DeleteDataRangeRequest? =
            parcel.readParcelable(DeleteDataRangeRequest::class.java.classLoader)
        assertThat(out?.proto).isEqualTo(deleteDataRangeRequest.proto)
    }

    @Test
    fun writeToParcel_localTimeSpec() {
        val deleteDataRangeRequest =
            DeleteDataRangeRequest(
                RequestProto.DeleteDataRangeRequest.newBuilder()
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                    .setTimeSpec(
                        TimeProto.TimeSpec.newBuilder()
                            .setStartLocalDateTime("date1")
                            .setEndLocalDateTime("date2")
                            .build()
                    )
                    .build()
            )

        val parcel: Parcel = Parcel.obtain()
        parcel.writeParcelable(deleteDataRangeRequest, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val out: DeleteDataRangeRequest? =
            parcel.readParcelable(DeleteDataRangeRequest::class.java.classLoader)
        assertThat(out?.proto).isEqualTo(deleteDataRangeRequest.proto)
    }

    @Test
    fun writeToParcel_multipleDataTypes() {
        val deleteDataRangeRequest =
            DeleteDataRangeRequest(
                RequestProto.DeleteDataRangeRequest.newBuilder()
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                    .addDataType(DataProto.DataType.newBuilder().setName("HeartRate").build())
                    .setTimeSpec(
                        TimeProto.TimeSpec.newBuilder()
                            .setStartTimeEpochMs(1234L)
                            .setEndTimeEpochMs(1235L)
                            .build()
                    )
                    .build()
            )

        val parcel: Parcel = Parcel.obtain()
        parcel.writeParcelable(deleteDataRangeRequest, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val out: DeleteDataRangeRequest? =
            parcel.readParcelable(DeleteDataRangeRequest::class.java.classLoader)
        assertThat(out?.proto).isEqualTo(deleteDataRangeRequest.proto)
    }
}
