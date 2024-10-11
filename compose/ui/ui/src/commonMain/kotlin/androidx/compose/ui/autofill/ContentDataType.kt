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

/**
 * Content data type information.
 *
 * Autofill services use the [ContentDataType] to determine what kind of field is associated with
 * the component.
 */
expect class NativeContentDataType

expect value class ContentDataType(val dataType: NativeContentDataType) {
    companion object {
        /** Indicates that the associated component is a text field. */
        val Text: ContentDataType

        /** Indicates that the associated component is a list. */
        val List: ContentDataType

        /** Indicates that the associated component is a date. */
        val Date: ContentDataType

        /** Indicates that the associated component is a toggle. */
        val Toggle: ContentDataType

        /**
         * Indicates that the associated component does not have a data type, and therefore is not
         * autofillable.
         */
        val None: ContentDataType
    }
}
