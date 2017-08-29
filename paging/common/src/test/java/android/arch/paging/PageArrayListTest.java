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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class PageArrayListTest {
    @Test
    public void simple() {
        List<String> data = Arrays.asList("A", "B", "C", "D", "E", "F");
        PageArrayList<String> list = new PageArrayList<>(2, data.size());

        assertEquals(2, list.mPageSize);
        assertEquals(data.size(), list.size());
        assertEquals(3, list.mMaxPageCount);

        for (int i = 0; i < data.size(); i++) {
            assertEquals(null, list.get(i));
        }
        for (int i = 0; i < data.size(); i += list.mPageSize) {
            list.mPages.add(data.subList(i, i + 2));
        }
        for (int i = 0; i < data.size(); i++) {
            assertEquals(data.get(i), list.get(i));
        }
    }
}
