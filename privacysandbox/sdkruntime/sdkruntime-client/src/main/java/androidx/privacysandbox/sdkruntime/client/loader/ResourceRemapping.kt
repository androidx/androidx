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

package androidx.privacysandbox.sdkruntime.client.loader

import androidx.privacysandbox.sdkruntime.client.config.ResourceRemappingConfig

/**
 * Update RPackage.packageId for supporting Android Resource remapping for SDK.
 * Each resource has id calculated as id = RPackage.packageId + index.
 * Updating packageId effectively shifting all SDK resource ids in resource table.
 * IMPORTANT: ResourceRemapping should happen before ANY interactions with R.class
 */
internal object ResourceRemapping {

    private const val PACKAGE_ID_FIELD_NAME = "packageId"

    fun apply(
        sdkClassLoader: ClassLoader,
        remappingConfig: ResourceRemappingConfig?
    ) {
        if (remappingConfig == null)
            return

        val rPackageClass = Class.forName(
            remappingConfig.rPackageClassName,
            /* initialize = */ false,
            sdkClassLoader
        )

        val field = rPackageClass.getDeclaredField(PACKAGE_ID_FIELD_NAME)

        field.setInt(null, remappingConfig.packageId)
    }
}
