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
package androidx.emoji.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class MetadataRepoTest {

    MetadataRepo mMetadataRepo;

    @Before
    public void clearResourceIndex() {
        mMetadataRepo = new MetadataRepo();
    }

    @Test(expected = NullPointerException.class)
    public void testPut_withNullMetadata() {
        mMetadataRepo.put(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPut_withEmptyKeys() {
        mMetadataRepo.put(new TestEmojiMetadata(new int[0]));
    }

    @Test
    public void testPut_withSingleCodePointMapping() {
        final int[] codePoint = new int[]{1};
        final TestEmojiMetadata metadata = new TestEmojiMetadata(codePoint);
        mMetadataRepo.put(metadata);
        assertSame(metadata, getNode(codePoint));
    }

    @Test
    public void testPut_withMultiCodePointsMapping() {
        final int[] codePoint = new int[]{1, 2, 3, 4};
        final TestEmojiMetadata metadata = new TestEmojiMetadata(codePoint);
        mMetadataRepo.put(metadata);
        assertSame(metadata, getNode(codePoint));

        assertEquals(null, getNode(new int[]{1}));
        assertEquals(null, getNode(new int[]{1, 2}));
        assertEquals(null, getNode(new int[]{1, 2, 3}));
        assertEquals(null, getNode(new int[]{1, 2, 3, 5}));
    }

    @Test
    public void testPut_sequentialCodePoints() {
        final int[] codePoint1 = new int[]{1, 2, 3, 4};
        final EmojiMetadata metadata1 = new TestEmojiMetadata(codePoint1);

        final int[] codePoint2 = new int[]{1, 2, 3};
        final EmojiMetadata metadata2 = new TestEmojiMetadata(codePoint2);

        final int[] codePoint3 = new int[]{1, 2};
        final EmojiMetadata metadata3 = new TestEmojiMetadata(codePoint3);

        mMetadataRepo.put(metadata1);
        mMetadataRepo.put(metadata2);
        mMetadataRepo.put(metadata3);

        assertSame(metadata1, getNode(codePoint1));
        assertSame(metadata2, getNode(codePoint2));
        assertSame(metadata3, getNode(codePoint3));

        assertEquals(null, getNode(new int[]{1}));
        assertEquals(null, getNode(new int[]{1, 2, 3, 4, 5}));
    }

    final EmojiMetadata getNode(final int[] codepoints) {
        return getNode(mMetadataRepo.getRootNode(), codepoints, 0);
    }

    final EmojiMetadata getNode(MetadataRepo.Node node, final int[] codepoints, int start) {
        if (codepoints.length < start) return null;
        if (codepoints.length == start) return node.getData();

        final MetadataRepo.Node childNode = node.get(codepoints[start]);
        if (childNode == null) return null;
        return getNode(childNode, codepoints, start + 1);
    }
}
