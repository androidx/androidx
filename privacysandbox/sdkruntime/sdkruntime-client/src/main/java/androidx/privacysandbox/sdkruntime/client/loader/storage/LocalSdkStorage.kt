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

package androidx.privacysandbox.sdkruntime.client.loader.storage

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig

/**
 * Provides interface for getting SDK related files.
 */
internal interface LocalSdkStorage {
    /**
     * Get [LocalSdkDexFiles] for bundled SDK.
     *
     * @param sdkConfig sdk config
     * @return [LocalSdkDexFiles] if DEX files available or null if not.
     */
    fun dexFilesFor(sdkConfig: LocalSdkConfig): LocalSdkDexFiles?
}
