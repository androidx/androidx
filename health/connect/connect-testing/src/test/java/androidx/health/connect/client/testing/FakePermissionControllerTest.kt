/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.health.connect.client.testing

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_EXERCISE_ROUTE
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** Unit tests for [FakePermissionController]. */
class FakePermissionControllerTest {

    @Test
    fun grantAll_grantsWriteAndRead() = runTest {
        val controller = FakePermissionController(grantAll = true)
        val permissions = controller.getGrantedPermissions()
        assertThat(permissions.size).isAtLeast(HealthPermission.ALL_PERMISSIONS.toSet().size)
    }

    @Test
    fun grantAll_grantsExtraPermissions() = runTest {
        val controller = FakePermissionController(grantAll = true)
        val permissions = controller.getGrantedPermissions()
        assertThat(permissions).contains(PERMISSION_WRITE_EXERCISE_ROUTE)
        assertThat(permissions).contains(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
    }

    @Test
    fun grantAllFalse_noPermissions() = runTest {
        val controller = FakePermissionController(grantAll = false)
        val permissions = controller.getGrantedPermissions()
        assertThat(permissions).hasSize(0)
    }

    @Test
    fun grantPermission_grantsPermission() = runTest {
        val controller = FakePermissionController(grantAll = false)
        controller.grantPermission(PERMISSION_WRITE_EXERCISE_ROUTE)
        val permissions = controller.getGrantedPermissions()
        assertThat(permissions).contains(PERMISSION_WRITE_EXERCISE_ROUTE)
    }

    @Test
    fun revokeAllPermissions_noPermissions() = runTest {
        val controller = FakePermissionController(grantAll = true)
        controller.revokeAllPermissions()
        val permissions = controller.getGrantedPermissions()
        assertThat(permissions).hasSize(0)
    }

    @Test
    fun grantPermissions_doesntReplace() = runTest {
        val controller = FakePermissionController(grantAll = false)
        controller.grantPermission("permission1")
        controller.grantPermissions(setOf("permission2"))
        assertThat(controller.getGrantedPermissions()).hasSize(2)
    }

    @Test
    fun revokePermissions() = runTest {
        val controller = FakePermissionController(grantAll = false)
        controller.grantPermission("permission1")
        controller.grantPermission("permission2")
        controller.revokePermission("permission1")
        assertThat(controller.getGrantedPermissions()).hasSize(1)
    }

    @Test
    fun replaceGrantedPermissions() = runTest {
        val controller = FakePermissionController(grantAll = false)
        controller.grantPermission("permission1")
        controller.grantPermission("permission2")
        controller.replaceGrantedPermissions(setOf("permission1", "permission3"))
        assertThat(controller.getGrantedPermissions())
            .isEqualTo(setOf("permission1", "permission3"))
    }
}
