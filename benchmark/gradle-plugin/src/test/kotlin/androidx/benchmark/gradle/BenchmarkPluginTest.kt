/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.gradle

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var localPropFile: File
    private lateinit var project: Project

    @Before
    fun setUp() {
        testProjectDir.root.mkdirs()

        localPropFile = File(testProjectDir.root, "local.properties")
        localPropFile.createNewFile()
        localPropFile.writeText("sdk.dir=/usr/test/android/home")

        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir.root)
            .build()
    }

    @Test
    fun applyPluginAppProject() {
        project.apply { it.plugin("com.android.application") }
        project.apply { it.plugin("androidx.benchmark") }

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test
    fun applyPluginAndroidLibProject() {
        project.apply { it.plugin("com.android.library") }
        project.apply { it.plugin("androidx.benchmark") }

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test(expected = PluginApplicationException::class)
    fun applyPluginNonAndroidProject() {
        project.apply { it.plugin("java-library") }
        project.apply { it.plugin("androidx.benchmark") }

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }
}
