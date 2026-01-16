/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation.highlights.models

import androidx.annotation.RestrictTo

/**
 * Identifier for a highlight gesture that is currently in progress.
 *
 * Returned by
 * [androidx.pdf.annotation.highlights.InProgressTextHighlightsListener.onTextHighlightStarted] upon
 * the successful start of a text highlight gesture. This identifier is unique within the app
 * process lifetime and serves as a stable key for tracking the highlight state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InProgressHighlightId private constructor() {

    internal companion object {
        internal fun create(): InProgressHighlightId = InProgressHighlightId()
    }
}
