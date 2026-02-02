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

import androidx.baselineprofile.gradle.utils.WarningsExtension
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

/** Allows specifying settings for the Baseline Profile Consumer Plugin. */
abstract class BaselineProfileConsumerExtension @Inject constructor(objectFactory: ObjectFactory) :
    BaselineProfileVariantConfiguration, ExtensionAware {

    companion object {
        private const val EXTENSION_NAME = "baselineProfile"

        internal fun register(project: Project): BaselineProfileConsumerExtension {
            val ext = project.extensions.findByType(BaselineProfileConsumerExtension::class.java)
            if (ext != null) {
                return ext
            }
            return project.extensions.create(
                EXTENSION_NAME,
                BaselineProfileConsumerExtension::class.java
            )
        }
    }

    val warnings = WarningsExtension.register(this.extensions)

    val variants: NamedDomainObjectContainer<BaselineProfileVariantConfigurationImpl> =
        objectFactory.domainObjectContainer(BaselineProfileVariantConfigurationImpl::class.java)

    // Shortcut to access the "main" variant.
    private val main: BaselineProfileVariantConfiguration =
        variants.create("main") {

            // These are the default global settings.
            it.mergeIntoMain = null
            it.baselineProfileOutputDir = "generated/baselineProfiles"
            it.baselineProfileRulesRewrite = null
            it.dexLayoutOptimization = null
            it.saveInSrc = true
            it.automaticGenerationDuringBuild = false
        }

    /**
     * Controls whether Android Studio should hide synthetic build types created to generate
     * baseline profiles and run benchmarks. These build types are copied from the existing release
     * ones, adding as prefix `nonMinified` and `benchmark`. For example, if the build type is
     * `release` the new build type will be `nonMinifiedRelease` and `benchmarkRelease`. Note that
     * in case of defined product flavors, these are normally merged in the variant name. For
     * example with flavor `free` the variant name will be `freeNonMinifiedRelease` and
     * `freeBenchmarkRelease`.
     */
    var hideSyntheticBuildTypesInAndroidStudio: Boolean = true

    /**
     * Controls the global [BaselineProfileVariantConfiguration.baselineProfileRulesRewrite]. Note
     * that this value is overridden by per variant configurations.
     */
    override var baselineProfileRulesRewrite: Boolean?
        get() = main.baselineProfileRulesRewrite
        set(value) {
            main.baselineProfileRulesRewrite = value
        }

    /**
     * Controls the global [BaselineProfileVariantConfiguration.dexLayoutOptimization]. Note that
     * this value is overridden by per variant configurations.
     */
    override var dexLayoutOptimization: Boolean?
        get() = main.dexLayoutOptimization
        set(value) {
            main.dexLayoutOptimization = value
        }

    /**
     * Controls the global [BaselineProfileVariantConfiguration.saveInSrc]. Note that this value is
     * overridden by per variant configurations.
     */
    override var saveInSrc: Boolean?
        get() = main.saveInSrc
        set(value) {
            main.saveInSrc = value
        }

    /**
     * Controls the global [BaselineProfileVariantConfiguration.automaticGenerationDuringBuild].
     * Note that this value is overridden by per variant configurations.
     */
    override var automaticGenerationDuringBuild: Boolean?
        get() = main.automaticGenerationDuringBuild
        set(value) {
            main.automaticGenerationDuringBuild = value
        }

    /**
     * Controls the global [BaselineProfileVariantConfiguration.baselineProfileOutputDir]. Note that
     * this value is overridden by per variant configurations.
     */
    override var baselineProfileOutputDir: String?
        get() = main.baselineProfileOutputDir
        set(value) {
            main.baselineProfileOutputDir = value
        }

    /**
     * Controls the global [BaselineProfileVariantConfiguration.mergeIntoMain]. Note that this value
     * is overridden by per variant configurations.
     */
    override var mergeIntoMain: Boolean?
        get() = main.mergeIntoMain
        set(value) {
            main.mergeIntoMain = value
        }

    /**
     * Applies the global [BaselineProfileVariantConfiguration.filter]. This function is just a
     * shortcut for `baselineProfiles.variants.main.filters { }`
     */
    override fun filter(action: FilterRules.() -> (Unit)) = main.filter(action)

    /**
     * Applies the global [BaselineProfileVariantConfiguration.filter]. This function is just a
     * shortcut for `baselineProfiles.variants.main.filters { }`
     */
    override fun filter(action: Action<FilterRules>) = main.filter(action)

    /**
     * Applies global dependencies for baseline profiles. This has the same effect of defining a
     * baseline profile dependency in the dependency block. For example:
     * ```
     * dependencies {
     *     baselineProfile(project(":baseline-profile"))
     * }
     * ```
     */
    override fun from(project: Project) = main.from(project)

    fun variants(
        action: Action<NamedDomainObjectContainer<BaselineProfileVariantConfigurationImpl>>
    ) {
        action.execute(variants)
    }

    fun variants(
        action: NamedDomainObjectContainer<out BaselineProfileVariantConfigurationImpl>.() -> Unit
    ) {
        action.invoke(variants)
    }
}

abstract class BaselineProfileVariantConfigurationImpl(val name: String) :
    BaselineProfileVariantConfiguration {

    internal val filters = FilterRules()
    internal val dependencies = mutableListOf<Project>()

    /** @inheritDoc */
    override fun filter(action: FilterRules.() -> (Unit)) = action.invoke(filters)

    /** @inheritDoc */
    override fun filter(action: Action<FilterRules>) = action.execute(filters)

    /** @inheritDoc */
    override fun from(project: Project) {
        dependencies.add(project)
    }
}

/**
 * Defines the configuration properties that each variant of a consumer module offers. Note that
 * also [BaselineProfileConsumerExtension] is an implementation of this interface and it's simply a
 * proxy to the `main` variant.
 */
interface BaselineProfileVariantConfiguration {

    /**
     * Enables R8 to rewrite the incoming human readable baseline profile rules to account for
     * synthetics, so they are preserved after optimizations by R8.
     *
     * TODO: This feature is experimental and currently not working properly.
     *   https://issuetracker.google.com/issue?id=271172067.
     */
    var baselineProfileRulesRewrite: Boolean?

    /**
     * Enables R8 to optimize the primary dex file used to contain only classes utilized for
     * startup.
     */
    var dexLayoutOptimization: Boolean?

    /**
     * Specifies whether generated baseline profiles should be stored in the src folder. When this
     * flag is set to true, the generated baseline profiles are stored in
     * `src/<variant>/generated/baselineProfiles`.
     */
    var saveInSrc: Boolean?

    /**
     * Specifies whether baseline profiles should be regenerated when building, for example, during
     * a full release build for distribution. When set to true a new profile is generated as part of
     * building the release build. This including rebuilding the non minified release, running the
     * baseline profile tests and ultimately building the release build.
     */
    var automaticGenerationDuringBuild: Boolean?

    /**
     * Specifies the output directory for generated baseline profiles when
     * [BaselineProfileVariantConfiguration.saveInSrc] is `true`. Note that the dir specified here
     * is created in the `src/<variant>/` folder.
     */
    var baselineProfileOutputDir: String?

    /**
     * Specifies if baseline profile files should be merged into a single one when generating for
     * multiple variants:
     * - When `true` all the generated baseline profile for each variant are merged into
     *   `src/main/generated/baselineProfiles`'.
     * - When `false` each variant will have its own baseline profile in
     *   `src/<variant>/generated/baselineProfiles`'. If this is not specified, by default it will
     *   be true for library modules and false for application modules. Note that when `saveInSrc`
     *   is false the output folder is in the build output folder but this setting still determines
     *   whether the profile included in the built apk or aar includes all the variant profiles.
     */
    var mergeIntoMain: Boolean?

    /**
     * Specifies a filtering rule to decide which profiles rules should be included in this consumer
     * baseline profile. This is useful especially for libraries, in order to exclude profile rules
     * for class and methods for dependencies of the sample app. The filter supports:
     * - Double wildcards, to match specified package and subpackages. Example: `com.example.**`
     * - Wildcards, to match specified package only. Example: `com.example.*`
     * - Class names, to match the specified class. Example: `com.example.MyClass`
     *
     * Note that when only excludes are specified, if there are no matches with any rule the profile
     * rule is selected.
     *
     * Example to include a package and all the subpackages:
     * ```
     *     filter { include "com.somelibrary.**" }
     * ```
     *
     * Example to exclude some packages and include all the rest:
     * ```
     *     filter { exclude "com.somelibrary.debug" }
     * ```
     *
     * Example to include and exclude specific packages:
     * ```
     *     filter {
     *          include "com.somelibrary.widget.grid.**"
     *          exclude "com.somelibrary.widget.grid.debug.**"
     *          include "com.somelibrary.widget.list.**"
     *          exclude "com.somelibrary.widget.grid.debug.**"
     *          include "com.somelibrary.widget.text.**"
     *          exclude "com.somelibrary.widget.grid.debug.**"
     *     }
     * ```
     */
    fun filter(action: FilterRules.() -> (Unit))

    /**
     * Specifies a filtering rule to decide which profiles rules should be included in this consumer
     * baseline profile. This is useful especially for libraries, in order to exclude profile rules
     * for class and methods for dependencies of the sample app. The filter supports:
     * - Double wildcards, to match specified package and subpackages. Example: `com.example.**`
     * - Wildcards, to match specified package only. Example: `com.example.*`
     * - Class names, to match the specified class. Example: `com.example.MyClass`
     *
     * Note that when only excludes are specified, if there are no matches with any rule the profile
     * rule is selected.
     *
     * Example to include a package and all the subpackages:
     * ```
     *     filter { include "com.somelibrary.**" }
     * ```
     *
     * Example to exclude some packages and include all the rest:
     * ```
     *     filter { exclude "com.somelibrary.debug" }
     * ```
     *
     * Example to include and exclude specific packages:
     * ```
     *     filter {
     *          include "com.somelibrary.widget.grid.**"
     *          exclude "com.somelibrary.widget.grid.debug.**"
     *          include "com.somelibrary.widget.list.**"
     *          exclude "com.somelibrary.widget.list.debug.**"
     *          include "com.somelibrary.widget.text.**"
     *          exclude "com.somelibrary.widget.text.debug.**"
     *     }
     * ```
     */
    fun filter(action: Action<FilterRules>)

    /**
     * Allows to specify a target `com.android.test` module that has the `androidx.baselineprofile`
     * plugin, and that can provide a baseline profile for this module. For example
     *
     * ```
     * baselineProfile {
     *     variants {
     *         freeRelease {
     *             from(project(":baseline-profile"))
     *         }
     *     }
     * }
     * ```
     */
    fun from(project: Project)
}

class FilterRules {

    internal val rules = mutableListOf<Pair<RuleType, String>>()

    fun include(pkg: String) = rules.add(Pair(RuleType.INCLUDE, pkg))

    fun exclude(pkg: String) = rules.add(Pair(RuleType.EXCLUDE, pkg))
}

enum class RuleType {
    INCLUDE,
    EXCLUDE
}
