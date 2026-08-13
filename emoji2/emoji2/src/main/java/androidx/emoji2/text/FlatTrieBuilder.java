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

import androidx.annotation.RestrictTo;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;

import org.jspecify.annotations.NonNull;

/**
 * Helper class to build the Flat Trie representation from emoji metadata.
 *
 * <p><h3>Flat Trie Layout Diagram</h3>
 * The entire trie is serialized into a single primitive int array ({@code mTrieArray})
 * and a flat array of data references ({@code mEmojiCache}).
 *
 * A single node at index {@code offset} in {@code mTrieArray} has the following structure:
 * <pre>
 *  Index:     [offset + 0]         [offset + 1]         [offset + 2]         ...
 *  Value:    +---------------------+  +---------------------+   +---------------------+
 *            |    packedHeader     |  |    child_entry_0    |   |    child_entry_1    |   ...
 *            +---------------------+  +---------------------+   +---------------------+
 *  Concept:   childrenCount (0-15)     childOffset (0-31)        childOffset (0-31)
 *             dataIndex+1   (16-31)    codepoint   (32-63)       codepoint   (32-63)
 *             compatAdded   (32-47)
 *             isDefault     (48)
 * </pre>
 *
 * <ul>
 *   <li><b>packedHeader</b> (64-bit):
 *       <ul>
 *         <li><b>childrenCount</b> (bits 0-15): Number of outgoing transitions from this node.</li>
 *         <li><b>dataIndex+1</b> (bits 16-31): Index of the
 *             TypefaceEmojiRasterizer in {@code mEmojiCache} plus 1,
 *             or 0 if the node is not a terminal emoji state.</li>
 *         <li><b>compatAdded</b> (bits 32-47): Metadata compat version
 *             required to render this emoji.</li>
 *         <li><b>isDefault</b> (bit 48): Whether the emoji should use
 *             emoji style presentation by default (1 = true).</li>
 *       </ul>
 *   </li>
 *   <li><b>child_entry</b> (64-bit):
 *       <ul>
 *         <li><b>childOffset</b> (bits 0-31): Offset of the child node in {@code mTrieArray}.</li>
 *         <li><b>codepoint</b> (bits 32-63): Codepoint transition key.</li>
 *       </ul>
 *       Because child entries are sorted by codepoint, we can binary
 *       search the transitions of a node.
 *   </li>
 * </ul>
 *
 * <h3>Root Node Optimization</h3>
 * Because the root node has a very high branching factor (~900 children), we bypass the
 * flat array for the root and use direct-access lookup tables for the two most common emoji
 * Planes (Plane 0 and Plane 1) which are returned in the {@link Result}:
 * <ul>
 *   <li>{@code mRootPlane1DirectOffset}: Direct offset lookup for Plane 1 codepoints
 *       (U+1F300 to max).</li>
 *   <li>{@code mRootPlane0DirectOffset}: Direct offset lookup for Plane 0 codepoints
 *       (U+2600 to 0x27BF).</li>
 *   <li>{@code mRootSparseKeys} / {@code mRootSparseOffsets}: Sorted key-value arrays for remaining
 *       root children.</li>
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class FlatTrieBuilder {

    static final class Result {
        public final long @NonNull [] trieArray;
        public final int @NonNull [] rootPlane1DirectOffset;
        public final int @NonNull [] rootPlane0DirectOffset;
        public final int @NonNull [] rootSparseKeys;
        public final int @NonNull [] rootSparseOffsets;

        Result(long @NonNull [] trieArray, int @NonNull [] rootPlane1DirectOffset,
                int @NonNull [] rootPlane0DirectOffset, int @NonNull [] rootSparseKeys,
                int @NonNull [] rootSparseOffsets) {
            this.trieArray = trieArray;
            this.rootPlane1DirectOffset = rootPlane1DirectOffset;
            this.rootPlane0DirectOffset = rootPlane0DirectOffset;
            this.rootSparseKeys = rootSparseKeys;
            this.rootSparseOffsets = rootSparseOffsets;
        }
    }

    private static final class IntStack {
        int[] mArray = new int[1024];
        void ensureCapacity(int requiredSize) {
            if (requiredSize > mArray.length) {
                int newCapacity = Math.max(mArray.length * 2, requiredSize);
                mArray = java.util.Arrays.copyOf(mArray, newCapacity);
            }
        }
    }

    private final @NonNull MetadataList mMetadataList;
    private final TypefaceEmojiRasterizer @NonNull [] mEmojiCache;
    private final int mEmojiCacheSize;

    // Build session state
    private long[] mTrieArray;
    private int mTrieSize;

    FlatTrieBuilder(@NonNull MetadataList metadataList,
            TypefaceEmojiRasterizer @NonNull [] emojiCache, int emojiCacheSize) {
        mMetadataList = metadataList;
        mEmojiCache = emojiCache;
        mEmojiCacheSize = emojiCacheSize;
    }

    /**
     * Builds the flat trie arrays and root direct/sparse tables.
     *
     * @return A {@link Result} containing the serialized flat trie and root tables.
     */
    @NonNull Result build() {
        int length = mEmojiCacheSize;
        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        MetadataItem serItem = new MetadataItem();
        FlatTrieSortHelper.quickSort(indices, mMetadataList, mEmojiCache, 0, length - 1);

        // Pre-scan to calculate exact trie array size and root children count
        int exactSize = 0;
        int rootChildrenCount = 0;
        int scan = 0;
        while (scan < length) {
            int cp = getCodepoint(indices[scan], 0, serItem);
            rootChildrenCount++;
            int childStart = scan;
            while (scan < length && getCodepoint(indices[scan], 0, serItem) == cp) {
                scan++;
            }
            exactSize += countTrieSize(indices, childStart, scan, 1, serItem);
        }

        mTrieArray = new long[exactSize];
        mTrieSize = 0;

        return buildFlatTrie(indices, serItem, rootChildrenCount);
    }

    private int getCodepoint(int emojiIndex, int codepointIndex, MetadataItem item) {
        if (emojiIndex < mEmojiCacheSize && mEmojiCache[emojiIndex] != null) {
            return mEmojiCache[emojiIndex].getCodepointAt(codepointIndex);
        }
        mMetadataList.list(item, emojiIndex);
        return item.codepoints(codepointIndex);
    }

    private int getCodepointsLength(int emojiIndex, MetadataItem item) {
        if (emojiIndex < mEmojiCacheSize && mEmojiCache[emojiIndex] != null) {
            return mEmojiCache[emojiIndex].getCodepointsLength();
        }
        mMetadataList.list(item, emojiIndex);
        return item.codepointsLength();
    }

    /**
     * Recursively counts the exact number of integers required to serialize the trie
     * segment represented by the sorted indices in the range [start, end) at the given depth.
     * This is used to pre-allocate the flat trie array to its exact size.
     *
     * @param sortedIndices Lexicographically sorted list of emoji indices.
     * @param start The starting index (inclusive) of this node's children in sortedIndices.
     * @param end The ending index (exclusive) of this node's children in sortedIndices.
     * @param depth The current codepoint depth of the trie node.
     * @param item Reusable MetadataItem to avoid allocations.
     * @return The number of longs needed to pack this node and all of its descendants.
     */
    private int countTrieSize(int[] sortedIndices, int start, int end, int depth,
            MetadataItem item) {
        int size = 1; // 1 for this node's header
        if (getCodepointsLength(sortedIndices[start], item) == depth) {
            start++;
        }
        if (start >= end) {
            return size; // Leaf node, no children
        }

        int scan = start;
        while (scan < end) {
            int cp = getCodepoint(sortedIndices[scan], depth, item);
            int childStart = scan;
            size += 1; // 1 long per child transition
            while (scan < end && getCodepoint(sortedIndices[scan], depth, item) == cp) {
                scan++;
            }
            size += countTrieSize(sortedIndices, childStart, scan, depth + 1, item);
        }
        return size;
    }

    /**
     * Builds the flat trie into the pre-allocated {@code mTrieArray} from the sorted
     * list of emoji indices. This initiates the serialization by traversing the root
     * children and serializing each subtree recursively.
     *
     * @param sortedIndices Lexicographically sorted list of emoji indices.
     * @param item Reusable MetadataItem to avoid allocations.
     * @param rootChildrenCount Number of children the root node has.
     * @return A {@link Result} containing the built flat trie structures.
     */
    private @NonNull Result buildFlatTrie(int[] sortedIndices, MetadataItem item,
            int rootChildrenCount) {
        int length = sortedIndices.length;
        if (length == 0) {
            return new Result(new long[0], new int[0], new int[0], new int[0], new int[0]);
        }

        int[] rootCps = new int[rootChildrenCount];
        int[] rootOffsets = new int[rootChildrenCount];
        IntStack scratchStack = new IntStack();

        int idx = 0;
        int scan = 0;
        while (scan < length) {
            int cp = getCodepoint(sortedIndices[scan], 0, item);
            rootCps[idx] = cp;
            int childStart = scan;
            while (scan < length && getCodepoint(sortedIndices[scan], 0, item) == cp) {
                scan++;
            }
            int childEnd = scan;
            rootOffsets[idx] = serializeNode(sortedIndices, childStart, childEnd, 1, 0, item,
                    scratchStack);
            idx++;
        }

        return buildRootTables(rootCps, rootOffsets);
    }

    /**
     * Recursively serializes a trie node and all of its descendants.
     * This packs the node data (data index and children transitions) into {@code mTrieArray}
     * at the current {@code mTrieSize} offset, and returns the node's starting offset.
     *
     * <p><b>Scratch Stack Layout (AoS):</b>
     * The temporary recursion stack ({@code scratchStack}) uses an interleaved layout where
     * each child entry takes exactly 4 slots:
     * <pre>
     * [entryOffset + 0] : childCp      (Codepoint transition key)
     * [entryOffset + 1] : childStart   (Start index in sortedIndices)
     * [entryOffset + 2] : childEnd     (End index in sortedIndices)
     * [entryOffset + 3] : childOffset  (Serialized offset of child in mTrieArray)
     * </pre>
     *
     * @param sortedIndices Lexicographically sorted list of emoji indices.
     * @param start The starting index (inclusive) of this node's children in sortedIndices.
     * @param end The ending index (exclusive) of this node's children in sortedIndices.
     * @param depth The current codepoint depth of the trie node.
     * @param stackPointer The current pointer in the temporary recursion stack.
     * @param item Reusable MetadataItem to avoid allocations.
     * @param scratchStack Flat stack used to track DFS state.
     * @return The offset in {@code mTrieArray} where this node is serialized.
     */
    private int serializeNode(int[] sortedIndices, int start, int end, int depth,
            int stackPointer, MetadataItem item, @NonNull IntStack scratchStack) {
        int dataIndex = -1;
        if (getCodepointsLength(sortedIndices[start], item) == depth) {
            dataIndex = sortedIndices[start];
            start++;
        }

        int childrenCount = 0;
        int scan = start;
        while (scan < end) {
            int cp = getCodepoint(sortedIndices[scan], depth, item);
            int entryOffset = stackPointer + childrenCount * 4;
            scratchStack.ensureCapacity(entryOffset + 4);

            scratchStack.mArray[entryOffset + 0] = cp;   // childCp
            scratchStack.mArray[entryOffset + 1] = scan; // childStart

            while (scan < end && getCodepoint(sortedIndices[scan], depth, item) == cp) {
                scan++;
            }
            scratchStack.mArray[entryOffset + 2] = scan; // childEnd
            childrenCount++;
        }

        int nextStackPointer = stackPointer + childrenCount * 4;
        for (int i = 0; i < childrenCount; i++) {
            int entryOffset = stackPointer + i * 4;
            int childStart = scratchStack.mArray[entryOffset + 1];
            int childEnd = scratchStack.mArray[entryOffset + 2];
            int childOffset = serializeNode(sortedIndices, childStart, childEnd, depth + 1,
                    nextStackPointer, item, scratchStack);
            scratchStack.mArray[entryOffset + 3] = childOffset; // Store the serialized offset
        }

        if (dataIndex >= 0xFFFF - 1) {
            throw new IllegalStateException("MetadataRepo dataIndex overflow: " + dataIndex
                    + ". Flat Trie serialization supports at most 65,534 emojis.");
        }
        int nodeOffset = mTrieSize;
        long packedHeader = (((long) (dataIndex + 1)) << 16) | (childrenCount & 0xFFFF);
        if (dataIndex >= 0) {
            long compatAdded = getCompatAdded(dataIndex, item);
            long isDefault = isDefaultEmoji(dataIndex, item) ? 1L : 0L;
            packedHeader |= ((compatAdded & 0xFFFFL) << 32);
            packedHeader |= (isDefault << 48);
        }
        writeLong(packedHeader);
        for (int i = 0; i < childrenCount; i++) {
            int entryOffset = stackPointer + i * 4;
            long cp = scratchStack.mArray[entryOffset + 0];
            long offset = scratchStack.mArray[entryOffset + 3];
            long entry = (cp << 32) | (offset & 0xFFFFFFFFL);
            writeLong(entry);
        }
        return nodeOffset;
    }

    private void writeLong(long val) {
        mTrieArray[mTrieSize++] = val;
    }

    /**
     * Populates the root lookup structures (direct tables for Plane 0 and Plane 1,
     * and sparse tables for other planes) from the root children.
     *
     * @param rootCps The codepoints of the root children.
     * @param rootOffsets The offset in {@code mTrieArray} where each root child is serialized.
     * @return A {@link Result} containing the populated arrays.
     */
    private @NonNull Result buildRootTables(int[] rootCps, int[] rootOffsets) {
        int rootChildrenCount = rootCps.length;
        int maxPlane1Key = 0;
        for (int cp : rootCps) {
            if (cp >= 0x1F300 && cp <= 0x1FFFF) {
                maxPlane1Key = Math.max(maxPlane1Key, cp);
            }
        }

        int plane1Size = 0;
        if (maxPlane1Key >= 0x1F300) {
            plane1Size = maxPlane1Key - 0x1F300 + 1;
        }
        int[] rootPlane1DirectOffset = new int[plane1Size];
        java.util.Arrays.fill(rootPlane1DirectOffset, -1);

        int[] rootPlane0DirectOffset = new int[448];
        java.util.Arrays.fill(rootPlane0DirectOffset, -1);

        int sparseCount = 0;
        for (int cp : rootCps) {
            int off1 = cp - 0x1F300;
            boolean isP1 = (off1 >= 0 && off1 < rootPlane1DirectOffset.length);
            int off2 = cp - 0x2600;
            boolean isP0 = (off2 >= 0 && off2 < rootPlane0DirectOffset.length);
            if (!isP1 && !isP0) {
                sparseCount++;
            }
        }

        int[] rootSparseKeys = new int[sparseCount];
        int[] rootSparseOffsets = new int[sparseCount];

        int sIdx = 0;
        for (int i = 0; i < rootChildrenCount; i++) {
            int cp = rootCps[i];
            int offset = rootOffsets[i];

            int off1 = cp - 0x1F300;
            if (off1 >= 0 && off1 < rootPlane1DirectOffset.length) {
                rootPlane1DirectOffset[off1] = offset;
                continue;
            }
            int off2 = cp - 0x2600;
            if (off2 >= 0 && off2 < rootPlane0DirectOffset.length) {
                rootPlane0DirectOffset[off2] = offset;
                continue;
            }
            rootSparseKeys[sIdx] = cp;
            rootSparseOffsets[sIdx] = offset;
            sIdx++;
        }

        return new Result(mTrieArray, rootPlane1DirectOffset, rootPlane0DirectOffset,
                rootSparseKeys, rootSparseOffsets);
    }

    private short getCompatAdded(int emojiIndex, MetadataItem item) {
        if (emojiIndex < mEmojiCacheSize && mEmojiCache[emojiIndex] != null) {
            return mEmojiCache[emojiIndex].getCompatAdded();
        }
        mMetadataList.list(item, emojiIndex);
        return item.compatAdded();
    }

    private boolean isDefaultEmoji(int emojiIndex, MetadataItem item) {
        if (emojiIndex < mEmojiCacheSize && mEmojiCache[emojiIndex] != null) {
            return mEmojiCache[emojiIndex].isDefaultEmoji();
        }
        mMetadataList.list(item, emojiIndex);
        return item.emojiStyle();
    }
}
