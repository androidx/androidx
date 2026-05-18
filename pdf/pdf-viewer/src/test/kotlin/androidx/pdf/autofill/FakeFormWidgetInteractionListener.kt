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

/** A fake implementation of [FormWidgetInteractionListener] that records interaction events. */
internal class FakeFormWidgetInteractionListener : FormWidgetInteractionListener {

    /** Groups the arguments of an interaction event for atomic verification. */
    data class FormWidgetInteractionEvent(
        val id: Int,
        val info: FormWidgetInfo? = null,
        val formEditInfo: FormEditInfo? = null,
    )

    var startEvent: FormWidgetInteractionEvent? = null
    var changeEvent: FormWidgetInteractionEvent? = null
    var finishEvent: FormWidgetInteractionEvent? = null

    override fun onWidgetInteractionStarted(virtualId: Int, widgetInfo: FormWidgetInfo) {
        startEvent = FormWidgetInteractionEvent(virtualId, widgetInfo)
    }

    override fun onWidgetValueChanged(virtualId: Int, formEditInfo: FormEditInfo) {
        changeEvent = FormWidgetInteractionEvent(virtualId, formEditInfo = formEditInfo)
    }

    override fun onWidgetInteractionFinished(virtualId: Int) {
        finishEvent = FormWidgetInteractionEvent(virtualId)
    }
}
