/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.testing

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.internal.NullTranslatorSelector
import androidx.appfunctions.testing.internal.FakeAppFunctionManagerApi
import androidx.appfunctions.testing.internal.FakeAppFunctionReader
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit TestRule for setting up an environment to exercise AppFunction APIs in unit or
 * Robolectric tests.
 *
 * Prefer using the real system setup whenever possible. This rule should only be used for local
 * tests that simulate cross-app interactions via AppFunctions.
 *
 * Any functions annotated with [androidx.appfunctions.service.AppFunction] in test code will be
 * automatically registered in this environment during initialization, provided the
 * `appfunctions-compiler` is applied to the test configuration.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class AppFunctionTestRule(private val context: Context) : TestRule {
    // TODO: b/426219836 - Dynamic registration and changing app function enabled state API(s).
    // TODO: b/425327400 - Move to use Robolectric shadows

    private val appFunctionReader = FakeAppFunctionReader(context)
    private val appFunctionManagerApi = FakeAppFunctionManagerApi(context, appFunctionReader)

    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            override fun evaluate() {
                base?.evaluate()
            }
        }

    /**
     * Returns an [AppFunctionManagerCompat] instance for interacting with AppFunctions registered
     * via the test rule.
     */
    public fun getAppFunctionManagerCompat(): AppFunctionManagerCompat {
        return AppFunctionManagerCompat(
            context = context,
            appFunctionReader = appFunctionReader,
            appFunctionManagerApi = appFunctionManagerApi,
            translatorSelector = NullTranslatorSelector(),
        )
    }
}
