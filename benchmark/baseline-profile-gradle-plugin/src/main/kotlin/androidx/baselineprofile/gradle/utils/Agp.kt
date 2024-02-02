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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider

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

    APPLICATION_VARIANT_HAS_UNIT_TEST_BUILDER(
        AndroidPluginVersion(8, 1, 0)
    ),
    TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES(
        AndroidPluginVersion(8, 1, 0).alpha(7)
    ),
    TEST_VARIANT_SUPPORTS_INSTRUMENTATION_RUNNER_ARGUMENTS(
        AndroidPluginVersion(8, 2, 0).alpha(3)
    ),
    LIBRARY_MODULE_SUPPORTS_BASELINE_PROFILE_SOURCE_SETS(
        AndroidPluginVersion(8, 3, 0).alpha(15)
    ),
    TEST_VARIANT_TESTED_APKS(
        AndroidPluginVersion(8, 3, 0).alpha(10)
    )
}

/**
 * This class should be referenced only in AGP 8.2, as the utilized api doesn't exist in
 * previous versions. Keeping it as a separate class instead of accessing the api directly
 * allows previous version of AGP to be compatible with this code base.
 */
internal object InstrumentationTestRunnerArgumentsAgp82 {
    fun set(variant: TestVariant, arguments: List<Pair<String, String>>) {
        arguments.forEach { (k, v) -> set(variant, k, v) }
    }

    fun set(variant: TestVariant, key: String, value: String) {
        variant.instrumentationRunnerArguments.put(key, value)
    }

    fun set(variant: TestVariant, key: String, value: Provider<String>) {
        variant.instrumentationRunnerArguments.put(key, value)
    }
}

/**
 * This class should be referenced only in AGP 8.3, as the utilized api doesn't exist in
 * previous versions. Keeping it as a separate class instead of accessing the api directly
 * allows previous version of AGP to be compatible with this code base.
 */
@Suppress("UnstableApiUsage")
internal object TestedApksAgp83 {
    fun getTargetAppApplicationId(variant: TestVariant): Provider<String> {
        return variant.testedApks.map {
            variant.artifacts.getBuiltArtifactsLoader().load(it)?.applicationId ?: ""
        }
    }
}
