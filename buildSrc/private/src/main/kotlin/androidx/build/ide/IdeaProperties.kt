/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.ide

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.WriteProperties

/** Settings in the idea.properties file that the studio and aswb tasks pass to the IDE. */
private val IDEA_PROPERTIES =
    mapOf(
        // Largest file size, in kilobytes, that gets code assistance.
        "idea.max.intellisense.filesize" to "2500",
        // Largest file size, in kilobytes, that the IDE can open.
        "idea.max.content.load.filesize" to "20000",
        // Size of the console buffer, in kilobytes.
        "idea.cycle.buffer.size" to "1024",
        "idea.no.launcher" to "false",
        "idea.dynamic.classpath" to "false",
        "idea.popup.weight" to "heavy",
        "sun.java2d.d3d" to "false",
        "swing.bufferPerWindow" to "true",
        "sun.java2d.pmoffscreen" to "false",
        "sun.java2d.uiScale.enabled" to "true",
        "javax.swing.rebaseCssSizeMap" to "true",
        "idea.xdebug.key" to "-Xdebug",
        "idea.fatal.error.notification" to "enabled",
        // Show the internal actions menu.
        "idea.is.internal" to "true",
        // Turn off automatic update checks.
        "ide.no.platform.update" to "true",
        // Fix for a slow debugger, b/388473186.
        "rundebug.compose.class.name.calculator.canonical.file.cache" to "true",
        // Render Compose @Preview.
        "compose.project.uses.compose.override" to "true",
        // Mark this IDE as used for androidx development.
        "studio.experiments.override" to "androidx_internal_user",
    )

/**
 * Adds a task that writes the idea.properties file for [ideName], e.g. `Studio`. The IDE keeps its
 * config and system files in [ideDirName], in your home dir.
 */
fun Project.registerWriteIdeaPropertiesTask(
    ideName: String,
    ideDirName: Provider<String>,
): TaskProvider<WriteProperties> =
    tasks.register("write${ideName}IdeaProperties", WriteProperties::class.java) { task ->
        task.destinationFile.set(
            layout.buildDirectory.file("${ideName.lowercase()}/idea.properties")
        )
        task.property("idea.config.path", ideDirName.map { "\${user.home}/$it/config" })
        task.property("idea.system.path", ideDirName.map { "\${user.home}/$it/system" })
        task.property("idea.plugins.path", ideDirName.map { "\${user.home}/$it/config/plugins" })
        task.property("idea.log.path", ideDirName.map { "\${user.home}/$it/system/log" })
        task.properties(IDEA_PROPERTIES)
    }
