/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark;

import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@LargeTest
@RunWith(JUnit4.class)
public class TrivialBenchmarkJava {
    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    public void nothing() {
        BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            // nothing
        }
    }

    @Test
    public void increment() {
        BenchmarkState state = mBenchmarkRule.getState();
        int i = 0;
        while (state.keepRunning()) {
            i++;
        }
    }
}
