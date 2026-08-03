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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.units.Length
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.platform.client.service.HealthDataServiceConstants.EXTRA_EXERCISE_ROUTE
import androidx.health.platform.client.service.HealthDataServiceConstants.EXTRA_SESSION_ID
import androidx.health.platform.client.utils.sBypassSignatureCheckForTesting
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class ExerciseRouteRequestAppContractTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sBypassSignatureCheckForTesting = true
    }

    @After
    fun tearDown() {
        sBypassSignatureCheckForTesting = false
    }

    @Test
    fun createIntentTest() {
        val requestRouteContract = ExerciseRouteRequestAppContract()
        val intent = requestRouteContract.createIntent(context, "someUid")
        assertThat(intent.action).isEqualTo("androidx.health.action.REQUEST_EXERCISE_ROUTE")
        assertThat(intent.getStringExtra(EXTRA_SESSION_ID)).isEqualTo("someUid")
        assertThat(intent.`package`).isEqualTo("com.google.android.apps.healthdata")
    }

    @Test
    fun createIntentTest_customProvider() {
        val requestRouteContract = ExerciseRouteRequestAppContract("custom.provider")
        val intent = requestRouteContract.createIntent(context, "someUid")
        assertThat(intent.action).isEqualTo("androidx.health.action.REQUEST_EXERCISE_ROUTE")
        assertThat(intent.getStringExtra(EXTRA_SESSION_ID)).isEqualTo("someUid")
        assertThat(intent.`package`).isEqualTo("custom.provider")
    }

    @Test
    fun createIntent_emptyProviderPackageName_throwsIllegalArgumentException() {
        val requestRouteContract = ExerciseRouteRequestAppContract("")
        assertThrows(IllegalArgumentException::class.java) {
            requestRouteContract.createIntent(context, "someUid")
        }
    }

    @Test
    fun createIntent_packageInstalledWithInvalidSignature_throwsSecurityException() {
        sBypassSignatureCheckForTesting = false
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfo =
            PackageInfo().apply { applicationInfo = ApplicationInfo().apply { enabled = true } }
        @Suppress("Deprecation")
        `when`(mockPackageManager.getPackageInfo(eq(DEFAULT_PROVIDER_PACKAGE_NAME), anyInt()))
            .thenReturn(packageInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            `when`(
                    mockPackageManager.getPackageInfo(
                        eq(DEFAULT_PROVIDER_PACKAGE_NAME),
                        any(PackageInfoFlags::class.java),
                    )
                )
                .thenReturn(packageInfo)
        }

        val requestRouteContract = ExerciseRouteRequestAppContract()
        assertThrows(SecurityException::class.java) {
            requestRouteContract.createIntent(mockContext, "someUid")
        }
    }

    @Test
    fun parseIntent_null() {
        val requestRouteContract = ExerciseRouteRequestAppContract()
        val result = requestRouteContract.parseResult(0, null)
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestRouteContract = ExerciseRouteRequestAppContract()
        val result = requestRouteContract.parseResult(0, Intent())
        assertThat(result).isNull()
    }

    @Test
    fun parseIntent_emptyRoute() {
        val requestRouteContract = ExerciseRouteRequestAppContract()
        val intent = Intent()
        intent.putExtra(
            EXTRA_EXERCISE_ROUTE,
            androidx.health.platform.client.exerciseroute.ExerciseRoute(
                DataProto.DataPoint.SubTypeDataList.newBuilder().build()
            ),
        )
        val result = requestRouteContract.parseResult(0, intent)
        assertThat(result).isEqualTo(ExerciseRoute(listOf()))
    }

    @Test
    fun parseIntent() {
        val requestRouteContract = ExerciseRouteRequestAppContract()
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
                    DataProto.Value.newBuilder().setDoubleVal(0.9).build(),
                )
                .putValues(
                    "vertical_accuracy",
                    DataProto.Value.newBuilder().setDoubleVal(0.3).build(),
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
            EXTRA_EXERCISE_ROUTE,
            androidx.health.platform.client.exerciseroute.ExerciseRoute(
                DataProto.DataPoint.SubTypeDataList.newBuilder()
                    .addAllValues(listOf(protoLocation1, protoLocation2))
                    .build()
            ),
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
                            altitude = Length.meters(12.3),
                        ),
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(3456L),
                            latitude = 23.45,
                            longitude = -23.45,
                        ),
                    )
                )
            )
    }
}
