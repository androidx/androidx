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

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache

class GradleEnterpriseConventionsPlugin implements Plugin<Settings> {
    void apply(Settings settings) {
        settings.apply(plugin: "com.gradle.enterprise")
        settings.apply(plugin: "com.gradle.common-custom-user-data-gradle-plugin")

        settings.gradleEnterprise {
            server = "https://ge.androidx.dev"

            buildScan {
                publishAlways()
                publishIfAuthenticated()

                capture {
                    taskInputFiles = true
                }
                obfuscation {
                    hostname { host -> "unset" }
                    ipAddresses { addresses -> addresses.collect { address -> "0.0.0.0"} }
                }
            }
        }

        settings.buildCache {
            remote(HttpBuildCache) {
                url = "https://ge.androidx.dev/cache/"
                var buildCachePassword = System.getenv("GRADLE_BUILD_CACHE_PASSWORD")
                if (buildCachePassword != null) {
                    push = true
                    credentials {
                        username = "ci"
                        password = buildCachePassword
                    }
                } else {
                    push = false
                }
            }
        }
    }
}
