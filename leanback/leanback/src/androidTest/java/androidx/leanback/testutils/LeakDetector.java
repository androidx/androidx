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
package androidx.leanback.testutils;

import static org.junit.Assert.assertNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Detect leaking java objects */
public final class LeakDetector {

    private final ArrayList<WeakReference<?>> mWeakReferences = new ArrayList<>();

    public void observeObject(Object object) {
        mWeakReferences.add(new WeakReference<>(object));
    }

    public void assertNoLeak() throws Exception {
        System.gc();
        System.runFinalization();
        for (WeakReference<?> weakReference : mWeakReferences) {
            int count = 0;
            while (weakReference.get() != null && count < 5) {
                System.gc();
                System.runFinalization();
                Thread.sleep(1000);
                count++;
            }
            /**
             * Debugging leak: Sleep and run adb command:
             * adb shell am dumpheap PID_OF_TEST /data/local/tmp/test_leak.hprof
             */
            assertNull("Leaked object", weakReference.get());
        }
    }
}
