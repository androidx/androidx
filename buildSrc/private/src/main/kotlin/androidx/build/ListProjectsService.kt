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

package androidx.build

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** Lists projects as specified by settings.gradle */
abstract class ListProjectsService : BuildService<ListProjectsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        var settingsFile: Provider<String>
    }

    // Lists all project paths mentioned in frameworks/support/settings.gradle
    // Note that this might be more than the full list of projects configured in this build:
    // a) Configuration-on-demand can disable projects mentioned in settings.gradle
    // B) Playground builds use their own settings.gradle files
    val allPossibleProjects: List<IncludedProject> by lazy {
        SettingsParser.findProjects(parameters.settingsFile.get())
    }

    companion object {
        internal fun registerOrGet(project: Project): Provider<ListProjectsService> {
            // service that can compute full list of projects in settings.gradle
            val settings = project.lazyReadFile("settings.gradle")
            return project.gradle.sharedServices.registerIfAbsent(
                "listProjectsService",
                ListProjectsService::class.java
            ) { spec ->
                spec.parameters.settingsFile = settings
            }
        }
    }
}
