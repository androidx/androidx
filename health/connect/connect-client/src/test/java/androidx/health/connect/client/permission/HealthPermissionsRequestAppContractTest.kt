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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_STRING
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_REQUESTED_PERMISSIONS_STRING
import androidx.health.platform.client.utils.sBypassSignatureCheckForTesting
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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

private const val TEST_PACKAGE = "com.test.app"

@RunWith(AndroidJUnit4::class)
class HealthPermissionsRequestAppContractTest {

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
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE),
            )

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(TEST_PACKAGE)
        assertThat(intent.`package`).isEqualTo(TEST_PACKAGE)

        @Suppress("Deprecation")
        assertThat(intent.getParcelableArrayListExtra<Permission>(KEY_REQUESTED_PERMISSIONS_STRING))
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
                    ),
                )
            )
    }

    @Test
    fun createIntent_defaultPackage() {
        val requestPermissionContract = HealthPermissionsRequestAppContract()
        val intent =
            requestPermissionContract.createIntent(context, setOf(HealthPermission.READ_STEPS))

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(DEFAULT_PROVIDER_PACKAGE_NAME)
    }

    @Test
    fun createIntent_emptyProviderPackageName_throwsIllegalArgumentException() {
        val requestPermissionContract = HealthPermissionsRequestAppContract("")
        assertThrows(IllegalArgumentException::class.java) {
            requestPermissionContract.createIntent(context, setOf(HealthPermission.READ_STEPS))
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

        val requestPermissionContract = HealthPermissionsRequestAppContract()
        assertThrows(SecurityException::class.java) {
            requestPermissionContract.createIntent(mockContext, setOf(HealthPermission.READ_STEPS))
        }
    }

    @Test
    fun parseIntent_null_fallback() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, null)

        assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent_emptyIntent() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result = requestPermissionContract.parseResult(0, Intent())

        assertThat(result).isEmpty()
    }

    @Test
    fun parseIntent() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val intent = Intent()
        intent.putParcelableArrayListExtra(
            KEY_GRANTED_PERMISSIONS_STRING,
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
                ),
            ),
        )
        val result = requestPermissionContract.parseResult(0, intent)

        assertThat(result)
            .containsExactly(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
    }

    @Test
    fun synchronousResult_null() {
        val requestPermissionContract = HealthPermissionsRequestAppContract(TEST_PACKAGE)
        val result =
            requestPermissionContract.getSynchronousResult(
                context,
                setOf(HealthPermission.READ_STEPS),
            )

        assertThat(result).isNull()
    }
}
