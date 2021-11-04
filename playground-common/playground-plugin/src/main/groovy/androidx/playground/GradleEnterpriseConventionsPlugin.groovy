package androidx.playground

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache

class GradleEnterpriseConventionsPlugin implements Plugin<Settings> {
    void apply(Settings settings) {
        settings.apply(plugin: 'com.gradle.enterprise')
        settings.apply(plugin: 'com.gradle.common-custom-user-data-gradle-plugin')

        settings.gradleEnterprise {
            server = "https://ge.androidx.dev"

            buildScan {
                publishAlways()
                publishIfAuthenticated()

                capture {
                    taskInputFiles = true
                }
                obfuscation {
                    username { name -> "unset" }
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
