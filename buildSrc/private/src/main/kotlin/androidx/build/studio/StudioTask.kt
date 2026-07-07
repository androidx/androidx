/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.studio

import androidx.build.BuildEnvironment
import androidx.build.ProjectLayoutType
import androidx.build.getSdkPath
import androidx.build.getVersionByName
import androidx.build.ide.IdePlugin
import androidx.build.ide.ManagedIdeTask
import androidx.build.ide.configureIntellijLikeIde
import androidx.build.ide.installIntellijPlugins
import androidx.build.ide.writeAndroidSdkPath
import java.io.File
import org.gradle.api.Project

private fun Project.configureCommonStudioTask(task: ManagedIdeTask) {
    task.ideName.convention("Studio")

    val studioNameVersion = getVersionByName("androidStudioName")
    val ext = if (task.osName == "linux") "tar.gz" else "dmg"
    task.ideArchiveName.convention("android-studio-$studioNameVersion-${task.osName}.$ext")

    val studioVersion = getVersionByName("androidStudioIj")
    val filename = task.ideArchiveName.get()
    val downloadUrl =
        if (task.osName == "mac_arm" || task.osName == "mac") {
            "https://edgedl.me.gvt1.com/android/studio/install/$studioVersion/$filename"
        } else {
            "https://edgedl.me.gvt1.com/android/studio/ide-zips/$studioVersion/$filename"
        }
    task.archiveUrl.convention(downloadUrl)

    task.licenseAgreementPath.convention(
        if (task.osName == "linux") "LICENSE.txt" else "Contents/Resources/LICENSE.txt"
    )

    task.ideBinaryRelativePath.convention(
        if (task.osName == "linux") "bin/studio" else "Contents/MacOS/studio"
    )

    val studioKtfmtPluginVersion = getVersionByName("ktfmtIdeaPlugin")
    val ktfmtPlugin =
        IdePlugin(
            downloadUrl =
                "https://downloads.marketplace.jetbrains.com/files/14912/923152/ktfmt_idea_plugin-$studioKtfmtPluginVersion.zip",
            checksum = "3280c1d7b6311f697f768ca80bd1c241ce0570fa76d43cd50055fee0808ac8fe",
            zipName = "ktfmt-$studioKtfmtPluginVersion.zip",
            targetDirectoryName = "ktfmt_idea_plugin",
        )

    val configBaseDir =
        layout.dir(
            providers.environmentVariable("HOME").map { File(it, ".AndroidStudioAndroidX/config") }
        )
    val sdkPath = getSdkPath()

    task.provisionAction.set {
        val configBaseDirFile = configBaseDir.get().asFile
        it.writeAndroidSdkPath(configBaseDirFile, sdkPath)
        it.installIntellijPlugins(configBaseDirFile, listOf(ktfmtPlugin))
        BuildEnvironment.setupSymlinksIfNeeded(sdkPath)
    }
}

fun Project.configureRootStudioTask(task: ManagedIdeTask) {
    configureCommonStudioTask(task)
    val vmOptionsFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/studio/studio.vmoptions"))
        }
    val ideaPropertiesFile =
        objects.fileProperty().apply {
            set(layout.projectDirectory.file("development/studio/idea.properties"))
        }

    task.configureIntellijLikeIde(
        envPrefix = "STUDIO",
        ideaPropertiesFile = ideaPropertiesFile,
        vmOptionsFile = vmOptionsFile,
    )
}

fun Project.configurePlaygroundStudioTask(task: ManagedIdeTask) {
    configureCommonStudioTask(task)
    val supportRootFolder = rootProject.extensions.extraProperties.get("supportRootFolder") as File

    task.requiresProjectList.convention(false)
    task.installParentDir.convention(layout.projectDirectory.dir(supportRootFolder.absolutePath))
    task.additionalEnvironmentProperties.put("ALLOW_PUBLIC_REPOS", "true")

    val vmOptionsFile =
        objects
            .fileProperty()
            .fileValue(supportRootFolder.resolve("playground-common/studio.vmoptions"))
    val ideaPropertiesFile =
        objects
            .fileProperty()
            .fileValue(supportRootFolder.resolve("playground-common/idea.properties"))

    task.configureIntellijLikeIde(
        envPrefix = "STUDIO",
        ideaPropertiesFile = ideaPropertiesFile,
        vmOptionsFile = vmOptionsFile,
    )
}

fun Project.registerStudioTask() {
    tasks.register("studio", ManagedIdeTask::class.java) { task ->
        when (ProjectLayoutType.from(this)) {
            ProjectLayoutType.ANDROIDX -> configureRootStudioTask(task)
            ProjectLayoutType.PLAYGROUND -> configurePlaygroundStudioTask(task)
        }
    }
}
