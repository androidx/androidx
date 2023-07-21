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

package androidx.bluetooth.integration.testapp.ui.common

import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog

fun AlertDialog.Builder.setViewEditText(editText: EditText): AlertDialog.Builder {
    val frameLayout = FrameLayout(editText.context)
    frameLayout.addView(editText)
    val frameLayoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    frameLayoutParams.setMargins(16.dp, 8.dp, 16.dp, 8.dp)
    frameLayout.layoutParams = frameLayoutParams

    val container = FrameLayout(editText.context)
    container.addView(frameLayout)

    setView(container)

    return this
}
