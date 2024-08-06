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

package androidx.build.clang

import androidx.build.KonanPrebuiltsSetup
import androidx.testutils.gradle.ProjectSetupRule
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * Base class for Clang tests that sets up necessary project properties to initialize
 * [KonanBuildService] in tests.
 */
abstract class BaseClangTest {
    @get:Rule val projectSetup = ProjectSetupRule()

    @get:Rule val tmpFolder = TemporaryFolder()

    protected lateinit var project: Project
    protected lateinit var clangExtension: AndroidXClang

    @Before
    fun init() {
        project = ProjectBuilder.builder().withProjectDir(projectSetup.rootDir).build()
        val extension = project.rootProject.property("ext") as ExtraPropertiesExtension
        // build service needs prebuilts location to "download" clang and targets.
        projectSetup.props.prebuiltsPath?.let { extension.set("prebuiltsRoot", it) }
        // ensure that kotlin doesn't try to download prebuilts
        extension.set("kotlin.native.distribution.downloadFromMaven", "false")
        project.pluginManager.apply(KotlinMultiplatformPluginWrapper::class.java)
        clangExtension = AndroidXClang(project)
        KonanPrebuiltsSetup.configureKonanDirectory(project)
    }
}
