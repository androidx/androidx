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

package org.jetbrains.androidx.build

import androidx.build.AndroidXExtension
import androidx.build.Version
import org.gradle.api.Project

fun Project.changeMavenCoordinatesToJetBrains(androidxExtension: AndroidXExtension) {
    // we are interested in changing coordinates only for what we publish
    val component = JetBrainsPublication.projectPathToComponent[path] ?: return
    val versions = JetBrainsVersionsService.versions(project)

    group = JetBrainsPublication.mavenGroupFor(path, androidxExtension.mavenGroup?.group) ?: group
    version = Version(versions.versionOf(component.library()))
}
