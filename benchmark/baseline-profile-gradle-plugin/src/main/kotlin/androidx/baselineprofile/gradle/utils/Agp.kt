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
import com.android.build.api.dsl.TestedExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

private val gradleSyncProps by lazy {
    listOf(
        "android.injected.build.model.v2",
        "android.injected.build.model.only",
        "android.injected.build.model.only.advanced",
    )
}

internal fun Project.isGradleSyncRunning() =
    gradleSyncProps.any { it in properties && properties[it].toString().toBoolean() }

internal fun Project.agpVersion(): AndroidPluginVersion {
    return project
        .extensions
        .findByType(AndroidComponentsExtension::class.java)
        ?.pluginVersion
        ?: throw GradleException(
            // This can happen only if the plugin is not applied to an android module.
            """
                The module $name does not have a registered `AndroidComponentsExtension`. This can
                only happen if this is not an Android module. Please review your build.gradle to
                ensure this plugin is applied to the correct module.
                """.trimIndent()
        )
}

internal fun Project.agpVersionString(): String {
    val agpVersion = agpVersion()
    val preview = if (!agpVersion.previewType.isNullOrBlank()) {
        "-${agpVersion.previewType}${agpVersion.preview}"
    } else {
        ""
    }
    return "${agpVersion.major}.${agpVersion.minor}.${agpVersion.micro}$preview"
}

internal fun Project.checkAgpVersion(
    min: AndroidPluginVersion = MIN_AGP_VERSION_REQUIRED,
    max: AndroidPluginVersion = MAX_AGP_VERSION_REQUIRED,
) {
    val agpVersion = agpVersion()
    if (agpVersion < min || agpVersion > max) {
        throw GradleException(
            """
            This version of the Baseline Profile Gradle Plugin only works with Android Gradle plugin
            between versions $MIN_AGP_VERSION_REQUIRED and $MAX_AGP_VERSION_REQUIRED. Current version
            is $agpVersion."
            """.trimIndent()
        )
    }
}

internal fun Project.afterVariants(block: () -> (Unit)) {
    val extensionVariants =
        when (val tested = extensions.getByType(TestedExtension::class.java)) {
            is AppExtension -> tested.applicationVariants
            is LibraryExtension -> tested.libraryVariants
            else -> {
                if (isGradleSyncRunning()) {
                    return
                }
                throw GradleException(
                    """
                    Unrecognized extension: $tested not of type AppExtension or LibraryExtension.
                    """.trimIndent()
                )
            }
        }

    var applied = false
    extensionVariants.all {
        if (applied) return@all
        applied = true
        block()
    }
}
