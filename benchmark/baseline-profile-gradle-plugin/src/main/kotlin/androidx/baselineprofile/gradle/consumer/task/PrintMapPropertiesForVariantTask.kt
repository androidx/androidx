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

package androidx.baselineprofile.gradle.consumer.task

import androidx.baselineprofile.gradle.utils.maybeRegister
import com.android.build.api.variant.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Only used for testing, this task does not have description or group so that it doesn't show up in
 * the task list. It prints internal properties to facilitate assertions in integration tests.
 */
@DisableCachingByDefault(because = "Not worth caching. Used only for tests.")
abstract class PrintMapPropertiesForVariantTask : DefaultTask() {

    companion object {

        private const val TASK_NAME_PREFIX = "printExperimentalPropertiesForVariant"

        internal fun registerForVariant(
            project: Project,
            variant: Variant,
        ) {
            project.tasks.maybeRegister<PrintMapPropertiesForVariantTask>(
                TASK_NAME_PREFIX,
                variant.name
            ) {
                it.properties.set(variant.experimentalProperties)
            }
        }
    }

    @get:Input abstract val properties: MapProperty<String, Any>

    @TaskAction
    fun exec() {
        properties.get().forEach { logger.warn("${it.key}=${it.value}") }
    }
}
