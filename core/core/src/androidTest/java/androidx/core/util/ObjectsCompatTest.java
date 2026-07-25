/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for ObjectsCompat
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ObjectsCompatTest {

    @Test
    public void testEquals() throws Exception {
        String a = "aaa";
        String b = "bbb";
        String c = new String(a);
        String n = null;

        assertFalse(ObjectsCompat.equals(a, b));
        assertFalse(ObjectsCompat.equals(a, n));
        assertFalse(ObjectsCompat.equals(n, a));

        assertTrue(ObjectsCompat.equals(n, n));
        assertTrue(ObjectsCompat.equals(a, a));
        assertTrue(ObjectsCompat.equals(a, c));
    }

    @Test
    public void testHashCode() {
        String a = "aaa";
        String n = null;

        assertEquals(ObjectsCompat.hashCode(a), a.hashCode());
        assertEquals(ObjectsCompat.hashCode(n), 0);
    }

    @Test
    public void testToString() {
        String a = "aaa";
        String b = "bbb";

        assertEquals(ObjectsCompat.toString(a, b), a);
        assertEquals(ObjectsCompat.toString(null, b), b);
    }

    @Test
    public void testRequireNotNull() {
        ObjectsCompat.requireNonNull(new Object(), "Message");
        ObjectsCompat.requireNonNull(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void testRequireNonNullException() {
        ObjectsCompat.requireNonNull(null);
    }

    @Test(expected = NullPointerException.class)
    public void testRequireNotNullExceptionWithMessage() {
        ObjectsCompat.requireNonNull(null, "Message");
    }

    @Test
    public void testRequireNonNullElse() {
        String a = "aaa";
        String b = "bbb";
        assertEquals(ObjectsCompat.requireNonNullElse(a, b), a);
        assertEquals(ObjectsCompat.requireNonNullElse(null, b), b);
    }

    @Test(expected = NullPointerException.class)
    public void testRequireNonNullElseExceptionWhenBothNull() {
        ObjectsCompat.requireNonNullElse(null, null);
    }

    @Test
    public void testRequireNonNullElseGet() {
        String a = "aaa";
        String b = "bbb";
        assertEquals(ObjectsCompat.requireNonNullElseGet(a, () -> b), a);
        assertEquals(ObjectsCompat.requireNonNullElseGet(null, () -> b), b);
    }

    @Test
    public void testRequireNonNullElseGetSupplierNotCalledWhenObjNonNull() {
        String a = "aaa";
        boolean[] called = new boolean[1];
        ObjectsCompat.requireNonNullElseGet(a, () -> {
            called[0] = true;
            return "bbb";
        });
        assertFalse(called[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testRequireNonNullElseGetExceptionWhenSupplierNull() {
        ObjectsCompat.requireNonNullElseGet(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testRequireNonNullElseGetExceptionWhenSupplierGetReturnsNull() {
        ObjectsCompat.requireNonNullElseGet(null, () -> null);
    }

    @Test
    public void testRequireNonNullElseGetNullSupplierWithNonNullObj() {
        String a = "aaa";
        assertEquals(ObjectsCompat.requireNonNullElseGet(a, null), a);
    }
}
