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
package androidx.health.connect.client.permission

import androidx.health.connect.client.RECORD_CLASSES
import androidx.health.connect.client.records.ExerciseRouteRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthPermissionTest {

    @Test
    fun createReadPermission() {
        val permission = HealthPermission.createReadPermission(StepsRecord::class)
        assertThat(permission.accessType).isEqualTo(AccessTypes.READ)
        assertThat(permission.recordType).isEqualTo(StepsRecord::class)
    }

    @Test
    fun createWritePermission() {
        val permission = HealthPermission.createWritePermission(StepsRecord::class)
        assertThat(permission.accessType).isEqualTo(AccessTypes.WRITE)
        assertThat(permission.recordType).isEqualTo(StepsRecord::class)
    }

    @Test
    fun createReadPermissionInternal() {
        val permission = HealthPermission.createReadPermissionInternal(StepsRecord::class)
        assertThat(permission).isEqualTo(HealthPermission.READ_STEPS)
    }

    @Test
    fun createReadPermissionInternal_everyRecord() {
        RECORD_CLASSES.filterNot { it == ExerciseRouteRecord::class }
            .forEach {
                val permission = HealthPermission.createReadPermissionInternal(it)
                assertThat(permission).isNotNull()
            }
    }

    @Test
    fun createReadPermissionInternal_invalidRecord_isNull() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.createReadPermissionInternal(Record::class)
        }
    }

    @Test
    fun createWritePermissionInternal() {
        val permission = HealthPermission.createWritePermissionInternal(StepsRecord::class)
        assertThat(permission).isEqualTo(HealthPermission.WRITE_STEPS)
    }

    @Test
    fun createWritePermissionInternal_everyRecord() {
        RECORD_CLASSES.filterNot { it == ExerciseRouteRecord::class }
            .forEach {
                val permission = HealthPermission.createWritePermissionInternal(it)
                assertThat(permission).isNotNull()
            }
    }

    @Test
    fun createWritePermissionInternal_invalidRecord_isNull() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.createWritePermissionInternal(Record::class)
        }
    }
}
