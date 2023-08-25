/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

/**
 * Copies characters from this [CharSequence] into [destination].
 *
 * Platform-specific implementations should use native functions for performing this operation if
 * they exist, since they will likely be more efficient than copying each character individually.
 *
 * @param destination The [CharArray] to copy into.
 * @param destinationOffset The index in [destination] to start copying to.
 * @param startIndex The index in `this` of the first character to copy from (inclusive).
 * @param endIndex The index in `this` of the last character to copy from (exclusive).
 */
internal expect fun CharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
)
