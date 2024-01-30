/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.service.HealthDataServiceConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_PACKAGE = "com.test.app"

@RunWith(AndroidJUnit4::class)
class HealthPermissionsRequestAppContractTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntentTest() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
            )

        Truth.assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        Truth.assertThat(intent.`package`).isEqualTo(TEST_PACKAGE)
        Truth.assertThat(intent.`package`).isEqualTo(TEST_PACKAGE)

        @Suppress("Deprecation")
        Truth.assertThat(
                intent.getParcelableArrayListExtra<Permission>(
                    HealthDataServiceConstants.KEY_REQUESTED_PERMISSIONS_STRING
                )
            )
            .isEqualTo(
                arrayListOf(
                    Permission(
                        PermissionProto.Permission.newBuilder()
                            .setPermission(HealthPermission.READ_STEPS)
                            .build()
                    ),
                    Permission(
                        PermissionProto.Permission.newBuilder()
                            .setPermission(HealthPermission.WRITE_DISTANCE)
                            .build()
                    )
                )
            )
    }

    @Test
    fun createIntent_defaultPackage() {
        val requestPermissionContract = HealthPermissionsRequestAppContract()
        val intent =
            requestPermissionContract.createIntent(context, setOf(HealthPermission.READ_STEPS))

        Truth.assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        Truth.assertThat(intent.`package`)
            .isEqualTo(HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)
    }

    @Test
    fun parseIntent_null_fallback() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, null)

        Truth.assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, Intent())

        Truth.assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val intent = Intent()
        intent.putParcelableArrayListExtra(
            HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_STRING,
            arrayListOf(
                Permission(
                    PermissionProto.Permission.newBuilder()
                        .setPermission(HealthPermission.READ_STEPS)
                        .build()
                ),
                Permission(
                    PermissionProto.Permission.newBuilder()
                        .setPermission(HealthPermission.WRITE_DISTANCE)
                        .build()
                )
            )
        )
        val result = requestPermissionContract.parseResult(0, intent)

        Truth.assertThat(result)
            .containsExactly(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
    }

    @Test
    fun synchronousResult_null() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result =
            requestPermissionContract.getSynchronousResult(
                context,
                setOf(HealthPermission.READ_STEPS)
            )

        Truth.assertThat(result).isNull()
    }
}
