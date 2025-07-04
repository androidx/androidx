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

import androidx.appfunctions.metadata.AppFunctionSchemaMetadata

/**
 * Marks an interface as a pre-defined schema for an App Function.
 *
 * The provided metadata will be stored in AppSearch when indexing App Functions. Agents can then
 * retrieve this metadata as an [AppFunctionSchemaMetadata] object by calling
 * [AppFunctionManagerCompat.observeAppFunctions]. Agent developers can define and share these
 * annotated App Function schemas as an SDK with app developers.
 *
 * A pre-defined schema outlines an App Function's capabilities, including its parameters and return
 * type. Knowing the schema in advance allows agents to perform tasks like model fine-tuning for
 * more accurate function calling.
 *
 * For example, here's how you might define a `findNotes` function schema:
 * ```
 * @AppFunctionSchemaDefinition(name = "findNotes", version = 1, category = "Notes")
 * interface FindNotesSchema {
 * suspend fun findNotes(
 * appFunctionContext: AppFunctionContext,
 * params: FindNotesParams,
 * ): List<Note>
 * }
 * ```
 *
 * @see AppFunctionSearchSpec
 */
@Retention(
    // Binary because it's used to determine the annotation values from the compiled schema library.
    AnnotationRetention.BINARY
)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSchemaDefinition(
    /** The name of the schema. */
    val name: String,
    /** The version of the schema. */
    val version: Int,
    /** The category of the schema. */
    val category: String,
)
