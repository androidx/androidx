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

package androidx.appfunctions

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/**
 * Annotates an interface defining the schema for an app function, outlining its input, output, and
 * behavior
 *
 * Example Usage:
 * ```kotlin
 * @AppFunctionSchemaDefinition(name = "findNotes", version = 1, category = "Notes")
 * interface FindNotes {
 *   suspend fun findNotes(
 *     appFunctionContext: AppFunctionContext,
 *     findNotesParams: FindNotesParams,
 *   ): List<Note>
 * }
 * ```
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@Retention(
    // Binary because it's used to determine the annotation values from the compiled schema library.
    AnnotationRetention.BINARY
)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSchemaDefinition(
    val name: String,
    val version: Int,
    val category: String
)
