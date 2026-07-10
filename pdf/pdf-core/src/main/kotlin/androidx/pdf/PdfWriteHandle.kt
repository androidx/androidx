/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.IOException

/** Represents a handle for writing the contents of a PDF to a destination. */
public interface PdfWriteHandle : Closeable {
    /**
     * Writes the contents of the PDF to [destination].
     *
     * @param destination The [ParcelFileDescriptor] to write to. The caller is responsible for
     *   closing [destination].
     */
    @Throws(IOException::class) public suspend fun writeTo(destination: ParcelFileDescriptor)
}
