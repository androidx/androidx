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

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectConfigValidatorsTest {

    @Test
    fun tesValidateProjectMavenGroup() {
        val project = ProjectBuilder.builder().withName("collection").build()
        project.validateProjectMavenGroup("collection")
    }

    @Test(expected = GradleException::class)
    fun tesValidateProjectMavenGroup_invalid() {
        // Group name cannot contain a hyphen.
        val project = ProjectBuilder.builder().withName("floatingpoint").build()
        project.validateProjectMavenGroup("collection-floatingpoint")
    }

    @Test
    fun testValidateProjectMavenName() {
        val project1 = ProjectBuilder.builder().withName("collection").build()
        project1.validateProjectMavenName("collection", "collection")

        val project2 = ProjectBuilder.builder().withName("collection-floatingpoint").build()
        project2.validateProjectMavenName("collection-floatingpoint", "collection")

        // Not ideal, but we allow "-s" it because of existing projects.
        val project3 = ProjectBuilder.builder().withName("collection").build()
        project3.validateProjectMavenName("collections", "collection")
    }

    @Test(expected = GradleException::class)
    fun testValidateProjectMavenName_invalid1() {
        // Maven name must match project name, modulo some exceptions.
        val project = ProjectBuilder.builder().withName("collection").build()
        project.validateProjectMavenName("collection-floatingpoint", "collection")
    }

    @Test(expected = GradleException::class)
    fun testValidateProjectMavenName_invalid2() {
        // Maven name must match project name, modulo some exceptions.
        val project = ProjectBuilder.builder().withName("collection").build()
        project.validateProjectMavenName("floatingpoint", "collection")
    }
}
