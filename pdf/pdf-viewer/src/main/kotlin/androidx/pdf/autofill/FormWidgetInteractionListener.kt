/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.autofill

import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo

/** Callback interface for events related to user interaction with form widgets. */
internal interface FormWidgetInteractionListener {
    /** Called when a user begins interacting with a widget. */
    fun onWidgetInteractionStarted(virtualId: Int, widgetInfo: FormWidgetInfo)

    /** Called when the value of a widget has changed. */
    fun onWidgetValueChanged(virtualId: Int, formEditInfo: FormEditInfo)

    /** Called when the user has finished interacting with a widget. */
    fun onWidgetInteractionFinished(virtualId: Int)
}
