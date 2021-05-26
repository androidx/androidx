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

import androidx.annotation.NonNull;

import java.util.List;

/**
 * A listener with the results from a permissions request.
 */
public interface OnRequestPermissionsListener {
    /**
     * Provides which permissions were approved and which were rejected by the user.
     *
     * @param grantedPermissions  the permissions that the user granted
     * @param rejectedPermissions the permissions that the user rejected
     */
    void onRequestPermissionsResult(@NonNull List<String> grantedPermissions,
            @NonNull List<String> rejectedPermissions);
}
