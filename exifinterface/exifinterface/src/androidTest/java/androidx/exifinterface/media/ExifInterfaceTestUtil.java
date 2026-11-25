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

package androidx.exifinterface.media;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import androidx.annotation.RawRes;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Shared helpers for {@link ExifInterface} instrumentation tests. */
final class ExifInterfaceTestUtil {
    private ExifInterfaceTestUtil() {}

    static void copyFromResourceToFile(@RawRes int resourceId, File file) throws IOException {
        try (InputStream inputStream =
                        getApplicationContext().getResources().openRawResource(resourceId);
                FileOutputStream outputStream = new FileOutputStream(file)) {
            ByteStreams.copy(inputStream, outputStream);
        }
    }

    /**
     * Returns the number of times {@code pattern} appears in {@code source}.
     *
     * <p>Overlapping occurrences are counted multiple times, e.g. {@code countOccurrences([0, 1, 0,
     * 1, 0], [0, 1, 0])} will return 2.
     */
    static int countOccurrences(byte[] source, byte[] pattern) {
        int count = 0;
        for (int i = 0; i <= source.length - pattern.length; i++) {
            if (containsAtIndex(source, i, pattern)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if {@code source} contains {@code pattern} starting at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code source.length < index + pattern.length}.
     */
    private static boolean containsAtIndex(byte[] source, int index, byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i] != source[index + i]) {
                return false;
            }
        }
        return true;
    }
}
