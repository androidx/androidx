/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app;

/**
 * A listener with the results from a permissions request.
 *
 * <p>This listener is sent between the {@link CarAppService} and {@link CarAppPermissionActivity}
 * and therefore runs entirely on the client process.
 *
 * @hide
 */
interface IOnRequestPermissionsListener {
    /**
     * Provides the permission request's results to the caller.
     */
    void onRequestPermissionsResult(in String[] approvedPermissions, in String[]
            rejectedPermissions) = 1;
}