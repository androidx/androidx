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

package android.arch.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class NullPaddedListTest {
    @Test
    public void simple() {
        List<String> data = Arrays.asList("A", "B", "C", "D", "E", "F");
        NullPaddedList<String> list = new NullPaddedList<>(
                2, data.subList(2, 4), 2);

        assertNull(list.get(0));
        assertNull(list.get(1));
        assertSame(data.get(2), list.get(2));
        assertSame(data.get(3), list.get(3));
        assertNull(list.get(4));
        assertNull(list.get(5));

        assertEquals(6, list.size());
        assertEquals(2, list.getLeadingNullCount());
        assertEquals(2, list.getTrailingNullCount());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getEmpty() {
        NullPaddedList<String> list = new NullPaddedList<>(0, new ArrayList<String>(), 0);
        list.get(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNegative() {
        NullPaddedList<String> list = new NullPaddedList<>(0, Arrays.asList("a", "b"), 0);
        list.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getPastEnd() {
        NullPaddedList<String> list = new NullPaddedList<>(0, Arrays.asList("a", "b"), 0);
        list.get(2);
    }
}
