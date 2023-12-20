/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core

/**
 * Information about runtime enabled SDK.
 * Could represent SDK loaded in sandbox or locally loaded SDK.
 */
class SandboxedSdkInfo(
    /**
     * Sdk Name.
     * This is a value of `android:name` attribute <sdk-library> tag of SDK Manifest.
     */
    val name: String,
    /**
     * Sdk Version.
     * This is a value of `android:versionMajor` attribute <sdk-library> tag of SDK Manifest.
     */
    val version: Long
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SandboxedSdkInfo

        if (name != other.name) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}
