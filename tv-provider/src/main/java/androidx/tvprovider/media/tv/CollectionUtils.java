/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.tvprovider.media.tv;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.RestrictTo;

import java.util.Arrays;

/**
 * Static utilities for collections
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class CollectionUtils {
    /**
     * Returns an array with the arrays concatenated together.
     *
     * @see <a href="http://stackoverflow.com/a/784842/1122089">Stackoverflow answer</a> by
     *      <a href="http://stackoverflow.com/users/40342/joachim-sauer">Joachim Sauer</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private CollectionUtils() {
    }
}
