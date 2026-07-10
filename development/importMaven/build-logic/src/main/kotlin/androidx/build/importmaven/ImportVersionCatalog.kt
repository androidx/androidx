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

package androidx.build.importmaven

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/** Loads all versions from a version catalog file. see [ImportToml]. */
object ImportVersionCatalog {
    /** Loads a gradle version file and returns all artifacts declared in it. */
    fun load(project: Project): List<String> {
        val libs =
            project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()
        return libs.libraryAliases.map { alias ->
            val dep = libs.findLibrary(alias).get().get()
            "${dep.group}:${dep.name}:${dep.version}"
        }
    }
}
