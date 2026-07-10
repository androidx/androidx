/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.registry.provider

/**
 * A request to clear the creation options stored for your app, which were registered using the
 * [RegistryManager.registerCreationOptions] API.
 *
 * @property isDeleteAll whether to delete all creation options for your app
 * @property deletePerTypeConfig an option to clear the creation options for a given type matching
 *   the [RegisterCreationOptionsRequest.type] provided during the
 *   [RegistryManager.registerCreationOptions] call
 * @constructor
 */
public class ClearCreationOptionsRequest
private constructor(
    public val isDeleteAll: Boolean,
    public val deletePerTypeConfig: PerTypeConfig?,
) {
    /**
     * Constructs a [ClearCreationOptionsRequest]
     *
     * @param isDeleteAll whether to delete all creation options for your app
     */
    public constructor(
        isDeleteAll: Boolean
    ) : this(isDeleteAll = isDeleteAll, deletePerTypeConfig = null)

    /**
     * Constructs a [ClearCreationOptionsRequest]
     *
     * @param deletePerTypeConfig an option to clear the creation options for a given type matching
     *   the [RegisterCreationOptionsRequest.type] provided during the
     *   [RegistryManager.registerCreationOptions] call
     */
    public constructor(
        deletePerTypeConfig: PerTypeConfig
    ) : this(isDeleteAll = false, deletePerTypeConfig = deletePerTypeConfig)

    /**
     * Configures how to clear the creation options for a given type.
     *
     * @param isDeleteAll whether to delete all creation options for the given type
     * @param type the type of creation options to clear, matching the
     *   [RegisterCreationOptionsRequest.type] provided provided during the
     *   [RegistryManager.registerCreationOptions] call
     * @param registryIds the IDs of the creation options for the given type to delete, matching one
     *   or more [RegisterCreationOptionsRequest.id] used during creation options registration
     * @constructor constructs an instance of [PerTypeConfig]
     */
    public class PerTypeConfig(
        public val isDeleteAll: Boolean,
        public val type: String,
        public val registryIds: List<String>,
    )
}
