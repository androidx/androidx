/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("unused")

package org.jetbrains.androidx.build

import androidx.build.AndroidXExtension
import androidx.build.ProjectLayoutType.Companion.isJetBrainsFork
import androidx.build.Publish
import androidx.build.RunApiTasks
import androidx.build.SoftwareType.ConfigurableSoftwareType
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType

class JetBrainsAndroidXRootImplPlugin @Inject constructor(
    val componentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        project.allprojects { subproject ->
            // Apply capability rule to resolve conflicts between org.jetbrains.androidx.* and androidx.*
            subproject.configureJetBrainsCapabilityResolution()

            subproject.tasks.configureEach {
                if (it.name == "kotlinStoreYarnLock") it.enabled = false
                if (it.name == "kotlinWasmStoreYarnLock") it.enabled = false
            }

            // Never cache test results
            subproject.tasks.withType<AbstractTestTask>().configureEach {
                it.outputs.upToDateWhen { false }
            }
        }

        project.rootProject.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.rootProject.extensions.configure(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension::class.java) {
                // Manually fixing the version. It's a transitive dependency of karma (web test runner).
                // It got updated automatically to 4.8.2, and k/js tests started to fail:
                // Error [ERR_SERVER_NOT_RUNNING]: Server is not running.
                //   at Server.close (node:net:2261:12)
                //    at Object.onceWrapper (node:events:634:28)
                //    at Server.emit (node:events:532:35)
                //    at emitCloseNT (node:net:2321:8)
                //    at process.processTicksAndRejections (node:internal/process/task_queues:81:21)
                it.resolution("socket.io", "4.8.1")
                // TODO: https://youtrack.jetbrains.com/issue/CMP-9479 - Consider using the newer version, since it has this fix - https://github.com/socketio/socket.io/pull/5344
                // Then remove the workarounds (delays) in our karma configs. Search in the config.js files for 3413540
            }
        }
    }
}
