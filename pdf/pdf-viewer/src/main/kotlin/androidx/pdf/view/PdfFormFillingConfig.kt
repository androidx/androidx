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

package androidx.pdf.view

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

/**
 * Configuration for form filling.
 *
 * @param isFormFillingEnabled A lambda that returns true if form filling is enabled, false
 *   otherwise.
 * @param formFieldsHighlightColor The color to highlight form fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfFormFillingConfig(
    public val isFormFillingEnabled: () -> Boolean,
    @ColorInt public val formFieldsHighlightColor: Int,
)
