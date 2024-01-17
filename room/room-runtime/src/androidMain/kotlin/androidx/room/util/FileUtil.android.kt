/*
 * Copyright 2019 The Android Open Source Project
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
@file:JvmName("FileUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RestrictTo
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel

/**
 * Copies data from the input channel to the output file channel.
 *
 * @param input  the input channel to copy.
 * @param output the output channel to copy.
 * @throws IOException if there is an I/O error.
 */
@SuppressLint("LambdaLast")
@Throws(IOException::class)
fun copy(input: ReadableByteChannel, output: FileChannel) {
    try {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            output.transferFrom(input, 0, Long.MAX_VALUE)
        } else {
            val inputStream = Channels.newInputStream(input)
            val outputStream = Channels.newOutputStream(output)
            var length: Int
            val buffer = ByteArray(1024 * 4)

            // TODO: Use Kotlin stdlib IO APIs
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        }
        output.force(false)
    } finally {
        input.close()
        output.close()
    }
}
