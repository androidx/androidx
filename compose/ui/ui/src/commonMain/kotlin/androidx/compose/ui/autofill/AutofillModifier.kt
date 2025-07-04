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

package androidx.compose.ui.autofill

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics

/**
 * Set autofill hint with [contentType].
 *
 * This applies the [contentType] to the modifier's semantics, in turn enabling autofill and marking
 * the hint to be associated with this composable. This allows autofill frameworks to provide
 * relevant suggestions to users.
 *
 * Using `contentType` is equivalent to simply setting the `contentType` semantic property, i.e.
 * `Modifier.contentType(ContentType.NewUsername)` is equivalent to setting `Modifier.semantics {
 * contentType = ContentType.NewUsername }`.
 *
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithAutofillModifier
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithContentTypeSemantics
 * @param contentType The [ContentType] to apply to the component's semantics.
 * @return The [Modifier] with the specified [ContentType] semantics set.
 */
fun Modifier.contentType(contentType: ContentType): Modifier =
    this.semantics { this.contentType = contentType }
