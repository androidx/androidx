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
package androidx.xr.glimmer.remote

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.creation.compose.util.DocumentStats

// TODO: b/575402181 - This is a temporary wrapper of [DocumentStats.examineDocument] because the
//  original doesn't work correctly. Remove and replace once the bug is fixed.
@Suppress("RestrictedApiAndroidX")
internal fun DocumentStats.Companion.examineFullDocument(document: CoreDocument): DocumentStats {
    val flatDocument = CoreDocument()
    document.operations.forEach { flatDocument.operations.addAllInTree(it) }
    return DocumentStats.examineDocument(flatDocument)
}

@Suppress("RestrictedApiAndroidX")
private fun MutableList<Operation>.addAllInTree(operation: Operation) {
    add(operation)
    if (operation is Container && operation !is ConditionalOperations) {
        operation.list.forEach { addAllInTree(it) }
    }
    if (operation is LayoutComponent) {
        operation.componentModifiers.list.forEach { addAllInTree(it) }
        operation.canvasOperations?.let { addAllInTree(it) }
    }
}
