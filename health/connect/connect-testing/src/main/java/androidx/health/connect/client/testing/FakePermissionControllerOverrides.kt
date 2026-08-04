/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.health.connect.client.testing.stubs.Stub

/**
 * Used to override or intercept responses to emulate scenarios that [FakePermissionController]
 * doesn't support directly, such as throwing an exception or applying custom logic to the
 * responses.
 *
 * Every call in [FakePermissionController] can be overridden.
 *
 * @param getGrantedPermissions A [Stub] used to set the next responses used in
 *   [FakePermissionController.getGrantedPermissions].
 * @param revokeAllPermissions A [Stub] used to set the next responses used in
 *   [FakePermissionController.revokeAllPermissions].
 */
public class FakePermissionControllerOverrides(
    /**
     * A [Stub] used to set the next responses used in [getGrantedPermissions].
     *
     * Once all stubbed responses are consumed (or if the stub is `null`),
     * [FakePermissionController.getGrantedPermissions] will fall back to its default behavior of
     * returning the set of granted permissions.
     */
    public var getGrantedPermissions: Stub<Unit, Set<String>>? = null,

    /**
     * A [Stub] used to set the next responses used in [revokeAllPermissions].
     *
     * Once all stubbed responses are consumed (or if the stub is `null`),
     * [FakePermissionController.revokeAllPermissions] will fall back to its default behavior of
     * clearing the set of granted permissions.
     */
    public var revokeAllPermissions: Stub<Unit, Unit>? = null,
)
