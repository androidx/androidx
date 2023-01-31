/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.internal.parsers

/**
 * Copied from https://cs.android.com/android-studio/platform/tools/adt/idea/+/858f81bb7c350bc7a05daad36edefd21f74c8cef:android/src/com/android/tools/idea/rendering/parsers/AaptAttrParser.java
 *
 * Interface for parsers that support declaration of inlined {@code aapt:attr} attributes
 */
interface AaptAttrParser {
  /**
   * Returns a [Map] that contains all the `aapt:attr` elements declared in this or any
   * children parsers. This list can be used to resolve `@aapt/_aapt` references into this parser.
   */
  fun getAaptDeclaredAttrs(): Map<String, TagSnapshot>
}
