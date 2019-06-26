/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.engine.text

/**
 * A visual position in a string of text.
 * Creates an object representing a particular position in a string.
 *
 * The arguments must not be null (so the [offset] argument is required).
 *
 * The index of the character that immediately follows the position.
 *
 * For example, given the string `'Hello'`, offset 0 represents the cursor
 * being before the `H`, while offset 5 represents the cursor being just
 * after the `o`.
 *
 */
/* inline */ data class TextPosition(val offset: Int)