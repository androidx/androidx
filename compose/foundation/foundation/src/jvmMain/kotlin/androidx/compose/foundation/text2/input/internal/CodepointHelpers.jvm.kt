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

internal actual fun CharSequence.codePointAt(index: Int): Int =
    java.lang.Character.codePointAt(this, index)

internal actual fun CharSequence.codePointCount(): Int =
    java.lang.Character.codePointCount(this, 0, length)

internal actual fun charCount(codePoint: Int): Int =
    java.lang.Character.charCount(codePoint)
