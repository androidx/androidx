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

package androidx.datastore.core.handlers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.CorruptionHandler

/**
 * A corruption handler that attempts to replace the on-disk data with data from produceNewData.
 *
 * If the handler successfully replaces the data, the original exception is swallowed. If the
 * handler encounters an exception when attempting to replace data, the new exception is added as a
 * suppressed exception to the original exception and the original exception is thrown.
 *
 * @param produceNewData The provided callback returns the data to be written to disk. If the
 *   callback fails, nothing will be written to disk. Since the exception will be swallowed after
 *   writing the data, this is a good place to log the exception.
 */
public actual class ReplaceFileCorruptionHandler<T>
actual constructor(private val produceNewData: (CorruptionException) -> T) : CorruptionHandler<T> {

    actual override suspend fun handleCorruption(ex: CorruptionException): T {
        return produceNewData(ex)
    }
}
