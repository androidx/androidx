/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.storage

/** Gets decompressed [ByteArray] from GZIP-compressed bytes. */
expect internal class DecompressedBytes(compressedBytes: ByteArray) {

    /** The first [size] bytes of this contain the decompressed bytes, the rest are zero. */
    val buffer: ByteArray

    /** The size of the initial portion of [buffer] containing the decompressed bytes. */
    val size: Int
}

expect internal fun ByteArray.compress(): ByteArray

@Suppress("AcronymName") // Capitalized for consistency with java.io.IOException.
expect public class IOException : Exception
