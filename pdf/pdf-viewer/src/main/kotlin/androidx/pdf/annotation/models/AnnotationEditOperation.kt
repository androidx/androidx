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

package androidx.pdf.annotation.models

import androidx.annotation.RestrictTo

/**
 * An [EditOperation] that specifically operates on [PdfAnnotationData].
 *
 * @param op The type of operation, such as [Add], [Remove], or [Update].
 * @param edit The [PdfAnnotationData] that was added, removed, or updated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AnnotationEditOperation(
    override val op: Operation,
    override val edit: PdfAnnotationData,
) : EditOperation<PdfAnnotationData>(op, edit)
