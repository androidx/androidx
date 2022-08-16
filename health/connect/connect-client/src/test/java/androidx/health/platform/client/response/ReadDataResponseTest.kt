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
package androidx.health.platform.client.response

import android.os.Parcel
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.ResponseProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadDataResponseTest {

    @Test
    fun writeToParcel() {
        val readDataResponse =
            ReadDataResponse(
                ResponseProto.ReadDataResponse.newBuilder()
                    .setDataPoint(
                        DataProto.DataPoint.newBuilder()
                            .setStartTimeMillis(1234L)
                            .addSeriesValues(
                                DataProto.SeriesValue.newBuilder()
                                    .setInstantTimeMillis(1245L)
                                    .putValues(
                                        "bpm",
                                        DataProto.Value.newBuilder().setLongVal(120).build()
                                    )
                                    .build()
                            )
                            .addSeriesValues(
                                DataProto.SeriesValue.newBuilder()
                                    .setInstantTimeMillis(1247L)
                                    .putValues(
                                        "bpm",
                                        DataProto.Value.newBuilder().setLongVal(140).build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

        val parcel: Parcel = Parcel.obtain()
        parcel.writeParcelable(readDataResponse, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val out: ReadDataResponse? = parcel.readParcelable(ReadDataResponse::class.java.classLoader)
        Truth.assertThat(out?.proto).isEqualTo(readDataResponse.proto)
    }
}
