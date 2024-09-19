/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.test.filters.LargeTest
import androidx.testutils.createCompilationParams
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class AnimatedIconButtonBenchmark(compilationMode: CompilationMode) :
    ButtonBenchmarkBase(compilationMode, BUTTON_ACTIVITY) {
    companion object {
        private const val BUTTON_ACTIVITY =
            "androidx.wear.compose.integration.macrobenchmark.target.ANIMATED_ICON_BUTTON_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
