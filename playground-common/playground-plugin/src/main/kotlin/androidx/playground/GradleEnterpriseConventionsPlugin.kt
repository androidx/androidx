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

package androidx.playground

import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache
import org.gradle.kotlin.dsl.gradleEnterprise
import java.net.InetAddress
import java.net.URI
import java.util.function.Function

class GradleEnterpriseConventionsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.apply(mapOf("plugin" to "com.gradle.enterprise"))
        settings.apply(mapOf("plugin" to "com.gradle.common-custom-user-data-gradle-plugin"))

        // Github Actions always sets a "CI" environment variable
        val isCI = System.getenv("CI") != null

        settings.gradleEnterprise {
            server = "https://ge.androidx.dev"

            buildScan.apply {
                publishAlways()
                (this as BuildScanExtensionWithHiddenFeatures).publishIfAuthenticated()
                isUploadInBackground = !isCI
                capture.isTaskInputFiles = true

                obfuscation.hostname(HostnameHider())
                obfuscation.ipAddresses(IpAddressHider())
            }
        }

        settings.buildCache.local { local ->
            // Aggressively clean up stale build cache entries on CI
            if (isCI) {
                local.removeUnusedEntriesAfterDays = 1
            }
        }

        settings.buildCache.remote(HttpBuildCache::class.java) { remote ->
            remote.url = URI("https://ge.androidx.dev/cache/")
            val buildCachePassword = System.getenv("GRADLE_BUILD_CACHE_PASSWORD")
            if (isCI && !buildCachePassword.isNullOrEmpty()) {
                remote.isPush = true
                remote.credentials { credentials ->
                    credentials.username = "ci"
                    credentials.password = buildCachePassword
                }
            } else {
                remote.isPush = false
            }
        }
    }

    /**
     * This class needs to be a concrete class and not a lambda due to
     * https://github.com/gradle/gradle/issues/19047
     */
    private class HostnameHider : Function<String?, String> {
        override fun apply(originalHostName: String?): String {
            return "unset"
        }
    }

    /**
     * This class needs to be a concrete class and not a lambda due to
     * https://github.com/gradle/gradle/issues/19047
     */
    private class IpAddressHider : Function<List<InetAddress>, List<String>> {
        override fun apply(list: List<InetAddress>): List<String> {
            return listOf("0.0.0.0")
        }
    }
}