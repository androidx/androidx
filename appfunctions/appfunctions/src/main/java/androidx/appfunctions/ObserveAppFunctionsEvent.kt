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

package androidx.appfunctions

import androidx.appfunctions.metadata.AppFunctionName
import java.util.Objects

/**
 * Represents a change event in the registered app functions observed by
 * [AppFunctionManager.observeAppFunctions].
 */
public abstract class ObserveAppFunctionsEvent private constructor() {
    /**
     * Triggered when changes occur to a package exposing app functions that may impact the state or
     * metadata of its contained functions.
     *
     * This includes changes such as:
     * - The definition of a function metadata exposed by the package has changed (e.g, the
     *   parameters of the function have changed).
     * - All functions within the package are added or removed due to the package being installed or
     *   uninstalled.
     * - The package's [androidx.appfunctions.metadata.AppFunctionPackageMetadata] has been updated.
     *
     * Upon receiving this notification, clients can call [AppFunctionManager.searchAppFunctions]
     * with [AppFunctionSearchSpec.packageNames] to retrieve the updated
     * [androidx.appfunctions.metadata.AppFunctionMetadata] for affected functions.
     *
     * Clients should call [AppFunctionManager.getAppFunctionStates] to retrieve the latest
     * [AppFunctionState] for packages affected by these changes.
     *
     * **Note:** If packages are reported to have changed but are not returned from
     * [AppFunctionManager.searchAppFunctions], it means that the packages have been uninstalled or
     * no longer have functions.
     */
    public class MetadataChanged(
        /** The set of package names that have changed. */
        public val changedPackageNames: Set<String>
    ) : ObserveAppFunctionsEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MetadataChanged
            return changedPackageNames == other.changedPackageNames
        }

        override fun hashCode(): Int {
            return Objects.hash(changedPackageNames)
        }

        override fun toString(): String {
            return "MetadataChanged(changedPackageNames=$changedPackageNames)"
        }
    }

    /**
     * Triggered when the runtime state of one or more app functions changes.
     *
     * Upon receiving this notification, clients can call [AppFunctionManager.getAppFunctionStates]
     * to retrieve the updated [AppFunctionState] for the affected functions.
     */
    public class StatesChanged(
        /** The set of [AppFunctionName]s representing functions whose state has changed. */
        public val changedFunctionNames: Set<AppFunctionName>
    ) : ObserveAppFunctionsEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StatesChanged
            return changedFunctionNames == other.changedFunctionNames
        }

        override fun hashCode(): Int {
            return Objects.hash(changedFunctionNames)
        }

        override fun toString(): String {
            return "StatesChanged(changedFunctionNames=$changedFunctionNames)"
        }
    }
}
