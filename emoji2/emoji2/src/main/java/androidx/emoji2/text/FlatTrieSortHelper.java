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
 * Helper class to sort emoji indices during Flat Trie serialization without boxing primitives.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class FlatTrieSortHelper {

    private FlatTrieSortHelper() {
        // utility class
    }

    /**
     * Standard quicksort implementation for primitive {@code int[]} index arrays.
     */
    static void quickSort(int[] indices, @NonNull MetadataList metadataList,
            TypefaceEmojiRasterizer @NonNull [] cache, int left, int right) {
        MetadataItem item1 = new MetadataItem();
        MetadataItem item2 = new MetadataItem();
        quickSort(indices, metadataList, cache, item1, item2, left, right);
    }

    private static void quickSort(int[] indices, @NonNull MetadataList metadataList,
            TypefaceEmojiRasterizer @NonNull [] cache, @NonNull MetadataItem item1,
            @NonNull MetadataItem item2, int left, int right) {
        if (left >= right) return;
        int pivotIdx = partition(indices, metadataList, cache, item1, item2, left, right);
        quickSort(indices, metadataList, cache, item1, item2, left, pivotIdx - 1);
        quickSort(indices, metadataList, cache, item1, item2, pivotIdx + 1, right);
    }

    private static int partition(int[] indices, @NonNull MetadataList metadataList,
            TypefaceEmojiRasterizer @NonNull [] cache, @NonNull MetadataItem item1,
            @NonNull MetadataItem item2, int left, int right) {
        int mid = (left + right) >>> 1;
        if (compareEmoji(indices[mid], indices[left], metadataList, cache, item1, item2) < 0) {
            swap(indices, left, mid);
        }
        if (compareEmoji(indices[right], indices[left], metadataList, cache, item1, item2) < 0) {
            swap(indices, left, right);
        }
        if (compareEmoji(indices[right], indices[mid], metadataList, cache, item1, item2) < 0) {
            swap(indices, mid, right);
        }
        swap(indices, mid, right);

        int pivot = indices[right];
        int i = left - 1;
        for (int j = left; j < right; j++) {
            if (compareEmoji(indices[j], pivot, metadataList, cache, item1, item2) < 0) {
                i++;
                swap(indices, i, j);
            }
        }
        swap(indices, i + 1, right);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int t = arr[i];
        arr[i] = arr[j];
        arr[j] = t;
    }

    private static int compareEmoji(int idx1, int idx2, @NonNull MetadataList metadataList,
            TypefaceEmojiRasterizer @NonNull [] cache, @NonNull MetadataItem item1,
            @NonNull MetadataItem item2) {
        int len1, len2;
        boolean useCache1 = (idx1 < cache.length && cache[idx1] != null);
        boolean useCache2 = (idx2 < cache.length && cache[idx2] != null);

        if (useCache1) {
            len1 = cache[idx1].getCodepointsLength();
        } else {
            metadataList.list(item1, idx1);
            len1 = item1.codepointsLength();
        }

        if (useCache2) {
            len2 = cache[idx2].getCodepointsLength();
        } else {
            metadataList.list(item2, idx2);
            len2 = item2.codepointsLength();
        }

        int minLen = Math.min(len1, len2);
        for (int i = 0; i < minLen; i++) {
            int cp1 = useCache1 ? cache[idx1].getCodepointAt(i) : item1.codepoints(i);
            int cp2 = useCache2 ? cache[idx2].getCodepointAt(i) : item2.codepoints(i);
            if (cp1 != cp2) {
                return cp1 < cp2 ? -1 : 1;
            }
        }
        return Integer.compare(len1, len2);
    }
}
