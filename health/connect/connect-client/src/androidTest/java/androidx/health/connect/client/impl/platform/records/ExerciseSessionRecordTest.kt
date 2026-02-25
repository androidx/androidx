/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform.records

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.AssumeRule
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.readRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ExerciseSessionRecordTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    private val assumeRule: AssumeRule =
        AssumeRule(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15)

    private val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *listOf(
                    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                )
                .toTypedArray()
        )

    @get:Rule val ruleChain: RuleChain = RuleChain.outerRule(assumeRule).around(grantPermissionRule)

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(
            ExerciseSessionRecord::class,
            TimeRangeFilter.after(Instant.EPOCH),
        )
    }

    @Test
    fun insertAndReadRecord() = runTest {
        val record =
            ExerciseSessionRecord(
                startTime = START_TIME,
                startZoneOffset = ZoneOffset.UTC,
                endTime = START_TIME.plusSeconds(150),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                exerciseType = EXERCISE_TYPE_RUNNING,
                title = "Morning Run",
                notes = "Easy pace",
                exerciseRoute = null,
            )
        val response = healthConnectClient.insertRecords(listOf(record))
        val readRecord =
            healthConnectClient
                .readRecord(ExerciseSessionRecord::class, response.recordIdsList[0])
                .record

        assertThat(readRecord.title).isEqualTo("Morning Run")
        assertThat(readRecord.notes).isEqualTo("Easy pace")
    }

    @Test
    fun insertAndReadRecord_withRateOfPerceivedExertion() = runTest {
        Assume.assumeTrue(
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 21
        )
        val record =
            ExerciseSessionRecord(
                startTime = START_TIME,
                startZoneOffset = ZoneOffset.UTC,
                endTime = START_TIME.plusSeconds(150),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                exerciseType = EXERCISE_TYPE_RUNNING,
                rateOfPerceivedExertion = 4.5f,
                exerciseRoute = null,
            )
        val response = healthConnectClient.insertRecords(listOf(record))
        val readRecord =
            healthConnectClient
                .readRecord(ExerciseSessionRecord::class, response.recordIdsList[0])
                .record

        assertThat(readRecord.rateOfPerceivedExertion).isEqualTo(4.5f)
    }
}
