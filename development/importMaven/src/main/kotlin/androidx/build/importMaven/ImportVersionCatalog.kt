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

package androidx.build.importMaven

import okio.Path
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.catalog.parser.TomlCatalogFileParser
import org.gradle.api.plugins.catalog.VersionCatalogPlugin.GRADLE_PLATFORM_DEPENDENCIES
import org.gradle.api.plugins.catalog.internal.DependenciesAwareVersionCatalogBuilder
import org.gradle.internal.impldep.com.google.common.collect.Interners

/**
 * Loads all versions from a version catalog file.
 * see [ImportToml].
 */
object ImportVersionCatalog {
    /**
     * Loads a gradle version file and returns all artifacts declared in it.
     */
    fun load(file: Path): List<String> {
        val project = ProjectService.createProject()
        val configurations = project.configurations.create(
            GRADLE_PLATFORM_DEPENDENCIES
        ) { cnf: Configuration ->
            cnf.isVisible = false
            cnf.isCanBeConsumed = false
            cnf.isCanBeResolved = false
        }
        val catalogBuilder = DependenciesAwareVersionCatalogBuilder(
            "loader",
            Interners.newStrongInterner(),
            Interners.newStrongInterner(),
            project.objects,
            { error("Not supported") },
            configurations
        )
        TomlCatalogFileParser.parse(
            file.toNioPath(),
            catalogBuilder
        )
        val built = catalogBuilder.build()
        return built.libraryAliases.map { alias ->
            val dep = built.getDependencyData(alias)
            "${dep.group}:${dep.name}:${dep.version.requiredVersion}"
        }
    }
}
