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

package androidx.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Represents resources embedded within a class annotated with [AppFunctionSerializable].
 *
 * The consuming application is responsible for determining the optimal presentation of these
 * resources, whether for the end-user's benefit or for further consumption by an AI model.
 */
@AppFunctionSchemaCapability
public interface AppFunctionResourceContainer {

    /** List of resources embedded within the AppFunctionData. */
    public val resources: List<AppFunctionTextResource>
        get() = emptyList()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Retrieves the [AppFunctionResourceContainer] that is embedded within the
         * [AppFunctionData].
         *
         * Returns `null` if no [AppFunctionResourceContainer] is embedded within the
         * [AppFunctionData].
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @JvmStatic
        @RestrictTo(
            // TODO: b/452298527 - Expose as a public API once this API can validate against AllOf
            // spec.
            RestrictTo.Scope.LIBRARY_GROUP
        )
        public fun AppFunctionData.asAppFunctionResourceContainer(): AppFunctionResourceContainer? {
            // TODO: b/452298527 - Validate AppFunctionData.asAppFunctionResourceContainer is used
            // on an AllOf type only
            val appFunctionData = this
            val resourcesAsAppFunctionData =
                appFunctionData.getAppFunctionDataList(RESOURCES_PROPERTY)?.map {
                    it.deserialize(AppFunctionTextResource::class.java)
                } ?: return null

            return object : AppFunctionResourceContainer {
                override val resources: List<AppFunctionTextResource> = resourcesAsAppFunctionData
            }
        }

        private const val RESOURCES_PROPERTY = "resources"
    }
}
