/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.stableaidl

import androidx.build.BUILD_ON_SERVER_TASK
import androidx.build.getSupportRootFolder
import androidx.stableaidl.withStableAidlPlugin
import java.io.File
import org.gradle.api.Project

fun Project.setupWithStableAidlPlugin() = this.withStableAidlPlugin { ext ->
    ext.checkAction.apply {
        before(project.tasks.named("check"))
        before(project.tasks.named(BUILD_ON_SERVER_TASK))
        before(project.tasks.register("checkAidlApi") { task ->
            task.group = "API"
            task.description = "Checks that the API surface generated Stable AIDL sources " +
                "matches the checked in API surface"
        })
    }

    ext.updateAction.apply {
        before(project.tasks.named("updateApi"))
        before(project.tasks.register("updateAidlApi") { task ->
            task.group = "API"
            task.description = "Updates the checked in API surface based on Stable AIDL sources"
        })
    }

    // Don't show tasks added by the Stable AIDL plugin.
    ext.taskGroup = null

    // Use a single top-level directory for shadow framework definitions.
    ext.addStaticImportDirs(
        File(project.getSupportRootFolder(), "buildSrc/stableAidlImports")
    )
}
