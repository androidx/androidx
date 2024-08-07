/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.fail;

import android.os.Build;
import android.util.SparseArray;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Iterator;
import java.util.NoSuchElementException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class CollectUtilsTest {

    @Test
    public void testSparseArrayIterableEmpty() {
        SparseArray<String> array = new SparseArray<String>(0);
        Iterable<String> iterable = CollectUtils.asIterable(array);
        assertThat(iterable.iterator().hasNext()).isFalse();
        try {
            iterable.iterator().next();
            fail("next() should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // okay!
        }

        for (String s : iterable) {
            fail("Shouldn't get a hit on empty array " + s);
        }
    }

    @Test
    public void testSparseArrayIterableWithCapacity() {
        SparseArray<String> array = new SparseArray<String>(10);
        array.put(120, "One hundred twenty");
        Iterable<String> iterable = CollectUtils.asIterable(array);
        Iterator<String> iterator = iterable.iterator();
        assertThat(iterator.hasNext()).isTrue();
        iterator.next();
        assertThat(iterator.hasNext()).isFalse();
        try {
            iterator.next();
            fail("next() should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // okay!
        }

        int i = 0;
        for (String s : iterable) {
            if (i++ > 0) {
                fail("Shouldn't get a hit on empty array " + s);
            }
        }
    }

    @Test
    public void testRemove() {
        SparseArray<String> array = new SparseArray<String>();
        array.put(120, "One hundred twenty");
        array.put(124, "Two hundred twenty four");
        array.put(128, "One hundred twenty eight");
        Iterable<String> iterable = CollectUtils.asIterable(array);

        for (Iterator<String> iterator = iterable.iterator(); iterator.hasNext(); ) {
            String s = iterator.next();
            if (s.endsWith("four")) {
                iterator.remove();
            }
        }

        assertThat(array.size()).isEqualTo(2);
        for (String s : iterable) {
            assertWithMessage("Wrong item not removed: " + s).that(s.startsWith("One")).isTrue();
        }
    }
}
