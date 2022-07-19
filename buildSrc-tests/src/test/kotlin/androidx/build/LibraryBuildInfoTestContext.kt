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

import androidx.build.AndroidXPluginTestContext.Companion.wrap
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.rules.TemporaryFolder

class LibraryBuildInfoTestContext(tmpFolder: TemporaryFolder) {
    val project = createRootProject(tmpFolder)
    val subProject: Project = addSubproject(project, tmpFolder)

    private fun createRootProject(tmpFolder: TemporaryFolder): Project {
        val project = ProjectBuilder.builder().build()!!
        project.setFolders(tmpFolder)
        return project
    }

    private fun addSubproject(project: Project, tmpFolder: TemporaryFolder): Project =
        ProjectBuilder.builder().withName("subproject").build().also {
            project.childProjects["subproject"] = it
            it.setFolders(tmpFolder)
        }

    private fun Project.setFolders(tmpFolder: TemporaryFolder) {
        setSupportRootFolder(tmpFolder.root)

        val extension = rootProject.property("ext") as ExtraPropertiesExtension
        val outDir = tmpFolder.newFolder()
        extension.set("outDir", outDir)
    }

    companion object {
        fun buildInfoTest(action: LibraryBuildInfoTestContext.() -> Unit) =
            TemporaryFolder().wrap { LibraryBuildInfoTestContext(it).action() }
    }
}