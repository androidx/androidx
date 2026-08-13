/*
 * Copyright 2021 The Android Open Source Project
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.graphics.Typeface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MetadataRepoTest {

    MetadataRepo mMetadataRepo;

    @Before
    public void clearResourceIndex() {
        mMetadataRepo = MetadataRepo.create(mock(Typeface.class), 3);
    }

    @Test(expected = NullPointerException.class)
    public void testPut_withNullMetadata() {
        mMetadataRepo.putForTesting(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPut_withEmptyKeys() {
        mMetadataRepo.putForTesting(new TestTypefaceEmojiRasterizer(new int[0]));
    }

    @Test
    public void testPut_withSingleCodePointMapping() {
        final int[] codePoint = new int[]{1};
        final TestTypefaceEmojiRasterizer metadata = new TestTypefaceEmojiRasterizer(codePoint);
        mMetadataRepo.putForTesting(metadata);
        assertSame(metadata, getNode(codePoint));
    }

    @Test
    public void testPut_withMultiCodePointsMapping() {
        final int[] codePoint = new int[]{1, 2, 3, 4};
        final TestTypefaceEmojiRasterizer metadata = new TestTypefaceEmojiRasterizer(codePoint);
        mMetadataRepo.putForTesting(metadata);
        assertSame(metadata, getNode(codePoint));

        assertNull(getNode(new int[]{1}));
        assertNull(getNode(new int[]{1, 2}));
        assertNull(getNode(new int[]{1, 2, 3}));
        assertNull(getNode(new int[]{1, 2, 3, 5}));
    }

    @Test
    public void testPut_sequentialCodePoints() {
        final int[] codePoint1 = new int[]{1, 2, 3, 4};
        final TypefaceEmojiRasterizer metadata1 = new TestTypefaceEmojiRasterizer(codePoint1);

        final int[] codePoint2 = new int[]{1, 2, 3};
        final TypefaceEmojiRasterizer metadata2 = new TestTypefaceEmojiRasterizer(codePoint2);

        final int[] codePoint3 = new int[]{1, 2};
        final TypefaceEmojiRasterizer metadata3 = new TestTypefaceEmojiRasterizer(codePoint3);

        mMetadataRepo.putForTesting(metadata1);
        mMetadataRepo.putForTesting(metadata2);
        mMetadataRepo.putForTesting(metadata3);

        assertSame(metadata1, getNode(codePoint1));
        assertSame(metadata2, getNode(codePoint2));
        assertSame(metadata3, getNode(codePoint3));

        assertNull(getNode(new int[]{1}));
        assertNull(getNode(new int[]{1, 2, 3, 4, 5}));
    }

    final TypefaceEmojiRasterizer getNode(final int[] codepoints) {
        if (codepoints.length == 0) return null;
        int offset = mMetadataRepo.getRootChildOffset(codepoints[0]);
        if (offset == -1) return null;
        for (int i = 1; i < codepoints.length; i++) {
            offset = mMetadataRepo.getChildOffset(offset, codepoints[i]);
            if (offset == -1) return null;
        }
        return mMetadataRepo.getNodeData(offset);
    }

    @Test
    public void testFlatTrie_plane1DirectLookupBoundary() {
        // Plane 1 range is U+1F300 to U+1FFFF.
        // Test U+1F300 (lower bound)
        final TestTypefaceEmojiRasterizer metadata1 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F300});
        mMetadataRepo.putForTesting(metadata1);
        assertSame(metadata1, getNode(new int[]{0x1F300}));

        // Test U+1FFFF (upper bound)
        final TestTypefaceEmojiRasterizer metadata2 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1FFFF});
        mMetadataRepo.putForTesting(metadata2);
        assertSame(metadata2, getNode(new int[]{0x1FFFF}));

        // Test key outside Plane 1 direct lookup range (lower)
        final TestTypefaceEmojiRasterizer metadata3 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F2FF});
        mMetadataRepo.putForTesting(metadata3);
        assertSame(metadata3, getNode(new int[]{0x1F2FF}));
    }

    @Test
    public void testFlatTrie_plane0DirectLookupBoundary() {
        // Plane 0 range is U+2600 to U+27BF.
        // Test U+2600 (lower bound)
        final TestTypefaceEmojiRasterizer metadata1 =
                new TestTypefaceEmojiRasterizer(new int[]{0x2600});
        mMetadataRepo.putForTesting(metadata1);
        assertSame(metadata1, getNode(new int[]{0x2600}));

        // Test U+27BF (upper bound)
        final TestTypefaceEmojiRasterizer metadata2 =
                new TestTypefaceEmojiRasterizer(new int[]{0x27BF});
        mMetadataRepo.putForTesting(metadata2);
        assertSame(metadata2, getNode(new int[]{0x27BF}));

        // Test key outside Plane 0 direct lookup range (lower)
        final TestTypefaceEmojiRasterizer metadata3 =
                new TestTypefaceEmojiRasterizer(new int[]{0x25FF});
        mMetadataRepo.putForTesting(metadata3);
        assertSame(metadata3, getNode(new int[]{0x25FF}));
    }

    @Test
    public void testFlatTrie_sparseLookup() {
        // Test keys that fall outside Plane 1 and Plane 0 ranges (e.g. U+200D ZWJ,
        // U+FE0F variation selector)
        final TestTypefaceEmojiRasterizer metadata1 =
                new TestTypefaceEmojiRasterizer(new int[]{0x200D});
        final TestTypefaceEmojiRasterizer metadata2 =
                new TestTypefaceEmojiRasterizer(new int[]{0xFE0F});
        mMetadataRepo.putForTesting(metadata1);
        mMetadataRepo.putForTesting(metadata2);

        assertSame(metadata1, getNode(new int[]{0x200D}));
        assertSame(metadata2, getNode(new int[]{0xFE0F}));
    }

    @Test
    public void testFlatTrie_binarySearchCorrectness() {
        // Build a node with multiple children:
        // Root -> U+1F300
        // U+1F300 -> U+1 (child offset idx)
        //         -> U+3
        //         -> U+5
        final TestTypefaceEmojiRasterizer meta1 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F300, 1});
        final TestTypefaceEmojiRasterizer meta2 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F300, 3});
        final TestTypefaceEmojiRasterizer meta3 =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F300, 5});

        mMetadataRepo.putForTesting(meta1);
        mMetadataRepo.putForTesting(meta2);
        mMetadataRepo.putForTesting(meta3);

        int rootOffset = mMetadataRepo.getRootChildOffset(0x1F300);
        org.junit.Assert.assertNotEquals(-1, rootOffset);

        // Match first child (U+1)
        org.junit.Assert.assertNotEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 1));
        assertSame(meta1, getNode(new int[]{0x1F300, 1}));

        // Match middle child (U+3)
        org.junit.Assert.assertNotEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 3));
        assertSame(meta2, getNode(new int[]{0x1F300, 3}));

        // Match last child (U+5)
        org.junit.Assert.assertNotEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 5));
        assertSame(meta3, getNode(new int[]{0x1F300, 5}));

        // Test search misses:
        // Key smaller than first child (U+0)
        org.junit.Assert.assertEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 0));
        // Key between children (U+2)
        org.junit.Assert.assertEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 2));
        // Key larger than last child (U+6)
        org.junit.Assert.assertEquals(-1, mMetadataRepo.getChildOffset(rootOffset, 6));
    }

    @Test
    public void testFlatTrie_lazyInstantiationCache() {
        final TestTypefaceEmojiRasterizer meta =
                new TestTypefaceEmojiRasterizer(new int[]{0x1F300});
        mMetadataRepo.putForTesting(meta);

        int offset = mMetadataRepo.getRootChildOffset(0x1F300);
        org.junit.Assert.assertNotEquals(-1, offset);

        // Query once - triggers creation
        TypefaceEmojiRasterizer rasterizer1 = mMetadataRepo.getNodeData(offset);
        assertSame(meta, rasterizer1);

        // Query again - should return the exact same instance (cache hit)
        TypefaceEmojiRasterizer rasterizer2 = mMetadataRepo.getNodeData(offset);
        assertSame(rasterizer1, rasterizer2);
    }
}
