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

@file:SuppressLint("NewApi")

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
class RecordConvertersTest {

    private var originalUExtension: Any? = null

    @Before
    fun setUp() {
        originalUExtension =
            ReflectionHelpers.getStaticField(SdkExtensions::class.java, "U_EXTENSION_INT")
        ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", 16)
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(
            SdkExtensions::class.java,
            "U_EXTENSION_INT",
            originalUExtension,
        )
    }

    @Test
    fun toSdkRecordClass_mappedRecords_returnsSdkClass() {
        assertThat(android.health.connect.datatypes.StepsRecord::class.java.toSdkRecordClass())
            .isEqualTo(StepsRecord::class)
        assertThat(android.health.connect.datatypes.HeartRateRecord::class.java.toSdkRecordClass())
            .isEqualTo(HeartRateRecord::class)
    }

    @Test
    fun toSdkRecordClass_abstractRecord_returnsNull() {
        assertThat(android.health.connect.datatypes.Record::class.java.toSdkRecordClass()).isNull()
    }

    @Test
    fun toSdkRecordClass_ext13Record() {
        assertThat(
                android.health.connect.datatypes.PlannedExerciseSessionRecord::class
                    .java
                    .toSdkRecordClass()
            )
            .isEqualTo(androidx.health.connect.client.records.PlannedExerciseSessionRecord::class)
    }

    @Test
    fun toSdkRecordClass_ext15Record() {
        assertThat(
                android.health.connect.datatypes.MindfulnessSessionRecord::class
                    .java
                    .toSdkRecordClass()
            )
            .isEqualTo(androidx.health.connect.client.records.MindfulnessSessionRecord::class)
    }

    @Test
    fun toSdkRecordClass_ext16Record() {
        assertThat(
                android.health.connect.datatypes.ActivityIntensityRecord::class
                    .java
                    .toSdkRecordClass()
            )
            .isEqualTo(androidx.health.connect.client.records.ActivityIntensityRecord::class)
    }
}
