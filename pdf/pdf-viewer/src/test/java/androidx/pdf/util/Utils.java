/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static final int DEFAULT_BLOCK_SIZE = 8192;

    /**
     *
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
        int size = 0;
        int n;
        while ((n = in.read(buffer)) > 0) {
            size += n;
            out.write(buffer, 0, n);
        }
        out.flush();
        return size;
    }

    /**
     *
     */
    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     *
     */
    public static int copyAndClose(InputStream in, OutputStream out) {
        try {
            int size = copy(in, out);
            in.close();
            return size;
        } catch (IOException e) {
            return -1;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }
}
