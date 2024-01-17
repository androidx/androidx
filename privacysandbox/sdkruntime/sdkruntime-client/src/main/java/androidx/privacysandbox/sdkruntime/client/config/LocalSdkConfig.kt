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
package androidx.privacysandbox.sdkruntime.client.config

/**
 * Information required for loading SDK bundled with App.
 *
 */
internal data class LocalSdkConfig(
    val packageName: String,
    val versionMajor: Int? = null,
    val dexPaths: List<String>,
    val entryPoint: String,
    val javaResourcesRoot: String? = null,
    val resourceRemapping: ResourceRemappingConfig? = null
)

internal data class ResourceRemappingConfig(
    val rPackageClassName: String,
    val packageId: Int
)
