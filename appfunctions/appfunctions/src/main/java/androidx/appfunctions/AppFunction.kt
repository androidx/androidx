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

/**
 * Marks a function under [AppFunctionServiceEntryPoint] as callable by other applications.
 *
 * The `@AppFunction` annotation signals that the annotated function can be invoked by external
 * applications with proper permission (e.g., an agent). For instance, a note-taking app could
 * expose a function allowing an agent to create notes based on user commands.
 *
 * ### Thread Management
 *
 * IMPORTANT: By default functions annotated with @AppFunction are executed on the main thread.
 * declare the function as a suspend function and switch threads if the implementation performs
 * blocking operations.
 *
 * ### Cancellation Handling
 *
 * If the system or the calling application cancels the execution of an AppFunction, the coroutine
 * running the function will be canceled. Therefore, the implementation is recommended to handle
 * coroutine cancellation gradefully.
 *
 * To make a CPU-intensive loop cooperative with cancellation, periodically call
 * [kotlinx.coroutines.ensureActive] within the loop. This is a lightweight check that throws a
 * [kotlinx.coroutines.CancellationException] if the execution has been canceled.
 *
 * For example:
 * ```kotlin
 * @AppFunction
 * suspend fun processLargeData(
 *     context: AppFunctionContext,
 *     data: List<String>
 * ): List<Result> {
 *     // Switch to a background dispatcher for CPU-intensive work
 *     return withContext(backgroundDispatcher) {
 *         val results = mutableListOf<Result>()
 *         for (item in data) {
 *             // Periodically check for cancellation
 *             ensureActive()
 *             results.add(heavyProcessing(item))
 *         }
 *         results
 *     }
 * }
 * ```
 *
 * ### Error Handling
 *
 * In exceptional cases, implementations should throw an appropriate
 * [androidx.appfunctions.AppFunctionException]. This allows the agent to better understand the
 * cause of the failure. For example, if an input argument is invalid, throw an
 * [androidx.appfunctions.AppFunctionInvalidArgumentException] with a detailed message explaining
 * why it is invalid.
 *
 * ### Supported Types
 *
 * For a detailed list of supported types and the rules governing their serialization, see
 * [androidx.appfunctions.AppFunctionSerializable].
 *
 * ### Deprecate AppFunction
 *
 * If an existing `AppFunction` needs to be deprecated (e.g., a replacement is available, but the
 * old version must remain for backward compatibility), mark the function with the
 * [kotlin.Deprecated] annotation.
 *
 * This deprecation status will be exposed in the
 * [androidx.appfunctions.metadata.AppFunctionMetadata.deprecation] field, allowing clients to
 * identify and migrate away from the deprecated function.
 *
 * Example:
 * ```
 * @AppFunction
 * @Deprecated(
 *   message = "Use newSearchFunction(query) instead. " +
 *     "This function will be removed in a future version.",
 * )
 * fun oldSearchFunction(...) {
 * // ...
 * }
 * ```
 *
 * @see AppFunctionServiceEntryPoint
 * @see androidx.appfunctions.AppFunctionException
 * @see androidx.appfunctions.AppFunctionSerializable
 */
// Use BINARY here so that the annotation is kept around at the aggregation stage.
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class AppFunction(
    /**
     * Indicates whether this function is enabled. The default value is `true`.
     *
     * If set to `false`, this function will be unavailable for invocation by other applications
     * unless explicitly enabled at runtime. When disabled, any attempt to call this function by
     * another application will be rejected.
     */
    public val isEnabled: Boolean = true,

    /**
     * Whether to use the function's KDoc as a function's description for the agent. The default
     * value is `false`.
     *
     * If set to `true`, the KDoc will be used to populate:
     * - The function's [androidx.appfunctions.metadata.AppFunctionMetadata.description] as the
     *   KDoc, excluding Kotlin's supported tags like `@param`, `@throws`.
     * - The function's parameters'
     *   [androidx.appfunctions.metadata.AppFunctionParameterMetadata.description] from the KDoc's
     *   `@param` tags.
     * - The function's response's
     *   [androidx.appfunctions.metadata.AppFunctionResponseMetadata.description] from the KDoc's
     *   `@return` tags.
     *
     * Note: If an [AppFunctionInstruction] annotation is also present on the method, parameter, or
     * return type, its value will take precedence and override the corresponding KDoc description.
     *
     * Example:
     * ```kotlin
     * /**
     * * Creates a new note with a given title and content.
     * *
     * * @param title The title of the note.
     * * @param content The main body or text of the note.
     * * @return The created note.
     * * @throws IllegalArgumentException if the `title` or `content` is empty or too long.
     * */
     * @AppFunction(isDescribedByKDoc = true)
     * fun CreateNote(title: String, content: String): Note { .. }
     * ```
     *
     * In this example:
     * - `AppFunctionMetadata.description` will be: "Creates a new note with a given title and
     *   content."
     * - `title`'s `AppFunctionParameterMetadata.description` will be: "The title of the note."
     * - `content`'s `AppFunctionParameterMetadata.description` will be: "The main body or text of
     *   the note."
     * - `AppFunctionResponseMetadata.description` will be: "The created note."
     */
    public val isDescribedByKDoc: Boolean = false,
)
