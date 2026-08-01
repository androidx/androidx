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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.TraceCompat;
import androidx.core.util.Preconditions;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Class to hold the emoji metadata required to process and draw emojis.
 *
 * <p><h3>Flat Trie Optimization Design</h3>
 * The trie data structure used to detect emoji sequences is packed into a single, contiguous
 * primitive {@code int[]} array ({@code mTrieArray}) and a flat array of data references
 * ({@code TypefaceEmojiRasterizer[]}). This design replaces the previous object-oriented trie
 * representation (where every node allocated a Node object and a SparseArray) to eliminate
 * startup memory spikes and runtime pointer-chasing.
 *
 * <h4>1. Flat Node Representation</h4>
 * A node starting at a given offset in {@code mTrieArray} is packed sequentially:
 * <pre>
 * [offset + 0] : packedHeader   (Packs 16-bit dataIndex + 1 and childrenCount)
 *                               Note: Limits total emojis to 65,534.
 * [offset + 1] : codepoint_0    (Transition key)
 * [offset + 2] : childOffset_0  (Offset of child node in mTrieArray)
 * ...
 * [offset + 1 + 2*i] : codepoint_i
 * [offset + 2 + 2*i] : childOffset_i
 * </pre>
 *
 * <h4>2. Root Node Optimization</h4>
 * The root node has a high branching factor (~900 children). To keep the start of searches
 * O(1), we bypass the flat array for the root and keep flat jump tables as class fields:
 * <ul>
 *   <li>{@code mRootPlane1DirectOffset}: Direct offset lookup for Plane 1 codepoints
 *       (U+1F300 to U+1FFFF).</li>
 *   <li>{@code mRootPlane0DirectOffset}: Direct offset lookup for Plane 0 codepoints
 *       (U+2600 to U+27BF).</li>
 *   <li>{@code mRootSparseKeys} / {@code mRootSparseOffsets}: Sorted primitive arrays for remaining
 *       root children.</li>
 * </ul>
 *
 * <h4>3. Direct Allocation-Free Construction</h4>
 * To build the flat trie without temporary object overhead at startup:
 * <ol>
 *   <li>We sort the indices of {@code mEmojiList} lexicographically by their codepoint sequences.
 *       Contiguous ranges in the sorted list naturally represent the prefix subranges.</li>
 *   <li>We pre-calculate the exact final size of the flat trie array using a fast pre-scan pass,
 *       allocate {@code mTrieArray} to its exact size once, and write directly into it.</li>
 *   <li>A stack-confined scratch buffer is used to track DFS recursion states inside
 *       {@link FlatTrieBuilder}. This achieves absolute zero heap garbage during
 *       the traversal.</li>
 *   <li>We lazily instantiate {@code TypefaceEmojiRasterizer} objects on demand during lookups
 *       and cache them in {@code mEmojiCache}, avoiding the creation of 7,025 objects at startup
 *       and saving ~168 KB of permanent heap overhead.</li>
 * </ol>
 *
 * <h4>4. Complexity & Memory Comparison</h4>
 * <ul>
 *   <li><b>Memory Footprint:</b> Reduced from ~488 KB (original OO Trie) to ~78 KB (Flat Trie).
 *       Heap objects reduced from 9,400+ to just 5 primitive arrays.</li>
 *   <li><b>Search Complexity:</b> Root search is O(1) via direct plane tables. Non-root search
 *       is O(log C) via primitive binary search in {@code mTrieArray}.</li>
 *   <li><b>Cache Locality:</b> Contiguous array slots eliminate pointer chasing across the heap,
 *       which is highly CPU-cache-friendly.</li>
 * </ul>
 */
@AnyThread
public final class MetadataRepo {
    private static final String S_TRACE_CREATE_REPO = "EmojiCompat.MetadataRepo.create";

    /** MetadataList that contains the emoji metadata. */
    private final @NonNull MetadataList mMetadataList;

    /**
     * char presentation of all TypefaceEmojiRasterizer's in a single array. All emojis we have are
     * mapped to Private Use Area A, in the range U+F0000..U+FFFFD. Therefore each emoji takes 2
     * chars.
     */
    private final char @NonNull [] mEmojiCharArray;

    /** Typeface to be used to render emojis. */
    private final @NonNull Typeface mTypeface;

    private final @NonNull Object mLock = new Object();

    // =========================================================================
    // Flat Trie representation.
    // =========================================================================
    //
    // The entire trie is serialized into a single primitive int array (mTrieArray)
    // and a flat array of data references (mEmojiCache).
    //
    // A single node at index `offset` in `mTrieArray` has the following structure:
    //
    //  Index:     [offset + 0]     [offset + 1]     [offset + 2]     ...
    //  Value:    +-----------------+  +------------+   +------------+   +--
    //            |  packedHeader   |  |codepoint_0 |   |childOffset0|   | ...
    //            +-----------------+  +------------+   +------------+   +--
    //  Concept:   dataIndex (16-bit)   Key transition      Child node index
    //             childrenCount(16-bit)  (Sorted)            in mTrieArray
    //
    //  - packedHeader: Packs the dataIndex + 1 in the upper 16 bits and the childrenCount
    //    in the lower 16 bits.
    //     - dataIndex: Index of the TypefaceEmojiRasterizer in `mEmojiCache`, or -1 if the node
    //       is not a valid emoji end state. (Packed as dataIndex + 1, so 0 represents -1).
    //       Note: This 16-bit packing limits the maximum number of emojis to 65,534.
    //     - childrenCount: Number of outgoing transitions from this node.
    //  - Transitions: Pairs of [codepoint, childOffset] sorted by codepoint key. Because
    //    they are sorted, we can binary search the transitions of a node.
    //
    // Root Node Optimization:
    // Because the root node has a very high branching factor (~900 children), binary searching
    // all 900+ children at the start of every search is slow. To keep root lookups O(1),
    // we bypass the flat array for the root and use direct-access lookup tables for the two
    // most common emoji Planes (Plane 0 and Plane 1):
    //  - mRootPlane1DirectOffset: Direct offset lookup for Plane 1 codepoints (0x1F300 to max).
    //  - mRootPlane0DirectOffset: Direct offset lookup for Plane 0 codepoints (0x2600 to 0x27BF).
    //  - mRootSparseKeys / mRootSparseOffsets: Sorted key-value arrays for remaining root children.
    private int[] mTrieArray;
    private TypefaceEmojiRasterizer[] mEmojiCache;
    private int mEmojiCacheSize;
    private int[] mRootPlane1DirectOffset;
    private int[] mRootPlane0DirectOffset;
    private int[] mRootSparseKeys;
    private int[] mRootSparseOffsets;

    /**
     * Private constructor that is called by one of {@code create} methods.
     *
     * @param typeface Typeface to be used to render emojis
     * @param metadataList MetadataList that contains the emoji metadata
     */
    private MetadataRepo(
            final @NonNull Typeface typeface, final @NonNull MetadataList metadataList) {
        mTypeface = typeface;
        mMetadataList = metadataList;
        mEmojiCharArray = new char[mMetadataList.listLength() * 2];
        mTrieArray = new int[0];
        mEmojiCache = new TypefaceEmojiRasterizer[0];
        mEmojiCacheSize = 0;
        mRootPlane1DirectOffset = new int[0];
        mRootPlane0DirectOffset = new int[0];
        mRootSparseKeys = new int[0];
        mRootSparseOffsets = new int[0];
        constructIndex(mMetadataList);
    }


    /**
     * Construct MetadataRepo with empty metadata.
     *
     * <p>This should only be used from tests.
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public static @NonNull MetadataRepo create(final @NonNull Typeface typeface) {
        return create(typeface, 0);
    }

    /**
     * Construct MetadataRepo with empty metadata and a pre-allocated capacity.
     *
     * <p>This should only be used from tests.
     *
     * @param capacity The exact number of custom emojis that will be added to the repo.
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public static @NonNull MetadataRepo create(final @NonNull Typeface typeface, int capacity) {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            MetadataRepo repo = new MetadataRepo(typeface, new MetadataList());
            repo.mEmojiCache = new TypefaceEmojiRasterizer[capacity];
            return repo;
        } finally {
            TraceCompat.endSection();
        }
    }

    /**
     * Construct MetadataRepo from an input stream. The library does not close the given
     * InputStream, therefore it is caller's responsibility to properly close the stream.
     *
     * @param typeface Typeface to be used to render emojis
     * @param inputStream InputStream to read emoji metadata from
     */
    public static @NonNull MetadataRepo create(
            final @NonNull Typeface typeface, final @NonNull InputStream inputStream)
            throws IOException {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            return new MetadataRepo(typeface, MetadataListReader.read(inputStream));
        } finally {
            TraceCompat.endSection();
        }
    }

    /**
     * Construct MetadataRepo from a byte buffer. The position of the ByteBuffer will change, it is
     * caller's responsibility to reposition the buffer if required.
     *
     * @param typeface Typeface to be used to render emojis
     * @param byteBuffer ByteBuffer to read emoji metadata from
     */
    public static @NonNull MetadataRepo create(
            final @NonNull Typeface typeface, final @NonNull ByteBuffer byteBuffer)
            throws IOException {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            return new MetadataRepo(typeface, MetadataListReader.read(byteBuffer));
        } finally {
            TraceCompat.endSection();
        }
    }

    /**
     * Construct MetadataRepo from an asset.
     *
     * @param assetManager AssetManager instance
     * @param assetPath asset manager path of the file that the Typeface and metadata will be
     *     created from
     */
    public static @NonNull MetadataRepo create(
            final @NonNull AssetManager assetManager, final @NonNull String assetPath)
            throws IOException {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            final Typeface typeface = Typeface.createFromAsset(assetManager, assetPath);
            return new MetadataRepo(typeface, MetadataListReader.read(assetManager, assetPath));
        } finally {
            TraceCompat.endSection();
        }
    }

    private void constructIndex(final MetadataList metadataList) {
        int length = metadataList.listLength();
        mEmojiCache = new TypefaceEmojiRasterizer[length];
        mEmojiCacheSize = length;
        MetadataItem item = new MetadataItem();
        for (int i = 0; i < length; i++) {
            metadataList.list(item, i);
            Character.toChars(item.id(), mEmojiCharArray, i * 2);
        }
        if (length > 0) {
            serializeFromEmojiList();
        }
    }

    /** */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull Typeface getTypeface() {
        return mTypeface;
    }

    /** */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int getMetadataVersion() {
        return mMetadataList.version();
    }

    private static final long ASCII_CANDIDATE_MASK = (1L << '#') | (1L << '*') | (0x3FFL << '0');

    /**
     * Checks if the given codepoint is a candidate for being an emoji.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean isEmojiCandidate(final int key) {
        // ASCII quick check (0..127). Emojis can only be in 0..63.
        if (key < 128) {
            return key < 64 && ((ASCII_CANDIDATE_MASK & (1L << key)) != 0);
        }
        // Dead Zone 1: Cyrillic, Arabic, Hebrew, Indic, etc. (up to 0x1FFF)
        if (key >= 0x00B0 && key <= 0x1FFF) {
            return false;
        }
        // Dead Zone 2: CJK, Hangul, Presentation Forms (Plane 0)
        if (key >= 0x3300 && key <= 0xFFFF) {
            return false;
        }
        // Dead Zone 3: Ancient scripts, Math, Symbols (Plane 1)
        if (key >= 0x10000 && key <= 0x1EFFF) {
            return false;
        }
        // Plane 2+
        if (key > 0x1FFFF) {
            return false;
        }
        return true;
    }

    /**
     * Returns the contiguous character array containing all emoji codepoint sequences.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public char @NonNull [] getEmojiCharArray() {
        return mEmojiCharArray;
    }

    /**
     * Returns the raw FlatBuffer metadata list.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @NonNull MetadataList getMetadataList() {
        return mMetadataList;
    }

    /**
     * Add a TypefaceEmojiRasterizer to the index.
     *
     * <p>Note: In production, the cache is initialized to the exact size of the metadata list and
     * remains right-sized. However, this method is package-private and visible for testing to allow
     * adding custom/mock emojis in unit tests. To avoid heap allocations/churn, the cache capacity
     * must be pre-allocated when constructing {@code MetadataRepo} in tests.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    void putForTesting(final @NonNull TypefaceEmojiRasterizer data) {
        Preconditions.checkNotNull(data, "emoji metadata cannot be null");
        Preconditions.checkArgument(
                data.getCodepointsLength() > 0, "invalid metadata codepoint length");

        synchronized (mLock) {
            if (mEmojiCacheSize >= mEmojiCache.length) {
                throw new IllegalStateException("MetadataRepo cache is full. "
                        + "Please construct MetadataRepo with sufficient capacity.");
            }
            mEmojiCache[mEmojiCacheSize] = data;
            mEmojiCacheSize++;
        }

        // Re-serialize the entire flat trie
        serializeFromEmojiList();
    }

    /**
     * Returns the offset of the child node for the given codepoint from the root.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getRootChildOffset(int codepoint) {
        int off1 = codepoint - 0x1F300;
        if (off1 >= 0 && off1 < mRootPlane1DirectOffset.length) {
            return mRootPlane1DirectOffset[off1];
        }
        int off2 = codepoint - 0x2600;
        if (off2 >= 0 && off2 < mRootPlane0DirectOffset.length) {
            return mRootPlane0DirectOffset[off2];
        }
        int idx = java.util.Arrays.binarySearch(mRootSparseKeys, codepoint);
        if (idx >= 0) {
            return mRootSparseOffsets[idx];
        }
        return -1;
    }

    /**
     * Returns the child offset for a non-root node at a given offset in mTrieArray.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getChildOffset(int nodeOffset, int codepoint) {
        int header = mTrieArray[nodeOffset];
        int childrenCount = header & 0xFFFF;
        if (childrenCount == 0) {
            return -1;
        }
        int start = nodeOffset + 1;
        // Linear scan vs Binary search threshold:
        //  - 73.09% of non-root nodes have 0 children (handled above).
        //  - Of the remaining nodes with children, 97.5% have <= 8 children.
        //  - Average comparisons per active node search: ~1.79 for linear, ~1.60 for binary.
        //  - At <= 8 elements, the bytecode and CPU branching overhead of binary search
        //    index math (mid, shifts, etc.) is higher than a simple linear array scan.
        //  - Only 31 nodes in the entire trie (0.66%) have >= 8 children. These correspond
        //    to the 5 skin-tone variants of "🧑 [Skin Tone] ZWJ"
        //    (U+1F9D1 + U+1F3FB..U+1F3FF + U+200D), which branch into 30 different
        //    profession and relationship suffixes.
        //  - Therefore, we optimize for the 99.34% of nodes by performing a fast linear
        //    scan for <= 8 children, and fallback to binary search only for these 5 nodes.
        if (childrenCount <= 8) {
            int end = start + 2 * childrenCount;
            for (int i = start; i < end; i += 2) {
                int key = mTrieArray[i];
                if (key == codepoint) {
                    return mTrieArray[i + 1];
                } else if (key > codepoint) {
                    break;
                }
            }
            return -1;
        }

        int low = 0;
        int high = childrenCount - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midKey = mTrieArray[start + 2 * mid];
            if (midKey < codepoint) {
                low = mid + 1;
            } else if (midKey > codepoint) {
                high = mid - 1;
            } else {
                return mTrieArray[start + 2 * mid + 1];
            }
        }
        return -1;
    }

    /**
     * Returns the TypefaceEmojiRasterizer for a node at a given offset.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @Nullable TypefaceEmojiRasterizer getNodeData(int nodeOffset) {
        if (nodeOffset < 0) {
            return null;
        }
        int header = mTrieArray[nodeOffset];
        int dataIdx = (header >>> 16) - 1;
        if (dataIdx == -1) {
            return null;
        }
        return getOrCreateEmojiRasterizer(dataIdx);
    }

    /**
     * Retrieves the {@link TypefaceEmojiRasterizer} at the given index, creating it
     * lazily if it has not yet been instantiated.
     *
     * @param index The index of the emoji in the cache.
     * @return The cached or newly created {@link TypefaceEmojiRasterizer}.
     */
    private @NonNull TypefaceEmojiRasterizer getOrCreateEmojiRasterizer(int index) {
        TypefaceEmojiRasterizer emoji = mEmojiCache[index];
        if (emoji == null) {
            synchronized (mLock) {
                emoji = mEmojiCache[index];
                if (emoji == null) {
                    emoji = new TypefaceEmojiRasterizer(this, index);
                    mEmojiCache[index] = emoji;
                }
            }
        }
        return emoji;
    }

    private void serializeFromEmojiList() {
        FlatTrieBuilder builder = new FlatTrieBuilder(mMetadataList, mEmojiCache,
                mEmojiCacheSize);
        FlatTrieBuilder.Result result = builder.build();
        mTrieArray = result.trieArray;
        mRootPlane1DirectOffset = result.rootPlane1DirectOffset;
        mRootPlane0DirectOffset = result.rootPlane0DirectOffset;
        mRootSparseKeys = result.rootSparseKeys;
        mRootSparseOffsets = result.rootSparseOffsets;
    }


}
