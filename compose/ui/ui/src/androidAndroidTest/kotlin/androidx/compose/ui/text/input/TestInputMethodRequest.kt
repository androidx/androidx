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

package androidx.compose.ui.text.input

import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

<<<<<<<< HEAD:compose/ui/ui-text/src/skikoMain/kotlin/androidx/compose/ui/text/input/PlatformTextInputAdapter.skiko.kt
// TODO(b/267235947) Flesh this out, document it, and wire it up when ready to integrate new text
//  field with desktop.
// TODO: [1.4 Update] implement it properly
@ExperimentalTextApi
@Immutable
actual interface PlatformTextInputPlugin<T : PlatformTextInputAdapter>
========
internal open class TestInputMethodRequest(view: View) : BaseInputConnection(view, false),
    PlatformTextInputMethodRequest {
>>>>>>>> sync-androidx/revert/revert-1.6.0-alpha01_merge-1.6.0-alpha02:compose/ui/ui/src/androidAndroidTest/kotlin/androidx/compose/ui/text/input/TestInputMethodRequest.kt

    override fun createInputConnection(outAttrs: EditorInfo): InputConnection {
        return this
    }
}
