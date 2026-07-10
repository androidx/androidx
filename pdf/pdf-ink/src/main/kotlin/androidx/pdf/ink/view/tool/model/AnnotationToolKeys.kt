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

package androidx.pdf.ink.view.tool.model

/**
 * Defines a centralized set of unique string keys for identifying each tool and action button
 * within the annotation toolbar.
 */
internal object AnnotationToolsKey {
    /** Key for the Pen tool. */
    const val PEN: String = "PEN"

    /** Key for the Highlighter tool. */
    const val HIGHLIGHTER: String = "HIGHLIGHTER"

    /** Key for the Eraser tool. */
    const val ERASER: String = "ERASER"

    /** Key for the Color Palette button. */
    const val COLOR_PALETTE: String = "COLOR_PALETTE"

    /** Key for the Undo action. */
    const val UNDO: String = "UNDO"

    /** Key for the Redo action. */
    const val REDO: String = "REDO"

    /** Key for the Toggle Annotation visibility button. */
    const val TOGGLE_ANNOTATION: String = "TOGGLE_ANNOTATION"
}
