/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

abstract class SingletonTestService : BuildService<BuildServiceParameters.None>

interface InjectedExecOps {
    @Inject fun getExecOps(): ExecOperations
}

object IosMetalHelper {
    @JvmStatic
    fun enableIosMetalSupport(
        project: Project,
        taskProvider: TaskProvider<KotlinNativeSimulatorTest>,
    ) {
        val service =
            project.gradle.sharedServices.registerIfAbsent(
                "singletonTestService",
                SingletonTestService::class.java,
            ) { spec ->
                spec.maxParallelUsages.set(1)
            }
        taskProvider.configure { task ->
            task.usesService(service)
            // This also needs to avoid standalone mode to support GPU access for rendering
            // brush textures from UIImage wrapping CIImage because CIContext.createCGImage
            // requires GPU by default. It does have a software rendering fallback option,
            // kCIContextUseSoftwareRenderer, but that is documented as, "This option has no
            // effect if the platform does not support OpenCL," which is deprecated
            // starting with iOS 12 and not available in iOS simulator.
            // Metal renderer does not work on the simulator if it's started with
            // --standalone, which is the default. If that is disabled, there needs to
            // be some setup/teardown to boot and shut down the simulator. That's not
            // currently handled by the KMP Gradle Plugin, see
            // https://youtrack.jetbrains.com/issue/KMT-2955.
            task.standalone.set(false)
            val injected = project.objects.newInstance(InjectedExecOps::class.java)
            task.doFirst {
                injected.getExecOps().exec {
                    it.commandLine("xcrun", "simctl", "boot", task.device.get())
                    it.isIgnoreExitValue = true
                }
            }
            task.doLast {
                injected.getExecOps().exec {
                    it.commandLine("xcrun", "simctl", "shutdown", task.device.get())
                    it.isIgnoreExitValue = true
                }
            }
        }
    }
}
