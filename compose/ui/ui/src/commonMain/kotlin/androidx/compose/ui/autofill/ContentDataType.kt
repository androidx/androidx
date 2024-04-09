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

package androidx.compose.ui.autofill

// TODO(b/333102566): When Autofill goes live for Compose,
//  these classes will need to be made public.

// Using `typealias` with the internal qualifier triggers a Kotlin visibility issue.
// For now, the types will be kept internal and just as Ints. When Autofill goes live,
// the `NativeContentDataType` and `ContentDataType` classes will be used.
// expect class NativeContentDataType

internal expect value class ContentDataType(val dataType: Int) {
    internal companion object {
        val Text: ContentDataType
        val List: ContentDataType
        val Date: ContentDataType
        val Toggle: ContentDataType
        val None: ContentDataType
    }
}
