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
import android.util.SparseArray;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.TraceCompat;
import androidx.core.util.Preconditions;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Class to hold the emoji metadata required to process and draw emojis.
 */
@AnyThread
public final class MetadataRepo {
    private static final String S_TRACE_CREATE_REPO = "EmojiCompat.MetadataRepo.create";

    /**
     * MetadataList that contains the emoji metadata.
     */
    private final @NonNull MetadataList mMetadataList;

    /**
     * char presentation of all TypefaceEmojiRasterizer's in a single array. All emojis we have are
     * mapped to Private Use Area A, in the range U+F0000..U+FFFFD. Therefore each emoji takes 2
     * chars.
     */
    private final char @NonNull [] mEmojiCharArray;

    /**
     * Empty root node of the trie.
     */
    private final @NonNull Node mRootNode;

    /**
     * Typeface to be used to render emojis.
     */
    private final @NonNull Typeface mTypeface;

    /**
     * Private constructor that is called by one of {@code create} methods.
     *
     * @param typeface Typeface to be used to render emojis
     * @param metadataList MetadataList that contains the emoji metadata
     */
    private MetadataRepo(final @NonNull Typeface typeface,
            final @NonNull MetadataList metadataList) {
        mTypeface = typeface;
        mMetadataList = metadataList;
        mRootNode = new RootNode(findMaxPlane1Key(metadataList));
        mEmojiCharArray = new char[mMetadataList.listLength() * 2];
        constructIndex(mMetadataList);
    }

    private static int findMaxPlane1Key(final MetadataList metadataList) {
        int length = metadataList.listLength();
        if (length == 0) {
            return 0;
        }
        int maxKey = 0;
        final MetadataItem item = new MetadataItem();
        for (int i = 0; i < length; i++) {
            metadataList.list(item, i);
            if (item.codepointsLength() > 0) {
                int firstCodepoint = item.codepoints(0);
                if (firstCodepoint >= 0x1F300 && firstCodepoint <= 0x1FFFF) {
                    if (firstCodepoint > maxKey) {
                        maxKey = firstCodepoint;
                    }
                }
            }
        }
        return maxKey;
    }

    /**
     * Construct MetadataRepo with empty metadata.
     *
     * This should only be used from tests.
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public static @NonNull MetadataRepo create(final @NonNull Typeface typeface) {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            return new MetadataRepo(typeface, new MetadataList());
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
    public static @NonNull MetadataRepo create(final @NonNull Typeface typeface,
            final @NonNull InputStream inputStream) throws IOException {
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
    public static @NonNull MetadataRepo create(final @NonNull Typeface typeface,
            final @NonNull ByteBuffer byteBuffer) throws IOException {
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
     *                  created from
     */
    public static @NonNull MetadataRepo create(final @NonNull AssetManager assetManager,
            final @NonNull String assetPath) throws IOException {
        try {
            TraceCompat.beginSection(S_TRACE_CREATE_REPO);
            final Typeface typeface = Typeface.createFromAsset(assetManager, assetPath);
            return new MetadataRepo(typeface,
                    MetadataListReader.read(assetManager, assetPath));
        } finally {
            TraceCompat.endSection();
        }
    }

    /**
     * Read emoji metadata list and construct the trie.
     */
    private void constructIndex(final MetadataList metadataList) {
        int length = metadataList.listLength();
        for (int i = 0; i < length; i++) {
            final TypefaceEmojiRasterizer metadata = new TypefaceEmojiRasterizer(this, i);
            //since all emojis are mapped to a single codepoint in Private Use Area A they are 2
            //chars wide
            //noinspection ResultOfMethodCallIgnored
            Character.toChars(metadata.getId(), mEmojiCharArray, i * 2);
            put(metadata);
        }
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull Typeface getTypeface() {
        return mTypeface;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int getMetadataVersion() {
        return mMetadataList.version();
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull Node getRootNode() {
        return mRootNode;
    }

    private static final long ASCII_CANDIDATE_MASK = (1L << '#') | (1L << '*') | (0x3FFL << '0');

    /**
     * Checks if the given codepoint is a candidate for being an emoji.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean isEmojiCandidate(final int key) {
        if (key < 128) {
            return key < 64 && ((ASCII_CANDIDATE_MASK & (1L << key)) != 0);
        }
        if (key >= 0x00B0 && key <= 0x1FFF) {
            return false;
        }
        if (key >= 0x3300 && key <= 0xFFFF) {
            return false;
        }
        if (key >= 0x10000 && key <= 0x1EFFF) {
            return false;
        }
        if (key > 0x1FFFF) {
            return false;
        }
        return true;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public char @NonNull [] getEmojiCharArray() {
        return mEmojiCharArray;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @NonNull MetadataList getMetadataList() {
        return mMetadataList;
    }

    /**
     * Add a TypefaceEmojiRasterizer to the index.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    void put(final @NonNull TypefaceEmojiRasterizer data) {
        Preconditions.checkNotNull(data, "emoji metadata cannot be null");
        Preconditions.checkArgument(data.getCodepointsLength() > 0,
                "invalid metadata codepoint length");

        mRootNode.put(data, 0, data.getCodepointsLength() - 1);
    }

    /**
     * Trie node that holds mapping from emoji codepoint(s) to TypefaceEmojiRasterizer.
     *
     * A single codepoint emoji is represented by a child of the root node.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static class Node {
        private final SparseArray<Node> mChildren;
        private TypefaceEmojiRasterizer mData;

        private Node() {
            this(1);
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Node(final int defaultChildrenSize) {
            mChildren = new SparseArray<>(defaultChildrenSize);
        }

        Node get(final int key) {
            return mChildren == null ? null : mChildren.get(key);
        }

        final TypefaceEmojiRasterizer getData() {
            return mData;
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        void put(final @NonNull TypefaceEmojiRasterizer data, final int start, final int end) {
            Node node = get(data.getCodepointAt(start));
            if (node == null) {
                node = new Node();
                mChildren.put(data.getCodepointAt(start), node);
            }

            if (end > start) {
                node.put(data, start + 1, end);
            } else {
                node.mData = data;
            }
        }
    }

    static class RootNode extends Node {
        private final Node[] mPlane1Direct;
        private final int mPlane1Min = 0x1F300;
        private final Node[] mPlane0Direct = new Node[448];
        private final int mPlane0Min = 0x2600;
        private final SparseArray<Node> mSparseDirect;

        RootNode(int maxPlane1Key) {
            super(0);
            mSparseDirect = new SparseArray<>(256);
            int plane1Size = 0;
            if (maxPlane1Key >= mPlane1Min) {
                plane1Size = Math.min(maxPlane1Key, 0x1FFFF) - mPlane1Min + 1;
            }
            mPlane1Direct = new Node[plane1Size];
        }

        @Override
        Node get(final int key) {
            int off1 = key - mPlane1Min;
            if (off1 >= 0 && off1 < mPlane1Direct.length) {
                return mPlane1Direct[off1];
            }
            int off2 = key - mPlane0Min;
            if (off2 >= 0 && off2 < 448) {
                return mPlane0Direct[off2];
            }
            return mSparseDirect.get(key);
        }

        @Override
        void put(final @NonNull TypefaceEmojiRasterizer data, final int start, final int end) {
            int key = data.getCodepointAt(start);
            Node node = get(key);
            if (node == null) {
                node = new Node();
                int off1 = key - mPlane1Min;
                if (off1 >= 0 && off1 < mPlane1Direct.length) {
                    mPlane1Direct[off1] = node;
                } else {
                    int off2 = key - mPlane0Min;
                    if (off2 >= 0 && off2 < 448) {
                        mPlane0Direct[off2] = node;
                    } else {
                        mSparseDirect.put(key, node);
                    }
                }
            }

            if (end > start) {
                node.put(data, start + 1, end);
            } else {
                node.mData = data;
            }
        }
    }
}
