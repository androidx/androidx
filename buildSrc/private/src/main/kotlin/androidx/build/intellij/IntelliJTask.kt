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

package androidx.build.intellij

import androidx.build.BuildEnvironment
import androidx.build.ProjectLayoutType
import androidx.build.getSdkPath
import androidx.build.getVersionByName
import androidx.build.ide.ManagedIdeTask
import androidx.build.ide.configureIntellijLikeIde
import androidx.build.ide.writeAndroidSdkPath
import java.io.File
import org.gradle.api.Project

fun Project.configureIntelliJTask(task: ManagedIdeTask) {
    task.ideName.convention("IntelliJ")

    val intelliJVersion = getVersionByName("intelliJVersion")

    val ext = if (task.osName == "linux") "tar.gz" else "dmg"
    task.ideArchiveName.convention("intellij-$intelliJVersion-${task.osName}.$ext")

    val downloadUrl =
        if (task.osName == "mac_arm" || task.osName == "mac") {
            "https://download.jetbrains.com/idea/idea-$intelliJVersion-aarch64.dmg"
        } else {
            "https://download.jetbrains.com/idea/idea-$intelliJVersion.tar.gz"
        }
    task.archiveUrl.convention(downloadUrl)

    task.licenseAgreementPath.convention("https://www.jetbrains.com/legal/docs/toolbox/user/")

    task.ideBinaryRelativePath.convention(
        if (task.osName == "linux") "bin/idea" else "Contents/MacOS/idea"
    )

    val vmOptionsFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/intellij/idea.vmoptions"))
        }
    val ideaPropertiesFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/intellij/idea.properties"))
        }

    task.configureIntellijLikeIde(
        envPrefix = "IDEA",
        ideaPropertiesFile = ideaPropertiesFile,
        vmOptionsFile = vmOptionsFile,
    )

    val configBaseDir =
        layout.dir(
            providers.environmentVariable("HOME").map { File(it, ".IntelliJAndroidX/config") }
        )
    val sdkPath = getSdkPath()

    task.provisionAction.set {
        val configBaseDirFile = configBaseDir.get().asFile
        it.writeAndroidSdkPath(configBaseDirFile, sdkPath)
        BuildEnvironment.setupSymlinksIfNeeded(sdkPath)
    }
}

fun Project.registerIntelliJTask() {
    if (ProjectLayoutType.from(this) == ProjectLayoutType.PLAYGROUND) return
    tasks.register("intellij", ManagedIdeTask::class.java) { task -> configureIntelliJTask(task) }
}
