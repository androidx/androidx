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

package androidx.camera.testing.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for tests containing methods related to garbage collection.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class GarbageCollectionUtil {

    private static final long FINALIZE_TIMEOUT_MILLIS = 200L;
    private static final int NUM_GC_ITERATIONS = 10;

    /**
     * Causes garbage collection and ensures finalization has run before returning.
     */
    public static void runFinalization() throws TimeoutException, InterruptedException {
        runFinalization(NUM_GC_ITERATIONS);
    }

    /**
     * Runs garbage collection and ensures finalization for a specified number of iterations.
     */
    private static void runFinalization(int numGcIterations)
            throws TimeoutException, InterruptedException {
        ReferenceQueue<Object> finalizeAwaitQueue = new ReferenceQueue<>();
        PhantomReference<Object> finalizeSignal;
        // Ensure finalization occurs multiple times
        for (int i = 0; i < numGcIterations; ++i) {
            finalizeSignal = new PhantomReference<>(new Object(), finalizeAwaitQueue);
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            if (finalizeAwaitQueue.remove(FINALIZE_TIMEOUT_MILLIS) == null) {
                throw new TimeoutException(
                        "Finalization failed on iteration " + (i + 1) + " of " + numGcIterations);
            }
            finalizeSignal.clear();
        }
    }

    /**
     * Returns a TestRule that runs garbage collection and ensures finalization after each test.
     */
    @NonNull
    public static TestRule getGcRule() {
        return (base, description) -> new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                // GC is slow. Doing it once already triples the time for some tests.
                runFinalization(/* numGcIterations= */1);
            }
        };
    }

    // Ensure this utility class can't be instantiated
    private GarbageCollectionUtil() {
    }
}
