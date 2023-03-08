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

package androidx.baselineprofile.gradle.consumer

import androidx.baselineprofile.gradle.attributes.BaselineProfilePluginVersionAttr
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_TARGET_JVM_ENVIRONMENT
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_USAGE_BASELINE_PROFILE
import androidx.baselineprofile.gradle.utils.BUILD_TYPE_BASELINE_PROFILE_PREFIX
import androidx.baselineprofile.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofile.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofile.gradle.utils.TASK_NAME_SUFFIX
import androidx.baselineprofile.gradle.utils.afterVariants
import androidx.baselineprofile.gradle.utils.agpVersion
import androidx.baselineprofile.gradle.utils.agpVersionString
import androidx.baselineprofile.gradle.utils.camelCase
import androidx.baselineprofile.gradle.utils.checkAgpVersion
import androidx.baselineprofile.gradle.utils.isGradleSyncRunning
import androidx.baselineprofile.gradle.utils.maybeRegister
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.attributes.AgpVersionAttr
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

private const val GENERATE_TASK_NAME = "generate"
private const val MERGE_TASK_NAME = "merge"
private const val COPY_TASK_NAME = "copy"

/**
 * This is the consumer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the module that should supply
 * the under test app (app target) and the last one is applied to a test module containing the ui
 * test that generate the baseline profile on the device (producer).
 */
class BaselineProfileConsumerPlugin : Plugin<Project> {

    companion object {
        private const val RELEASE = "release"
        private const val PROPERTY_R8_REWRITE_BASELINE_PROFILE_RULES =
            "android.experimental.art-profile-r8-rewriting"
    }

    override fun apply(project: Project) {
        var foundAppOrLibraryPlugin = false
        project.pluginManager.withPlugin("com.android.application") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project, isApplication = true)
        }
        project.pluginManager.withPlugin("com.android.library") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project, isApplication = false)
        }

        // Only used to verify that the android application plugin has been applied.
        // Note that we don't want to throw any exception if gradle sync is in progress.
        project.afterEvaluate {
            if (!project.isGradleSyncRunning()) {
                if (!foundAppOrLibraryPlugin) {
                    throw IllegalStateException(
                        """
                    The module ${project.name} does not have the `com.android.application` or
                    `com.android.library` plugin applied. The `androidx.baselineprofile.consumer`
                    plugin supports only android application and library modules. Please review
                    your build.gradle to ensure this plugin is applied to the correct module.
                    """.trimIndent()
                    )
                }
                project.logger.debug(
                    """
                    [BaselineProfileConsumerPlugin] afterEvaluate check: app or library plugin
                    was applied""".trimIndent()
                )
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun configureWithAndroidPlugin(project: Project, isApplication: Boolean) {

        // Checks that the required AGP version is applied to this project.
        project.checkAgpVersion()

        val baselineProfileExtension =
            BaselineProfileConsumerExtension.registerExtension(project)

        // Creates the main baseline profile configuration
        val mainBaselineProfileConfiguration = createBaselineProfileConfigurationForVariant(
            project,
            productFlavors = listOf(),
            variantName = "",
            flavorName = "",
            buildTypeName = "",
            mainConfiguration = null,
            hasDirectConfiguration = false
        )

        // Here we select the build types we want to process, i.e. non debuggable build types that
        // have not been created by the app target plugin. Variants are used to create
        // per-variant configurations, tasks and configured for baseline profiles src sets.
        val nonDebuggableBuildTypes = mutableListOf<String>()

        // This extension exists only if the module is an application.
        project
            .extensions
            .findByType(ApplicationAndroidComponentsExtension::class.java)
            ?.finalizeDsl { ext ->
                nonDebuggableBuildTypes.addAll(ext.buildTypes
                    .filter {

                        // We want to enable baseline profile generation only for non-debuggable
                        // build types. Additionally we exclude the ones we may have created in the
                        // app target plugin if this is also applied to this module.
                        !it.isDebuggable && !it.name.startsWith(
                            BUILD_TYPE_BASELINE_PROFILE_PREFIX
                        )
                    }
                    .map { it.name }
                )
            }

        // This extension exists only if the module is a library.
        project
            .extensions
            .findByType(LibraryAndroidComponentsExtension::class.java)
            ?.finalizeDsl { ext ->
                nonDebuggableBuildTypes.addAll(ext.buildTypes
                    .filter {

                        // Note that library build types don't have a `debuggable` flag so we'll
                        // just exclude the one named `debug`. Note that we don't need to filter
                        // for baseline profile build type if this is a library, since the apk
                        // provider cannot be applied.
                        it.name != "debug"
                    }
                    .map { it.name })
            }

        // A list of blocks to execute after agp tasks have been created
        val afterVariantBlocks = mutableListOf<() -> (Unit)>()

        // Iterate baseline profile variants to create per-variant tasks and configurations
        project
            .extensions
            .getByType(AndroidComponentsExtension::class.java)
            .apply {
                onVariants { variant ->

                    if (variant.buildType !in nonDebuggableBuildTypes) return@onVariants

                    // For test only: this registers a print task with the configuration of the
                    // variant.
                    baselineProfileExtension
                        .registerPrintConfigurationTaskForVariant(project, variant)

                    // Sets the r8 rewrite baseline profile for the non debuggable variant.
                    val enableR8Rewrite = baselineProfileExtension
                        .getValueForVariant(variant) { enableR8BaselineProfileRewrite }
                    if (enableR8Rewrite &&
                        project.agpVersion() >= AndroidPluginVersion(8, 0, 0).beta(2)
                    ) {
                        // TODO: Note that currently there needs to be at least a baseline profile,
                        //  even if empty. For this reason we always add a src set that points to
                        //  an empty file. This can removed after b/271158087 is fixed.
                        GenerateDummyBaselineProfileTask.setupForVariant(project, variant)
                        @Suppress("UnstableApiUsage")
                        variant.experimentalProperties.put(
                            PROPERTY_R8_REWRITE_BASELINE_PROFILE_RULES,
                            enableR8Rewrite
                        )
                    }

                    // Check if this variant has any direct dependency
                    val variantDependencies = baselineProfileExtension
                        .getMergedListValuesForVariant(variant) { dependencies }

                    // Creates the configuration to carry the specific variant artifact
                    val baselineProfileConfiguration =
                        createBaselineProfileConfigurationForVariant(
                            project,
                            variantName = variant.name,
                            productFlavors = variant.productFlavors,
                            flavorName = variant.flavorName ?: "",
                            buildTypeName = variant.buildType ?: "",
                            mainConfiguration = mainBaselineProfileConfiguration,
                            hasDirectConfiguration = variantDependencies.any { it.second != null }
                        )

                    // Adds the custom dependencies for baseline profiles. Note that dependencies
                    // for global, build type, flavor and variant specific are all merged.
                    variantDependencies.forEach {
                        val targetProject = it.first
                        val variantName = it.second
                        val targetProjectDependency = if (variantName != null) {
                            val configurationName = camelCase(
                                variantName,
                                CONFIGURATION_NAME_BASELINE_PROFILES
                            )
                            project.dependencies.project(
                                mutableMapOf(
                                    "path" to targetProject.path,
                                    "configuration" to configurationName
                                )
                            )
                        } else {
                            project.dependencyFactory.create(targetProject)
                        }
                        baselineProfileConfiguration.dependencies.add(targetProjectDependency)
                    }

                    // There are 2 different ways in which the output task can merge the baseline
                    // profile rules, according to [BaselineProfileConsumerExtension#mergeIntoMain].
                    // When mergeIntoMain is `true` the first variant will create a task shared across
                    // all the variants to merge, while the next variants will simply add the additional
                    // baseline profile artifacts, modifying the existing task.
                    // When mergeIntoMain is `false` each variants has its own task with a single
                    // artifact per task, specific for that variant.
                    // When mergeIntoMain is not specified, it's by default true for libraries and false
                    // for apps.
                    val mergeIntoMain = baselineProfileExtension
                        .getValueForVariant(variant, default = !isApplication) { mergeIntoMain }

                    // TODO: When `mergeIntoMain` is true it lazily triggers the generation of all
                    //  the variants for all the build types. Due to b/265438201, that fails when
                    //  there are multiple build types. As temporary workaround, when `mergeIntoMain`
                    //  is true, calling a generation task for a specific build type will merge
                    //  profiles for all the variants of that build type and output it in the `main`
                    //  folder.
                    val (mergeAwareVariantName, mergeAwareVariantOutput) = if (mergeIntoMain) {
                        listOf(variant.buildType ?: "", "main")
                    } else {
                        listOf(variant.name, variant.name)
                    }

                    // Creates the task to merge the baseline profile artifacts coming from
                    // different configurations.
                    val mergedTaskOutputDir = project
                        .layout
                        .buildDirectory
                        .dir("$INTERMEDIATES_BASE_FOLDER/$mergeAwareVariantOutput/merged")

                    val mergeTaskProvider = project
                        .tasks
                        .maybeRegister<MergeBaselineProfileTask>(
                            MERGE_TASK_NAME, mergeAwareVariantName, TASK_NAME_SUFFIX,
                        ) { task ->

                            // Sets whether or not baseline profile dependencies have been set.
                            // If they haven't, the task will fail at execution time.
                            task.hasDependencies.set(
                                baselineProfileConfiguration.allDependencies.isNotEmpty()
                            )

                            // Sets the name of this variant to print it in error messages.
                            task.variantName.set(mergeAwareVariantName)

                            // These are all the configurations this task depends on,
                            // in order to consume their artifacts. Note that if this task already
                            // exist (for example if `merge` is `all`) the new artifact will be
                            // added to the existing list.
                            task.baselineProfileFileCollection
                                .from
                                .add(baselineProfileConfiguration)

                            // This is the task output for the generated baseline profile. Output
                            // is always stored in the intermediates
                            task.baselineProfileDir.set(mergedTaskOutputDir)

                            // Sets the package filter rules. Note that variant rules are merged
                            // with global rules here.
                            task.filterRules.addAll(
                                baselineProfileExtension
                                    .getMergedListValuesForVariant(variant) { filters.rules }
                            )
                        }

                    // If `saveInSrc` is true, we create an additional task to copy the output
                    // of the merge task in the src folder.
                    val saveInSrc = baselineProfileExtension
                        .getValueForVariant(variant) { saveInSrc }
                    val lastTaskProvider = if (saveInSrc) {

                        val baselineProfileOutputDir = baselineProfileExtension
                            .getValueForVariant(variant) { baselineProfileOutputDir }
                        val srcOutputDir = project
                            .layout
                            .projectDirectory
                            .dir("src/$mergeAwareVariantOutput/$baselineProfileOutputDir/")

                        // This task copies the baseline profile generated from the merge task.
                        // Note that we're reutilizing the [MergeBaselineProfileTask] because
                        // if the flag `mergeIntoMain` is true tasks will have the same name
                        // and we just want to add more file to copy to the same output. This is
                        // already handled in the MergeBaselineProfileTask.
                        val copyTaskProvider = project
                            .tasks
                            .maybeRegister<MergeBaselineProfileTask>(
                                COPY_TASK_NAME, mergeAwareVariantName, "baselineProfileIntoSrc",
                            ) { task ->
                                task.baselineProfileFileCollection
                                    .from
                                    .add(mergeTaskProvider.flatMap { it.baselineProfileDir })
                                task.baselineProfileDir.set(srcOutputDir)
                            }

                        // Applies the source path for this variant
                        srcOutputDir.asFile.apply {
                            mkdirs()
                            variant
                                .sources
                                .baselineProfiles?.addStaticSourceDirectory(absolutePath)
                        }

                        // If this is an application, we need to ensure that:
                        // If `automaticGenerationDuringBuild` is true, building a release build
                        // should trigger the generation of the profile. This is done through a
                        // dependsOn rule.
                        // If `automaticGenerationDuringBuild` is false and the user calls both
                        // tasks to generate and assemble, assembling the release should wait of the
                        // generation to be completed. This is done through a `mustRunAfter` rule.
                        // Depending on whether the flag `automaticGenerationDuringBuild` is enabled
                        // Note that we cannot use the variant src set api
                        // `addGeneratedSourceDirectory` since that overwrites the outputDir,
                        // that would be re-set in the build dir.
                        // Also this is specific for applications: doing this for a library would
                        // trigger a circular task dependency since the library would require
                        // the profile in order to build the aar for the sample app and generate
                        // the profile.
                        if (isApplication) {
                            afterVariantBlocks.add {
                                project
                                    .tasks
                                    .named(camelCase("merge", variant.name, "artProfile"))
                                    .configure {
                                        // Sets the task dependency according to the configuration
                                        // flag.
                                        val automaticGeneration = baselineProfileExtension
                                            .getValueForVariant(variant) {
                                                automaticGenerationDuringBuild
                                            }
                                        if (automaticGeneration) {
                                            it.dependsOn(copyTaskProvider)
                                        } else {
                                            it.mustRunAfter(copyTaskProvider)
                                        }
                                    }
                            }
                        }

                        // In this case the last task is the copy task.
                        copyTaskProvider
                    } else {

                        val automaticGeneration = baselineProfileExtension
                            .getValueForVariant(variant) { automaticGenerationDuringBuild }
                        if (automaticGeneration) {
                            // If the flag `automaticGenerationDuringBuild` is true, we can set the
                            // merge task to provide generated sources for the variant, using the
                            // src set variant api. This means that we don't need to manually depend
                            // on the merge or prepare art profile task.
                            variant
                                .sources
                                .baselineProfiles?.addGeneratedSourceDirectory(
                                    taskProvider = mergeTaskProvider,
                                    wiredWith = MergeBaselineProfileTask::baselineProfileDir
                                )
                        } else {

                            // This is the case of `saveInSrc` and `automaticGenerationDuringBuild`
                            // both false, that is unsupported. In this case we simply throw an
                            // error.
                            if (!project.isGradleSyncRunning()) {
                                throw GradleException(
                                    """
                                    The current configuration of flags `saveInSrc` and
                                    `automaticGenerationDuringBuild` is not supported. At least
                                    one of these should be set to `true`. Please review your
                                    baseline profile plugin configuration in your build.gradle.
                                """.trimIndent()
                                )
                            }
                        }

                        // In this case the last task is the merge task.
                        mergeTaskProvider
                    }

                    // Here we create the final generate task that triggers the whole generation
                    // for this variant and all the parent tasks. For this one the child task
                    // is either copy or merge, depending on the configuration.
                    val variantGenerateTask = maybeCreateGenerateTask<Task>(
                        project = project,
                        variantName = mergeAwareVariantName,
                        childGenerationTaskProvider = lastTaskProvider
                    )

                    // Create the build type task. For example `generateReleaseBaselineProfile`
                    // The variant name is equal to the build type name if there are no flavors.
                    // Note that if `mergeIntoMain` is `true` the build type task already exists.
                    if (!mergeIntoMain &&
                        !variant.buildType.isNullOrBlank() &&
                        variant.name != variant.buildType
                    ) {
                        maybeCreateGenerateTask<Task>(
                            project = project,
                            variantName = variant.buildType!!,
                            childGenerationTaskProvider = variantGenerateTask
                        )
                    }

                    // TODO: Due to b/265438201 we cannot have a global task
                    //  `generateBaselineProfile` that triggers generation for all the
                    //  variants when there are multiple build types. The temporary workaround
                    //  is to generate baseline profiles only for variants with the `release`
                    //  build type until that bug is fixed, when running the global task
                    //  `generateBaselineProfile`. This can be removed after fix.
                    if (variant.buildType == RELEASE) {
                        maybeCreateGenerateTask<MainGenerateBaselineProfileTask>(
                            project,
                            "",
                            variantGenerateTask
                        )
                    }
                }
            }

        // After variants have been resolved the AGP tasks have been created, so we can set our
        // task dependency if any.
        project.afterVariants {
            afterVariantBlocks.forEach { it() }
        }
    }

    private inline fun <reified T : Task> maybeCreateGenerateTask(
        project: Project,
        variantName: String,
        childGenerationTaskProvider: TaskProvider<*>? = null
    ) = project.tasks.maybeRegister<T>(GENERATE_TASK_NAME, variantName, TASK_NAME_SUFFIX) {
        it.group = "Baseline Profile"
        it.description = "Generates a baseline profile for the specified variants or dimensions."
        if (childGenerationTaskProvider != null) it.dependsOn(childGenerationTaskProvider)
    }

    private fun createBaselineProfileConfigurationForVariant(
        project: Project,
        variantName: String,
        productFlavors: List<Pair<String, String>>,
        flavorName: String,
        buildTypeName: String,
        mainConfiguration: Configuration?,
        hasDirectConfiguration: Boolean
    ): Configuration {

        val buildTypeConfiguration =
            if (buildTypeName.isNotBlank() && buildTypeName != variantName) {
                project
                    .configurations
                    .maybeCreate(camelCase(buildTypeName, CONFIGURATION_NAME_BASELINE_PROFILES))
                    .apply {
                        if (mainConfiguration != null) extendsFrom(mainConfiguration)
                        isCanBeResolved = true
                        isCanBeConsumed = false
                    }
            } else null

        val flavorConfiguration = if (flavorName.isNotBlank() && flavorName != variantName) {
            project
                .configurations
                .maybeCreate(camelCase(flavorName, CONFIGURATION_NAME_BASELINE_PROFILES))
                .apply {
                    if (mainConfiguration != null) extendsFrom(mainConfiguration)
                    isCanBeResolved = true
                    isCanBeConsumed = false
                }
        } else null

        return project
            .configurations
            .maybeCreate(camelCase(variantName, CONFIGURATION_NAME_BASELINE_PROFILES))
            .apply {

                // The variant specific configuration always extends from build type and flavor
                // configurations, when existing.
                setExtendsFrom(
                    listOfNotNull(
                        mainConfiguration,
                        flavorConfiguration,
                        buildTypeConfiguration
                    )
                )

                isCanBeResolved = true
                isCanBeConsumed = false

                // Skip the attributes configuration if there is a direct named configuration
                // matching this one.
                if (hasDirectConfiguration) return@apply

                attributes {

                    // Main specialized attribute
                    it.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.objects.named(
                            Usage::class.java, ATTRIBUTE_USAGE_BASELINE_PROFILE
                        )
                    )

                    // Build type
                    it.attribute(
                        BuildTypeAttr.ATTRIBUTE,
                        project.objects.named(
                            BuildTypeAttr::class.java, buildTypeName
                        )
                    )

                    // Jvm Environment
                    it.attribute(
                        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                        project.objects.named(
                            TargetJvmEnvironment::class.java, ATTRIBUTE_TARGET_JVM_ENVIRONMENT
                        )
                    )

                    // Agp version
                    it.attribute(
                        AgpVersionAttr.ATTRIBUTE,
                        project.objects.named(
                            AgpVersionAttr::class.java, project.agpVersionString()
                        )
                    )

                    // Baseline Profile Plugin Version
                    it.attribute(
                        BaselineProfilePluginVersionAttr.ATTRIBUTE,
                        project.objects.named(
                            BaselineProfilePluginVersionAttr::class.java,
                            ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION
                        )
                    )

                    // Product flavors
                    productFlavors.forEach { (flavorName, flavorValue) ->
                        it.attribute(
                            @Suppress("UnstableApiUsage")
                            ProductFlavorAttr.of(flavorName),
                            project.objects.named(
                                ProductFlavorAttr::class.java, flavorValue
                            )
                        )
                    }
                }
            }
    }

    private fun <T> BaselineProfileConsumerExtension.getMergedListValuesForVariant(
        variant: Variant,
        getter: BaselineProfileVariantConfigurationImpl.() -> (List<T>)
    ): List<T> =
        listOfNotNull("main", variant.flavorName, variant.buildType, variant.name)
            .mapNotNull { variants.findByName(it) }
            .map { getter.invoke(it) }
            .flatten()

    private fun <T> BaselineProfileConsumerExtension.getValueForVariant(
        variant: Variant,
        default: T? = null,
        getter: BaselineProfileVariantConfigurationImpl.() -> (T?)
    ): T {

        // Here we select a setting for the given variant. [BaselineProfileVariantConfiguration]
        // are evaluated in the following order: variant, flavor, build type, `main`.
        // If a property is found it will return it. Note that `main` should have all the defaults
        // set so this method never returns a nullable value and should always return.

        val definedProperties = listOfNotNull(
            variant.name,
            variant.flavorName,
            variant.buildType,
            "main"
        ).mapNotNull {
            val variantConfig = variants.findByName(it) ?: return@mapNotNull null
            return@mapNotNull Pair(it, getter.invoke(variantConfig))
        }.filter { it.second != null }

        // This is a case where the property is defined in both build type and flavor.
        // In this case it should fail because the result is ambiguous.
        val propMap = definedProperties.toMap()
        if (variant.flavorName in propMap &&
            variant.buildType in propMap &&
            propMap[variant.flavorName] != propMap[variant.buildType]
        ) {
            throw GradleException(
                """
            The per-variant configuration for baseline profiles is ambiguous. This happens when
            that the same property has been defined in both a build type and a flavor.

            For example:

            baselineProfiles {
                variants {
                    free {
                        saveInSrc = true
                    }
                    release {
                        saveInSrc = false
                    }
                }
            }

            In this case for `freeRelease` it's not possible to determine the exact value of the
            property. Please specify either the build type or the flavor.
            """.trimIndent()
            )
        }

        val value = definedProperties.firstOrNull()?.second
        if (value != null) {
            return value
        }
        if (default != null) {
            return default
        }

        // This should never happen. It means the extension is missing a default property and no
        // default was specified when accessing this value. This cannot happen because of the user
        // configuration.
        throw GradleException("The required property does not have a default.")
    }

    fun BaselineProfileConsumerExtension.registerPrintConfigurationTaskForVariant(
        project: Project,
        variant: Variant
    ) {
        project
            .tasks
            .maybeRegister<PrintConfigurationForVariant>(
                "printBaselineProfileExtensionForVariant",
                variant.name
            ) {
                it.text.set(
                    """
            mergeIntoMain=`${getValueForVariant(variant, default = "null") { mergeIntoMain }}`
            baselineProfileOutputDir=`${getValueForVariant(variant) { baselineProfileOutputDir }}`
            enableR8BaselineProfileRewrite=`${getValueForVariant(variant) { enableR8BaselineProfileRewrite }}`
            saveInSrc=`${getValueForVariant(variant) { saveInSrc }}`
            automaticGenerationDuringBuild=`${getValueForVariant(variant) { automaticGenerationDuringBuild }}`
                """.trimIndent()
                )
            }
    }
}

@DisableCachingByDefault(because = "Not worth caching. Used only for tests.")
abstract class PrintConfigurationForVariant : DefaultTask() {

    @get: Input
    abstract val text: Property<String>

    @TaskAction
    fun exec() {
        logger.warn(text.get())
    }
}

@DisableCachingByDefault(because = "Not worth caching.")
abstract class MainGenerateBaselineProfileTask : DefaultTask() {

    init {
        group = "Baseline Profile"
        description = "Generates a baseline profile"
    }

    @TaskAction
    fun exec() {
        this.logger.warn(
            """
                The task `generateBaselineProfile` cannot currently support
                generation for all the variants when there are multiple build
                types without improvements planned for a future version of the
                Android Gradle Plugin.
                Until then, `generateBaselineProfile` will only generate
                baseline profiles for the variants of the release build type,
                behaving like `generateReleaseBaselineProfile`.
                If you intend to generate profiles for multiple build types
                you'll need to run separate gradle commands for each build type.
                For example: `generateReleaseBaselineProfile` and
                `generateAnotherReleaseBaselineProfile`.

                Details on https://issuetracker.google.com/issue?id=270433400.
                """.trimIndent()
        )
    }
}

@DisableCachingByDefault(because = "Not worth caching.")
abstract class GenerateDummyBaselineProfileTask : DefaultTask() {

    companion object {
        fun setupForVariant(
            project: Project,
            variant: Variant
        ) {
            val taskProvider = project
                .tasks
                .maybeRegister<GenerateDummyBaselineProfileTask>(
                    "generate", variant.name, "profileForR8RuleRewrite"
                ) {
                    it.outputDir.set(
                        project
                            .layout
                            .buildDirectory
                            .dir("$INTERMEDIATES_BASE_FOLDER/${variant.name}/empty/")
                    )
                    it.variantName.set(variant.name)
                }
            @Suppress("UnstableApiUsage")
            variant.sources.baselineProfiles?.addGeneratedSourceDirectory(
                taskProvider, GenerateDummyBaselineProfileTask::outputDir
            )
        }
    }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val variantName: Property<String>

    @TaskAction
    fun exec() {
        outputDir
            .file("empty-baseline-prof.txt")
            .get()
            .asFile
            .writeText("Lignore/This;")
    }
}
