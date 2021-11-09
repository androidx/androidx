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
public class CircularIntArrayTest {
    @Test(expected = IllegalArgumentException.class)
    public void creatingWithZeroCapacity() {
        new CircularIntArray(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingWithOverCapacity() {
        new CircularIntArray(Integer.MAX_VALUE);
    }

    @Test
    public void basicOperations() {
        CircularIntArray array = new CircularIntArray();

        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
        array.addFirst(42);
        array.addFirst(43);
        array.addLast(-1);
        assertFalse(array.isEmpty());
        assertEquals(3, array.size());

        assertEquals(43, array.getFirst());
        assertEquals(-1, array.getLast());
        assertEquals(42, array.get(1));

        assertEquals(43, array.popFirst());
        assertEquals(-1, array.popLast());
        assertEquals(42, array.getFirst());
        assertEquals(42, array.getLast());
        assertEquals(42, array.popFirst());
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overpoppingFromStart() {
        CircularIntArray array = new CircularIntArray();
        array.addFirst(42);
        array.popFirst();
        array.popFirst();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overpoppingFromEnd() {
        CircularIntArray array = new CircularIntArray();
        array.addFirst(42);
        array.popLast();
        array.popLast();
    }

    @Test
    public void removeFromEitherEnd() {
        CircularIntArray array = new CircularIntArray();
        array.addFirst(42);
        array.addFirst(43);
        array.addLast(-1);

        // These are no-ops.
        array.removeFromStart(0);
        array.removeFromStart(-1);
        array.removeFromEnd(0);
        array.removeFromEnd(-1);

        array.removeFromStart(2);
        assertEquals(-1, array.getFirst());
        array.removeFromEnd(1);
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overremovalFromStart() {
        CircularIntArray array = new CircularIntArray();
        array.addFirst(42);
        array.addFirst(43);
        array.addLast(-1);
        array.removeFromStart(4);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void overremovalFromEnd() {
        CircularIntArray array = new CircularIntArray();
        array.addFirst(42);
        array.addFirst(43);
        array.addLast(-1);
        array.removeFromEnd(4);
    }

    @Test
    public void grow() {
        CircularIntArray array = new CircularIntArray(1);
        final int expectedSize = 32768;
        for (int i = 0; i < expectedSize; i++) {
            array.addFirst(i);
        }
        assertEquals(expectedSize, array.size());
    }

}
