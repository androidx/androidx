/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.core

import kotlinx.io.IOException

/**
 * A subclass of IOException that indicates that the file could not be de-serialized due
 * to data format corruption. This exception should not be thrown when the IOException is
 * due to a transient IO issue or permissions issue.
 */
public class CorruptionException(message: String, cause: Throwable? = null) :
    IOException(message, cause)