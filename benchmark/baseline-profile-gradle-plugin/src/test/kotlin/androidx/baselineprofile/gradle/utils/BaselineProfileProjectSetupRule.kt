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

package androidx.baselineprofile.gradle.utils

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement

class BaselineProfileProjectSetupRule : ExternalResource() {

    val rootFolder = TemporaryFolder().also { it.create() }

    // Project setup rules
    private val appTargetSetupRule by lazy { ProjectSetupRule(rootFolder.root) }
    private val consumerSetupRule by lazy { ProjectSetupRule(rootFolder.root) }
    private val producerSetupRule by lazy { ProjectSetupRule(rootFolder.root) }

    val appTarget by lazy { Module(appTargetSetupRule, rootFolder) }
    val consumer by lazy { Module(consumerSetupRule, rootFolder) }
    val producer by lazy { Module(producerSetupRule, rootFolder) }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(appTargetSetupRule)
            .around(producerSetupRule)
            .around(consumerSetupRule)
            .around { b, _ -> applyInternal(b) }
            .apply(base, description)
    }

    private fun applyInternal(base: Statement) = object : Statement() {
        override fun evaluate() {

            // Creates the main settings.gradle
            rootFolder.newFile("settings.gradle").writeText(
                """
                include '${appTarget.name}'
                include '${producer.name}'
                include '${consumer.name}'
            """.trimIndent()
            )

            // Copies test project data
            mapOf(
                "app-target" to appTargetSetupRule,
                "consumer" to consumerSetupRule,
                "producer" to producerSetupRule
            ).forEach { (folder, project) ->
                File("src/test/test-data", folder)
                    .apply { deleteOnExit() }
                    .copyRecursively(project.rootDir)
            }

            base.evaluate()
        }
    }
}

class Module internal constructor(
    private val rule: ProjectSetupRule,
    rootFolder: TemporaryFolder
) {

    val rootDir = rule.rootDir
    val name: String = rule.rootDir.relativeTo(rootFolder.root).name

    val gradleRunner: GradleRunner by lazy {
        GradleRunner
            .create()
            .withProjectDir(rule.rootDir)
            .withPluginClasspath()
    }

    fun setBuildGradle(buildGradleContent: String) =
        rule.writeDefaultBuildGradle(
            prefix = buildGradleContent,
            suffix = """
                $GRADLE_CODE_PRINT_TASK
            """.trimIndent()
        )
}
