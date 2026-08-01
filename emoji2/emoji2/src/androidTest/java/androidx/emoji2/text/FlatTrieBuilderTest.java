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
import static org.junit.Assert.assertEquals;

import androidx.emoji2.text.flatbuffer.MetadataList;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FlatTrieBuilderTest {

    @Test
    public void testBuild_simpleTrie() {
        // Emojis:
        // Index 0: [1]
        // Index 1: [1, 2]
        // Index 2: [2]
        TypefaceEmojiRasterizer[] cache = new TypefaceEmojiRasterizer[] {
                new TestTypefaceEmojiRasterizer(new int[]{1}),
                new TestTypefaceEmojiRasterizer(new int[]{1, 2}),
                new TestTypefaceEmojiRasterizer(new int[]{2})
        };

        MetadataList dummyList = new MetadataList();
        FlatTrieBuilder builder = new FlatTrieBuilder(dummyList, cache, 3);
        FlatTrieBuilder.Result result = builder.build();

        // Expected trie content (compiled from trace analysis):
        // Offset 0: Node [1, 2] -> dataIndex 1, children 0 -> packed: 0x00020000
        // Offset 1: Node [1]    -> dataIndex 0, children 1 -> packed: 0x00010001
        // Offset 2: Transition cp -> 2
        // Offset 3: Transition offset -> 0 (points to Node [1, 2])
        // Offset 4: Node [2]    -> dataIndex 2, children 0 -> packed: 0x00030000
        int[] expectedTrie = new int[] {
                0x00020000,
                0x00010001,
                2,
                0,
                0x00030000
        };
        assertArrayEquals(expectedTrie, result.trieArray);

        // Since codepoints 1 and 2 are not in Plane 0 (0x2600-0x27BF) or Plane 1
        // (0x1F300-0x1FFFF) direct ranges, they must go to sparse tables.
        int[] expectedSparseKeys = new int[] {1, 2};
        int[] expectedSparseOffsets = new int[] {1, 4};
        assertArrayEquals(expectedSparseKeys, result.rootSparseKeys);
        assertArrayEquals(expectedSparseOffsets, result.rootSparseOffsets);

        // Plane 0 should be all -1 (default initialized)
        for (int offset : result.rootPlane0DirectOffset) {
            assertEquals(-1, offset);
        }
        // Plane 1 should be empty
        assertEquals(0, result.rootPlane1DirectOffset.length);
    }
}
