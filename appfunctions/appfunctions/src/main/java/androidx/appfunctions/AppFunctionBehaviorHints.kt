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

package androidx.appfunctions

import androidx.annotation.RestrictTo
import androidx.appfunctions.metadata.AppFunctionBehaviorHintsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata

/**
 * Declares advisory hints about the runtime behavior of an app function.
 *
 * Apply this annotation to a function annotated with [AppFunctionDeclaration] or an interface
 * annotated with [AppFunctionSignature] to populate [AppFunctionMetadata.behaviorHints] with an
 * [AppFunctionBehaviorHintsMetadata]. If an app function is not annotated with
 * [AppFunctionBehaviorHints], [AppFunctionMetadata.behaviorHints] is `null`.
 *
 * The properties and their default values conform to the
 * [Model Context Protocol (MCP) ToolAnnotations specification](https://modelcontextprotocol.io/specification/2025-11-25/schema#toolannotations):
 * [isReadOnly], [isDestructive], [isIdempotent], and [isOpenWorld].
 *
 * Behavior hints have the following characteristics:
 * - **Advisory only (no binding contract):** Hints describe how the function behaves without
 *   prescribing or enforcing how callers react. Any policy based on hints (such as showing a
 *   warning before invoking a destructive function) is determined solely by the caller.
 * - **No guarantee of accuracy:** Neither the library nor the platform verifies that declared hints
 *   match the function's implementation. Callers should treat hints from untrusted apps as
 *   untrusted and never make security-critical decisions based solely on them.
 * - **No validation:** Hint values and combinations are not validated at declaration time or
 *   reading time. Behavior hints are bespoke signals between the providing app (server) and caller
 *   (client) that the library does not mediate; the declared values are preserved as-is for the
 *   client and server to interpret.
 *
 * Example usage for service-based functions:
 * ```kotlin
 * @AppFunctionDeclaration(isDescribedByKDoc = false)
 * @AppFunctionBehaviorHints(
 *     isDestructive = true,
 *     isIdempotent = true,
 * )
 * suspend fun deleteNote(noteId: String) { ... }
 * ```
 *
 * Example usage for dynamically registered functions:
 * ```kotlin
 * @AppFunctionSignature(
 *     scope = AppFunctionMetadata.SCOPE_GLOBAL,
 *     appFunctionXmlFileName = "dynamic_signature_definitions",
 *     isDescribedByKDoc = false,
 * )
 * @AppFunctionBehaviorHints(isReadOnly = true)
 * fun interface SearchNotesSignature {
 *     suspend fun searchNotes(query: String): List<Note>
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AppFunctionBehaviorHints(
    /**
     * Whether the app function does not modify its environment.
     *
     * Defaults to `false`.
     */
    public val isReadOnly: Boolean = false,
    /**
     * Whether the app function may perform destructive updates to its environment, as opposed to
     * purely additive updates.
     *
     * Destructive updates include deleting or overwriting existing data, whereas additive updates
     * only create new records. Meaningful only when the function modifies its environment (that is,
     * when [isReadOnly] is `false`).
     *
     * Defaults to `true`.
     */
    public val isDestructive: Boolean = true,
    /**
     * Whether calling the app function repeatedly with the same arguments has no additional effect
     * on its environment.
     *
     * Meaningful only when the function modifies its environment (that is, when [isReadOnly] is
     * `false`).
     *
     * Defaults to `false`.
     */
    public val isIdempotent: Boolean = false,
    /**
     * Whether the app function may interact with an external "open world" of entities, as opposed
     * to operating strictly within a closed domain.
     *
     * Examples include searching the web, sending messages to external recipients, or calling
     * third-party services, as opposed to operating strictly within local app storage.
     *
     * Defaults to `true`.
     */
    public val isOpenWorld: Boolean = true,
)
