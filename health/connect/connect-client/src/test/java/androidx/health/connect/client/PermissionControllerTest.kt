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

package androidx.health.connect.client

import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.health.connect.client.permission.HealthPermission
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private const val PROVIDER_PACKAGE_NAME = "com.example.fake.provider"

@RunWith(AndroidJUnit4::class)
class PermissionControllerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createIntent_permissionStrings() {
        val requestPermissionContract =
            PermissionController.createRequestPermissionResultContract(PROVIDER_PACKAGE_NAME)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.READ_ACTIVE_CALORIES_BURNED)
            )

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(PROVIDER_PACKAGE_NAME)
    }

    @Test
    @Config(minSdk = VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createIntent_UpsideDownCake() {
        val requestPermissionContract =
            PermissionController.createRequestPermissionResultContract(PROVIDER_PACKAGE_NAME)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.WRITE_STEPS, HealthPermission.READ_DISTANCE)
            )

        assertThat(intent.action).isEqualTo(RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS)
        assertThat(intent.getStringArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS))
            .asList()
            .containsExactly(HealthPermission.WRITE_STEPS, HealthPermission.READ_DISTANCE)
        assertThat(intent.`package`).isNull()
    }

    @Test
    @Config(minSdk = VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createIntentLegacy_UpsideDownCake() {
        val requestPermissionContract =
            PermissionController.createRequestPermissionResultContractLegacy(PROVIDER_PACKAGE_NAME)
        val intent =
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.WRITE_STEPS, HealthPermission.READ_DISTANCE)
            )

        assertThat(intent.action).isEqualTo("androidx.health.ACTION_REQUEST_PERMISSIONS")
        assertThat(intent.`package`).isEqualTo(PROVIDER_PACKAGE_NAME)
    }
}
