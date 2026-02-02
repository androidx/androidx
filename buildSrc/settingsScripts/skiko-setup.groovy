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

import org.gradle.api.GradleException
import org.gradle.api.initialization.Settings

class SkikoSetup {
    /**
     * Declares the skiko entry in the version catalog of the given settings instance.
     *
     * @param settings The settings instance for the current root project
     */
    static void defineSkikoInVersionCatalog(Settings settings) {
        settings.dependencyResolutionManagement {
            versionCatalogs {
                libs {
                    def skikoOverride = System.getenv("SKIKO_VERSION")
                    if (skikoOverride != null) {
                        logger.warn("Using custom version ${skikoOverride} of SKIKO due to " +
                                "SKIKO_VERSION being set.")
                        version('skiko', skikoOverride)
                    }
                    String os = System.getProperty("os.name").toLowerCase(Locale.US)
                    String currentOsArtifact
                    if (os.contains("mac os x") || os.contains("darwin") || os.contains("osx")) {
                        def arch = System.getProperty("os.arch")
                        if (arch == "aarch64") {
                            currentOsArtifact = "skiko-awt-runtime-macos-arm64"
                        } else {
                            currentOsArtifact = "skiko-awt-runtime-macos-x64"
                        }
                    } else if (os.startsWith("win")) {
                        currentOsArtifact = "skiko-awt-runtime-windows-x64"
                    } else if (os.startsWith("linux")) {
                        def arch = System.getProperty("os.arch")
                        if (arch == "aarch64") {
                            currentOsArtifact = "skiko-awt-runtime-linux-arm64"
                        } else {
                            currentOsArtifact = "skiko-awt-runtime-linux-x64"
                        }
                    } else {
                        throw new GradleException("Unsupported operating system $os")
                    }
                    library("skikoCurrentOs", "org.jetbrains.skiko",
                            currentOsArtifact).versionRef("skiko")
                }
            }
        }
    }
}

ext.skikoSetup = new SkikoSetup()