/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

/**
 * A collection of utility methods that supports asserting conditions in tests.
 */
public class Assertions {

    private Assertions() {
        // No instantiable class.
    }

    /**
     * Asserts the given block throws the given throwable.
     */
    public static void assertThrows(Class<? extends Throwable> throwableClass, Runnable runnable) {
        try {
            runnable.run();
            fail();
        } catch (Throwable e) {
            assertEquals(e.getClass(), throwableClass);
        }
    }

    /**
     * Asserts the given list can not be mutated.
     */
    public static void assertImmutable(List<?> list) {
        assertThrows(UnsupportedOperationException.class, () -> list.add(null));
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, null));
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    }
}
