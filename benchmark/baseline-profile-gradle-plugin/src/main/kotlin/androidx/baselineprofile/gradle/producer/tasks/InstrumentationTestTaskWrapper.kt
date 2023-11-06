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

package androidx.baselineprofile.gradle.producer.tasks

import androidx.baselineprofile.gradle.utils.namedOrNull
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Wrapper around the instrumentation test task. This wrapper tries to retrieve the instrumentation
 * test task named `<device><variantName>AndroidTest`, if existing. For example:
 * `connectedFreeReleaseAndroidTest`.It supports both `managed` and `connected` devices.
 */
internal class InstrumentationTestTaskWrapper(private val testTask: TaskProvider<Task>) {

    companion object {

        private const val ANDROID_TEST = "androidTest"

        internal fun getByName(
            project: Project,
            device: String,
            variantName: String
        ): InstrumentationTestTaskWrapper? {
            val taskProvider = project
                .tasks
                .namedOrNull<Task>(device, variantName, ANDROID_TEST) ?: return null
            return InstrumentationTestTaskWrapper(taskProvider)
        }
    }

    val resultsDir: Provider<Directory>
        get() = testTask.flatMap { t -> t.property("resultsDir") as DirectoryProperty }

    fun setEnableEmulatorDisplay(value: Boolean) {
        testTask.configure { t ->
            // TODO: this is a bit hack-ish but we can rewrite if we decide to keep the
            //  configuration [BaselineProfileProducerExtension.enableEmulatorDisplay]
            if (t.hasProperty("enableEmulatorDisplay")) {
                t.setProperty("enableEmulatorDisplay", value)
            }
        }
    }

    fun setTaskEnabled(enabled: Boolean) {
        testTask.configure { t -> t.enabled = enabled }
    }
}
