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
import androidx.annotation.RestrictTo
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.collectBaselineProfile
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that helps collect baseline profiles.
 *
 * @suppress
 */
@RequiresApi(28)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            currentDescription = description
            base.evaluate()
        }
    }

    public fun collectBaselineProfile(
        packageName: String,
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
        profileBlock: MacrobenchmarkScope.() -> Unit
    ) {
        collectBaselineProfile(
            currentDescription.toUniqueName(),
            packageName = packageName,
            setupBlock = setupBlock,
            profileBlock = profileBlock
        )
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
