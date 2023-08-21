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

package androidx.health.connect.client.contracts

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.platform.client.service.HealthDataServiceConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
class HealthPermissionsRequestContractTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun createIntent_nonHealthPermission_throwsIAE() {
        val requestPermissionContract = HealthPermissionsRequestContract()
        assertFailsWith<IllegalArgumentException> {
            requestPermissionContract.createIntent(
                context,
                setOf(HealthPermission.READ_STEPS, "NON_HEALTH_PERMISSION")
            )
        }
    }

    @Test
    fun createIntent_noPermissions_throwsIAE() {
        val requestPermissionContract = HealthPermissionsRequestContract()
        assertFailsWith<IllegalArgumentException> {
            requestPermissionContract.createIntent(context, setOf())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createIntent_hasPlatformIntentAction() {
        val intent =
            HealthPermissionsRequestContract()
                .createIntent(context, setOf(HealthPermission.READ_STEPS))
        assertThat(intent.action)
            .isEqualTo(
                ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS
            )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun createIntent_hasApkIntentAction() {
        val intent =
            HealthPermissionsRequestContract()
                .createIntent(context, setOf(HealthPermission.READ_STEPS))
        assertThat(intent.action).isEqualTo(HealthDataServiceConstants.ACTION_REQUEST_PERMISSIONS)
        assertThat(intent.`package`).isEqualTo(HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun createIntent_hasApkIntentActionAndProvider() {
        val intent =
            HealthPermissionsRequestContract("some.provider")
                .createIntent(context, setOf(HealthPermission.READ_STEPS))
        assertThat(intent.action).isEqualTo(HealthDataServiceConstants.ACTION_REQUEST_PERMISSIONS)
        assertThat(intent.`package`).isEqualTo("some.provider")
    }
}
