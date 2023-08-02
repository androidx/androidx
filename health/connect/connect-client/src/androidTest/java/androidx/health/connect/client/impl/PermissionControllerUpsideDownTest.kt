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

package androidx.health.connect.client.impl

import android.annotation.TargetApi
import android.health.connect.HealthPermissions
import android.os.Build
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class PermissionControllerUpsideDownTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(HealthPermissions.WRITE_STEPS, HealthPermissions.READ_DISTANCE)

    @Test
    fun getGrantedPermissions() = runTest {
        val permissionController: PermissionController =
            HealthConnectClientUpsideDownImpl(ApplicationProvider.getApplicationContext())
        // Permissions may have been granted by the other instrumented test in this directory.
        // Since there is no way to revoke permissions with grantPermissionRule, use containsAtLeast
        // instead of containsExactly.
        assertThat(permissionController.getGrantedPermissions())
            .containsAtLeast(HealthPermissions.WRITE_STEPS, HealthPermissions.READ_DISTANCE)
    }

    @Test
    fun revokeAllPermissions_revokesHealthPermissions() = runTest {
        val revokedPermissions: MutableList<String> = mutableListOf()
        val permissionController: PermissionController =
            HealthConnectClientUpsideDownImpl(ApplicationProvider.getApplicationContext()) {
                permissionsToRevoke ->
                revokedPermissions.addAll(permissionsToRevoke)
            }
        permissionController.revokeAllPermissions()
        assertThat(revokedPermissions.all { it.startsWith(PERMISSION_PREFIX) }).isTrue()
    }
}
