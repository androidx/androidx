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

import org.gradle.api.Action
import org.gradle.api.Incubating
import org.gradle.api.Project

/**
 * Allows specifying settings for the Baseline Profile Consumer Plugin.
 */
open class BaselineProfileConsumerExtension {

    companion object {
        private const val EXTENSION_NAME = "baselineProfile"

        internal fun registerExtension(project: Project): BaselineProfileConsumerExtension {
            val ext = project.extensions.findByType(BaselineProfileConsumerExtension::class.java)
            if (ext != null) {
                return ext
            }
            return project
                .extensions.create(EXTENSION_NAME, BaselineProfileConsumerExtension::class.java)
        }
    }

    /**
     * Specifies whether generated baseline profiles should be stored in the src folder.
     * When this flag is set to true, the generated baseline profiles are stored in
     * `src/<variant>/generated/baselineProfiles`.
     */
    var saveInSrc = true

    /**
     * Specifies whether baseline profiles should be regenerated when building, for example, during
     * a full release build for distribution. When set to true a new profile is generated as part
     * of building the release build. This including rebuilding the non minified release, running
     * the baseline profile tests and ultimately building the release build.
     */
    var automaticGenerationDuringBuild = false

    /**
     * Specifies the output directory for generated baseline profiles when [saveInSrc] is
     * `true`. Note that the dir specified here is created in the `src/<variant>/` folder.
     */
    var baselineProfileOutputDir = "generated/baselineProfiles"

    /**
     * Specifies if baseline profile files should be merged into a single one when generating for
     * multiple variants:
     *  - When `true` all the generated baseline profile for each variant are merged into
     *      `src/main/generated/baselineProfiles`'.
     *  - When `false` each variant will have its own baseline profile in
     *      `src/<variant>/generated/baselineProfiles`'.
     *  If this is not specified, by default it will be true for library modules and false for
     *  application modules.
     *  Note that when `saveInSrc` is false the output folder is in the build output folder but
     *  this setting still determines whether the profile included in the built apk or
     *  aar includes all the variant profiles.
     */
    var mergeIntoMain: Boolean? = null

    /**
     * Enables R8 to rewrite the incoming human readable baseline profile rules to account for
     * synthetics, so they are preserved after optimizations by R8.
     * TODO: This feature is experimental and currently not working properly.
     *  https://issuetracker.google.com/issue?id=271172067.
     */
    @Incubating
    var enableR8BaselineProfileRewrite = false

    /**
     * Specifies a filtering rule to decide which profiles rules should be included in this
     * consumer baseline profile. This is useful especially for libraries, in order to exclude
     * profile rules for class and methods for dependencies of the sample app. The filter supports:
     *  - Double wildcards, to match specified package and subpackages. Example: `com.example.**`
     *  - Wildcards, to match specified package only. Example: `com.example.*`
     *  - Class names, to match the specified class. Example: `com.example.MyClass`
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
     *
     * Filters also support variants and they can be expressed as follows:
     * ```
     *     filter { include "com.somelibrary.*" }
     *     filter("free") { include "com.somelibrary.*" }
     *     filter("paid") { include "com.somelibrary.*" }
     *     filter("release") { include "com.somelibrary.*" }
     *     filter("freeRelease") { include "com.somelibrary.*" }
     * ```
     * Filter block without specifying a variant applies to `main`, i.e. all the variants.
     * Note that when a variant matches multiple filter blocks, all the filters will be merged.
     * For example with `filter { ... }`, `filter("free") { ... }` and `filter("release") { ... }`
     * all the blocks will be evaluated for variant `freeRelease` but only `main` and `release` for
     * variant `paidRelease`.
     */
    @JvmOverloads
    fun filter(variant: String = "main", action: FilterRules.() -> (Unit)) = action
        .invoke(filterRules.computeIfAbsent(variant) { FilterRules() })

    /**
     * Specifies a filtering rule to decide which profiles rules should be included in this
     * consumer baseline profile. This is useful especially for libraries, in order to exclude
     * profile rules for class and methods for dependencies of the sample app. The filter supports:
     *  - Double wildcards, to match specified package and subpackages. Example: `com.example.**`
     *  - Wildcards, to match specified package only. Example: `com.example.*`
     *  - Class names, to match the specified class. Example: `com.example.MyClass`
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
     *
     * Filters also support variants and they can be expressed as follows:
     * ```
     *     filter { include "com.somelibrary.*" }
     *     filter("free") { include "com.somelibrary.*" }
     *     filter("paid") { include "com.somelibrary.*" }
     *     filter("release") { include "com.somelibrary.*" }
     *     filter("freeRelease") { include "com.somelibrary.*" }
     * ```
     * Filter block without specifying a variant applies to `main`, i.e. all the variants.
     * Note that when a variant matches multiple filter blocks, all the filters will be merged.
     * For example with `filter { ... }`, `filter("free") { ... }` and `filter("release") { ... }`
     * all the blocks will be evaluated for variant `freeRelease` but only `main` and `release` for
     * variant `paidRelease`.
     */
    @JvmOverloads
    fun filter(variant: String = "main", action: Action<FilterRules>) = action
        .execute(filterRules.computeIfAbsent(variant) { FilterRules() })

    internal val filterRules = mutableMapOf<String, FilterRules>()
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
