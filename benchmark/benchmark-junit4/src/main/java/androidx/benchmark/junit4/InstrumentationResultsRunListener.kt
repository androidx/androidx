/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.junit4

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.benchmark.InstrumentationResults
import androidx.test.internal.runner.listener.InstrumentationRunListener
import org.junit.runner.Result
import java.io.PrintStream

/**
 * Used to register files to copy at the end of the entire test run in CI.
 *
 * See [InstrumentationResults.runEndResultBundle]
 *
 * @hide
 */
@Suppress("unused") // referenced by inst arg at runtime
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InstrumentationResultsRunListener : InstrumentationRunListener() {
    override fun instrumentationRunFinished(
        streamResult: PrintStream?,
        resultBundle: Bundle,
        junitResults: Result?
    ) {
        resultBundle.putAll(InstrumentationResults.runEndResultBundle)
    }
}