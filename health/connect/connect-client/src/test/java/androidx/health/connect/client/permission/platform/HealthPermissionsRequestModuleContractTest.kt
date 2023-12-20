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

package androidx.health.connect.client.permission.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.health.connect.client.permission.HealthPermission
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthPermissionsRequestModuleContractTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntent() {
        val requestPermissionContract = HealthPermissionsRequestModuleContract()
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
            )

        assertThat(intent.action).isEqualTo(RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS)
        assertThat(intent.getStringArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS))
            .asList()
            .containsExactly(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
    }

    @Test
    fun parseIntent() {
        val requestPermissionContract = HealthPermissionsRequestModuleContract()

        val intent = Intent()
        intent.putExtra(
            RequestMultiplePermissions.EXTRA_PERMISSIONS,
            arrayOf(
                HealthPermission.READ_STEPS,
                HealthPermission.WRITE_STEPS,
                HealthPermission.WRITE_DISTANCE,
                HealthPermission.READ_HEART_RATE
            )
        )
        intent.putExtra(
            RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS,
            intArrayOf(
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED
            )
        )

        val result = requestPermissionContract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result)
            .containsExactly(HealthPermission.READ_STEPS, HealthPermission.WRITE_DISTANCE)
    }
}
