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

import android.view.View.AUTOFILL_TYPE_DATE
import android.view.View.AUTOFILL_TYPE_LIST
import android.view.View.AUTOFILL_TYPE_NONE
import android.view.View.AUTOFILL_TYPE_TEXT
import android.view.View.AUTOFILL_TYPE_TOGGLE

actual typealias NativeContentDataType = Int

@JvmInline
actual value class ContentDataType actual constructor(val dataType: Int) {
    actual companion object {
        actual val Text = ContentDataType(AUTOFILL_TYPE_TEXT)
        actual val List = ContentDataType(AUTOFILL_TYPE_LIST)
        actual val Date = ContentDataType(AUTOFILL_TYPE_DATE)
        actual val Toggle = ContentDataType(AUTOFILL_TYPE_TOGGLE)
        actual val None = ContentDataType(AUTOFILL_TYPE_NONE)
    }
}
