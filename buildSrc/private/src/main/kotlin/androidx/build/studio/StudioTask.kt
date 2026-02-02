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

import androidx.build.OperatingSystem
import androidx.build.ProjectLayoutType
import androidx.build.getOperatingSystem
import androidx.build.getSdkPath
import androidx.build.getSupportRootFolder
import androidx.build.getVersionByName
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Base task with common logic for updating and launching studio in both the frameworks/support
 * project and playground projects. Project-specific configuration is provided by
 * [RootStudioTask] and [PlaygroundStudioTask].
 */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        // Nothing in JB fork
    }

    private val platformUtilities by lazy {
        StudioPlatformUtilities.get(projectRoot, studioInstallationDir)
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * If `true`, checks for `ANDROIDX_PROJECTS` environment variable to decide which
     * projects need to be loaded.
     */
    @get:Internal
    protected open val requiresProjectList: Boolean = true

    @get:Internal
    protected val projectRoot: File = project.rootDir

    @get:Internal
    protected open val installParentDir: File = project.rootDir

    private val studioVersion by lazy {
        project.getVersionByName("androidStudio")
    }

    /**
     * Directory name (not path) that Studio will be unzipped into.
     */
    private val studioDirectoryName: String
        get() {
            val osName = StudioPlatformUtilities.osName
            return "android-studio-$studioVersion-$osName"
        }

    /**
     * Filename (not path) of the Studio archive
     */
    private val studioArchiveName: String
        get() = studioDirectoryName + platformUtilities.archiveExtension

    /**
     * The install directory containing Studio
     *
     * Note: Given that the contents of this directory changes a lot, we don't want to annotate this
     * property for task avoidance - it's not stable enough for us to get any value out of this.
     */
    private val studioInstallationDir by lazy {
        File(installParentDir, "studio/$studioDirectoryName")
    }

    /**
     * Absolute path of the Studio archive
     */
    private val studioArchivePath: String by lazy {
        File(studioInstallationDir.parentFile, studioArchiveName).absolutePath
    }

    /**
     * The idea.properties file that we want to tell Studio to use
     */
    @get:Internal
    protected abstract val ideaProperties: File

    /**
     * The studio.vmoptions file that we want to start Studio with
     */
    @get:Internal
    open val vmOptions = File(project.getSupportRootFolder(), "development/studio/studio.vmoptions")

    /**
     * The path to the SDK directory used by Studio.
     */
    @get:Internal
    open val localSdkPath = project.getSdkPath()

    /**
     * List of additional environment variables to pass into the Studio application.
     */
    @get:Internal
    open val additionalEnvironmentProperties: Map<String, String> = emptyMap()

    private val licenseAcceptedFile: File by lazy {
        File("$studioInstallationDir/STUDIOW_LICENSE_ACCEPTED")
    }

    companion object {
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            val studioTask = when (ProjectLayoutType.from(this)) {
                ProjectLayoutType.ANDROIDX -> RootStudioTask::class.java
                ProjectLayoutType.PLAYGROUND -> PlaygroundStudioTask::class.java
            }
            tasks.register(STUDIO_TASK, studioTask)
        }
    }
}

/**
 * Task for launching studio in the frameworks/support project
 */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class RootStudioTask : StudioTask() {
    override val ideaProperties get() = projectRoot.resolve("development/studio/idea.properties")
}

/**
 * Task for launching studio in a playground project
 */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class PlaygroundStudioTask : RootStudioTask() {
    @get:Internal
    val supportRootFolder = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        .let { it.get("supportRootFolder") as File }

    /**
     * Playground projects have only 1 setup so there is no need to specify the project list.
     */
    override val requiresProjectList get() = false
    override val installParentDir get() = supportRootFolder
    override val additionalEnvironmentProperties: Map<String, String>
        get() = mapOf("ALLOW_PUBLIC_REPOS" to "true")
    override val ideaProperties
        get() = supportRootFolder.resolve("playground-common/idea.properties")
    override val vmOptions
        get() = supportRootFolder.resolve("playground-common/studio.vmoptions")
}
