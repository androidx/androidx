/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.plugin.gradle

import com.android.tools.build.jetifier.core.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This serves as the main entry point of this plugin and registers the extension object.
 */
open class JetifierPlugin : Plugin<Project> {

    companion object {
        const val GROOVY_OBJECT_NAME: String = "jetifier"
    }

    override fun apply(project: Project) {
        project.extensions.create(GROOVY_OBJECT_NAME, JetifierExtension::class.java, project)

        project.afterEvaluate({
            val jetifyLibs = it.tasks.findByName(JetifyLibsTask.TASK_NAME)
            val jetifyGlobal = it.tasks.findByName(JetifyGlobalTask.TASK_NAME)

            if (jetifyLibs == null && jetifyGlobal == null) {
                return@afterEvaluate
            }

            if (jetifyLibs != null && jetifyGlobal != null) {
                jetifyGlobal.dependsOn(jetifyLibs)
            }

            val preBuildTask = it.tasks.findByName("preBuild")
            if (preBuildTask == null) {
                Log.e("TAG", "Failed to hook jetifier tasks. PreBuild task was not found.")
                return@afterEvaluate
            }

            if (jetifyGlobal != null) {
                preBuildTask.dependsOn(jetifyGlobal)
            } else {
                preBuildTask.dependsOn(jetifyLibs)
            }
        })
    }
}