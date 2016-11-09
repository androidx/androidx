/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.provider.tests;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities for tests.
 */
final class TestUtils {
    /**
     * Saves a file from resources to a temporary location and returns a File instance for it.
     *
     * @param id Resource ID
     */
    static File createFileFromResource(Context context, int id) throws IOException {
        final File file = File.createTempFile("android.support.provider.tests{",
                "}.zip", context.getCacheDir());
        try (
            final FileOutputStream outputStream =
                    new ParcelFileDescriptor.AutoCloseOutputStream(
                            ParcelFileDescriptor.open(
                                    file, ParcelFileDescriptor.MODE_WRITE_ONLY));
            final InputStream inputStream = context.getResources().openRawResource(id);
        ) {
            final byte[] buffer = new byte[32 * 1024];
            int bytes;
            while ((bytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytes);
            }
            outputStream.flush();
            return file;
        }
    }
}
