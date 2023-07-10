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

package androidx.build

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project

// Translate common phrases and marketing names into Maven name component equivalents.
private val mavenNameMap =
    mapOf(
        "android for cars" to "car",
        "android wear" to "wear",
        "internationalization" to "i18n",
        "kotlin extensions" to "ktx",
        "lint checks" to "lint",
        "material components" to "material",
        "material3 components" to "material3",
        "workmanager" to "work",
        "windowmanager" to "window",
    )

// Allow a small set of common Maven name components that don't need to appear in the project name.
private val mavenNameAllowlist =
    setOf(
        "extension",
        "extensions",
        "for",
        "integration",
        "with",
    )

/** Validates the project's Maven name against Jetpack guidelines. */
fun Project.validateProjectMavenName(mavenName: String?, groupId: String) {
    if (mavenName == null) return

    // Tokenize the Maven name into components. This is *very* permissive regarding separators, and
    // we may want to revisit that policy in the future.
    val nameComponents =
        mavenName
            .lowercase()
            .let { name ->
                mavenNameMap.entries.fold(name) { newName, entry ->
                    newName.replace(entry.key, entry.value)
                }
            }
            .split(" ", ",", ":", "-")
            .toMutableList() - mavenNameAllowlist

    // Remaining components *must* appear in the Maven coordinate. Shortening long (>10 char) words
    // to five letters or more is allowed, as is changing the pluralization of words.
    nameComponents
        .find { nameComponent ->
            !name.contains(nameComponent) &&
                !groupId.contains(nameComponent) &&
                !(nameComponent.length > 10 && name.contains(nameComponent.substring(0, 5))) &&
                !(nameComponent.endsWith("s") && name.contains(nameComponent.dropLast(1)))
        }
        ?.let { missingComponent ->
            throw GradleException(
                "Invalid Maven name! Found \"$missingComponent\" in Maven name for $displayName, " +
                    "but not project name.\n\nConsider removing \"$missingComponent\" from" +
                    "\"$mavenName\"."
            )
        }
}

private const val GROUP_PREFIX = "androidx."

/** Validates the project structure against Jetpack guidelines. */
fun Project.validateProjectStructure(groupId: String) {
    if (!project.isValidateProjectStructureEnabled()) {
        return
    }

    val shortGroupId =
        if (groupId.startsWith(GROUP_PREFIX)) {
            groupId.substring(GROUP_PREFIX.length)
        } else {
            groupId
        }

    // Fully-qualified Gradle project name should match the Maven coordinate.
    val expectName = ":${shortGroupId.replace(".",":")}:${project.name}"
    val actualName = project.path
    if (expectName != actualName) {
        throw GradleException(
            "Invalid project structure! Expected $expectName as project name, found $actualName"
        )
    }

    // Project directory should match the Maven coordinate.
    val expectDir = shortGroupId.replace(".", File.separator) + "${File.separator}${project.name}"
    // Canonical projectDir is needed because sometimes, at least in tests, on OSX, supportRoot
    // starts with /var, and projectDir starts with /private/var (which are the same thing)
    val canonicalProjectDir = project.projectDir.canonicalFile
    val actualDir =
        canonicalProjectDir.toRelativeString(project.getSupportRootFolder().canonicalFile)
    if (expectDir != actualDir) {
        throw GradleException(
            "Invalid project structure! Expected $expectDir as project directory, found $actualDir"
        )
    }
}
