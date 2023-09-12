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

package androidx.health.connect.client.impl

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.os.Build
import androidx.health.connect.client.impl.platform.records.PlatformExerciseRoute
import androidx.health.connect.client.impl.platform.records.PlatformExerciseRouteLocationBuilder
import androidx.health.connect.client.impl.platform.records.PlatformLength
import androidx.health.connect.client.permission.platform.ExerciseRouteRequestModuleContract
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.units.Length
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class ExerciseRouteRequestModuleContractTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntentTest() {
        val requestRouteContract = ExerciseRouteRequestModuleContract()
        val intent = requestRouteContract.createIntent(context, "someUid")
        assertThat(intent.action).isEqualTo("android.health.connect.action.REQUEST_EXERCISE_ROUTE")
        assertThat(intent.getStringExtra("android.health.connect.extra.SESSION_ID"))
            .isEqualTo("someUid")
    }

    @Test
    fun parseIntent_null() {
        val requestRouteContract = ExerciseRouteRequestModuleContract()
        val result = requestRouteContract.parseResult(0, null)
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestRouteContract = ExerciseRouteRequestModuleContract()
        val result = requestRouteContract.parseResult(0, Intent())
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyRoute() {
        val requestRouteContract = ExerciseRouteRequestModuleContract()
        val intent = Intent()
        intent.putExtra(HealthConnectManager.EXTRA_EXERCISE_ROUTE, PlatformExerciseRoute(listOf()))
        val result = requestRouteContract.parseResult(0, intent)
        assertThat(result).isEqualTo(ExerciseRoute(listOf()))
    }

    @Test
    fun parseIntent() {
        val requestRouteContract = ExerciseRouteRequestModuleContract()
        val intent = Intent()
        val location1 =
            PlatformExerciseRouteLocationBuilder(Instant.ofEpochMilli(1234L), 23.4, -23.4)
                .setAltitude(PlatformLength.fromMeters(12.3))
                .setHorizontalAccuracy(PlatformLength.fromMeters(0.9))
                .setVerticalAccuracy(PlatformLength.fromMeters(0.3))
                .build()
        val location2 =
            PlatformExerciseRouteLocationBuilder(Instant.ofEpochMilli(3456L), 23.45, -23.45).build()

        intent.putExtra(
            HealthConnectManager.EXTRA_EXERCISE_ROUTE,
            PlatformExerciseRoute(listOf(location1, location2))
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
