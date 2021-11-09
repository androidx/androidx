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

package androidx.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CircularArrayTest {
    private static final String ELEMENT_X = "x";
    private static final String ELEMENT_Y = "y";
    private static final String ELEMENT_Z = "z";

    @Test(expected = IllegalArgumentException.class)
    public void creatingWithZeroCapacity() {
        new CircularArray<String>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingWithOverCapacity() {
        new CircularArray<String>(Integer.MAX_VALUE);
    }

    @Test
    public void basicOperations() {
        CircularArray<String> array = new CircularArray<>();

        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
        array.addFirst(ELEMENT_X);
        array.addFirst(ELEMENT_Y);
        array.addLast(ELEMENT_Z);
        assertFalse(array.isEmpty());
        assertEquals(3, array.size());

        assertEquals(ELEMENT_Y, array.getFirst());
        assertEquals(ELEMENT_Z, array.getLast());
        assertEquals(ELEMENT_X, array.get(1));

        assertEquals(ELEMENT_Y, array.popFirst());
        assertEquals(ELEMENT_Z, array.popLast());
        assertEquals(ELEMENT_X, array.getFirst());
        assertEquals(ELEMENT_X, array.getLast());
        assertEquals(ELEMENT_X, array.popFirst());
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overpoppingFromStart() {
        CircularArray<String> array = new CircularArray<>();
        array.addFirst(ELEMENT_X);
        array.popFirst();
        array.popFirst();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overpoppingFromEnd() {
        CircularArray<String> array = new CircularArray<>();
        array.addFirst(ELEMENT_X);
        array.popLast();
        array.popLast();
    }

    @Test
    public void removeFromEitherEnd() {
        CircularArray<String> array = new CircularArray<>();
        array.addFirst(ELEMENT_X);
        array.addFirst(ELEMENT_Y);
        array.addLast(ELEMENT_Z);

        // These are no-ops.
        array.removeFromStart(0);
        array.removeFromStart(-1);
        array.removeFromEnd(0);
        array.removeFromEnd(-1);

        array.removeFromStart(2);
        assertEquals(ELEMENT_Z, array.getFirst());
        array.removeFromEnd(1);
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overremovalFromStart() {
        CircularArray<String> array = new CircularArray<>();
        array.addFirst(ELEMENT_X);
        array.addFirst(ELEMENT_Y);
        array.addLast(ELEMENT_Z);
        array.removeFromStart(4);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overremovalFromEnd() {
        CircularArray<String> array = new CircularArray<>();
        array.addFirst(ELEMENT_X);
        array.addFirst(ELEMENT_Y);
        array.addLast(ELEMENT_Z);
        array.removeFromEnd(4);
    }

    @Test
    public void grow() {
        CircularArray<String> array = new CircularArray<>(1);
        final int expectedSize = 32768;
        for (int i = 0; i < expectedSize; i++) {
            array.addFirst("String " + i);
        }
        assertEquals(expectedSize, array.size());
    }
}
