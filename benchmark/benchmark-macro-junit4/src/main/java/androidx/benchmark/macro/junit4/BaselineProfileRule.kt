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

import android.Manifest
import androidx.annotation.RequiresApi
import androidx.benchmark.Arguments
import androidx.benchmark.macro.ExperimentalStableBaselineProfilesApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.collectBaselineProfile
import androidx.benchmark.macro.collectStableBaselineProfile
import androidx.test.rule.GrantPermissionRule
import org.junit.Assume.assumeTrue
import org.junit.rules.RuleChain
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
 * ```
 * @RunWith(AndroidJUnit4::class)
 * class BaselineProfileGenerator {
 *     @get:Rule
 *     val baselineProfileRule = BaselineProfileRule()
 *
 *     @Test
 *     fun startup() = baselineProfileRule.collectBaselineProfile(
 *         packageName = "com.example.my.application.id"
 *     ) {
 *         pressHome()
 *         // This block defines the app's critical user journey. Here we are
 *         // interested in optimizing for app startup, but you can also navigate
 *         // and scroll through your most important UI.
 *         startActivityAndWait()
 *     }
 * }
 * ```
 *
 * See the [Baseline Profile Guide](https://d.android.com/baseline-profiles) for more information
 * on creating Baseline Profiles.
 */
@RequiresApi(28)
class BaselineProfileRule : TestRule {
    private lateinit var currentDescription: Description

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(::applyInternal)
            .apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            assumeTrue(Arguments.RuleType.BaselineProfile in Arguments.enabledRules)
            currentDescription = description
            base.evaluate()
        }
    }

    /**
     * Collects baseline profiles for a set of interactions with the application
     * @param packageName ApplicationId / Application manifest package name of the app for
     *   which profiles are generated.
     * @param packageFilters List of package names to use as a filter for the generated profiles.
     *  By default no filters are applied. Note that this works only when the code is not
     *  obfuscated.
     * @param [profileBlock] defines the critical user journey.
     */
    @JvmOverloads
    public fun collectBaselineProfile(
        packageName: String,
        iterations: Int = 3,
        packageFilters: List<String> = emptyList(),
        profileBlock: MacrobenchmarkScope.() -> Unit
    ) {
        collectBaselineProfile(
            uniqueName = currentDescription.toUniqueName(),
            packageName = packageName,
            iterations = iterations,
            packageFilters = packageFilters,
            profileBlock = profileBlock
        )
    }

    /**
     * Collects baseline profiles for a critical user journey, while ensuring that the generated
     * profiles are stable for a minimum of [stableIterations].
     *
     * @param packageName Package name of the app for which profiles are to be generated.
     * @param maxIterations Maximum number of iterations to run for when collecting profiles.
     * @param stableIterations Minimum number of iterations for while baseline profiles have to be stable.
     * @param strictStability Enforce if the generated profile was stable
     * @param packageFilters List of package names to use as a filter for the generated profiles.
     *  By default no filters are applied. Note that this works only when the code is not obfuscated.
     *  Package filters are only applied after the profiles are deemed stable.
     * @param [profileBlock] defines the critical user journey.
     */
    @JvmOverloads
    @ExperimentalStableBaselineProfilesApi
    public fun collectStableBaselineProfile(
        packageName: String,
        maxIterations: Int,
        stableIterations: Int = 3,
        strictStability: Boolean = false,
        packageFilters: List<String> = emptyList(),
        profileBlock: MacrobenchmarkScope.() -> Unit
    ) {
        collectStableBaselineProfile(
            uniqueName = currentDescription.toUniqueName(),
            packageName = packageName,
            stableIterations = stableIterations,
            maxIterations = maxIterations,
            strictStability = strictStability,
            packageFilters = packageFilters,
            profileBlock = profileBlock
        )
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
