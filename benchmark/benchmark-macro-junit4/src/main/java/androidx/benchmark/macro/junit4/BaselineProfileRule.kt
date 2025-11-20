/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.junit4

import androidx.annotation.RequiresApi
import androidx.benchmark.Arguments
import androidx.benchmark.macro.BaselineProfileConfig
import androidx.benchmark.macro.BaselineProfileResult
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.collect
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that collects Baseline Profiles to be embedded in your APK.
 *
 * These rules are used at install time to partially pre-compile your application code.
 *
 * `BaselineProfileRule` is only supported on Android 13 (API 33) and above, or if using a rooted
 * device, Android P (API 28) and above.
 *
 * Note that you can specify a `filterPredicate` to filter captured rules, for example, if you're
 * generating rules for a library, and don't want to record profiles from outside that library.
 *
 * See the [Baseline Profile Guide](https://d.android.com/baseline-profiles) for more information on
 * creating Baseline Profiles.
 *
 * @sample androidx.benchmark.samples.baselineProfileRuleSample
 * @sample androidx.benchmark.samples.baselineProfileRuleLibrarySample
 */
@RequiresApi(28)
class BaselineProfileRule : TestRule {
    private lateinit var currentDescription: Description

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                assumeTrue(Arguments.RuleType.BaselineProfile in Arguments.enabledRules)
                currentDescription = description
                base.evaluate()
            }
        }

    /**
     * Collects baseline profiles for a critical user journey, while ensuring that the generated
     * profiles are stable for a minimum of [stableIterations].
     *
     * @param packageName Package name of the app for which profiles are to be generated.
     * @param maxIterations Maximum number of iterations to run when collecting profiles.
     * @param stableIterations Minimum number of iterations to observe as stable before assuming
     *   stability, and completing profile generation.
     * @param outputFilePrefix An optional file name prefix used when creating the output file with
     *   the contents of the human readable baseline profile. For example:
     *   `outputFilePrefix-baseline-prof.txt`
     * @param includeInStartupProfile determines whether the generated profile should be also used
     *   as a startup profile. A startup profile is utilized during the build process in order to
     *   determine which classes are needed in the primary dex to optimize the startup time. This
     *   flag should be used only for startup flows, such as main application startup pre and post
     *   login or other entry points of the app. Note that methods collected in a startup profiles
     *   are also utilized for baseline profiles.
     * @param strictStability Enforce if the generated profile was stable
     * @param filterPredicate Function used to filter individual rules / lines of the baseline
     *   profile. By default, no filters are applied. Note that this works only when the target
     *   application's code is not obfuscated.
     * @param [profileBlock] defines the critical user journey.
     */
    @JvmOverloads
    fun collect(
        packageName: String,
        maxIterations: Int = 15,
        stableIterations: Int = 3,
        outputFilePrefix: String? = null,
        includeInStartupProfile: Boolean = false,
        strictStability: Boolean = false,
        filterPredicate: ((String) -> Boolean) = { true },
        profileBlock: MacrobenchmarkScope.() -> Unit,
    ) {
        collect(
            uniqueName = outputFilePrefix ?: currentDescription.toUniqueName(),
            packageName = packageName,
            stableIterations = stableIterations,
            maxIterations = maxIterations,
            includeInStartupProfile = includeInStartupProfile,
            strictStability = strictStability,
            filterPredicate = filterPredicate,
            profileBlock = profileBlock,
        )
    }

    /**
     * Collects baseline profiles for a critical user journey, while ensuring that the generated
     * profiles are stable for a minimum of [stableIterations].
     *
     * @param packageName Package name of the app for which profiles are to be generated.
     * @param maxIterations Maximum number of iterations to run when collecting profiles.
     * @param stableIterations Minimum number of iterations to observe as stable before assuming
     *   stability, and completing profile generation.
     * @param outputFilePrefix An optional file name prefix used when creating the output file with
     *   the contents of the human readable baseline profile. For example:
     *   `outputFilePrefix-baseline-prof.txt`
     * @param includeInStartupProfile determines whether the generated profile should be also used
     *   as a startup profile. A startup profile is utilized during the build process in order to
     *   determine which classes are needed in the primary dex to optimize the startup time. This
     *   flag should be used only for startup flows, such as main application startup pre and post
     *   login or other entry points of the app. Note that methods collected in a startup profiles
     *   are also utilized for baseline profiles.
     * @param strictStability Enforce if the generated profile was stable
     * @param filterPredicate Function used to filter individual rules / lines of the baseline
     *   profile. By default, no filters are applied. Note that this works only when the target
     *   application's code is not obfuscated.
     * @param [profileBlock] defines the critical user journey.
     * @return [BaselineProfileResult] which can be used to determine the absolute paths of the
     *   collected baseline profiles.
     */
    @JvmOverloads
    public fun collectWithResults(
        packageName: String,
        maxIterations: Int = 15,
        stableIterations: Int = 3,
        outputFilePrefix: String? = null,
        includeInStartupProfile: Boolean = false,
        strictStability: Boolean = false,
        filterPredicate: ((String) -> Boolean) = { true },
        profileBlock: MacrobenchmarkScope.() -> Unit,
    ): BaselineProfileResult {
        return collect(
            uniqueName = outputFilePrefix ?: currentDescription.toUniqueName(),
            packageName = packageName,
            stableIterations = stableIterations,
            maxIterations = maxIterations,
            includeInStartupProfile = includeInStartupProfile,
            strictStability = strictStability,
            filterPredicate = filterPredicate,
            profileBlock = profileBlock,
        )
    }

    /**
     * Collects baseline profiles for a critical user journey, while ensuring that the generated
     * profiles are stable.
     *
     * @param config which is used to manage the collection of Baseline Profiles.
     * @return [BaselineProfileResult] which can be used to determine the absolute paths of the
     *   collected baseline profiles.
     * @see [BaselineProfileConfig.Builder] to construct an instance of [BaselineProfileConfig].
     */
    public fun collectWithResults(config: BaselineProfileConfig): BaselineProfileResult {
        return collect(
            uniqueName = config.getOutputFilePrefix() ?: currentDescription.toUniqueName(),
            packageName = config.getPackageName(),
            stableIterations = config.getStableIterations(),
            maxIterations = config.getMaxIterations(),
            includeInStartupProfile = config.isIncludeInStartupProfile(),
            strictStability = config.isStrictStability(),
            filterPredicate = config.getFilterPredicate(),
            profileBlock = config.getProfileBlock(),
        )
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
