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
import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.collectBaselineProfile
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
 * ```
 * @ExperimentalBaselineProfilesApi
 * @RunWith(AndroidJUnit4::class)
 * class BaselineProfileGenerator {
 *     @get:Rule
 *     val baselineProfileRule = BaselineProfileRule()
 *
 *     @Test
 *     fun startup() = baselineProfileRule.collectBaselineProfile(
 *         packageName = "com.example.app"
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
 * See the [Baseline Profile Guide](https://developer.android.com/studio/profile/baselineprofiles)
 * for more information on creating Baseline Profiles.
 */
@RequiresApi(28)
@ExperimentalBaselineProfilesApi
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
     * Collects baseline profiles for a critical user journey.
     * @param packageName Package name of the app for which profiles are to be generated.
     * @param [profileBlock] defines the critical user journey.
     */
    public fun collectBaselineProfile(
        packageName: String,
        profileBlock: MacrobenchmarkScope.() -> Unit
    ) {
        collectBaselineProfile(
            currentDescription.toUniqueName(),
            packageName = packageName,
            profileBlock = profileBlock
        )
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
