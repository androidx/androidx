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

package androidx.benchmark.macro.junit4

import androidx.annotation.RestrictTo
import androidx.benchmark.DisableDexOpt
import androidx.benchmark.DisablePackages
import androidx.benchmark.RunListenerDelegate
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.RunListener

/**
 * Enables the use of side-effects that reduce the noise during a macro benchmark run.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SideEffectRunListener : RunListener() {
    private val delegate: RunListenerDelegate = RunListenerDelegate(
        sideEffects = listOf(
            DisablePackages(),
            DisableDexOpt(),
        )
    )

    override fun testRunStarted(description: Description) {
        super.testRunStarted(description)
        delegate.onTestRunStarted()
    }

    override fun testRunFinished(result: Result) {
        super.testRunFinished(result)
        delegate.onTestRunFinished()
    }
}
