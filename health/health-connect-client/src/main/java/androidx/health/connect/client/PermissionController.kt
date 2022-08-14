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
package androidx.health.connect.client

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.permission.Permission

/** Interface for operations related to permissions. */
interface PermissionController {

    /**
     * Creates an [ActivityResultContract] to request Health permissions.
     *
     * @see androidx.activity.ComponentActivity.registerForActivityResult
     * @sample androidx.health.connect.client.samples.RequestPermission
     */
    fun createRequestPermissionActivityContract():
        ActivityResultContract<Set<Permission>, Set<Permission>>

    /**
     * Returns a set of [Permission] granted by the user to the calling app, out of the input
     * [permissions] set.
     *
     * @param permissions set of permissions interested to check if granted or not
     * @return set of granted permissions.
     *
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws java.io.IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun getGrantedPermissions(permissions: Set<Permission>): Set<Permission>

    /**
     * Revokes all previously granted [Permission] by the user to the calling app.
     *
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws java.io.IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun revokeAllPermissions()
}
