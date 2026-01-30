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

package androidx.build

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import java.io.File
import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * If the user hasn't set up develocity on this machine then fetch a shared key to enable it for
 * them.
 */
internal fun Project.fetchDevelocityKeysIfNeeded() {
    // Playground users don't need Develocity set up
    if (ProjectLayoutType.isPlayground(this)) return

    // We are in CI, so we should not fetch these keys
    if (System.getenv("IS_ANDROIDX_CI") != null) return

    // User does not have remote cache enabled, so we will not have access to GCP
    if (System.getenv("USE_ANDROIDX_REMOTE_BUILD_CACHE") !in setOf("gcp", "true")) return

    val keys = File("${System.getenv("GRADLE_USER_HOME")}/develocity/keys.properties")

    // User already has the keys
    if (keys.exists()) return

    keys.parentFile.mkdirs()

    val keysProvider = providers.of(DevelocityKeysValueSource::class.java) {}
    keys.writeText(keysProvider.get())
}

/**
 * Using a ValueSource to fetch Develocity keys because the SecretManagerServiceClient on Macs use
 * external processes (such as codesign and install_name_tool) and that is not allowed when
 * configuration cache is enabled without wrapping those calls in a ValueSource.
 */
internal abstract class DevelocityKeysValueSource :
    ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String? {
        var value: String? = null
        try {
            SecretManagerServiceClient.create().use { manager ->
                val secretVersionName =
                    SecretVersionName.of("androidx-ge", "develocity-token", "latest")
                val response = manager.accessSecretVersion(secretVersionName)
                value = response.payload.data.toStringUtf8()
            }
        } catch (e: Exception) {
            println("Failed to fetch develocity keys")
            e.printStackTrace()
        }
        return value
    }
}
