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
package androidx.health.connect.client.permission

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.units.Length
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.service.HealthDataServiceConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestExerciseRouteInternalTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntentTest() {
        val requestRouteContract = RequestExerciseRouteInternal()
        val intent = requestRouteContract.createIntent(context, "someUid")
        assertThat(intent.action).isEqualTo("androidx.health.action.REQUEST_EXERCISE_ROUTE")
        assertThat(intent.getStringExtra(HealthDataServiceConstants.EXTRA_SESSION_ID))
            .isEqualTo("someUid")
    }

    @Test
    fun parseIntent_null() {
        val requestRouteContract = RequestExerciseRouteInternal()
        val result = requestRouteContract.parseResult(0, null)
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestRouteContract = RequestExerciseRouteInternal()
        val result = requestRouteContract.parseResult(0, Intent())
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyRoute() {
        val requestRouteContract = RequestExerciseRouteInternal()
        val intent = Intent()
        intent.putExtra(
            HealthDataServiceConstants.EXTRA_EXERCISE_ROUTE,
            androidx.health.platform.client.exerciseroute.ExerciseRoute(
                DataProto.DataPoint.SubTypeDataList.newBuilder().build()
            )
        )
        val result = requestRouteContract.parseResult(0, intent)
        assertThat(result).isEqualTo(ExerciseRoute(listOf()))
    }

    @Test
    fun parseIntent() {
        val requestRouteContract = RequestExerciseRouteInternal()
        val intent = Intent()
        val protoLocation1 =
            DataProto.SubTypeDataValue.newBuilder()
                .setStartTimeMillis(1234L)
                .setEndTimeMillis(2345L)
                .putValues("latitude", DataProto.Value.newBuilder().setDoubleVal(23.4).build())
                .putValues("longitude", DataProto.Value.newBuilder().setDoubleVal(-23.4).build())
                .putValues("altitude", DataProto.Value.newBuilder().setDoubleVal(12.3).build())
                .putValues(
                    "horizontal_accuracy",
                    DataProto.Value.newBuilder().setDoubleVal(0.9).build()
                )
                .putValues(
                    "vertical_accuracy",
                    DataProto.Value.newBuilder().setDoubleVal(0.3).build()
                )
                .build()
        val protoLocation2 =
            DataProto.SubTypeDataValue.newBuilder()
                .setStartTimeMillis(3456L)
                .setEndTimeMillis(4567L)
                .putValues("latitude", DataProto.Value.newBuilder().setDoubleVal(23.45).build())
                .putValues("longitude", DataProto.Value.newBuilder().setDoubleVal(-23.45).build())
                .build()
        intent.putExtra(
            HealthDataServiceConstants.EXTRA_EXERCISE_ROUTE,
            androidx.health.platform.client.exerciseroute.ExerciseRoute(
                DataProto.DataPoint.SubTypeDataList.newBuilder()
                    .addAllValues(listOf(protoLocation1, protoLocation2))
                    .build()
            )
        )
        val result = requestRouteContract.parseResult(0, intent)
        assertThat(result)
            .isEqualTo(
                ExerciseRoute(
                    listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1234L),
                            latitude = 23.4,
                            longitude = -23.4,
                            horizontalAccuracy = Length.meters(0.9),
                            verticalAccuracy = Length.meters(0.3),
                            altitude = Length.meters(12.3)
                        ),
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(3456L),
                            latitude = 23.45,
                            longitude = -23.45,
                        )
                    )
                )
            )
    }
}
