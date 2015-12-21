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

package android.support.provider;

import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Collection;

/**
 * Simple static methods to perform common IO operations.
 * @hide
 */
final class IoUtils {
    static void closeQuietly(@Nullable Closeable closeable) {
       if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // Ignore.
            }
        }
    }

    static void closeQuietly(@Nullable InputStream stream) {
       if (stream != null) {
            try {
                stream.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // Ignore.
            }
        }
    }
}
