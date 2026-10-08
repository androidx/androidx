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

package androidx.build.aswb

import androidx.build.BuildEnvironment
import androidx.build.ProjectLayoutType
import androidx.build.getSdkPath
import androidx.build.getSupportRootFolder
import androidx.build.getVersionByName
import androidx.build.ide.IdePlugin
import androidx.build.ide.ManagedIdeTask
import androidx.build.ide.configureIntellijLikeIde
import androidx.build.ide.installIntellijPlugins
import androidx.build.ide.writeAndroidSdkPath
import java.io.File
import org.gradle.api.Project

fun Project.configureASWBTask(task: ManagedIdeTask) {
    task.ideName.convention("ASwB")

    val aswbVersion = getVersionByName("aswbBuildId")

    val gcsSubDir = if (task.osName == "linux") "glinux" else "mac"
    val packageExtension = if (task.osName == "linux") "deb" else "burrito"
    task.archiveUrl.convention(
        "gs://androidx-aswb-binaries/${gcsSubDir}/android-studio-with-blaze-canary-$aswbVersion.$packageExtension"
    )

    task.licenseAgreementPath.convention(
        if (task.osName == "linux") "LICENSE.txt" else "Contents/Resources/LICENSE.txt"
    )

    task.ideBinaryRelativePath.convention(
        if (task.osName == "linux") "android-studio-with-blaze/ide-update/bin/studio"
        else "Contents/MacOS/studio"
    )

    task.ideArchiveName.convention(
        "android-studio-with-blaze-canary-$aswbVersion.$packageExtension"
    )

    val vmOptionsFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/aswb/studio.vmoptions"))
        }
    val ideaPropertiesFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/aswb/idea.properties"))
        }

    task.configureIntellijLikeIde(
        envPrefix = "STUDIO",
        ideaPropertiesFile = ideaPropertiesFile,
        vmOptionsFile = vmOptionsFile,
    )

    val configBaseDir =
        layout.dir(
            providers.environmentVariable("HOME").map {
                File(it, ".AndroidStudioWithBlazeAndroidX/config")
            }
        )
    val relativeSdkPath = getSdkPath().relativeTo(project.getSupportRootFolder())

    val studioKtfmtPluginVersion = getVersionByName("ktfmtIdeaPlugin")
    val ktfmtPlugin =
        IdePlugin(
            downloadUrl =
                "https://downloads.marketplace.jetbrains.com/files/14912/1084943/ktfmt_idea_plugin-$studioKtfmtPluginVersion.zip",
            checksum = "bc6d1d3efe40ef117bac8ba920d55dc2a1b7e879b86f464d85aeb34fb548eaef",
            zipName = "ktfmt-$studioKtfmtPluginVersion.zip",
            targetDirectoryName = "ktfmt_idea_plugin",
        )

    task.provisionAction.set {
        val configBaseDirFile = configBaseDir.get().asFile
        it.writeAndroidSdkPath(configBaseDirFile, relativeSdkPath)
        it.installIntellijPlugins(configBaseDirFile, listOf(ktfmtPlugin))
        BuildEnvironment.setupSymlinksIfNeeded(relativeSdkPath)
    }
}

fun Project.registerASWBTask() {
    if (ProjectLayoutType.from(this) == ProjectLayoutType.PLAYGROUND) return
    tasks.register("aswb", ManagedIdeTask::class.java) { task -> configureASWBTask(task) }
}
