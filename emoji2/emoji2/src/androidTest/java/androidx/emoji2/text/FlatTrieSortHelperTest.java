/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.emoji2.text;

import static org.junit.Assert.assertArrayEquals;

import androidx.emoji2.text.flatbuffer.MetadataList;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FlatTrieSortHelperTest {

    @Test
    public void testQuickSort_lexicographicalOrder() {
        // Emojis to sort:
        // Index 0: [1, 2, 3]
        // Index 1: [1, 2]
        // Index 2: [2, 1]
        // Index 3: [1, 3]
        // Index 4: [1, 2, 4]
        //
        // Expected sorted order:
        // 1. [1, 2]     (Index 1)
        // 2. [1, 2, 3]  (Index 0)
        // 3. [1, 2, 4]  (Index 4)
        // 4. [1, 3]     (Index 3)
        // 5. [2, 1]     (Index 2)
        //
        // Expected sorted indices: [1, 0, 4, 3, 2]

        TypefaceEmojiRasterizer[] cache = new TypefaceEmojiRasterizer[] {
                new TestTypefaceEmojiRasterizer(new int[]{1, 2, 3}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2}),
                new TestTypefaceEmojiRasterizer(new int[]{2, 1}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 3}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2, 4})
        };

        int[] indices = new int[]{0, 1, 2, 3, 4};

        MetadataList dummyList = new MetadataList();
        FlatTrieSortHelper.quickSort(indices, dummyList, cache, 0, 4);

        int[] expectedIndices = new int[]{1, 0, 4, 3, 2};
        assertArrayEquals(expectedIndices, indices);
    }

    @Test
    public void testQuickSort_duplicates() {
        // Index 0: [1, 2]
        // Index 1: [1, 2]
        // Index 2: [1]
        // Index 3: [1, 2]
        //
        // Expected sorted order:
        // 1. [1]        (Index 2)
        // 2. [1, 2]     (Index 0 / 1 / 3 - stable sorting not strictly required,
        //               but order is preserved)
        // Expected: [2, (0, 1, 3 in any order)]

        TypefaceEmojiRasterizer[] cache = new TypefaceEmojiRasterizer[] {
                new TestTypefaceEmojiRasterizer(new int[]{1, 2}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2}),
                new TestTypefaceEmojiRasterizer(new int[]{1}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2})
        };

        int[] indices = new int[]{0, 1, 2, 3};

        MetadataList dummyList = new MetadataList();
        FlatTrieSortHelper.quickSort(indices, dummyList, cache, 0, 3);

        // Index 2 ([1]) must be first
        org.junit.Assert.assertEquals(2, indices[0]);
    }

    @Test
    public void testQuickSort_publicOverload() {
        TypefaceEmojiRasterizer[] cache = new TypefaceEmojiRasterizer[] {
                new TestTypefaceEmojiRasterizer(new int[]{1, 2, 3}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2}),
                new TestTypefaceEmojiRasterizer(new int[]{2, 1}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 3}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2, 4})
        };

        int[] indices = new int[]{0, 1, 2, 3, 4};
        MetadataList dummyList = new MetadataList();

        // Use the public overload (no MetadataItem parameters)
        FlatTrieSortHelper.quickSort(indices, dummyList, cache, 0, 4);

        int[] expectedIndices = new int[]{1, 0, 4, 3, 2};
        assertArrayEquals(expectedIndices, indices);
    }
}
