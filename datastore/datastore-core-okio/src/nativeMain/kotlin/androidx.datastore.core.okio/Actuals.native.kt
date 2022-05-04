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

package androidx.datastore.core.okio

import androidx.datastore.core.InputStream
import androidx.datastore.core.OutputStream
import okio.BufferedSink
import okio.BufferedSource


actual fun InputStream.asBufferedSource(): BufferedSource {
    return this.delegate as BufferedSource
}

actual  fun OutputStream.asBufferedSink(): BufferedSink {
    return this.delegate as BufferedSink
}