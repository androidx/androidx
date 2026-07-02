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

package androidx.pdf.ink.state

import androidx.annotation.IntDef
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_ANNOTATIONS

/** Represents the current editing state of the PDF document. */
internal sealed interface PdfEditMode {

    /** Edit mode is disabled; the user is in viewing mode. */
    object Disabled : PdfEditMode

    /**
     * Edit mode is enabled for a specific [journey].
     *
     * @property journey The current editing journey. Defaults to [EDITING_JOURNEY_ANNOTATIONS]
     */
    data class Enabled(@EditingJourney val journey: Int = EDITING_JOURNEY_ANNOTATIONS) : PdfEditMode

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDITING_JOURNEY_ANNOTATIONS, EDITING_JOURNEY_FORM_FILLING)
    annotation class EditingJourney

    companion object {
        internal const val EDITING_JOURNEY_ANNOTATIONS: Int = 0
        internal const val EDITING_JOURNEY_FORM_FILLING: Int = 1
    }
}
