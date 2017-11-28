package android.support.tools.jetifier.plugin.gradle

import android.support.tools.jetifier.core.config.ConfigParser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task that processes whole configurations. This is the recommended way if processing a set of
 * dependencies where it is unknown which exactly need to be rewritten.
 *
 * This will create a new detached configuration and resolve all the dependencies = obtaining
 * all the files. Jetifier is then run with all the files and only the files that were rewritten
 * are added to the given configuration and the original dependencies that didn't have to be
 * changed are kept.
 *
 * Advantage is that all the dependencies that didn't have to be changed are kept intact so
 * their artifactsIds and groupIds are kept (instead of replacing them with files) which allows
 * other steps in the build process to use the artifacts information to generate pom files
 * and other stuff.
 *
 * This will NOT resolve the given configurations as the dependencies are resolved in a detached
 * configuration. If you give it a configuration that was already resolved the process will
 * end up with exception saying that resolved configuration cannot be changed. This is expected
 * as Jetifier cannot add new files to an already resolved configuration.
 *
 * Example usage in Gradle:
 * jetifier.addConfigurationToProcess(configurations.implementation)
 * afterEvaluate {
 *   tasks.preBuild.dependsOn tasks.jetifyGlobal
 * }
 *
 * TODO: Add caching for this task
 */
open class JetifyGlobalTask : DefaultTask() {

    companion object {
        const val TASK_NAME = "jetifyGlobal"
        const val GROUP_ID = "Pre-build"
        // TODO: Get back to this once the name of the library is decided.
        const val DESCRIPTION = "Rewrites input libraries to run with jetpack"

        const val OUTPUT_DIR_APPENDIX = "jetifier"

        fun resolveTask(project: Project) : JetifyGlobalTask {
            val task = project.tasks.findByName(TASK_NAME) as? JetifyGlobalTask
            if (task != null) {
                return task
            }
            return project.tasks.create(TASK_NAME, JetifyGlobalTask::class.java)
        }
    }

    private var configurationsToProcess = mutableListOf<Configuration>()

    private val outputDir = File(project.buildDir, OUTPUT_DIR_APPENDIX)


    override fun getGroup() = GROUP_ID

    override fun getDescription() = DESCRIPTION

    /**
     * Add a whole configuration to be processed by Jetifier.
     *
     * See [JetifierExtension] for details on how to use this.
     */
    fun addConfigurationToProcess(config: Configuration) {
        configurationsToProcess.add(config)
    }

    @TaskAction
    @Throws(Exception::class)
    fun run() {
        val config = ConfigParser.loadConfigOrFail(TasksCommon.configFilePath)

        val dependenciesMap = mutableMapOf<File, MutableSet<Dependency>>()
        // Build a map where for each file we have a set of dependencies that pulled that file in.
        configurationsToProcess.forEach { conf ->
            for (dep in conf.dependencies) {
                if (dep is ProjectDependency) {
                    project.logger.log(LogLevel.DEBUG, "Ignoring project dependency {}", dep.name)
                    continue
                }

                val fileDep = dep as? FileCollectionDependency
                if (fileDep != null) {
                    fileDep.files.forEach {
                        dependenciesMap
                            .getOrPut(it, { mutableSetOf<Dependency>() } )
                            .add(fileDep)
                    }
                } else {
                    if (TasksCommon.shouldSkipArtifact(dep.name, dep.group, config)) {
                        project.logger.log(
                            LogLevel.DEBUG, "Skipping rewriting of support library {}:{}:{}",
                            dep.group, dep.name, dep.version)
                        continue
                    }

                    val detached = project.configurations.detachedConfiguration()
                    detached.dependencies.add(dep)
                    detached.resolvedConfiguration.resolvedArtifacts.forEach {
                        dependenciesMap
                            .getOrPut(it.file, { mutableSetOf<Dependency>() } )
                            .add(dep)
                    }
                }
            }
        }

        // Process the files using Jetifier
        val result = TasksCommon.processFiles(config, dependenciesMap.keys, project.logger, outputDir)

        // Apply changes
        configurationsToProcess.forEach { conf ->
            // Apply that on our set
            result.filesToRemove.forEach { file ->
                dependenciesMap[file]!!.forEach {
                    conf.dependencies.remove(it)
                }
            }

            result.filesToAdd.forEach {
                project.dependencies.add(conf.name, project.files(it))
            }
        }
    }

}