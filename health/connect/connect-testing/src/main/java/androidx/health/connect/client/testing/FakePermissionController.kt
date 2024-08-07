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

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission

/**
 * A fake [PermissionController] that enables full control of permissions in tests for a
 * [HealthConnectClient].
 *
 * @param grantAll grants all permissions on creation
 */
@ExperimentalTestingApi
public class FakePermissionController(grantAll: Boolean = true) : PermissionController {
    private val grantedPermissions =
        if (grantAll) HealthPermission.ALL_PERMISSIONS.toMutableSet() else mutableSetOf()

    /** Replaces the set of permissions returned by [getGrantedPermissions] with a new set. */
    public fun replaceGrantedPermissions(permissions: Set<String>) {
        grantedPermissions.clear()
        grantedPermissions.addAll(permissions)
    }

    /** Adds a permission to the set of granted permissions returned by [getGrantedPermissions]. */
    public fun grantPermission(permission: String) {
        grantedPermissions.add(permission)
    }

    /** Adds permissions to the set of granted permissions returned by [getGrantedPermissions]. */
    public fun grantPermissions(permission: Set<String>) {
        grantedPermissions.addAll(permission)
    }

    /**
     * Removes a permission from the set of granted permissions returned by [getGrantedPermissions].
     */
    public fun revokePermission(permission: String) {
        grantedPermissions.remove(permission)
    }

    /** Returns a fake set of permissions. */
    override suspend fun getGrantedPermissions(): Set<String> {
        return grantedPermissions.toSet()
    }

    /** Clears the set of permissions returned by [getGrantedPermissions]. */
    override suspend fun revokeAllPermissions() {
        grantedPermissions.clear()
    }
}
