/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room3.gradle.util

import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

internal fun String.capitalize(): String =
    this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }

internal val kspTwoTaskClass =
    try {
        Class.forName("com.google.devtools.ksp.gradle.KspAATask")
    } catch (ex: ClassNotFoundException) {
        null
    }

internal fun Task.isKspTask() = kspTwoTaskClass?.isAssignableFrom(this::class.java) == true

@OptIn(ExperimentalContracts::class)
internal fun Project.check(value: Boolean, isFatal: Boolean = false, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (isGradleSyncRunning() && !isFatal) return
    if (!value) {
        throw GradleException(lazyMessage())
    }
}

private fun Project.isGradleSyncRunning() =
    gradleSyncProps.any { property ->
        providers.gradleProperty(property).map { it.toBoolean() }.orElse(false).get()
    }

private val gradleSyncProps by lazy {
    listOf(
        "android.injected.build.model.v2",
        "android.injected.build.model.only",
        "android.injected.build.model.only.advanced",
    )
}
