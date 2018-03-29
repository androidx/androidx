/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.testlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.os.Bundle;

/**
 * Utility methods used for testing.
 */
public final class TestUtil {

    /**
     * Asserts that two Bundles are equal.
     */
    public static void assertBundleEquals(Bundle expected, Bundle observed) {
        if (expected == null || observed == null) {
            assertSame(expected, observed);
        }
        assertEquals(expected.size(), observed.size());
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), observed.get(key));
        }
    }

    private TestUtil() {
    }
}
