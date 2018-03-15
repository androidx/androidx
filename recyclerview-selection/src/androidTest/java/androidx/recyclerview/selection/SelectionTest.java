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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SelectionTest {

    private final String[] mIds = new String[] {
            "foo",
            "43",
            "auth|id=@53di*/f3#d"
    };

    private Selection<String> mSelection;

    @Before
    public void setUp() throws Exception {
        mSelection = new Selection<>();
        mSelection.add(mIds[0]);
        mSelection.add(mIds[1]);
        mSelection.add(mIds[2]);
    }

    @Test
    public void testAdd() {
        // We added in setUp.
        assertEquals(3, mSelection.size());
        assertContains(mIds[0]);
        assertContains(mIds[1]);
        assertContains(mIds[2]);
    }

    @Test
    public void testAdd_NewItemReturnsTrue() {
        assertTrue(mSelection.add("poodles"));
    }

    @Test
    public void testAdd_ExistingReturnsFalse() {
        assertFalse(mSelection.add(mIds[0]));
    }

    @Test
    public void testRemove() {
        mSelection.remove(mIds[0]);
        mSelection.remove(mIds[2]);
        assertEquals(1, mSelection.size());
        assertContains(mIds[1]);
    }

    @Test
    public void testRemove_ExistingItemReturnsTrue() {
        assertTrue(mSelection.remove(mIds[0]));
    }

    @Test
    public void testRemove_NewItemReturnsFalse() {
        assertFalse(mSelection.remove("poodles"));
    }

    @Test
    public void testClear() {
        mSelection.clear();
        assertEquals(0, mSelection.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(new Selection<>().isEmpty());
        mSelection.clear();
        assertTrue(mSelection.isEmpty());
    }

    @Test
    public void testSize() {
        Selection<String> other = new Selection<>();
        for (int i = 0; i < mSelection.size(); i++) {
            other.add(mIds[i]);
        }
        assertEquals(mSelection.size(), other.size());
    }

    @Test
    public void testEqualsSelf() {
        assertEquals(mSelection, mSelection);
    }

    @Test
    public void testEqualsOther() {
        Selection<String> other = new Selection<>();
        other.add(mIds[0]);
        other.add(mIds[1]);
        other.add(mIds[2]);
        assertEquals(mSelection, other);
        assertEquals(mSelection.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsCopy() {
        Selection<String> other = new Selection<>();
        other.copyFrom(mSelection);
        assertEquals(mSelection, other);
        assertEquals(mSelection.hashCode(), other.hashCode());
    }

    @Test
    public void testNotEquals() {
        Selection<String> other = new Selection<>();
        other.add("foobar");
        assertFalse(mSelection.equals(other));
    }

    private void assertContains(String id) {
        String err = String.format("Selection %s does not contain %s", mSelection, id);
        assertTrue(err, mSelection.contains(id));
    }

    public static <E> Set<E> newSet(E... elements) {
        HashSet<E> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }
}
