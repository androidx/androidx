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

package androidx.emoji2.benchmark.reflection;

import static org.junit.Assert.assertNotNull;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.invoke.MethodHandle;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReflectionsBenchmark {
    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Test
    public void static_methodHandle() throws Throwable {
        BenchmarkState state = benchmarkRule.getState();
        ReflectionImplementation subject = new ReflectionImplementation();
        ReflectionParent result = null;
        while (state.keepRunning()) {
            result = subject.staticActualCall();
        }
        assertNotNull(result);
    }

    @Test
    public void regularJavaDispatch() {
        BenchmarkState state = benchmarkRule.getState();
        ReflectionImplementation subject = new ReflectionImplementation();
        ReflectionParent result = null;
        while (state.keepRunning()) {
            result = subject.actualCall();
        }
        assertNotNull(result);
    }

    /**
     * This test is not an accurate reflection of first lookup cost, as it will warm up caches
     * before the main benchmark starts.
     *
     * However, it is a good _lower bound_ of the cost to do this lookup, with the assumption
     * that real world lookups will always be the same cost or slower.
     */
    @Test
    public void doWarmedUpMethodLookup() throws NoSuchMethodException, IllegalAccessException {
        BenchmarkState state = benchmarkRule.getState();
        ReflectionImplementation subject = new ReflectionImplementation();
        MethodHandle result = null;
        while (state.keepRunning()) {
            result = subject.doMethodLookup();
        }
        assertNotNull(result);
    }
}
