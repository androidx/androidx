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

package androidx.collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import androidx.arch.core.internal.FastSafeIterableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FastSafeIterableMapTest {
    @Test
    public void testCeil() {
        FastSafeIterableMap<Integer, Boolean> map = new FastSafeIterableMap<>();
        assertThat(map.ceil(1), nullValue());
        map.putIfAbsent(1, false);
        assertThat(map.ceil(1), nullValue());
        map.putIfAbsent(2, false);
        assertThat(map.ceil(2).getKey(), is(1));
        map.remove(1);
        assertThat(map.ceil(2), nullValue());
    }

    @Test
    public void testPut() {
        FastSafeIterableMap<Integer, Integer> map = new FastSafeIterableMap<>();
        map.putIfAbsent(10, 20);
        map.putIfAbsent(20, 40);
        map.putIfAbsent(30, 60);
        assertThat(map.putIfAbsent(5, 10), is((Integer) null));
        assertThat(map.putIfAbsent(10, 30), is(20));
    }

    @Test
    public void testContains() {
        FastSafeIterableMap<Integer, Integer> map = new FastSafeIterableMap<>();
        map.putIfAbsent(10, 20);
        map.putIfAbsent(20, 40);
        map.putIfAbsent(30, 60);
        assertThat(map.contains(10), is(true));
        assertThat(map.contains(11), is(false));
        assertThat(new FastSafeIterableMap<Integer, Integer>().contains(0), is(false));
    }


    @Test
    public void testRemove() {
        FastSafeIterableMap<Integer, Integer> map = new FastSafeIterableMap<>();
        map.putIfAbsent(10, 20);
        map.putIfAbsent(20, 40);
        assertThat(map.contains(10), is(true));
        assertThat(map.contains(20), is(true));
        assertThat(map.remove(10), is(20));
        assertThat(map.contains(10), is(false));
        assertThat(map.putIfAbsent(10, 30), nullValue());
        assertThat(map.putIfAbsent(10, 40), is(30));
    }
}
