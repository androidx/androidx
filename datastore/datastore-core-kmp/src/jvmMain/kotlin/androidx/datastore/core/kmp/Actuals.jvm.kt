/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.core.kmp

import androidx.datastore.core.InputStream
import androidx.datastore.core.OutputStream
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source

actual fun InputStream.asBufferedSource(): BufferedSource {
    return if (this is BufferedSource) {
        return this
    } else {
        this.source().buffer()
    }
}

actual  fun OutputStream.asBufferedSink(): BufferedSink {
    return if (this is BufferedSink) {
        this
    } else {
        this.sink().buffer()
    }
}