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

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.TestVariant
import kotlin.reflect.full.memberFunctions
import org.gradle.api.GradleException
import org.gradle.api.Project

internal fun Project.agpVersion(): AndroidPluginVersion {
    return project
        .extensions
        .findByType(AndroidComponentsExtension::class.java)
        ?.pluginVersion
        ?: throw GradleException(
            // This can happen only if the plugin is not applied to an android module.
            """
        The module $name does not have a registered `AndroidComponentsExtension`. This can only
        happen if this is not an Android module. Please review your build.gradle to ensure this
        plugin is applied to the correct module.
            """.trimIndent()
        )
}

internal enum class AgpFeature(
    internal val version: AndroidPluginVersion
) {

    TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES(
        AndroidPluginVersion(8, 1, 0).alpha(7)
    ),
    TEST_VARIANT_SUPPORTS_INSTRUMENTATION_RUNNER_ARGUMENTS(
        AndroidPluginVersion(8, 2, 0).alpha(3)
    )
}

/**
 * This class should be referenced only in AGP 8.2, as the utilized api doesn't exist in
 * previous versions. Keeping it as a separate class instead of accessing the api directly
 * allows previous version of AGP to be compatible with this code base.
 */
internal object InstrumentationTestRunnerArgumentsAgp82 {
    fun set(variant: TestVariant, arguments: List<Pair<String, String>>) {
        arguments.forEach { (k, v) ->

            // What follows here is some reflection code to achieve the following:
            // `variant.instrumentationRunnerArguments.put(k, v)`
            // Note that once androidx is on Agp 8.2 this reflection code can be substituted with
            // the above line.

            val instrumentationRunnerArgumentsFieldMember = variant::class
                .members
                .firstOrNull { it.name == "instrumentationRunnerArguments" }
                ?: throw IllegalStateException(
                    "`TestVariant#instrumentationRunnerArguments` not found."
                )
            val instrumentationRunnerArgumentsMap = instrumentationRunnerArgumentsFieldMember
                .call(variant)
                ?: throw IllegalStateException(
                    "Failed to acquire `TestVariant#instrumentationRunnerArguments`."
                )
            val putMethod = instrumentationRunnerArgumentsMap::class
                .memberFunctions
                .firstOrNull {
                    it.name == "put" &&
                        it.parameters[1].name == "key" &&
                        it.parameters[2].name == "value"
                }
                ?: throw IllegalStateException(
                    "`instrumentationRunnerArguments` does not have a `put(key, value)` method."
                )
            putMethod.call(instrumentationRunnerArgumentsMap, k, v)
        }
    }
}
