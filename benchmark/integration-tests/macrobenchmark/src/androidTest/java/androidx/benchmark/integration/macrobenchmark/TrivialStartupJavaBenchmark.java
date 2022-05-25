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

package androidx.benchmark.integration.macrobenchmark;

import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.filters.SdkSuppress;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import kotlin.Unit;

public class TrivialStartupJavaBenchmark {
    @Rule
    public MacrobenchmarkRule mBenchmarkRule = new MacrobenchmarkRule();

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void startup() {
        mBenchmarkRule.measureRepeated(
                "androidx.benchmark.integration.macrobenchmark.target",
                Collections.singletonList(new StartupTimingMetric()),
                new CompilationMode.Partial(),
                StartupMode.COLD,
                3,
                scope -> {
                    scope.pressHome();
                    return Unit.INSTANCE;
                },
                scope -> {
                    scope.pressHome();
                    scope.startActivityAndWait();
                    return Unit.INSTANCE;
                }
        );
    }
}
