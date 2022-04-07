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
package androidx.health.data.client.permission

import android.content.Context
import android.content.Intent
import androidx.health.data.client.HealthDataClient
import androidx.health.data.client.records.Steps
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.service.HealthDataServiceConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_PACKAGE = "com.test.app"

@RunWith(AndroidJUnit4::class)
class HealthDataRequestPermissionsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntentTest() {
        val requestPermissionContract = HealthDataRequestPermissions(TEST_PACKAGE)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(Permission.create<Steps>(AccessTypes.READ))
            )

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(TEST_PACKAGE)
    }

    @Test
    fun createIntent_defaultPackage() {
        val requestPermissionContract = HealthDataRequestPermissions()
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(Permission.create<Steps>(AccessTypes.READ))
            )

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(HealthDataClient.DEFAULT_PROVIDER_PACKAGE_NAME)
    }

    @Test
    fun parseIntent_null_fallback() {
        val requestPermissionContract = HealthDataRequestPermissions(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, null)

        assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestPermissionContract = HealthDataRequestPermissions(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, Intent())

        assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent() {
        val requestPermissionContract = HealthDataRequestPermissions(TEST_PACKAGE)
        val intent = Intent()
        intent.putParcelableArrayListExtra(
            HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_JETPACK,
            arrayListOf(
                androidx.health.platform.client.permission.Permission(
                    PermissionProto.Permission.newBuilder()
                        .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                        .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                        .build()
                )
            )
        )
        val result = requestPermissionContract.parseResult(0, intent)

        assertThat(result).containsExactly(Permission.create<Steps>(AccessTypes.READ))
    }

    @Test
    fun synchronousResult_null() {
        val requestPermissionContract = HealthDataRequestPermissions(TEST_PACKAGE)
        val result =
            requestPermissionContract.getSynchronousResult(
                context,
                setOf(Permission.create<Steps>(AccessTypes.READ))
            )

        assertThat(result).isNull()
    }
}
