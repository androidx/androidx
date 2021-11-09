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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@RunWith(JUnit4.class)
public class IndexBasedArrayIteratorTest {

    @Test
    public void iterateAll() {
        Iterator<String> iterator = new ArraySet<>(setOf("a", "b", "c")).iterator();
        assertThat(toList(iterator)).containsExactly("a", "b", "c");
    }

    @Test
    public void iterateEmptyList() {
        Iterator<String> iterator = new ArraySet<String>().iterator();
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test(expected = NoSuchElementException.class)
    public void iterateEmptyListThrowsUponNext() {
        Iterator<String> iterator = new ArraySet<String>().iterator();
        iterator.next();
    }

    @Test(expected = IllegalStateException.class)
    public void removeSameItemTwice() {
        Iterator<String> iterator = new ArraySet<>(listOf("a", "b", "c")).iterator();
        iterator.next(); // move to next
        iterator.remove();
        iterator.remove();
    }


    @Test
    public void removeLast() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c"),
                /* toBeRemoved= */ setOf("c"),
                /* expected= */ setOf("a", "b"));
    }

    @Test
    public void removeFirst() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c"),
                /* toBeRemoved= */ setOf("a"),
                /* expected= */ setOf("b", "c"));
    }

    @Test
    public void removeMid() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c"),
                /* toBeRemoved= */ setOf("b"),
                /* expected= */ setOf("a", "c"));
    }

    @Test
    public void removeConsecutive() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c", "d"),
                /* toBeRemoved= */ setOf("b", "c"),
                /* expected= */ setOf("a", "d"));
    }

    @Test
    public void removeLastTwo() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c", "d"),
                /* toBeRemoved= */ setOf("c", "d"),
                /* expected= */ setOf("a", "b"));
    }

    @Test
    public void removeFirstTwo() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c", "d"),
                /* toBeRemoved= */ setOf("a", "b"),
                /* expected= */ setOf("c", "d"));
    }

    @Test
    public void removeMultiple() {
        removeViaIterator(
                /* original= */ setOf("a", "b", "c", "d"),
                /* toBeRemoved= */ setOf("a", "c"),
                /* expected= */ setOf("b", "d"));
    }

    private static void removeViaIterator(
            Set<String> original,
            Set<String> toBeRemoved,
            Set<String> expected) {
        ArraySet<String> subject = new ArraySet<>(original);
        Iterator<String> iterator = subject.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (toBeRemoved.contains(next)) {
                iterator.remove();
            }
        }
        assertThat(subject).containsExactlyElementsIn(expected);
    }

    @SuppressWarnings("unchecked")
    private static <V> List<V> listOf(V... values) {
        List<V> list = new ArrayList<>();
        for (V value : values) {
            list.add(value);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static <V> Set<V> setOf(V... values) {
        Set<V> set = new HashSet<>();
        for (V value : values) {
            set.add(value);
        }
        return set;
    }

    private static <V> List<V> toList(Iterator<V> iterator) {
        List<V> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
