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

package androidx.kruth

/**
 * Clear the stacktrace of a [Throwable]. Unlike [cleanStackTrace], which only cleans "redundant"
 * information, this completely empties the entire stack trace.
 */
internal expect fun Throwable.clearStackTrace()

/**
 *  Cleans the stack trace on the given [Throwable], replacing the original stack trace
 *  stored on the instance (see [Throwable.stackTrace]).
 *
 *  Removes Truth stack frames from the top and JUnit framework and reflective call frames from
 *  the bottom. Collapses the frames for various frameworks in the middle of the trace as well.
 */
internal expect fun Throwable.cleanStackTrace()
